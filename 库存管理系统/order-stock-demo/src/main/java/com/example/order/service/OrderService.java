package com.example.order.service;

import com.example.order.common.BusinessException;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderEvent;
import com.example.order.entity.Order;
import com.example.order.entity.OrderItem;
import com.example.order.entity.Product;
import com.example.order.mapper.OrderItemMapper;
import com.example.order.mapper.OrderMapper;
import com.example.order.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;

/**
 * 订单服务
 * 核心流程：下单扣库存 -> 创建订单 -> 发送延时消息（超时未支付自动取消）
 */
@Slf4j
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private StockCache stockCache;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 创建订单（整个方法在一个事务里，任何一步失败都会回滚）
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("请选择要购买的商品");
        }

        // 日志：下单入口，此时还没有 orderId，用 requestNo 追踪
        log.info("收到下单请求, requestNo={}, 商品数={}",
                request.getRequestNo(), request.getItems().size());

        String idempotentKey = "order:submit:" + request.getRequestNo();
        Boolean firstSubmit = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", Duration.ofMinutes(10));
        if (!Boolean.TRUE.equals(firstSubmit)) {
            // 日志：重复提交
            log.warn("重复提交被拦截, requestNo={}", request.getRequestNo());
            throw new BusinessException("请勿重复提交订单");
        }

        List<OrderItem> items = new ArrayList<>();
        try {
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
                if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                    throw new BusinessException("商品数量必须大于 0");
                }

                Product product = productMapper.findById(itemReq.getProductId());
                if (product == null) {
                    throw new BusinessException("商品不存在，id=" + itemReq.getProductId());
                }

                Long left = stockCache.deduct(product.getId(), itemReq.getQuantity());
                if (left == null || left < 0) {
                    // 日志：库存不足
                    log.warn("下单库存不足, requestNo={}, productId={}, 需要={}",
                            request.getRequestNo(), product.getId(), itemReq.getQuantity());
                    throw new BusinessException("库存不足：" + product.getName() + "（剩余 " + product.getStock() + "）");
                }

                int rows = productMapper.deductStock(product.getId(), itemReq.getQuantity());
                if (rows == 0) {
                    throw new BusinessException("库存不足：" + product.getName());
                }

                OrderItem item = new OrderItem();
                item.setProductId(product.getId());
                item.setProductName(product.getName());
                item.setPrice(product.getPrice());
                item.setQuantity(itemReq.getQuantity());
                item.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
                items.add(item);
                totalAmount = totalAmount.add(item.getTotalPrice());
            }

            Order order = new Order();
            order.setOrderNo(generateOrderNo());
            order.setTotalAmount(totalAmount);
            order.setStatus(Order.STATUS_UNPAID);
            orderMapper.insert(order);

            // 日志：下单成功，到这里 orderId 已经生成（insert 回填了 order.getId()）
            log.info("下单成功, orderId={}, orderNo={}, 金额={}",
                    order.getId(), order.getOrderNo(), totalAmount);

            // 明细入库（只插明细，不在这里发消息）
            for (OrderItem item : items) {
                item.setOrderId(order.getId());
                orderItemMapper.insert(item);
            }

            // 发延时消息：把订单事件打包成 OrderEvent 发出去（一个订单只发一条）
            OrderEvent event = new OrderEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setOrderId(order.getId());
            event.setEventVersion(1);
            event.setCreateTime(new Date());

            Message<OrderEvent> msg = MessageBuilder.withPayload(event).build();
            // 异步发送：不阻塞下单主链路。延时取消是旁路逻辑，发送失败不应让下单整体回滚
            rocketMQTemplate.asyncSend("order-delay-topic:cancel", msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("延时取消消息已发送, orderId={}, msgId={}", order.getId(), sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("延时取消消息发送失败(下单不回滚), orderId={}", order.getId(), e);
                }
            }, 5000, 16);

            order.setItems(items);
            return order;
        } catch (Exception e) {
            stringRedisTemplate.delete(idempotentKey);
            for (OrderItem item : items) {
                stockCache.addStock(item.getProductId(), item.getQuantity());
            }
            // 日志：下单失败，回补了哪些库存
            log.warn("下单失败已回补, requestNo={}, 已回补商品数={}, 原因={}",
                    request.getRequestNo(), items.size(), e.getMessage());
            throw e;
        }
    }

    /** 查询订单明细 */
    public Order detail(Long id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setItems(orderItemMapper.findByOrderId(id));
        return order;
    }

    public List<Order> list() {
        return orderMapper.findAll();
    }

    public List<Order> listRecent(int limit) {
        return orderMapper.findRecent(limit);
    }

    /** 支付订单：待支付 -> 已支付 */
    @Transactional
    public void pay(Long id) {
        int rows = orderMapper.updateStatus(id, Order.STATUS_PAID, Order.STATUS_UNPAID);
        if (rows == 0) {
            // 日志：支付失败
            log.warn("支付失败, orderId={}, 原因=订单不存在或非待支付", id);
            throw new BusinessException("订单不存在或不是待支付状态");
        }
        // 日志：支付成功
        log.info("支付成功, orderId={}", id);
    }

    /** 手动取消订单 */
    @Transactional
    public void cancel(Long id) {
        doCancel(id);
    }

    /**
     * 执行取消：状态 待支付 -> 已取消，并回补库存
     * 用 WHERE status = 待支付 保证并发下只回补一次
     */
    public void doCancel(Long id) {
        int rows = orderMapper.updateStatus(id, Order.STATUS_CANCELED, Order.STATUS_UNPAID);
        if (rows == 0) {
            // 日志：取消没成功（订单不存在或已被处理）
            log.warn("取消失败, orderId={}, 原因=订单不存在或已处理", id);
            return;
        }
        List<OrderItem> items = orderItemMapper.findByOrderId(id);
        for (OrderItem item : items) {
            productMapper.addStock(item.getProductId(), item.getQuantity());
            stockCache.addStock(item.getProductId(), item.getQuantity());
        }
        // 日志：取消成功（原来的这行日志也规范化了，统一用 orderId= 前缀）
        log.info("取消成功并回补库存, orderId={}, 回补商品数={}", id, items.size());
    }

    /** 生成订单号：时间戳 + 3 位随机数，简单保证唯一 */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + String.format("%03d", new Random().nextInt(1000));
    }
}
