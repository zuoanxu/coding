package com.example.order.RocketMQ;

import com.example.order.dto.OrderEvent;
import com.example.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 订单延时取消监听器
 * 消费端幂等：用 eventId + Redis 去重，保证同一条消息重复投递时只处理一次
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "order-delay-topic",
        consumerGroup = "order-cancel-group",
        selectorExpression = "cancel"   // 只收 tag=cancel 的消息
)
public class OrderCancelListener implements RocketMQListener<OrderEvent> {

    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(OrderEvent event) {
        // 幂等第一步：用 eventId 占位。占到就处理，占不到说明处理过，跳过
        String key = "order:event:" + event.getEventId();
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMinutes(10));
        if (!Boolean.TRUE.equals(first)) {
            log.warn("重复事件已处理过，跳过, eventId={}", event.getEventId());
            return;
        }

        try {
            // 幂等第二步：真正业务逻辑（doCancel 内部还有 status 判断，做双重保险）
            orderService.doCancel(event.getOrderId());
        } catch (Exception e) {
            // 处理失败：删掉占位 key 再抛异常，让 RocketMQ 重投后能重新处理
            stringRedisTemplate.delete(key);
            throw e;
        }
    }
}