package com.aitravel.vo;

import com.aitravel.entity.ScenicSpot;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

/**
 * 景点视图对象（详细设计 §4.3.1）。
 *
 * <p>库中 {@code tags}/{@code images} 以逗号分隔字符串存储，此处转为数组输出；未登录时
 * {@code isFavorite=false}（D-04）。
 */
@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "Lombok @Data 生成的 tags/images 存取器；列表由 Service 层一次性组装后"
                + "交由 Jackson 序列化，外部不持有可变引用")
public class ScenicVO {

    /** 景点主键。 */
    private Long id;

    /** 景点名称。 */
    private String name;

    /** 所在城市。 */
    private String city;

    /** 详细地址。 */
    private String address;

    /** 分类。 */
    private String category;

    /** 门票价格。 */
    private BigDecimal price;

    /** 开放时间。 */
    private String openTime;

    /** 景点介绍。 */
    private String description;

    /** 标签数组。 */
    private List<String> tags;

    /** 图片数组。 */
    private List<String> images;

    /** 平均评分。 */
    private BigDecimal avgRating;

    /** 收藏数。 */
    private Integer favoriteCount;

    /** 浏览量。 */
    private Integer viewCount;

    /** 当前用户是否已收藏。 */
    private Boolean isFavorite;

    /**
     * 由实体构造视图对象，收藏态默认为 {@code false}。
     *
     * @param spot 景点实体，可为 {@code null}
     * @return 景点视图对象；入参为 {@code null} 时返回 {@code null}
     */
    public static ScenicVO from(ScenicSpot spot) {
        return from(spot, false);
    }

    /**
     * 由实体构造视图对象。
     *
     * @param spot       景点实体，可为 {@code null}
     * @param isFavorite 当前用户是否已收藏
     * @return 景点视图对象；入参为 {@code null} 时返回 {@code null}
     */
    public static ScenicVO from(ScenicSpot spot, boolean isFavorite) {
        if (spot == null) {
            return null;
        }
        ScenicVO vo = new ScenicVO();
        populate(vo, spot, isFavorite);
        return vo;
    }

    /**
     * 将实体字段复制到视图对象（供子类复用）。
     *
     * @param vo         目标视图对象
     * @param spot       景点实体
     * @param isFavorite 收藏态
     */
    static void populate(ScenicVO vo, ScenicSpot spot, boolean isFavorite) {
        vo.setId(spot.getId());
        vo.setName(spot.getName());
        vo.setCity(spot.getCity());
        vo.setAddress(spot.getAddress());
        vo.setCategory(spot.getCategory());
        vo.setPrice(spot.getPrice());
        vo.setOpenTime(spot.getOpenTime());
        vo.setDescription(spot.getDescription());
        vo.setTags(split(spot.getTags()));
        vo.setImages(split(spot.getImages()));
        vo.setAvgRating(spot.getAvgRating());
        vo.setFavoriteCount(spot.getFavoriteCount());
        vo.setViewCount(spot.getViewCount());
        vo.setIsFavorite(isFavorite);
    }

    /**
     * 将逗号分隔字符串拆分为列表；{@code null}/空串返回空列表，空元素被忽略。
     *
     * @param csv 逗号分隔字符串，可为 {@code null}
     * @return 非 {@code null} 的字符串列表
     */
    static List<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
