/**
 * 用户与认证接口（F-API，对应后端 `AuthController`，前缀 `/api/auth`）。
 *
 * <p>说明：本模块（f-foundation）仅建立文件与统一导出约定，函数签名按**后端实际契约为准**；
 * 具体请求/错误处理逻辑由 f-user 模块补齐（薄封装直接返回 request 结果即可）。
 *
 * <p>返回：`request` 的响应拦截器已剥离 `{code,message,data}` 外层，直接得到 `data`。
 */
import request from '@/utils/request'

/**
 * 用户注册。
 *
 * @param {{username:string,password:string,confirmPassword:string}} data 注册请求体
 * @returns {Promise<void>}
 */
export function register(data) {
  return request.post('/auth/register', data)
}

/**
 * 用户登录。
 *
 * @param {{username:string,password:string}} data 登录请求体
 * @returns {Promise<{token:string,expiresIn:string,user:object}>} 登录响应（expiresIn 为字符串）
 */
export function login(data) {
  return request.post('/auth/login', data)
}

/**
 * 获取当前登录用户信息（需登录）。
 *
 * @returns {Promise<object>} UserVO（id 为字符串）
 */
export function getMe() {
  return request.get('/auth/me')
}

/**
 * 更新当前用户信息（需登录）。
 *
 * @param {{nickname?:string,avatar?:string,oldPassword?:string,newPassword?:string}} data
 * @returns {Promise<void>}
 */
export function updateProfile(data) {
  return request.put('/auth/update', data)
}
