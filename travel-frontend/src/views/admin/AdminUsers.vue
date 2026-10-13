<script setup>
/**
 * 用户管理（F-VIEW，路由 `/admin/users`，需 admin）。
 *
 * <p>功能：
 * <ol>
 *   <li>keyword 搜索 + 分页列表（`listUsers`）；</li>
 *   <li>行内「禁用 / 启用」开关（`el-switch`，0=禁用 / 1=正常），调用 `updateUserStatus`；</li>
 *   <li>保护：**admin 角色账号禁止在此被禁用**（开关禁用），普通账号禁用前二次确认。</li>
 * </ol>
 *
 * <p>列表 VO 不含密码；所有文本纯文本渲染（禁用 v-html）。
 */
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, updateUserStatus } from '@/api/admin'
import { PAGE, ROLE } from '@/utils/constants'
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
/** 搜索关键词。 */
const keyword = ref('')

/** 拉取列表。 */
async function fetchList() {
  loading.value = true
  try {
    const data = await listUsers({
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value
    })
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

/** 查询：回到第 1 页。 */
function handleSearch() {
  page.value = PAGE.DEFAULT_PAGE
  fetchList()
}

/** 重置搜索。 */
function handleReset() {
  keyword.value = ''
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

/**
 * 是否允许操作该行状态（admin 账号受保护）。
 *
 * @param {object} row 用户行
 * @returns {boolean} true=管理员账号，禁止操作
 */
function isProtected(row) {
  return row.role === ROLE.ADMIN
}

/**
 * `el-switch` 切换前置钩子：禁用前二次确认；启用直接放行。
 *
 * @param {object} row 用户行
 * @returns {boolean|Promise<boolean>} 允许切换为 true
 */
function beforeStatusChange(row) {
  const target = row.status === 1 ? 0 : 1
  if (target === 1) {
    return true
  }
  return ElMessageBox.confirm(
    `确定禁用用户「${row.username}」吗？禁用后该账号将无法登录。`,
    '禁用确认',
    { type: 'warning', confirmButtonText: '确定禁用', cancelButtonText: '取消' }
  )
    .then(() => true)
    .catch(() => false)
}

/**
 * 状态切换成功后落库；失败则回滚本地值。
 *
 * @param {object} row 用户行
 * @param {number} value 新状态（0/1）
 */
async function handleStatusChange(row, value) {
  try {
    await updateUserStatus(row.id, value)
    ElMessage.success(value === 1 ? '已启用' : '已禁用')
  } catch (e) {
    // 回滚本地状态（错误提示由 request 拦截器统一展示）
    row.status = value === 1 ? 0 : 1
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="admin-users">
    <div class="admin-users__head">
      <h2 class="admin-users__title">用户管理</h2>
    </div>

    <el-card class="admin-users__filters" shadow="never">
      <div class="admin-users__filter-row">
        <el-input
          v-model="keyword"
          placeholder="搜索用户名 / 昵称"
          clearable
          class="admin-users__keyword"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="username" label="用户名" min-width="140" show-overflow-tooltip />
      <el-table-column label="昵称" min-width="140">
        <template #default="{ row }">{{ emptyText(row.nickname) }}</template>
      </el-table-column>
      <el-table-column label="角色" width="110">
        <template #default="{ row }">
          <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
            {{ row.role === 'admin' ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            :active-value="1"
            :inactive-value="0"
            active-text="正常"
            inactive-text="禁用"
            inline-prompt
            :disabled="isProtected(row)"
            :before-change="() => beforeStatusChange(row)"
            @change="(val) => handleStatusChange(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createAt, '-') }}</template>
      </el-table-column>
      <template #empty>
        <EmptyState description="暂无用户数据" />
      </template>
    </el-table>

    <div v-if="total > 0" class="admin-users__pagination">
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
.admin-users__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.admin-users__title {
  margin: 0;
  font-size: 18px;
}

.admin-users__filters {
  margin-bottom: 12px;
}

.admin-users__filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.admin-users__keyword {
  width: 260px;
}

.admin-users__pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
