<template>
  <div class="profile love-card">
    <div class="head">
      <h2 class="gradient-title">个人信息</h2>
      <el-button type="primary" plain @click="editing = !editing">
        <el-icon v-if="!editing"><EditPen /></el-icon>
        {{ editing ? '取消' : '编辑资料' }}
      </el-button>
    </div>
    <div class="info">
      <div class="avatar-box">
        <el-avatar :size="96" :src="form.avatar || user?.avatar || defaultAvatarFor(editing ? form.gender : user?.gender)">{{ user?.nickname?.[0] }}</el-avatar>
        <el-upload v-if="editing" :show-file-list="false" :before-upload="handleAvatarUpload" accept="image/*">
          <el-button size="small">
            <el-icon><Camera /></el-icon>
            更换头像
          </el-button>
        </el-upload>
        <div v-if="editing && !form.avatar" class="hint">未上传时将使用默认头像</div>
      </div>
      <div class="meta">
        <template v-if="!editing">
          <h3>{{ user?.nickname }}</h3>
          <div class="info-grid">
            <div class="info-item"><span class="info-label">用户名</span><span>{{ user?.username }}</span></div>
            <div class="info-item"><span class="info-label">手机号</span><span>{{ user?.phone || '未绑定' }}</span></div>
            <div class="info-item"><span class="info-label">邮箱</span><span>{{ user?.email || '未绑定' }}</span></div>
          </div>
        </template>
        <template v-else>
          <el-form :model="form" label-width="80px">
            <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
                <el-radio :value="0">保密</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="生日"><el-date-picker v-model="form.birthday" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
            <el-button type="primary" size="large" class="full" @click="save">
              <el-icon><Check /></el-icon>
              保存修改
            </el-button>
          </el-form>
        </template>
      </div>
    </div>

    <el-divider />
    <h3 class="section-heading"><el-icon><Star /></el-icon>电子宠物</h3>
    <div class="pet-toggle-row">
      <div>
        <p class="toggle-title">显示悬浮电子宠物</p>
        <p class="toggle-hint">仅影响你这一端是否在网页角落渲染挂件，不影响双方共养的数据。</p>
      </div>
      <el-switch v-model="petDisplayEnabled" :active-value="1" :inactive-value="0" @change="onTogglePetDisplay" />
    </div>

    <el-divider />
    <h3 class="section-heading"><el-icon><Link /></el-icon>情侣关系</h3>
    <div v-if="user?.partner" class="partner-card">
      <div class="partner">
        <el-avatar :src="avatarOf(user.partner)">{{ user.partner.nickname?.[0] }}</el-avatar>
        <div>
          <b>{{ user.partner.nickname }}</b>
          <p v-if="user.loveStartDate">恋爱开始日期：{{ user.loveStartDate }}</p>
        </div>
      </div>
      <el-button type="danger" plain @click="onUnbind">解除绑定</el-button>
    </div>
    <div v-else class="bind-card">
      <div class="invite">
        <p>把你的邀请码发给另一半：</p>
        <div class="code">{{ user?.inviteCode || '生成中...' }}</div>
      </div>
      <div class="bind-form">
        <p>或输入对方的邀请码进行绑定：</p>
        <el-input v-model="bindForm.inviteCode" placeholder="输入对方邀请码" style="margin-bottom: 10px" />
        <el-date-picker v-model="bindForm.loveStartDate" value-format="YYYY-MM-DD" placeholder="恋爱开始日期" style="margin-bottom: 10px; width: 100%" />
        <el-button type="primary" size="large" class="full" @click="onBind">
          <el-icon><Connection /></el-icon>
          绑定情侣
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Camera, Check, Connection, EditPen, Link, Star } from '@element-plus/icons-vue'
import { bindCouple, unbindCouple, updateProfile, uploadImage } from '../api/user'
import { updatePetDisplay } from '../api/pet'
import { useUserStore } from '../stores/userStore'
import { avatarOf, defaultAvatarFor } from '../utils/defaultAvatar'

const userStore = useUserStore()
const user = computed(() => userStore.user)
const editing = ref(false)
const form = reactive({ nickname: '', gender: 0, birthday: null, avatar: '' })
const bindForm = reactive({ inviteCode: '', loveStartDate: '' })
const petDisplayEnabled = ref(1)

watch(user, value => {
  if (!value) return
  form.nickname = value.nickname
  form.gender = value.gender
  form.avatar = value.avatar
  petDisplayEnabled.value = value.petDisplayEnabled === 0 ? 0 : 1
}, { immediate: true })

async function onTogglePetDisplay(val) {
  try {
    await updatePetDisplay(val === 1)
    if (userStore.user) userStore.user.petDisplayEnabled = val
    ElMessage.success(val === 1 ? '已开启悬浮宠物' : '已关闭悬浮宠物')
  } catch (_) { petDisplayEnabled.value = val === 1 ? 0 : 1 }
}

async function handleAvatarUpload(file) {
  const res = await uploadImage(file, 'avatar')
  form.avatar = res.url
  ElMessage.success('头像已上传，记得点击保存修改')
  return false
}

async function save() {
  await updateProfile(form)
  ElMessage.success('资料已更新')
  editing.value = false
  await userStore.fetchUser()
}

async function onBind() {
  await bindCouple(bindForm)
  ElMessage.success('绑定成功')
  await userStore.fetchUser()
}

async function onUnbind() {
  try { await ElMessageBox.confirm('确定要解除情侣绑定吗？', '提示', { type: 'warning' }) } catch { return }
  await unbindCouple()
  ElMessage.success('已解除绑定')
  await userStore.fetchUser()
}
</script>

<style scoped>
.profile {
  padding: 32px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.head h2 {
  margin: 0;
  font-size: 28px;
}

.info {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}

.avatar-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.hint {
  font-size: 12px;
  color: var(--love-text-muted);
}

.meta {
  flex: 1;
}

.meta h3 {
  font-size: 22px;
  margin: 0 0 16px;
}

.info-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 247, 251, 0.8);
  border-radius: var(--love-radius-xs);
}

.info-label {
  color: var(--love-text-light);
  font-size: 13px;
  min-width: 56px;
}

.full {
  width: 100%;
}

p {
  color: #8e6d7c;
}

.partner-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 247, 251, 0.8);
  padding: 20px;
  border-radius: var(--love-radius-sm);
  transition: background 0.2s;
}

.partner-card:hover {
  background: rgba(255, 214, 231, 0.25);
}

.partner {
  display: flex;
  gap: 14px;
  align-items: center;
}

.bind-card {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.invite, .bind-form {
  background: rgba(255, 247, 251, 0.8);
  padding: 24px;
  border-radius: var(--love-radius-sm);
  transition: background 0.2s;
}

.invite:hover, .bind-form:hover {
  background: rgba(255, 214, 231, 0.25);
}

.code {
  font-size: 36px;
  font-weight: 800;
  color: var(--love-primary);
  letter-spacing: 4px;
  margin-top: 8px;
}

.pet-toggle-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 247, 251, 0.8);
  padding: 20px;
  border-radius: var(--love-radius-sm);
}

.toggle-title {
  font-weight: 600;
  color: var(--love-text);
  margin: 0;
}

.toggle-hint {
  color: var(--love-text-light);
  font-size: 12px;
  margin: 4px 0 0;
}

h3 {
  font-size: 18px;
  margin: 0 0 14px;
}

.section-heading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.section-heading .el-icon,
.head .el-icon,
.full .el-icon {
  color: var(--love-primary);
}

@media (max-width: 640px) {
  .info {
    flex-direction: column;
    align-items: center;
  }
  .bind-card {
    grid-template-columns: 1fr;
  }
}
</style>
