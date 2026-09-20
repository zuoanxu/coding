package com.example.order.mapper;

import com.example.order.entity.OrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单明细 Mapper
 */
public interface OrderItemMapper {

    @Insert("INSERT INTO t_order_item(order_id, product_id, product_name, price, quantity, total_price) " +
            "VALUES(#{orderId}, #{productId}, #{productName}, #{price}, #{quantity}, #{totalPrice})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderItem item);

    @Select("SELECT * FROM t_order_item WHERE order_id = #{orderId}")
    List<OrderItem> findByOrderId(Long orderId);
}
