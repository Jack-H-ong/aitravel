<script setup>
/**
 * 根组件：顶部导航（品牌 + 用户区）+ 路由出口（f-foundation）。
 *
 * <p>全局约定：
 * <ul>
 *   <li>导航入口在此预留，具体页面由各业务模块补齐（新增前须先在 router 注册路由）；</li>
 *   <li>用户区依据登录态展示；「退出登录」= 前端清 token 并跳 /login；</li>
 *   <li>所有用户/AI 文本一律**纯文本**渲染，禁用 v-html。</li>
 * </ul>
 */
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

/** 导航项（占位；后续模块补页面时相应登记路由）。 */
const navItems = [
  { label: '首页', to: '/' },
  { label: '我的行程', to: '/trips' },
  { label: 'AI 问答', to: '/chat' }
]

/** 是否展示后台入口。 */
const showAdmin = computed(() => userStore.isAdmin)

/** 头像兜底文字（昵称首字符）。 */
const avatarText = computed(() => (userStore.displayName || 'U').charAt(0))

/** 已登录用户区下拉菜单选中项。 */
function handleUserCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'settings') {
    router.push('/profile/settings')
  } else if (command === 'logout') {
    handleLogout()
  }
}

/**
 * 退出登录：清除本地 token 并回到登录页。
 */
function handleLogout() {
  userStore.logout()
  router.push('/login')
}

/**
 * 应用启动恢复会话：已登录（localStorage 有 token）则拉取最新用户信息；
 * 失败（含 401）由 request 拦截器处理（清 token 跳 /login）。
 */
onMounted(() => {
  if (userStore.isLogin) {
    userStore.fetchMe().catch(() => {})
  }
})
</script>

<template>
  <el-container class="app">
    <el-header class="app__header">
      <div class="app__brand" @click="router.push('/')">
        <span class="app__logo">✈</span>
        <span class="app__name">AI 旅游攻略</span>
      </div>

      <nav class="app__nav">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="app__nav-link"
        >
          {{ item.label }}
        </router-link>
        <router-link v-if="showAdmin" to="/admin" class="app__nav-link">
          后台管理
        </router-link>
      </nav>

      <div class="app__user">
        <template v-if="userStore.isLogin">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <span class="app__user-trigger">
              <el-avatar :size="28" :src="userStore.userInfo?.avatar || ''">
                {{ avatarText }}
              </el-avatar>
              <span class="app__username">{{ userStore.displayName || '已登录' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">资料设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button link type="primary" @click="router.push('/login')">登录</el-button>
          <el-button link @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>

    <el-main class="app__main">
      <router-view v-slot="{ Component }">
        <component :is="Component" />
      </router-view>
    </el-main>
  </el-container>
</template>

<style scoped>
.app {
  min-height: 100vh;
}

.app__header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 28px;
  height: 64px;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: saturate(180%) blur(12px);
  -webkit-backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid rgba(24, 40, 38, 0.07);
}

.app__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  white-space: nowrap;
  user-select: none;
}

.app__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  font-size: 17px;
  color: #fff;
  background: linear-gradient(135deg, var(--app-primary) 0%, var(--app-primary-deep) 100%);
  box-shadow: 0 6px 14px -6px rgba(14, 124, 107, 0.5);
}

.app__name {
  font-family: var(--app-font-display);
  font-size: 18px;
  font-weight: 600;
  color: var(--app-ink);
}

.app__nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.app__nav-link {
  padding: 7px 14px;
  border-radius: 999px;
  color: var(--app-ink-soft);
  font-size: 14px;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.app__nav-link:hover {
  background: var(--app-primary-soft);
  color: var(--app-primary-ink);
}

.app__nav-link.router-link-active {
  background: var(--app-primary-soft);
  color: var(--app-primary-ink);
  font-weight: 600;
}

.app__user {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.app__username {
  font-size: 14px;
  color: var(--app-ink);
  font-weight: 500;
}

.app__user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.app__main {
  background: transparent;
}
</style>
