<script setup>
/**
 * 提问输入区（F-COMPONENT）。
 *
 * <p>`el-input` textarea + 发送按钮：
 * <ul>
 *   <li>`maxlength=500` 且展示字数统计（与服务端 `@Size(max=500)` 对齐）；</li>
 *   <li>发送中禁用输入与按钮，避免重复提交；</li>
 *   <li>Enter 发送、Shift+Enter 换行（输入法组合中不触发发送）。</li>
 * </ul>
 *
 * <p>本组件只负责收集输入与触发 `send` 事件；空/超长校验与请求由页面处理。
 */
import { computed } from 'vue'

const props = defineProps({
  /** 输入框内容（v-model）。 */
  modelValue: {
    type: String,
    default: ''
  },
  /** 发送中：禁用输入与按钮。 */
  loading: {
    type: Boolean,
    default: false
  },
  /** 最大字数。 */
  maxlength: {
    type: Number,
    default: 500
  }
})

const emit = defineEmits(['update:modelValue', 'send'])

/** 是否可发送：非空且未在发送中。 */
const canSend = computed(() => !props.loading && props.modelValue.trim().length > 0)

/**
 * 输入变更。
 *
 * @param {string} value 新值
 */
function handleInput(value) {
  emit('update:modelValue', value)
}

/**
 * 触发发送（由父级做最终校验）。
 */
function handleSend() {
  if (props.loading) {
    return
  }
  emit('send')
}

/**
 * 键盘事件：Enter 发送，Shift+Enter 换行，输入法组合中忽略。
 *
 * @param {KeyboardEvent} event
 */
function handleKeydown(event) {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) {
    return
  }
  event.preventDefault()
  handleSend()
}
</script>

<template>
  <div class="chat-input">
    <el-input
      :model-value="modelValue"
      type="textarea"
      :rows="3"
      resize="none"
      :maxlength="maxlength"
      show-word-limit
      :disabled="loading"
      placeholder="输入你的旅游问题，Enter 发送，Shift+Enter 换行（不超过 500 字）"
      @update:model-value="handleInput"
      @keydown="handleKeydown"
    />
    <div class="chat-input__actions">
      <el-button type="primary" :disabled="!canSend" @click="handleSend">
        {{ loading ? '回答中…' : '发送' }}
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.chat-input {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
}

.chat-input__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
