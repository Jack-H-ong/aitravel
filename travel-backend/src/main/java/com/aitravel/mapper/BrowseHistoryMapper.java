package com.aitravel.mapper;

import com.aitravel.entity.BrowseHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** {@code browse_history} 表数据访问。 */
@Mapper
public interface BrowseHistoryMapper extends BaseMapper<BrowseHistory> {

    /**
     * 浏览记录 UPSERT：命中唯一键 {@code (user_id, scenic_id)} 时仅刷新 {@code browse_time}。
     *
     * <p><b>MySQL 专有语法</b>（{@code ON DUPLICATE KEY UPDATE}），H2 不支持，因此不在 H2 集成测试中
     * 真实执行；其正确性由 Mapper 契约单测断言 SQL 文本覆盖，真实行为留待 MySQL 部署冒烟。
     *
     * @param userId   用户 ID
     * @param scenicId 景点 ID
     * @return 受影响行数
     */
    @Insert("INSERT INTO browse_history(user_id, scenic_id, browse_time) "
            + "VALUES(#{userId}, #{scenicId}, NOW()) "
            + "ON DUPLICATE KEY UPDATE browse_time = NOW()")
    int upsert(@Param("userId") Long userId, @Param("scenicId") Long scenicId);
}
