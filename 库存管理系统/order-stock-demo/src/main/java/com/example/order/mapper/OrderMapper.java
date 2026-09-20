package com.example.order.mapper;

import com.example.order.entity.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单 Mapper
 */
public interface OrderMapper {

    @Select("SELECT * FROM t_order ORDER BY id DESC")
    List<Order> findAll();

    @Select("SELECT * FROM t_order ORDER BY id DESC LIMIT #{limit}")
    List<Order> findRecent(@Param("limit") int limit);

    @Select("SELECT * FROM t_order WHERE id = #{id}")
    Order findById(Long id);

    @Insert("INSERT INTO t_order(order_no, total_amount, status, create_time, update_time) " +
            "VALUES(#{orderNo}, #{totalAmount}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    /**
     * 更新订单状态（带旧状态判断，防止并发下重复操作）
     * 返回 0 表示状态已变化，更新失败
     */
    @Update("UPDATE t_order SET status = #{status}, update_time = NOW() " +
            "WHERE id = #{id} AND status = #{oldStatus}")
    int updateStatus(@Param("id") Long id,
                     @Param("status") int status,
                     @Param("oldStatus") int oldStatus);

    @Select("SELECT COUNT(*) FROM t_order")
    long count();

    @Select("SELECT COUNT(*) FROM t_order WHERE status = #{status}")
    long countByStatus(@Param("status") int status);

    @Select("SELECT IFNULL(SUM(total_amount), 0) FROM t_order WHERE status = 1")
    BigDecimal sumPaidAmount();
}
