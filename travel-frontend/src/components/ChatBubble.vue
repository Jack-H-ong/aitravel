<script setup>
/**
 * 对话气泡（F-COMPONENT）。
 *
 * <p>按 `role` 区分：`user` 靠右（主色），`assistant` 靠左（浅灰）；
 * `loading` 为真时展示「正在思考」的等待态（用于发送后的占位气泡）。
 *
 * <p>安全红线：内容一律 `{{ }}` 纯文本插值渲染，**禁用 v-html**；
 * 换行依靠 CSS `white-space: pre-wrap` 呈现，不做任何 HTML 注入。
 */
import { computed } from 'vue'
import { formatDate } from '@/utils/format'

const props = defineProps({
  /** 消息：{ role:'user'|'assistant', content:string, time?:string, loading?:boolean } */
  message: {
    type: Object,
    required: true
  }
})

/** 是否为用户消息（决定左右与配色）。 */
const isUser = computed(() => props.message.role === 'user')
/** 是否处于等待回答态。 */
const isLoading = computed(() => !!props.message.loading)
/** 展示时间（后端 `yyyy-MM-dd HH:mm:ss`，直读；为空则不展示）。 */
const displayTime = computed(() => formatDate(props.message.time, ''))

/** 头像兜底文字。 */
const avatarText = computed(() => (isUser.value ? '我' : 'AI'))
</script>

<template>
  <div class="bubble" :class="isUser ? 'bubble--user' : 'bubble--assistant'">
    <div class="bubble__avatar" :class="isUser ? 'bubble__avatar--user' : 'bubble__avatar--ai'">
      {{ avatarText }}
    </div>
    <div class="bubble__body">
      <div class="bubble__content">
        <span v-if="isLoading" class="bubble__loading">
          <span class="bubble__dot" />
          <span class="bubble__dot" />
          <span class="bubble__dot" />
        </span>
        <span v-else class="bubble__text">{{ message.content }}</span>
      </div>
      <div v-if="!isLoading && displayTime" class="bubble__time">{{ displayTime }}</div>
    </div>
  </div>
</template>

<style scoped>
.bubble {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  align-items: flex-start;
}

.bubble--user {
  flex-direction: row-reverse;
}

.bubble__avatar {
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #fff;
}

.bubble__avatar--user {
  background: var(--el-color-primary);
}

.bubble__avatar--ai {
  background: var(--el-color-success);
}

.bubble__body {
  max-width: 72%;
  display: flex;
  flex-direction: column;
}

.bubble--user .bubble__body {
  align-items: flex-end;
}

.bubble__content {
  padding: 10px 14px;
  border-radius: var(--app-radius);
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.bubble--assistant .bubble__content {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  border-top-left-radius: 2px;
}

.bubble--user .bubble__content {
  background: var(--el-color-primary);
  color: #fff;
  border-top-right-radius: 2px;
}

/* 纯文本渲染：保留换行与空白，且不解析 HTML */
.bubble__text {
  white-space: pre-wrap;
}

.bubble__time {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.bubble__loading {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  height: 20px;
}

.bubble__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-text-color-secondary);
  animation: bubble-blink 1.2s infinite ease-in-out;
}

.bubble__dot:nth-child(2) {
  animation-delay: 0.2s;
}

.bubble__dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bubble-blink {
  0%,
  80%,
  100% {
    opacity: 0.3;
  }
  40% {
    opacity: 1;
  }
}
</style>
