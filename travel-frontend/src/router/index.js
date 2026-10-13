/**
 * 路由与全局守卫（F-ROUTER）。
 *
 * <p>约定：
 * <ul>
 *   <li>全部路由**懒加载**（`() => import(...)`）；</li>
 *   <li>`meta.requiresAuth` 标记需登录；`meta.requiresAdmin` 标记需管理员；</li>
 *   <li>`meta.title` 用于设置文档标题。</li>
 * </ul>
 *
 * <p>全局前置守卫 `beforeEach`：
 * <ol>
 *   <li>目标含 `meta.requiresAuth` 且无 token → 跳 `/login?redirect=<fullPath>`；</li>
 *   <li>目标含 `meta.requiresAdmin` 且 `role !== 'admin'` → 跳 `/403`。</li>
 * </ol>
 * 守卫仅做**跳转拦截**，真实权限一律由后端裁定（后端仍会返回 401/403）。
 *
 * <p>本模块先注册占位路由 `/login`、`/register`、`/`、`/403`（另含 404 兜底），
 * 后续业务模块在此**新增**自己的路由即可。
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ROLE } from '@/utils/constants'

/** 路由表。 */
const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    // 顶部导航「景点」入口 → 首页景点列表（列表页即首页，公开）
    path: '/scenic',
    redirect: '/'
  },
  {
    path: '/scenic/:id',
    name: 'scenic-detail',
    component: () => import('@/views/ScenicDetail.vue'),
    meta: { title: '景点详情' }
  },
  {
    // 顶部导航「个人中心」入口 → 个人中心聚合页（三 Tab）
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/profile/settings',
    name: 'profile-settings',
    component: () => import('@/views/ProfileSettings.vue'),
    meta: { title: '资料设置', requiresAuth: true }
  },
  {
    // 顶部导航「我的行程」入口 → 行程列表
    path: '/trips',
    redirect: '/trip/list'
  },
  {
    path: '/trip/generate',
    name: 'trip-generate',
    component: () => import('@/views/TripGenerate.vue'),
    meta: { title: '生成攻略', requiresAuth: true }
  },
  {
    path: '/trip/list',
    name: 'trip-list',
    component: () => import('@/views/TripList.vue'),
    meta: { title: '我的行程', requiresAuth: true }
  },
  {
    // 静态段 `edit` 先于 `/trip/:id` 注册，避免被 `:id` 吞掉
    path: '/trip/:id/edit',
    name: 'trip-edit',
    component: () => import('@/views/TripEdit.vue'),
    meta: { title: '编辑行程', requiresAuth: true }
  },
  {
    path: '/trip/:id',
    name: 'trip-detail',
    component: () => import('@/views/TripDetail.vue'),
    meta: { title: '行程详情', requiresAuth: true }
  },
  {
    path: '/chat',
    name: 'chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { title: 'AI 问答', requiresAuth: true }
  },
  {
    // 后台管理（F-ADMIN，需登录 + admin 角色）；侧边栏 + 子页面布局
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { title: '后台管理', requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/scenic' },
      {
        path: 'scenic',
        name: 'admin-scenic',
        component: () => import('@/views/admin/AdminScenic.vue'),
        meta: { title: '景点管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/AdminUsers.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'trips',
        name: 'admin-trips',
        component: () => import('@/views/admin/AdminTrips.vue'),
        meta: { title: '行程管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'chats',
        name: 'admin-chats',
        component: () => import('@/views/admin/AdminChats.vue'),
        meta: { title: '对话记录', requiresAuth: true, requiresAdmin: true }
      }
    ]
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/Forbidden.vue'),
    meta: { title: '无权限' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

/** 路由器实例。 */
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

/**
 * 全局前置守卫：登录态与管理员权限拦截。
 *
 * @param {import('vue-router').RouteLocationNormalized} to 目标路由
 * @returns {boolean|object} `true` 放行，或重定向描述对象
 */
router.beforeEach((to) => {
  const userStore = useUserStore()

  // 1) 需登录但无 token → 跳登录，保留回跳地址
  if (to.meta && to.meta.requiresAuth && !userStore.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 2) 需管理员但角色不符 → 跳 403（守卫仅拦截，真实权限由后端裁定）
  if (to.meta && to.meta.requiresAdmin && userStore.role !== ROLE.ADMIN) {
    return { path: '/403' }
  }

  return true
})

/** 后置钩子：设置文档标题。 */
router.afterEach((to) => {
  const title = to.meta && to.meta.title
  document.title = title ? `${title} - AI 旅游攻略` : 'AI 旅游攻略'
})

export default router
