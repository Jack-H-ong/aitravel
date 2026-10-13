/**
 * API 统一出口（F-API 约定）。
 *
 * <p>各业务模块按域分文件，并在此统一导出，便于页面以
 * `import { login } from '@/api/auth'` 或 `import authApi from '@/api'` 两种方式使用。
 */
export * as authApi from './auth'
export * as scenicApi from './scenic'
export * as tripApi from './trip'
export * as chatApi from './chat'
export * as profileApi from './profile'
export * as adminApi from './admin'
