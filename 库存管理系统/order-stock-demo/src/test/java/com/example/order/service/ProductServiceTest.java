package com.example.order.service;

import com.example.order.common.BusinessException;
import com.example.order.entity.Product;
import com.example.order.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    void add_emptyName_throws() {
        Product p = new Product();
        p.setPrice(new BigDecimal("10"));
        assertThrows(BusinessException.class, () ->
                productService.add(p));
    }

    @Test
    void add_negativePrice_throws() {
        Product p = new Product();
        p.setName("商品");
        p.setPrice(new BigDecimal("-1"));
        assertThrows(BusinessException.class, () ->
                productService.add(p));
    }

    @Test
    void add_success() {
        Product p = new Product();
        p.setName("商品");
        p.setPrice(new BigDecimal("10"));
        p.setStock(null); // 库存没传时，Service 会默认设成 0
        productService.add(p);
        assertEquals(0, p.getStock());
        verify(productMapper).insert(p);
    }
}