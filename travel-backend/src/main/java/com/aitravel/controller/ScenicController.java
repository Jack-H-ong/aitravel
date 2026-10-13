package com.aitravel.controller;

import com.aitravel.common.PageResult;
import com.aitravel.common.Result;
import com.aitravel.dto.FavoriteRequest;
import com.aitravel.dto.ScenicQuery;
import com.aitravel.security.UserContext;
import com.aitravel.security.annotation.OptionalAuthApi;
import com.aitravel.service.ScenicService;
import com.aitravel.vo.FavoriteVO;
import com.aitravel.vo.ScenicDetailVO;
import com.aitravel.vo.ScenicVO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 景点接口（详细设计 §4.3）。
 *
 * <p>{@code /list}、{@code /{id}} 标注 {@link OptionalAuthApi}：未登录可访问，登录时回填收藏态；
 * {@code /favorite}、{@code /my-favorites} 需登录，{@code userId} 取自 {@link UserContext}。
 */
@RestController
@RequestMapping("/api/scenic")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "DI 注入的 ScenicService 由 Spring 容器持有，此处仅保存引用")
public class ScenicController {

    private final ScenicService scenicService;

    /**
     * 构造控制器。
     *
     * @param scenicService 景点服务
     */
    public ScenicController(ScenicService scenicService) {
        this.scenicService = scenicService;
    }

    /**
     * 景点列表（§4.3.1，可选登录）。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping("/list")
    @OptionalAuthApi
    public Result<PageResult<ScenicVO>> list(@Valid ScenicQuery query) {
        return Result.ok(scenicService.pageScenic(query, UserContext.getUserId()));
    }

    /**
     * 景点详情（§4.3.2，可选登录）。
     *
     * @param id 景点 ID
     * @return 景点详情
     */
    @GetMapping("/{id}")
    @OptionalAuthApi
    public Result<ScenicDetailVO> detail(@PathVariable Long id) {
        return Result.ok(scenicService.detail(id, UserContext.getUserId()));
    }

    /**
     * 收藏/取消收藏切换（§4.3.3，需登录）。
     *
     * @param request 收藏请求
     * @return 切换结果
     */
    @PostMapping("/favorite")
    public Result<FavoriteVO> favorite(@Valid @RequestBody FavoriteRequest request) {
        return Result.ok(
                scenicService.toggleFavorite(UserContext.getUserId(), request.getScenicId()));
    }

    /**
     * 我的收藏（§4.3.4，需登录）。
     *
     * @param page 页码，从 1 开始，默认 1
     * @param size 每页大小，默认 10
     * @return 分页结果
     */
    @GetMapping("/my-favorites")
    public Result<PageResult<ScenicVO>> myFavorites(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return Result.ok(scenicService.myFavorites(UserContext.getUserId(), page, size));
    }
}
