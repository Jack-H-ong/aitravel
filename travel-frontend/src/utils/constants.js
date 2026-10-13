/**
 * 全局常量（F-UTIL）。
 *
 * <p>集中放置后端契约相关的常量，供 request/store/各业务模块复用，
 * 避免魔法值散落各处。
 */

/** 后端统一响应码。 */
export const CODE = {
  /** 成功。 */
  SUCCESS: 200,
  /** 参数/业务规则错误。 */
  BAD_REQUEST: 400,
  /** 未认证（Token 缺失/无效/过期、账号禁用）。 */
  UNAUTHORIZED: 401,
  /** 无权限。 */
  FORBIDDEN: 403,
  /** 资源不存在。 */
  NOT_FOUND: 404,
  /** 服务器内部错误。 */
  SERVER_ERROR: 500,
  /** AI 服务异常。 */
  AI_ERROR: 5001
}

/** 分页默认参数（后端 page 从 1 起，size 默认 10、上限 100）。 */
export const PAGE = {
  DEFAULT_PAGE: 1,
  DEFAULT_SIZE: 10,
  MAX_SIZE: 100
}

/** 角色常量。 */
export const ROLE = {
  USER: 'user',
  ADMIN: 'admin'
}
