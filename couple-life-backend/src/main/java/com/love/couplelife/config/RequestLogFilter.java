package com.love.couplelife.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求级日志：为每个请求注入 traceId（MDC），并记录 method、uri、状态码、耗时。
 * 优先级高于 JwtAuthFilter，保证鉴权日志也能携带 traceId。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);
    private static final String TRACE_ID = "traceId";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(TRACE_ID, traceId);
        response.setHeader(HEADER_TRACE_ID, traceId);

        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String fullUri = query == null ? uri : uri + "?" + query;

        try {
            log.info("--> {} {} from {}", method, fullUri, request.getRemoteAddr());
            filterChain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            int status = response.getStatus();
            if (status >= 500) {
                log.error("<-- {} {} {} {}ms", method, fullUri, status, cost);
            } else if (status >= 400) {
                log.warn("<-- {} {} {} {}ms", method, fullUri, status, cost);
            } else {
                log.info("<-- {} {} {} {}ms", method, fullUri, status, cost);
            }
            MDC.remove(TRACE_ID);
        }
    }
}
