<script setup>
/**
 * 首页 · 景点列表页（F-VIEW，路由 `/`，公开）。
 *
 * <p>功能：
 * <ol>
 *   <li>筛选区：城市下拉 / 分类下拉 / 关键词输入 + 查询 / 重置；</li>
 *   <li>景点卡片网格（{@link ScenicCard}）+ `el-pagination` 分页；</li>
 *   <li>加载中展示骨架，空结果显示空状态；</li>
 *   <li>筛选条件与分页写入 `scenicStore`，供详情页「返回列表」时恢复。</li>
 * </ol>
 *
 * <p>注意：后端分页 `total` 为字符串，传 `el-pagination` 前用 `Number()` 转换；
 * `current-page` / `page-size` 为数字。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useScenicStore } from '@/store/scenic'
import { listScenics } from '@/api/scenic'
import { PAGE } from '@/utils/constants'
import ScenicCard from '@/components/ScenicCard.vue'
import ScenicCardSkeleton from '@/components/ScenicCardSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'

/** 城市固定选项（后端城市为精确匹配）。 */
const CITIES = ['北京', '上海', '杭州', '成都']

/** 分类固定选项。 */
const CATEGORIES = ['自然', '人文', '主题乐园', '其他']

/** 首页景点列表每页默认条数。 */
const HOME_PAGE_SIZE = 12

const scenicStore = useScenicStore()

/** 列表加载态。 */
const loading = ref(false)

/**
 * 表单筛选值：与 store 分离，避免输入即触发请求；
 * 点击「查询」时才写回 store 并请求。
 */
const form = reactive({
  city: scenicStore.filters.city,
  category: scenicStore.filters.category,
  keyword: scenicStore.filters.keyword
})

/** 当前页码（数字）。 */
const page = ref(Number(scenicStore.filters.page) || PAGE.DEFAULT_PAGE)

/** 每页条数（数字）。 */
const size = ref(Number(scenicStore.filters.size) || HOME_PAGE_SIZE)

/** 列表数据（id 为字符串）。 */
const list = computed(() => scenicStore.list)

/** 分页总数（转为数字供组件使用）。 */
const total = computed(() => Number(scenicStore.total) || 0)

/**
 * 加载列表：以 store 中的筛选条件 + 本地分页参数请求，并写回 store。
 */
async function fetchList() {
  loading.value = true
  try {
    const data = await listScenics({
      city: scenicStore.filters.city,
      category: scenicStore.filters.category,
      keyword: scenicStore.filters.keyword,
      page: page.value,
      size: size.value
    })
    scenicStore.setList(data && data.records, data && data.total)
  } catch (e) {
    // 错误提示由 request 拦截器统一展示；此处展示空状态
    scenicStore.setList([], '0')
  } finally {
    loading.value = false
  }
}

/** 查询：写回筛选条件并重置到第 1 页。 */
function handleSearch() {
  page.value = PAGE.DEFAULT_PAGE
  scenicStore.setFilters({
    city: form.city,
    category: form.category,
    keyword: form.keyword,
    page: PAGE.DEFAULT_PAGE,
    size: size.value
  })
  fetchList()
}

/** 重置：清空筛选与分页，回到默认列表。 */
function handleReset() {
  form.city = ''
  form.category = ''
  form.keyword = ''
  scenicStore.resetFilters()
  page.value = PAGE.DEFAULT_PAGE
  size.value = HOME_PAGE_SIZE
  fetchList()
}

/**
 * 分页变更（页码或每页条数）。
 *
 * @param {number} currentPage 当前页
 * @param {number} pageSize 每页条数
 */
function handlePagerChange(currentPage, pageSize) {
  page.value = currentPage
  size.value = pageSize
  scenicStore.setFilters({ page: currentPage, size: pageSize })
  fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="home page">
    <header class="home__hero">
      <h1 class="home__title">发现你的下一段旅程</h1>
      <p class="home__subtitle">AI 智能规划行程，把山川湖海装进每一天</p>
    </header>

    <el-card class="home__filters" shadow="never">
      <div class="home__filter-row">
        <el-select
          v-model="form.city"
          placeholder="选择城市"
          clearable
          class="home__control"
        >
          <el-option v-for="c in CITIES" :key="c" :label="c" :value="c" />
        </el-select>

        <el-select
          v-model="form.category"
          placeholder="选择分类"
          clearable
          class="home__control"
        >
          <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
        </el-select>

        <el-input
          v-model="form.keyword"
          placeholder="搜索景点名称或简介"
          clearable
          class="home__keyword"
          @keyup.enter="handleSearch"
        />

        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <ScenicCardSkeleton v-if="loading" :count="8" />

    <EmptyState
      v-else-if="list.length === 0"
      description="没有找到符合条件的景点"
    >
      <el-button v-if="scenicStore.hasFilter" type="primary" @click="handleReset">
        清空筛选
      </el-button>
    </EmptyState>

    <div v-else class="home__grid">
      <ScenicCard v-for="item in list" :key="item.id" :scenic="item" />
    </div>

    <div v-if="!loading && total > 0" class="home__pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[HOME_PAGE_SIZE, 24, 48]"
        @change="handlePagerChange"
      />
    </div>
  </div>
</template>

<style scoped>
.home {
  padding-top: 4px;
}

.home__hero {
  position: relative;
  overflow: hidden;
  margin: 4px 0 24px;
  padding: 48px 44px;
  border-radius: 22px;
  color: #fff;
  background:
    radial-gradient(520px 240px at 88% 16%, rgba(232, 161, 58, 0.22), transparent 60%),
    linear-gradient(130deg, var(--app-primary-ink) 0%, var(--app-primary-deep) 55%, var(--app-primary) 100%);
}

.home__hero::after {
  content: '';
  position: absolute;
  right: -60px;
  bottom: -90px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.home__title {
  font-family: var(--app-font-display);
  font-size: 36px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: 0.02em;
  margin: 0 0 14px;
}

.home__subtitle {
  color: rgba(255, 255, 255, 0.82);
  font-size: 15px;
  margin: 0;
}

.home__filters {
  margin-bottom: 24px;
  border-radius: var(--app-radius);
}

.home__filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.home__control {
  width: 160px;
}

.home__keyword {
  width: 280px;
  flex: 1 1 220px;
  max-width: 360px;
}

.home__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--app-gap);
}

.home__pagination {
  display: flex;
  justify-content: center;
  margin: 24px 0 8px;
}
</style>
