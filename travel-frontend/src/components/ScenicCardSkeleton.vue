<script setup>
/**
 * 景点卡片加载骨架（F-COMPONENT）。
 *
 * <p>列表加载中展示，占位数量与卡片网格保持一致，避免布局跳动。
 */
defineProps({
  /** 骨架数量。 */
  count: {
    type: Number,
    default: 8
  }
})
</script>

<template>
  <div class="skeleton-grid">
    <el-card
      v-for="n in count"
      :key="n"
      class="skeleton-card"
      shadow="never"
      :body-style="{ padding: '0' }"
    >
      <el-skeleton animated>
        <template #template>
          <el-skeleton-item variant="image" class="skeleton-card__cover" />
          <div class="skeleton-card__body">
            <el-skeleton-item variant="h3" class="skeleton-card__title" />
            <el-skeleton-item variant="text" class="skeleton-card__line" />
            <el-skeleton-item variant="text" class="skeleton-card__line skeleton-card__line--short" />
          </div>
        </template>
      </el-skeleton>
    </el-card>
  </div>
</template>

<style scoped>
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--app-gap);
}

.skeleton-card {
  overflow: hidden;
}

.skeleton-card__cover {
  width: 100%;
  height: 160px;
}

.skeleton-card__body {
  padding: 12px 14px 14px;
}

.skeleton-card__title {
  width: 60%;
  margin-bottom: 10px;
}

.skeleton-card__line {
  width: 100%;
  margin-bottom: 8px;
}

.skeleton-card__line--short {
  width: 40%;
  margin-bottom: 0;
}
</style>
