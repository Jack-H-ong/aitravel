package com.aitravel.controller;

import com.aitravel.common.PageResult;
import com.aitravel.common.Result;
import com.aitravel.dto.GenerateTripRequest;
import com.aitravel.dto.UpdateTripRequest;
import com.aitravel.security.UserContext;
import com.aitravel.service.TripService;
import com.aitravel.vo.TripDetailVO;
import com.aitravel.vo.TripVO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 行程接口（详细设计 §5.3）。
 *
 * <p>全部接口需登录，{@code userId} 取自 {@link UserContext}。
 */
@RestController
@RequestMapping("/api/trip")
@Validated
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "DI 注入的 TripService 由 Spring 容器持有，此处仅保存引用")
public class TripController {

    private final TripService tripService;

    /**
     * 构造控制器。
     *
     * @param tripService 行程服务
     */
    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    /**
     * 生成攻略（§5.3.1，需登录）。
     *
     * @param request 生成请求
     * @return 新行程详情
     */
    @PostMapping("/generate")
    public Result<TripDetailVO> generate(@Valid @RequestBody GenerateTripRequest request) {
        return Result.ok(tripService.generateTrip(UserContext.getUserId(), request));
    }

    /**
     * 行程列表（§5.3.2，需登录）。
     *
     * @param city 目的地城市过滤，可选
     * @param page 页码，从 1 开始，默认 1
     * @param size 每页大小，默认 10
     * @return 行程分页
     */
    @GetMapping("/list")
    public Result<PageResult<TripVO>> list(
            @RequestParam(required = false) @Size(max = 50) String city,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(tripService.pageTrips(UserContext.getUserId(), city, page, size));
    }

    /**
     * 行程详情（§5.3.3，需登录）。
     *
     * @param id 行程 ID
     * @return 行程详情
     */
    @GetMapping("/{id}")
    public Result<TripDetailVO> detail(@PathVariable Long id) {
        return Result.ok(tripService.getTripDetail(UserContext.getUserId(), id));
    }

    /**
     * 修改行程（§5.3.4，需登录）。
     *
     * @param id      行程 ID
     * @param request 修改请求
     * @return 空成功响应
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody UpdateTripRequest request) {
        tripService.updateTrip(UserContext.getUserId(), id, request);
        return Result.ok();
    }

    /**
     * 删除行程（§5.3.5，需登录）。
     *
     * @param id 行程 ID
     * @return 空成功响应
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tripService.deleteTrip(UserContext.getUserId(), id);
        return Result.ok();
    }
}
