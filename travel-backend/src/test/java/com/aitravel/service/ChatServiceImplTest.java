package com.aitravel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aitravel.common.BusinessException;
import com.aitravel.common.ErrorCode;
import com.aitravel.common.ErrorMessage;
import com.aitravel.dto.ChatRequest;
import com.aitravel.entity.AiChat;
import com.aitravel.mapper.AiChatMapper;
import com.aitravel.service.ai.AiGateway;
import com.aitravel.service.ai.AiServiceException;
import com.aitravel.service.ai.ChatContext;
import com.aitravel.service.ai.ChatTurn;
import com.aitravel.service.ai.ScenicBrief;
import com.aitravel.service.impl.ChatServiceImpl;
import com.aitravel.vo.ChatMessageVO;
import com.aitravel.vo.ChatVO;
import com.aitravel.vo.TripDetailVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * {@link ChatServiceImpl} 单元测试（详细设计 §6.7）。
 *
 * <p>Mock {@link AiChatMapper}、{@link TripService}、{@link ScenicService}、{@link AiGateway}。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceImplTest {

    private static final long USER_ID = 1L;
    private static final long TRIP_ID = 3001L;
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 10, 12, 10, 5, 0);

    @Mock
    private AiChatMapper aiChatMapper;

    @Mock
    private TripService tripService;

    @Mock
    private ScenicService scenicService;

    @Mock
    private AiGateway aiGateway;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(aiChatMapper, tripService, scenicService, aiGateway);
        when(aiChatMapper.insert(any(AiChat.class))).thenAnswer(invocation -> {
            AiChat chat = invocation.getArgument(0);
            if ("assistant".equals(chat.getRole())) {
                chat.setCreateAt(FIXED_TIME);
            }
            return 1;
        });
    }

    // ---------- 正常：无 tripId ----------

    @Test
    void chatWithoutTripIdInsertsUserThenAssistant() {
        when(aiChatMapper.selectRecent(USER_ID, null, 20)).thenReturn(List.of());
        when(aiGateway.chat(any())).thenReturn("西湖周边推荐……");

        ChatRequest req = req("西湖附近有什么好吃的？", null);
        ChatVO vo = chatService.chat(USER_ID, req);

        ArgumentCaptor<AiChat> captor = ArgumentCaptor.forClass(AiChat.class);
        verify(aiChatMapper, times(2)).insert(captor.capture());
        assertEquals("user", captor.getAllValues().get(0).getRole());
        assertEquals("assistant", captor.getAllValues().get(1).getRole());
        assertEquals(USER_ID, captor.getAllValues().get(0).getUserId());
        assertNull(captor.getAllValues().get(0).getTripId());

        assertEquals("西湖周边推荐……", vo.getAnswer());
        assertEquals(FIXED_TIME, vo.getCreatedAt());

        ArgumentCaptor<ChatContext> ctxCaptor = ArgumentCaptor.forClass(ChatContext.class);
        verify(aiGateway, times(1)).chat(ctxCaptor.capture());
        assertNull(ctxCaptor.getValue().tripSummary());
        assertEquals("", ctxCaptor.getValue().scenicSummary());
    }

    // ---------- 正常：带 tripId ----------

    @Test
    void chatWithTripIdCallsSummaryAndWritesTripId() {
        when(aiChatMapper.selectRecent(USER_ID, TRIP_ID, 20)).thenReturn(List.of());
        when(tripService.getTripSummary(TRIP_ID, USER_ID)).thenReturn("【杭州2日】杭州 · 2天");
        TripDetailVO detail = new TripDetailVO();
        detail.setCity("杭州");
        when(tripService.getTripDetail(USER_ID, TRIP_ID)).thenReturn(detail);
        when(scenicService.listBriefByCity("杭州")).thenReturn(List.of(
                new ScenicBrief(2001L, "西湖", "杭州", "自然", "标签", "5A 景区", "全天",
                        new BigDecimal("0.00"))));
        when(aiGateway.chat(any())).thenReturn("回答");

        chatService.chat(USER_ID, req("有什么好玩的？", TRIP_ID));

        verify(tripService, times(1)).getTripSummary(TRIP_ID, USER_ID);
        ArgumentCaptor<AiChat> captor = ArgumentCaptor.forClass(AiChat.class);
        verify(aiChatMapper, times(2)).insert(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .allMatch(c -> TRIP_ID == c.getTripId()), "两条记录均应写入 tripId");

        ArgumentCaptor<ChatContext> ctxCaptor = ArgumentCaptor.forClass(ChatContext.class);
        verify(aiGateway).chat(ctxCaptor.capture());
        assertEquals("【杭州2日】杭州 · 2天", ctxCaptor.getValue().tripSummary());
        assertTrue(ctxCaptor.getValue().scenicSummary().contains("西湖"));
    }

    // ---------- 正常：历史查询 ----------

    @Test
    @SuppressWarnings("unchecked")
    void pageHistoryFiltersByUserId() {
        doReturn(pageOf(chat(4001L, "user", "问"), 1L)).when(aiChatMapper).selectPage(any(), any());

        chatService.pageHistory(USER_ID, null, 1, 10);

        ArgumentCaptor<LambdaQueryWrapper<AiChat>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(aiChatMapper).selectPage(any(), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("user_id"), "应按 user_id 过滤");
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(USER_ID));
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageHistoryWithTripIdAddsTripCondition() {
        doReturn(pageOf(chat(4001L, "user", "问"), 1L)).when(aiChatMapper).selectPage(any(), any());

        com.aitravel.common.PageResult<ChatMessageVO> result =
                chatService.pageHistory(USER_ID, TRIP_ID, 1, 10);

        ArgumentCaptor<LambdaQueryWrapper<AiChat>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(aiChatMapper).selectPage(any(), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("trip_id"), "应按 trip_id 过滤");
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(TRIP_ID));

        assertEquals(1, result.getRecords().size());
        assertEquals("user", result.getRecords().get(0).getRole());
    }

    // ---------- 正常：历史上下文裁剪与正序 ----------

    @Test
    void chatTrimsHistoryToTwentyAndPassesAscendingOrder() {
        // selectRecent 返回倒序（最新在前）
        when(aiChatMapper.selectRecent(USER_ID, null, 20)).thenReturn(List.of(
                chat(4003L, "assistant", "第三"),
                chat(4002L, "user", "第二"),
                chat(4001L, "user", "第一")));
        when(aiGateway.chat(any())).thenReturn("答案");

        chatService.chat(USER_ID, req("问题", null));

        verify(aiChatMapper, times(1)).selectRecent(eq(USER_ID), isNull(), eq(20));

        ArgumentCaptor<ChatContext> ctxCaptor = ArgumentCaptor.forClass(ChatContext.class);
        verify(aiGateway).chat(ctxCaptor.capture());
        List<ChatTurn> history = ctxCaptor.getValue().history();
        assertEquals(3, history.size());
        assertEquals(List.of("第一", "第二", "第三"),
                history.stream().map(ChatTurn::content).toList(), "历史应反转为正序");
    }

    @Test
    void chatWithFewerThanTenRoundsDoesNotThrow() {
        when(aiChatMapper.selectRecent(USER_ID, null, 20)).thenReturn(List.of(
                chat(4002L, "assistant", "回答"),
                chat(4001L, "user", "提问")));
        when(aiGateway.chat(any())).thenReturn("答案");

        chatService.chat(USER_ID, req("问题", null));

        ArgumentCaptor<ChatContext> ctxCaptor = ArgumentCaptor.forClass(ChatContext.class);
        verify(aiGateway).chat(ctxCaptor.capture());
        assertEquals(2, ctxCaptor.getValue().history().size());
    }

    // ---------- 边界：tripId 为 null ----------

    @Test
    void chatWithoutTripIdNeverCallsTripService() {
        when(aiChatMapper.selectRecent(USER_ID, null, 20)).thenReturn(List.of());
        when(aiGateway.chat(any())).thenReturn("答案");

        chatService.chat(USER_ID, req("问题", null));

        verify(tripService, never()).getTripSummary(anyLong(), anyLong());
        verify(tripService, never()).getTripDetail(anyLong(), anyLong());
        verify(scenicService, never()).listBriefByCity(any());
    }

    // ---------- 异常：行程越权 ----------

    @Test
    void chatForbiddenTripPropagatesAndNeverInserts() {
        when(tripService.getTripSummary(TRIP_ID, USER_ID))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, ErrorMessage.TRIP_FORBIDDEN));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.chat(USER_ID, req("问题", TRIP_ID)));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(aiChatMapper, never()).insert(any(AiChat.class));
        verify(aiGateway, never()).chat(any());
    }

    // ---------- 异常：AI 失败（先落库） ----------

    @Test
    void chatAiFailureConvertsTo5001AndInsertsOnlyUser() {
        when(aiChatMapper.selectRecent(USER_ID, null, 20)).thenReturn(List.of());
        when(aiGateway.chat(any())).thenThrow(new AiServiceException("boom"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.chat(USER_ID, req("问题", null)));

        assertEquals(ErrorCode.AI_SERVICE_ERROR, ex.getErrorCode());
        assertEquals(ErrorMessage.AI_CHAT_UNAVAILABLE, ex.getMessage());

        ArgumentCaptor<AiChat> captor = ArgumentCaptor.forClass(AiChat.class);
        verify(aiChatMapper, times(1)).insert(captor.capture());
        assertEquals("user", captor.getValue().getRole());
        assertEquals("问题", captor.getValue().getContent());
    }

    // ---------- pageChatsForAdmin ----------

    @Test
    void pageChatsForAdminDelegatesToJoinQuery() {
        com.aitravel.vo.AdminChatVO adminChat = new com.aitravel.vo.AdminChatVO();
        adminChat.setId(4001L);
        adminChat.setUserId(USER_ID);
        adminChat.setUsername("alice");
        Page<com.aitravel.vo.AdminChatVO> page = new Page<>(1, 10);
        page.setRecords(List.of(adminChat));
        page.setTotal(1L);
        doReturn(page).when(aiChatMapper).selectAdminChatPage(any());

        com.aitravel.common.PageResult<com.aitravel.vo.AdminChatVO> result =
                chatService.pageChatsForAdmin(1, 10);

        assertEquals(1, result.getRecords().size());
        assertEquals("alice", result.getRecords().get(0).getUsername());
        verify(aiChatMapper, times(1)).selectAdminChatPage(any());
    }

    // ---------- helpers ----------

    private static ChatRequest req(String message, Long tripId) {
        ChatRequest req = new ChatRequest();
        req.setMessage(message);
        req.setTripId(tripId);
        return req;
    }

    private static AiChat chat(Long id, String role, String content) {
        AiChat chat = new AiChat();
        chat.setId(id);
        chat.setRole(role);
        chat.setContent(content);
        chat.setUserId(USER_ID);
        chat.setCreateAt(FIXED_TIME);
        return chat;
    }

    private static Page<AiChat> pageOf(AiChat chat, long total) {
        Page<AiChat> page = new Page<>(1, 10);
        page.setRecords(List.of(chat));
        page.setTotal(total);
        return page;
    }
}
