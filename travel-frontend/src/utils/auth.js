/**
 * 认证相关的本地存储读写（F-UTIL）。
 *
 * <p>约定：Token 存 localStorage，key 统一为 {@link TOKEN_KEY}；
 * 登出 = 前端清除 token（无服务端接口）。为支撑路由守卫，额外持久化
 * 角色与用户信息，避免刷新页面后守卫无法判定角色。
 */

/** Token 存储 key。 */
export const TOKEN_KEY = 'aitravel_token'

/** 角色存储 key。 */
export const ROLE_KEY = 'aitravel_role'

/** 用户信息存储 key。 */
export const USER_KEY = 'aitravel_user'

/**
 * 读取 Token。
 *
 * @returns {string} token 字符串；不存在时为空字符串
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/**
 * 写入 Token。
 *
 * @param {string} token JWT 令牌
 */
export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  }
}

/**
 * 移除 Token。
 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 读取角色（user/admin）。
 *
 * @returns {string} 角色字符串；不存在时为空字符串
 */
export function getRole() {
  return localStorage.getItem(ROLE_KEY) || ''
}

/**
 * 写入角色。
 *
 * @param {string} role 角色
 */
export function setRole(role) {
  if (role) {
    localStorage.setItem(ROLE_KEY, role)
  }
}

/**
 * 移除角色。
 */
export function removeRole() {
  localStorage.removeItem(ROLE_KEY)
}

/**
 * 读取用户信息对象。
 *
 * @returns {object|null} 用户信息；不存在或解析失败时为 null
 */
export function getUserInfo() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

/**
 * 写入用户信息对象。
 *
 * @param {object} user 用户信息
 */
export function setUserInfo(user) {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  }
}

/**
 * 移除用户信息。
 */
export function removeUserInfo() {
  localStorage.removeItem(USER_KEY)
}

/**
 * 一次性清除全部登录态（token + 角色 + 用户信息）。
 */
export function clearAuth() {
  removeToken()
  removeRole()
  removeUserInfo()
}
