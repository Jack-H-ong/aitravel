<script setup>
/**
 * 404 页面不存在（f-foundation）。
 *
 * <p>由 `/:pathMatch(.*)*` 兜底路由渲染，对应「404 资源不存在」的前端体验。
 */
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found page">
    <el-result icon="error" title="404" sub-title="页面不存在">
      <template #extra>
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.not-found {
  padding-top: 48px;
}
</style>
