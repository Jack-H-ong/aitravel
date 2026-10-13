/**
 * 对话消息状态（F-STORE）。
 *
 * <p>持有当前对话的消息列表、关联行程与发送状态，并提供乐观发送 action（f-chat）。
 *
 * <p>安全红线：消息内容一律**纯文本渲染**（禁用 v-html）。
 */
import { defineStore } from 'pinia'
import { sendChat } from '@/api/chat'

/**
 * 对话 store。
 */
export const useChatStore = defineStore('chat', {
  state: () => ({
    /** 当前关联行程 id（字符串 Long）；null 表示全局问答。 */
    tripId: null,
    /** 消息列表：[{ role:'user'|'assistant', content:string, time:string }]。 */
    messages: [],
    /** AI 请求是否进行中。 */
    loading: false
  }),

  getters: {
    /** 消息条数。 */
    count: (state) => state.messages.length,
    /** 是否为空对话。 */
    isEmpty: (state) => state.messages.length === 0
  },

  actions: {
    /**
     * 设置当前关联的行程。
     *
     * @param {string|number|null} tripId 行程 id
     */
    setTripId(tripId) {
      this.tripId = tripId === undefined || tripId === null ? null : String(tripId)
    },

    /**
     * 追加一条消息。
     *
     * @param {{role:string,content:string,time?:string}} message
     */
    addMessage(message) {
      if (!message) {
        return
      }
      this.messages.push(message)
    },

    /**
     * 批量设置消息（如加载历史）。
     *
     * @param {Array} messages
     */
    setMessages(messages) {
      this.messages = Array.isArray(messages) ? messages : []
    },

    /**
     * 清空对话。
     */
    clear() {
      this.messages = []
    },

    /**
     * 发送提问（乐观更新）。
     *
     * <p>流程：
     * <ol>
     *   <li>**立即**追加 user 气泡（乐观），再追加一个 assistant 占位（loading）气泡；</li>
     *   <li>置 `loading`，携带当前 `tripId` 调用 {@link sendChat}；</li>
     *   <li>成功 → 用真实回答**替换**占位气泡；</li>
     *   <li>失败（含 5001/403）→ **移除占位气泡**，user 气泡保留不丢；错误提示由
     *       `request` 拦截器统一展示，本 action 不重复提示。</li>
     * </ol>
     *
     * @param {string} message 用户提问文本
     * @returns {Promise<{ok:boolean, data?:object, error?:Error, reason?:string}>}
     *          结果对象；`reason='empty'` 表示空消息未发送
     */
    async send(message) {
      const content = typeof message === 'string' ? message.trim() : ''
      if (!content) {
        return { ok: false, reason: 'empty' }
      }
      // 记录本次发送归属的行程，避免请求期间切换行程导致串话
      const targetTripId = this.tripId

      // 1) 乐观追加：user 气泡 + assistant loading 占位气泡
      this.messages.push({ role: 'user', content, time: '' })
      const placeholder = { role: 'assistant', content: '', time: '', loading: true }
      this.messages.push(placeholder)
      this.loading = true

      try {
        const data = await sendChat({
          message: content,
          tripId: targetTripId || undefined
        })
        // 3) 替换占位气泡为真实回答
        const idx = this.messages.indexOf(placeholder)
        const answer = {
          role: 'assistant',
          content: (data && data.answer) || '',
          time: (data && data.createdAt) || '',
          loading: false
        }
        if (idx !== -1) {
          this.messages.splice(idx, 1, answer)
        } else {
          this.messages.push(answer)
        }
        return { ok: true, data }
      } catch (error) {
        // 4) 失败：移除占位气泡，保留 user 气泡
        const idx = this.messages.indexOf(placeholder)
        if (idx !== -1) {
          this.messages.splice(idx, 1)
        }
        return { ok: false, error }
      } finally {
        this.loading = false
      }
    }
  }
})
