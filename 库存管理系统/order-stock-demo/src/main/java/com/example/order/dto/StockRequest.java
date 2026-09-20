package com.example.order.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * 调整库存的请求参数：delta 为正表示增加，为负表示减少
 */
@Data
public class StockRequest {
    @NotNull(message = "库存调整值不能为空")
    private Integer delta;
}
