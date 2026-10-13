/**
 * Pinia 统一出口（F-STORE 约定）。
 *
 * <p>各业务模块按域分文件，并在此统一导出，便于页面
 * `import { useUserStore } from '@/store'` 使用。
 */
export { useUserStore } from './user'
export { useScenicStore } from './scenic'
export { useTripStore } from './trip'
export { useChatStore } from './chat'
