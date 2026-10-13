package com.aitravel.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 后台行程视图对象（详细设计 §5.1，供 M-ADMIN 行程管理）。
 *
 * <p>由 {@code TripMapper} 自定义 JOIN 查询直接映射：{@code user_id}、{@code create_at} 经
 * {@code map-underscore-to-camel-case} 映射为驼峰字段。
 */
@Data
public class AdminTripVO {

    /** 行程主键。 */
    private Long id;

    /** 归属用户 ID。 */
    private Long userId;

    /** 归属用户名。 */
    private String username;

    /** 行程标题。 */
    private String title;

    /** 目的地城市。 */
    private String city;

    /** 旅行天数。 */
    private Integer days;

    /** 创建时间。 */
    private LocalDateTime createAt;
}
