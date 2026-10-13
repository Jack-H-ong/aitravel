<script setup>
/**
 * 景点管理（F-VIEW，路由 `/admin/scenic`，需 admin）。
 *
 * <p>功能：
 * <ol>
 *   <li>筛选（city/category/keyword）+ 分页列表（`listAdminScenics`）；</li>
 *   <li>「新增 / 编辑」弹窗（`el-dialog` + `el-form`）：name/city 必填，price 非负，
 *       tags/images 以多值输入（`el-select multiple allow-create filterable`），
 *       提交前统一组装为**数组**；</li>
 *   <li>「删除」带 `ElMessageBox.confirm` 二次确认（后端为逻辑删除）。</li>
 * </ol>
 *
 * <p>所有文本纯文本渲染（禁用 v-html）。分页 `total` 为字符串，传组件前 `Number()`。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listAdminScenics,
  createScenic,
  updateScenic,
  deleteScenic
} from '@/api/admin'
import { PAGE } from '@/utils/constants'
import { formatPrice, emptyText } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

/** 城市固定选项（后端城市为精确匹配）。 */
const CITIES = ['北京', '上海', '杭州', '成都']
/** 分类固定选项。 */
const CATEGORIES = ['自然', '人文', '主题乐园', '其他']
/** 常用标签（可选，也允许自由输入）。 */
const PRESET_TAGS = ['免费', '亲子', '美食', '打卡', '夜景', '历史']

/** 列表加载态。 */
const loading = ref(false)
/** 列表数据。 */
const list = ref([])
/** 总数（数字）。 */
const total = ref(0)
/** 当前页 / 每页条数（数字）。 */
const page = ref(PAGE.DEFAULT_PAGE)
const size = ref(PAGE.DEFAULT_SIZE)

/** 筛选表单。 */
const filters = reactive({ city: '', category: '', keyword: '' })

/** 弹窗可见性 / 标题 / 提交中。 */
const dialogVisible = ref(false)
const dialogTitle = ref('新增景点')
const saving = ref(false)
/** 编辑态 id（null 表示新增）。 */
const editingId = ref(null)

/** 表单引用（校验用）。 */
const formRef = ref(null)

/** 表单模型。 */
const form = reactive({
  name: '',
  city: '',
  address: '',
  category: '',
  price: 0,
  openTime: '',
  description: '',
  tags: [],
  images: []
})

/** 价格非负校验。 */
function validatePrice(rule, value, callback) {
  if (value === '' || value === null || value === undefined) {
    callback()
    return
  }
  const n = Number(value)
  if (!Number.isFinite(n) || n < 0) {
    callback(new Error('价格不能为负数'))
    return
  }
  callback()
}

/** 表单校验规则。 */
const rules = {
  name: [{ required: true, message: '请输入景点名称', trigger: 'blur' }],
  city: [{ required: true, message: '请输入所在城市', trigger: 'blur' }],
  price: [{ validator: validatePrice, trigger: 'change' }]
}

/** 拉取列表。 */
async function fetchList() {
  loading.value = true
  try {
    const data = await listAdminScenics({
      city: filters.city || undefined,
      category: filters.category || undefined,
      keyword: filters.keyword || undefined,
      page: page.value,
      size: size.value
    })
    list.value = (data && data.records) || []
    total.value = Number(data && data.total) || 0
  } catch (e) {
    // 403/404/500 由 request 拦截器统一提示
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

/** 重置筛选。 */
function handleReset() {
  filters.city = ''
  filters.category = ''
  filters.keyword = ''
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

/** 重置表单模型为默认值。 */
function resetForm() {
  form.name = ''
  form.city = ''
  form.address = ''
  form.category = ''
  form.price = 0
  form.openTime = ''
  form.description = ''
  form.tags = []
  form.images = []
}

/** 打开「新增」弹窗。 */
function openCreate() {
  editingId.value = null
  dialogTitle.value = '新增景点'
  resetForm()
  dialogVisible.value = true
  // 清空上一次的校验态
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

/**
 * 打开「编辑」弹窗并回填。
 *
 * @param {object} row 行数据（ScenicVO，tags/images 已是数组）
 */
function openEdit(row) {
  editingId.value = row.id
  dialogTitle.value = '编辑景点'
  form.name = row.name || ''
  form.city = row.city || ''
  form.address = row.address || ''
  form.category = row.category || ''
  form.price = row.price === null || row.price === undefined ? 0 : Number(row.price)
  form.openTime = row.openTime || ''
  form.description = row.description || ''
  form.tags = Array.isArray(row.tags) ? [...row.tags] : []
  form.images = Array.isArray(row.images) ? [...row.images] : []
  dialogVisible.value = true
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

/** 组装保存请求（tags/images 确保为数组）。 */
function buildPayload() {
  return {
    name: String(form.name).trim(),
    city: String(form.city).trim(),
    address: form.address ? String(form.address).trim() : undefined,
    category: form.category || undefined,
    price:
      form.price === '' || form.price === null || form.price === undefined
        ? undefined
        : Number(form.price),
    openTime: form.openTime ? String(form.openTime).trim() : undefined,
    description: form.description ? String(form.description) : undefined,
    tags: Array.isArray(form.tags) ? form.tags : [],
    images: Array.isArray(form.images) ? form.images : []
  }
}

/** 提交新增 / 编辑。 */
async function handleSubmit() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    if (editingId.value === null) {
      await createScenic(buildPayload())
      ElMessage.success('新增成功')
    } else {
      await updateScenic(editingId.value, buildPayload())
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (e) {
    // 400/403 由 request 拦截器统一提示
  } finally {
    saving.value = false
  }
}

/**
 * 删除（逻辑删除）：二次确认后调用。
 *
 * @param {object} row 行数据
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除景点「${row.name}」吗？该操作会将其下架（逻辑删除）。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteScenic(row.id)
    ElMessage.success('已删除')
    // 若当前页删空则回退一页
    if (list.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    fetchList()
  } catch (e) {
    // 404/403 由 request 拦截器统一提示
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="admin-scenic">
    <div class="admin-scenic__head">
      <h2 class="admin-scenic__title">景点管理</h2>
      <el-button type="primary" @click="openCreate">新增景点</el-button>
    </div>

    <el-card class="admin-scenic__filters" shadow="never">
      <div class="admin-scenic__filter-row">
        <el-select v-model="filters.city" placeholder="城市" clearable class="admin-scenic__control">
          <el-option v-for="c in CITIES" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select
          v-model="filters.category"
          placeholder="分类"
          clearable
          class="admin-scenic__control"
        >
          <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
        </el-select>
        <el-input
          v-model="filters.keyword"
          placeholder="搜索景点名称"
          clearable
          class="admin-scenic__keyword"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="city" label="城市" width="90" />
      <el-table-column label="分类" width="110">
        <template #default="{ row }">{{ emptyText(row.category) }}</template>
      </el-table-column>
      <el-table-column label="价格" width="90">
        <template #default="{ row }">{{ formatPrice(row.price) }}</template>
      </el-table-column>
      <el-table-column label="评分" width="80">
        <template #default="{ row }">
          {{ row.avgRating === null || row.avgRating === undefined ? '-' : Number(row.avgRating) }}
        </template>
      </el-table-column>
      <el-table-column label="收藏数" width="90">
        <template #default="{ row }">{{ emptyText(row.favoriteCount, 0) }}</template>
      </el-table-column>
      <el-table-column label="浏览量" width="90">
        <template #default="{ row }">{{ emptyText(row.viewCount, 0) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <EmptyState description="暂无景点数据" />
      </template>
    </el-table>

    <div v-if="total > 0" class="admin-scenic__pagination">
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" @submit.prevent>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="景点名称（必填）" maxlength="100" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" placeholder="所在城市（必填）" maxlength="50" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类" clearable class="admin-scenic__full">
            <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" />
          <span class="admin-scenic__hint text-secondary">0 表示免费</span>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="详细地址" maxlength="200" />
        </el-form-item>
        <el-form-item label="开放时间" prop="openTime">
          <el-input v-model="form.openTime" placeholder="如 08:00-18:00" maxlength="100" />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入后回车可创建标签"
            class="admin-scenic__full"
          >
            <el-option v-for="t in PRESET_TAGS" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片" prop="images">
          <el-select
            v-model="form.images"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入图片 URL 后回车"
            class="admin-scenic__full"
          />
        </el-form-item>
        <el-form-item label="介绍" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="2000"
            show-word-limit
            placeholder="景点介绍"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button :disabled="saving" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="saving" @click="handleSubmit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-scenic__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.admin-scenic__title {
  margin: 0;
  font-size: 18px;
}

.admin-scenic__filters {
  margin-bottom: 12px;
}

.admin-scenic__filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.admin-scenic__control {
  width: 140px;
}

.admin-scenic__keyword {
  width: 240px;
}

.admin-scenic__full {
  width: 100%;
}

.admin-scenic__hint {
  margin-left: 10px;
  font-size: 12px;
}

.admin-scenic__pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
