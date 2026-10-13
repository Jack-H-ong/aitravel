package com.aitravel.service.ai;

/**
 * 单条历史对话（详细设计 §2.4.2）。
 *
 * @param role 角色（user / assistant）
 * @param content 内容
 */
public record ChatTurn(String role, String content) {
}
