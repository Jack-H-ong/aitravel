package com.aitravel.service.impl;

import com.aitravel.common.BusinessException;
import com.aitravel.common.ErrorCode;
import com.aitravel.common.ErrorMessage;
import com.aitravel.common.PageResult;
import com.aitravel.dto.ChatRequest;
import com.aitravel.entity.AiChat;
import com.aitravel.mapper.AiChatMapper;
import com.aitravel.service.ChatService;
import com.aitravel.service.ScenicService;
import com.aitravel.service.TripService;
import com.aitravel.service.ai.AiGateway;
import com.aitravel.service.ai.AiServiceException;
import com.aitravel.service.ai.ChatContext;
import com.aitravel.service.ai.ChatTurn;
import com.aitravel.service.ai.ScenicBrief;
import com.aitravel.vo.AdminChatVO;
import com.aitravel.vo.ChatMessageVO;
import com.aitravel.vo.ChatVO;
import com.aitravel.vo.TripDetailVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI 问答服务实现（详细设计 §6.5）。
 *
 * <p>上下文由行程摘要（{@link TripService#getTripSummary}）+ 目的地景点（
 * {@link ScenicService#listBriefByCity}）+ 正序历史对话组成；用户消息<b>先落库</b>，即使 AI 失败也
 * 保留提问记录。
 */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "DI 注入的 Mapper 与上游服务由 Spring 容器持有，此处仅保存引用")
public class ChatServiceImpl implements ChatService {

    /** 历史对话上限（约 10 轮）。 */
    private static final int HISTORY_LIMIT = 20;
    /** 默认页码。 */
    private static final int DEFAULT_PAGE = 1;
    /** 默认每页大小。 */
    private static final int DEFAULT_SIZE = 10;
    /** 每页大小上限。 */
    private static final int MAX_SIZE = 100;

    private final AiChatMapper aiChatMapper;
    private final TripService tripService;
    private final ScenicService scenicService;
    private final AiGateway aiGateway;

    /**
     * 构造服务实现。
     *
     * @param aiChatMapper  对话记录数据访问
     * @param tripService   行程服务
     * @param scenicService 景点服务
     * @param aiGateway     AI 网关
     */
    public ChatServiceImpl(AiChatMapper aiChatMapper, TripService tripService,
                           ScenicService scenicService, AiGateway aiGateway) {
        this.aiChatMapper = aiChatMapper;
        this.tripService = tripService;
        this.scenicService = scenicService;
        this.aiGateway = aiGateway;
    }

    @Override
    public ChatVO chat(Long userId, ChatRequest req) {
        // 1. 行程上下文（含归属校验，越权抛 403）
        String tripSummary = null;
        String scenicSummary = "";
        if (req.getTripId() != null) {
            tripSummary = tripService.getTripSummary(req.getTripId(), userId);
            // 2. 景点上下文：取行程目的地城市（内部同样做归属校验）
            TripDetailVO detail = tripService.getTripDetail(userId, req.getTripId());
            scenicSummary = buildScenicSummary(detail == null ? null : detail.getCity());
        }

        // 3. 历史对话（最近 20 条，倒序查询后反转正序）
        List<ChatTurn> history = new ArrayList<>(aiChatMapper
                .selectRecent(userId, req.getTripId(), HISTORY_LIMIT).stream()
                .map(chat -> new ChatTurn(chat.getRole(), chat.getContent()))
                .collect(Collectors.toList()));
        Collections.reverse(history);

        // 4. 先落库用户消息（即使 AI 失败也保留提问）
        aiChatMapper.insert(AiChat.user(userId, req.getTripId(), req.getMessage()));

        // 5. 调 AI
        String answer;
        try {
            answer = aiGateway.chat(new ChatContext(tripSummary, scenicSummary, history,
                    req.getMessage()));
        } catch (AiServiceException e) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, ErrorMessage.AI_CHAT_UNAVAILABLE);
        }

        // 6. 落库 AI 回答
        AiChat assistant = AiChat.assistant(userId, req.getTripId(), answer);
        aiChatMapper.insert(assistant);

        return new ChatVO(answer, assistant.getCreateAt());
    }

    @Override
    public PageResult<ChatMessageVO> pageHistory(Long userId, Long tripId, int page, int size) {
        Page<AiChat> result = aiChatMapper.selectPage(
                new Page<>(normalizePage(page), normalizeSize(size)),
                new LambdaQueryWrapper<AiChat>()
                        .eq(AiChat::getUserId, userId)
                        .eq(tripId != null, AiChat::getTripId, tripId)
                        .orderByAsc(AiChat::getCreateAt));
        List<ChatMessageVO> records = result.getRecords().stream()
                .map(ChatMessageVO::from)
                .collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<AdminChatVO> pageChatsForAdmin(int page, int size) {
        IPage<AdminChatVO> result = aiChatMapper.selectAdminChatPage(
                new Page<>(normalizePage(page), normalizeSize(size)));
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(),
                result.getSize());
    }

    /**
     * 将目的地城市全部景点拼接为上下文文本；城市为空或无景点时返回空串。
     *
     * @param city 目的地城市，可为 {@code null}
     * @return 景点上下文文本
     */
    private String buildScenicSummary(String city) {
        if (!StringUtils.hasText(city)) {
            return "";
        }
        List<ScenicBrief> briefs = scenicService.listBriefByCity(city);
        if (briefs == null || briefs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ScenicBrief brief : briefs) {
            sb.append("- 名称：").append(blankToDash(brief.name()))
                    .append("；类别：").append(blankToDash(brief.category()))
                    .append("；简介：").append(blankToDash(brief.description()))
                    .append('\n');
        }
        return sb.toString().stripTrailing();
    }

    /**
     * 空值转破折号占位。
     *
     * @param value 原始值
     * @return 非空值或「-」
     */
    private static String blankToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    /**
     * 归一化页码：非法值回退默认页。
     *
     * @param page 原始页码
     * @return 合法页码
     */
    private static int normalizePage(int page) {
        return page < 1 ? DEFAULT_PAGE : page;
    }

    /**
     * 归一化每页大小：非法值回退默认值，超上限截断。
     *
     * @param size 原始大小
     * @return 合法大小
     */
    private static int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
