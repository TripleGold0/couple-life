<template>
  <div class="app-shell">
    <aside class="side">
      <router-link to="/app/home" class="brand" aria-label="Couple Life 首页">
        <el-icon><StarFilled /></el-icon>
        <span>Couple Life</span>
      </router-link>
      <nav class="nav-list desktop-nav" aria-label="主导航">
        <router-link v-for="item in menus" :key="item.path" :to="item.path" class="menu-item">
          <component :is="item.icon" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
      <nav class="mobile-primary-nav" aria-label="主导航">
        <router-link v-for="item in primaryMobileMenus" :key="item.path" :to="item.path" class="menu-item">
          <component :is="item.icon" />
          <span>{{ item.mobileLabel || item.label }}</span>
        </router-link>
        <el-dropdown trigger="click" placement="top-end">
          <button class="menu-item mobile-more-nav" :class="{ 'is-active': secondaryMobileMenus.some(item => item.path === route.path) }" type="button" aria-label="更多功能">
            <MoreFilled />
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
      <p class="side-hint">记录属于你们的日常</p>
    </aside>

    <main class="main">
      <header class="top">
        <div>
          <div class="hello">我们的空间</div>
          <div class="sub">记录日常，也收藏值得回看的时刻</div>
        </div>
        <el-dropdown>
          <button class="user-box" type="button" aria-label="打开用户菜单">
            <el-avatar :src="avatarOf(userStore.user)">{{ userStore.user?.nickname?.[0] || '我' }}</el-avatar>
            <span>{{ userStore.user?.nickname || '情侣用户' }}</span>
          </button>
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
  { path: '/app/home', label: '情侣主页', mobileLabel: '首页', icon: HomeFilled },
  { path: '/app/checkin', label: '每日打卡', mobileLabel: '打卡', icon: Calendar },
  { path: '/app/travel', label: '旅行地图', icon: Location },
  { path: '/app/album', label: '情侣相册', mobileLabel: '相册', icon: Camera },
  { path: '/app/counselor', label: '情感顾问', icon: ChatDotRound },
  { path: '/app/pet', label: '电子宠物', icon: Star },
  { path: '/app/profile', label: '个人信息', mobileLabel: '我的', icon: User }
]
const primaryMobileMenus = menus.filter(item => ['/app/home', '/app/checkin', '/app/album', '/app/profile'].includes(item.path))
const secondaryMobileMenus = menus.filter(item => !primaryMobileMenus.includes(item))

onMounted(async () => {
  try {
    const user = await userStore.fetchUser()
    if (user && user.profileCompleted === 0) showProfileDialog.value = true
  } catch (_) { /* The request interceptor handles authentication errors. */ }
})

function onProfileCompleted() { userStore.fetchUser().catch(() => {}) }
function logout() { userStore.logout(); router.push('/login') }
</script>

<style scoped>
.app-shell { display: flex; min-height: 100dvh; background: var(--color-bg); }
.side { position: sticky; top: 0; display: flex; flex: 0 0 208px; flex-direction: column; width: 208px; height: 100dvh; padding: 24px 16px; border-right: 1px solid var(--color-border); background: var(--color-surface); }
.brand { display: flex; align-items: center; gap: 8px; padding: 0 10px 24px; color: var(--color-primary); font-size: 17px; font-weight: 800; }
.nav-list { display: flex; flex: 1; flex-direction: column; gap: 4px; }
.mobile-primary-nav { display: none; }
.menu-item { display: flex; align-items: center; gap: 10px; width: 100%; min-height: 44px; padding: 10px 12px; border: 0; border-radius: var(--radius-md); color: var(--color-text-secondary); background: transparent; cursor: pointer; transition: color var(--transition-fast), background-color var(--transition-fast); }
.menu-item :deep(svg), .menu-item > svg { width: 20px; height: 20px; flex: 0 0 20px; }
.menu-item:hover { color: var(--color-text); background: var(--color-surface-subtle); }
.menu-item.router-link-active, .menu-item.is-active { color: var(--color-primary); background: #faeef2; font-weight: 700; }
.side-hint { margin: 0; padding: 16px 10px 0; border-top: 1px solid var(--color-border); color: var(--color-text-secondary); font-size: 12px; }
.main { flex: 1; min-width: 0; max-width: 1440px; margin: 0 auto; padding: 0 32px 40px; }
.top { display: flex; align-items: center; justify-content: space-between; min-height: 76px; margin-bottom: 20px; padding: 16px 0; }
.hello { font-size: 18px; font-weight: 800; }
.sub { margin-top: 4px; color: var(--color-text-secondary); font-size: 13px; }
.user-box { display: flex; align-items: center; gap: 10px; min-height: 44px; padding: 4px 8px; border: 0; border-radius: var(--radius-md); color: var(--color-text); background: transparent; cursor: pointer; }
.user-box:hover { background: var(--color-surface-subtle); }
@media (max-width: 900px) {
  .app-shell { display: block; padding-bottom: calc(76px + env(safe-area-inset-bottom)); }
  .side { position: fixed; z-index: 100; inset: auto 0 0 0; width: 100%; height: calc(68px + env(safe-area-inset-bottom)); padding: 6px 8px env(safe-area-inset-bottom); border: 0; border-top: 1px solid var(--color-border); box-shadow: 0 -6px 20px rgb(48 39 45 / 8%); }
  .brand, .desktop-nav, .side-hint { display: none; }
  .mobile-primary-nav { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 2px; }
  .mobile-primary-nav :deep(.el-dropdown) { min-width: 0; }
  .mobile-primary-nav .menu-item { flex-direction: column; justify-content: center; min-height: 56px; padding: 5px 4px; gap: 2px; font-size: 11px; }
  .main { width: 100%; padding: 0 16px 24px; }
  .top { min-height: 64px; margin-bottom: 12px; }
  .top .sub, .user-box > span { display: none; }
}
</style>
