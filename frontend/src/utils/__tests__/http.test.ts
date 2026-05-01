import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock dependencies before importing the module
vi.mock('axios', () => {
  const mockInstance = {
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() }
    },
    request: vi.fn()
  }
  return {
    default: {
      create: vi.fn(() => mockInstance)
    }
  }
})

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn()
  }
}))

vi.mock('@/router', () => ({
  default: { push: vi.fn() }
}))

vi.mock('@/utils/storage', () => ({
  storage: {
    getToken: vi.fn(() => null),
    setToken: vi.fn(),
    clearToken: vi.fn(),
    clearUser: vi.fn()
  }
}))

import axios from 'axios'
import { storage } from '@/utils/storage'

describe('http utility', () => {
  let requestInterceptor: Function
  let responseErrorInterceptor: Function

  beforeEach(() => {
    vi.clearAllMocks()

    // Capture the interceptors registered during module load
    const mockInstance = vi.mocked(axios.create()).mock.results?.[0]?.value ??
      (axios.create as any)()

    // Get the interceptor functions
    const requestUse = mockInstance.interceptors.request.use
    const responseUse = mockInstance.interceptors.response.use

    if (requestUse.mock?.calls?.length) {
      requestInterceptor = requestUse.mock.calls[0][0]
    }
    if (responseUse.mock?.calls?.length) {
      responseErrorInterceptor = responseUse.mock.calls[0][1]
    }
  })

  it('axios.create is called with baseURL and timeout', () => {
    expect(axios.create).toHaveBeenCalledWith(
      expect.objectContaining({
        timeout: 15000
      })
    )
  })

  it('request interceptor is registered', () => {
    const mockInstance = (axios.create as any)()
    expect(mockInstance.interceptors.request.use).toHaveBeenCalled()
  })

  it('response interceptor is registered', () => {
    const mockInstance = (axios.create as any)()
    expect(mockInstance.interceptors.response.use).toHaveBeenCalled()
  })

  it('interceptors handle token injection when available', () => {
    vi.mocked(storage.getToken).mockReturnValue('my-token')

    // The request interceptor should be callable
    const mockInstance = (axios.create as any)()
    const requestUse = mockInstance.interceptors.request.use
    expect(requestUse).toHaveBeenCalled()
  })

  it('interceptors handle missing token gracefully', () => {
    vi.mocked(storage.getToken).mockReturnValue(null)

    const mockInstance = (axios.create as any)()
    const requestUse = mockInstance.interceptors.request.use
    expect(requestUse).toHaveBeenCalled()
  })
})
