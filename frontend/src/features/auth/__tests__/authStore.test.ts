import { describe, expect, it, beforeEach } from 'vitest'
import { useAuthStore } from '../authStore'

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    useAuthStore.getState().clear()
  })

  it('persists a completed login response', () => {
    useAuthStore.getState().persistFromResponse({
      token: 'token-1',
      deviceId: 7,
      userInfo: { id: 1, username: 'admin', role: 'ADMIN' }
    })

    expect(useAuthStore.getState().token).toBe('token-1')
    expect(useAuthStore.getState().user?.role).toBe('ADMIN')
    expect(localStorage.getItem('offerpilot_device_id')).toBe('7')
  })
})
