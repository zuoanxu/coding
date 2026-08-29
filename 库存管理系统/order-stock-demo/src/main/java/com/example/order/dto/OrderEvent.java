package com.example.order.dto;

import lombok.Data;

import java.util.Date;

/**
 * 订单事件 DTO：
 * 订单领域"发生了一件事"时，把这件事打包成对象，通过 RocketMQ 发出去。
 * 它会序列化成 JSON 传输，所以字段必须能被 JSON 序列化（有 getter/setter 就行）。
 */
@Data
public class OrderEvent {

    /** 事件ID：全局唯一，用 UUID 生成。消费者拿它去重，避免重复处理（幂等）。 */
    private String eventId;

    /** 订单ID：这个事件是关于哪个订单的，业务核心信息。 */
    private Long orderId;

    /** 事件版本：事件"格式"的版本号，从 1 开始。以后加字段时靠它区分新老消息。 */
    private Integer eventVersion;

    /** 创建时间：这件事什么时候发生的。 */
    private Date createTime;
}
