import { request, http } from '@/api/client'
import type { AnyRecord } from '@/api/types'

export const fetchAdminUsersApi = (params?: AnyRecord) => request<AnyRecord>({ url: '/api/admin/users', params })
export const updateAdminUserApi = (id: number, payload: AnyRecord) => request<AnyRecord>({ url: '/api/admin/users/' + id, method: 'PUT', data: payload })
export const banUserApi = (id: number) => request<void>({ url: '/api/admin/users/' + id + '/ban', method: 'POST' })
export const unbanUserApi = (id: number) => request<void>({ url: '/api/admin/users/' + id + '/unban', method: 'POST' })
export const fetchUserDetailApi = (id: number) => request<AnyRecord>({ url: '/api/admin/users/' + id + '/detail' })

export const fetchPendingContentApi = (params?: AnyRecord) => request<AnyRecord>({ url: '/api/admin/community/pending', params })
export const approveContentApi = (id: number) => request<void>({ url: '/api/admin/community/' + id + '/approve', method: 'POST' })
export const rejectContentApi = (id: number, reason?: string) => request<void>({ url: '/api/admin/community/' + id + '/reject', method: 'POST', params: { reason } })

export const fetchAdminOverviewApi = () => request<AnyRecord>({ url: '/api/admin/overview' })
export const fetchAdminTrendApi = () => request<AnyRecord>({ url: '/api/admin/overview/trend' })
export const fetchAdminAiLogSummaryApi = () => request<AnyRecord>({ url: '/api/admin/ai-logs/summary' })
export const fetchAdminAiLogsApi = (params?: AnyRecord) => request<AnyRecord>({ url: '/api/admin/ai-logs', params })

export const fetchAdminSystemConfigsApi = () => request<AnyRecord>({ url: '/api/admin/system-config' })
export const updateAdminSystemConfigApi = (configKey: string, payload: AnyRecord) => request<AnyRecord>({ url: '/api/admin/system-config/' + encodeURIComponent(configKey), method: 'PUT', data: payload })

export const fetchAdminInterviewGovernanceSummaryApi = () => request<AnyRecord>({ url: '/api/admin/interviews/summary' })
export const fetchAdminInterviewGovernanceApi = (params?: AnyRecord) => request<AnyRecord>({ url: '/api/admin/interviews', params })
export const fetchAdminRuntimeGovernanceSummaryApi = () => request<AnyRecord>({ url: '/api/admin/runtime-governance/summary' })

export const addAdminQuestionApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/admin/question/add', method: 'POST', data: payload })
export const updateAdminQuestionApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/admin/question/update', method: 'PUT', data: payload })
export const deleteAdminQuestionApi = (id: number) => request<void>({ url: '/api/admin/question/delete/' + id, method: 'DELETE' })

export const addAdminCategoryApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/admin/category/add', method: 'POST', data: payload })
export const updateAdminCategoryApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/admin/category/update', method: 'PUT', data: payload })
export const deleteAdminCategoryApi = (id: number) => request<void>({ url: '/api/admin/category/delete/' + id, method: 'DELETE' })

export const importKnowledgeSeedApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/admin/knowledge/import', method: 'POST', data: payload })
export const rechunkKnowledgeDocApi = (docId: number) => request<AnyRecord>({ url: '/api/admin/knowledge/rechunk/' + docId, method: 'POST' })
export const reindexKnowledgeDocApi = (docId: number) => request<AnyRecord>({ url: '/api/admin/knowledge/reindex/' + docId, method: 'POST' })
export const batchRechunkKnowledgeDocsApi = (docIds: number[]) => request<AnyRecord>({ url: '/api/admin/knowledge/rechunk/batch', method: 'POST', data: { docIds } })
export const batchReindexKnowledgeDocsApi = (docIds: number[]) => request<AnyRecord>({ url: '/api/admin/knowledge/reindex/batch', method: 'POST', data: { docIds } })

export const exportQuestionsApi = () => http.get('/api/admin/export/questions', { responseType: 'blob' })
export const exportUsersApi = () => http.get('/api/admin/export/users', { responseType: 'blob' })
export const importQuestionsApi = (file: File) => {
  const data = new FormData()
  data.append('file', file)
  return request<AnyRecord>({ url: '/api/admin/import/questions', method: 'POST', data })
}
