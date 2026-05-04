import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { storage } from './storage'
import { getStoredDeviceId } from './device'
import type { ApiResponse } from '@/types/api'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = storage.getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const deviceId = getStoredDeviceId()
  if (deviceId) {
    config.headers['X-Device-Id'] = deviceId
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      storage.clearToken()
      storage.clearUser()
      if (window.location.pathname !== '/login') {
        window.location.replace('/login')
      }
    }
    ElMessage.error(error.response?.data?.message || '网络异常')
    return Promise.reject(error)
  }
)

export default http

export async function request<T>(config: AxiosRequestConfig): Promise<ApiResponse<T>> {
  const response = await http.request<ApiResponse<T>>(config)
  const payload = response.data
  if (payload.code !== 200) {
    ElMessage.error(payload.message || '请求失败')
    return Promise.reject(payload)
  }
  return payload
}
