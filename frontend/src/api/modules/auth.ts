import { request, http } from '@/api/client'
import type { LoginResponse, UserInfo, AnyRecord } from '@/api/types'
import { getDeviceName, getOrCreateDeviceFingerprint } from '@/utils/device'

export interface LoginPayload { username: string; password: string; captchaKey?: string; captchaCode?: string }
export interface RegisterPayload extends LoginPayload { email?: string; nickname?: string }
export const loginApi = (payload: LoginPayload) => request<LoginResponse>({ url: '/api/auth/login', method: 'POST', data: { ...payload, deviceFingerprint: getOrCreateDeviceFingerprint(), deviceName: getDeviceName() } })
export const registerApi = (payload: RegisterPayload) => request<LoginResponse>({ url: '/api/auth/register', method: 'POST', data: { ...payload, deviceFingerprint: getOrCreateDeviceFingerprint(), deviceName: getDeviceName() } })
export const fetchCurrentUserApi = () => request<UserInfo>({ url: '/api/user/me' })
export const logoutApi = () => request<void>({ url: '/api/auth/logout', method: 'POST' })
export const sendEmailVerificationCodeApi = () => request<void>({ url: '/api/auth/email/send-verification-code', method: 'POST' })
export const verifyEmailCodeApi = (code: string) => request<void>({ url: '/api/auth/email/verify', method: 'POST', data: { code } })
export const forgotPasswordApi = (email: string) => request<void>({ url: '/api/auth/password/forgot', method: 'POST', data: { email } })
export const resetPasswordApi = (payload: AnyRecord) => request<void>({ url: '/api/auth/password/reset', method: 'POST', data: payload })
export const uploadAvatarApi = (file: File) => { const data = new FormData(); data.append('file', file); return request<string>({ url: '/api/user/avatar', method: 'POST', data }) }
export const fetchDevicesApi = () => request<AnyRecord[]>({ url: '/api/auth/devices' })
export const revokeDeviceApi = (deviceId: number) => request<void>({ url: '/api/auth/devices/' + deviceId + '/revoke', method: 'POST' })
export const revokeAllDevicesApi = () => request<void>({ url: '/api/auth/devices/revoke-all', method: 'POST' })
export const fetchLoginLogsApi = (params: AnyRecord = {}) => request<AnyRecord>({ url: '/api/auth/login-logs', params })
export const fetchAdminLoginLogsApi = (params: AnyRecord = {}) => request<AnyRecord>({ url: '/api/admin/login-logs', params })
export const fetchCaptchaApi = () => request<AnyRecord>({ url: '/api/auth/captcha' })
export const fetchTwoFactorStatusApi = () => request<AnyRecord>({ url: '/api/auth/2fa/status' })
export const setupTwoFactorApi = () => request<AnyRecord>({ url: '/api/auth/2fa/setup', method: 'POST' })
export const enableTwoFactorApi = (code: string) => request<void>({ url: '/api/auth/2fa/enable', method: 'POST', data: { code } })
export const disableTwoFactorApi = (code: string) => request<void>({ url: '/api/auth/2fa/disable', method: 'POST', data: { code } })
export const verifyTwoFactorApi = (tempToken: string, code: string) => request<LoginResponse>({ url: '/api/auth/2fa/verify', method: 'POST', data: { tempToken, code } })
export const exportMyDataApi = () => http.get('/api/export/my-data', { responseType: 'blob' })
