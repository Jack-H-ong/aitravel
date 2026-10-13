<script setup>
/**
 * 对话记录（F-VIEW，路由 `/admin/chats`，需 admin）。
 *
 * <p>分页展示全部用户与会话记录（`listAdminChats`）：
 * 用户名 / 角色 / 内容 / 关联行程 / 时间。内容列用 `show-overflow-tooltip` 截断展示。
 * 所有文本纯文本渲染（禁用 v-html）。
 */
import { onMounted, ref } from 'vue'
import { listAdminChats } from '@/api/admin'
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

/**
 * 角色展示文案。
 *
 * @param {string} role 'user' | 'assistant'
 * @returns {string} 中文文案
 */
function roleText(role) {
  return role === 'assistant' ? 'AI' : '用户'
}

/**
 * 角色标签类型。
 *
 * @param {string} role 角色
 * @returns {string} el-tag type
 */
function roleTagType(role) {
  return role === 'assistant' ? 'success' : 'info'
}

/** 拉取列表。 */
async function fetchList() {
  loading.value = true
  try {
    const data = await listAdminChats({ page: page.value, size: size.value })
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
  <div class="admin-chats">
    <div class="admin-chats__head">
      <h2 class="admin-chats__title">对话记录</h2>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="用户名" width="150">
        <template #default="{ row }">{{ emptyText(row.username) }}</template>
      </el-table-column>
      <el-table-column label="角色" width="90">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.role)" size="small">{{ roleText(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="320" show-overflow-tooltip />
      <el-table-column label="关联行程" width="140">
        <template #default="{ row }">{{ emptyText(row.tripId, '无') }}</template>
      </el-table-column>
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createAt, '-') }}</template>
      </el-table-column>
      <template #empty>
        <EmptyState description="暂无对话记录" />
      </template>
    </el-table>

    <div v-if="total > 0" class="admin-chats__pagination">
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
.admin-chats__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.admin-chats__title {
  margin: 0;
  font-size: 18px;
}

.admin-chats__pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
