<script setup>
/**
 * 行程详情页（F-VIEW，路由 `/trip/:id`，需登录）。
 *
 * <p>展示行程概览（标题 / 城市 / 天数 / 预算 / 消费水平 / 偏好 / 创建时间）与
 * {@link TripTimeline} 时间轴（按 `dayList` 渲染每日主题、午餐 / 晚餐、当日提示、
 * 景点顺序 / 停留时间 / 游玩提示）。
 *
 * <p>「编辑」跳 `/trip/:id/edit`；「删除」经 `ElMessageBox.confirm` 确认后删除并回列表。
 * 详情命中 403（非本人）/ 404（不存在）时展示空状态，文案由 `request` 拦截器提示。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTripDetail, deleteTrip } from '@/api/trip'
import { useTripStore } from '@/store/trip'
import { formatDate, emptyText } from '@/utils/format'
import TripTimeline from '@/components/TripTimeline.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const tripStore = useTripStore()

/** 加载态。 */
const loading = ref(false)
/** 详情数据（TripDetailVO）。 */
const detail = ref(null)
/** 加载失败（403/404）。 */
const loadFailed = ref(false)
/** 删除请求进行中。 */
const deleting = ref(false)

/** 逐日安排。 */
const dayList = computed(() => (detail.value && detail.value.dayList) || [])

/** 偏好标签。 */
const preferences = computed(() => {
  const list = detail.value && detail.value.preferences
  return Array.isArray(list) ? list.filter((p) => !!p) : []
})

/** 创建时间文案。 */
const createAtText = computed(() => formatDate(detail.value && detail.value.createAt, '-'))

/** 拉取详情。 */
async function fetchDetail() {
  loading.value = true
  loadFailed.value = false
  try {
    const data = await getTripDetail(route.params.id)
    if (data) {
      detail.value = data
      tripStore.setCurrent(data)
    } else {
      detail.value = null
      loadFailed.value = true
    }
  } catch (e) {
    // 403/404 文案由 request 拦截器统一展示
    detail.value = null
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

/** 编辑行程。 */
function goEdit() {
  router.push(`/trip/${route.params.id}/edit`)
}

/** 删除行程：确认后删除并回列表。 */
async function handleDelete() {
  if (deleting.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      '确定删除该行程吗？删除后不可恢复。',
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  deleting.value = true
  try {
    await deleteTrip(route.params.id)
    ElMessage.success('已删除')
    router.push('/trip/list')
  } catch (e) {
    // 403/404 由 request 拦截器统一提示
  } finally {
    deleting.value = false
  }
}

/** 返回列表。 */
function goList() {
  router.push('/trip/list')
}

onMounted(fetchDetail)
</script>

<template>
  <div class="trip-detail page">
    <el-button class="trip-detail__back" text @click="goList">‹ 返回行程列表</el-button>

    <el-skeleton v-if="loading" :rows="8" animated />

    <EmptyState
      v-else-if="loadFailed || !detail"
      description="行程不存在或无权访问"
    >
      <el-button type="primary" @click="goList">返回行程列表</el-button>
    </EmptyState>

    <template v-else>
      <el-card class="trip-detail__card" shadow="never">
        <div class="trip-detail__head">
          <div class="trip-detail__headline">
            <h1 class="trip-detail__title">{{ emptyText(detail.title, '未命名行程') }}</h1>
            <div class="trip-detail__meta text-secondary">
              <span>{{ emptyText(detail.city) }}</span>
              <span class="trip-detail__dot">·</span>
              <span>{{ Number(detail.days) || 0 }} 天</span>
              <span class="trip-detail__dot">·</span>
              <span>{{ emptyText(detail.spendMode, '未指定') }}</span>
              <span class="trip-detail__dot">·</span>
              <span>创建于 {{ createAtText }}</span>
            </div>
          </div>
          <div class="trip-detail__actions">
            <el-button type="primary" @click="goEdit">编辑</el-button>
            <el-button type="danger" :loading="deleting" @click="handleDelete">
              删除
            </el-button>
          </div>
        </div>

        <ul class="trip-detail__facts">
          <li>
            <span class="trip-detail__label">预算</span>
            <span>{{ emptyText(detail.budget, '未设定') }}</span>
          </li>
          <li>
            <span class="trip-detail__label">偏好</span>
            <span v-if="preferences.length" class="trip-detail__tags">
              <el-tag
                v-for="p in preferences"
                :key="p"
                type="info"
                effect="plain"
                size="small"
              >
                {{ p }}
              </el-tag>
            </span>
            <span v-else class="text-secondary">未设置</span>
          </li>
        </ul>
      </el-card>

      <el-card class="trip-detail__card" shadow="never">
        <template #header>行程安排</template>
        <TripTimeline v-if="dayList.length" :day-list="dayList" />
        <p v-else class="text-secondary">该行程暂无逐日安排</p>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.trip-detail {
  padding-top: 8px;
}

.trip-detail__back {
  margin-bottom: 8px;
}

.trip-detail__card {
  margin-bottom: 20px;
}

.trip-detail__head {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.trip-detail__title {
  font-size: 22px;
  margin: 0 0 6px;
}

.trip-detail__meta {
  font-size: 13px;
}

.trip-detail__dot {
  margin: 0 6px;
}

.trip-detail__actions {
  display: flex;
  gap: 12px;
  flex: 0 0 auto;
}

.trip-detail__facts {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
}

.trip-detail__facts li {
  display: flex;
  gap: 12px;
  padding: 4px 0;
  font-size: 14px;
}

.trip-detail__label {
  flex: 0 0 auto;
  width: 48px;
  color: var(--el-text-color-secondary);
}

.trip-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
