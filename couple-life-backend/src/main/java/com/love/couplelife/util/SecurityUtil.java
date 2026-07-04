package com.love.couplelife.util;

import com.love.couplelife.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 登录态工具类。
 * <p>从 SecurityContext 中取出由 {@link com.love.couplelife.config.JwtAuthFilter} 写入的当前登录用户 ID。
 * 适用于 Service 层无法注入 Request 的场景。</p>
 */
public class SecurityUtil {
    private SecurityUtil() {
        // 工具类不允许实例化
    }

    /**
     * @return 当前登录用户 ID
     * @throws BizException 未登录或登录态异常
     */
    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new BizException("请先登录");
        }
        return userId;
    }
}
