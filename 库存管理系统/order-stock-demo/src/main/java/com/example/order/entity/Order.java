package com.example.order.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单实体，对应数据库表 t_order
 */
@Data
public class Order {

    /** 订单状态常量 */
    public static final int STATUS_UNPAID = 0;    // 待支付
    public static final int STATUS_PAID = 1;      // 已支付
    public static final int STATUS_CANCELED = 2;  // 已取消

    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer status;
    private Date createTime;
    private Date updateTime;

    /** 订单明细（非数据库字段，查询明细时再填充） */
    private List<OrderItem> items;
}
