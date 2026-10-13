<script setup>
/**
 * 空状态占位（F-COMPONENT）。
 *
 * <p>列表/筛选无结果时展示（非报错）。支持默认插槽追加操作按钮。
 */
defineProps({
  /** 提示文案。 */
  description: {
    type: String,
    default: '暂无数据'
  },
  /** 插图尺寸。 */
  imageSize: {
    type: Number,
    default: 100
  }
})
</script>

<template>
  <div class="empty-state">
    <el-empty :description="description" :image-size="imageSize">
      <slot />
    </el-empty>
  </div>
</template>

<style scoped>
.empty-state {
  padding: 40px 0;
}
</style>
