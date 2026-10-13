<script setup>
/**
 * 我的行程列表页（F-VIEW，路由 `/trip/list`，需登录）。
 *
 * <p>功能：
 * <ol>
 *   <li>城市筛选（下拉）+ 查询 / 重置；</li>
 *   <li>行程卡片网格（{@link TripCard}）+ `el-pagination` 分页；</li>
 *   <li>卡片点击进详情 `/trip/:id`；「删除」经 `ElMessageBox.confirm` 确认后调用接口并刷新列表；</li>
 *   <li>加载中骨架、空结果显示空状态，并提供「生成攻略」入口。</li>
 * </ol>
 *
 * <p>注意：后端分页 `total` 为字符串，传 `el-pagination` 前 `Number()` 转换。
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTrips, deleteTrip } from '@/api/trip'
import { PAGE } from '@/utils/constants'
import TripCard from '@/components/TripCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

/** 城市筛选选项。 */
const CITIES = ['北京', '上海', '杭州', '成都']

/** 列表加载态。 */
const loading = ref(false)
/** 行程列表（id 为字符串）。 */
const list = ref([])
/** 总数（数字，来自后端字符串 total）。 */
const total = ref(0)
/** 当前城市筛选（空串 = 全部）。 */
const city = ref('')
/** 当前页码（数字）。 */
const page = ref(PAGE.DEFAULT_PAGE)
/** 每页条数（数字）。 */
const size = ref(PAGE.DEFAULT_SIZE)
/** 删除请求进行中（防重复）。 */
const deleting = ref(false)

/**
 * 加载列表。
 */
async function fetchList() {
  loading.value = true
  try {
    const data = await listTrips({
      city: city.value || undefined,
      page: page.value,
      size: size.value
    })
    list.value = (data && data.records) || []
    total.value = Number(data && data.total) || 0
  } catch (e) {
    // 错误提示由 request 拦截器统一展示
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 查询：重置到第 1 页后请求。 */
function handleSearch() {
  page.value = PAGE.DEFAULT_PAGE
  fetchList()
}

/** 重置筛选。 */
function handleReset() {
  city.value = ''
  page.value = PAGE.DEFAULT_PAGE
  size.value = PAGE.DEFAULT_SIZE
  fetchList()
}

/**
 * 分页变更。
 *
 * @param {number} currentPage 当前页
 * @param {number} pageSize 每页条数
 */
function handlePagerChange(currentPage, pageSize) {
  page.value = currentPage
  size.value = pageSize
  fetchList()
}

/** 查看详情。 */
function goDetail(trip) {
  if (trip && trip.id !== undefined && trip.id !== null) {
    router.push(`/trip/${trip.id}`)
  }
}

/**
 * 删除行程：确认后调用接口并刷新列表。
 *
 * @param {object} trip 行程 VO
 */
async function handleDelete(trip) {
  if (deleting.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除「${trip.title || '未命名行程'}」吗？删除后不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    // 用户取消
    return
  }

  deleting.value = true
  try {
    await deleteTrip(trip.id)
    ElMessage.success('已删除')
    // 删除的是当前页最后一条且非首页 → 回退一页
    if (list.value.length === 1 && page.value > PAGE.DEFAULT_PAGE) {
      page.value -= 1
    }
    await fetchList()
  } catch (e) {
    // 403/404 等由 request 拦截器统一提示
  } finally {
    deleting.value = false
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="trips page">
    <header class="trips__head">
      <h1 class="trips__title">我的行程</h1>
      <el-button type="primary" @click="router.push('/trip/generate')">
        生成新攻略
      </el-button>
    </header>

    <el-card class="trips__filters" shadow="never">
      <div class="trips__filter-row">
        <el-select
          v-model="city"
          placeholder="选择城市"
          clearable
          class="trips__control"
          @change="handleSearch"
        >
          <el-option v-for="c in CITIES" :key="c" :label="c" :value="c" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-skeleton v-if="loading" :rows="6" animated />

    <EmptyState v-else-if="list.length === 0" description="还没有行程，去生成一份吧">
      <el-button type="primary" @click="router.push('/trip/generate')">
        生成攻略
      </el-button>
    </EmptyState>

    <div v-else class="trips__grid">
      <TripCard
        v-for="item in list"
        :key="item.id"
        :trip="item"
        @view="goDetail"
        @remove="handleDelete"
      />
    </div>

    <div v-if="!loading && total > 0" class="trips__pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @change="handlePagerChange"
      />
    </div>
  </div>
</template>

<style scoped>
.trips {
  padding-top: 8px;
}

.trips__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.trips__title {
  font-size: 22px;
  margin: 0;
}

.trips__filters {
  margin-bottom: 20px;
}

.trips__filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.trips__control {
  width: 160px;
}

.trips__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--app-gap);
}

.trips__pagination {
  display: flex;
  justify-content: center;
  margin: 24px 0 8px;
}
</style>
