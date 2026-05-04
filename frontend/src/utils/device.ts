/**
 * Generate a simple browser/device fingerprint.
 * Not cryptographically secure — good enough for device identification.
 */
export function generateDeviceFingerprint(): string {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (ctx) {
    ctx.textBaseline = 'top'
    ctx.font = '14px Arial'
    ctx.fillText('ByteCoach', 2, 2)
  }
  const canvasData = canvas.toDataURL()

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
  let browser = 'Unknown'
  if (ua.includes('Firefox/')) {
    browser = 'Firefox'
  } else if (ua.includes('Edg/')) {
    browser = 'Edge'
  } else if (ua.includes('Chrome/')) {
    browser = 'Chrome'
  } else if (ua.includes('Safari/')) {
    browser = 'Safari'
  }

  // Detect OS
  let os = 'Unknown'
  if (ua.includes('Windows')) {
    os = 'Windows'
  } else if (ua.includes('Mac OS')) {
    os = 'macOS'
  } else if (ua.includes('Linux')) {
    os = 'Linux'
  } else if (ua.includes('Android')) {
    os = 'Android'
  } else if (ua.includes('iPhone') || ua.includes('iPad')) {
    os = 'iOS'
  }

  return `${browser} on ${os}`
}

const DEVICE_ID_KEY = 'bytecoach_device_id'
const DEVICE_FP_KEY = 'bytecoach_device_fp'

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
