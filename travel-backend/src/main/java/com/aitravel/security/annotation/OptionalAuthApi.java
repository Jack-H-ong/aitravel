package com.aitravel.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 可选登录接口标记。接口公开放行，登录态可选：携带合法 Token 时注入身份，否则身份为空（D-04）。
 *
 * <p>判定优先级：{@code @PublicApi > @OptionalAuthApi > @RequireAdmin > 默认（需登录）}。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface OptionalAuthApi {
}
