package com.aitravel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 行程主表 {@code trip} 实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trip")
public class Trip extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 归属用户 ID。 */
    private Long userId;

    /** 行程标题。 */
    private String title;

    /** 目的地城市。 */
    private String city;

    /** 旅行天数。 */
    private Integer days;

    /** 预算范围。 */
    private String budget;

    /** 消费模式：经济/舒适/高端。 */
    private String spendMode;

    /** 兴趣偏好，逗号分隔。 */
    private String preferences;

    /** 状态：0=草稿，1=已完成。 */
    private Integer status;
}
