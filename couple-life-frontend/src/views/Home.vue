<template>
  <div class="home-grid">
    <section class="hero love-card">
      <div>
        <h1 class="gradient-title">{{ user?.nickname || '你' }} 和 {{ user?.partner?.nickname || 'TA' }} 的情侣生活</h1>
        <p v-if="summary?.loveDays">已经一起度过了 <b>{{ summary.loveDays }}</b> 天，每一天都值得纪念。</p>
        <p v-else>还没有绑定另一半？前往个人信息分享你的邀请码吧。</p>
      </div>
      <div class="avatars">
        <el-avatar :size="78" :src="avatarOf(user)">{{ user?.nickname?.[0] || '我' }}</el-avatar>
        <span class="heart-icon">❤</span>
        <el-avatar :size="78" :src="avatarOf(user?.partner)">{{ user?.partner?.nickname?.[0] || 'TA' }}</el-avatar>
      </div>
    </section>

    <section class="block love-card">
      <h3>😊 最近的心情</h3>
      <el-empty v-if="!summary?.recentCheckins?.length" description="还没有打卡，去记录今天的心情吧" />
      <div v-else class="checkin-list">
        <div v-for="item in summary.recentCheckins" :key="item.id" class="checkin-item">
          <span class="emoji">{{ item.moodEmoji }}</span>
          <div><b>{{ item.nickname }}</b> · {{ item.date }}<p>{{ item.content || item.moodText }}</p></div>
        </div>
      </div>
    </section>

    <section class="block love-card">
      <h3>🗺️ 最近的旅行</h3>
      <el-empty v-if="!summary?.recentTravels?.length" description="还没有旅行记录" />
      <div v-else class="travel-list">
        <div v-for="item in summary.recentTravels" :key="item.id" class="travel-item">
          <el-image :src="item.coverImage" fit="cover" />
          <div><b>{{ item.locationName }}</b><p>{{ item.travelDate }} · {{ item.summary }}</p></div>
        </div>
      </div>
    </section>

    <section class="block love-card">
      <h3>📸 最近的照片</h3>
      <el-empty v-if="!summary?.recentPhotos?.length" description="还没有上传照片" />
      <div v-else class="photo-list">
        <el-image v-for="photo in summary.recentPhotos" :key="photo.id" :src="photo.imageUrl" fit="cover" class="photo" />
      </div>
    </section>

    <router-link v-for="card in cards" :key="card.path" :to="card.path" class="entry love-card">
      <div class="icon"><component :is="card.icon" /></div>
      <h3>{{ card.title }}</h3>
      <p>{{ card.desc }}</p>
    </router-link>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Calendar, Camera, ChatDotRound, Location, Star, User } from '@element-plus/icons-vue'
import { getHomeSummary } from '../api/user'
import { useUserStore } from '../stores/userStore'
import { avatarOf } from '../utils/defaultAvatar'

const userStore = useUserStore()
const user = computed(() => userStore.user)
const summary = ref(null)
const cards = [
  { path: '/app/checkin', icon: Calendar, title: '每日心情打卡', desc: '在日历上留下今天的表情和一句话。' },
  { path: '/app/travel', icon: Location, title: '旅行地图日志', desc: '点亮一起去过的世界角落。' },
  { path: '/app/album', icon: Camera, title: '情侣相册', desc: '按日期收藏照片和评论。' },
  { path: '/app/counselor', icon: ChatDotRound, title: '情感顾问', desc: '专业温柔的 AI 情感陪伴。' },
  { path: '/app/pet', icon: Star, title: '电子宠物', desc: '一起喂养属于你们的小宠物。' },
  { path: '/app/profile', icon: User, title: '个人信息', desc: '维护资料、绑定另一半。' }
]
onMounted(async () => { summary.value = await getHomeSummary().catch(() => null) })
</script>

<style scoped>
.home-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.hero {
  grid-column: 1 / -1;
  min-height: 200px;
  padding: 36px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, rgba(255, 214, 231, 0.5), rgba(231, 220, 255, 0.5));
}

.block {
  grid-column: span 2;
  padding: 24px;
}

.entry {
  padding: 24px;
  min-height: 160px;
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
}

.entry:hover {
  transform: translateY(-6px);
  box-shadow: 0 24px 64px rgba(255, 124, 168, 0.25);
}

h1 {
  margin: 0;
  font-size: 36px;
}

h3 {
  margin: 0 0 14px;
  font-size: 16px;
}

p {
  color: var(--love-text-light);
  line-height: 1.7;
}

.avatars {
  display: flex;
  align-items: center;
  gap: 18px;
}

.heart-icon {
  font-size: 32px;
  color: var(--love-primary);
  animation: heartbeat 1.5s ease-in-out infinite;
}

@keyframes heartbeat {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

.checkin-list, .travel-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.checkin-item {
  display: flex;
  gap: 12px;
  padding: 12px 14px;
  background: rgba(255, 247, 251, 0.8);
  border-radius: var(--love-radius-sm);
  transition: background 0.2s;
}

.checkin-item:hover {
  background: rgba(255, 214, 231, 0.3);
}

.emoji {
  font-size: 28px;
}

.travel-item {
  display: flex;
  gap: 14px;
  padding: 12px;
  background: rgba(255, 247, 251, 0.8);
  border-radius: var(--love-radius-sm);
  transition: background 0.2s;
}

.travel-item:hover {
  background: rgba(255, 214, 231, 0.3);
}

.travel-item .el-image {
  width: 90px;
  height: 64px;
  border-radius: var(--love-radius-xs);
}

.photo-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.photo {
  height: 84px;
  border-radius: var(--love-radius-xs);
  transition: transform 0.2s;
}

.photo:hover {
  transform: scale(1.05);
}

.icon {
  width: 38px;
  height: 38px;
  margin-bottom: 8px;
  color: var(--love-primary);
}

.icon svg {
  width: 100%;
  height: 100%;
}

/* 响应式 */
@media (max-width: 1024px) {
  .home-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .block {
    grid-column: span 2;
  }
}

@media (max-width: 640px) {
  .home-grid {
    grid-template-columns: 1fr;
  }
  .block {
    grid-column: span 1;
  }
  .hero {
    flex-direction: column;
    text-align: center;
    gap: 20px;
  }
  .entry {
    min-height: auto;
  }
}
</style>
