<template>
  <!--
    FloatingPet —— 悬浮电子宠物挂件
    ------------------------------------------------------------
    渲染条件（任一不满足则不渲染）：
      1) 已登录
      2) 用户偏好 pet_display_enabled === 1
      3) 已绑定情侣 且 已选定宠物（pet 非空）
    交互：
      - 长按（>200ms）后拖拽，松手落位；位置持久化到 localStorage
      - 单击（未触发拖拽）打开互动 popover：喂食 / 抚摸 / 玩耍
      - popover 中亦展示亲密度、陪伴天数、饱食度、心情、等级等核心属性
      - 互动后通过 store 触发整体刷新；移动端 touch 事件兼容
  -->
  <div
    v-if="visible"
    ref="petEl"
    class="floating-pet"
    :class="{ dragging }"
    :style="petStyle"
    @mousedown="onPressStart"
    @touchstart.prevent="onPressStart"
  >
    <div class="pet-avatar" :title="petTooltip">
      <!-- MVP 阶段使用文字 emoji 兜底；如后端配置了 sprite/avatar 资源则优先用图片 -->
      <img v-if="petImage" :src="petImage" :alt="pet?.petTypeName" draggable="false" />
      <span v-else class="pet-emoji">{{ emojiOf(pet?.petTypeCode) }}</span>
    </div>
    <transition name="pop">
      <div v-if="showPanel" class="pet-panel" @mousedown.stop @click.stop>
        <div class="panel-header">
          <strong>{{ pet?.nickname || pet?.petTypeName || '我们的宠物' }}</strong>
          <span class="lv">Lv.{{ pet?.level || 1 }} · {{ stageLabel(pet?.stage) }}</span>
        </div>
        <div class="panel-stats">
          <div>亲密度 <b>{{ pet?.intimacy || 0 }}</b></div>
          <div>陪伴 <b>{{ pet?.companionDays || 0 }}</b> 天</div>
          <div>饱食 <b>{{ pet?.fullness || 0 }}</b> / 100</div>
          <div>心情 <b>{{ pet?.mood || 0 }}</b> / 100</div>
        </div>
        <div class="panel-actions">
          <el-button size="small" type="warning" :loading="loadingAction === 'FEED'" @click="onInteract('FEED')">喂食</el-button>
          <el-button size="small" type="success" :loading="loadingAction === 'PET'" @click="onInteract('PET')">抚摸</el-button>
          <el-button size="small" type="primary" :loading="loadingAction === 'PLAY'" @click="onInteract('PLAY')">玩耍</el-button>
        </div>
        <div class="panel-footer">
          <el-link type="primary" @click="goPet">查看详情 / 更换</el-link>
          <el-link @click="showPanel = false">关闭</el-link>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
/**
 * FloatingPet 组件脚本
 * - 通过 useUserStore 拿到当前用户的 pet_display_enabled
 * - 通过 usePetStore 拿到当前情侣的宠物状态（首次挂载时拉取一次，互动后局部刷新）
 * - 拖拽：mousedown/touchstart 计时 200ms 触发拖拽态；mousemove/touchmove 更新位置；
 *   mouseup/touchend 落位；落位坐标写入 localStorage
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore'
import { usePetStore } from '../stores/petStore'
import { interactWithPet } from '../api/pet'

const router = useRouter()
const userStore = useUserStore()
const petStore = usePetStore()

// 持久化位置的本地存储 key（按用户区分，避免共用浏览器时坐标错位）
const STORAGE_KEY = 'floating_pet_position'
// 元素引用，便于读取宽高做边界限制
const petEl = ref(null)

// 当前位置（相对 viewport 左上角）
const pos = ref(loadPosition())
// 是否处于拖拽态（超过长按阈值）
const dragging = ref(false)
// 是否展示互动面板
const showPanel = ref(false)
// 当前正在执行的互动类型，用于 loading 态
const loadingAction = ref('')

// === 渲染条件 ===
const pet = computed(() => petStore.pet)
const visible = computed(() => {
  if (!localStorage.getItem('token')) return false
  if (!userStore.user) return false
  if (userStore.user.petDisplayEnabled === 0) return false
  return !!pet.value
})

const petStyle = computed(() => ({ left: pos.value.x + 'px', top: pos.value.y + 'px' }))

const petImage = computed(() => pet.value?.spriteUrl || pet.value?.typeAvatar || '')
const petTooltip = computed(() => {
  const p = pet.value
  if (!p) return ''
  return `${p.nickname || p.petTypeName} · Lv.${p.level || 1}\n亲密度 ${p.intimacy || 0} | 陪伴 ${p.companionDays || 0} 天`
})

/** 不同种类的兜底 emoji；后端配 spriteUrl 后可不用 */
function emojiOf(code) {
  switch (code) {
    case 'CAT': return '🐱'
    case 'DOG': return '🐶'
    case 'RABBIT': return '🐰'
    case 'DRAGON': return '🐉'
    case 'SLIME': return '🟢'
    default: return '🐾'
  }
}

function stageLabel(stage) {
  return ({ BABY: '幼年', TEEN: '成长', ADULT: '成年' })[stage] || '幼年'
}

// ============== 拖拽逻辑 ==============
let pressTimer = null
let pressPoint = null
let elementOrigin = null
const LONG_PRESS_MS = 200

function onPressStart(e) {
  // 读出按下时的指针位置（鼠标 / 触摸统一处理）
  const point = pointOf(e)
  pressPoint = point
  elementOrigin = { x: pos.value.x, y: pos.value.y }
  pressTimer = setTimeout(() => {
    dragging.value = true
  }, LONG_PRESS_MS)
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onPressEnd)
  window.addEventListener('touchmove', onMove, { passive: false })
  window.addEventListener('touchend', onPressEnd)
}

function onMove(e) {
  if (!pressPoint) return
  const point = pointOf(e)
  const dx = point.x - pressPoint.x
  const dy = point.y - pressPoint.y
  // 在长按计时器还未触发前如果已经移动超过阈值，也直接进入拖拽态（兼容快速拖动）
  if (!dragging.value && Math.abs(dx) + Math.abs(dy) > 8) {
    clearTimeout(pressTimer)
    dragging.value = true
  }
  if (!dragging.value) return
  if (e.cancelable) e.preventDefault()
  const next = clampToViewport(elementOrigin.x + dx, elementOrigin.y + dy)
  pos.value = next
}

function onPressEnd(e) {
  clearTimeout(pressTimer)
  window.removeEventListener('mousemove', onMove)
  window.removeEventListener('mouseup', onPressEnd)
  window.removeEventListener('touchmove', onMove)
  window.removeEventListener('touchend', onPressEnd)
  if (dragging.value) {
    // 拖拽落位：保存位置，且不触发"单击"打开面板的逻辑
    savePosition(pos.value)
    setTimeout(() => { dragging.value = false }, 0)
  } else {
    // 视为单击：切换面板显示
    showPanel.value = !showPanel.value
  }
  pressPoint = null
  elementOrigin = null
}

/** 兼容鼠标 / 触摸的指针坐标提取 */
function pointOf(e) {
  if (e.touches && e.touches[0]) return { x: e.touches[0].clientX, y: e.touches[0].clientY }
  if (e.changedTouches && e.changedTouches[0]) return { x: e.changedTouches[0].clientX, y: e.changedTouches[0].clientY }
  return { x: e.clientX, y: e.clientY }
}

/** 限制在视口可见范围（留出 16px 边距） */
function clampToViewport(x, y) {
  const w = petEl.value?.offsetWidth || 80
  const h = petEl.value?.offsetHeight || 80
  const maxX = window.innerWidth - w - 16
  const maxY = window.innerHeight - h - 16
  return {
    x: Math.max(16, Math.min(maxX, x)),
    y: Math.max(16, Math.min(maxY, y))
  }
}

function loadPosition() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const obj = JSON.parse(raw)
      if (typeof obj.x === 'number' && typeof obj.y === 'number') return obj
    }
  } catch (_) { /* ignore */ }
  // 默认放在右下角
  return { x: window.innerWidth - 120, y: window.innerHeight - 160 }
}

function savePosition(p) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(p))
  } catch (_) { /* ignore */ }
}

// 浏览器尺寸变化时确保挂件仍在视口内
function onResize() {
  pos.value = clampToViewport(pos.value.x, pos.value.y)
}

/** 点击挂件 / 面板之外的区域时收起面板 */
function onDocumentClick(e) {
  if (!showPanel.value) return
  if (petEl.value && !petEl.value.contains(e.target)) {
    showPanel.value = false
  }
}

onMounted(() => {
  // 仅在已登录时主动拉取宠物，避免登录页报 401
  if (localStorage.getItem('token')) {
    petStore.fetchPet().catch(() => {})
  }
  window.addEventListener('resize', onResize)
  document.addEventListener('mousedown', onDocumentClick, true)
  document.addEventListener('touchstart', onDocumentClick, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  document.removeEventListener('mousedown', onDocumentClick, true)
  document.removeEventListener('touchstart', onDocumentClick, true)
})

// ============== 互动逻辑 ==============
async function onInteract(action) {
  if (loadingAction.value) return
  loadingAction.value = action
  try {
    const updated = await interactWithPet(action)
    petStore.pet = updated
    ElMessage.success(actionMessage(action))
  } catch (_) {
    // 错误已在 axios 拦截器统一 toast
  } finally {
    loadingAction.value = ''
  }
}

function actionMessage(action) {
  return ({ FEED: '吃得真香～', PET: '小家伙开心地蹭了蹭你', PLAY: '玩得不亦乐乎！' })[action] || '互动成功'
}

function goPet() {
  showPanel.value = false
  router.push('/app/pet')
}
</script>

<style scoped>
.floating-pet {
  position: fixed;
  z-index: 9999;
  width: 80px;
  height: 80px;
  cursor: grab;
  user-select: none;
  touch-action: none;
}
.floating-pet.dragging { cursor: grabbing; }
.pet-avatar {
  width: 80px; height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd6e7, #fff1cc);
  box-shadow: 0 6px 16px rgba(255, 111, 159, 0.25);
  display: flex; align-items: center; justify-content: center;
  font-size: 44px;
  border: 3px solid #fff;
}
.pet-avatar img { width: 70%; height: 70%; object-fit: contain; pointer-events: none; }
.pet-emoji { line-height: 1; }
.pet-panel {
  position: absolute;
  right: 90px; top: 0;
  width: 240px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
  padding: 14px 16px;
  font-size: 13px;
}
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.panel-header .lv { color: #ff6f9f; font-size: 12px; }
.panel-stats { display: grid; grid-template-columns: 1fr 1fr; gap: 6px 12px; margin-bottom: 12px; color: #765567; }
.panel-stats b { color: #ff5f97; }
.panel-actions { display: flex; gap: 6px; margin-bottom: 8px; }
.panel-actions .el-button { flex: 1; padding: 6px 0; }
.panel-footer { display: flex; justify-content: space-between; }
.pop-enter-active, .pop-leave-active { transition: all 0.18s ease; }
.pop-enter-from, .pop-leave-to { opacity: 0; transform: translateY(-4px) scale(0.96); }

/* 移动端面板靠左展开避免溢出 */
@media (max-width: 600px) {
  .pet-panel { right: auto; left: 90px; }
}
</style>
