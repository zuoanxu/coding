package com.example.order.service;

import com.example.order.common.BusinessException;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.entity.Order;
import com.example.order.entity.Product;
import com.example.order.mapper.OrderItemMapper;
import com.example.order.mapper.OrderMapper;
import com.example.order.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;        // 假对象：不会真的操作数据库
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private OrderService orderService;      // 把上面三个假对象自动注入进来

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("iPhone 15 Pro");
        product.setPrice(new BigDecimal("8999.00"));
        product.setStock(100);
    }

    // 1. 正常下单成功
    @Test
    void createOrder_success() {
        CreateOrderRequest req = new CreateOrderRequest();
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);
        req.setItems(Collections.singletonList(item));

        // 设定假对象的行为：查到商品、扣库存成功
        when(productMapper.findById(1L)).thenReturn(product);
        when(productMapper.deductStock(1L, 2)).thenReturn(1);

        Order order = orderService.createOrder(req);

        assertNotNull(order);
        assertEquals(new BigDecimal("17998.00"), order.getTotalAmount()); // 8999 * 2
        assertTrue(order.getOrderNo().startsWith("ORD"));
        verify(orderMapper).insert(any(Order.class));   // 确认真的调用了 insert
        verify(orderItemMapper).insert(any());
    }

    // 2. 商品列表为空
    @Test
    void createOrder_emptyItems_throws() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setItems(Collections.emptyList());
        assertThrows(BusinessException.class, () -> orderService.createOrder(req));
    }

    // 3. 数量非法
    @Test
    void createOrder_invalidQuantity_throws() {
        CreateOrderRequest req = new CreateOrderRequest();
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(0);
        req.setItems(Collections.singletonList(item));
        assertThrows(BusinessException.class, () -> orderService.createOrder(req));
    }

    // 4. 商品不存在
    @Test
    void createOrder_productNotFound_throws() {
        CreateOrderRequest req = new CreateOrderRequest();
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(99L);
        item.setQuantity(1);
        req.setItems(Collections.singletonList(item));

        when(productMapper.findById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> orderService.createOrder(req));
    }

    // 5. 库存不足
    @Test
    void createOrder_insufficientStock_throws() {
        CreateOrderRequest req = new CreateOrderRequest();
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(1000);
        req.setItems(Collections.singletonList(item));

        when(productMapper.findById(1L)).thenReturn(product);
        when(productMapper.deductStock(1L, 1000)).thenReturn(0); // 返回 0 = 库存不足
        assertThrows(BusinessException.class, () -> orderService.createOrder(req));
    }
}