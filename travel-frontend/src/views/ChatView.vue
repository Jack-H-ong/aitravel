<script setup>
/**
 * AI 智能问答页（F-VIEW，路由 `/chat`，需登录）。
 *
 * <p>功能：
 * <ol>
 *   <li>顶部「关联行程」下拉（`listTrips` 拉取，含「全局问答」选项，选它则不带 tripId）；</li>
 *   <li>进入页面按当前 tripId（优先取路由 `?tripId=`，其次 store）调 `getChatHistory` 渲染历史，
 *       切换行程时重新加载；</li>
 *   <li>对话气泡列表（user 靠右 / assistant 靠左）+ 输入区；发送后**立即**显示 user 气泡与
 *       loading 气泡，返回后替换为 assistant 气泡，并自动滚动到底部；</li>
 *   <li>空消息 / 超 500 字前端拦截（`ElMessage.warning`，不发请求）；失败时保留用户消息，
 *       错误提示（5001「问答服务暂时不可用，请稍后重试」、403 越权）由 request 拦截器统一展示。</li>
 * </ol>
 */
import { nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listTrips } from '@/api/trip'
import { getChatHistory } from '@/api/chat'
import { useChatStore } from '@/store/chat'
import ChatBubble from '@/components/ChatBubble.vue'
import ChatInput from '@/components/ChatInput.vue'
import EmptyState from '@/components/EmptyState.vue'

/** 单次发送最大字数（与服务端 @Size(max=500) 对齐）。 */
const MAX_LENGTH = 500
/** 历史一次拉取条数（取上限，页面内不做分页）。 */
const HISTORY_SIZE = 100

const route = useRoute()
const chatStore = useChatStore()

/** 消息滚动容器。 */
const scrollEl = ref(null)
/** 输入框内容。 */
const inputText = ref('')
/** 关联行程下拉的当前值（'' 表示全局问答）。 */
const currentTripId = ref('')
/** 行程下拉选项。 */
const tripOptions = ref([])
/** 历史加载态。 */
const historyLoading = ref(false)

/**
 * 滚动消息容器到底部。
 */
async function scrollToBottom() {
  await nextTick()
  const el = scrollEl.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

/**
 * 拉取行程下拉选项（失败时静默，仅保留「全局问答」）。
 */
async function fetchTripOptions() {
  try {
    const data = await listTrips({ page: 1, size: 100 })
    tripOptions.value = (data && data.records) || []
  } catch (e) {
    tripOptions.value = []
  }
}

/**
 * 按当前 tripId 加载对话历史并渲染。
 */
async function loadHistory() {
  historyLoading.value = true
  try {
    const data = await getChatHistory({
      tripId: currentTripId.value || undefined,
      page: 1,
      size: HISTORY_SIZE
    })
    const records = (data && data.records) || []
    // 归一化为 store 的消息结构（后端字段 content / createAt，按时间升序返回）
    chatStore.setMessages(
      records.map((item) => ({
        id: item.id,
        tripId: item.tripId,
        role: item.role,
        content: item.content,
        time: item.createAt
      }))
    )
  } catch (e) {
    // 错误提示由 request 拦截器统一展示
    chatStore.setMessages([])
  } finally {
    historyLoading.value = false
    scrollToBottom()
  }
}

/**
 * 切换关联行程：同步 store 并重新加载该行程历史。
 *
 * @param {string} value 行程 id（'' 表示全局）
 */
function handleTripChange(value) {
  currentTripId.value = value || ''
  chatStore.setTripId(currentTripId.value || null)
  loadHistory()
}

/**
 * 发送提问：前端校验（空 / 超 500 字）→ 交给 store 乐观发送。
 */
async function handleSend() {
  const content = inputText.value.trim()
  if (!content) {
    ElMessage.warning('请输入问题内容')
    return
  }
  if (content.length > MAX_LENGTH) {
    ElMessage.warning(`问题不能超过 ${MAX_LENGTH} 字`)
    return
  }
  // 先清空输入，消息已进入列表（失败时用户气泡仍保留，不会丢）
  inputText.value = ''
  scrollToBottom()
  // 失败（5001/403 等）：store 已移除占位气泡、保留 user 气泡；提示由拦截器统一展示
  await chatStore.send(content)
  scrollToBottom()
}

/** 消息数量变化（乐观追加 / 替换 / 加载历史）→ 自动滚到底部。 */
watch(
  () => chatStore.messages.length,
  () => scrollToBottom()
)

onMounted(async () => {
  // 初始化 tripId：优先路由 query，其次 store 既有值
  const queryTripId = route.query.tripId != null ? String(route.query.tripId) : ''
  currentTripId.value = queryTripId || chatStore.tripId || ''
  chatStore.setTripId(currentTripId.value || null)
  await fetchTripOptions()
  await loadHistory()
})
</script>

<template>
  <div class="chat page">
    <header class="chat__head">
      <h1 class="chat__title">AI 智能问答</h1>
      <div class="chat__trip">
        <span class="chat__trip-label">关联行程</span>
        <el-select
          v-model="currentTripId"
          placeholder="选择关联行程"
          class="chat__trip-select"
          @change="handleTripChange"
        >
          <el-option label="全局问答（不限行程）" value="" />
          <el-option
            v-for="trip in tripOptions"
            :key="trip.id"
            :label="trip.title || trip.city || '未命名行程'"
            :value="String(trip.id)"
          />
        </el-select>
      </div>
    </header>

    <div ref="scrollEl" v-loading="historyLoading" class="chat__messages">
      <EmptyState
        v-if="!historyLoading && chatStore.messages.length === 0"
        description="还没有对话，向 AI 提问开启你的旅程吧"
      />
      <template v-else>
        <ChatBubble
          v-for="(msg, index) in chatStore.messages"
          :key="msg.id || index"
          :message="msg"
        />
      </template>
    </div>

    <ChatInput v-model="inputText" :loading="chatStore.loading" @send="handleSend" />
  </div>
</template>

<style scoped>
.chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 92px);
}

.chat__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.chat__title {
  font-size: 22px;
  margin: 0;
}

.chat__trip {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat__trip-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.chat__trip-select {
  width: 240px;
}

.chat__messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius);
  background: #fff;
  margin-bottom: 12px;
}
</style>
