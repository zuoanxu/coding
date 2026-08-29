package com.example.order.entity;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品实体，对应数据库表 t_product
 */
@Data
public class Product {
    private Long id;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;   // 金额用 BigDecimal，避免精度丢失

    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    private String description;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
