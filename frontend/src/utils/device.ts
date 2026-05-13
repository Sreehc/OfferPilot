/**
 * Generate a simple browser/device fingerprint.
 * Not cryptographically secure — good enough for device identification.
 */
export function generateDeviceFingerprint(): string {
  const canvas = document.createElement('canvas')
  let canvasData = 'canvas-unavailable'

  try {
    const ctx = canvas.getContext('2d')
    if (ctx) {
      ctx.textBaseline = 'top'
      ctx.font = '14px Arial'
      ctx.fillText('OfferPilot', 2, 2)
    }
    const encoded = canvas.toDataURL()
    if (encoded) {
      canvasData = encoded
    }
  } catch {
    // jsdom and some privacy modes may not expose canvas APIs.
  }

  const components = [
    navigator.userAgent,
    navigator.language,
    screen.width + 'x' + screen.height,
    screen.colorDepth,
    new Date().getTimezoneOffset(),
    canvasData.slice(-50), // last 50 chars of canvas fingerprint
  ]

  let hash = 0
  const str = components.join('|')
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash // Convert to 32-bit integer
  }
  return 'fp_' + Math.abs(hash).toString(36)
}

/**
 * Build a human-readable device name from the user agent.
 */
export function getDeviceName(): string {
  const ua = navigator.userAgent

  // Detect browser
  let browser = '未知浏览器'
  if (ua.includes('Firefox/')) {
    browser = '火狐浏览器'
  } else if (ua.includes('Edg/')) {
    browser = '系统浏览器'
  } else if (ua.includes('Chrome/')) {
    browser = '谷歌浏览器'
  } else if (ua.includes('Safari/')) {
    browser = '苹果浏览器'
  }

  // Detect OS
  let os = '未知系统'
  if (ua.includes('Windows')) {
    os = '视窗系统'
  } else if (ua.includes('Mac OS')) {
    os = '苹果电脑'
  } else if (ua.includes('Linux')) {
    os = 'Linux 系统'
  } else if (ua.includes('Android')) {
    os = '安卓设备'
  } else if (ua.includes('iPhone') || ua.includes('iPad')) {
    os = '苹果设备'
  }

  return `${os} · ${browser}`
}

export function localizeDeviceName(name?: string): string {
  if (!name) return '未知设备'

  return name
    .replace(/Firefox/g, '火狐浏览器')
    .replace(/Edge/g, '系统浏览器')
    .replace(/Chrome/g, '谷歌浏览器')
    .replace(/Safari/g, '苹果浏览器')
    .replace(/Windows/g, '视窗系统')
    .replace(/macOS/g, '苹果电脑')
    .replace(/Linux/g, 'Linux 系统')
    .replace(/Android/g, '安卓设备')
    .replace(/\biOS\b/g, '苹果设备')
    .replace(/\bon\b/g, '·')
    .trim()
}

const DEVICE_ID_KEY = 'offerpilot_device_id'
const DEVICE_FP_KEY = 'offerpilot_device_fp'

export function getStoredDeviceId(): string | null {
  return localStorage.getItem(DEVICE_ID_KEY)
}

export function setStoredDeviceId(id: string): void {
  localStorage.setItem(DEVICE_ID_KEY, id)
}

export function getOrCreateDeviceFingerprint(): string {
  let fp = localStorage.getItem(DEVICE_FP_KEY)
  if (!fp) {
    fp = generateDeviceFingerprint()
    localStorage.setItem(DEVICE_FP_KEY, fp)
  }
  return fp
}
