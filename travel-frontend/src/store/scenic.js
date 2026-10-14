/**
 * 景点筛选状态（F-STORE）。
 *
 * <p>本模块仅建立结构；列表页的具体逻辑由 f-scenic 模块补齐。
 * 用于在列表 / 详情 / 返回列表之间**保留筛选条件与分页**。
 */
import { defineStore } from 'pinia'
import { PAGE } from '@/utils/constants'

/** 筛选条件默认值。 */
function defaultFilters() {
  return {
    city: '',
    category: '',
    keyword: '',
    page: PAGE.DEFAULT_PAGE,
    size: 12 // 首页景点列表默认每页 12 条
  }
}

/**
 * 景点筛选 store。
 */
export const useScenicStore = defineStore('scenic', {
  state: () => ({
    /** 当前筛选条件。 */
    filters: defaultFilters(),
    /** 最近一次列表结果（records 中的 id 为字符串）。 */
    list: [],
    /** 分页总数（字符串，来自后端）。 */
    total: '0'
  }),

  getters: {
    /** 是否设置了任何筛选条件（用于展示「清空筛选」）。 */
    hasFilter: (state) =>
      !!(state.filters.city || state.filters.category || state.filters.keyword)
  },

  actions: {
    /**
     * 合并设置筛选条件。
     *
     * @param {object} patch 部分筛选条件
     */
    setFilters(patch) {
      this.filters = { ...this.filters, ...patch }
    },

    /**
     * 重置筛选条件（保留默认分页）。
     */
    resetFilters() {
      this.filters = defaultFilters()
    },

    /**
     * 记录列表结果。
     *
     * @param {Array} records 列表数据
     * @param {string|number} total 总数（字符串）
     */
    setList(records, total) {
      this.list = Array.isArray(records) ? records : []
      this.total = total === undefined || total === null ? '0' : String(total)
    }
  }
})
