/**
 * 旅行（Travel）API 模块
 * --------------------------------------------------
 * 提供情侣旅行记录的增删改查接口，配合 Travel.vue 中的 echarts
 * 散点地图展示。
 */
import request from '../utils/request'

/**
 * 新增一条旅行记录
 * @param {Object} data 包含 locationName / 经纬度 / 日期 / 双方感受 / 图片等
 * @returns {Promise<any>}
 */
export const addTravel = data => request.post('/api/travels', data)

/**
 * 获取当前情侣的所有旅行记录（用于地图打点）
 * @returns {Promise<Array>}
 */
export const getTravels = () => request.get('/api/travels')

/**
 * 获取单条旅行的详细信息（包含图片轮播、双方感受等）
 * @param {number|string} id 旅行记录 ID
 * @returns {Promise<Object>}
 */
export const getTravelDetail = id => request.get(`/api/travels/${id}`)

/**
 * 更新一条旅行记录
 * @param {number|string} id 记录 ID
 * @param {Object} data 待更新的字段
 * @returns {Promise<any>}
 */
export const updateTravel = (id, data) => request.put(`/api/travels/${id}`, data)

/**
 * 删除一条旅行记录
 * @param {number|string} id 记录 ID
 * @returns {Promise<any>}
 */
export const deleteTravel = id => request.delete(`/api/travels/${id}`)
