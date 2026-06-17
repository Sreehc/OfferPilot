import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const fetchKnowledgeDocsApi = (params?: AnyRecord) => request<AnyRecord>({ url: '/api/knowledge/list', params })
export const fetchKnowledgeDocDetailApi = (docId: number) => request<AnyRecord>({ url: '/api/knowledge/' + docId })
export const searchKnowledgeApi = (query: string) => request<AnyRecord>({ url: '/api/knowledge/search', method: 'POST', data: { query } })
export const importKnowledgeSeedApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/admin/knowledge/import', method: 'POST', data: payload })
export const rechunkKnowledgeDocApi = (docId: number) => request<AnyRecord>({ url: '/api/admin/knowledge/rechunk/' + docId, method: 'POST' })
export const reindexKnowledgeDocApi = (docId: number) => request<AnyRecord>({ url: '/api/admin/knowledge/reindex/' + docId, method: 'POST' })
export const batchRechunkKnowledgeDocsApi = (docIds: number[]) => request<AnyRecord>({ url: '/api/admin/knowledge/rechunk/batch', method: 'POST', data: { docIds } })
export const batchReindexKnowledgeDocsApi = (docIds: number[]) => request<AnyRecord>({ url: '/api/admin/knowledge/reindex/batch', method: 'POST', data: { docIds } })
export const uploadKnowledgeDocApi = (file: File, categoryId?: number) => { const data = new FormData(); data.append('file', file); if (categoryId) data.append('categoryId', String(categoryId)); return request<AnyRecord>({ url: '/api/knowledge/upload', method: 'POST', data }) }
export const fetchMyKnowledgeDocsApi = (params?: AnyRecord) => request<AnyRecord>({ url: '/api/knowledge/my', params })
export const deleteKnowledgeDocApi = (docId: number) => request<void>({ url: '/api/knowledge/' + docId, method: 'DELETE' })
