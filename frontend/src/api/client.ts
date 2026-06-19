import axios, { type AxiosRequestConfig } from 'axios'
import { storage } from '@/utils/storage'
import { getStoredDeviceId } from '@/utils/device'
import type { ApiResponse } from './types'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

http.interceptors.request.use((config) => {
  if (config.baseURL && config.url?.startsWith(config.baseURL)) {
    config.baseURL = ''
  }
  const token = storage.getToken()
  if (token) config.headers.Authorization = 'Bearer ' + token
  const deviceId = getStoredDeviceId()
  if (deviceId) config.headers['X-Device-Id'] = deviceId
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      storage.clearToken()
      storage.clearUser()
      if (window.location.pathname !== '/login') window.location.replace('/login')
    }
    return Promise.reject(error)
  }
)

export async function request<T>(config: AxiosRequestConfig): Promise<ApiResponse<T>> {
  const response = await http.request<ApiResponse<T>>(config)
  const payload = response.data
  if (payload.code !== 200) return Promise.reject(payload)
  return payload
}

export function getErrorMessage(error: unknown, fallback = '操作失败，请稍后重试') {
  if (typeof error === 'object' && error && 'message' in error) {
    return String((error as { message?: unknown }).message || fallback)
  }
  return fallback
}
