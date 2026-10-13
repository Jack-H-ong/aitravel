/**
 * 行程接口（F-API，对应后端 `TripController`，前缀 `/api/trip`）。
 *
 * <p>全部需登录；生成接口走 AI，失败时后端返回 `code=5001`。
 * 具体页面逻辑由 f-trip 模块补齐。
 */
import request from '@/utils/request'

/**
 * 生成行程攻略（AI，需登录）。
 *
 * @param {{city:string,days:number,budget:string,preferences:string[],spendMode:string}} data
 * @returns {Promise<object>} TripDetailVO（含 dayList）
 */
export function generateTrip(data) {
  return request.post('/trip/generate', data)
}

/**
 * 我的行程分页列表（需登录）。
 *
 * @param {{city?:string,page?:number,size?:number}} [params]
 * @returns {Promise<{records:Array,total:string,page:string,size:string}>}
 */
export function listTrips(params) {
  return request.get('/trip/list', { params })
}

/**
 * 行程详情（需登录）。
 *
 * @param {string|number} id 行程 id
 * @returns {Promise<object>} TripDetailVO
 */
export function getTripDetail(id) {
  return request.get(`/trip/${id}`)
}

/**
 * 更新行程（需登录）。
 *
 * @param {string|number} id 行程 id
 * @param {{title?:string,dayList?:Array}} data 更新请求体
 * @returns {Promise<void>}
 */
export function updateTrip(id, data) {
  return request.put(`/trip/${id}`, data)
}

/**
 * 删除行程（需登录）。
 *
 * @param {string|number} id 行程 id
 * @returns {Promise<void>}
 */
export function deleteTrip(id) {
  return request.delete(`/trip/${id}`)
}
