<script setup>
/**
 * 行程管理（F-VIEW，路由 `/admin/trips`，需 admin）。
 *
 * <p>分页展示全部用户行程（`listAdminTrips`）：用户名 / 标题 / 城市 / 天数 / 创建时间。
 * 只读，无编辑操作。所有文本纯文本渲染（禁用 v-html）。
 */
import { onMounted, ref } from 'vue'
import { listAdminTrips } from '@/api/admin'
import { PAGE } from '@/utils/constants'
import { formatDate, emptyText } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

/** 列表加载态。 */
const loading = ref(false)
/** 列表数据。 */
const list = ref([])
/** 总数（数字）。 */
const total = ref(0)
/** 分页。 */
const page = ref(PAGE.DEFAULT_PAGE)
const size = ref(PAGE.DEFAULT_SIZE)

/** 拉取列表。 */
async function fetchList() {
  loading.value = true
  try {
    const data = await listAdminTrips({ page: page.value, size: size.value })
    list.value = (data && data.records) || []
    total.value = Number(data && data.total) || 0
  } catch (e) {
    // 403/404 由 request 拦截器统一提示
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
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

onMounted(fetchList)
</script>

<template>
  <div class="admin-trips">
    <div class="admin-trips__head">
      <h2 class="admin-trips__title">行程管理</h2>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="用户名" width="150">
        <template #default="{ row }">{{ emptyText(row.username) }}</template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="城市" width="120">
        <template #default="{ row }">{{ emptyText(row.city) }}</template>
      </el-table-column>
      <el-table-column label="天数" width="90">
        <template #default="{ row }">{{ emptyText(row.days, '-') }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createAt, '-') }}</template>
      </el-table-column>
      <template #empty>
        <EmptyState description="暂无行程数据" />
      </template>
    </el-table>

    <div v-if="total > 0" class="admin-trips__pagination">
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
.admin-trips__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.admin-trips__title {
  margin: 0;
  font-size: 18px;
}

.admin-trips__pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
