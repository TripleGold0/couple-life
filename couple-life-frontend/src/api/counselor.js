/**
 * 情感顾问（Counselor / LLM）API 模块
 * --------------------------------------------------
 * 提供与后端 LLM 对话接口的封装，并支持用户在前端自定义
 * 大模型服务（baseUrl/apiKey/model）。配置仅保存在
 * localStorage，不会上传服务器永久存储。
 */
import request from '../utils/request'

// 自定义 LLM 配置在 localStorage 中的存储 key
const STORAGE_KEY = 'counselor_llm_config'

/**
 * 读取用户在前端配置的自定义模型设置（baseUrl/apiKey/model）
 * @returns {{baseUrl: string, apiKey: string, model: string}}
 */
// 读取用户在前端配置的自定义模型设置（baseUrl/apiKey/model）
export function getLlmConfig() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : { baseUrl: '', apiKey: '', model: '' }
  } catch (_) {
    // JSON 解析失败时返回空配置（容错）
    return { baseUrl: '', apiKey: '', model: '' }
  }
}

/**
 * 保存自定义 LLM 配置到 localStorage
 * @param {{baseUrl?: string, apiKey?: string, model?: string}} cfg
 */
export function saveLlmConfig(cfg) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(cfg || {}))
}

/**
 * 清除自定义 LLM 配置，恢复使用默认免费模型
 */
export function clearLlmConfig() {
  localStorage.removeItem(STORAGE_KEY)
}

/**
 * 与情感顾问聊天：把会话历史发给后端，由后端转发给 LLM
 *  - LLM 响应可能较慢，单独将超时放宽到 90s
 *  - 若用户配置了自定义模型，会一并提交给后端透传
 * @param {Array<{role: 'user'|'assistant', content: string}>} messages 会话历史
 * @returns {Promise<{reply: string}>} 顾问回复
 */
// LLM 响应可能较慢，单独放宽超时到 90s
export const counselorChat = messages => {
  const cfg = getLlmConfig()
  const payload = { messages }
  // 只把非空字段透传给后端，后端缺省时使用默认免费模型
  if (cfg.baseUrl) payload.baseUrl = cfg.baseUrl
  if (cfg.apiKey) payload.apiKey = cfg.apiKey
  if (cfg.model) payload.model = cfg.model
  return request.post('/api/counselor/chat', payload, { timeout: 90000 })
}
