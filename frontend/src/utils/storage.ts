import type { UserInfo } from '@/types/api'

const TOKEN_KEY = 'bytecoach_token'
const USER_KEY = 'bytecoach_user'

const guideKey = (userId: number | string) => `bytecoach_guide_seen_${userId}`

export const storage = {
  getToken: (): string | null => localStorage.getItem(TOKEN_KEY),
  setToken: (token: string): void => { localStorage.setItem(TOKEN_KEY, token) },
  clearToken: (): void => { localStorage.removeItem(TOKEN_KEY) },
  getUser: (): UserInfo | null => {
    const raw = localStorage.getItem(USER_KEY)
    if (!raw) return null
    try {
      return JSON.parse(raw) as UserInfo
    } catch {
      return null
    }
  },
  setUser: (user: UserInfo): void => { localStorage.setItem(USER_KEY, JSON.stringify(user)) },
  clearUser: (): void => { localStorage.removeItem(USER_KEY) },
  getGuideSeen: (userId: number | string): boolean => localStorage.getItem(guideKey(userId)) === '1',
  setGuideSeen: (userId: number | string): void => { localStorage.setItem(guideKey(userId), '1') }
}
