package com.aitravel.vo;

import com.aitravel.entity.TripSpot;
import lombok.Data;

/** 行程景点视图对象（详细设计 §5.3.3）。 */
@Data
public class TripSpotVO {

    /** 关联景点 ID，AI 生成的可能为 {@code null}。 */
    private Long scenicId;

    /** 景点名称。 */
    private String spotName;

    /** 访问顺序。 */
    private Integer visitOrder;

    /** 建议停留（分钟）。 */
    private Integer stayMinutes;

    /** 游玩提示。 */
    private String tip;

    /**
     * 由实体构造景点视图对象。
     *
     * @param spot 行程景点实体，可为 {@code null}
     * @return 景点视图对象；入参为 {@code null} 时返回 {@code null}
     */
    public static TripSpotVO from(TripSpot spot) {
        if (spot == null) {
            return null;
        }
        TripSpotVO vo = new TripSpotVO();
        vo.setScenicId(spot.getScenicId());
        vo.setSpotName(spot.getSpotName());
        vo.setVisitOrder(spot.getVisitOrder());
        vo.setStayMinutes(spot.getStayMinutes());
        vo.setTip(spot.getTip());
        return vo;
    }
}
