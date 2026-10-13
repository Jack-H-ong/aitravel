package com.aitravel.mapper;

import com.aitravel.entity.AiChat;
import com.aitravel.vo.AdminChatVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** {@code ai_chat} 表数据访问。 */
@Mapper
public interface AiChatMapper extends BaseMapper<AiChat> {

    /**
     * 按 {@code create_at} 倒序取某用户最近 N 条对话（供 AI 上下文使用）。
     *
     * @param userId 用户 ID
     * @param tripId 行程 ID，可为 {@code null}（不限定行程）
     * @param limit  条数上限
     * @return 最近 N 条对话（时间倒序）
     */
    @Select("<script>SELECT * FROM ai_chat WHERE user_id = #{userId} "
            + "<if test='tripId != null'> AND trip_id = #{tripId}</if> "
            + "ORDER BY create_at DESC LIMIT #{limit}</script>")
    List<AiChat> selectRecent(@Param("userId") Long userId, @Param("tripId") Long tripId,
            @Param("limit") int limit);

    /**
     * 后台分页查询全部对话并关联归属用户名（供 M-ADMIN）。
     *
     * <p>按创建时间倒序；{@code user_id}/{@code create_at} 经 {@code map-underscore-to-camel-case}
     * 映射为驼峰字段。
     *
     * @param page 分页参数（须为方法首参以触动物理分页）
     * @return 含用户名的对话分页
     */
    @Select("SELECT c.id, c.user_id, u.username, c.trip_id, c.role, c.content, c.create_at "
            + "FROM ai_chat c JOIN `user` u ON c.user_id = u.id ORDER BY c.create_at DESC")
    IPage<AdminChatVO> selectAdminChatPage(IPage<AdminChatVO> page);
}
