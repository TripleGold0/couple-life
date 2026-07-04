/**
 * 鉴权（Auth）API 模块
 * --------------------------------------------------
 * 提供注册、登录、图形/短信验证码相关接口。
 * 登录成功后由调用方将返回的 token 写入 localStorage，
 * 后续请求由 request.js 拦截器自动携带。
 */
import request from '../utils/request'

/**
 * 发送短信/邮箱验证码
 * @param {{account: string, type: 'REGISTER'|'LOGIN'|string}} data 账号与场景类型
 * @returns {Promise<{captcha?: string}>} 开发环境会回显验证码方便调试
 */
export const sendCaptcha = data => request.post('/api/auth/captcha', data)

/**
 * 获取图形验证码（用于密码登录）
 * @returns {Promise<{captchaKey: string, captchaImage: string}>} key 与图片 base64
 */
export const getImageCaptcha = () => request.get('/api/auth/image-captcha')

/**
 * 用户注册
 * @param {Object} data 注册信息（用户名/昵称/手机或邮箱/密码/验证码 等）
 * @returns {Promise<any>}
 */
export const register = data => request.post('/api/auth/register', data)

/**
 * 用户登录（支持账号密码 / 短信验证码两种方式，由 loginType 区分）
 * @param {Object} data 登录参数
 * @returns {Promise<{token: string, user: Object}>} JWT token 与用户信息
 */
export const login = data => request.post('/api/auth/login', data)
