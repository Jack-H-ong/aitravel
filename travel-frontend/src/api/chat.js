/**
 * AI 问答接口（F-API，对应后端 `ChatController`，前缀 `/api/chat`）。
 *
 * <p>需登录；AI 失败时后端返回 `code=5001`。具体页面逻辑由 f-chat 模块补齐。
 */
import request from '@/utils/request'

/**
 * 发送提问（AI，需登录）。
 *
 * @param {{message:string,tripId?:string|number}} data 提问请求体（tripId 可选，字符串 Long）
 * @returns {Promise<object>} ChatVO（含 answer）
 */
export function sendChat(data) {
  return request.post('/chat', data)
}

/**
 * 对话历史分页（需登录）。
 *
 * @param {{tripId?:string|number,page?:number,size?:number}} [params] 不传 tripId 为全局历史
 * @returns {Promise<{records:Array,total:string,page:string,size:string}>}
 */
export function getChatHistory(params) {
  return request.get('/chat/history', { params })
}
