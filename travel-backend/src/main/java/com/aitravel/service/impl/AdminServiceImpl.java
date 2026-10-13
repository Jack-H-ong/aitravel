package com.aitravel.service.impl;

import com.aitravel.common.BusinessException;
import com.aitravel.common.ErrorCode;
import com.aitravel.common.PageResult;
import com.aitravel.dto.ScenicQuery;
import com.aitravel.dto.ScenicSaveRequest;
import com.aitravel.service.AdminService;
import com.aitravel.service.ChatService;
import com.aitravel.service.ScenicService;
import com.aitravel.service.TripService;
import com.aitravel.service.UserService;
import com.aitravel.vo.AdminChatVO;
import com.aitravel.vo.AdminTripVO;
import com.aitravel.vo.AdminUserVO;
import com.aitravel.vo.ScenicVO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

/**
 * 后台管理服务实现（详细设计 §8.2 / §8.4）。
 *
 * <p>纯编排层：不访问任何 Mapper，所有数据操作委托归属模块的 Service。上游抛出的
 * {@link BusinessException}（含 404）原样透传，本层不改造错误语义。
 */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "DI 注入的 4 个上游 Service 由 Spring 容器持有，此处仅保存引用")
public class AdminServiceImpl implements AdminService {

    private final UserService userService;
    private final ScenicService scenicService;
    private final TripService tripService;
    private final ChatService chatService;

    /**
     * 构造服务实现（构造注入 4 个上游 Service）。
     *
     * @param userService   用户服务
     * @param scenicService 景点服务
     * @param tripService   行程服务
     * @param chatService   对话服务
     */
    public AdminServiceImpl(UserService userService, ScenicService scenicService,
                            TripService tripService, ChatService chatService) {
        this.userService = userService;
        this.scenicService = scenicService;
        this.tripService = tripService;
        this.chatService = chatService;
    }

    @Override
    public PageResult<AdminUserVO> pageUsers(String keyword, int page, int size) {
        return userService.pageUsersForAdmin(keyword, page, size);
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        userService.updateStatus(userId, status);
    }

    @Override
    public PageResult<ScenicVO> pageSpots(ScenicQuery query) {
        return scenicService.pageSpotsForAdmin(query);
    }

    @Override
    public Long createSpot(ScenicSaveRequest req) {
        return scenicService.createSpot(req);
    }

    @Override
    public void updateSpot(Long id, ScenicSaveRequest req) {
        scenicService.updateSpot(id, req);
    }

    @Override
    public void deleteSpot(Long id) {
        scenicService.deleteSpot(id);
    }

    @Override
    public PageResult<AdminTripVO> pageTrips(int page, int size) {
        return tripService.pageTripsForAdmin(page, size);
    }

    @Override
    public PageResult<AdminChatVO> pageChats(int page, int size) {
        return chatService.pageChatsForAdmin(page, size);
    }
}
