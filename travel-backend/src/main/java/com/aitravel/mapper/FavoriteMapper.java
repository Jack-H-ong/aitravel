package com.aitravel.mapper;

import com.aitravel.entity.Favorite;
import com.aitravel.entity.ScenicSpot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** {@code favorite} 表数据访问。 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 分页查询某用户收藏的景点，按收藏时间倒序（{@code favorite} JOIN {@code scenic_spot}）。
     *
     * <p>借助 MyBatis-Plus 分页插件自动生成 count 语句；仅返回未逻辑删除的景点。
     *
     * @param page   分页参数（须为方法首参以触发物理分页）
     * @param userId 用户 ID
     * @return 分页的景点实体
     */
    @Select("SELECT s.* FROM favorite f JOIN scenic_spot s ON f.scenic_id = s.id "
            + "WHERE f.user_id = #{userId} AND s.deleted = 0 "
            + "ORDER BY f.create_at DESC")
    IPage<ScenicSpot> selectFavoriteScenicPage(IPage<ScenicSpot> page, @Param("userId") Long userId);
}
