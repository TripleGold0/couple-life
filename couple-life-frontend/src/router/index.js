/**
 * 应用路由定义
 * --------------------------------------------------
 * 路由结构：
 *  /login     —— 登录页（公开，无需鉴权）
 *  /register  —— 注册页（公开，无需鉴权）
 *  /          —— 主布局 MainLayout（需鉴权），默认重定向到 /home
 *    ├── home       情侣主页（首页概览）
 *    ├── checkin    每日打卡日历
 *    ├── travel     旅行地图日志
 *    ├── album      情侣相册
 *    ├── counselor  情感顾问 Agent（LLM 对话）
 *    └── profile    个人信息与情侣绑定
 *
 * 全局守卫：
 *  - publicPages 之外的页面均要求 localStorage 中存在 token
 *  - 未登录访问受保护页面会被强制跳转到 /login
 *  - token 校验由 utils/request.js 与后端 401 响应共同兜底
 */
import { createRouter, createWebHistory } from 'vue-router'

// 路由表：所有页面均使用动态 import 实现路由级懒加载，减小首屏体积
const routes = [
  // 公开路由：Landing Page、登录与注册页
  { path: '/', component: () => import('../views/Landing.vue') },
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') },
  // 主布局（侧边栏 + 顶部 + 内容区），子路由都在此布局内渲染
  {
    path: '/app',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/app/home',
    children: [
      { path: 'home', component: () => import('../views/Home.vue') },           // 主页
      { path: 'checkin', component: () => import('../views/Checkin.vue') },     // 每日打卡
      { path: 'travel', component: () => import('../views/Travel.vue') },       // 旅行地图
      { path: 'album', component: () => import('../views/Album.vue') },         // 情侣相册
      { path: 'counselor', component: () => import('../views/Counselor.vue') }, // 情感顾问
      { path: 'pet', component: () => import('../views/Pet.vue') },             // 电子宠物（详情 / 选择）
      { path: 'profile', component: () => import('../views/Profile.vue') }      // 个人信息
    ]
  }
]

// 使用 HTML5 History 模式（依赖部署环境配置 fallback 到 index.html）
const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局前置守卫：登录态检查
 * 进入非公开页面时若无 token，则强制跳转到 /login
 * @param {RouteLocationNormalized} to    即将进入的路由
 * @param {RouteLocationNormalized} from  当前导航正要离开的路由
 * @param {Function} next                 放行 / 重定向函数
 */
router.beforeEach((to, from, next) => {
  // 不需要登录即可访问的白名单
  const publicPages = ['/', '/login', '/register']
  const token = localStorage.getItem('token')
  // 已登录用户访问 Landing Page，直接跳转到应用主页
  if (to.path === '/' && token) {
    next('/app/home')
    return
  }
  // 未登录访问受保护页面，跳转到登录页
  if (!publicPages.includes(to.path) && !token) {
    next('/login')
    return
  }
  next()
})

export default router
