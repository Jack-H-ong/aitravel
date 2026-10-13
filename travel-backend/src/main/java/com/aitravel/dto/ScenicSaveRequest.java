package com.aitravel.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/** 景点新增/编辑请求（供 M-ADMIN，§8.3）。 */
@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "Lombok @Data 生成的 tags/images 存取器；请求体列表由 Spring 绑定后仅在本"
                + "模块内拼接为字符串，不对外暴露可变引用")
public class ScenicSaveRequest {

    /** 景点名称。 */
    @NotBlank(message = "景点名称不能为空")
    @Size(max = 100)
    private String name;

    /** 所在城市。 */
    @NotBlank(message = "城市不能为空")
    @Size(max = 50)
    private String city;

    /** 详细地址。 */
    @Size(max = 255)
    private String address;

    /** 分类。 */
    @Size(max = 50)
    private String category;

    /** 门票价格（0=免费）。 */
    @DecimalMin(value = "0", message = "价格不能为负")
    private BigDecimal price;

    /** 开放时间。 */
    @Size(max = 100)
    private String openTime;

    /** 景点介绍。 */
    private String description;

    /** 标签列表，入库时拼接为逗号分隔字符串。 */
    private List<String> tags;

    /** 图片 URL 列表，入库时拼接为逗号分隔字符串。 */
    private List<String> images;
}
