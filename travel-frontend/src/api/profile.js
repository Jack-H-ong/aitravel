/**
 * 个人中心接口（F-API，对应后端 `ProfileController`，前缀 `/api/profile`）。
 *
 * <p>需登录；「我的行程」「我的收藏」分别复用 `/api/trip/list`、`/api/scenic/my-favorites`。
 * 具体页面逻辑由 f-profile 模块补齐。
 */
import request from '@/utils/request'

/**
 * 浏览历史分页（需登录）。
 *
 * @param {{page?:number,size?:number}} [params]
 * @returns {Promise<{records:Array,total:string,page:string,size:string}>}
 */
export function getBrowseHistory(params) {
  return request.get('/profile/history', { params })
}
