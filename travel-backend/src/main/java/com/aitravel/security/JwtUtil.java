package com.aitravel.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

/**
 * JWT 签发与解析工具（HS256）。
 *
 * <p>密钥与有效期通过构造参数注入，便于单元测试使用测试密钥构造。生产环境由 {@code WebMvcConfig}
 * 从 {@code jwt.secret} / {@code jwt.expiration} 注入。密钥长度必须满足 HS256 要求（≥ 256 bit）。
 */
public final class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    /**
     * 使用密钥与有效期构造。
     *
     * @param secret       签发/校验密钥，UTF-8 编码后长度须 ≥ 32 字节
     * @param expirationMs Token 有效期（毫秒）
     * @throws io.jsonwebtoken.security.WeakKeyException 密钥长度不足 HS256 要求时抛出
     */
    public JwtUtil(String secret, long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 签发 Token。Claims 含 {@code userId} / {@code role}，subject 为 {@code username}，
     * {@code exp = now + expirationMs}。
     *
     * @param userId   用户主键
     * @param username 用户名
     * @param role     角色：user / admin
     * @return 紧凑序列化后的 JWT 字符串
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 Token 并构造登录身份。签名非法或 Token 过期时由 jjwt 抛出异常。
     *
     * @param token JWT 字符串
     * @return 解析得到的登录用户
     * @throws JwtException Token 非法（签名不匹配 / 过期 / 结构损坏）时抛出
     */
    public LoginUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Number userId = claims.get("userId", Number.class);
        String role = claims.get("role", String.class);
        return new LoginUser(userId == null ? null : userId.longValue(), claims.getSubject(), role);
    }

    /**
     * 轻量校验 Token 是否合法（签名正确且未过期）。解析异常一律返回 {@code false}。
     *
     * @param token JWT 字符串
     * @return 合法返回 {@code true}，否则 {@code false}
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
