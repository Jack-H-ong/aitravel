/**
 * 景点接口（F-API，对应后端 `ScenicController`，前缀 `/api/scenic`）。
 *
 * <p>列表/详情为**公开接口**；收藏相关接口需登录。
 * 具体页面逻辑由 f-scenic 模块补齐。
 */
import request from '@/utils/request'

/**
 * 景点分页列表（公开）。
 *
 * @param {{city?:string,category?:string,keyword?:string,page?:number,size?:number}} params
 * @returns {Promise<{records:Array,total:string,page:string,size:string}>} 分页结果（total/page/size 为字符串）
 */
export function listScenics(params) {
  return request.get('/scenic/list', { params })
}

/**
 * 景点详情（公开，会累加浏览量）。
 *
 * @param {string|number} id 景点 id（字符串比较，按字符串传参）
 * @returns {Promise<object>} ScenicDetailVO
 */
export function getScenicDetail(id) {
  return request.get(`/scenic/${id}`)
}

/**
 * 切换收藏状态（需登录）。后端按当前状态取反。
 *
 * @param {string|number} scenicId 景点 id
 * @returns {Promise<object>} FavoriteVO
 */
export function toggleFavorite(scenicId) {
  return request.post('/scenic/favorite', { scenicId })
}

/**
 * 我的收藏分页列表（需登录）。
 *
 * @param {{page?:number,size?:number}} [params]
 * @returns {Promise<{records:Array,total:string,page:string,size:string}>}
 */
export function getMyFavorites(params) {
  return request.get('/scenic/my-favorites', { params })
}
