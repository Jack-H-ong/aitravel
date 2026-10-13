<script setup>
/**
 * 行程时间轴（F-COMPONENT）。
 *
 * <p>按 `dayList` 渲染每日（`el-timeline`），每日展示：主题、午餐 / 晚餐、
 * 当日提示，以及当日景点列表（按 `visitOrder` 顺序，含停留分钟与游玩提示）。
 *
 * <p>`dayList` 由后端保证按 `dayNo` 升序、`spots` 按 `visitOrder` 升序；
 * 组件仅做兜底排序，**不修改**传入数据。
 *
 * <p>AI 文本（theme / lunch / dinner / tip / spotName）一律 `{{ }}` 纯文本插值，
 * 禁用 v-html。
 */
import { computed } from 'vue'
import { emptyText } from '@/utils/format'

const props = defineProps({
  /** 逐日安排数组（TripDayVO[]）。 */
  dayList: {
    type: Array,
    default: () => []
  }
})

/** 归一化并排序后的每日数据（dayNo 升序、spots 按 visitOrder 升序）。 */
const days = computed(() => {
  const list = Array.isArray(props.dayList) ? props.dayList : []
  return [...list]
    .sort((a, b) => (Number(a.dayNo) || 0) - (Number(b.dayNo) || 0))
    .map((day) => {
      const spots = Array.isArray(day.spots) ? [...day.spots] : []
      spots.sort((a, b) => (Number(a.visitOrder) || 0) - (Number(b.visitOrder) || 0))
      return { ...day, spots }
    })
})

/** 停留时长文案（分钟，空值兜底）。 */
function stayText(minutes) {
  const n = Number(minutes)
  return Number.isFinite(n) && n > 0 ? `${n} 分钟` : ''
}
</script>

<template>
  <el-timeline class="trip-timeline">
    <el-timeline-item
      v-for="day in days"
      :key="day.dayNo"
      type="primary"
      :hollow="false"
    >
      <div class="trip-timeline__day">
        <h3 class="trip-timeline__dayno">第 {{ Number(day.dayNo) || 0 }} 天</h3>
        <p class="trip-timeline__theme">{{ emptyText(day.theme, '自由活动') }}</p>

        <ul class="trip-timeline__meals">
          <li>
            <span class="trip-timeline__label">午餐</span>
            <span>{{ emptyText(day.lunch) }}</span>
          </li>
          <li>
            <span class="trip-timeline__label">晚餐</span>
            <span>{{ emptyText(day.dinner) }}</span>
          </li>
        </ul>

        <el-alert
          v-if="day.tip"
          class="trip-timeline__tip"
          :title="day.tip"
          type="info"
          :closable="false"
          show-icon
        />

        <div class="trip-timeline__spots">
          <div class="trip-timeline__spots-title text-secondary">
            景点安排（{{ day.spots.length }}）
          </div>

          <ol v-if="day.spots.length" class="trip-timeline__spot-list">
            <li
              v-for="(spot, idx) in day.spots"
              :key="`${day.dayNo}-${spot.visitOrder}-${idx}`"
              class="trip-timeline__spot"
            >
              <span class="trip-timeline__order">{{ Number(spot.visitOrder) || idx + 1 }}</span>
              <div class="trip-timeline__spot-body">
                <div class="trip-timeline__spot-name">
                  {{ emptyText(spot.spotName, '未命名景点') }}
                  <span v-if="stayText(spot.stayMinutes)" class="trip-timeline__stay">
                    · 建议停留 {{ stayText(spot.stayMinutes) }}
                  </span>
                </div>
                <div v-if="spot.tip" class="trip-timeline__spot-tip text-secondary">
                  {{ spot.tip }}
                </div>
              </div>
            </li>
          </ol>

          <p v-else class="trip-timeline__empty text-secondary">当日暂无景点安排</p>
        </div>
      </div>
    </el-timeline-item>
  </el-timeline>
</template>

<style scoped>
.trip-timeline {
  padding-left: 4px;
}

.trip-timeline__dayno {
  margin: 0 0 4px;
  font-size: 16px;
}

.trip-timeline__theme {
  color: var(--el-color-primary);
  font-weight: 500;
  margin-bottom: 12px;
}

.trip-timeline__meals {
  margin-bottom: 12px;
}

.trip-timeline__meals li {
  display: flex;
  gap: 12px;
  padding: 2px 0;
  font-size: 14px;
}

.trip-timeline__label {
  flex: 0 0 auto;
  width: 40px;
  color: var(--el-text-color-secondary);
}

.trip-timeline__tip {
  margin-bottom: 12px;
}

.trip-timeline__spots-title {
  font-size: 12px;
  margin-bottom: 8px;
}

.trip-timeline__spot-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.trip-timeline__spot {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.trip-timeline__order {
  flex: 0 0 auto;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 1px;
}

.trip-timeline__spot-body {
  flex: 1;
}

.trip-timeline__spot-name {
  font-size: 14px;
  font-weight: 500;
}

.trip-timeline__stay {
  font-weight: 400;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.trip-timeline__spot-tip {
  font-size: 12px;
}

.trip-timeline__empty {
  font-size: 13px;
}
</style>
