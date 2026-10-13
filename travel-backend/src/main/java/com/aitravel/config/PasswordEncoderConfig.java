package com.aitravel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器装配（详细设计 §1.3.6）。
 *
 * <p>仅使用 {@code spring-security-crypto} 工具包提供 BCrypt 实现，<b>不引入</b>
 * {@code spring-boot-starter-security}，因此不会触发 Spring Security 自动配置与默认过滤器链。
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 注册 BCrypt 密码编码器。
     *
     * @return BCrypt 实现的 {@link PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
