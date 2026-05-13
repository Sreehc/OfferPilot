import { request } from '@/utils/http'
import type { PageResult } from '@/types/api'

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

export interface AdminUserListItem {
  id: number
  username: string
  nickname: string
  avatar?: string
  role: string
  status?: number
  createTime?: string
  lastLoginTime?: string
}

export const fetchAdminUsersApi = (params: { keyword?: string; role?: string; pageNum?: number; pageSize?: number }) => {
  return request<PageResult<AdminUserListItem>>({ url: '/admin/users', method: 'get', params })
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

export interface AdminAiLogItem {
  id: string
  userId?: string
  provider: string
  model: string
  callType: string
  scene: string
  inputTokens?: number
  outputTokens?: number
  latencyMs?: number
  success: number
  errorMessage?: string
  createTime: string
}

export interface AdminAiLogSummary {
  totalCalls: number
  successCalls: number
  failedCalls: number
  avgLatencyMs: number
  chatCalls: number
  embeddingCalls: number
}

export interface AdminSystemConfigItem {
  configGroup: string
  configKey: string
  displayName: string
  description: string
  valueType: string
  configValue: string
  enabled: boolean
  runtimeDefault?: string
}

export interface AdminSystemConfigUpdatePayload {
  configValue?: string
  enabled: boolean
}

export interface AdminInterviewGovernanceItem {
  sessionId: string
  userId: string
  direction: string
  jobRole?: string
  experienceLevel?: string
  techStack?: string
  mode?: string
  status: string
  totalScore?: number
  questionCount: number
  durationMinutes?: number
  includeResumeProject: boolean
  lowScoreCount: number
  startTime?: string
  endTime?: string
}

export interface AdminInterviewGovernanceSummary {
  totalSessions: number
  finishedSessions: number
  voiceSessions: number
  resumeProjectSessions: number
  averageScore: number
}

export const fetchAdminOverviewApi = () => {
  return request<AdminOverview>({ url: '/admin/overview', method: 'get' })
}

export const fetchAdminTrendApi = () => {
  return request<AdminTrendItem[]>({ url: '/admin/overview/trend', method: 'get' })
}

export const fetchAdminAiLogSummaryApi = () => {
  return request<AdminAiLogSummary>({ url: '/admin/ai-logs/summary', method: 'get' })
}

export const fetchAdminAiLogsApi = (params: {
  scene?: string
  callType?: string
  success?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}) => {
  return request<PageResult<AdminAiLogItem>>({ url: '/admin/ai-logs', method: 'get', params })
}

export const fetchAdminSystemConfigsApi = () => {
  return request<AdminSystemConfigItem[]>({ url: '/admin/system-config', method: 'get' })
}

export const updateAdminSystemConfigApi = (configKey: string, payload: AdminSystemConfigUpdatePayload) => {
  return request<AdminSystemConfigItem>({
    url: `/admin/system-config/${encodeURIComponent(configKey)}`,
    method: 'put',
    data: payload
  })
}

export const fetchAdminInterviewGovernanceSummaryApi = () => {
  return request<AdminInterviewGovernanceSummary>({ url: '/admin/interviews/summary', method: 'get' })
}

export const fetchAdminInterviewGovernanceApi = (params: {
  status?: string
  mode?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}) => {
  return request<PageResult<AdminInterviewGovernanceItem>>({ url: '/admin/interviews', method: 'get', params })
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
