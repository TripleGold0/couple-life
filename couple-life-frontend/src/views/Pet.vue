<template>
  <div class="pet-page">
    <div class="love-card detail-card">
      <h2 class="gradient-title">我们的电子宠物</h2>
      <template v-if="pet">
        <div class="detail-main">
          <div class="avatar">
            <span class="emoji">{{ emojiOf(pet.petTypeCode) }}</span>
          </div>
          <div class="info">
            <div class="title">
              <strong>{{ pet.nickname || pet.petTypeName }}</strong>
              <span class="tag">Lv.{{ pet.level }} · {{ stageLabel(pet.stage) }}</span>
            </div>
            <p class="muted">陪伴 {{ pet.companionDays }} 天 · 自 {{ pet.boundDate }} 起</p>
            <div class="bars">
              <div class="bar-item">
          <span>亲密度</span>
        <el-progress :percentage="intimacyPercent" :stroke-width="10" color="#c93665" />
                <span class="num">{{ pet.intimacy }}</span>
              </div>
              <div class="bar-item">
          <span>饱食度</span>
                <el-progress :percentage="pet.fullness" :stroke-width="10" color="#ffb84a" />
              </div>
              <div class="bar-item">
          <span>心情值</span>
                <el-progress :percentage="pet.mood" :stroke-width="10" color="#67c23a" />
              </div>
            </div>
            <div class="actions">
          <el-button type="warning" @click="onInteract('FEED')" :loading="loadingAction === 'FEED'">喂食</el-button>
              <el-button type="success" @click="onInteract('PET')" :loading="loadingAction === 'PET'">🤚 抚摸</el-button>
          <el-button type="primary" @click="onInteract('PLAY')" :loading="loadingAction === 'PLAY'">玩耍</el-button>
            </div>
          </div>
        </div>
      </template>
      <template v-else>
        <p class="muted no-pet-tip">{{ noPetTip }}</p>
      </template>
    </div>

    <div v-if="myRequests.length" class="love-card selection-card">
      <h3>选择请求</h3>
      <div v-for="r in myRequests" :key="r.id" class="req">
        <div class="req-info">
          <span class="emoji small">{{ emojiOf(r.petTypeCode) }}</span>
          <div>
            <div>
              <b>{{ r.requesterId === userId ? '我' : (r.requesterNickname || '伴侣') }}</b>
              发起 → {{ r.petTypeName }}
              <span class="status" :class="r.status">{{ statusLabel(r.status) }}</span>
            </div>
            <div class="muted small">
              {{ r.nickname ? `昵称：${r.nickname} · ` : '' }}过期：{{ formatTime(r.expireTime) }}
            </div>
          </div>
        </div>
        <div v-if="r.status === 'PENDING' && r.partnerId === userId" class="req-actions">
          <el-button size="small" type="primary" @click="onAgree(r.id)">同意</el-button>
          <el-button size="small" @click="onReject(r.id)">拒绝</el-button>
        </div>
      </div>
    </div>

    <div class="love-card types-card">
      <h3>{{ pet ? '想换一只？' : '选一只共养的宠物' }}</h3>
      <p class="muted">选择 / 更换宠物需对方在 24 小时内同意</p>
      <div class="type-grid">
        <div v-for="t in types" :key="t.id" class="type-item" :class="{ selected: form.petTypeId === t.id }" @click="form.petTypeId = t.id">
          <div class="emoji">{{ emojiOf(t.code) }}</div>
          <strong>{{ t.name }}</strong>
          <p class="muted small">{{ t.description }}</p>
        </div>
      </div>
      <div class="type-form">
        <el-input v-model="form.nickname" placeholder="给它起个昵称（可选）" maxlength="20" style="max-width: 240px" />
        <el-button type="primary" :disabled="!form.petTypeId" :loading="creating" @click="onCreateRequest">
          {{ pet ? '发起更换请求' : '发起选择请求' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { agreePetSelectionRequest, createPetSelectionRequest, getPetSelectionRequests, getPetTypes, interactWithPet, rejectPetSelectionRequest } from '../api/pet'
import { usePetStore } from '../stores/petStore'
import { useUserStore } from '../stores/userStore'

const petStore = usePetStore()
const userStore = useUserStore()
const userId = computed(() => userStore.user?.id)
const pet = computed(() => petStore.pet)

const types = ref([])
const myRequests = ref([])
const form = reactive({ petTypeId: null, nickname: '' })
const creating = ref(false)
const loadingAction = ref('')

const intimacyPercent = computed(() => { const v = pet.value?.intimacy || 0; return v % 100 })

const noPetTip = computed(() => {
  if (!userStore.user?.partner) return '请先在「个人信息」绑定情侣，再共同选择一只宠物吧～'
  return '还没有共同的宠物，从下方选择一只发起请求，对方同意后即可生效。'
})

function emojiOf(code) { return ({ CAT: '🐱', DOG: '🐶', RABBIT: '🐰', DRAGON: '🐉', SLIME: '🟢' })[code] || '🐾' }
function stageLabel(stage) { return ({ BABY: '幼年', TEEN: '成长', ADULT: '成年' })[stage] || '幼年' }
function statusLabel(status) { return ({ PENDING: '待对方同意', AGREED: '已同意', REJECTED: '已拒绝', EXPIRED: '已过期' })[status] || status }
function formatTime(s) { if (!s) return ''; return String(s).replace('T', ' ').slice(0, 16) }

async function refreshAll() {
  const [typeList, reqList] = await Promise.all([getPetTypes().catch(() => []), getPetSelectionRequests().catch(() => [])])
  types.value = typeList || []
  myRequests.value = reqList || []
  await petStore.fetchPet()
}

onMounted(() => { refreshAll() })

async function onCreateRequest() {
  if (!userStore.user?.partner) { ElMessage.warning('请先绑定情侣关系'); return }
  creating.value = true
  try {
    await createPetSelectionRequest({ petTypeId: form.petTypeId, nickname: form.nickname || null })
  ElMessage.success('已发送给对方，等待同意')
    form.nickname = ''
    await refreshAll()
  } finally { creating.value = false }
}

async function onAgree(id) { await agreePetSelectionRequest(id); ElMessage.success('已同意，宠物已生效'); await refreshAll() }

async function onReject(id) {
  try { await ElMessageBox.confirm('确定要拒绝这个请求吗？', '提示', { type: 'warning' }) } catch { return }
  await rejectPetSelectionRequest(id); ElMessage.success('已拒绝'); await refreshAll()
}

async function onInteract(action) {
  if (loadingAction.value) return
  loadingAction.value = action
  try { const updated = await interactWithPet(action); petStore.pet = updated; ElMessage.success('互动成功') }
  finally { loadingAction.value = '' }
}
</script>

<style scoped>
.pet-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.love-card {
  padding: 24px;
}

h2 {
  margin: 0 0 16px;
  font-size: 28px;
}

h3 {
  margin: 0 0 8px;
}

.detail-main {
  display: flex;
  gap: 28px;
  align-items: flex-start;
  margin-top: 16px;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd6e7, #fff1cc);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(255, 124, 168, 0.2);
}

.avatar .emoji {
  font-size: 64px;
  line-height: 1;
}

.info {
  flex: 1;
  min-width: 0;
}

.title strong {
  font-size: 22px;
}

.title .tag {
  margin-left: 10px;
  color: var(--love-primary);
  font-size: 13px;
  font-weight: 600;
}

.muted {
  color: var(--love-text-light);
}

.no-pet-tip {
  text-align: center;
  padding: 20px 0;
  font-size: 15px;
}

.small {
  font-size: 12px;
}

.bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 16px 0 20px;
}

.bar-item {
  display: grid;
  grid-template-columns: 80px 1fr 56px;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.bar-item .num {
  color: var(--love-primary);
  font-weight: 700;
}

.actions {
  display: flex;
  gap: 10px;
}

.selection-card .req {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px dashed rgba(255, 214, 231, 0.5);
}

.selection-card .req:last-child {
  border-bottom: none;
}

.req-info {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.req-info .emoji.small {
  font-size: 32px;
}

.req-actions {
  display: flex;
  gap: 8px;
}

.status {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 999px;
  margin-left: 6px;
  font-weight: 600;
}

.status.PENDING {
  background: #fff7e0;
  color: #b78400;
}

.status.AGREED {
  background: #e7f9e3;
  color: #3a8f1a;
}

.status.REJECTED {
  background: #fde2e2;
  color: #c1452a;
}

.status.EXPIRED {
  background: #ececec;
  color: #888;
}

.types-card .type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  margin: 16px 0;
}

.type-item {
  padding: 16px;
  border-radius: var(--love-radius-sm);
  background: rgba(255, 247, 251, 0.8);
  cursor: pointer;
  text-align: center;
  border: 2px solid transparent;
  transition: border-color 0.2s, background-color 0.2s, box-shadow 0.2s, transform 0.2s;
}

.type-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(255, 124, 168, 0.15);
}

.type-item.selected {
  border-color: var(--love-primary);
  background: rgba(255, 214, 231, 0.4);
  box-shadow: 0 8px 20px rgba(255, 124, 168, 0.2);
}

.type-item .emoji {
  font-size: 40px;
  line-height: 1;
  margin-bottom: 8px;
}

.type-form {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
