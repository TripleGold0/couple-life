package com.love.couplelife.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * <p>
 * 统一拦截 Controller 抛出的异常并转换为 {@link Result} 结构返回，避免在每个接口里重复 try/catch。
 * 处理优先级：BizException > 参数校验异常 > 其它兜底异常。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：可控的、面向用户的错误，按 warn 级别记录。
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException exception, HttpServletRequest request) {
        log.warn("业务异常 [{} {}]: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return Result.fail(exception.getMessage());
    }

    /**
     * @RequestBody + @Valid 校验失败时抛出。提取第一条字段错误信息返回。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("参数校验失败");
        log.warn("参数校验失败 [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.fail(message);
    }

    /**
     * 表单参数绑定校验失败（@ModelAttribute / 普通表单提交）。
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("参数校验失败");
        log.warn("参数绑定失败 [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.fail(message);
    }

    /**
     * 兜底异常：未预料到的运行时错误，按 error 级别记录完整堆栈以便排查。
     * 对外仅暴露通用提示，避免泄漏内部细节。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception, HttpServletRequest request) {
        log.error("系统异常 [{} {}]: {}", request.getMethod(), request.getRequestURI(), exception.getMessage(), exception);
        return Result.fail("系统繁忙，请稍后再试");
    }
}
