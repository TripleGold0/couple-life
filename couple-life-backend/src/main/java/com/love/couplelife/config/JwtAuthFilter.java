package com.love.couplelife.config;

import com.love.couplelife.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 鉴权过滤器。
 * <p>
 * 解析请求头 {@code Authorization: Bearer <token>}，校验通过后将 userId 写入
 * Spring Security 上下文，便于后续在 Controller / Service 中通过
 * {@link com.love.couplelife.util.SecurityUtil} 取出当前登录用户。
 * <p>
 * 解析失败不抛异常，仅清空上下文，由后续的鉴权规则决定是否拒绝访问，
 * 这样可以让公开接口（如 /api/auth/**）即使带了非法 token 也能继续访问。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                // 截掉 "Bearer " 前缀，剩余部分即 JWT
                String token = authorization.substring(7);
                Long userId = jwtUtil.parseUserId(token);
                // principal 直接放 userId，避免再查 DB；权限列表暂未启用，传空集合
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                if (log.isDebugEnabled()) {
                    log.debug("JWT 鉴权通过, userId={}, uri={}", userId, request.getRequestURI());
                }
            } catch (Exception ex) {
                // token 过期或被篡改：清空上下文，让后续 SecurityFilter 走未登录分支
                log.warn("JWT 解析失败 uri={}, reason={}", request.getRequestURI(), ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
