package com.aitravel.vo;

import lombok.Data;

/** 收藏切换结果（详细设计 §4.3.3）。 */
@Data
public class FavoriteVO {

    /** 动作：{@code favorite}（收藏）或 {@code unfavorite}（取消收藏）。 */
    private String action;

    /** 切换后的收藏态。 */
    private Boolean isFavorite;

    /** Jackson 反序列化用无参构造。 */
    public FavoriteVO() {
    }

    /**
     * 全量构造。
     *
     * @param action     动作
     * @param isFavorite 切换后的收藏态
     */
    public FavoriteVO(String action, Boolean isFavorite) {
        this.action = action;
        this.isFavorite = isFavorite;
    }
}
