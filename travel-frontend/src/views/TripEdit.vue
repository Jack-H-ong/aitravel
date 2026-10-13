<script setup>
/**
 * 编辑行程页（F-VIEW，路由 `/trip/:id/edit`，需登录）。
 *
 * <p>先拉取详情回填，再编辑：
 * <ul>
 *   <li>标题（可选）；</li>
 *   <li>按天编辑主题 / 午餐 / 晚餐 / 当日提示；</li>
 *   <li>每日景点支持**增删、上移 / 下移、编辑字段**（`spotName` 必填）；</li>
 *   <li>保存时组装 `UpdateTripRequest`，`visitOrder` 按数组顺序**重算为 1..n**，
 *       `scenicId` 可空（保留原值，不编辑）。成功后跳回 `/trip/:id`。</li>
 * </ul>
 *
 * <p>所有文本均为纯文本渲染（禁用 v-html）。
 */
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTripDetail, updateTrip } from '@/api/trip'
import { emptyText } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

/** 加载态。 */
const loading = ref(false)
/** 保存中（防重复提交）。 */
const saving = ref(false)
/** 加载失败（403/404）。 */
const loadFailed = ref(false)

/** 行程标题。 */
const title = ref('')
/** 可编辑的逐日安排：[{ dayNo, theme, lunch, dinner, tip, spots:[{scenicId, spotName, stayMinutes, tip}] }]。 */
const days = ref([])

/**
 * 将详情映射为可编辑模型（保留 scenicId；spots 顺序即 visitOrder 顺序）。
 *
 * @param {object} detail TripDetailVO
 */
function buildModel(detail) {
  title.value = detail.title || ''
  const list = Array.isArray(detail.dayList) ? detail.dayList : []
  days.value = [...list]
    .sort((a, b) => (Number(a.dayNo) || 0) - (Number(b.dayNo) || 0))
    .map((day) => {
      const spots = Array.isArray(day.spots) ? [...day.spots] : []
      spots.sort((a, b) => (Number(a.visitOrder) || 0) - (Number(b.visitOrder) || 0))
      return {
        dayNo: day.dayNo,
        theme: day.theme || '',
        lunch: day.lunch || '',
        dinner: day.dinner || '',
        tip: day.tip || '',
        spots: spots.map((s) => ({
          scenicId: s.scenicId === undefined ? null : s.scenicId,
          spotName: s.spotName || '',
          stayMinutes: s.stayMinutes === undefined || s.stayMinutes === null ? '' : s.stayMinutes,
          tip: s.tip || ''
        }))
      }
    })
}

/** 拉取详情并回填。 */
async function fetchDetail() {
  loading.value = true
  loadFailed.value = false
  try {
    const data = await getTripDetail(route.params.id)
    if (data) {
      buildModel(data)
    } else {
      loadFailed.value = true
    }
  } catch (e) {
    // 403/404 文案由 request 拦截器统一展示
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

/**
 * 新增一个景点（追加到当日末尾）。
 *
 * @param {object} day 当日模型
 */
function addSpot(day) {
  day.spots.push({ scenicId: null, spotName: '', stayMinutes: 60, tip: '' })
}

/**
 * 删除景点。
 *
 * @param {object} day 当日模型
 * @param {number} index 下标
 */
function removeSpot(day, index) {
  day.spots.splice(index, 1)
}

/**
 * 上移景点（交换后由数组顺序决定 visitOrder）。
 *
 * @param {object} day 当日模型
 * @param {number} index 当前下标
 */
function moveUp(day, index) {
  if (index <= 0) {
    return
  }
  const arr = day.spots
  ;[arr[index - 1], arr[index]] = [arr[index], arr[index - 1]]
}

/**
 * 下移景点。
 *
 * @param {object} day 当日模型
 * @param {number} index 当前下标
 */
function moveDown(day, index) {
  const arr = day.spots
  if (index >= arr.length - 1) {
    return
  }
  ;[arr[index + 1], arr[index]] = [arr[index], arr[index + 1]]
}

/** 校验：所有景点 spotName 非空。 */
function validate() {
  for (const day of days.value) {
    for (let i = 0; i < day.spots.length; i += 1) {
      const name = String(day.spots[i].spotName || '').trim()
      if (!name) {
        ElMessage.warning(`第 ${Number(day.dayNo) || '?'} 天的第 ${i + 1} 个景点名称不能为空`)
        return false
      }
    }
  }
  return true
}

/** 组装 UpdateTripRequest（visitOrder 重算为 1..n）。 */
function buildPayload() {
  return {
    title: title.value.trim() || undefined,
    dayList: days.value.map((day) => ({
      dayNo: Number(day.dayNo),
      theme: day.theme || undefined,
      lunch: day.lunch || undefined,
      dinner: day.dinner || undefined,
      tip: day.tip || undefined,
      spots: day.spots.map((spot, index) => ({
        scenicId:
          spot.scenicId === null || spot.scenicId === undefined || spot.scenicId === ''
            ? undefined
            : spot.scenicId,
        spotName: String(spot.spotName).trim(),
        visitOrder: index + 1,
        stayMinutes:
          spot.stayMinutes === '' || spot.stayMinutes === null || spot.stayMinutes === undefined
            ? undefined
            : Number(spot.stayMinutes),
        tip: spot.tip || undefined
      }))
    }))
  }
}

/** 保存：校验 → 组装 → PUT → 回详情。 */
async function handleSave() {
  if (saving.value) {
    return
  }
  if (!validate()) {
    return
  }

  saving.value = true
  try {
    await updateTrip(route.params.id, buildPayload())
    ElMessage.success('保存成功')
    router.push(`/trip/${route.params.id}`)
  } catch (e) {
    // 403/404/400 由 request 拦截器统一提示
  } finally {
    saving.value = false
  }
}

/** 取消并返回详情。 */
function goDetail() {
  router.push(`/trip/${route.params.id}`)
}

onMounted(fetchDetail)
</script>

<template>
  <div class="trip-edit page">
    <el-button class="trip-edit__back" text @click="goDetail">‹ 返回详情</el-button>

    <el-skeleton v-if="loading" :rows="8" animated />

    <EmptyState v-else-if="loadFailed" description="行程不存在或无权访问">
      <el-button type="primary" @click="router.push('/trip/list')">返回行程列表</el-button>
    </EmptyState>

    <template v-else>
      <el-card class="trip-edit__card" shadow="never">
        <el-form label-width="72px" @submit.prevent>
          <el-form-item label="标题">
            <el-input
              v-model="title"
              placeholder="行程标题"
              maxlength="100"
              show-word-limit
              clearable
            />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card
        v-for="day in days"
        :key="day.dayNo"
        class="trip-edit__card"
        shadow="never"
      >
        <template #header>
          <span class="trip-edit__dayno">第 {{ Number(day.dayNo) || 0 }} 天</span>
        </template>

        <el-form label-width="72px" @submit.prevent>
          <el-form-item label="主题">
            <el-input v-model="day.theme" placeholder="当日主题" maxlength="100" />
          </el-form-item>
          <el-form-item label="午餐">
            <el-input v-model="day.lunch" placeholder="午餐推荐" maxlength="255" />
          </el-form-item>
          <el-form-item label="晚餐">
            <el-input v-model="day.dinner" placeholder="晚餐推荐" maxlength="255" />
          </el-form-item>
          <el-form-item label="提示">
            <el-input
              v-model="day.tip"
              type="textarea"
              :rows="2"
              placeholder="当日提示"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>

        <div class="trip-edit__spots-head">
          <span class="trip-edit__spots-title">景点安排（{{ day.spots.length }}）</span>
          <el-button size="small" type="primary" plain @click="addSpot(day)">
            添加景点
          </el-button>
        </div>

        <div v-if="day.spots.length" class="trip-edit__spots">
          <div
            v-for="(spot, idx) in day.spots"
            :key="idx"
            class="trip-edit__spot"
          >
            <div class="trip-edit__spot-head">
              <span class="trip-edit__order">#{{ idx + 1 }}</span>
              <span
                v-if="spot.scenicId !== null && spot.scenicId !== undefined && spot.scenicId !== ''"
                class="trip-edit__scenic text-secondary"
              >
                关联景点 ID：{{ spot.scenicId }}
              </span>
              <span v-else class="trip-edit__scenic text-secondary">未关联景点</span>

              <div class="trip-edit__spot-tools">
                <el-button size="small" text :disabled="idx === 0" @click="moveUp(day, idx)">
                  上移
                </el-button>
                <el-button
                  size="small"
                  text
                  :disabled="idx === day.spots.length - 1"
                  @click="moveDown(day, idx)"
                >
                  下移
                </el-button>
                <el-button size="small" text type="danger" @click="removeSpot(day, idx)">
                  删除
                </el-button>
              </div>
            </div>

            <div class="trip-edit__spot-fields">
              <el-input
                v-model="spot.spotName"
                class="trip-edit__name"
                placeholder="景点名称（必填）"
                maxlength="100"
              />
              <el-input-number
                v-model="spot.stayMinutes"
                :min="0"
                :max="1440"
                :step="30"
                controls-position="right"
                class="trip-edit__stay"
              />
              <span class="trip-edit__unit text-secondary">分钟</span>
              <el-input
                v-model="spot.tip"
                class="trip-edit__tip"
                placeholder="游玩提示"
                maxlength="500"
              />
            </div>
          </div>
        </div>

        <p v-else class="trip-edit__empty text-secondary">当日暂无景点，点击上方「添加景点」新增。</p>
      </el-card>

      <div class="trip-edit__actions">
        <el-button type="primary" :loading="saving" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中…' : '保存' }}
        </el-button>
        <el-button :disabled="saving" @click="goDetail">取消</el-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.trip-edit {
  padding-top: 8px;
}

.trip-edit__back {
  margin-bottom: 8px;
}

.trip-edit__card {
  margin-bottom: 20px;
}

.trip-edit__dayno {
  font-weight: 600;
}

.trip-edit__spots-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 12px;
}

.trip-edit__spots-title {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.trip-edit__spots {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trip-edit__spot {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius);
  padding: 12px;
}

.trip-edit__spot-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.trip-edit__order {
  font-weight: 600;
  color: var(--el-color-primary);
}

.trip-edit__scenic {
  font-size: 12px;
  flex: 1;
}

.trip-edit__spot-tools {
  display: flex;
  gap: 2px;
  flex: 0 0 auto;
}

.trip-edit__spot-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.trip-edit__name {
  width: 200px;
}

.trip-edit__stay {
  width: 140px;
}

.trip-edit__unit {
  font-size: 12px;
}

.trip-edit__tip {
  flex: 1 1 200px;
  min-width: 180px;
}

.trip-edit__empty {
  font-size: 13px;
}

.trip-edit__actions {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}
</style>
