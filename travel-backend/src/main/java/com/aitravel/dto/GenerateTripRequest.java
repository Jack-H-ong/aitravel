package com.aitravel.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 生成攻略请求（详细设计 §5.3.1）。 */
@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "Lombok @Data 生成的 preferences 存取器；请求体列表由 Spring 绑定后仅在本"
                + "模块内拼接为字符串，不对外暴露可变引用")
public class GenerateTripRequest {

    /** 目的地城市。 */
    @NotBlank(message = "城市不能为空")
    @Size(max = 50)
    private String city;

    /** 旅行天数，1~7。 */
    @NotNull(message = "天数不能为空")
    @Min(1)
    @Max(7)
    private Integer days;

    /** 预算范围。 */
    @Size(max = 50)
    private String budget;

    /** 兴趣偏好标签。 */
    private List<String> preferences;

    /** 消费模式：经济/舒适/高端。 */
    private String spendMode;
}
