package com.aitravel.service;

import com.aitravel.common.PageResult;
import com.aitravel.dto.ScenicQuery;
import com.aitravel.dto.ScenicSaveRequest;
import com.aitravel.vo.AdminChatVO;
import com.aitravel.vo.AdminTripVO;
import com.aitravel.vo.AdminUserVO;
import com.aitravel.vo.ScenicVO;

/**
 * 后台管理服务（详细设计 §8.2）。
 *
 * <p>编排层：本模块无自有表与 Mapper，全部数据操作委托归属模块的 Service
 * （{@link UserService}、{@link ScenicService}、{@link TripService}、{@link ChatService}）。
 * 权限由 M-SECURITY 的 {@code @RequireAdmin} 裁决，本层不重复校验权限。
 *
 * @see com.aitravel.service.impl.AdminServiceImpl
 */
public interface AdminService {

    /**
     * 后台分页查询用户（委托 {@link UserService#pageUsersForAdmin}）。
     *
     * @param keyword 用户名/昵称模糊关键字，可为空
     * @param page    页码，从 1 开始
     * @param size    每页大小
     * @return 分页结果（不含密码）
     */
    PageResult<AdminUserVO> pageUsers(String keyword, int page, int size);

    /**
     * 启用/禁用用户（委托 {@link UserService#updateStatus}）。
     *
     * @param userId 目标用户主键
     * @param status 状态，仅允许 0（禁用）/ 1（正常）
     * @throws com.aitravel.common.BusinessException {@code status} 为 {@code null} 或非 0/1（400）；
     *         用户不存在（404，由上游抛出）
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 后台分页查询景点（委托 {@link ScenicService#pageSpotsForAdmin}）。
     *
     * @param query 查询参数（{@code city}/{@code category}/{@code keyword}/{@code page}/{@code size}）
     * @return 分页结果
     */
    PageResult<ScenicVO> pageSpots(ScenicQuery query);

    /**
     * 新增景点（委托 {@link ScenicService#createSpot}）；{@code tags}/{@code images} 的逗号拼接
     * 在 ScenicService 内部完成，本层透传请求对象。
     *
     * @param req 新增请求
     * @return 新景点 ID
     * @throws com.aitravel.common.BusinessException 必填字段缺失（400）
     */
    Long createSpot(ScenicSaveRequest req);

    /**
     * 编辑景点（委托 {@link ScenicService#updateSpot}）。
     *
     * @param id  景点 ID
     * @param req 编辑请求
     * @throws com.aitravel.common.BusinessException 景点不存在（404）
     */
    void updateSpot(Long id, ScenicSaveRequest req);

    /**
     * 逻辑删除景点（委托 {@link ScenicService#deleteSpot}）；不级联删除收藏与行程快照。
     *
     * @param id 景点 ID
     * @throws com.aitravel.common.BusinessException 景点不存在（404）
     */
    void deleteSpot(Long id);

    /**
     * 后台分页查询行程（委托 {@link TripService#pageTripsForAdmin}）。
     *
     * @param page 页码，从 1 开始
     * @param size 每页大小
     * @return 分页结果
     */
    PageResult<AdminTripVO> pageTrips(int page, int size);

    /**
     * 后台分页查询对话记录（委托 {@link ChatService#pageChatsForAdmin}）。
     *
     * @param page 页码，从 1 开始
     * @param size 每页大小
     * @return 分页结果
     */
    PageResult<AdminChatVO> pageChats(int page, int size);
}
