package com.love.couplelife.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用响应结果封装类。
 * <p>
 * 所有 Controller 接口统一返回此结构，便于前端做统一的状态码处理与错误提示。
 * 约定的 code 含义：
 * <ul>
 *     <li>200 - 业务成功</li>
 *     <li>401 - 未登录或登录态失效</li>
 *     <li>500 - 业务异常或系统异常</li>
 * </ul>
 *
 * @param <T> 业务数据载体类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    /** 状态码：200 成功，401 未登录，500 失败 */
    private Integer code;
    /** 提示信息，前端可直接展示 */
    private String message;
    /** 业务数据，失败时通常为 null */
    private T data;

    /**
     * 成功响应（默认提示语 "success"）。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 成功响应（自定义提示语）。
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败响应（500），用于业务异常。
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 未登录响应（401），由鉴权过滤器或全局拦截器使用。
     */
    public static <T> Result<T> unauthorized() {
        return new Result<>(401, "请先登录", null);
    }
}
