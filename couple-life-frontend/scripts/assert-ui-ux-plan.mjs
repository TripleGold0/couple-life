import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = resolve(__dirname, '..')

function read(relativePath) {
  return readFileSync(resolve(root, relativePath), 'utf8')
}

function mustInclude(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`${label} must include "${expected}"`)
  }
}

function mustNotInclude(source, unexpected, label) {
  if (source.includes(unexpected)) {
    throw new Error(`${label} must not include "${unexpected}"`)
  }
}

const app = read('src/App.vue')
mustInclude(app, '<ParticleBackground />', 'App.vue')
mustInclude(app, 'import ParticleBackground', 'App.vue')

const particle = read('src/components/ParticleBackground.vue')
mustInclude(particle, 'pointer-events: none', 'ParticleBackground.vue')
mustInclude(particle, '@media (prefers-reduced-motion: reduce)', 'ParticleBackground.vue')
mustInclude(particle, 'particle-core', 'ParticleBackground.vue')
mustInclude(particle, 'particle-halo', 'ParticleBackground.vue')

const main = read('src/main.js')
mustInclude(main, "import zhCn from 'element-plus/es/locale/lang/zh-cn'", 'main.js')
mustInclude(main, '.use(ElementPlus, { locale: zhCn })', 'main.js')

const index = read('index.html')
mustNotInclude(index, 'webapi.amap.com/maps', 'index.html')
mustNotInclude(index, '_AMapSecurityConfig', 'index.html')

const vite = read('vite.config.js')
mustInclude(vite, 'manualChunks', 'vite.config.js')

const request = read('src/utils/request.js')
mustInclude(request, 'showErrorMessage', 'request.js')
mustInclude(request, 'lastErrorMessage', 'request.js')

const login = read('src/views/Login.vue')
mustInclude(login, 'width: min(100%, 460px)', 'Login.vue')
mustInclude(login, '@media (max-width: 640px)', 'Login.vue')

const register = read('src/views/Register.vue')
mustInclude(register, 'width: min(100%, 580px)', 'Register.vue')
mustInclude(register, ':span="formColSpan"', 'Register.vue')

const mainLayout = read('src/layouts/MainLayout.vue')
mustInclude(mainLayout, 'mobile-primary-nav', 'MainLayout.vue')
mustInclude(mainLayout, 'mobile-more-nav', 'MainLayout.vue')
mustInclude(mainLayout, 'const primaryMobileMenus', 'MainLayout.vue')
mustInclude(mainLayout, 'const secondaryMobileMenus', 'MainLayout.vue')

for (const file of ['src/views/Album.vue', 'src/views/Counselor.vue', 'src/views/Checkin.vue']) {
  const source = read(file)
  mustInclude(source, '@media (max-width: 640px)', file)
  mustInclude(source, '.page-head', file)
  mustInclude(source, 'flex-direction: column', file)
}

const travel = read('src/views/Travel.vue')
mustInclude(travel, "import { loadAMap } from '../utils/amapLoader'", 'Travel.vue')
mustInclude(travel, 'await loadAMap()', 'Travel.vue')

const landing = read('src/views/Landing.vue')
mustInclude(landing, 'clamp(', 'Landing.vue')
mustInclude(landing, 'grid-template-columns: repeat(3, minmax(0, 1fr))', 'Landing.vue')

console.log('UI/UX plan assertions passed.')
