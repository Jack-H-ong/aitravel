package com.aitravel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.aitravel.common.BusinessException;
import com.aitravel.common.ErrorCode;
import com.aitravel.common.PageResult;
import com.aitravel.dto.ScenicQuery;
import com.aitravel.dto.ScenicSaveRequest;
import com.aitravel.dto.UpdateStatusRequest;
import com.aitravel.service.impl.AdminServiceImpl;
import com.aitravel.vo.AdminChatVO;
import com.aitravel.vo.AdminTripVO;
import com.aitravel.vo.AdminUserVO;
import com.aitravel.vo.ScenicVO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link AdminServiceImpl} 单元测试（详细设计 §8.6）。
 *
 * <p>Mock 4 个上游 Service，不启动 Spring 容器；不测权限（{@code @RequireAdmin} 的 401/403
 * 分支在 M-SECURITY 覆盖），直接以「已通过鉴权」为前提调用 Service。
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    private static final long USER_ID = 7L;
    private static final long SPOT_ID = 2001L;

    @Mock
    private UserService userService;

    @Mock
    private ScenicService scenicService;

    @Mock
    private TripService tripService;

    @Mock
    private ChatService chatService;

    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(userService, scenicService, tripService, chatService);
    }

    // ---------- 列表：委托上游 + 分页参数透传 ----------

    @Test
    void pageUsers_delegatesKeywordAndPaging() {
        PageResult<AdminUserVO> expected = new PageResult<>(List.of(), 0L, 2L, 20L);
        when(userService.pageUsersForAdmin("zhao", 2, 20)).thenReturn(expected);

        PageResult<AdminUserVO> actual = adminService.pageUsers("zhao", 2, 20);

        verify(userService).pageUsersForAdmin("zhao", 2, 20);
        assertSame(expected, actual);
    }

    @Test
    void pageSpots_delegatesQueryObject() {
        ScenicQuery query = new ScenicQuery();
        query.setCity("杭州");
        query.setCategory("自然");
        query.setKeyword("湖");
        query.setPage(3);
        query.setSize(5);
        PageResult<ScenicVO> expected = new PageResult<>(List.of(new ScenicVO()), 1L, 3L, 5L);
        when(scenicService.pageSpotsForAdmin(query)).thenReturn(expected);

        PageResult<ScenicVO> actual = adminService.pageSpots(query);

        verify(scenicService).pageSpotsForAdmin(query);
        assertSame(expected, actual);
    }

    @Test
    void pageTrips_delegatesPaging() {
        PageResult<AdminTripVO> expected = new PageResult<>(List.of(), 0L, 1L, 10L);
        when(tripService.pageTripsForAdmin(1, 10)).thenReturn(expected);

        PageResult<AdminTripVO> actual = adminService.pageTrips(1, 10);

        verify(tripService).pageTripsForAdmin(1, 10);
        assertSame(expected, actual);
    }

    @Test
    void pageChats_delegatesPaging() {
        PageResult<AdminChatVO> expected = new PageResult<>(List.of(), 0L, 4L, 30L);
        when(chatService.pageChatsForAdmin(4, 30)).thenReturn(expected);

        PageResult<AdminChatVO> actual = adminService.pageChats(4, 30);

        verify(chatService).pageChatsForAdmin(4, 30);
        assertSame(expected, actual);
    }

    // ---------- 新增/修改景点：透传请求对象（含 tags/images 为 List） ----------

    @Test
    void createSpot_delegatesRequestAndReturnsNewId() {
        ScenicSaveRequest req = sampleRequest();
        when(scenicService.createSpot(req)).thenReturn(SPOT_ID);

        Long newId = adminService.createSpot(req);

        verify(scenicService).createSpot(req);
        assertEquals(SPOT_ID, newId);
        // tags/images 为 List，字符串拼接由 ScenicService 内部负责，此处仅透传
        assertEquals(List.of("自然", "5A"), req.getTags());
        assertEquals(List.of("https://cdn.example.com/a.jpg"), req.getImages());
    }

    @Test
    void updateSpot_delegatesIdAndRequest() {
        ScenicSaveRequest req = sampleRequest();

        adminService.updateSpot(SPOT_ID, req);

        verify(scenicService).updateSpot(SPOT_ID, req);
    }

    // ---------- 删除景点：不级联 ----------

    @Test
    void deleteSpot_delegatesWithoutAnyCascade() {
        adminService.deleteSpot(SPOT_ID);

        verify(scenicService).deleteSpot(SPOT_ID);
        // 不调用任何级联删除：上游 4 个 Service 无其它交互
        verifyNoMoreInteractions(scenicService);
        verifyNoInteractions(userService, tripService, chatService);
    }

    // ---------- 禁用/启用用户：status=0/1 均透传 ----------

    @Test
    void updateUserStatus_disableStatusZero_delegates() {
        adminService.updateUserStatus(USER_ID, 0);

        verify(userService).updateStatus(USER_ID, 0);
    }

    @Test
    void updateUserStatus_enableStatusOne_delegates() {
        adminService.updateUserStatus(USER_ID, 1);

        verify(userService).updateStatus(USER_ID, 1);
    }

    // ---------- 边界：status 非法 → 400 且上游未被调用 ----------

    @Test
    void updateUserStatus_statusTwo_throwsBadRequestWithoutUpstreamCall() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.updateUserStatus(USER_ID, 2));

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        verifyNoInteractions(userService);
    }

    @Test
    void updateUserStatus_statusNull_throwsBadRequestWithoutUpstreamCall() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.updateUserStatus(USER_ID, null));

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        verifyNoInteractions(userService);
    }

    // ---------- 异常透传：404 不被吞掉 ----------

    @Test
    void updateSpot_spotNotFound_propagates404() {
        ScenicSaveRequest req = sampleRequest();
        doThrow(new BusinessException(ErrorCode.NOT_FOUND))
                .when(scenicService).updateSpot(SPOT_ID, req);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.updateSpot(SPOT_ID, req));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(scenicService).updateSpot(SPOT_ID, req);
    }

    @Test
    void deleteSpot_spotNotFound_propagates404() {
        doThrow(new BusinessException(ErrorCode.NOT_FOUND))
                .when(scenicService).deleteSpot(SPOT_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.deleteSpot(SPOT_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    // ---------- 异常透传：错误码与 message 原样（不改造语义） ----------

    @Test
    void updateUserStatus_upstreamBusinessException_propagatesCodeAndMessage() {
        doThrow(new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"))
                .when(userService).updateStatus(USER_ID, 0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.updateUserStatus(USER_ID, 0));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        assertEquals("用户不存在", ex.getMessage());
    }

    // ---------- DTO 校验边界 ----------

    @Test
    void updateStatusRequest_nullStatus_failsBeanValidation() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            UpdateStatusRequest missing = new UpdateStatusRequest();
            Set<?> violations = validator.validate(missing);
            assertFalse(violations.isEmpty(), "status 为 null 应产生约束违例");

            UpdateStatusRequest present = new UpdateStatusRequest();
            present.setStatus(1);
            assertTrue(validator.validate(present).isEmpty(), "status=1 应通过校验");
        }
    }

    private static ScenicSaveRequest sampleRequest() {
        ScenicSaveRequest req = new ScenicSaveRequest();
        req.setName("西湖");
        req.setCity("杭州");
        req.setCategory("自然");
        req.setPrice(new BigDecimal("0"));
        req.setTags(List.of("自然", "5A"));
        req.setImages(List.of("https://cdn.example.com/a.jpg"));
        return req;
    }
}
