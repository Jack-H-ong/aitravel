package com.aitravel.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 身份解析过滤器。只解析、不裁决：从 {@code Authorization: Bearer xxx} 提取 Token，
 * 合法则写入 {@link UserContext}；无 Token 或 Token 无效时不抛异常、不做 401/403 决策，
 * 交由 {@link AuthInterceptor} 依据 HandlerMethod 注解裁决。
 *
 * <p>请求无论成功或异常，均在 {@code finally} 中 {@link UserContext#clear()}，避免线程池复用时身份串号。
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    /**
     * 构造过滤器。
     *
     * @param jwtUtil JWT 工具
     */
    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveBearer(request);
            if (StringUtils.hasText(token) && jwtUtil.isValid(token)) {
                UserContext.set(jwtUtil.parseToken(token));
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 从 {@code Authorization} 头解析 Bearer Token。
     *
     * @param request 当前请求
     * @return 去掉 {@code "Bearer "} 前缀的 Token，缺失或格式不符时返回 {@code null}
     */
    private String resolveBearer(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
