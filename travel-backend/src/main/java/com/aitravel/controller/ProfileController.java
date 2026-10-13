package com.aitravel.controller;

import com.aitravel.common.PageResult;
import com.aitravel.common.Result;
import com.aitravel.dto.ProfileHistoryQuery;
import com.aitravel.security.UserContext;
import com.aitravel.service.ProfileService;
import com.aitravel.vo.BrowseHistoryVO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心接口（详细设计 §7.3 / §7.4）。
 *
 * <p>仅暴露「浏览历史」；「我的行程」「我的收藏」复用 {@code /api/trip/list}、
 * {@code /api/scenic/my-favorites}，本控制器不提供。
 *
 * <p>{@code /history} 需登录，{@code userId} 取自 {@link UserContext}。参数使用
 * {@code @Valid @ModelAttribute} 绑定 POJO：{@code size} 越界时抛 {@code BindException}，
 * 由 {@code GlobalExceptionHandler} 统一转 400。
 */
@RestController
@RequestMapping("/api/profile")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "DI 注入的 ProfileService 由 Spring 容器持有，此处仅保存引用")
public class ProfileController {

    private final ProfileService profileService;

    /**
     * 构造控制器。
     *
     * @param profileService 个人中心服务
     */
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * 浏览历史（§7.4，需登录），按浏览时间倒序。
     *
     * @param query 分页参数（page 默认 1，size 默认 10，1~100）
     * @return 分页结果
     */
    @GetMapping("/history")
    public Result<PageResult<BrowseHistoryVO>> history(
            @Valid @ModelAttribute ProfileHistoryQuery query) {
        return Result.ok(profileService.pageBrowseHistory(
                UserContext.getUserId(), query.getPage(), query.getSize()));
    }
}
