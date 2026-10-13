package com.aitravel.service;

import com.aitravel.common.PageResult;
import com.aitravel.dto.ScenicQuery;
import com.aitravel.dto.ScenicSaveRequest;
import com.aitravel.service.ai.ScenicBrief;
import com.aitravel.vo.BrowseHistoryVO;
import com.aitravel.vo.FavoriteVO;
import com.aitravel.vo.ScenicDetailVO;
import com.aitravel.vo.ScenicVO;
import java.util.List;

/**
 * 景点服务（详细设计 §4.1 / §4.2）。
 *
 * <p>对外提供景点列表/详情、收藏切换、我的收藏、浏览历史，以及供 M-TRIP / M-CHAT / M-ADMIN
 * 复用的城市景点查询与后台 CRUD 能力。
 *
 * @see com.aitravel.service.impl.ScenicServiceImpl
 */
public interface ScenicService {

    /**
     * 分页查询景点（含收藏态）。
     *
     * @param query  查询参数
     * @param userId 当前用户 ID，未登录为 {@code null}
     * @return 分页结果；未登录时所有 {@code isFavorite=false}
     */
    PageResult<ScenicVO> pageScenic(ScenicQuery query, Long userId);

    /**
     * 查询景点详情（浏览量 +1；已登录时 UPSERT 浏览记录并回填收藏态）。
     *
     * @param id     景点 ID
     * @param userId 当前用户 ID，未登录为 {@code null}
     * @return 景点详情，{@code viewCount} 为自增后的值
     * @throws com.aitravel.common.BusinessException 景点不存在（404）
     */
    ScenicDetailVO detail(Long id, Long userId);

    /**
     * 收藏/取消收藏切换（同一事务维护 {@code favorite_count}）。
     *
     * @param userId   当前用户 ID
     * @param scenicId 景点 ID
     * @return 切换后的动作与收藏态
     * @throws com.aitravel.common.BusinessException 景点不存在（404）
     */
    FavoriteVO toggleFavorite(Long userId, Long scenicId);

    /**
     * 分页查询当前用户的收藏景点，按收藏时间倒序。
     *
     * @param userId 当前用户 ID
     * @param page   页码，从 1 开始
     * @param size   每页大小
     * @return 分页结果，{@code isFavorite} 恒为 {@code true}
     */
    PageResult<ScenicVO> myFavorites(Long userId, int page, int size);

    /**
     * 查询某城市全部景点简要信息（供 M-TRIP 构造 Prompt）。
     *
     * @param city 城市
     * @return 景点简要列表（可能为空）
     */
    List<ScenicBrief> listByCity(String city);

    /**
     * 查询某城市全部景点简要信息（供 M-CHAT 构造上下文）。
     *
     * @param city 城市
     * @return 景点简要列表（可能为空）
     */
    List<ScenicBrief> listBriefByCity(String city);

    /**
     * 分页查询用户浏览历史（关联景点信息，按浏览时间倒序，供 M-PROFILE）。
     *
     * @param userId 用户 ID
     * @param page   页码，从 1 开始
     * @param size   每页大小
     * @return 分页结果
     */
    PageResult<BrowseHistoryVO> pageBrowseHistory(Long userId, int page, int size);

    /**
     * 后台分页查询景点（供 M-ADMIN）。
     *
     * @param query 查询参数
     * @return 分页结果，{@code isFavorite=false}
     */
    PageResult<ScenicVO> pageSpotsForAdmin(ScenicQuery query);

    /**
     * 新增景点（供 M-ADMIN）；{@code tags}/{@code images} 拼接为逗号分隔字符串入库。
     *
     * @param req 新增请求
     * @return 新景点 ID
     */
    Long createSpot(ScenicSaveRequest req);

    /**
     * 编辑景点（供 M-ADMIN）。
     *
     * @param id  景点 ID
     * @param req 编辑请求
     * @throws com.aitravel.common.BusinessException 景点不存在（404）
     */
    void updateSpot(Long id, ScenicSaveRequest req);

    /**
     * 逻辑删除景点（供 M-ADMIN）。
     *
     * @param id 景点 ID
     */
    void deleteSpot(Long id);
}
