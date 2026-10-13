package com.aitravel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 注册请求（详细设计 §3.3.1）。 */
@Data
public class RegisterRequest {

    /** 用户名，长度 3-20。 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需为 3-20 位")
    private String username;

    /** 密码，长度 6-20。 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    private String password;

    /** 确认密码。 */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
