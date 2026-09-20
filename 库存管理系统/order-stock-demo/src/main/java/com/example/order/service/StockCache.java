package com.example.order.service;

import com.example.order.entity.Product;
import com.example.order.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

/**
 * 库存缓存：把库存放到 Redis 里，用 Lua 脚本实现「检查 + 扣减」的原子操作
 *
 * 为什么用 Lua？
 * 「先查库存、再扣减」如果分成两条 Redis 命令，中间会被别的请求插进来，
 * 两个请求可能都读到「库存够」，然后都扣减，导致超卖。
 * 而 Lua 脚本在 Redis 里是「一次执行、中间不被打断」的，所以检查 + 扣减是一个原子动作。
 */
@Slf4j
@Component
public class StockCache {

    /** 库存 key 前缀，实际 key 形如 stock:1 */
    private static final String STOCK_KEY_PREFIX = "stock:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 扣库存的 Lua 脚本
     * KEYS[1] = 库存 key（如 stock:1）
     * ARGV[1] = 要扣的数量
     * 逻辑：库存够就 DECRBY 扣减并返回剩余库存；不够返回 -1
     */
    private static final DefaultRedisScript<Long> DEDUCT_SCRIPT = new DefaultRedisScript<>(
            "local stock = tonumber(redis.call('GET', KEYS[1]) or '0')\n" +
                    "local qty = tonumber(ARGV[1])\n" +
                    "if stock >= qty then\n" +
                    "    redis.call('DECRBY', KEYS[1], qty)\n" +
                    "    return stock - qty\n" +
                    "else\n" +
                    "    return -1\n" +
                    "end",
            Long.class);

    /** 项目启动时，把 MySQL 里的库存加载进 Redis（缓存预热），否则 Redis 里没有库存数据 */
    @PostConstruct
    public void init() {
        List<Product> products = productMapper.findAll();
        for (Product p : products) {
            stringRedisTemplate.opsForValue()
                    .set(STOCK_KEY_PREFIX + p.getId(), String.valueOf(p.getStock()));
        }
        log.info("库存缓存预热完成，共 {} 个商品", products.size());
    }

    /**
     * 预扣库存（原子）：检查 Redis 库存是否足够，足够则扣减
     * @return 剩余库存；返回 -1 表示库存不足
     */
    public Long deduct(Long productId, int quantity) {
        String key = STOCK_KEY_PREFIX + productId;
        return stringRedisTemplate.execute(
                DEDUCT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(quantity));
    }

    /** 回补库存（取消订单 / 下单失败时用）。INCRBY 本身就是原子的，所以不需要 Lua */
    public Long addStock(Long productId, int quantity) {
        String key = STOCK_KEY_PREFIX + productId;
        return stringRedisTemplate.opsForValue().increment(key, quantity);
    }

    /** 设置库存（调整库存、新增商品时，让 Redis 跟 MySQL 保持一致） */
    public void setStock(Long productId, int stock) {
        stringRedisTemplate.opsForValue().set(STOCK_KEY_PREFIX + productId, String.valueOf(stock));
    }

    /** 删除库存缓存（删除商品时） */
    public void delete(Long productId) {
        stringRedisTemplate.delete(STOCK_KEY_PREFIX + productId);
    }
}