package com.aitravel.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 已认证用户身份，来源于 JWT Claims，用于填充 {@link UserContext}。
 */
@Data
@AllArgsConstructor
public class LoginUser {

    /** 用户主键。 */
    private Long userId;

    /** 用户名（JWT subject）。 */
    private String username;

    /** 角色：user / admin。 */
    private String role;

    /**
     * 是否为管理员。
     *
     * @return {@code role} 等于 {@code "admin"} 时返回 {@code true}
     */
    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
