package com.aitravel.config;

import com.aitravel.security.AuthInterceptor;
import com.aitravel.security.JwtAuthFilter;
import com.aitravel.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 装配：注册 JWT 过滤器与鉴权拦截器。
 *
 * <p>装配顺序固定为 <b>Filter → Interceptor → Controller</b>：过滤器只解析身份写入
 * {@code UserContext}，拦截器在 HandlerMapping 解析出 HandlerMethod 后依据注解裁决。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String jwtSecret;
    private final long jwtExpiration;

    /**
     * 注入 JWT 配置。
     *
     * @param jwtSecret    签名密钥（{@code jwt.secret}）
     * @param jwtExpiration 有效期毫秒（{@code jwt.expiration}）
     */
    public WebMvcConfig(@Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long jwtExpiration) {
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
    }

    /**
     * JWT 工具 Bean。
     *
     * @return 由配置密钥与有效期构造的 {@link JwtUtil}
     */
    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(jwtSecret, jwtExpiration);
    }

    /**
     * JWT 身份解析过滤器 Bean。
     *
     * @param jwtUtil JWT 工具
     * @return 过滤器实例
     */
    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtUtil jwtUtil) {
        return new JwtAuthFilter(jwtUtil);
    }

    /**
     * 注册 JWT 过滤器，order 取较小值以保证其在业务过滤器之前执行。
     *
     * @param jwtAuthFilter 过滤器
     * @return 过滤器注册 Bean
     */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter jwtAuthFilter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(jwtAuthFilter);
        registration.setName("jwtAuthFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor()).addPathPatterns("/api/**");
    }
}
