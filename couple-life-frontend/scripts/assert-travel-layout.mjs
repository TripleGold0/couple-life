import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const viewPath = resolve(__dirname, '../src/views/Travel.vue')
const source = readFileSync(viewPath, 'utf8')

function block(selector) {
  const escaped = selector.replace('.', '\\.')
  const match = source.match(new RegExp(`${escaped}\\s*\\{([\\s\\S]*?)\\}`))
  if (!match) throw new Error(`Missing CSS block for ${selector}`)
  return match[1]
}

function mustContain(css, expected, selector) {
  if (!css.includes(expected)) {
    throw new Error(`${selector} must include "${expected}"`)
  }
}

const wrap = block('.travel-wrap')
const mapCard = block('.map-card')
const map = block('.map')

mustContain(wrap, 'min-height: calc(100vh - 126px)', '.travel-wrap')
mustContain(mapCard, 'display: flex', '.map-card')
mustContain(mapCard, 'flex-direction: column', '.map-card')
mustContain(mapCard, 'min-height: calc(100vh - 126px)', '.map-card')
mustContain(map, 'flex: 1', '.map')
mustContain(map, 'min-height: 520px', '.map')

console.log('Travel layout OK: map card stretches to the page bottom and map fills remaining space.')
