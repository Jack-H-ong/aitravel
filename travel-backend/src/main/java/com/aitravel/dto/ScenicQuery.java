package com.aitravel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 景点列表查询参数（详细设计 §4.3.1）。用于 {@code GET /api/scenic/list} 的查询串绑定。
 */
@Data
public class ScenicQuery {

    /** 城市（精确匹配），可选。 */
    @Size(max = 50)
    private String city;

    /** 分类（精确匹配），可选。 */
    private String category;

    /** 关键字（名称或简介模糊匹配），可选。 */
    @Size(max = 50)
    private String keyword;

    /** 页码，从 1 开始，默认 1。 */
    @Min(1)
    private Integer page = 1;

    /** 每页大小，1~100，默认 10。 */
    @Min(1)
    @Max(100)
    private Integer size = 10;
}
