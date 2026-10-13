package com.aitravel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户表 {@code user} 实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 用户名（唯一）。 */
    private String username;

    /** 密码（BCrypt 哈希）。 */
    private String password;

    /** 昵称。 */
    private String nickname;

    /** 头像 URL。 */
    private String avatar;

    /** 角色：user/admin。 */
    private String role;

    /** 状态：0=禁用，1=正常。 */
    private Integer status;
}
