<script setup>
/**
 * 行程卡片（F-COMPONENT）。
 *
 * <p>展示目的地行程概览：标题、城市 · 天数、预算、消费水平、偏好标签、创建时间。
 * 卡片点击派发 `view` 事件（由父级跳转详情）；右上「删除」按钮派发 `remove`
 * （点击已 `stop`，不影响卡片跳转）。
 *
 * <p>所有文本均为纯文本渲染（禁用 v-html）。
 */
import { computed } from 'vue'
import { formatDate, emptyText } from '@/utils/format'

const props = defineProps({
  /** 行程 VO（id 为字符串）。 */
  trip: {
    type: Object,
    required: true
  }
})

/** 事件：`view` 查看详情、`remove` 删除。 */
const emit = defineEmits(['view', 'remove'])

/** 偏好标签列表。 */
const preferences = computed(() => {
  const list = props.trip && props.trip.preferences
  return Array.isArray(list) ? list.filter((p) => !!p) : []
})

/** 创建时间文案。 */
const createAtText = computed(() => formatDate(props.trip.createAt, '-'))

/** 删除中（由父级控制的消息确认，此处只做事件派发）。 */
function handleRemove() {
  emit('remove', props.trip)
}
</script>

<template>
  <el-card class="trip-card" shadow="hover" @click="emit('view', trip)">
    <div class="trip-card__head">
      <h3 class="trip-card__title text-ellipsis">
        {{ emptyText(trip.title, '未命名行程') }}
      </h3>
      <el-button
        class="trip-card__remove"
        link
        type="danger"
        @click.stop="handleRemove"
      >
        删除
      </el-button>
    </div>

    <div class="trip-card__meta text-secondary">
      <span>{{ emptyText(trip.city) }}</span>
      <span class="trip-card__dot">·</span>
      <span>{{ Number(trip.days) || 0 }} 天</span>
      <span class="trip-card__dot">·</span>
      <span>{{ emptyText(trip.spendMode, '未指定') }}</span>
    </div>

    <div class="trip-card__facts">
      <span class="trip-card__label">预算</span>
      <span>{{ emptyText(trip.budget, '未设定') }}</span>
    </div>

    <div v-if="preferences.length" class="trip-card__tags">
      <el-tag
        v-for="p in preferences"
        :key="p"
        type="info"
        effect="plain"
        size="small"
      >
        {{ p }}
      </el-tag>
    </div>

    <div class="trip-card__footer text-secondary">
      <span>创建于 {{ createAtText }}</span>
      <span class="trip-card__more">查看详情 ›</span>
    </div>
  </el-card>
</template>

<style scoped>
.trip-card {
  cursor: pointer;
}

.trip-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.trip-card__title {
  margin: 0;
  font-size: 16px;
  flex: 1;
}

.trip-card__remove {
  flex: 0 0 auto;
}

.trip-card__meta {
  font-size: 12px;
  margin: 6px 0 10px;
}

.trip-card__dot {
  margin: 0 6px;
}

.trip-card__facts {
  display: flex;
  gap: 12px;
  font-size: 14px;
  margin-bottom: 10px;
}

.trip-card__label {
  color: var(--el-text-color-secondary);
  flex: 0 0 auto;
}

.trip-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.trip-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 10px;
}

.trip-card__more {
  color: var(--el-color-primary);
}
</style>
