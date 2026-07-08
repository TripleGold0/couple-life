const AMAP_URL = 'https://webapi.amap.com/maps?v=2.0&key=2fbcf96605d89eed3194de796497b203'
const SECURITY_JS_CODE = '6173a54e27f6ecab89c4f0d4ff13189b'

let amapPromise = null

export function loadAMap() {
  if (window.AMap) return Promise.resolve(window.AMap)
  if (amapPromise) return amapPromise

  window._AMapSecurityConfig = {
    securityJsCode: SECURITY_JS_CODE
  }

  amapPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = AMAP_URL
    script.async = true
    script.onload = () => resolve(window.AMap)
    script.onerror = () => {
      amapPromise = null
      reject(new Error('AMap script failed to load'))
    }
    document.head.appendChild(script)
  })

  return amapPromise
}
