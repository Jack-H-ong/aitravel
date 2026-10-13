package com.aitravel.security;

import com.aitravel.common.BusinessException;
import com.aitravel.common.ErrorCode;
import com.aitravel.security.annotation.OptionalAuthApi;
import com.aitravel.security.annotation.PublicApi;
import com.aitravel.security.annotation.RequireAdmin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权裁决拦截器。依据业务方法（或所在类）上的注解决定放行或抛异常。
 *
 * <p>判定优先级：{@code @PublicApi} &gt; {@code @OptionalAuthApi} &gt; {@code @RequireAdmin} &gt;
 * 默认（需登录）。方法级注解优先于类级注解。非 {@link HandlerMethod}（静态资源、错误页）直接放行。
 */
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (hasMethodOrClassAnnotation(handlerMethod, PublicApi.class)) {
            return true;
        }

        LoginUser user = UserContext.get();

        if (hasMethodOrClassAnnotation(handlerMethod, OptionalAuthApi.class)) {
            return true;
        }
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (hasMethodOrClassAnnotation(handlerMethod, RequireAdmin.class) && !user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return true;
    }

    /**
     * 判定目标方法或其所在类是否带有指定注解，方法级优先。
     *
     * @param handlerMethod  处理方法
     * @param annotationType 注解类型
     * @return 方法或类上存在该注解时返回 {@code true}
     */
    private boolean hasMethodOrClassAnnotation(HandlerMethod handlerMethod,
            Class<? extends Annotation> annotationType) {
        if (handlerMethod.getMethodAnnotation(annotationType) != null) {
            return true;
        }
        return handlerMethod.getBeanType().getAnnotation(annotationType) != null;
    }
}
