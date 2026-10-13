/**
 * 统一 axios 封装（F-UTIL）——全工程唯一请求出口。
 *
 * <p>约定（后端契约）：
 * <ul>
 *   <li>响应体统一为 {@code {code, message, data}}，{@code code === 200} 成功；</li>
 *   <li>成功时**返回 `data`**（已剥离外层包装），失败时 `ElMessage.error` 并 reject；</li>
 *   <li>错误码：400 业务错误、401 未认证（清 token 跳 /login）、403 无权限、
 *       404 不存在、500 服务器错误、5001 AI 服务异常。</li>
 * </ul>
 *
 * <p>注意：本模块**不打印** token / 密码等敏感信息。
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken, clearAuth } from '@/utils/auth'

/** 后端错误码常量。 */
import { CODE } from '@/utils/constants'

/**
 * 跳转登录页，并保留当前路径作为登录后回跳地址。
 * 若已在登录页则不重复跳转，避免死循环。
 */
function redirectToLogin() {
  clearAuth()
  const current = router.currentRoute.value
  if (current && current.path === '/login') {
    return
  }
  router.push({
    path: '/login',
    query: { redirect: current ? current.fullPath : '/' }
  })
}

/**
 * 统一处理业务错误码。
 *
 * @param {number} code 业务错误码
 * @param {string} [message] 后端返回文案
 */
function handleBusinessError(code, message) {
  switch (code) {
    case CODE.UNAUTHORIZED:
      redirectToLogin()
      ElMessage.error(message || '登录已过期，请重新登录')
      break
    case CODE.FORBIDDEN:
      ElMessage.error(message || '无权限')
      break
    case CODE.NOT_FOUND:
      ElMessage.error(message || '资源不存在')
      break
    case CODE.SERVER_ERROR:
      ElMessage.error(message || '服务器内部错误')
      break
    case CODE.AI_ERROR:
      // 5001：展示后端具体文案（生成失败/问答不可用）
      ElMessage.error(message || 'AI 服务暂时不可用，请稍后重试')
      break
    case CODE.BAD_REQUEST:
    default:
      // 400 参数/业务错误：优先展示后端文案
      ElMessage.error(message || '请求失败')
      break
  }
}

/** axios 实例：baseURL `/api`（由 Vite dev server 代理到后端），超时 30s。 */
const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

/**
 * 请求拦截器：从 localStorage 读取 token，存在则注入 `Authorization: Bearer <token>`。
 */
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/**
 * 响应拦截器：
 * - HTTP 2xx：若为统一响应体且 `code === 200`，返回 `data`；否则按错误码提示并 reject。
 * - HTTP 非 2xx：按 HTTP 状态码与响应体 `code` 统一提示；401 清 token 跳 /login。
 */
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 非统一响应结构（如文件流/纯文本）原样返回
    if (res === null || typeof res !== 'object' || !('code' in res)) {
      return res
    }
    if (Number(res.code) === CODE.SUCCESS) {
      return res.data
    }
    handleBusinessError(Number(res.code), res.message)
    return Promise.reject(new Error(res.message || `业务错误（code=${res.code}）`))
  },
  (error) => {
    const response = error.response
    if (response) {
      const body = response.data
      // 后端若在错误响应体中带业务 code，以业务 code 为准
      const bizCode = body && typeof body === 'object' && 'code' in body ? Number(body.code) : null
      const message = body && typeof body === 'object' ? body.message : ''
      if (bizCode !== null && bizCode !== CODE.SUCCESS) {
        handleBusinessError(bizCode, message)
      } else {
        handleBusinessError(Number(response.status), message)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default service
