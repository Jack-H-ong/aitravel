/**
 * 后台管理接口（F-API，对应后端 `AdminController`，前缀 `/api/admin`）。
 *
 * <p>全部需管理员权限（`role=admin`）；普通用户访问由后端返回 `403`。
 * 具体页面逻辑由 f-admin 模块补齐。
 */
import request from '@/utils/request'

/**
 * 景点管理分页列表。
 *
 * @param {{city?:string,category?:string,keyword?:string,page?:number,size?:number}} [params]
 * @returns {Promise<{records:Array,total:string,page:string,size:string}>}
 */
export function listAdminScenics(params) {
  return request.get('/admin/scenic', { params })
}

/**
 * 新增景点。
 *
 * @param {object} data ScenicSaveRequest（tags/images 为数组）
 * @returns {Promise<string>} 新景点 id（字符串）
 */
export function createScenic(data) {
  return request.post('/admin/scenic', data)
}

/**
 * 编辑景点。
 *
 * @param {string|number} id 景点 id
 * @param {object} data ScenicSaveRequest
 * @returns {Promise<void>}
 */
export function updateScenic(id, data) {
  return request.put(`/admin/scenic/${id}`, data)
}

/**
 * 删除景点（逻辑删除）。
 *
 * @param {string|number} id 景点 id
 * @returns {Promise<void>}
 */
export function deleteScenic(id) {
  return request.delete(`/admin/scenic/${id}`)
}

/**
 * 用户管理分页列表。
 *
 * @param {{keyword?:string,page?:number,size?:number}} [params]
 * @returns {Promise<{records:Array,total:string,page:string,size:string}>}
 */
export function listUsers(params) {
  return request.get('/admin/users', { params })
}

/**
 * 修改用户状态（0=禁用，1=正常）。
 *
 * @param {string|number} id 用户 id
 * @param {number} status 目标状态
 * @returns {Promise<void>}
 */
export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, { status })
}

/**
 * 行程管理分页列表。
 *
 * @param {{page?:number,size?:number}} [params]
 * @returns {Promise<{records:Array,total:string,page:string,size:string}>}
 */
export function listAdminTrips(params) {
  return request.get('/admin/trips', { params })
}

/**
 * 对话记录分页列表。
 *
 * @param {{page?:number,size?:number}} [params]
 * @returns {Promise<{records:Array,total:string,page:string,size:string}>}
 */
export function listAdminChats(params) {
  return request.get('/admin/chats', { params })
}
