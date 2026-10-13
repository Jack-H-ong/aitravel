package com.aitravel.entity;

import com.aitravel.service.ai.TripPlanDay;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 每日行程表 {@code trip_day} 实体。
 *
 * <p>该表有 {@code create_at} 与 {@code update_at}，但无 {@code deleted}，故继承 {@link BaseEntity}
 * （仅映射 {@code createAt}）。{@code update_at} 由数据库 {@code ON UPDATE CURRENT_TIMESTAMP}
 * 维护；行程子表采用「物理删+重建」，不做 in-place 更新，因此不在实体映射 {@code updateAt}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trip_day")
public class TripDay extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 归属行程 ID。 */
    private Long tripId;

    /** 第几天。 */
    private Integer dayNo;

    /** 当日主题。 */
    private String theme;

    /** 午餐推荐。 */
    private String lunch;

    /** 晚餐推荐。 */
    private String dinner;

    /** 当日提示。 */
    private String tip;

    /**
     * 由 AI 生成结果构造单日实体（详细设计 §5.5）。
     *
     * @param tripId 归属行程 ID
     * @param day    AI 生成的单日安排
     * @return 待落库的单日实体
     */
    public static TripDay from(Long tripId, TripPlanDay day) {
        TripDay entity = new TripDay();
        entity.setTripId(tripId);
        entity.setDayNo(day.day());
        entity.setTheme(day.theme());
        entity.setLunch(day.lunch());
        entity.setDinner(day.dinner());
        entity.setTip(day.tip());
        return entity;
    }
}
