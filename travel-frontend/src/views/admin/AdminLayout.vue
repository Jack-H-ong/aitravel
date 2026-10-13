<script setup>
/**
 * 后台管理布局（F-ROUTER，路由 `/admin`）。
 *
 * <p>左侧固定侧边栏承载四个管理入口（景点/用户/行程/对话），右侧 `<router-view>`
 * 渲染子路由页面。菜单使用 `el-menu` 的 `router` 模式，`index` 即目标路径，
 * 高亮项由当前路由 `path` 决定。
 *
 * <p>权限（需登录 + admin 角色）由父路由 `meta.requiresAuth/requiresAdmin` 及全局
 * 守卫统一拦截，此处不做重复判定。
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

/** 侧边栏菜单项。 */
const menus = [
  { path: '/admin/scenic', label: '景点管理' },
  { path: '/admin/users', label: '用户管理' },
  { path: '/admin/trips', label: '行程管理' },
  { path: '/admin/chats', label: '对话记录' }
]

/** 当前高亮菜单（跟随路由）。 */
const activeMenu = computed(() => route.path)
</script>

<template>
  <div class="admin">
    <aside class="admin__aside">
      <div class="admin__title">后台管理</div>
      <el-menu :default-active="activeMenu" router class="admin__menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <section class="admin__content">
      <router-view />
    </section>
  </div>
</template>

<style scoped>
.admin {
  display: flex;
  align-items: stretch;
  gap: 20px;
  min-height: 480px;
}

.admin__aside {
  flex: 0 0 180px;
  width: 180px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius);
  overflow: hidden;
}

.admin__title {
  padding: 16px;
  font-weight: 600;
  font-size: 15px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.admin__menu {
  border-right: none;
}

.admin__content {
  flex: 1 1 auto;
  min-width: 0;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius);
  padding: 16px;
}
</style>
