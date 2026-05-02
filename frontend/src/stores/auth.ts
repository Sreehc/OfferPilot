import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchCurrentUserApi, loginApi, logoutApi, registerApi } from '@/api/auth'
import type { LoginPayload, RegisterPayload } from '@/api/auth'
import type { UserInfo } from '@/types/api'
import { storage } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(storage.getToken())
  const user = ref<UserInfo | null>(storage.getUser())
  const isLoggedIn = computed(() => Boolean(token.value))

  function persist(nextToken: string, nextUser: UserInfo) {
    token.value = nextToken
    user.value = nextUser
    storage.setToken(nextToken)
    storage.setUser(nextUser)
  }

  async function login(payload: LoginPayload) {
    const response = await loginApi(payload)
    persist(response.data.token, response.data.userInfo)
  }

  async function register(payload: RegisterPayload) {
    const response = await registerApi(payload)
    persist(response.data.token, response.data.userInfo)
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
