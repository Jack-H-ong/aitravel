package com.aitravel.vo;

import com.aitravel.entity.Trip;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

/**
 * 行程列表视图对象（详细设计 §5.3.2）。
 *
 * <p>列表页轻量化：<b>不含</b> {@code dayList}；{@code preferences} 由库中逗号分隔字符串拆分为数组。
 */
@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "Lombok @Data 生成的 preferences 存取器；列表由 Service 层一次性组装后交由"
                + " Jackson 序列化，外部不持有可变引用")
public class TripVO {

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

    /**
     * 由实体构造列表视图对象。
     *
     * @param trip 行程实体，可为 {@code null}
     * @return 列表视图对象；入参为 {@code null} 时返回 {@code null}
     */
    public static TripVO from(Trip trip) {
        if (trip == null) {
            return null;
        }
        TripVO vo = new TripVO();
        vo.setId(trip.getId());
        vo.setTitle(trip.getTitle());
        vo.setCity(trip.getCity());
        vo.setDays(trip.getDays());
        vo.setBudget(trip.getBudget());
        vo.setSpendMode(trip.getSpendMode());
        vo.setPreferences(splitPreferences(trip.getPreferences()));
        vo.setStatus(trip.getStatus());
        vo.setCreateAt(trip.getCreateAt());
        return vo;
    }

    /**
     * 将逗号分隔的偏好字符串拆分为列表；{@code null}/空串返回空列表，空元素被忽略。
     *
     * @param csv 逗号分隔字符串，可为 {@code null}
     * @return 非 {@code null} 的字符串列表
     */
    static List<String> splitPreferences(String csv) {
        if (csv == null || csv.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
