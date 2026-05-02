import { request } from '@/utils/http'
import type { ApiResponse, LoginResponse, UserInfo } from '@/types/api'

export interface LoginPayload {
  username: string
  password: string
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
