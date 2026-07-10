<template>
  <div class="auth-page">
    <ParticleBackground />
    <div class="deco deco-1">💌</div>
    <div class="deco deco-2">🌸</div>
    <div class="deco deco-3">✨</div>

    <div class="auth-card love-card">
      <div class="auth-header">
        <router-link to="/" class="back-link">← 返回首页</router-link>
        <div class="brand-icon">💕</div>
        <h1 class="gradient-title">创建甜蜜账号</h1>
        <p class="auth-sub">30 秒注册，开启你们的专属空间</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="14">
          <el-col :span="formColSpan"><el-form-item label="用户名" prop="username"><el-input v-model="form.username" placeholder="设置登录用户名" /></el-form-item></el-col>
          <el-col :span="formColSpan"><el-form-item label="昵称" prop="nickname"><el-input v-model="form.nickname" placeholder="对方看到的名字" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender" class="gender-group">
            <el-radio-button :value="1">男</el-radio-button>
            <el-radio-button :value="2">女</el-radio-button>
            <el-radio-button :value="0">保密</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="账号类型">
          <el-radio-group v-model="accountType" @change="onAccountTypeChange">
            <el-radio value="phone">手机号</el-radio>
            <el-radio value="email">邮箱</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="accountType === 'phone'" label="手机号" prop="phone"><el-input v-model="form.phone" maxlength="11" placeholder="请输入手机号" /></el-form-item>
        <el-form-item v-else label="邮箱" prop="email"><el-input v-model="form.email" placeholder="请输入邮箱" /></el-form-item>

        <el-row :gutter="14">
          <el-col :span="formColSpan"><el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" /></el-form-item></el-col>
          <el-col :span="formColSpan"><el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入密码" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="验证码" prop="captcha">
          <div class="captcha-row"><el-input v-model="form.captcha" placeholder="请输入验证码" /><el-button @click="getCaptcha">获取验证码</el-button></div>
        </el-form-item>
        <el-button type="primary" size="large" class="full" @click="submit">注册</el-button>
        <div class="switch">已有账号？<router-link to="/login">去登录</router-link></div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, sendCaptcha } from '../api/auth'
import { isPhone, isEmail, phoneValidator, emailValidator } from '../utils/validators'
import ParticleBackground from '../components/ParticleBackground.vue'

const router = useRouter()
const formRef = ref()
const accountType = ref('phone')
const formColSpan = ref(window.innerWidth <= 640 ? 24 : 12)
const form = reactive({ username: '', nickname: '', gender: 2, phone: '', email: '', password: '', confirmPassword: '', captcha: '' })
const samePassword = (rule, value, callback) => value === form.password ? callback() : callback(new Error('两次输入的密码不一致'))
const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  nickname: [{ required: true, message: '请输入昵称' }],
  gender: [{ required: true, message: '请选择性别' }],
  phone: [{ required: true, validator: phoneValidator, trigger: 'blur' }],
  email: [{ required: true, validator: emailValidator, trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码' }, { min: 6, message: '密码至少6位' }],
  confirmPassword: [{ required: true, message: '请确认密码' }, { validator: samePassword }],
  captcha: [{ required: true, message: '请输入验证码' }]
}

const captchaAccount = computed(() => accountType.value === 'phone' ? form.phone : form.email)

function onAccountTypeChange() {
  form.captcha = ''
  if (accountType.value === 'phone') form.email = ''
  else form.phone = ''
}

function updateFormColSpan() {
  formColSpan.value = window.innerWidth <= 640 ? 24 : 12
}

onMounted(() => { window.addEventListener('resize', updateFormColSpan) })
onBeforeUnmount(() => { window.removeEventListener('resize', updateFormColSpan) })

async function getCaptcha() {
  if (accountType.value === 'phone') {
    if (!isPhone(form.phone)) return ElMessage.warning('请先输入正确的手机号')
  } else {
    if (!isEmail(form.email)) return ElMessage.warning('请先输入正确的邮箱')
  }
  const res = await sendCaptcha({ account: captchaAccount.value, type: 'REGISTER' })
  const tip = accountType.value === 'phone' ? '验证码已发送至手机' : '验证码已发送至邮箱'
  ElMessage.success(res?.captcha ? `开发环境验证码：${res.captcha}` : tip)
}

async function submit() {
  await formRef.value.validate()
  const payload = {
    username: form.username,
    nickname: form.nickname,
    gender: form.gender,
    phone: accountType.value === 'phone' ? form.phone : null,
    email: accountType.value === 'email' ? form.email : null,
    password: form.password,
    confirmPassword: form.confirmPassword,
    captcha: form.captcha
  }
  await register(payload)
  ElMessage.success('注册成功，请登录')
  router.push('/login')
}
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

.deco {
  position: fixed;
  font-size: 48px;
  opacity: 0.12;
  pointer-events: none;
  animation: float 6s ease-in-out infinite;
}
.deco-1 { top: 8%; left: 10%; animation-delay: 0s; }
.deco-2 { top: 25%; right: 8%; animation-delay: 2s; font-size: 36px; }
.deco-3 { bottom: 20%; left: 15%; animation-delay: 4s; font-size: 32px; }

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(10deg); }
}

.auth-card {
  width: min(100%, 580px);
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
  font-size: 30px;
}

.auth-sub {
  color: var(--love-text-light);
  margin: 0;
  font-size: 15px;
}

.gender-group .el-radio-button {
  margin-right: 4px;
}

.captcha-row {
  display: flex;
  width: 100%;
  gap: 10px;
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
    font-size: 27px;
  }

  .gender-group {
    display: grid;
    grid-template-columns: 1fr;
    gap: 6px;
    width: 100%;
  }

  .gender-group .el-radio-button {
    margin-right: 0;
  }

  .captcha-row {
    flex-wrap: wrap;
  }

  .captcha-row .el-button {
    width: 100%;
  }
}
</style>
