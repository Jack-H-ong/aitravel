package com.aitravel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 收藏表 {@code favorite} 实体（仅 {@code create_at}）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("favorite")
public class Favorite extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 归属用户 ID。 */
    private Long userId;

    /** 收藏的景点 ID。 */
    private Long scenicId;
}
