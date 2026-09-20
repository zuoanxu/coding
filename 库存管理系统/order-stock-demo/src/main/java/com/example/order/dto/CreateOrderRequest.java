package com.example.order.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import javax.validation.constraints.NotBlank;

@Data
public class CreateOrderRequest {

    /** @NotEmpty：列表不能为 null 也不能为空 */
    @NotEmpty(message = "请选择要购买的商品")
    @Valid   // 这个 @Valid 让 Spring 继续校验列表里每一个子项
    private List<OrderItemRequest> items;
    /** 业务请求号：前端生成，用于防重复提交（幂等）。每次打开下单弹窗生成一次，本次下单所有请求共用 */
    @NotBlank(message = "缺少业务请求号")
    private String requestNo;
    @Data
    public static class OrderItemRequest {
        @NotNull(message = "商品ID不能为空")
        private Long productId;

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量必须大于 0")
        private Integer quantity;
    }
}
