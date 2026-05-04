import { request } from '@/utils/http'
import type { PageResult, UserInfo } from '@/types/api'

// ─── User management ───

export interface AdminUserUpdatePayload {
  nickname?: string
  role?: string
  status?: number
}

export interface AdminUserDetail {
  id: number
  username: string
  nickname: string
  avatar?: string
  email?: string
  role: string
  status: number
  createTime: string
  lastLoginTime?: string
  interviewCount: number
  wrongCount: number
  reviewCount: number
  communityQuestions: number
  communityAnswers: number
}

export const fetchAdminUsersApi = (params: { keyword?: string; role?: string; pageNum?: number; pageSize?: number }) => {
  return request<PageResult<UserInfo>>({ url: '/admin/users', method: 'get', params })
}

export const updateAdminUserApi = (id: number, payload: AdminUserUpdatePayload) => {
  return request<null>({ url: `/admin/users/${id}`, method: 'put', data: payload })
}

export const banUserApi = (id: number) => {
  return request<null>({ url: `/admin/users/${id}/ban`, method: 'post' })
}

export const unbanUserApi = (id: number) => {
  return request<null>({ url: `/admin/users/${id}/unban`, method: 'post' })
}

export const fetchUserDetailApi = (id: number) => {
  return request<AdminUserDetail>({ url: `/admin/users/${id}/detail`, method: 'get' })
}

// ─── Content moderation ───

export interface AdminContentReviewItem {
  id: number
  type: string
  userId: number
  username?: string
  title: string
  content: string
  createTime: string
}

export const fetchPendingContentApi = (params: { pageNum?: number; pageSize?: number }) => {
  return request<PageResult<AdminContentReviewItem>>({ url: '/admin/community/pending', method: 'get', params })
}

export const approveContentApi = (id: number) => {
  return request<null>({ url: `/admin/community/${id}/approve`, method: 'post' })
}

export const rejectContentApi = (id: number, reason?: string) => {
  return request<null>({ url: `/admin/community/${id}/reject`, method: 'post', params: { reason } })
}

// ─── System overview ───

export interface AdminOverview {
  totalUsers: number
  todayActive: number
  todayNew: number
  totalInterviews: number
  totalReviews: number
}

export interface AdminTrendItem {
  date: string
  newUsers: number
  activeUsers: number
}

export const fetchAdminOverviewApi = () => {
  return request<AdminOverview>({ url: '/admin/overview', method: 'get' })
}

export const fetchAdminTrendApi = () => {
  return request<AdminTrendItem[]>({ url: '/admin/overview/trend', method: 'get' })
}

// ─── Data export ───

export const exportQuestionsApi = () => {
  return request<null>({ url: '/admin/export/questions', method: 'get', responseType: 'blob' as any })
}

export const exportUsersApi = () => {
  return request<null>({ url: '/admin/export/users', method: 'get', responseType: 'blob' as any })
}

// ─── Data import ───

export interface ImportResult {
  successCount: number
  failCount: number
  errors: string[]
}

export const importQuestionsApi = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request<ImportResult>({ url: '/admin/import/questions', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
}
