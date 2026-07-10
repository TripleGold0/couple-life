<template>
  <!-- 强制完善资料弹窗：未填完不可关闭 -->
  <el-dialog
    :model-value="modelValue"
    title="完善个人信息"
    width="440px"
    align-center
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
  >
    <div class="tip">为了更好的体验，请先完善以下信息（必填）。</div>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" maxlength="20" placeholder="给自己起一个昵称吧" />
      </el-form-item>
      <el-form-item label="性别" prop="gender">
        <el-radio-group v-model="form.gender">
          <el-radio :value="1">男</el-radio>
          <el-radio :value="2">女</el-radio>
          <el-radio :value="0">保密</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="设置密码" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="loading" class="full" @click="submit">提交并继续</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * ProfileSetupDialog —— 强制完善资料对话框
 * ----------------------------------------------------------
 * 职责：
 *  - 当用户首次通过短信验证码登录、或资料未完善时弹出
 *  - 收集昵称 / 性别 / 密码 / 确认密码并调用 completeProfile 接口
 *  - 通过 v-model 与父组件双向绑定显隐；提交成功后 emit 'completed'
 *  - 未完成填写时禁止 ESC / 点击遮罩关闭，强制用户填完
 */
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { completeProfile } from '../api/user'

// 父组件通过 v-model 控制显隐；同时支持 'completed' 事件
const props = defineProps({ modelValue: { type: Boolean, default: false } })
const emit = defineEmits(['update:modelValue', 'completed'])

const formRef = ref()
const loading = ref(false)
// 表单数据
const form = reactive({ nickname: '', gender: null, password: '', confirmPassword: '' })

// 自定义校验：两次密码必须一致
const samePassword = (_, value, cb) => value === form.password ? cb() : cb(new Error('两次输入的密码不一致'))
// 表单校验规则
const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: samePassword, trigger: 'blur' }
  ]
}

// 提交完善资料：校验 -> 调用接口 -> 关闭弹窗并通知父组件
async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await completeProfile({
      nickname: form.nickname,
      gender: form.gender,
      password: form.password
    })
    ElMessage.success('信息已保存')
    emit('update:modelValue', false)
    emit('completed')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tip { color: var(--color-text-secondary); margin-bottom: 12px; }
.full { width: 100%; }
</style>
