/**
 * 电子宠物（Pet）API 模块
 * --------------------------------------------------
 * 围绕「情侣共养电子宠物」功能，覆盖：
 *  - 当前宠物详情、可选种类列表
 *  - 选择请求（共同同意机制）：发起、查看、同意、拒绝
 *  - 互动行为：喂食 / 抚摸 / 玩耍
 *  - 个人侧悬浮宠物显示开关（属于 user 模块，但和宠物强相关，统一放此处便于查找）
 */
import request from '../utils/request'

/** 当前情侣的宠物详情；未绑定情侣或未选择宠物时返回 null */
export const getCurrentPet = () => request.get('/api/pet/current')

/** 平台上架的可选宠物种类列表 */
export const getPetTypes = () => request.get('/api/pet/types')

/** 与我相关的选择请求（我发起的 + 需我处理的） */
export const getPetSelectionRequests = () => request.get('/api/pet/selection/list')

/**
 * 发起一次宠物选择 / 更换请求
 * @param {{petTypeId: number, nickname?: string}} data
 */
export const createPetSelectionRequest = data => request.post('/api/pet/selection/request', data)

/** 同意一个选择请求，成功后会创建/替换宠物 */
export const agreePetSelectionRequest = id => request.post(`/api/pet/selection/${id}/agree`)

/** 拒绝一个选择请求 */
export const rejectPetSelectionRequest = id => request.post(`/api/pet/selection/${id}/reject`)

/**
 * 与宠物互动
 * @param {('FEED'|'PET'|'PLAY')} action 互动类型
 */
export const interactWithPet = action => request.post('/api/pet/interact', { action })

/**
 * 切换"悬浮宠物在我侧是否显示"
 * @param {boolean} enabled
 */
export const updatePetDisplay = enabled => request.put('/api/user/pet-display', { enabled })
