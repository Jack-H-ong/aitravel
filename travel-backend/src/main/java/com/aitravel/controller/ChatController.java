package com.aitravel.controller;

import com.aitravel.common.PageResult;
import com.aitravel.common.Result;
import com.aitravel.dto.ChatRequest;
import com.aitravel.security.UserContext;
import com.aitravel.service.ChatService;
import com.aitravel.vo.ChatMessageVO;
import com.aitravel.vo.ChatVO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 问答接口（详细设计 §6.3）。
 *
 * <p>全部接口需登录，{@code userId} 取自 {@link UserContext}。
 */
@RestController
@RequestMapping("/api/chat")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "DI 注入的 ChatService 由 Spring 容器持有，此处仅保存引用")
public class ChatController {

    private final ChatService chatService;

    /**
     * 构造控制器。
     *
     * @param chatService 问答服务
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 智能问答（§6.3.1，需登录）。
     *
     * @param request 问答请求
     * @return 回答文本与落库时间
     */
    @PostMapping
    public Result<ChatVO> chat(@Valid @RequestBody ChatRequest request) {
        return Result.ok(chatService.chat(UserContext.getUserId(), request));
    }

    /**
     * 对话历史（§6.3.2，需登录）。
     *
     * @param tripId 行程 ID 过滤，可选
     * @param page   页码，从 1 开始，默认 1
     * @param size   每页大小，默认 10
     * @return 对话历史分页
     */
    @GetMapping("/history")
    public Result<PageResult<ChatMessageVO>> history(
            @RequestParam(required = false) Long tripId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(chatService.pageHistory(UserContext.getUserId(), tripId, page, size));
    }
}
