import type { UserInfo } from '@/api/types'

const TOKEN_KEY = 'offerpilot_token'
const USER_KEY = 'offerpilot_user'
const DEVICE_KEY = 'offerpilot_device_id'
const THEME_KEY = 'offerpilot_theme'

const safeStorage = () => {
  try { return window.localStorage } catch { return null }
}

export const storage = {
  getToken: () => safeStorage()?.getItem(TOKEN_KEY) ?? null,
  setToken: (token: string) => safeStorage()?.setItem(TOKEN_KEY, token),
  clearToken: () => safeStorage()?.removeItem(TOKEN_KEY),
  getUser: (): UserInfo | null => {
    const raw = safeStorage()?.getItem(USER_KEY)
    if (!raw) return null
    try { return JSON.parse(raw) as UserInfo } catch { return null }
  },
  setUser: (user: UserInfo) => safeStorage()?.setItem(USER_KEY, JSON.stringify(user)),
  clearUser: () => safeStorage()?.removeItem(USER_KEY),
  getDeviceId: () => safeStorage()?.getItem(DEVICE_KEY) ?? null,
  setDeviceId: (id: string) => safeStorage()?.setItem(DEVICE_KEY, id),
  clearDeviceId: () => safeStorage()?.removeItem(DEVICE_KEY),
  getTheme: () => safeStorage()?.getItem(THEME_KEY) as 'light' | 'dark' | null,
  setTheme: (mode: 'light' | 'dark') => safeStorage()?.setItem(THEME_KEY, mode)
}
