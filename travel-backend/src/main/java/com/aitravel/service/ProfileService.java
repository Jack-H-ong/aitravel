package com.aitravel.service;

import com.aitravel.common.PageResult;
import com.aitravel.vo.BrowseHistoryVO;

/**
 * 个人中心聚合查询服务（详细设计 §7）。
 *
 * <p>本模块为聚合查询层，无自有表、无写操作；仅暴露「浏览历史」能力，其余功能复用
 * {@code /api/trip/list}、{@code /api/scenic/my-favorites}。
 *
 * @see com.aitravel.service.impl.ProfileServiceImpl
 */
public interface ProfileService {

    /**
     * 分页查询当前用户的浏览历史（按浏览时间倒序，委托 {@link ScenicService}）。
     *
     * @param userId 当前用户 ID
     * @param page   页码，从 1 开始
     * @param size   每页大小
     * @return 分页结果；无记录时 {@code records} 为空列表（非 {@code null}）
     */
    PageResult<BrowseHistoryVO> pageBrowseHistory(Long userId, int page, int size);
}
