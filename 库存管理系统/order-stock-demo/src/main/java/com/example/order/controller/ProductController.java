package com.example.order.controller;

import com.example.order.common.Result;
import com.example.order.dto.StockRequest;
import javax.validation.Valid;
import com.example.order.entity.Product;
import com.example.order.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

/**
 * 商品接口
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    /** 商品列表（走 Redis 缓存） */
    @GetMapping
    public Result<List<Product>> list() {
        return Result.success(productService.list());
    }

    /** 商品详情 */
    @GetMapping("/{id}")
    public Result<Product> get(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }
    @GetMapping("/search")
    public Result<List<Product>> search(@RequestParam String keyword) {
        return Result.success(productService.search(keyword));
    }
    /** 新增商品 */
    @PostMapping
    public Result<Product> add(@Valid @RequestBody Product product) {
        return Result.success(productService.add(product));
    }

    /** 调整库存 */
    @PutMapping("/{id}/stock")
    public Result<Void> adjustStock(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        productService.adjustStock(id, request.getDelta());
        return Result.success();
    }

    /** 删除商品 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }
}
