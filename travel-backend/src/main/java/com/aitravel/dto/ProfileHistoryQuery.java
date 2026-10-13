package com.aitravel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 浏览历史分页查询参数（详细设计 §7.4）。用于 {@code GET /api/profile/history} 的查询串绑定。
 */
@Data
public class ProfileHistoryQuery {

    /** 页码，从 1 开始，默认 1。 */
    @Min(1)
    private Integer page = 1;

    /** 每页大小，1~100，默认 10。 */
    @Min(1)
    @Max(100)
    private Integer size = 10;
}
