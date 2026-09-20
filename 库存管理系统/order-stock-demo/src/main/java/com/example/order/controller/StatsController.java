package com.example.order.controller;

import com.example.order.common.Result;
import com.example.order.entity.Order;
import com.example.order.mapper.OrderMapper;
import com.example.order.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 概览统计接口（首页仪表盘用）
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @GetMapping
    public Result<Map<String, Object>> stats() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("productCount", productMapper.count());
        map.put("orderCount", orderMapper.count());
        map.put("paidOrderCount", orderMapper.countByStatus(Order.STATUS_PAID));
        map.put("totalSales", orderMapper.sumPaidAmount());
        map.put("lowStockCount", productMapper.countLowStock(10));
        return Result.success(map);
    }
}
