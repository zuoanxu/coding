package com.example.order.common;

/**
 * 业务异常：库存不足、订单状态错误等，会被全局异常处理器捕获返回给前端
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
