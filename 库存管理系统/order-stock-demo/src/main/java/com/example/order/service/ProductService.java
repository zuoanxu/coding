package com.example.order.service;

import com.example.order.common.BusinessException;
import com.example.order.entity.Product;
import com.example.order.mapper.ProductMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * 商品服务：商品的增删改查和库存调整
 */
@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;

    // StringRedisTemplate：操作 Redis，key 和 value 都是字符串，简单直观
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ObjectMapper：负责 Java 对象 <-> JSON 字符串 的互相转换
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StockCache stockCache;

    /** 缓存 key 的前缀，避免和其他业务的 key 撞名 */
    private static final String CACHE_KEY_PREFIX = "product:";

    /** 缓存过期时间：30 分钟 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    /** 空值缓存的标记：表示「这个商品不存在」。用 "NULL" 这个特殊字符串，和正常商品 JSON 不会撞 */
    private static final String EMPTY_CACHE = "NULL";

    /** 空值缓存的过期时间：1 分钟（比正常缓存短，防止大量空值占满内存） */
    private static final Duration EMPTY_CACHE_TTL = Duration.ofMinutes(1);

    /** 查询商品列表 */
    public List<Product> list() {
        return productMapper.findAll();
    }

    /** 查询单个商品（带缓存） */
    public Product getById(Long id) {
        String key = CACHE_KEY_PREFIX + id;

        // 第 1 步：先查缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json != null) {
            // 【新增】命中了「空值缓存」，说明之前查过、这个商品不存在，直接抛异常，不再查数据库
            if (EMPTY_CACHE.equals(json)) {
                throw new BusinessException("商品不存在");
            }
            // 正常命中，把 JSON 转回 Product 返回
            try {
                return objectMapper.readValue(json, Product.class);
            } catch (JsonProcessingException e) {
                stringRedisTemplate.delete(key);
            }
        }

        // 第 2 步：缓存没命中，查数据库
        Product product = productMapper.findById(id);
        if (product == null) {
            // 【新增】商品不存在：先缓存一个空值标记，再抛异常，防止缓存穿透
            stringRedisTemplate.opsForValue().set(key, EMPTY_CACHE, EMPTY_CACHE_TTL);
            throw new BusinessException("商品不存在");
        }

        // 第 3 步：把查到的商品写进缓存
        try {
            stringRedisTemplate.opsForValue()
                    .set(key, objectMapper.writeValueAsString(product), CACHE_TTL);
        } catch (JsonProcessingException e) {
        }

        return product;
    }

    /** 新增商品 */
    @Transactional
    public Product add(Product product) {
        if (!StringUtils.hasText(product.getName())) {
            throw new BusinessException("商品名称不能为空");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("价格不能为负数");
        }
        if (product.getStock() == null) {
            product.setStock(0);
        }
        productMapper.insert(product);
        // 新增商品后，把它的库存也放进 Redis（insert 会回填 product.id，所以此时 id 已经有了）
        stockCache.setStock(product.getId(), product.getStock());
        return product;
    }

    /** 调整库存：delta 为正增加，为负减少 */
    @Transactional
    public void adjustStock(Long id, int delta) {
        Product product = getById(id);
        if (delta > 0) {
            productMapper.addStock(id, delta);
        } else if (delta < 0) {
            // 乐观锁扣库存：只有库存足够才成功
            int rows = productMapper.deductStock(id, -delta);
            if (rows == 0) {
                throw new BusinessException("库存不足，当前库存：" + product.getStock());
            }
        }
        // 改完 MySQL 后，把 Redis 里的库存同步成 MySQL 的最新值，保证两边一致
        stockCache.setStock(id, productMapper.findById(id).getStock());
        // 库存变了，删掉缓存，下次查询重新读数据库
        deleteCache(id);
    }

    /** 删除商品 */
    @Transactional
    public void delete(Long id) {
        getById(id);
        productMapper.delete(id);
        // 商品删了，删掉缓存
        stockCache.delete(id);   // 商品删了，Redis 里的库存也删掉
        // 商品删了，删掉缓存
        deleteCache(id);
    }

    public List<Product> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return productMapper.findAll();
        }
        return productMapper.search(keyword);
    }

    /** 删除某个商品的缓存 */
    private void deleteCache(Long id) {
        stringRedisTemplate.delete(CACHE_KEY_PREFIX + id);
    }
}
