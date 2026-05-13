import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'

vi.mock('@/api/auth', () => ({
  loginApi: vi.fn(),
  logoutApi: vi.fn(),
  registerApi: vi.fn(),
  fetchCurrentUserApi: vi.fn()
}))

vi.mock('@/utils/storage', () => ({
  storage: {
    getToken: vi.fn(() => null),
    setToken: vi.fn(),
    getUser: vi.fn(() => null),
    setUser: vi.fn(),
    clearToken: vi.fn(),
    clearUser: vi.fn()
  }
}))

vi.mock('@/utils/device', () => ({
  getOrCreateDeviceFingerprint: vi.fn(() => 'fp_test'),
  getDeviceName: vi.fn(() => '测试设备'),
  setStoredDeviceId: vi.fn()
}))

import { loginApi, logoutApi, registerApi, fetchCurrentUserApi } from '@/api/auth'
import { storage } from '@/utils/storage'

describe('AuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('initial state: not logged in', () => {
    const store = useAuthStore()
    expect(store.isLoggedIn).toBe(false)
    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
  })

  it('login persists token and user', async () => {
    const mockResponse = {
      code: 200,
      message: 'success',
      data: {
        token: 'jwt-token-123',
        userInfo: { id: 1, username: 'test', nickname: 'Test', role: 'USER' }
      }
    }
    vi.mocked(loginApi).mockResolvedValue(mockResponse as any)

    const store = useAuthStore()
    await store.login({ username: 'test', password: 'pass' })

    expect(store.isLoggedIn).toBe(true)
    expect(store.token).toBe('jwt-token-123')
    expect(store.user?.username).toBe('test')
    expect(storage.setToken).toHaveBeenCalledWith('jwt-token-123')
    expect(storage.setUser).toHaveBeenCalled()
  })

  it('register persists token and user', async () => {
    const mockResponse = {
      code: 200,
      message: 'success',
      data: {
        token: 'jwt-token-456',
        userInfo: { id: 2, username: 'newuser', nickname: 'New', role: 'USER' }
      }
    }
    vi.mocked(registerApi).mockResolvedValue(mockResponse as any)

    const store = useAuthStore()
    await store.register({ username: 'newuser', password: 'pass', nickname: 'New', email: 'new@example.com' })

    expect(store.isLoggedIn).toBe(true)
    expect(store.token).toBe('jwt-token-456')
  })

  it('logout clears state', async () => {
    // First login
    const loginResponse = {
      code: 200,
      message: 'success',
      data: { token: 'jwt', userInfo: { id: 1, username: 't', nickname: 'T', role: 'USER' } }
    }
    vi.mocked(loginApi).mockResolvedValue(loginResponse as any)
    vi.mocked(logoutApi).mockResolvedValue({ code: 200, message: 'ok', data: null } as any)

    const store = useAuthStore()
    await store.login({ username: 't', password: 'p' })
    expect(store.isLoggedIn).toBe(true)

    await store.logout()
    expect(store.isLoggedIn).toBe(false)
    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
    expect(storage.clearToken).toHaveBeenCalled()
    expect(storage.clearUser).toHaveBeenCalled()
  })

  it('restoreProfile updates user', async () => {
    // Set initial token via storage mock
    vi.mocked(storage.getToken).mockReturnValue('existing-token')

    const mockUser = { id: 1, username: 'restored', nickname: 'Restored', role: 'USER' }
    vi.mocked(fetchCurrentUserApi).mockResolvedValue({
      code: 200,
      message: 'success',
      data: mockUser
    } as any)

    const store = useAuthStore()
    // Pinia doesn't re-read storage on create, so set token manually
    store.token = 'existing-token'
    await store.restoreProfile()

    expect(store.user?.username).toBe('restored')
    expect(storage.setUser).toHaveBeenCalledWith(mockUser)
  })

  it('restoreProfile failure clears state', async () => {
    vi.mocked(storage.getToken).mockReturnValue('bad-token')
    vi.mocked(fetchCurrentUserApi).mockRejectedValue(new Error('401'))

    const store = useAuthStore()
    store.token = 'bad-token'
    await store.restoreProfile()

    expect(store.isLoggedIn).toBe(false)
    expect(store.token).toBeNull()
  })
})
