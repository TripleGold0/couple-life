/**
 * 应用入口文件
 * --------------------------------------------------
 * 职责：
 *  1. 创建 Vue 3 应用根实例并挂载到 #app 节点
 *  2. 注册 Pinia 状态管理（用于 userStore 等）
 *  3. 注册 Vue Router（路由及鉴权守卫）
 *  4. 注册 Element Plus UI 组件库与全局样式
 *
 * 项目背景：情侣空间 couple-life-frontend，前后端分离，
 *           后端为 Java Spring Boot，使用 JWT 鉴权。
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import './assets/styles/global.css'
import App from './App.vue'
import router from './router'

// 创建应用实例并按顺序挂载插件，最后挂载到 index.html 的 #app
createApp(App)
  .use(createPinia())
  .use(router)
  .use(ElementPlus, { locale: zhCn })
  .mount('#app')
