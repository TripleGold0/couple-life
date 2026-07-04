import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const layoutPath = resolve(__dirname, '../src/layouts/MainLayout.vue')
const source = readFileSync(layoutPath, 'utf8')

function block(selector) {
  const match = source.match(new RegExp(`${selector.replace('.', '\\.')}\\s*\\{([\\s\\S]*?)\\}`))
  if (!match) {
    throw new Error(`Missing CSS block for ${selector}`)
  }
  return match[1]
}

function px(css, property) {
  const match = css.match(new RegExp(`${property}\\s*:\\s*(\\d+)px`))
  if (!match) {
    throw new Error(`Missing ${property} px value`)
  }
  return Number(match[1])
}

const side = block('.side')
const menuItem = block('.menu-item')
const sidebarWidth = px(side, 'width')
const menuPadding = menuItem.match(/padding\s*:\s*(\d+)px\s+(\d+)px/)

if (sidebarWidth > 172) {
  throw new Error(`Sidebar is ${sidebarWidth}px wide; expected 172px or less.`)
}

if (!menuPadding) {
  throw new Error('Menu item padding must use vertical and horizontal px values.')
}

const verticalPadding = Number(menuPadding[1])
if (verticalPadding < 10) {
  throw new Error(`Menu item vertical padding is ${verticalPadding}px; keep touch targets comfortable.`)
}

console.log(`Sidebar compactness OK: ${sidebarWidth}px wide with ${verticalPadding}px vertical item padding.`)
