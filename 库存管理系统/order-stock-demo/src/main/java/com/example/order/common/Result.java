package com.example.order.common;

import lombok.Data;

/**
 * 统一返回结果，前端根据 code 判断是否成功
 */
@Data
public class Result<T> {

    private int code;      // 200 成功，其它为失败
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
