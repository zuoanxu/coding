package com.example.order.mapper;

import com.example.order.entity.Product;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品 Mapper，使用注解方式写 SQL，简单直观
 */
public interface ProductMapper {

    @Select("SELECT * FROM t_product ORDER BY id DESC")
    List<Product> findAll();

    @Select("SELECT * FROM t_product WHERE id = #{id}")
    Product findById(Long id);

    @Insert("INSERT INTO t_product(name, price, stock, description, remark,create_time,  update_time) " +
            "VALUES(#{name}, #{price}, #{stock}, #{description}, #{remark}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    @Update("UPDATE t_product SET name = #{name}, price = #{price}, description = #{description},remark = #{remark}, update_time = NOW() " +
            "WHERE id = #{id}")
    int update(Product product);

    /**
     * 扣减库存（乐观锁）：只有库存充足时才会成功，返回受影响行数
     * 返回 0 表示库存不足
     */
    @Update("UPDATE t_product SET stock = stock - #{quantity}, update_time = NOW() " +
            "WHERE id = #{id} AND stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    /** 增加库存（用于取消订单时回补） */
    @Update("UPDATE t_product SET stock = stock + #{quantity}, update_time = NOW() WHERE id = #{id}")
    int addStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Delete("DELETE FROM t_product WHERE id = #{id}")
    int delete(Long id);

    @Select("SELECT COUNT(*) FROM t_product")
    long count();

    @Select("SELECT COUNT(*) FROM t_product WHERE stock < #{threshold}")
    long countLowStock(@Param("threshold") int threshold);
    @Select("SELECT * FROM t_product WHERE name LIKE CONCAT('%',#{keyword}, '%') OR remark LIKE CONCAT('%', #{keyword}, '%') ORDER BY id DESC")
    List<Product> search(@Param("keyword") String keyword);
}
