package com.love.couplelife.common;

/**
 * 业务异常。
 * <p>
 * 用于业务逻辑层主动抛出的、可被前端直接展示给用户的错误提示。
 * 由 {@link GlobalExceptionHandler} 统一捕获并转换为 {@link Result#fail(String)}。
 * <p>
 * 仅用于"预期内"的业务校验失败（如参数错误、状态不合法等），系统级异常请直接抛 RuntimeException。
 */
public class BizException extends RuntimeException {

    /**
     * @param message 用户可见的错误提示
     */
    public BizException(String message) {
        super(message);
    }
}
