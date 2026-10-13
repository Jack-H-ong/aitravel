package com.aitravel.vo;

import com.aitravel.entity.ScenicSpot;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 浏览历史条目视图对象（详细设计 §4.3 / §7）。 */
@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "Lombok @Data 生成的 images 存取器；列表由 Service 层组装后交由 Jackson "
                + "序列化，外部不持有可变引用")
public class BrowseHistoryVO {

    /** 景点主键。 */
    private Long scenicId;

    /** 景点名称。 */
    private String name;

    /** 所在城市。 */
    private String city;

    /** 分类。 */
    private String category;

    /** 图片数组。 */
    private List<String> images;

    /** 平均评分。 */
    private BigDecimal avgRating;

    /** 最近浏览时间。 */
    private LocalDateTime browseTime;

    /**
     * 由景点实体与浏览时间构造视图对象。
     *
     * @param spot       景点实体，可为 {@code null}
     * @param browseTime 浏览时间
     * @return 浏览历史条目；入参 {@code spot} 为 {@code null} 时返回 {@code null}
     */
    public static BrowseHistoryVO from(ScenicSpot spot, LocalDateTime browseTime) {
        if (spot == null) {
            return null;
        }
        BrowseHistoryVO vo = new BrowseHistoryVO();
        vo.setScenicId(spot.getId());
        vo.setName(spot.getName());
        vo.setCity(spot.getCity());
        vo.setCategory(spot.getCategory());
        vo.setImages(ScenicVO.split(spot.getImages()));
        vo.setAvgRating(spot.getAvgRating());
        vo.setBrowseTime(browseTime);
        return vo;
    }
}
