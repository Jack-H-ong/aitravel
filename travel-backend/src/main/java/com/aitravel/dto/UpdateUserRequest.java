package com.aitravel.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户信息请求（详细设计 §3.3.4）。
 *
 * <p>所有字段均可选：仅非空字段参与更新。当 {@code newPassword} 非空时业务层要求
 * {@code oldPassword} 非空并校验原密码。
 */
@Data
public class UpdateUserRequest {

    /** 昵称，可选，长度不超过 50。 */
    @Size(max = 50, message = "昵称长度不能超过 50 位")
    private String nickname;

    /** 头像 URL，可选，长度不超过 255。 */
    @Size(max = 255, message = "头像地址长度不能超过 255 位")
    private String avatar;

    /** 原密码，可选；当 {@code newPassword} 非空时必填。 */
    private String oldPassword;

    /** 新密码，可选，长度 6-20。 */
    @Size(min = 6, max = 20, message = "新密码长度需为 6-20 位")
    private String newPassword;
}
