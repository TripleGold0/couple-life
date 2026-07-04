/**
 * 默认头像生成工具
 * --------------------------------------------------
 * 提供基于性别（gender）的内联 SVG 头像，并以 data URI 形式导出，
 * 避免对静态图片资源的依赖；当用户未上传头像时自动回退使用。
 *
 * gender 取值约定：
 *   1 = 男     -> DEFAULT_AVATAR_MALE
 *   2 = 女     -> DEFAULT_AVATAR_FEMALE
 *   其它/0/null = 中性 -> DEFAULT_AVATAR_NEUTRAL
 */

// Warm, gender-based default avatars rendered as inline SVG data URIs.
// gender: 1 = 男, 2 = 女, 其它 = 中性

// 女性默认头像（粉色系背景 + 长发 + 蝴蝶结 + 心形装饰）
const femaleSvg = `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'>
  <defs>
    <linearGradient id='bgF' x1='0' y1='0' x2='1' y2='1'>
      <stop offset='0' stop-color='#ffd6e7'/>
      <stop offset='1' stop-color='#ffb6c8'/>
    </linearGradient>
  </defs>
  <rect width='120' height='120' rx='60' fill='url(#bgF)'/>
  <!-- hair back -->
  <path d='M28 70c0-22 14-38 32-38s32 16 32 38v6H28z' fill='#5b3a2e'/>
  <!-- face -->
  <circle cx='60' cy='58' r='22' fill='#ffe2cf'/>
  <!-- bangs -->
  <path d='M40 50c4-12 14-18 20-18s16 6 20 18c-6-4-14-6-20-6s-14 2-20 6z' fill='#5b3a2e'/>
  <!-- cheeks -->
  <circle cx='49' cy='64' r='3' fill='#ff8aa8' opacity='.6'/>
  <circle cx='71' cy='64' r='3' fill='#ff8aa8' opacity='.6'/>
  <!-- eyes -->
  <circle cx='52' cy='58' r='2' fill='#3b2a2a'/>
  <circle cx='68' cy='58' r='2' fill='#3b2a2a'/>
  <!-- smile -->
  <path d='M54 68c2 3 10 3 12 0' stroke='#c2536a' stroke-width='2' fill='none' stroke-linecap='round'/>
  <!-- bow -->
  <path d='M40 38l8 4-8 4z' fill='#ff7aa3'/>
  <path d='M48 40l-2 2 2 2z' fill='#e35e8a'/>
  <!-- shoulders -->
  <path d='M22 110c4-14 18-22 38-22s34 8 38 22z' fill='#fff1cc'/>
  <!-- heart -->
  <path d='M84 90c0-3 3-5 5-3 2-2 5 0 5 3 0 4-5 7-5 7s-5-3-5-7z' fill='#ff6f9f'/>
</svg>`

// 男性默认头像（蓝色系背景 + 短发 + 衬衣领 + 心形装饰）
const maleSvg = `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'>
  <defs>
    <linearGradient id='bgM' x1='0' y1='0' x2='1' y2='1'>
      <stop offset='0' stop-color='#cfe6ff'/>
      <stop offset='1' stop-color='#a8c8ff'/>
    </linearGradient>
  </defs>
  <rect width='120' height='120' rx='60' fill='url(#bgM)'/>
  <!-- face -->
  <circle cx='60' cy='58' r='22' fill='#ffe0c7'/>
  <!-- hair -->
  <path d='M38 50c0-12 10-22 22-22s22 10 22 22c-4-4-10-6-14-6-4 0-6 4-8 4s-4-4-8-4-10 2-14 6z' fill='#3a2a22'/>
  <!-- cheeks -->
  <circle cx='49' cy='64' r='3' fill='#ffae8c' opacity='.6'/>
  <circle cx='71' cy='64' r='3' fill='#ffae8c' opacity='.6'/>
  <!-- eyes -->
  <circle cx='52' cy='58' r='2' fill='#2c2018'/>
  <circle cx='68' cy='58' r='2' fill='#2c2018'/>
  <!-- smile -->
  <path d='M54 68c2 3 10 3 12 0' stroke='#a85746' stroke-width='2' fill='none' stroke-linecap='round'/>
  <!-- shoulders -->
  <path d='M22 110c4-14 18-22 38-22s34 8 38 22z' fill='#ffeacc'/>
  <!-- collar -->
  <path d='M50 90l10 10 10-10-10-4z' fill='#ffffff' opacity='.8'/>
  <!-- heart -->
  <path d='M84 90c0-3 3-5 5-3 2-2 5 0 5 3 0 4-5 7-5 7s-5-3-5-7z' fill='#ff6f9f'/>
</svg>`

// 中性默认头像（暖色背景，未指定性别或保密时使用）
const neutralSvg = `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'>
  <defs>
    <linearGradient id='bgN' x1='0' y1='0' x2='1' y2='1'>
      <stop offset='0' stop-color='#fde0c2'/>
      <stop offset='1' stop-color='#ffd6e7'/>
    </linearGradient>
  </defs>
  <rect width='120' height='120' rx='60' fill='url(#bgN)'/>
  <circle cx='60' cy='58' r='22' fill='#ffe2cf'/>
  <path d='M40 52c2-10 10-18 20-18s18 8 20 18c-6-2-14-4-20-4s-14 2-20 4z' fill='#6b4a3a'/>
  <circle cx='52' cy='58' r='2' fill='#3b2a2a'/>
  <circle cx='68' cy='58' r='2' fill='#3b2a2a'/>
  <path d='M54 68c2 3 10 3 12 0' stroke='#c2536a' stroke-width='2' fill='none' stroke-linecap='round'/>
  <path d='M22 110c4-14 18-22 38-22s34 8 38 22z' fill='#fff1cc'/>
  <path d='M82 86c0-4 4-6 7-3 3-3 7-1 7 3 0 6-7 10-7 10s-7-4-7-10z' fill='#ff6f9f'/>
</svg>`

/**
 * 将 SVG 字符串转为可直接给 <img src> 使用的 data URI
 * @param {string} svg 内联 SVG 字符串
 * @returns {string} data:image/svg+xml;utf8,... 形式的 URI
 */
function toDataUri(svg) {
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}

// 三个默认头像的 data URI 常量
export const DEFAULT_AVATAR_FEMALE = toDataUri(femaleSvg)
export const DEFAULT_AVATAR_MALE = toDataUri(maleSvg)
export const DEFAULT_AVATAR_NEUTRAL = toDataUri(neutralSvg)

/**
 * 根据性别返回对应的默认头像 data URI
 * @param {number} gender 1=男 2=女 其它=中性
 * @returns {string} 对应的 data URI
 */
export function defaultAvatarFor(gender) {
  if (gender === 1) return DEFAULT_AVATAR_MALE
  if (gender === 2) return DEFAULT_AVATAR_FEMALE
  return DEFAULT_AVATAR_NEUTRAL
}

/**
 * 取出某个用户实际使用的头像：优先使用 user.avatar，
 * 否则按 user.gender 回退到默认头像
 * @param {Object} user 用户对象，可能为 null/undefined
 * @returns {string} 头像 URL 或 data URI
 */
export function avatarOf(user) {
  if (!user) return DEFAULT_AVATAR_NEUTRAL
  return user.avatar || defaultAvatarFor(user.gender)
}
