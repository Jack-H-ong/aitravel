package com.aitravel.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 修改行程请求（详细设计 §5.3.4）。 */
@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "Lombok @Data 生成的 dayList 存取器；请求体列表由 Spring 绑定后仅在本模块内"
                + "读取，不对外暴露可变引用")
public class UpdateTripRequest {

    /** 行程标题，可选。 */
    @Size(max = 100)
    private String title;

    /** 逐日安排，可选。 */
    @Valid
    private List<UpdateTripDay> dayList;

    /** 单日安排。 */
    @Data
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
            justification = "Lombok @Data 生成的 spots 存取器；绑定后仅在本模块内读取")
    public static class UpdateTripDay {

        /** 第几天。 */
        @Min(1)
        private Integer dayNo;

        /** 当日主题。 */
        @Size(max = 100)
        private String theme;

        /** 午餐推荐。 */
        @Size(max = 255)
        private String lunch;

        /** 晚餐推荐。 */
        @Size(max = 255)
        private String dinner;

        /** 当日提示。 */
        @Size(max = 500)
        private String tip;

        /** 当日景点。 */
        @Valid
        private List<UpdateTripSpot> spots;
    }

    /** 单日景点安排。 */
    @Data
    public static class UpdateTripSpot {

        /** 关联景点 ID，可选（AI 生成的可能无对应景点）。 */
        private Long scenicId;

        /** 景点名称。 */
        @NotBlank(message = "景点名称不能为空")
        @Size(max = 100)
        private String spotName;

        /** 访问顺序。 */
        @Min(1)
        private Integer visitOrder;

        /** 建议停留（分钟），缺省为 60。 */
        private Integer stayMinutes;

        /** 游玩提示。 */
        @Size(max = 500)
        private String tip;
    }
}
