package com.aitravel.entity;

import com.aitravel.service.ai.TripPlanSpot;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 行程-景点关联表 {@code trip_spot} 实体（仅 {@code create_at}）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trip_spot")
public class TripSpot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 归属行程 ID。 */
    private Long tripId;

    /** 关联景点 ID（AI 生成的可能为 null）。 */
    private Long scenicId;

    /** 第几天。 */
    private Integer dayNo;

    /** 景点名称。 */
    private String spotName;

    /** 访问顺序。 */
    private Integer visitOrder;

    /** 建议停留（分钟）。 */
    private Integer stayMinutes;

    /** 游玩提示。 */
    private String tip;

    /**
     * 由 AI 生成结果构造行程景点实体（详细设计 §5.5）。
     *
     * @param tripId    归属行程 ID
     * @param dayNo     第几天
     * @param spot      AI 生成的景点安排
     * @param scenicId  按景点名匹配到的景点 ID，匹配不到为 {@code null}
     * @return 待落库的行程景点实体
     */
    public static TripSpot from(Long tripId, int dayNo, TripPlanSpot spot, Long scenicId) {
        TripSpot entity = new TripSpot();
        entity.setTripId(tripId);
        entity.setScenicId(scenicId);
        entity.setDayNo(dayNo);
        entity.setSpotName(spot.name());
        entity.setVisitOrder(spot.order());
        entity.setStayMinutes(spot.stayMinutes());
        entity.setTip(spot.tip());
        return entity;
    }
}
