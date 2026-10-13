package com.aitravel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 景点信息表 {@code scenic_spot} 实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scenic_spot")
public class ScenicSpot extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 景点名称。 */
    private String name;

    /** 所在城市。 */
    private String city;

    /** 详细地址。 */
    private String address;

    /** 分类：自然/人文/主题乐园/其他。 */
    private String category;

    /** 门票价格（0=免费）。 */
    private BigDecimal price;

    /** 开放时间。 */
    private String openTime;

    /** 景点介绍。 */
    private String description;

    /** 标签，逗号分隔。 */
    private String tags;

    /** 图片 URL，逗号分隔。 */
    private String images;

    /** 平均评分。 */
    private BigDecimal avgRating;

    /** 收藏数（收藏/取消时同步维护）。 */
    private Integer favoriteCount;

    /** 浏览量。 */
    private Integer viewCount;
}
