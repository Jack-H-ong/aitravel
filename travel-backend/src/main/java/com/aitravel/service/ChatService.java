package com.aitravel.service;

import com.aitravel.common.PageResult;
import com.aitravel.dto.ChatRequest;
import com.aitravel.vo.AdminChatVO;
import com.aitravel.vo.ChatMessageVO;
import com.aitravel.vo.ChatVO;

/**
 * AI 问答服务（详细设计 §6.2）。
 *
 * <p>对外提供问答（构造上下文 → 调 AI → 落库 user/assistant 两条记录）、对话历史查询，以及供
 * M-ADMIN 使用的对话记录查询。
 *
 * @see com.aitravel.service.impl.ChatServiceImpl
 */
public interface ChatService {

    /**
     * 智能问答：构造上下文 → 调 AI → 落库 user/assistant 两条记录。
     *
     * <p>先落库用户提问（即使 AI 失败也保留），AI 成功后落库回答并返回其 {@code createAt}。
     *
     * @param userId 当前用户 ID
     * @param req    问答请求
     * @return 回答文本与落库时间
     * @throws com.aitravel.common.BusinessException 行程不属于当前用户（403）或 AI 异常（5001）
     */
    ChatVO chat(Long userId, ChatRequest req);

    /**
     * 分页查询当前用户的对话历史，按创建时间正序。
     *
     * @param userId 当前用户 ID
     * @param tripId 行程 ID 过滤，可选
     * @param page   页码，从 1 开始
     * @param size   每页大小
     * @return 对话历史分页
     */
    PageResult<ChatMessageVO> pageHistory(Long userId, Long tripId, int page, int size);

    /**
     * 后台分页查询全部对话（关联归属用户名，供 M-ADMIN）。
     *
     * @param page 页码，从 1 开始
     * @param size 每页大小
     * @return 对话分页
     */
    PageResult<AdminChatVO> pageChatsForAdmin(int page, int size);
}
