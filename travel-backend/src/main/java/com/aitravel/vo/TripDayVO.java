package com.aitravel.vo;

import com.aitravel.entity.TripDay;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 行程单日视图对象（详细设计 §5.3.3）。 */
@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "Lombok @Data 生成的 spots 存取器；列表由 Service 层一次性组装后交由 Jackson "
                + "序列化，外部不持有可变引用")
public class TripDayVO {

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

    /** 当日景点，按访问顺序升序。 */
    private List<TripSpotVO> spots = new ArrayList<>();

    /**
     * 由实体构造单日视图对象，{@code spots} 初始为空列表，由调用方按序装配。
     *
     * @param day 单日实体，可为 {@code null}
     * @return 单日视图对象；入参为 {@code null} 时返回 {@code null}
     */
    public static TripDayVO from(TripDay day) {
        if (day == null) {
            return null;
        }
        TripDayVO vo = new TripDayVO();
        vo.setDayNo(day.getDayNo());
        vo.setTheme(day.getTheme());
        vo.setLunch(day.getLunch());
        vo.setDinner(day.getDinner());
        vo.setTip(day.getTip());
        return vo;
    }
}
