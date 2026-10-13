package com.aitravel.mapper;

import com.aitravel.entity.ScenicSpot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** {@code scenic_spot} 表数据访问，含浏览量/收藏数的原子自增自减。 */
@Mapper
public interface ScenicSpotMapper extends BaseMapper<ScenicSpot> {

    /**
     * 浏览量 +1（原子操作，仅作用于未删除行）。
     *
     * @param id 景点 ID
     * @return 受影响行数（1=成功，0=记录不存在或已删除）
     */
    @Update("UPDATE scenic_spot SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 收藏数 +1（原子操作，仅作用于未删除行）。
     *
     * @param id 景点 ID
     * @return 受影响行数
     */
    @Update("UPDATE scenic_spot SET favorite_count = favorite_count + 1 "
            + "WHERE id = #{id} AND deleted = 0")
    int incrementFavoriteCount(@Param("id") Long id);

    /**
     * 收藏数 -1（原子操作，仅作用于未删除行）。
     *
     * @param id 景点 ID
     * @return 受影响行数
     */
    @Update("UPDATE scenic_spot SET favorite_count = favorite_count - 1 "
            + "WHERE id = #{id} AND deleted = 0")
    int decrementFavoriteCount(@Param("id") Long id);
}
