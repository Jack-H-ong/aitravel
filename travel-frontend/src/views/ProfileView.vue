<script setup>
/**
 * 个人中心聚合页（F-VIEW，路由 `/profile`，需登录）。
 *
 * <p>顶部为**用户资料卡**（头像/昵称/用户名/角色 + 「编辑资料」入口），
 * 下方 `el-tabs` 三个聚合 Tab：
 * <ol>
 *   <li>「我的行程」：复用 `trip.list` + {@link TripCard}，可查看详情 / 删除；</li>
 *   <li>「我的收藏」：复用 `scenic.myFavorites` + {@link ScenicCard}，卡片整卡进详情，
 *       包裹层内提供「取消收藏」按钮（`@click.stop` 阻止冒泡到卡片跳转）；</li>
 *   <li>「浏览历史」：`profile.history`（后端按浏览时间倒序），点击进景点详情。</li>
 * </ol>
 *
 * <p>边界处理：
 * <ul>
 *   <li>三 Tab **各自独立**的分页状态（页码 / 每页条数 / total / loading）；</li>
 *   <li>**按需加载**：首次切入某 Tab 才发起请求（`loaded` 标记）；</li>
 *   <li>空数据显示 {@link EmptyState}；取消收藏 / 删除行程后刷新当前列表。</li>
 * </ul>
 *
 * <p>后端分页 `total` 为字符串，绑定 `el-pagination` 前 `Number()` 转换。
 * 所有文本均为纯文本渲染（禁用 v-html）。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTrips, deleteTrip } from '@/api/trip'
import { getMyFavorites, toggleFavorite } from '@/api/scenic'
import { getBrowseHistory } from '@/api/profile'
import { useUserStore } from '@/store/user'
import { PAGE } from '@/utils/constants'
import { formatDate, emptyText } from '@/utils/format'
import TripCard from '@/components/TripCard.vue'
import ScenicCard from '@/components/ScenicCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const userStore = useUserStore()

/** 每页条数选项。 */
const PAGE_SIZES = [10, 20, 50]

/** 当前激活的 Tab。 */
const activeTab = ref('trips')

/**
 * 生成一个 Tab 的独立分页状态。
 *
 * @returns {{list:Array,total:number,page:number,size:number,loading:boolean,loaded:boolean}}
 */
function createTabState() {
  return reactive({
    list: [],
    total: 0,
    page: PAGE.DEFAULT_PAGE,
    size: PAGE.DEFAULT_SIZE,
    loading: false,
    loaded: false
  })
}

/** 三个 Tab 各自独立的分页状态。 */
const tripState = createTabState()
const favoriteState = createTabState()
const historyState = createTabState()

/** 删除行程进行中（防重复）。 */
const deleting = ref(false)
/** 取消收藏进行中（防重复）。 */
const unfavoriting = ref(false)

/** 头像地址。 */
const avatar = computed(() => (userStore.userInfo && userStore.userInfo.avatar) || '')
/** 展示昵称。 */
const displayName = computed(() => userStore.displayName || '未登录')
/** 用户名。 */
const username = computed(() => (userStore.userInfo && userStore.userInfo.username) || '')
/** 角色文案。 */
const roleText = computed(() => (userStore.isAdmin ? '管理员' : '普通用户'))

/**
 * 通用分页加载：请求 → 写入状态 → 重置失败数据。
 *
 * @param {object} state Tab 状态
 * @param {Function} fetcher 请求函数，入参 `{page,size}`，返回分页结果
 */
async function loadPage(state, fetcher) {
  state.loading = true
  try {
    const data = await fetcher({ page: state.page, size: state.size })
    state.list = (data && data.records) || []
    state.total = Number(data && data.total) || 0
  } catch (e) {
    // 错误提示由 request 拦截器统一展示
    state.list = []
    state.total = 0
  } finally {
    state.loading = false
    state.loaded = true
  }
}

/** 加载「我的行程」。 */
function fetchTrips() {
  return loadPage(tripState, listTrips)
}

/** 加载「我的收藏」。 */
function fetchFavorites() {
  return loadPage(favoriteState, getMyFavorites)
}

/** 加载「浏览历史」。 */
function fetchHistory() {
  return loadPage(historyState, getBrowseHistory)
}

/**
 * 切换 Tab：首次进入才加载（按需加载）。
 *
 * @param {string} name Tab 名
 */
function handleTabChange(name) {
  activeTab.value = name
  if (name === 'trips' && !tripState.loaded) {
    fetchTrips()
  } else if (name === 'favorites' && !favoriteState.loaded) {
    fetchFavorites()
  } else if (name === 'history' && !historyState.loaded) {
    fetchHistory()
  }
}

/**
 * 某 Tab 分页变更。
 *
 * @param {object} state Tab 状态
 * @param {Function} fetcher 请求函数
 * @param {number} currentPage 当前页
 * @param {number} pageSize 每页条数
 */
function handlePagerChange(state, fetcher, currentPage, pageSize) {
  state.page = currentPage
  state.size = pageSize
  fetcher()
}

/** 查看行程详情。 */
function goTrip(trip) {
  if (trip && trip.id !== undefined && trip.id !== null) {
    router.push(`/trip/${trip.id}`)
  }
}

/** 查看景点详情。 */
function goScenic(id) {
  if (id !== undefined && id !== null && id !== '') {
    router.push(`/scenic/${id}`)
  }
}

/**
 * 删除行程：确认后调用接口并刷新当前列表。
 *
 * @param {object} trip 行程 VO
 */
async function handleDeleteTrip(trip) {
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
    if (tripState.list.length === 1 && tripState.page > PAGE.DEFAULT_PAGE) {
      tripState.page -= 1
    }
    await fetchTrips()
  } catch (e) {
    // 403/404 等由 request 拦截器统一提示
  } finally {
    deleting.value = false
  }
}

/**
 * 取消收藏：调用 toggle 接口后刷新当前收藏列表。
 *
 * @param {object} scenic 景点 VO
 */
async function handleUnfavorite(scenic) {
  if (unfavoriting.value) {
    return
  }
  unfavoriting.value = true
  try {
    await toggleFavorite(scenic.id)
    ElMessage.success('已取消收藏')
    // 取消的是当前页最后一条且非首页 → 回退一页
    if (favoriteState.list.length === 1 && favoriteState.page > PAGE.DEFAULT_PAGE) {
      favoriteState.page -= 1
    }
    await fetchFavorites()
  } catch (e) {
    // 错误提示由 request 拦截器统一展示
  } finally {
    unfavoriting.value = false
  }
}

/** 浏览历史时间文案（后端已倒序，展示原始时间）。 */
function browseTimeText(value) {
  return formatDate(value, '-')
}

/** 浏览历史封面（取 images[0]，无图返回空串）。 */
function historyCover(item) {
  const images = item && item.images
  return Array.isArray(images) && images.length > 0 ? images[0] : ''
}

onMounted(async () => {
  // 拉取最新用户信息（失败由拦截器处理，不影响页面）
  try {
    await userStore.fetchMe()
  } catch (e) {
    // 忽略
  }
  // 默认 Tab 首次加载
  fetchTrips()
})
</script>

<template>
  <div class="profile page">
    <!-- 顶部用户资料卡 -->
    <el-card class="profile__user" shadow="never">
      <div class="profile__user-inner">
        <el-avatar :size="72" :src="avatar">
          {{ (displayName || 'U').charAt(0) }}
        </el-avatar>

        <div class="profile__user-info">
          <h2 class="profile__nickname text-ellipsis">{{ displayName }}</h2>
          <div class="profile__meta text-secondary">
            <span>用户名：{{ emptyText(username) }}</span>
            <el-tag
              :type="userStore.isAdmin ? 'warning' : 'info'"
              effect="plain"
              size="small"
            >
              {{ roleText }}
            </el-tag>
          </div>
        </div>

        <el-button
          class="profile__edit"
          type="primary"
          plain
          @click="router.push('/profile/settings')"
        >
          编辑资料
        </el-button>
      </div>
    </el-card>

    <!-- 聚合 Tab -->
    <el-tabs v-model="activeTab" class="profile__tabs" @tab-change="handleTabChange">
      <!-- Tab 1：我的行程 -->
      <el-tab-pane label="我的行程" name="trips">
        <el-skeleton v-if="tripState.loading" :rows="6" animated />

        <EmptyState
          v-else-if="tripState.list.length === 0"
          description="还没有行程，去生成一份吧"
        >
          <el-button type="primary" @click="router.push('/trip/generate')">
            生成攻略
          </el-button>
        </EmptyState>

        <template v-else>
          <div class="profile__grid">
            <TripCard
              v-for="item in tripState.list"
              :key="item.id"
              :trip="item"
              @view="goTrip"
              @remove="handleDeleteTrip"
            />
          </div>
          <div class="profile__pagination">
            <el-pagination
              v-model:current-page="tripState.page"
              v-model:page-size="tripState.size"
              background
              layout="total, sizes, prev, pager, next"
              :total="tripState.total"
              :page-sizes="PAGE_SIZES"
              @change="(p, s) => handlePagerChange(tripState, fetchTrips, p, s)"
            />
          </div>
        </template>
      </el-tab-pane>

      <!-- Tab 2：我的收藏 -->
      <el-tab-pane label="我的收藏" name="favorites">
        <el-skeleton v-if="favoriteState.loading" :rows="6" animated />

        <EmptyState
          v-else-if="favoriteState.list.length === 0"
          description="还没有收藏的景点，去逛逛吧"
        >
          <el-button type="primary" @click="router.push('/scenic')">
            逛逛景点
          </el-button>
        </EmptyState>

        <template v-else>
          <div class="profile__grid">
            <div
              v-for="item in favoriteState.list"
              :key="item.id"
              class="profile__fav-item"
            >
              <!-- ScenicCard 整卡可点进详情（不改动其本体） -->
              <ScenicCard :scenic="item" />
              <el-button
                class="profile__unfav"
                type="danger"
                size="small"
                :loading="unfavoriting"
                @click.stop="handleUnfavorite(item)"
              >
                取消收藏
              </el-button>
            </div>
          </div>
          <div class="profile__pagination">
            <el-pagination
              v-model:current-page="favoriteState.page"
              v-model:page-size="favoriteState.size"
              background
              layout="total, sizes, prev, pager, next"
              :total="favoriteState.total"
              :page-sizes="PAGE_SIZES"
              @change="(p, s) => handlePagerChange(favoriteState, fetchFavorites, p, s)"
            />
          </div>
        </template>
      </el-tab-pane>

      <!-- Tab 3：浏览历史 -->
      <el-tab-pane label="浏览历史" name="history">
        <el-skeleton v-if="historyState.loading" :rows="6" animated />

        <EmptyState
          v-else-if="historyState.list.length === 0"
          description="还没有浏览记录，去逛逛景点吧"
        >
          <el-button type="primary" @click="router.push('/scenic')">
            逛逛景点
          </el-button>
        </EmptyState>

        <template v-else>
          <ul class="profile__history">
            <li
              v-for="item in historyState.list"
              :key="item.scenicId"
              class="profile__history-item"
              @click="goScenic(item.scenicId)"
            >
              <div class="profile__history-cover">
                <img
                  v-if="historyCover(item)"
                  :src="historyCover(item)"
                  :alt="item.name"
                />
                <span v-else class="profile__history-placeholder">暂无图片</span>
              </div>

              <div class="profile__history-body">
                <h3 class="profile__history-name text-ellipsis">
                  {{ emptyText(item.name, '未命名景点') }}
                </h3>
                <div class="profile__history-meta text-secondary">
                  <span>{{ emptyText(item.city) }}</span>
                  <span class="profile__dot">·</span>
                  <span>{{ emptyText(item.category, '未分类') }}</span>
                  <span class="profile__dot">·</span>
                  <span>评分 {{ (Number(item.avgRating) || 0).toFixed(1) }}</span>
                </div>
                <div class="profile__history-time text-secondary">
                  浏览于 {{ browseTimeText(item.browseTime) }}
                </div>
              </div>

              <span class="profile__history-more">查看详情 ›</span>
            </li>
          </ul>
          <div class="profile__pagination">
            <el-pagination
              v-model:current-page="historyState.page"
              v-model:page-size="historyState.size"
              background
              layout="total, sizes, prev, pager, next"
              :total="historyState.total"
              :page-sizes="PAGE_SIZES"
              @change="(p, s) => handlePagerChange(historyState, fetchHistory, p, s)"
            />
          </div>
        </template>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.profile {
  padding-top: 8px;
}

/* ---------- 用户资料卡 ---------- */
.profile__user {
  margin-bottom: 16px;
}

.profile__user-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile__user-info {
  flex: 1;
  min-width: 0;
}

.profile__nickname {
  margin: 0 0 6px;
  font-size: 20px;
}

.profile__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.profile__edit {
  flex: 0 0 auto;
}

/* ---------- 网格与分页 ---------- */
.profile__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--app-gap);
}

.profile__pagination {
  display: flex;
  justify-content: center;
  margin: 24px 0 8px;
}

/* ---------- 收藏项（包裹层，取消收藏按钮浮于卡片右上） ---------- */
.profile__fav-item {
  position: relative;
}

.profile__unfav {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
}

/* ---------- 浏览历史列表 ---------- */
.profile__history {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.profile__history-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius);
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.profile__history-item:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: var(--el-box-shadow-light);
}

.profile__history-cover {
  flex: 0 0 auto;
  width: 96px;
  height: 68px;
  border-radius: var(--app-radius);
  overflow: hidden;
  background: var(--el-fill-color-light);
}

.profile__history-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.profile__history-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.profile__history-body {
  flex: 1;
  min-width: 0;
}

.profile__history-name {
  margin: 0 0 6px;
  font-size: 16px;
}

.profile__history-meta {
  font-size: 12px;
  margin-bottom: 6px;
}

.profile__dot {
  margin: 0 6px;
}

.profile__history-time {
  font-size: 12px;
}

.profile__history-more {
  flex: 0 0 auto;
  color: var(--el-color-primary);
  font-size: 12px;
}
</style>
