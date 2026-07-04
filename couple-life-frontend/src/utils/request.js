/**
 * Axios 请求封装与统一拦截器
 * --------------------------------------------------
 * 职责：
 *  1. 创建全局唯一的 axios 实例（baseURL/timeout）
 *  2. 请求拦截器：
 *     - 从 localStorage 读取 token，自动注入 Authorization 请求头
 *     - 记录请求开始时间，便于响应耗时统计
 *     - 打印请求日志（开发环境）
 *  3. 响应拦截器：
 *     - 统一解析后端约定的 { code, message, data } 包装格式
 *     - code === 200 时直接返回 data，业务层无需自行剥壳
 *     - code === 401 / HTTP 401|403：清除 token 并跳转登录页
 *     - 其它异常：通过 ElMessage 全局弹出错误提示
 *  4. JWT 鉴权：与后端 Spring Boot Security/JWT 过滤器配合
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { createLogger } from './logger'

// 专用 logger，控制台中以 [HTTP] 标识
const log = createLogger('HTTP')

// 创建 axios 实例：开发环境通过 Vite 代理转发到后端，因此 baseURL 留空
const request = axios.create({
  baseURL: '',
  timeout: 15000
})

/**
 * 请求拦截器：注入 token + 记录请求起始时间 + 打日志
 */
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  // 已登录则带上 Authorization 头（后端返回的 token 已含 "Bearer " 前缀，原样透传）
  if (token) {
    config.headers.Authorization = token
  }
  // 在 config 上挂一个 metadata，用于响应阶段计算耗时
  config.metadata = { start: Date.now() }
  log.debug(`--> ${config.method?.toUpperCase()} ${config.url}`, config.params || config.data || '')
  return config
}, error => {
  // 请求阶段直接抛出的异常（极少见，比如配置错误）
  log.error('请求发起失败', error)
  return Promise.reject(error)
})

/**
 * 处理未授权场景：清空 token 并跳转登录页
 * 已经在 /login 时不重复跳转，避免循环
 */
function handleUnauthorized() {
  localStorage.removeItem('token')
  if (location.pathname !== '/login') {
    location.replace('/login')
  }
}

/**
 * 响应拦截器：统一处理后端 { code, message, data } 包装
 *  - code === 200：成功，直接返回 data
 *  - code === 401：登录失效，清 token 并跳转登录
 *  - 其它 code：视为业务失败，弹出 message 并 reject
 *  - HTTP 异常：401/403 跳转登录，其它弹出网络异常
 */
request.interceptors.response.use(response => {
  const cost = Date.now() - (response.config.metadata?.start || Date.now())
  const result = response.data
  // 业务码 401：登录已过期
  if (result.code === 401) {
    log.warn(`<-- ${response.config.url} 401 ${cost}ms`, result.message)
    ElMessage.error(result.message || '登录已过期，请重新登录')
    handleUnauthorized()
    return Promise.reject(new Error(result.message || '未登录'))
  }
  // 业务码非 200：通用业务失败
  if (result.code !== 200) {
    log.warn(`<-- ${response.config.url} biz-fail ${cost}ms`, result)
    ElMessage.error(result.message || '请求失败')
    return Promise.reject(new Error(result.message || '请求失败'))
  }
  // 成功：剥掉 code/message 包装，把 data 直接吐给业务层
  log.debug(`<-- ${response.config.url} 200 ${cost}ms`)
  return result.data
}, error => {
  // 网络错误 / HTTP 状态码非 2xx
  const cost = Date.now() - (error.config?.metadata?.start || Date.now())
  const status = error.response?.status
  const url = error.config?.url
  log.error(`<-- ${url} ${status || 'NETWORK'} ${cost}ms`, error.response?.data || error.message)
  if (status === 401 || status === 403) {
    // HTTP 层鉴权失败也走统一登出流程
    ElMessage.error('登录已过期，请重新登录')
    handleUnauthorized()
  } else {
    ElMessage.error(error.response?.data?.message || '网络异常')
  }
  return Promise.reject(error)
})

export default request
