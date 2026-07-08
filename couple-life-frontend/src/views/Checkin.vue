<template>
  <div class="love-card checkin-page">
    <div class="page-head">
      <div>
        <h2 class="gradient-title">每日打卡</h2>
        <p>每一天只能打卡一次，把心情放进日历里。</p>
      </div>
      <el-button type="primary" size="large" @click="openToday">
        <el-icon><EditPen /></el-icon>
        今日打卡
      </el-button>
    </div>
    <el-calendar v-model="currentDate">
      <template #date-cell="{ data }">
        <div class="calendar-cell" :class="{ 'is-today': data.day === today }" @click="openDetail(data.day)">
          <div class="day">{{ data.day.split('-')[2] }}</div>
          <div class="emojis"><span v-for="item in getByDate(data.day)" :key="item.id" class="emoji-tag">{{ item.moodEmoji }}</span></div>
        </div>
      </template>
    </el-calendar>
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form v-if="isToday" :model="form" label-position="top">
        <el-form-item label="今天的心情">
          <el-radio-group v-model="form.moodEmoji" class="emoji-picker">
            <el-radio-button v-for="emoji in emojis" :key="emoji" :value="emoji">{{ emoji }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="心情文字"><el-input v-model="form.moodText" placeholder="开心、想念、感动..." /></el-form-item>
        <el-form-item label="今日记录"><el-input v-model="form.content" type="textarea" :rows="4" placeholder="记录今天想说的话..." /></el-form-item>
        <el-button type="primary" size="large" class="full" @click="submit">保存打卡</el-button>
      </el-form>
      <div v-if="!isToday" class="detail-list">
        <div v-for="item in detailRecords" :key="item.id" class="detail-item">
          <div class="detail-header">
            <span class="detail-emoji">{{ item.moodEmoji }}</span>
            <b>{{ item.nickname }}</b>
            <span class="detail-mood">{{ item.moodText }}</span>
          </div>
          <p v-if="item.content">{{ item.content }}</p>
        </div>
        <el-empty v-if="!detailRecords.length" description="这天还没有打卡记录" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import { addCheckin, getCheckinCalendar, getCoupleCheckins } from '../api/checkin'

const currentDate = ref(new Date())
const records = ref([])
const detailRecords = ref([])
const dialogVisible = ref(false)
const selectedDate = ref('')
const emojis = ['😊', '🥰', '💕', '🌙', '☀️', '🍰', '😭', '😴']
const form = reactive({ moodEmoji: '😊', moodText: '', content: '' })
const today = new Date().toISOString().slice(0, 10)
const isToday = computed(() => selectedDate.value === today)
const dialogTitle = computed(() => isToday.value ? '📝 今日打卡' : `📅 ${selectedDate.value} 打卡详情`)

onMounted(loadCalendar)
watch(currentDate, loadCalendar)

function monthValue() {
  const date = currentDate.value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

async function loadCalendar() { records.value = await getCheckinCalendar(monthValue()) }
function getByDate(date) { return records.value.filter(item => item.date === date) }
function openToday() { openDetail(today) }
async function openDetail(date) { selectedDate.value = date; detailRecords.value = await getCoupleCheckins(date); dialogVisible.value = true }

async function submit() {
  await addCheckin({ checkinDate: selectedDate.value, ...form })
  ElMessage.success('打卡成功 ✨')
  dialogVisible.value = false
  await loadCalendar()
}
</script>

<style scoped>
.checkin-page {
  padding: 28px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  margin-bottom: 20px;
}

.page-head h2 {
  margin: 0;
  font-size: 28px;
}

.page-head p {
  color: var(--love-text-light);
  margin: 6px 0 0;
}

.full {
  width: 100%;
}

.page-head .el-button {
  flex: 0 0 auto;
  gap: 6px;
}

.calendar-cell {
  height: 82px;
  padding: 8px;
  border-radius: var(--love-radius-sm);
  cursor: pointer;
  transition: background 0.2s;
}

.calendar-cell:hover {
  background: rgba(255, 240, 246, 0.8);
}

.calendar-cell.is-today {
  background: rgba(255, 214, 231, 0.3);
}

.day {
  font-weight: 700;
  font-size: 14px;
}

.emojis {
  margin-top: 6px;
  display: flex;
  gap: 4px;
}

.emoji-tag {
  font-size: 20px;
  transition: transform 0.2s;
}

.emoji-tag:hover {
  transform: scale(1.2);
}

.emoji-picker .el-radio-button {
  margin-bottom: 4px;
}

.detail-item {
  padding: 16px;
  border-radius: var(--love-radius-sm);
  background: rgba(255, 247, 251, 0.8);
  margin-bottom: 12px;
  transition: background 0.2s;
}

.detail-item:hover {
  background: rgba(255, 214, 231, 0.25);
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-emoji {
  font-size: 24px;
}

.detail-mood {
  color: var(--love-text-light);
  font-size: 13px;
}

.detail-item p {
  margin: 8px 0 0;
  color: var(--love-text);
  line-height: 1.6;
}

@media (max-width: 640px) {
  .checkin-page {
    padding: 20px;
  }

  .page-head {
    flex-direction: column;
    align-items: stretch;
  }

  .page-head h2 {
    font-size: 26px;
  }

  .page-head .el-button {
    width: 100%;
    justify-content: center;
  }

  .calendar-cell {
    height: 68px;
    padding: 6px;
  }

  .emojis {
    flex-wrap: wrap;
  }
}
</style>
