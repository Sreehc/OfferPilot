import { request } from '@/utils/http'
import type { LoginDeviceItem, LoginLogItem, LoginResponse, PageResult, UserInfo } from '@/types/api'

export interface LoginPayload {
  username: string
  password: string
  deviceFingerprint?: string
  deviceName?: string
  captchaCode?: string
  captchaKey?: string
}

export interface RegisterPayload extends LoginPayload {
  nickname: string
}

export const loginApi = (payload: LoginPayload) => {
  return request<LoginResponse>({ url: '/auth/login', method: 'post', data: payload })
}

export const registerApi = (payload: RegisterPayload) => {
  return request<LoginResponse>({ url: '/auth/register', method: 'post', data: payload })
}

export const fetchCurrentUserApi = () => {
  return request<UserInfo>({ url: '/user/me', method: 'get' })
}

export const logoutApi = () => {
  return request<null>({ url: '/auth/logout', method: 'post' })
}

export const uploadAvatarApi = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request<string>({
    url: '/user/avatar',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ─── Device management ───

export const fetchDevicesApi = () => {
  return request<LoginDeviceItem[]>({ url: '/auth/devices', method: 'get' })
}

export const revokeDeviceApi = (deviceId: number) => {
  return request<null>({ url: `/auth/devices/${deviceId}/revoke`, method: 'post' })
}

export const revokeAllDevicesApi = () => {
  return request<null>({ url: '/auth/devices/revoke-all', method: 'post' })
}

// ─── Login logs ───

export const fetchLoginLogsApi = (params: { pageNum?: number; pageSize?: number }) => {
  return request<PageResult<LoginLogItem>>({ url: '/auth/login-logs', method: 'get', params })
}

// ─── Admin login logs ───

export const fetchAdminLoginLogsApi = (params: { keyword?: string; pageNum?: number; pageSize?: number }) => {
  return request<PageResult<LoginLogItem>>({ url: '/admin/login-logs', method: 'get', params })
}

// ─── Captcha ───

export const fetchCaptchaApi = () => {
  return request<{ key: string; image: string }>({ url: '/auth/captcha', method: 'get' })
}
