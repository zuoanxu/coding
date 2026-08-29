package com.example.order.controller;

import com.example.order.common.Result;
import com.example.order.dto.CreateOrderRequest;
import javax.validation.Valid;
import com.example.order.entity.Order;
import com.example.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单接口
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /** 订单列表 */
    @GetMapping
    public Result<List<Order>> list() {
        return Result.success(orderService.list());
    }

    /** 订单详情（含明细） */
    @GetMapping("/{id}")
    public Result<Order> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    /** 创建订单 */
    @PostMapping
    public Result<Order> create(@Valid @RequestBody CreateOrderRequest request) {
        return Result.success(orderService.createOrder(request));
    }

    /** 支付订单 */
    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.success();
    }

    /** 手动取消订单 */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.success();
    }
}
