/**
 * 前端统一日志工具。
 * - 通过 import.meta.env.DEV 自动区分开发 / 生产
 * - 生产仅输出 warn / error，开发输出全部级别
 * - 可通过 localStorage.setItem('LOG_LEVEL', 'debug'|'info'|'warn'|'error'|'silent') 临时调整
 */

// 日志级别枚举（数值越大优先级越高，silent 表示完全静默）
const LEVELS = { debug: 10, info: 20, warn: 30, error: 40, silent: 99 }

/**
 * 解析当前生效的日志级别
 * 优先级：localStorage 的 LOG_LEVEL > 开发环境 debug > 生产环境 warn
 * @returns {number} 当前生效的级别数值
 */
function resolveLevel() {
  const override = typeof localStorage !== 'undefined' ? localStorage.getItem('LOG_LEVEL') : null
  if (override && LEVELS[override] != null) return LEVELS[override]
  return import.meta.env.DEV ? LEVELS.debug : LEVELS.warn
}

/**
 * 生成日志前缀：[时间戳] [tag]
 * @param {string} tag 模块标签
 * @returns {string} 格式化前缀
 */
function fmt(tag) {
  const time = new Date().toISOString().substring(11, 23)
  return `[${time}] [${tag}]`
}

/**
 * 工厂：根据级别 & tag 创建一个对应的日志方法
 * @param {string} level 日志级别名称
 * @param {string} tag 模块标签
 * @param {Function} consoleFn 实际写入的 console 方法
 * @returns {Function} 与 console.log 同签名的函数
 */
function make(level, tag, consoleFn) {
  return (...args) => {
    // 当前级别低于阈值则跳过
    if (LEVELS[level] < resolveLevel()) return
    consoleFn(fmt(tag), ...args)
  }
}

/**
 * 创建一个带 tag 的 logger 实例
 * @param {string} [tag='APP'] 模块标签，便于在控制台中区分来源
 * @returns {{debug: Function, info: Function, warn: Function, error: Function}}
 */
export function createLogger(tag = 'APP') {
  return {
    debug: make('debug', tag, console.debug.bind(console)),
    info: make('info', tag, console.info.bind(console)),
    warn: make('warn', tag, console.warn.bind(console)),
    error: make('error', tag, console.error.bind(console))
  }
}

// 默认导出一个 tag 为 APP 的 logger 实例，供应用通用日志使用
const logger = createLogger('APP')
export default logger
