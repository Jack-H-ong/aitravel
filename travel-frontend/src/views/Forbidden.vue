<script setup>
/**
 * 403 无权限页（f-foundation）。
 *
 * <p>由路由守卫（`meta.requiresAdmin` 角色不符）或后端 403 提示跳转至此。
 */
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="forbidden page">
    <el-result icon="warning" title="403" sub-title="抱歉，你没有访问该页面的权限">
      <template #extra>
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.forbidden {
  padding-top: 48px;
}
</style>
