/**
 * 用户状态管理 Store（Pinia）
 * --------------------------------------------------
 * 状态结构：
 *  - user：当前登录用户信息（包含 nickname / gender / avatar /
 *          profileCompleted / partner / inviteCode 等字段）。
 *          未登录或刷新前为 null。
 *
 * 持久化策略：
 *  - 本 Store 自身不做持久化；登录态由 localStorage 中的 token 维持
 *  - 应用启动后（如 MainLayout onMounted）调用 fetchUser() 通过
 *    /api/user/me 拉取最新用户信息并写入内存
 *
 * 主要方法：
 *  - fetchUser(): 从后端拉取当前登录用户，刷新 store.user
 *  - logout():    清除本地 token 与 user 状态（由调用方负责跳转到登录页）
 */
import { defineStore } from 'pinia'
import { getCurrentUser } from '../api/user'
import { usePetStore } from './petStore'

export const useUserStore = defineStore('user', {
  // 状态：当前用户对象，未登录时为 null
  state: () => ({
    user: null
  }),
  actions: {
    /**
     * 拉取并刷新当前登录用户信息
     * @returns {Promise<Object>} 后端返回的用户对象
     */
    async fetchUser() {
      this.user = await getCurrentUser()
      return this.user
    },
    /**
     * 退出登录：清除 token + 用户状态 + 关联子 store（如宠物缓存）
     * 注意：路由跳转由调用方完成（如 MainLayout 中 push('/login')）
     */
    logout() {
      localStorage.removeItem('token')
      this.user = null
      // 清空宠物缓存，避免另一账号在同浏览器登录时短暂闪现旧宠物
      try { usePetStore().clear() } catch (_) { /* ignore */ }
    }
  }
})
