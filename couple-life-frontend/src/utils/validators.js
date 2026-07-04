/**
 * 账号格式校验工具
 * ----------------------------------------------------------
 * 统一前端手机号 / 邮箱的正则与 Element Plus el-form 校验规则，
 * 避免散落在 Login / Register / Profile 等多处重复定义。
 *
 * 后端对应位置：AuthServiceImpl 中的 PHONE_PATTERN / EMAIL_PATTERN
 * （两端正则保持一致，避免"前端通过、后端拒绝"的体验割裂）
 */

/** 中国大陆手机号：1 开头、第 2 位 3-9、共 11 位 */
export const PHONE_REGEX = /^1[3-9]\d{9}$/

/**
 * 邮箱：与后端 EMAIL_PATTERN 对齐。
 * 不追求完整 RFC 5322 校验，覆盖 99% 真实邮箱即可，避免误拒。
 */
export const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

export const isPhone = (v) => PHONE_REGEX.test(v || '')
export const isEmail = (v) => EMAIL_REGEX.test(v || '')

/** el-form 校验器：必须是合法手机号 */
export const phoneValidator = (_, value, cb) => {
  if (!value) return cb(new Error('请输入手机号'))
  if (!isPhone(value)) return cb(new Error('手机号格式不正确'))
  cb()
}

/** el-form 校验器：必须是合法邮箱 */
export const emailValidator = (_, value, cb) => {
  if (!value) return cb(new Error('请输入邮箱'))
  if (!isEmail(value)) return cb(new Error('邮箱格式不正确'))
  cb()
}

/** el-form 校验器：手机号或邮箱二选一（用于密码登录的 account 字段） */
export const phoneOrEmailValidator = (_, value, cb) => {
  if (!value) return cb(new Error('请输入手机号或邮箱'))
  if (!isPhone(value) && !isEmail(value)) return cb(new Error('请输入正确的手机号或邮箱'))
  cb()
}
