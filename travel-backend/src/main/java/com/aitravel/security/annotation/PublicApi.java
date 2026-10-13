package com.aitravel.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 完全公开接口标记。标注的类或方法无需任何身份即可访问，鉴权直接放行。
 *
 * <p>判定优先级最高：{@code @PublicApi > @OptionalAuthApi > @RequireAdmin > 默认（需登录）}。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface PublicApi {
}
