package com.aitravel.service;

import com.aitravel.common.PageResult;
import com.aitravel.dto.GenerateTripRequest;
import com.aitravel.dto.UpdateTripRequest;
import com.aitravel.vo.AdminTripVO;
import com.aitravel.vo.TripDetailVO;
import com.aitravel.vo.TripVO;

/**
 * 行程服务（详细设计 §5.2）。
 *
 * <p>对外提供攻略生成、行程列表/详情/修改/删除，以及供 M-CHAT 的 {@code getTripSummary} 与供
 * M-ADMIN 的 {@code pageTripsForAdmin}。
 *
 * @see com.aitravel.service.impl.TripServiceImpl
 */
public interface TripService {

    /**
     * 生成攻略：查景点 → 调 AI（事务外） → 事务内落库 → 返回详情。
     *
     * @param userId 当前用户 ID
     * @param req    生成请求
     * @return 新行程详情
     * @throws com.aitravel.common.BusinessException 该城市无景点（400）或 AI 生成失败（5001）
     */
    TripDetailVO generateTrip(Long userId, GenerateTripRequest req);

    /**
     * 分页查询当前用户的行程，按创建时间倒序。
     *
     * @param userId 当前用户 ID
     * @param city   目的地城市过滤，可选
     * @param page   页码，从 1 开始
     * @param size   每页大小
     * @return 行程分页（{@link TripVO} 不含 {@code dayList}）
     */
    PageResult<TripVO> pageTrips(Long userId, String city, int page, int size);

    /**
     * 查询行程详情（含逐日安排，按 {@code day_no}/{@code visit_order} 升序）。
     *
     * @param userId 当前用户 ID
     * @param tripId 行程 ID
     * @return 行程详情
     * @throws com.aitravel.common.BusinessException 行程不存在（404）或不属于当前用户（403）
     */
    TripDetailVO getTripDetail(Long userId, Long tripId);

    /**
     * 修改行程：更新基础信息 → 物理删除旧子表记录 → 批量插入新记录。
     *
     * @param userId 当前用户 ID
     * @param tripId 行程 ID
     * @param req    修改请求
     * @throws com.aitravel.common.BusinessException 行程不存在（404）或不属于当前用户（403）
     */
    void updateTrip(Long userId, Long tripId, UpdateTripRequest req);

    /**
     * 删除行程：{@code trip} 逻辑删除，{@code trip_day}/{@code trip_spot} 物理删除（D-01）。
     *
     * @param userId 当前用户 ID
     * @param tripId 行程 ID
     * @throws com.aitravel.common.BusinessException 行程不存在（404）或不属于当前用户（403）
     */
    void deleteTrip(Long userId, Long tripId);

    /**
     * 生成行程文本摘要（供 M-CHAT 构造上下文）。
     *
     * @param tripId 行程 ID
     * @param userId 当前用户 ID
     * @return 行程摘要文本
     * @throws com.aitravel.common.BusinessException 行程不存在（404）或不属于当前用户（403）
     */
    String getTripSummary(Long tripId, Long userId);

    /**
     * 后台分页查询全部行程（关联归属用户名，供 M-ADMIN）。
     *
     * @param page 页码，从 1 开始
     * @param size 每页大小
     * @return 行程分页
     */
    PageResult<AdminTripVO> pageTripsForAdmin(int page, int size);
}
