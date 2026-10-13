/**
 * 用户状态（F-STORE）。
 *
 * <p>本模块（f-foundation）建立**结构骨架**：state / getters / actions 签名，
 * 以及 token/角色/用户信息的**持久化读写**（刷新后登录态不丢）。
 * `login` / `fetchMe` 的具体请求逻辑由 f-user 模块补齐；`logout` 依据全局约定
 * 「登出 = 前端清 token」已可直接使用。
 */
import { defineStore } from 'pinia'
import {
  getToken,
  setToken,
  clearAuth,
  getRole,
  setRole,
  getUserInfo,
  setUserInfo
} from '@/utils/auth'
import { ROLE } from '@/utils/constants'
import * as authApi from '@/api/auth'

/**
 * 用户 store。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    /** JWT 令牌（持久化于 localStorage）。 */
    token: getToken(),
    /** 当前用户信息 UserVO（持久化）；未登录为 null。 */
    userInfo: getUserInfo(),
    /** 角色：user/admin（持久化）。 */
    role: getRole()
  }),

  getters: {
    /** 是否已登录（存在 token 即视为已登录）。 */
    isLogin: (state) => !!state.token,
    /** 是否为管理员。 */
    isAdmin: (state) => state.role === ROLE.ADMIN,
    /** 展示用昵称（昵称优先，其次用户名）。 */
    displayName: (state) =>
      (state.userInfo && (state.userInfo.nickname || state.userInfo.username)) || ''
  },

  actions: {
    /**
     * 写入会话（登录成功后调用）：token + 用户信息 + 角色，并持久化。
     *
     * @param {string} token JWT
     * @param {object} [user] UserVO
     */
    setSession(token, user) {
      this.token = token || ''
      setToken(this.token)
      if (user) {
        this.userInfo = user
        setUserInfo(user)
        this.role = user.role || ''
        setRole(this.role)
      }
    },

    /**
     * 更新用户信息（改昵称/头像后调用），并持久化。
     *
     * @param {object} user UserVO（部分字段）
     */
    setUser(user) {
      this.userInfo = { ...(this.userInfo || {}), ...(user || {}) }
      setUserInfo(this.userInfo)
      if (this.userInfo.role) {
        this.role = this.userInfo.role
        setRole(this.role)
      }
    },

    /**
     * 登出：清除 token 与用户信息（前端清 token，无服务端接口）。
     */
    logout() {
      this.token = ''
      this.userInfo = null
      this.role = ''
      clearAuth()
    },

    /**
     * 登录：调用登录接口，成功后持久化 token + 用户信息 + 角色。
     *
     * <p>错误（用户名或密码错误 400 / 账号已被禁用 401 等）由 `request` 拦截器统一
     * `ElMessage` 展示；此处仅向上抛出，供页面结束 loading / 决定是否跳转。
     *
     * @param {{username:string,password:string}} payload 登录参数
     * @returns {Promise<object>} 登录成功的用户信息 UserVO
     */
    async login(payload) {
      const res = await authApi.login(payload)
      const token = res && res.token
      const user = res && res.user
      // 持久化 token 与用户信息（role 一并写入，供刷新后路由守卫判定）
      this.setSession(token, user)
      return user || res
    },

    /**
     * 拉取当前用户信息（刷新页面后恢复/同步用户信息）。
     *
     * <p>未登录（无 token）时直接返回 null，不发请求；请求失败（含 401）由
     * `request` 拦截器处理（清 token 跳 /login），本 action 幂等：重复调用只会
     * 用后端最新数据合并覆盖 `userInfo`。
     *
     * @returns {Promise<object|null>} 最新用户信息；未登录时为 null
     */
    async fetchMe() {
      if (!this.token) {
        return null
      }
      const user = await authApi.getMe()
      if (user) {
        this.setUser(user)
      }
      return user
    }
  }
})
