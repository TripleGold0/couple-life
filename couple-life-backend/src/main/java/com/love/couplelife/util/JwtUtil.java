package com.love.couplelife.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * JWT 工具类：负责生成与解析 token。
 * <p>
 * 配置项：
 * <ul>
 *     <li>{@code app.jwt.secret} —— HMAC 密钥（建议 32 字节以上）</li>
 *     <li>{@code app.jwt.expire-hours} —— token 过期时间（小时）</li>
 * </ul>
 * subject 中存放的是用户 ID，业务侧只需要这一个标识即可，不下发其它敏感信息。
 */
@Component
public class JwtUtil {
    private final SecretKey secretKey;
    private final long expireHours;

    public JwtUtil(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expire-hours}") long expireHours) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireHours = expireHours;
    }

    /**
     * 生成 JWT。
     *
     * @param userId 当前登录用户 ID，会写入 subject
     * @return 紧凑格式的 JWT 字符串
     */
    public String generateToken(Long userId) {
        Date expireAt = Date.from(LocalDateTime.now().plusHours(expireHours).atZone(ZoneId.systemDefault()).toInstant());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .expiration(expireAt)
                .issuedAt(new Date())
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 JWT 并返回 userId。token 非法 / 过期 / 篡改时抛出 JwtException。
     */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }
}
