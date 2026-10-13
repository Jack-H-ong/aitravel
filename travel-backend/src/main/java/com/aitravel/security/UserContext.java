package com.aitravel.security;

/**
 * 当前请求登录用户的线程上下文。
 *
 * <p>由 {@link JwtAuthFilter} 在请求进入时 {@link #set(LoginUser)}、请求结束时 {@link #clear()}
 * （防止线程池复用导致身份串号）。工具类，禁止实例化。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 绑定当前线程的登录用户。
     *
     * @param user 登录用户，可为 {@code null}
     */
    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /**
     * 获取当前线程的登录用户。
     *
     * @return 登录用户，未登录时返回 {@code null}
     */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID，未登录时返回 {@code null}
     */
    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    /**
     * 当前用户是否为管理员。
     *
     * @return 已登录且角色为 {@code admin} 时返回 {@code true}
     */
    public static boolean isAdmin() {
        LoginUser user = HOLDER.get();
        return user != null && "admin".equals(user.getRole());
    }

    /** 清除当前线程的登录用户。 */
    public static void clear() {
        HOLDER.remove();
    }
}
