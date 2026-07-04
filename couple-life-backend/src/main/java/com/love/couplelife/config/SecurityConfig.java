package com.love.couplelife.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置。
 * <p>
 * 设计目标：纯无状态 JWT 鉴权 + 简单白名单。具体规则：
 * <ul>
 *     <li>所有 OPTIONS 预检请求放行（CORS 需要）</li>
 *     <li>{@code /api/auth/**} 登录注册相关接口放行</li>
 *     <li>{@code /uploads/**} 静态资源放行（已上传图片）</li>
 *     <li>其余接口必须通过 JWT 鉴权</li>
 * </ul>
 * 关闭 CSRF（因为是前后端分离 + JWT，无 Session）。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 安全过滤链：将 {@link JwtAuthFilter} 注入到用户名密码过滤器之前，
     * 这样在 SecurityContext 校验前就能解析 JWT 并写入登录态。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/uploads/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 密码编码器：BCrypt 自带盐，建议保持不变；如修改算法需提供数据迁移方案。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 配置。允许任意来源、携带 Cookie；上线前建议将 OriginPatterns 收敛为业务域名白名单。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
