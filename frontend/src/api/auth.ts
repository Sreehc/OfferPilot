import { request } from '@/utils/http'
import type {
  AuthDelivery,
  LoginDeviceItem,
  LoginLogItem,
  LoginResponse,
  OAuthProviderInfo,
  PageResult,
  TwoFactorEnable,
  TwoFactorSetup,
  TwoFactorStatus,
  UserInfo
} from '@/types/api'

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
  email: string
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

export const sendEmailVerificationCodeApi = () => {
  return request<AuthDelivery>({ url: '/auth/email/send-verification-code', method: 'post' })
}

export const verifyEmailCodeApi = (code: string) => {
  return request<UserInfo>({ url: '/auth/email/verify', method: 'post', data: { code } })
}

export const forgotPasswordApi = (email: string) => {
  return request<AuthDelivery>({ url: '/auth/password/forgot', method: 'post', data: { email } })
}

export const resetPasswordApi = (payload: { email: string; code: string; newPassword: string }) => {
  return request<null>({ url: '/auth/password/reset', method: 'post', data: payload })
}

export const fetchOAuthProvidersApi = () => {
  return request<OAuthProviderInfo[]>({ url: '/auth/oauth/providers', method: 'get' })
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

// ─── Two-Factor Auth ───

export const fetchTwoFactorStatusApi = () => {
  return request<TwoFactorStatus>({ url: '/auth/2fa/status', method: 'get' })
}

export const setupTwoFactorApi = () => {
  return request<TwoFactorSetup>({ url: '/auth/2fa/setup', method: 'post' })
}

export const enableTwoFactorApi = (code: string) => {
  return request<TwoFactorEnable>({ url: '/auth/2fa/enable', method: 'post', data: { code } })
}

export const disableTwoFactorApi = (code: string) => {
  return request<null>({ url: '/auth/2fa/disable', method: 'post', data: { code } })
}

export const verifyTwoFactorApi = (tempToken: string, code: string) => {
  return request<LoginResponse>({ url: '/auth/2fa/verify', method: 'post', data: { tempToken, code } })
}

// ─── Personal data export ───

export const exportMyDataApi = () => {
  return request<null>({ url: '/export/my-data', method: 'get', responseType: 'blob' as any })
}
