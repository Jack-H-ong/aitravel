package com.aitravel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户启用/禁用请求（供 M-ADMIN，§8.3）：请求体 {@code { "status": 0 | 1 }}。
 *
 * <p>{@code status} 的取值约束（仅 0/1）由 {@code AdminService} 在服务层校验。
 */
@Data
public class UpdateStatusRequest {

    /** 状态：0=禁用，1=正常。 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
