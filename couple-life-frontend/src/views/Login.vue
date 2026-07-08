<template>
  <div class="auth-page">
    <!-- 装饰浮动元素 -->
    <div class="deco deco-1">💕</div>
    <div class="deco deco-2">🌸</div>
    <div class="deco deco-3">✨</div>
    <div class="deco deco-4">💗</div>

    <div class="auth-card love-card">
      <div class="auth-header">
        <router-link to="/" class="back-link">← 返回首页</router-link>
        <div class="brand-icon">💕</div>
        <h1 class="gradient-title">欢迎回来</h1>
        <p class="auth-sub">用今天的心情，继续收藏彼此的温柔</p>
      </div>

      <el-tabs v-model="loginType" class="login-tabs" @tab-change="onTabChange">
        <el-tab-pane label="账号密码登录" name="PASSWORD" />
        <el-tab-pane label="手机验证码登录" name="SMS_CODE" />
      </el-tabs>

      <!-- 账号密码登录 -->
      <el-form
        v-if="loginType === 'PASSWORD'"
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-position="top"
      >
        <el-form-item label="手机号 / 邮箱" prop="account">
          <el-input v-model="pwdForm.account" size="large" placeholder="请输入手机号或邮箱" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="pwdForm.password" size="large" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="图形验证码" prop="captcha">
          <div class="captcha-row">
            <el-input v-model="pwdForm.captcha" size="large" placeholder="请输入图中字符" />
            <img
              v-if="imageCaptcha.captchaImage"
              :src="imageCaptcha.captchaImage"
              class="captcha-img"
              title="点击刷新"
              @click="loadImageCaptcha"
            />
            <el-button v-else size="large" @click="loadImageCaptcha">获取验证码</el-button>
          </div>
        </el-form-item>
        <el-button type="primary" size="large" class="full" @click="submitPassword">登录</el-button>
      </el-form>

      <!-- 手机短信登录 -->
      <el-form
        v-else
        ref="smsFormRef"
        :model="smsForm"
        :rules="smsRules"
        label-position="top"
      >
        <el-form-item label="手机号" prop="account">
          <el-input v-model="smsForm.account" size="large" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="短信验证码" prop="captcha">
          <div class="captcha-row">
            <el-input v-model="smsForm.captcha" size="large" placeholder="请输入短信验证码" />
            <el-button size="large" :disabled="smsCountdown > 0" @click="getSmsCaptcha">
              {{ smsCountdown > 0 ? `${smsCountdown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-button type="primary" size="large" class="full" @click="submitSms">登录</el-button>
      </el-form>

      <div class="switch">还没有账号？<router-link to="/register">立即注册</router-link></div>
    </div>
    <ProfileSetupDialog v-model="showProfileDialog" @completed="onProfileCompleted" />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, sendCaptcha, getImageCaptcha } from '../api/auth'
import { useUserStore } from '../stores/userStore'
import ProfileSetupDialog from '../components/ProfileSetupDialog.vue'
import { isPhone, phoneValidator, phoneOrEmailValidator } from '../utils/validators'

const router = useRouter()
const userStore = useUserStore()
const loginType = ref('PASSWORD')
const showProfileDialog = ref(false)

const pwdFormRef = ref()
const pwdForm = reactive({ account: '', password: '', captcha: '' })
const imageCaptcha = reactive({ captchaKey: '', captchaImage: '' })
const pwdRules = {
  account: [{ required: true, validator: phoneOrEmailValidator, trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }]
}

async function loadImageCaptcha() {
  const res = await getImageCaptcha()
  imageCaptcha.captchaKey = res.captchaKey
  imageCaptcha.captchaImage = res.captchaImage
  pwdForm.captcha = ''
}

async function submitPassword() {
  await pwdFormRef.value.validate()
  if (!imageCaptcha.captchaKey) {
    ElMessage.warning('请先获取图形验证码')
    return
  }
  try {
    const res = await login({
      loginType: 'PASSWORD',
      account: pwdForm.account,
      password: pwdForm.password,
      captcha: pwdForm.captcha,
      captchaKey: imageCaptcha.captchaKey
    })
    handleLoginSuccess(res)
  } catch (e) {
    loadImageCaptcha()
  }
}

const smsFormRef = ref()
const smsForm = reactive({ account: '', captcha: '' })
const smsRules = {
  account: [{ required: true, validator: phoneValidator, trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入短信验证码', trigger: 'blur' }]
}
const smsCountdown = ref(0)
let smsTimer = null

async function getSmsCaptcha() {
  if (!isPhone(smsForm.account)) {
    return ElMessage.warning('请输入正确的手机号')
  }
  const res = await sendCaptcha({ account: smsForm.account, type: 'LOGIN' })
  ElMessage.success(res?.captcha ? `开发环境验证码：${res.captcha}` : '验证码已发送，请注意查收短信')
  smsCountdown.value = 60
  smsTimer = setInterval(() => {
    smsCountdown.value -= 1
    if (smsCountdown.value <= 0) {
      clearInterval(smsTimer)
      smsTimer = null
    }
  }, 1000)
}

async function submitSms() {
  await smsFormRef.value.validate()
  try {
    const res = await login({
      loginType: 'SMS_CODE',
      account: smsForm.account,
      captcha: smsForm.captcha
    })
    handleLoginSuccess(res)
  } catch (e) {
    smsForm.captcha = ''
    if (smsTimer) {
      clearInterval(smsTimer)
      smsTimer = null
    }
    smsCountdown.value = 0
  }
}

function handleLoginSuccess(res) {
  localStorage.setItem('token', res.token)
  userStore.user = res.user
  ElMessage.success('登录成功')
  if (res.user && res.user.profileCompleted === 0) {
    showProfileDialog.value = true
    return
  }
  router.push('/app/home')
}

function onProfileCompleted() {
  userStore.fetchUser().catch(() => {}).finally(() => router.push('/app/home'))
}

function onTabChange(name) {
  if (name === 'PASSWORD' && !imageCaptcha.captchaKey) {
    loadImageCaptcha()
  }
}

onMounted(() => {
  loadImageCaptcha()
})

onBeforeUnmount(() => {
  if (smsTimer) clearInterval(smsTimer)
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  position: relative;
  overflow: hidden;
}

/* 装饰浮动元素 */
.deco {
  position: fixed;
  font-size: 48px;
  opacity: 0.15;
  pointer-events: none;
  animation: float 6s ease-in-out infinite;
}
.deco-1 { top: 10%; left: 8%; animation-delay: 0s; }
.deco-2 { top: 20%; right: 10%; animation-delay: 1.5s; font-size: 36px; }
.deco-3 { bottom: 15%; left: 12%; animation-delay: 3s; font-size: 32px; }
.deco-4 { bottom: 25%; right: 8%; animation-delay: 4.5s; }

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(10deg); }
}

.auth-card {
  width: min(100%, 460px);
  padding: 40px;
  position: relative;
  z-index: 1;
}

.auth-header {
  text-align: center;
  margin-bottom: 24px;
}

.back-link {
  display: inline-block;
  font-size: 13px;
  color: var(--love-text-light);
  margin-bottom: 16px;
  transition: color 0.2s;
}
.back-link:hover {
  color: var(--love-primary);
}

.brand-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

h1 {
  margin: 0 0 8px;
  font-size: 32px;
}

.auth-sub {
  color: var(--love-text-light);
  margin: 0;
  font-size: 15px;
}

.login-tabs {
  margin-bottom: 8px;
}

.captcha-row {
  display: flex;
  width: 100%;
  gap: 10px;
  align-items: center;
}

.captcha-img {
  height: 40px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid var(--love-pink);
  background: #fff;
  transition: transform 0.2s;
}
.captcha-img:hover {
  transform: scale(1.05);
}

.full {
  width: 100%;
  height: 48px;
  font-size: 16px;
}

.switch {
  text-align: center;
  margin-top: 20px;
  color: var(--love-text-light);
}
.switch a {
  color: var(--love-primary);
  font-weight: 700;
}

@media (max-width: 640px) {
  .auth-page {
    padding: 16px;
    overflow-x: hidden;
  }

  .auth-card {
    padding: 28px 20px;
  }

  h1 {
    font-size: 28px;
  }

  .captcha-row {
    flex-wrap: wrap;
  }

  .captcha-row .el-button,
  .captcha-img {
    width: 100%;
  }
}
</style>
