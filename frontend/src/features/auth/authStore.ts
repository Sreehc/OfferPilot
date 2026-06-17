import { create } from 'zustand'
import { fetchCurrentUserApi, loginApi, logoutApi, registerApi, verifyTwoFactorApi, type LoginPayload, type RegisterPayload } from '@/api/modules/auth'
import type { LoginResponse, UserInfo } from '@/api/types'
import { setStoredDeviceId } from '@/utils/device'
import { storage } from '@/utils/storage'

interface AuthState {
  token: string | null
  user: UserInfo | null
  restoring: boolean
  isLoggedIn: boolean
  login: (payload: LoginPayload) => Promise<LoginResponse>
  register: (payload: RegisterPayload) => Promise<void>
  verifyTwoFactor: (tempToken: string, code: string) => Promise<void>
  restoreProfile: () => Promise<void>
  logout: () => Promise<void>
  clear: () => void
  persistFromResponse: (data: LoginResponse) => void
}

function persist(data: LoginResponse) {
  if (!data.token || !data.userInfo) return
  storage.setToken(data.token)
  storage.setUser(data.userInfo)
  if (data.deviceId) setStoredDeviceId(String(data.deviceId))
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: storage.getToken(),
  user: storage.getUser(),
  restoring: false,
  get isLoggedIn() { return Boolean(get().token) },
  async login(payload) {
    const response = await loginApi(payload)
    const data = response.data
    if (!data.requires2fa) {
      persist(data)
      set({ token: data.token, user: data.userInfo })
    }
    return data
  },
  async register(payload) {
    const response = await registerApi(payload)
    persist(response.data)
    set({ token: response.data.token, user: response.data.userInfo })
  },
  async verifyTwoFactor(tempToken, code) {
    const response = await verifyTwoFactorApi(tempToken, code)
    persist(response.data)
    set({ token: response.data.token, user: response.data.userInfo })
  },
  async restoreProfile() {
    if (!get().token) return
    set({ restoring: true })
    try {
      const response = await fetchCurrentUserApi()
      storage.setUser(response.data)
      set({ user: response.data })
    } catch {
      get().clear()
    } finally {
      set({ restoring: false })
    }
  },
  async logout() {
    try { await logoutApi() } finally { get().clear(); window.location.replace('/login') }
  },
  clear() {
    storage.clearToken(); storage.clearUser(); set({ token: null, user: null })
  },
  persistFromResponse(data) {
    persist(data); set({ token: data.token, user: data.userInfo })
  }
}))
