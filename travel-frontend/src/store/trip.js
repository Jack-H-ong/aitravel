/**
 * 行程草稿状态（F-STORE）。
 *
 * <p>本模块仅建立结构；生成表单 / 编辑的具体逻辑由 f-trip 模块补齐。
 * 草稿用于「生成参数 → 生成结果 → 编辑」之间的临时状态保持。
 */
import { defineStore } from 'pinia'

/** 生成参数默认值（对应后端 GenerateTripRequest）。 */
function defaultDraft() {
  return {
    city: '',
    days: 2,
    budget: '',
    preferences: [],
    spendMode: ''
  }
}

/**
 * 行程 store。
 */
export const useTripStore = defineStore('trip', {
  state: () => ({
    /** 生成参数草稿。 */
    draft: defaultDraft(),
    /** 最近一次生成的行程详情（含 dayList）。 */
    current: null,
    /** 最近一次生成是否进行中（供 loading 展示）。 */
    generating: false
  }),

  getters: {
    /** 草稿是否已填写城市（生成前置校验用）。 */
    canGenerate: (state) => !!state.draft.city
  },

  actions: {
    /**
     * 合并更新草稿。
     *
     * @param {object} patch 部分字段
     */
    setDraft(patch) {
      this.draft = { ...this.draft, ...patch }
    },

    /**
     * 重置草稿。
     */
    resetDraft() {
      this.draft = defaultDraft()
    },

    /**
     * 设置当前行程详情。
     *
     * @param {object|null} trip TripDetailVO
     */
    setCurrent(trip) {
      this.current = trip
    }
  }
})
