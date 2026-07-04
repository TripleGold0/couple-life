/**
 * 用户与情侣关系（User / Couple）API 模块
 * --------------------------------------------------
 * 包含：当前用户信息、资料更新、首次完善资料、情侣绑定/解绑、
 *      首页聚合数据、通用图片上传等接口。
 */
import request from '../utils/request'

/**
 * 获取当前登录用户信息（含 partner / inviteCode 等）
 * @returns {Promise<Object>}
 */
export const getCurrentUser = () => request.get('/api/user/me')

/**
 * 更新用户资料（昵称、性别、生日、头像等）
 * @param {Object} data 待更新字段
 * @returns {Promise<any>}
 */
export const updateProfile = data => request.put('/api/user/profile', data)

/**
 * 首次登录强制完善资料（昵称 + 性别 + 密码）
 * 主要用于短信验证码首次登录的兜底场景
 * @param {{nickname: string, gender: number, password: string}} data
 * @returns {Promise<any>}
 */
export const completeProfile = data => request.post('/api/user/complete-profile', data)

/**
 * 通过对方的邀请码绑定情侣关系
 * @param {{inviteCode: string, loveStartDate: string}} data
 * @returns {Promise<any>}
 */
export const bindCouple = data => request.post('/api/couple/bind', data)

/**
 * 解除当前情侣绑定
 * @returns {Promise<any>}
 */
export const unbindCouple = () => request.post('/api/couple/unbind')

/**
 * 获取首页聚合摘要（恋爱天数 / 最近打卡 / 最近旅行 / 最近照片）
 * @returns {Promise<Object>}
 */
export const getHomeSummary = () => request.get('/api/home/summary')

/**
 * 通用图片上传（头像 / 旅行图 / 相册图等场景共用）
 * @param {File} file 浏览器原生 File 对象
 * @param {string} [module='common'] 业务模块标识，便于后端归类存储
 * @returns {Promise<{url: string}>} 服务器返回的可访问 URL
 */
export const uploadImage = (file, module = 'common') => {
  const data = new FormData()
  data.append('file', file)
  data.append('module', module)
  return request.post('/api/upload/image', data)
}
