package com.aitravel.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理员接口标记。要求已登录且角色为 {@code admin}，否则返回 401 / 403。
 *
 * <p>约定：标注 {@code @RequireAdmin} 的接口不应同时标注 {@code @PublicApi} / {@code @OptionalAuthApi}。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequireAdmin {
}
