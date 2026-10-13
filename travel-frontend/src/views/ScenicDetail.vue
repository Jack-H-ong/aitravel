<script setup>
/**
 * 景点详情页（F-VIEW，路由 `/scenic/:id`，公开）。
 *
 * <p>展示：图片（`images[]` 轮播 / 首图 / 占位）、名称、城市 · 地址、标签、分类、
 * 开放时间、门票（0 显示「免费」）、评分 / 收藏数 / 浏览量、简介。
 *
 * <p>收藏按钮：
 * <ul>
 *   <li>已登录：调用 `toggleFavorite(id)`，用响应的 `isFavorite` 回写收藏态，
 *       并按 `action` 增减 `favoriteCount`；</li>
 *   <li>未登录：跳 `/login?redirect=<当前详情页>`；收藏失败（401/404）由 request
 *       拦截器统一提示。</li>
 * </ul>
 *
 * <p>「返回列表」回跳 `/`，Home 读取 `scenicStore` 恢复筛选与分页。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getScenicDetail, toggleFavorite } from '@/api/scenic'
import { useUserStore } from '@/store/user'
import { formatPrice, emptyText } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 详情加载态。 */
const loading = ref(false)
/** 收藏请求进行中。 */
const favoriting = ref(false)
/** 详情数据。 */
const detail = ref(null)
/** 详情加载失败（如景点不存在 404）。 */
const loadFailed = ref(false)
/** 加载失败的图片地址集合（用于回退占位）。 */
const brokenImages = ref({})

/** 有效图片地址列表。 */
const images = computed(() => {
  const list = detail.value && detail.value.images
  return Array.isArray(list) ? list.filter((url) => !!url) : []
})

/** 门票文案（0 → 免费）。 */
const priceText = computed(() => formatPrice(detail.value && detail.value.price))

/** 是否免费。 */
const isFree = computed(() => priceText.value === '免费')

/** 评分（数字，默认 0）。 */
const rating = computed(() => Number(detail.value && detail.value.avgRating) || 0)

/** 收藏态。 */
const isFavorite = computed(() => !!(detail.value && detail.value.isFavorite))

/** 标签列表。 */
const tags = computed(() => {
  const list = detail.value && detail.value.tags
  return Array.isArray(list) ? list : []
})

/** 当前收藏按钮文案。 */
const favoriteText = computed(() => (isFavorite.value ? '已收藏' : '收藏'))

/**
 * 拉取详情（公开接口，会累加浏览量）。
 */
async function fetchDetail() {
  loading.value = true
  loadFailed.value = false
  try {
    const data = await getScenicDetail(route.params.id)
    if (data) {
      detail.value = data
    } else {
      detail.value = null
      loadFailed.value = true
    }
  } catch (e) {
    // 错误提示由 request 拦截器统一展示（如 404 景点不存在）
    detail.value = null
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

/** 图片加载失败 → 标记为占位。 */
function markImageBroken(url) {
  brokenImages.value = { ...brokenImages.value, [url]: true }
}

/**
 * 切换收藏：未登录引导登录；已登录调用接口并回写收藏态与收藏数。
 */
async function handleFavorite() {
  if (!userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!detail.value || favoriting.value) {
    return
  }

  favoriting.value = true
  try {
    const res = await toggleFavorite(detail.value.id)
    // 后端返回 { action, isFavorite }；以 isFavorite 为准，action 兜底
    const nowFavorite =
      res && res.isFavorite !== undefined
        ? !!res.isFavorite
        : !!(res && res.action === 'favorite')

    const count = Number(detail.value.favoriteCount) || 0
    detail.value.isFavorite = nowFavorite
    detail.value.favoriteCount = nowFavorite ? count + 1 : Math.max(count - 1, 0)
    ElMessage.success(nowFavorite ? '已加入收藏' : '已取消收藏')
  } catch (e) {
    // 401 未登录 / 404 景点不存在：由 request 拦截器统一提示
  } finally {
    favoriting.value = false
  }
}

/** 返回列表（Home 读取 store 恢复筛选与分页）。 */
function goBack() {
  router.push('/')
}

onMounted(fetchDetail)
</script>

<template>
  <div class="detail page">
    <el-button class="detail__back" text @click="goBack">‹ 返回列表</el-button>

    <el-skeleton v-if="loading" :rows="8" animated class="detail__skeleton" />

    <EmptyState
      v-else-if="loadFailed || !detail"
      description="景点不存在或已下架"
    >
      <el-button type="primary" @click="goBack">返回列表</el-button>
    </EmptyState>

    <template v-else>
      <el-card class="detail__card" shadow="never">
        <div class="detail__layout">
          <div class="detail__gallery">
            <el-carousel v-if="images.length > 1" height="320px" :interval="5000">
              <el-carousel-item v-for="url in images" :key="url">
                <img
                  v-if="!brokenImages[url]"
                  class="detail__img"
                  :src="url"
                  :alt="detail.name"
                  @error="markImageBroken(url)"
                />
                <div v-else class="detail__placeholder">暂无图片</div>
              </el-carousel-item>
            </el-carousel>

            <template v-else-if="images.length === 1">
              <img
                v-if="!brokenImages[images[0]]"
                class="detail__img detail__img--single"
                :src="images[0]"
                :alt="detail.name"
                @error="markImageBroken(images[0])"
              />
              <div v-else class="detail__placeholder detail__placeholder--single">暂无图片</div>
            </template>

            <div v-else class="detail__placeholder detail__placeholder--single">暂无图片</div>
          </div>

          <div class="detail__info">
            <h1 class="detail__name">{{ detail.name }}</h1>

            <div class="detail__location text-secondary">
              {{ emptyText(detail.city) }} · {{ emptyText(detail.address) }}
            </div>

            <div class="detail__tags">
              <el-tag type="primary" effect="plain" class="detail__tag">
                {{ emptyText(detail.category, '未分类') }}
              </el-tag>
              <el-tag
                v-for="tag in tags"
                :key="tag"
                type="info"
                effect="plain"
                class="detail__tag"
              >
                {{ tag }}
              </el-tag>
            </div>

            <div class="detail__stats">
              <div class="detail__stat">
                <span class="detail__stat-value">{{ rating.toFixed(1) }}</span>
                <span class="detail__stat-label">评分</span>
              </div>
              <div class="detail__stat">
                <span class="detail__stat-value">{{ Number(detail.favoriteCount) || 0 }}</span>
                <span class="detail__stat-label">收藏</span>
              </div>
              <div class="detail__stat">
                <span class="detail__stat-value">{{ Number(detail.viewCount) || 0 }}</span>
                <span class="detail__stat-label">浏览</span>
              </div>
            </div>

            <ul class="detail__facts">
              <li>
                <span class="detail__fact-label">开放时间</span>
                <span>{{ emptyText(detail.openTime) }}</span>
              </li>
              <li>
                <span class="detail__fact-label">门票</span>
                <span :class="['detail__price', { 'is-free': isFree }]">{{ priceText }}</span>
              </li>
            </ul>

            <div class="detail__actions">
              <el-button
                :type="isFavorite ? 'default' : 'danger'"
                :loading="favoriting"
                @click="handleFavorite"
              >
                {{ favoriteText }}
              </el-button>
              <el-button @click="goBack">返回列表</el-button>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="detail__card" shadow="never">
        <template #header>景点简介</template>
        <p class="detail__description">{{ emptyText(detail.description, '暂无简介') }}</p>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.detail {
  padding-top: 8px;
}

.detail__back {
  margin-bottom: 8px;
}

.detail__card {
  margin-bottom: 20px;
}

.detail__layout {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
}

.detail__gallery {
  flex: 1 1 360px;
  min-width: 280px;
  border-radius: var(--app-radius);
  overflow: hidden;
  background: var(--el-fill-color-light);
}

.detail__img {
  width: 100%;
  height: 320px;
  object-fit: cover;
  display: block;
}

.detail__img--single {
  height: 320px;
}

.detail__placeholder {
  width: 100%;
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  font-size: 14px;
  background: repeating-linear-gradient(
    45deg,
    var(--el-fill-color-light),
    var(--el-fill-color-light) 12px,
    var(--el-fill-color) 12px,
    var(--el-fill-color) 24px
  );
}

.detail__info {
  flex: 1 1 340px;
  min-width: 280px;
}

.detail__name {
  font-size: 24px;
  margin: 0 0 8px;
}

.detail__location {
  font-size: 13px;
  margin-bottom: 12px;
}

.detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.detail__stats {
  display: flex;
  gap: 32px;
  padding: 12px 0;
  border-top: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 16px;
}

.detail__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.detail__stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.detail__stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.detail__facts {
  margin: 0 0 20px;
}

.detail__facts li {
  display: flex;
  gap: 12px;
  padding: 4px 0;
  font-size: 14px;
}

.detail__fact-label {
  width: 68px;
  color: var(--el-text-color-secondary);
  flex: 0 0 auto;
}

.detail__price {
  color: var(--el-color-danger);
  font-weight: 600;
}

.detail__price.is-free {
  color: var(--el-color-success);
}

.detail__actions {
  display: flex;
  gap: 12px;
}

.detail__description {
  color: var(--el-text-color-regular);
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
}
</style>
