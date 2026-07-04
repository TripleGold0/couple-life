<template>
  <div class="landing">
    <!-- 导航栏 -->
    <header class="nav">
      <div class="nav-inner">
        <div class="brand">💕 Couple Life</div>
        <div class="nav-links">
          <a href="#features">功能</a>
          <a href="#how">流程</a>
          <a href="#story">故事</a>
          <router-link to="/login" class="btn-login">登录</router-link>
          <router-link to="/register" class="btn-register">免费注册</router-link>
        </div>
      </div>
    </header>

    <!-- Hero 区域 -->
    <section class="hero">
      <div class="hero-content">
        <h1 class="gradient-title">
          把每一天的小确幸<br />沉淀为可回顾的甜蜜时间线
        </h1>
        <p class="hero-sub">
          Couple Life 为情侣打造专属的生活记录空间——打卡心情、记录旅行、共享相册，还有 AI 情感顾问和电子宠物陪伴你们的每一刻。
        </p>
        <div class="hero-actions">
          <router-link to="/register" class="cta-primary">免费开始使用</router-link>
          <a href="#features" class="cta-secondary">了解更多 ↓</a>
        </div>
        <div class="hero-stats">
          <div class="stat"><span class="stat-num">10,000+</span><span class="stat-label">甜蜜情侣</span></div>
          <div class="stat"><span class="stat-num">500,000+</span><span class="stat-label">打卡记录</span></div>
          <div class="stat"><span class="stat-num">1,000,000+</span><span class="stat-label">共享照片</span></div>
        </div>
      </div>
      <div class="hero-visual">
        <div class="phone-mockup love-card">
          <div class="mock-header">
            <div class="mock-dots">
              <span></span><span></span><span></span>
            </div>
          </div>
          <div class="mock-body">
            <div class="mock-heart">❤️</div>
            <div class="mock-title">在一起的第 <strong>365</strong> 天</div>
            <div class="mock-row">
              <div class="mock-card mini">📸 今日相册</div>
              <div class="mock-card mini">🗺️ 旅行足迹</div>
            </div>
            <div class="mock-card">😊 今日打卡：想你的一天</div>
            <div class="mock-card">🐱 宠物「团团」正在撒娇</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 功能亮点 -->
    <section id="features" class="features">
      <h2 class="section-title gradient-title">为爱设计的每一个功能</h2>
      <p class="section-sub">从日常点滴到珍贵回忆，一个平台满足你们所有的记录需求</p>
      <div class="feature-grid">
        <div v-for="f in features" :key="f.title" class="feature-card love-card">
          <div class="feature-icon">{{ f.icon }}</div>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 使用流程 -->
    <section id="how" class="how-it-works">
      <h2 class="section-title gradient-title">三步开启甜蜜旅程</h2>
      <p class="section-sub">注册只需 30 秒，邀请 TA 一起加入</p>
      <div class="steps">
        <div v-for="(step, i) in steps" :key="i" class="step">
          <div class="step-num">{{ i + 1 }}</div>
          <div class="step-icon">{{ step.icon }}</div>
          <h3>{{ step.title }}</h3>
          <p>{{ step.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 情感故事 -->
    <section id="story" class="story">
      <h2 class="section-title gradient-title">属于你们的甜蜜故事</h2>
      <p class="section-sub">每一对情侣都有独一无二的相处方式，Couple Life 帮你珍藏每一个瞬间</p>
      <div class="story-cards">
        <div v-for="s in stories" :key="s.name" class="story-card love-card">
          <div class="story-quote">"{{ s.quote }}"</div>
          <div class="story-author">
            <div class="story-avatar">{{ s.avatar }}</div>
            <div>
              <div class="story-name">{{ s.name }}</div>
              <div class="story-meta">{{ s.meta }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 底部 CTA -->
    <section class="final-cta">
      <div class="cta-box love-card">
        <h2 class="gradient-title">准备好开始记录了吗？</h2>
        <p>免费注册，邀请你的另一半，一起把生活过成诗。</p>
        <router-link to="/register" class="cta-primary">立即免费注册 →</router-link>
      </div>
    </section>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-brand">💕 Couple Life</div>
        <div class="footer-links">
          <a href="#features">功能</a>
          <a href="#how">流程</a>
          <a href="#story">故事</a>
          <router-link to="/login">登录</router-link>
        </div>
        <div class="footer-copy">© 2026 Couple Life. 用心记录，用爱守护。</div>
      </div>
    </footer>
  </div>
</template>

<script setup>
const features = [
  { icon: '📅', title: '每日心情打卡', desc: '用 emoji + 文字记录今天的心情，和 TA 一起分享每一天的喜怒哀乐。' },
  { icon: '🗺️', title: '旅行足迹地图', desc: '记录每一次出行，标注地点坐标，留下双人感受和旅途照片。' },
  { icon: '📸', title: '共享情侣相册', desc: '按日期分组的照片墙，支持评论互动，一键批量导出 ZIP 留档。' },
  { icon: '🤖', title: 'AI 情感顾问', desc: '内置心理咨询师，帮你们化解矛盾、增进理解，24 小时在线。' },
  { icon: '🐱', title: '电子宠物共养', desc: '一起养一只电子宠物，喂食、抚摸、玩耍，见证 TA 从 Baby 成长到 Adult。' },
  { icon: '📊', title: '恋爱数据看板', desc: '恋爱天数、打卡统计、旅行轨迹，用数据可视化你们的爱情故事。' }
]

const steps = [
  { icon: '✏️', title: '注册账号', desc: '手机号或邮箱注册，30 秒完成，立即进入你们的小天地。' },
  { icon: '💌', title: '邀请 TA 绑定', desc: '分享你的专属邀请码，对方输入后即刻建立情侣关系。' },
  { icon: '🎉', title: '开始记录', desc: '打卡、旅行、相册、养宠物……把每一个瞬间都变成永恒。' }
]

const stories = [
  { avatar: '👩', name: '小雨 & 阿杰', meta: '在一起 2 年', quote: '以前照片散落在各个地方，现在有了 Couple Life，每次翻相册都像重新恋爱了一次。' },
  { avatar: '👫', name: '琳琳 & 大伟', meta: '在一起 1 年', quote: '每天的打卡成了我们的小仪式，心情不好时看看对方写的文字，瞬间就被治愈了。' },
  { avatar: '💑', name: '晓晓 & 小明', meta: '在一起 3 年', quote: 'AI 顾问真的帮了我们大忙，吵架后它总能帮我们冷静下来好好沟通。强烈推荐！' }
]
</script>

<style scoped>
/* ===== 全局 ===== */
.landing {
  min-height: 100vh;
  overflow-x: hidden;
}

/* ===== 导航栏 ===== */
.nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.6);
}
.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 14px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand {
  font-size: 20px;
  font-weight: 800;
  color: #ff6f9f;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 28px;
}
.nav-links a {
  font-size: 14px;
  color: #765567;
  transition: color 0.2s;
}
.nav-links a:hover {
  color: #ff6f9f;
}
.btn-login {
  padding: 6px 18px;
  border-radius: 20px;
  border: 1px solid #ffd6e7;
  background: rgba(255, 255, 255, 0.6);
}
.btn-register {
  padding: 8px 22px;
  border-radius: 20px;
  background: linear-gradient(90deg, #ff7ca8, #b58cff);
  color: #fff !important;
  font-weight: 600;
}

/* ===== Hero ===== */
.hero {
  max-width: 1200px;
  margin: 0 auto;
  padding: 140px 24px 80px;
  display: flex;
  align-items: center;
  gap: 60px;
}
.hero-content {
  flex: 1;
}
.hero h1 {
  font-size: 48px;
  line-height: 1.25;
  margin: 0 0 20px;
}
.hero-sub {
  font-size: 17px;
  color: #765567;
  line-height: 1.7;
  margin-bottom: 32px;
  max-width: 520px;
}
.hero-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 40px;
}
.cta-primary {
  display: inline-block;
  padding: 14px 36px;
  border-radius: 28px;
  background: linear-gradient(90deg, #ff7ca8, #b58cff);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  box-shadow: 0 8px 30px rgba(255, 124, 168, 0.35);
  transition: transform 0.2s, box-shadow 0.2s;
}
.cta-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(255, 124, 168, 0.45);
}
.cta-secondary {
  display: inline-flex;
  align-items: center;
  padding: 14px 28px;
  border-radius: 28px;
  border: 1px solid #ffd6e7;
  background: rgba(255, 255, 255, 0.5);
  color: #ff6f9f;
  font-size: 16px;
  font-weight: 600;
  transition: background 0.2s;
}
.cta-secondary:hover {
  background: rgba(255, 255, 255, 0.8);
}
.hero-stats {
  display: flex;
  gap: 36px;
}
.stat {
  display: flex;
  flex-direction: column;
}
.stat-num {
  font-size: 22px;
  font-weight: 800;
  color: #ff6f9f;
}
.stat-label {
  font-size: 13px;
  color: #9b7587;
  margin-top: 2px;
}

/* Hero 视觉区 */
.hero-visual {
  flex-shrink: 0;
}
.phone-mockup {
  width: 320px;
  padding: 0;
  overflow: hidden;
}
.mock-header {
  padding: 12px 16px;
  background: linear-gradient(90deg, #ffd6e7, #e7dcff);
}
.mock-dots {
  display: flex;
  gap: 6px;
}
.mock-dots span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.7);
}
.mock-body {
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.mock-heart {
  font-size: 40px;
  text-align: center;
  margin-bottom: 4px;
}
.mock-title {
  text-align: center;
  font-size: 16px;
  color: #765567;
}
.mock-title strong {
  color: #ff6f9f;
  font-size: 28px;
}
.mock-row {
  display: flex;
  gap: 10px;
}
.mock-card {
  padding: 14px;
  border-radius: 14px;
  background: rgba(255, 214, 231, 0.25);
  font-size: 14px;
  color: #765567;
}
.mock-card.mini {
  flex: 1;
  padding: 12px 10px;
  font-size: 13px;
  text-align: center;
}

/* ===== 通用区块 ===== */
.section-title {
  text-align: center;
  font-size: 36px;
  margin: 0 0 12px;
}
.section-sub {
  text-align: center;
  color: #9b7587;
  font-size: 16px;
  margin-bottom: 48px;
}

/* ===== 功能亮点 ===== */
.features {
  max-width: 1200px;
  margin: 0 auto;
  padding: 80px 24px;
}
.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.feature-card {
  padding: 32px 28px;
  transition: transform 0.2s, box-shadow 0.2s;
}
.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 24px 64px rgba(255, 124, 168, 0.25);
}
.feature-icon {
  font-size: 40px;
  margin-bottom: 16px;
}
.feature-card h3 {
  font-size: 18px;
  margin: 0 0 10px;
  color: #5f4150;
}
.feature-card p {
  font-size: 14px;
  color: #9b7587;
  line-height: 1.7;
  margin: 0;
}

/* ===== 使用流程 ===== */
.how-it-works {
  max-width: 1000px;
  margin: 0 auto;
  padding: 80px 24px;
}
.steps {
  display: flex;
  gap: 32px;
}
.step {
  flex: 1;
  text-align: center;
  padding: 36px 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.6);
  position: relative;
}
.step-num {
  position: absolute;
  top: -16px;
  left: 50%;
  transform: translateX(-50%);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(90deg, #ff7ca8, #b58cff);
  color: #fff;
  font-weight: 800;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.step-icon {
  font-size: 44px;
  margin-bottom: 16px;
  margin-top: 8px;
}
.step h3 {
  font-size: 18px;
  margin: 0 0 10px;
  color: #5f4150;
}
.step p {
  font-size: 14px;
  color: #9b7587;
  line-height: 1.7;
  margin: 0;
}

/* ===== 情感故事 ===== */
.story {
  max-width: 1200px;
  margin: 0 auto;
  padding: 80px 24px;
}
.story-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.story-card {
  padding: 28px;
}
.story-quote {
  font-size: 15px;
  line-height: 1.8;
  color: #765567;
  margin-bottom: 20px;
  font-style: italic;
}
.story-author {
  display: flex;
  align-items: center;
  gap: 12px;
}
.story-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd6e7, #e7dcff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.story-name {
  font-weight: 700;
  font-size: 14px;
  color: #5f4150;
}
.story-meta {
  font-size: 12px;
  color: #b89daa;
}

/* ===== 底部 CTA ===== */
.final-cta {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 24px 80px;
}
.cta-box {
  text-align: center;
  padding: 60px 40px;
}
.cta-box h2 {
  font-size: 32px;
  margin: 0 0 16px;
}
.cta-box p {
  color: #9b7587;
  font-size: 16px;
  margin-bottom: 32px;
}

/* ===== 页脚 ===== */
.footer {
  border-top: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.3);
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 36px 24px;
  text-align: center;
}
.footer-brand {
  font-size: 18px;
  font-weight: 800;
  color: #ff6f9f;
  margin-bottom: 16px;
}
.footer-links {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 20px;
}
.footer-links a {
  font-size: 14px;
  color: #9b7587;
}
.footer-links a:hover {
  color: #ff6f9f;
}
.footer-copy {
  font-size: 13px;
  color: #b89daa;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .nav-links a:not(.btn-login):not(.btn-register) {
    display: none;
  }
  .hero {
    flex-direction: column;
    padding: 100px 20px 60px;
    text-align: center;
  }
  .hero h1 {
    font-size: 32px;
  }
  .hero-sub {
    max-width: 100%;
  }
  .hero-actions {
    justify-content: center;
    flex-wrap: wrap;
  }
  .hero-stats {
    justify-content: center;
  }
  .feature-grid {
    grid-template-columns: 1fr;
  }
  .steps {
    flex-direction: column;
  }
  .story-cards {
    grid-template-columns: 1fr;
  }
}
</style>
