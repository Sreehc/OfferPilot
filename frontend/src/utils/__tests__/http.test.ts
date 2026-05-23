import { beforeEach, describe, expect, it, vi } from 'vitest'

const requestUse = vi.fn()
const responseUse = vi.fn()
const requestMock = vi.fn()
const replaceMock = vi.fn()
const getTokenMock = vi.fn()
const clearTokenMock = vi.fn()
const clearUserMock = vi.fn()
const getStoredDeviceIdMock = vi.fn()
const errorMock = vi.fn()

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      interceptors: {
        request: { use: requestUse },
        response: { use: responseUse }
      },
      request: requestMock
    }))
  }
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: errorMock
  }
}))

vi.mock('@/utils/storage', () => ({
  storage: {
    getToken: getTokenMock,
    clearToken: clearTokenMock,
    clearUser: clearUserMock
  }
}))

vi.mock('@/utils/device', () => ({
  getStoredDeviceId: getStoredDeviceIdMock
}))

describe('http utility', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: {
        pathname: '/dashboard',
        replace: replaceMock
      }
    })
  })

  it('registers request and response interceptors when module loads', async () => {
    await import('../http')

    expect(requestUse).toHaveBeenCalledTimes(1)
    expect(responseUse).toHaveBeenCalledTimes(1)
  })

  it('injects auth token and device id into request headers', async () => {
    getTokenMock.mockReturnValue('token-123')
    getStoredDeviceIdMock.mockReturnValue('device-456')
    await import('../http')

    const onRequest = requestUse.mock.calls[0]?.[0]
    const config = onRequest({ headers: {} })

    expect(config.headers.Authorization).toBe('Bearer token-123')
    expect(config.headers['X-Device-Id']).toBe('device-456')
  })

  it('handles business errors from request helper', async () => {
    requestMock.mockResolvedValue({
      data: { code: 500, message: 'bad request' }
    })
    const { request } = await import('../http')

    await expect(request({ url: '/demo', method: 'GET' })).rejects.toEqual({
      code: 500,
      message: 'bad request'
    })
  })

  it('clears auth and redirects on 401 responses', async () => {
    await import('../http')

    const onResponseError = responseUse.mock.calls[0]?.[1]
    await expect(onResponseError({ response: { status: 401 } })).rejects.toEqual({ response: { status: 401 } })

    expect(clearTokenMock).toHaveBeenCalledTimes(1)
    expect(clearUserMock).toHaveBeenCalledTimes(1)
    expect(replaceMock).toHaveBeenCalledWith('/login')
  })

  it('shows a network error message when no response is present', async () => {
    await import('../http')

    const onResponseError = responseUse.mock.calls[0]?.[1]
    await expect(onResponseError({})).rejects.toEqual({})

    expect(errorMock).toHaveBeenCalledWith('网络异常，请检查网络连接')
  })
})
