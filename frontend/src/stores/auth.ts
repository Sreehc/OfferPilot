import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchCurrentUserApi, loginApi, logoutApi, registerApi } from '@/api/auth'
import type { LoginPayload, RegisterPayload } from '@/api/auth'
import type { UserInfo } from '@/types/api'
import { storage } from '@/utils/storage'
import { getOrCreateDeviceFingerprint, getDeviceName, setStoredDeviceId } from '@/utils/device'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(storage.getToken())
  const user = ref<UserInfo | null>(storage.getUser())
  const isLoggedIn = computed(() => Boolean(token.value))

  function persist(nextToken: string, nextUser: UserInfo, deviceId?: number) {
    token.value = nextToken
    user.value = nextUser
    storage.setToken(nextToken)
    storage.setUser(nextUser)
    if (deviceId) {
      setStoredDeviceId(String(deviceId))
    }
  }

  async function login(payload: LoginPayload) {
    const fp = getOrCreateDeviceFingerprint()
    const response = await loginApi({
      ...payload,
      deviceFingerprint: fp,
      deviceName: getDeviceName()
    })
    persist(response.data.token, response.data.userInfo, response.data.deviceId)
  }

  async function register(payload: RegisterPayload) {
    const fp = getOrCreateDeviceFingerprint()
    const response = await registerApi({
      ...payload,
      deviceFingerprint: fp,
      deviceName: getDeviceName()
    })
    persist(response.data.token, response.data.userInfo, response.data.deviceId)
  }

  async function restoreProfile() {
    if (!token.value) return
    try {
      const response = await fetchCurrentUserApi()
      user.value = response.data
      storage.setUser(response.data)
    } catch {
      clear()
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      clear()
      window.location.replace('/login')
    }
  }

  function clear() {
    token.value = null
    user.value = null
    storage.clearToken()
    storage.clearUser()
  }

  function persistUser() {
    if (user.value) {
      storage.setUser(user.value)
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    login,
    register,
    restoreProfile,
    logout,
    clear,
    persistUser
  }
})
