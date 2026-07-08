<template>
  <div class="app-shell">
    <aside class="side love-card">
      <div class="brand">
        <el-icon><StarFilled /></el-icon>
        <span>Couple Life</span>
      </div>
      <nav class="nav-list desktop-nav">
        <router-link v-for="item in menus" :key="item.path" :to="item.path" class="menu-item">
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
      <nav class="mobile-primary-nav" aria-label="主要导航">
        <router-link v-for="item in primaryMobileMenus" :key="item.path" :to="item.path" class="menu-item">
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </router-link>
        <el-dropdown trigger="click" placement="bottom-end">
          <button class="menu-item mobile-more-nav" :class="{ 'is-active': secondaryMobileMenus.some(item => item.path === route.path) }" type="button">
            <MoreFilled :size="18" />
            <span>更多</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="item in secondaryMobileMenus" :key="item.path" @click="router.push(item.path)">
                <el-icon><component :is="item.icon" /></el-icon>
                {{ item.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </nav>
      <div class="side-footer">
        <div class="side-hint">记录每一个甜蜜瞬间</div>
      </div>
    </aside>
    <main class="main">
      <header class="top love-card">
        <div>
          <div class="hello">愿每一天都值得被温柔记录</div>
          <div class="sub">打卡、旅行、相册和属于你们的纪念日</div>
        </div>
        <el-dropdown>
          <div class="user-box">
            <el-avatar :src="avatarOf(userStore.user)">{{ userStore.user?.nickname?.[0] || '爱' }}</el-avatar>
            <span>{{ userStore.user?.nickname || '情侣用户' }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/app/profile')">个人信息</el-dropdown-item>
              <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <router-view />
    </main>
    <ProfileSetupDialog v-model="showProfileDialog" @completed="onProfileCompleted" />
    <FloatingPet />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Calendar, Camera, ChatDotRound, HomeFilled, Location, MoreFilled, Star, StarFilled, User } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/userStore'
import { avatarOf } from '../utils/defaultAvatar'
import ProfileSetupDialog from '../components/ProfileSetupDialog.vue'
import FloatingPet from '../components/FloatingPet.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const showProfileDialog = ref(false)
const menus = [
  { path: '/app/home', label: '情侣主页', icon: HomeFilled },
  { path: '/app/checkin', label: '每日打卡', icon: Calendar },
  { path: '/app/travel', label: '旅行地图', icon: Location },
  { path: '/app/album', label: '情侣相册', icon: Camera },
  { path: '/app/counselor', label: '情感顾问', icon: ChatDotRound },
  { path: '/app/pet', label: '电子宠物', icon: Star },
  { path: '/app/profile', label: '个人信息', icon: User }
]
const primaryMobileMenus = menus.filter(item => ['/app/home', '/app/checkin', '/app/album', '/app/profile'].includes(item.path))
const secondaryMobileMenus = menus.filter(item => !primaryMobileMenus.includes(item))

onMounted(async () => {
  try {
    const u = await userStore.fetchUser()
    if (u && u.profileCompleted === 0) showProfileDialog.value = true
  } catch (_) { /* ignore */ }
})

function onProfileCompleted() {
  userStore.fetchUser().catch(() => {})
}

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
  padding: 16px;
  gap: 16px;
}

.side {
  width: 168px;
  padding: 18px 10px;
  position: sticky;
  top: 16px;
  height: calc(100vh - 32px);
  display: flex;
  flex-direction: column;
  flex: 0 0 168px;
}

.brand {
  font-size: 17px;
  font-weight: 800;
  margin-bottom: 22px;
  color: var(--love-primary);
  padding: 0 8px;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 7px;
}

.nav-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-primary-nav {
  display: none;
}

.menu-item {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-start;
  min-height: 46px;
  padding: 10px 10px;
  border-radius: 12px;
  color: #765567;
  font-size: 13px;
  line-height: 1.25;
  border: 0;
  background: transparent;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}

.menu-item :deep(svg) {
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
}

.menu-item:hover {
  background: rgba(255, 214, 231, 0.3);
}

.menu-item.router-link-active,
.menu-item.is-active {
  background: linear-gradient(90deg, #ffd6e7, #fff1cc);
  color: var(--love-primary);
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(255, 124, 168, 0.15);
}

.side-footer {
  padding: 12px 8px 4px;
  border-top: 1px solid rgba(255, 214, 231, 0.4);
}

.side-hint {
  font-size: 12px;
  color: var(--love-text-muted);
  text-align: center;
}

.main {
  flex: 1;
  min-width: 0;
}

.top {
  height: 88px;
  padding: 20px 28px;
  margin-bottom: 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hello {
  font-size: 20px;
  font-weight: 800;
}

.sub {
  margin-top: 6px;
  color: var(--love-text-light);
  font-size: 14px;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 14px;
  transition: background 0.2s;
}

.user-box:hover {
  background: rgba(255, 214, 231, 0.3);
}

@media (max-width: 900px) {
  .app-shell {
    flex-direction: column;
    padding: 12px;
  }

  .side {
    position: static;
    width: 100%;
    height: auto;
    flex-basis: auto;
    padding: 14px;
  }

  .brand {
    margin-bottom: 14px;
  }

  .desktop-nav {
    display: none;
  }

  .mobile-primary-nav {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 6px;
  }

  .mobile-primary-nav :deep(.el-dropdown) {
    min-width: 0;
  }

  .mobile-primary-nav .menu-item {
    flex-direction: row;
    width: 100%;
    min-height: 54px;
    padding: 8px 6px;
    justify-content: center;
    flex-wrap: wrap;
    gap: 4px 6px;
    text-align: center;
    touch-action: manipulation;
  }

  .side-footer {
    display: none;
  }
}
</style>
