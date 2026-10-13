package com.aitravel.vo;

import com.aitravel.entity.ScenicSpot;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 景点详情视图对象（详细设计 §4.3.2）。字段与 {@link ScenicVO} 一致，{@code viewCount}
 * 为自增后的值。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScenicDetailVO extends ScenicVO {

    /**
     * 由实体构造详情视图对象。
     *
     * @param spot       景点实体，可为 {@code null}
     * @param isFavorite 当前用户是否已收藏
     * @return 详情视图对象；入参为 {@code null} 时返回 {@code null}
     */
    public static ScenicDetailVO from(ScenicSpot spot, boolean isFavorite) {
        if (spot == null) {
            return null;
        }
        ScenicDetailVO vo = new ScenicDetailVO();
        ScenicVO.populate(vo, spot, isFavorite);
        return vo;
    }
}
