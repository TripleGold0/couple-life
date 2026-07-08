<template>
  <div class="love-card counselor-page">
    <div class="page-head">
      <div>
        <h2 class="gradient-title">情感陪伴顾问</h2>
        <p>专业、中立、温柔的情侣情感调节，帮你化解矛盾、增进理解。</p>
      </div>
      <div class="head-actions">
        <el-button @click="settingsVisible = true" :disabled="loading">
          <el-icon><Setting /></el-icon>
          模型设置
        </el-button>
        <el-button @click="resetChat" :disabled="loading">
          <el-icon><Refresh /></el-icon>
          新对话
        </el-button>
      </div>
    </div>

    <el-dialog v-model="settingsVisible" title="⚙️ 自定义模型设置" width="520px">
      <el-alert type="info" :closable="false" show-icon title="默认免费：Pollinations.ai（无需 API Key）" description="如需更高质量，可填入任意 OpenAI 兼容服务。配置仅保存在本浏览器，不会上传服务器。" />
      <el-form :model="settingsForm" label-width="90px" style="margin-top: 18px;">
        <el-form-item label="Base URL">
          <el-input v-model="settingsForm.baseUrl" placeholder="留空使用默认；如 https://api.openai.com/v1" />
          <div class="form-tip">填完整地址即可，系统会自动处理</div>
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="settingsForm.apiKey" type="password" show-password placeholder="免费默认服务可不填" />
        </el-form-item>
        <el-form-item label="模型">
          <el-input v-model="settingsForm.model" placeholder="如 gpt-4o-mini、deepseek-chat" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetSettings">恢复默认</el-button>
        <el-button @click="settingsVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存</el-button>
      </template>
    </el-dialog>

    <div class="chat-window" ref="chatWindow">
      <div v-for="(msg, idx) in messages" :key="idx" class="bubble-row" :class="msg.role">
        <el-avatar v-if="msg.role === 'assistant'" class="avatar assistant-avatar" :size="36">💗</el-avatar>
        <div class="bubble" :class="msg.role">
          <div class="content" v-text="msg.content"></div>
        </div>
        <el-avatar v-if="msg.role === 'user'" class="avatar user-avatar" :size="36">
          {{ userStore.user?.nickname?.[0] || '我' }}
        </el-avatar>
      </div>
      <div v-if="loading" class="bubble-row assistant">
        <el-avatar class="avatar assistant-avatar" :size="36">💗</el-avatar>
        <div class="bubble assistant"><div class="content typing">顾问正在思考…</div></div>
      </div>
    </div>

    <div class="composer">
      <el-input v-model="input" type="textarea" :rows="3" resize="none" :disabled="loading" placeholder="把你的感受、最近发生的事告诉我吧" @keydown.ctrl.enter.prevent="send" @keydown.meta.enter.prevent="send" />
      <div class="actions">
        <span class="hint">🔒 所有对话仅用于本次会话，不会被永久保存</span>
        <el-button type="primary" :loading="loading" @click="send" :disabled="!input.trim()">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Setting } from '@element-plus/icons-vue'
import { clearLlmConfig, counselorChat, getLlmConfig, saveLlmConfig } from '../api/counselor'
import { useUserStore } from '../stores/userStore'

const userStore = useUserStore()
const messages = ref([])
const input = ref('')
const loading = ref(false)
const chatWindow = ref(null)
const settingsVisible = ref(false)
const settingsForm = reactive({ baseUrl: '', apiKey: '', model: '' })

function loadSettingsForm() {
  const cfg = getLlmConfig()
  settingsForm.baseUrl = cfg.baseUrl || ''
  settingsForm.apiKey = cfg.apiKey || ''
  settingsForm.model = cfg.model || ''
}

function saveSettings() {
  saveLlmConfig({ baseUrl: settingsForm.baseUrl.trim(), apiKey: settingsForm.apiKey.trim(), model: settingsForm.model.trim() })
  settingsVisible.value = false
  ElMessage.success('已保存模型设置')
}

function resetSettings() {
  clearLlmConfig()
  settingsForm.baseUrl = ''
  settingsForm.apiKey = ''
  settingsForm.model = ''
  ElMessage.success('已恢复为默认免费模型')
}

async function scrollToBottom() {
  await nextTick()
  if (chatWindow.value) { chatWindow.value.scrollTop = chatWindow.value.scrollHeight }
}

async function loadGreeting() {
  loading.value = true
  try {
    const res = await counselorChat([])
    if (res?.reply) { messages.value.push({ role: 'assistant', content: res.reply }) }
  } catch (_) {
    messages.value.push({ role: 'assistant', content: '你好，我是你的情感陪伴顾问。可以先告诉我，最近发生了什么让你想到来这里？' })
  } finally { loading.value = false; scrollToBottom() }
}

async function send() {
  const text = input.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  scrollToBottom()
  loading.value = true
  try {
    const history = messages.value.slice(-20)
    const res = await counselorChat(history)
    messages.value.push({ role: 'assistant', content: res.reply })
  } catch (e) { console.warn('[counselor] chat failed', e) }
  finally { loading.value = false; scrollToBottom() }
}

function resetChat() { messages.value = []; loadGreeting() }

onMounted(() => { loadSettingsForm(); loadGreeting() })
</script>

<style scoped>
.counselor-page {
  display: flex;
  flex-direction: column;
  padding: 24px 28px;
  min-height: calc(100vh - 140px);
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 16px;
  gap: 8px;
}

.page-head > div:first-child {
  flex: 1;
}

.page-head h2 {
  margin: 0;
  font-size: 28px;
}

.page-head p {
  margin: 6px 0 0;
  color: var(--love-text-light);
  font-size: 14px;
}

.head-actions {
  display: flex;
  gap: 8px;
}

.head-actions .el-button {
  gap: 6px;
}

.chat-window {
  flex: 1;
  overflow-y: auto;
  background: linear-gradient(180deg, rgba(255, 247, 251, 0.8), rgba(255, 255, 255, 0.6));
  border-radius: var(--love-radius);
  padding: 20px;
  border: 1px solid rgba(255, 214, 231, 0.5);
  min-height: 360px;
}

.bubble-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  align-items: flex-end;
}

.bubble-row.user {
  flex-direction: row-reverse;
}

.avatar {
  flex-shrink: 0;
}

.assistant-avatar {
  background: linear-gradient(135deg, #ffd6e7, #e7dcff);
  color: #ff5f97;
  font-weight: 700;
}

.user-avatar {
  background: linear-gradient(135deg, #ff8fb5, #ff6f9f);
  color: #fff;
  font-weight: 700;
}

.bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: var(--love-radius-sm);
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble.assistant {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 214, 231, 0.5);
  color: #5a3344;
  border-top-left-radius: 4px;
}

.bubble.user {
  background: linear-gradient(135deg, #ff8fb5, #ff6f9f);
  color: #fff;
  border-top-right-radius: 4px;
  box-shadow: 0 4px 16px rgba(255, 111, 159, 0.25);
}

.typing {
  color: #c08aa0;
  font-style: italic;
}

.composer {
  margin-top: 14px;
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.hint {
  color: var(--love-text-muted);
  font-size: 12px;
}

.form-tip {
  font-size: 12px;
  color: var(--love-text-light);
  margin-top: 4px;
}

@media (max-width: 640px) {
  .counselor-page {
    padding: 20px;
    min-height: auto;
  }

  .page-head {
    flex-direction: column;
    align-items: stretch;
  }

  .page-head h2 {
    font-size: 26px;
  }

  .head-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .head-actions .el-button {
    width: 100%;
    justify-content: center;
  }

  .chat-window {
    min-height: 420px;
    padding: 16px;
  }

  .bubble {
    max-width: 82%;
  }

  .actions {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
  }

  .actions .el-button {
    width: 100%;
  }
}
</style>
