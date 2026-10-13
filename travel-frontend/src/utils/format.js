/**
 * 通用格式化工具（F-UTIL）。
 *
 * <p>类型约定（后端契约）：`price`/`avgRating` 为 JSON 数字，免费为 `0`；
 * 时间字段 `LocalDateTime` 已序列化为 `"yyyy-MM-dd HH:mm:ss"` 字符串；
 * `id`/`scenicId`/`tripId`/`userId`/分页 `total`/`page`/`size` 均为**字符串**。
 */

/**
 * 格式化价格：0（或空）显示「免费」，否则显示 `¥xx`。
 *
 * @param {number|string|null|undefined} price 价格（可能为字符串形态的 BigDecimal）
 * @returns {string} 展示文案
 */
export function formatPrice(price) {
  if (price === null || price === undefined || price === '') {
    return '免费'
  }
  const n = Number(price)
  if (!Number.isFinite(n) || n <= 0) {
    return '免费'
  }
  // 去掉浮点尾零：60.00 -> 60，4.90 -> 4.9
  const text = String(Math.round(n * 100) / 100)
  return `¥${text}`
}

/**
 * 格式化日期时间。后端已是 `"yyyy-MM-dd HH:mm:ss"`，直读即可；
 * 其它可解析输入做安全兜底，无法解析时返回兜底文案。
 *
 * @param {string|number|Date|null|undefined} value 时间值
 * @param {string} [fallback=''] 兜底文案
 * @returns {string} 展示文案
 */
export function formatDate(value, fallback = '') {
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  if (typeof value === 'string') {
    // 后端契约格式直接返回
    if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(value)) {
      return value
    }
  }
  const d = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(d.getTime())) {
    return fallback
  }
  const pad = (n) => String(n).padStart(2, '0')
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}

/**
 * 空值兜底文案。
 *
 * @param {*} value 任意值
 * @param {string} [placeholder='-'] 空值时的占位符
 * @returns {*} 非空原值或占位符
 */
export function emptyText(value, placeholder = '-') {
  if (value === null || value === undefined || value === '') {
    return placeholder
  }
  if (Array.isArray(value) && value.length === 0) {
    return placeholder
  }
  return value
}
