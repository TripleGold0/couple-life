/**
 * 宠物状态管理 Store（Pinia）
 * --------------------------------------------------
 * 职责：
 *  - 缓存当前情侣的宠物详情，供悬浮挂件 / 详情页 / 主页快捷读取
 *  - 提供 fetchPet() 拉取最新数据；互动接口执行后由调用方直接覆写 store.pet
 *  - 未绑定情侣 / 未选择宠物时 pet 为 null
 */
import { defineStore } from 'pinia'
import { getCurrentPet } from '../api/pet'

export const usePetStore = defineStore('pet', {
  state: () => ({
    /** 当前宠物 VO，未存在时为 null */
    pet: null
  }),
  actions: {
    /** 拉取并刷新当前宠物 */
    async fetchPet() {
      try {
        this.pet = await getCurrentPet()
      } catch (_) {
        this.pet = null
      }
      return this.pet
    },
    /** 清空（如解除情侣绑定 / 退出登录时） */
    clear() {
      this.pet = null
    }
  }
})
