<script setup>
/**
 * 景点卡片（F-COMPONENT）。
 *
 * <p>展示：封面图（无图/加载失败时占位）、名称、城市 · 分类、评分、收藏数、价格
 * （0 显示「免费」）。点击整卡跳转详情 `/scenic/:id`。
 *
 * <p>所有文本均为纯文本渲染（禁用 v-html），图片以 `:src` 绑定。
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { formatPrice, emptyText } from '@/utils/format'

const props = defineProps({
  /** 景点 VO（id 为字符串）。 */
  scenic: {
    type: Object,
    required: true
  }
})

const router = useRouter()

/** 封面地址：取 images[0]，无图返回空串。 */
const cover = computed(() => {
  const images = props.scenic.images
  return Array.isArray(images) && images.length > 0 ? images[0] : ''
})

/** 封面是否加载失败（失败时回退占位）。 */
const coverFailed = ref(false)

/** 是否展示占位图（无地址或加载失败）。 */
const showPlaceholder = computed(() => !cover.value || coverFailed.value)

/** 价格文案（0 → 免费）。 */
const priceText = computed(() => formatPrice(props.scenic.price))

/** 是否免费。 */
const isFree = computed(() => priceText.value === '免费')

/** 评分（数字，默认 0）。 */
const rating = computed(() => Number(props.scenic.avgRating) || 0)

/** 分类文案（空值兜底）。 */
const categoryText = computed(() => emptyText(props.scenic.category, '未分类'))

function handleImgError() {
  coverFailed.value = true
}

/** 跳转景点详情。 */
function goDetail() {
  const id = props.scenic && props.scenic.id
  if (id !== undefined && id !== null && id !== '') {
    router.push(`/scenic/${id}`)
  }
}
</script>

<template>
  <el-card
    class="scenic-card"
    shadow="hover"
    :body-style="{ padding: '0' }"
    @click="goDetail"
  >
    <div class="scenic-card__cover">
      <img
        v-if="!showPlaceholder"
        class="scenic-card__img"
        :src="cover"
        :alt="scenic.name"
        @error="handleImgError"
      />
      <div v-else class="scenic-card__placeholder">暂无图片</div>
    </div>

    <div class="scenic-card__body">
      <h3 class="scenic-card__name text-ellipsis">{{ scenic.name }}</h3>

      <div class="scenic-card__meta">
        <span class="scenic-card__city">{{ emptyText(scenic.city) }}</span>
        <span class="scenic-card__dot">·</span>
        <span class="scenic-card__category">{{ categoryText }}</span>
      </div>

      <div class="scenic-card__stats">
        <span class="scenic-card__rating">评分 {{ rating.toFixed(1) }}</span>
        <span class="scenic-card__fav">收藏 {{ Number(scenic.favoriteCount) || 0 }}</span>
      </div>

      <div class="scenic-card__footer">
        <span :class="['scenic-card__price', { 'is-free': isFree }]">{{ priceText }}</span>
        <span class="scenic-card__detail">查看详情 ›</span>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.scenic-card {
  cursor: pointer;
  overflow: hidden;
}

.scenic-card__cover {
  width: 100%;
  height: 160px;
  background: var(--el-fill-color-light);
}

.scenic-card__img {
  width: 100%;
  height: 160px;
  object-fit: cover;
  display: block;
}

.scenic-card__placeholder {
  width: 100%;
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
  background: repeating-linear-gradient(
    45deg,
    var(--el-fill-color-light),
    var(--el-fill-color-light) 10px,
    var(--el-fill-color) 10px,
    var(--el-fill-color) 20px
  );
}

.scenic-card__body {
  padding: 12px 14px 14px;
}

.scenic-card__name {
  margin: 0 0 6px;
  font-size: 16px;
}

.scenic-card__meta {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-bottom: 8px;
}

.scenic-card__dot {
  margin: 0 6px;
}

.scenic-card__stats {
  display: flex;
  gap: 16px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-bottom: 10px;
}

.scenic-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.scenic-card__price {
  color: var(--el-color-danger);
  font-size: 16px;
  font-weight: 600;
}

.scenic-card__price.is-free {
  color: var(--el-color-success);
}

.scenic-card__detail {
  color: var(--el-color-primary);
  font-size: 12px;
}
</style>
