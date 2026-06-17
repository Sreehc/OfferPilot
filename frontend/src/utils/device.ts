const FINGERPRINT_KEY = 'offerpilot_device_fingerprint'

const safeStorage = () => {
  try { return window.localStorage } catch { return null }
}

export function getOrCreateDeviceFingerprint() {
  const storage = safeStorage()
  const existing = storage?.getItem(FINGERPRINT_KEY)
  if (existing) return existing
  const fingerprint = String(navigator.platform || 'web') + '-' + Date.now() + '-' + Math.random().toString(16).slice(2)
  storage?.setItem(FINGERPRINT_KEY, fingerprint)
  return fingerprint
}

export function getDeviceName() {
  const platform = navigator.platform || 'Web'
  const browser = navigator.userAgent.includes('Chrome') ? 'Chrome' : navigator.userAgent.includes('Safari') ? 'Safari' : 'Browser'
  return platform + ' ' + browser
}

export function getStoredDeviceId() {
  return safeStorage()?.getItem('offerpilot_device_id') ?? null
}

export function setStoredDeviceId(id: string) {
  safeStorage()?.setItem('offerpilot_device_id', id)
}
