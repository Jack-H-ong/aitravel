package com.aitravel.controller;

import com.aitravel.common.PageResult;
import com.aitravel.common.Result;
import com.aitravel.dto.ScenicQuery;
import com.aitravel.dto.ScenicSaveRequest;
import com.aitravel.dto.UpdateStatusRequest;
import com.aitravel.security.annotation.RequireAdmin;
import com.aitravel.service.AdminService;
import com.aitravel.vo.AdminChatVO;
import com.aitravel.vo.AdminTripVO;
import com.aitravel.vo.AdminUserVO;
import com.aitravel.vo.ScenicVO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台管理接口（详细设计 §8.3）。
 *
 * <p>类级 {@link RequireAdmin} 覆盖全部端点：未登录 401、非 admin 403，均拒绝访问，
 * 权限裁决由 M-SECURITY 的 {@code AuthInterceptor} 完成，本控制器不做权限判断。
 * 所有业务逻辑委托 {@link AdminService}（编排层，无自有持久化）。
 */
@RestController
@RequestMapping("/api/admin")
@RequireAdmin
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "DI 注入的 AdminService 由 Spring 容器持有，此处仅保存引用")
public class AdminController {

    private final AdminService adminService;

    /**
     * 构造控制器。
     *
     * @param adminService 后台管理服务
     */
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * 后台景点列表（§8.3）。
     *
     * @param query 查询参数（{@code city}/{@code category}/{@code keyword}/{@code page}/{@code size}）
     * @return 分页结果
     */
    @GetMapping("/scenic")
    public Result<PageResult<ScenicVO>> listScenic(@Valid @ModelAttribute ScenicQuery query) {
        return Result.ok(adminService.pageSpots(query));
    }

    /**
     * 新增景点（§8.3）。
     *
     * @param request 景点新增请求（{@code name}/{@code city} 必填）
     * @return 新景点 ID
     */
    @PostMapping("/scenic")
    public Result<Long> createScenic(@Valid @RequestBody ScenicSaveRequest request) {
        return Result.ok(adminService.createSpot(request));
    }

    /**
     * 编辑景点（§8.3）。
     *
     * @param id      景点 ID
     * @param request 景点编辑请求（{@code name}/{@code city} 必填）
     * @return 空响应
     */
    @PutMapping("/scenic/{id}")
    public Result<Void> updateScenic(@PathVariable Long id,
                                     @Valid @RequestBody ScenicSaveRequest request) {
        adminService.updateSpot(id, request);
        return Result.ok();
    }

    /**
     * 删除景点（§8.3，逻辑删除，不级联）。
     *
     * @param id 景点 ID
     * @return 空响应
     */
    @DeleteMapping("/scenic/{id}")
    public Result<Void> deleteScenic(@PathVariable Long id) {
        adminService.deleteSpot(id);
        return Result.ok();
    }

    /**
     * 后台用户列表（§8.3）。
     *
     * @param keyword 用户名/昵称模糊关键字，可选
     * @param page    页码，从 1 开始，默认 1
     * @param size    每页大小，默认 10
     * @return 分页结果（不含密码）
     */
    @GetMapping("/users")
    public Result<PageResult<AdminUserVO>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminService.pageUsers(keyword, page, size));
    }

    /**
     * 启用/禁用用户（§8.3，不删除用户数据）。
     *
     * @param id      目标用户主键
     * @param request 状态请求（{@code status=0|1}）
     * @return 空响应
     */
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                         @Valid @RequestBody UpdateStatusRequest request) {
        adminService.updateUserStatus(id, request.getStatus());
        return Result.ok();
    }

    /**
     * 后台行程列表（§8.3）。
     *
     * @param page 页码，从 1 开始，默认 1
     * @param size 每页大小，默认 10
     * @return 分页结果
     */
    @GetMapping("/trips")
    public Result<PageResult<AdminTripVO>> listTrips(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminService.pageTrips(page, size));
    }

    /**
     * 后台对话记录列表（§8.3）。
     *
     * @param page 页码，从 1 开始，默认 1
     * @param size 每页大小，默认 10
     * @return 分页结果
     */
    @GetMapping("/chats")
    public Result<PageResult<AdminChatVO>> listChats(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminService.pageChats(page, size));
    }
}
