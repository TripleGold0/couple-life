/**
 * 相册（Album）API 模块
 * --------------------------------------------------
 * 提供照片上传 / 列表 / 评论的封装，所有接口均通过统一的
 * request 实例发送，自动注入 JWT token 并解包后端响应。
 */
import axios from 'axios'
import request from '../utils/request'

/**
 * 批量上传照片（multipart/form-data）
 * @param {FormData} formData 包含 files / shootDate / title / description 等字段
 * @returns {Promise<any>} 上传结果
 */
export const uploadPhotos = formData => request.post('/api/album/photos', formData)

/**
 * 获取相册中所有照片，后端按日期分组返回 [{ date, photos: [...] }]
 * @returns {Promise<Array>}
 */
export const getPhotos = () => request.get('/api/album/photos')

/**
 * 给指定照片添加评论
 * @param {number|string} photoId 照片 ID
 * @param {{content: string}} data 评论内容
 * @returns {Promise<any>}
 */
export const addPhotoComment = (photoId, data) => request.post(`/api/album/photos/${photoId}/comments`, data)

/**
 * 获取指定照片下的所有评论
 * @param {number|string} photoId 照片 ID
 * @returns {Promise<Array>} 评论列表
 */
export const getPhotoComments = photoId => request.get(`/api/album/photos/${photoId}/comments`)

/**
 * 批量导出指定照片为 ZIP。
 *
 * <p>因为返回的是二进制流而非通用 {@code {code,message,data}} 包装，
 * 这里绕过 {@code request} 实例的响应拦截器，直接用 axios 发起请求；
 * 鉴权头与拦截器保持一致——后端返回的 token 已含 "Bearer " 前缀，原样透传。
 *
 * <p>异常处理：后端校验失败时会返回 JSON 错误（content-type 非 zip），
 * 调用方需读取 Blob 文本以解析错误消息。
 *
 * @param {Array<number|string>} photoIds 照片 id 列表
 * @returns {Promise<import('axios').AxiosResponse<Blob>>} 完整的 axios 响应（含 headers / data）
 */
export const exportPhotos = photoIds => {
  const token = localStorage.getItem('token') || ''
  return axios.post('/api/album/photos/export', photoIds, {
    headers: { Authorization: token, 'Content-Type': 'application/json' },
    responseType: 'blob',
    timeout: 120000
  })
}
