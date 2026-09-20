package com.example.order.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细实体，对应数据库表 t_order_item
 */
@Data
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;  // 下单时的商品名称快照
    private BigDecimal price;    // 下单时的单价快照
    private Integer quantity;
    private BigDecimal totalPrice;
}
