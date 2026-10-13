package com.aitravel.vo;

import com.aitravel.entity.Trip;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 行程详情视图对象（详细设计 §5.3.3）。
 *
 * <p>含 {@code dayList}，按 {@code day_no}、{@code visit_order} 升序装配。
 */
@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "Lombok @Data 生成的 preferences/dayList 存取器；列表由 Service 层一次性"
                + "组装后交由 Jackson 序列化，外部不持有可变引用")
public class TripDetailVO {

    /** 行程主键。 */
    private Long id;

    /** 行程标题。 */
    private String title;

    /** 目的地城市。 */
    private String city;

    /** 旅行天数。 */
    private Integer days;

    /** 预算范围。 */
    private String budget;

    /** 消费模式。 */
    private String spendMode;

    /** 兴趣偏好数组。 */
    private List<String> preferences;

    /** 状态：0=草稿，1=已完成。 */
    private Integer status;

    /** 创建时间。 */
    private LocalDateTime createAt;

    /** 逐日安排，按 {@code day_no} 升序。 */
    private List<TripDayVO> dayList = new ArrayList<>();

    /**
     * 由实体构造详情视图对象（{@code dayList} 初始为空列表，由调用方装配）。
     *
     * @param trip 行程实体，可为 {@code null}
     * @return 详情视图对象；入参为 {@code null} 时返回 {@code null}
     */
    public static TripDetailVO from(Trip trip) {
        if (trip == null) {
            return null;
        }
        TripDetailVO vo = new TripDetailVO();
        vo.setId(trip.getId());
        vo.setTitle(trip.getTitle());
        vo.setCity(trip.getCity());
        vo.setDays(trip.getDays());
        vo.setBudget(trip.getBudget());
        vo.setSpendMode(trip.getSpendMode());
        vo.setPreferences(TripVO.splitPreferences(trip.getPreferences()));
        vo.setStatus(trip.getStatus());
        vo.setCreateAt(trip.getCreateAt());
        return vo;
    }
}
