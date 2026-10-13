package com.aitravel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求（详细设计 §3.3.2）。 */
@Data
public class LoginRequest {

    /** 用户名。 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码。 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
