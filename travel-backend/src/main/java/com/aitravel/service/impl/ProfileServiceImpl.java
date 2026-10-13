package com.aitravel.service.impl;

import com.aitravel.common.PageResult;
import com.aitravel.service.ProfileService;
import com.aitravel.service.ScenicService;
import com.aitravel.vo.BrowseHistoryVO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

/**
 * 个人中心聚合查询服务实现（详细设计 §7.3）。
 *
 * <p>浏览历史直接委托 {@link ScenicService#pageBrowseHistory(Long, int, int)}，
 * 不注入 {@code TripService}（「我的行程」由前端直接调用 {@code /api/trip/list}）。
 */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "DI 注入的 ScenicService 由 Spring 容器持有，此处仅保存引用")
public class ProfileServiceImpl implements ProfileService {

    private final ScenicService scenicService;

    /**
     * 构造服务实现。
     *
     * @param scenicService 景点服务（提供浏览历史查询能力）
     */
    public ProfileServiceImpl(ScenicService scenicService) {
        this.scenicService = scenicService;
    }

    @Override
    public PageResult<BrowseHistoryVO> pageBrowseHistory(Long userId, int page, int size) {
        return scenicService.pageBrowseHistory(userId, page, size);
    }
}
