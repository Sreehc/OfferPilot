import { request } from '@/utils/http'
import type { KnowledgeDocItem, KnowledgeSearchResult, PageResult } from '@/types/api'

export interface KnowledgeListQuery {
  categoryId?: number
  keyword?: string
  status?: KnowledgeDocItem['status']
  pageNum?: number
  pageSize?: number
}

export interface KnowledgeImportPayload {
  seedKey: string
  categoryId?: number
  forceRebuild?: boolean
}

export const fetchKnowledgeDocsApi = (params?: KnowledgeListQuery) => {
  return request<PageResult<KnowledgeDocItem>>({ url: '/knowledge/list', method: 'get', params })
}

export const searchKnowledgeApi = (query: string) => {
  return request<KnowledgeSearchResult>({ url: '/knowledge/search', method: 'post', data: { query } })
}

export const importKnowledgeSeedApi = (payload: KnowledgeImportPayload) => {
  return request<KnowledgeDocItem>({ url: '/admin/knowledge/import', method: 'post', data: payload })
}

export const rechunkKnowledgeDocApi = (docId: number) => {
  return request<KnowledgeDocItem>({ url: `/admin/knowledge/rechunk/${docId}`, method: 'post' })
}

export const reindexKnowledgeDocApi = (docId: number) => {
  return request<KnowledgeDocItem>({ url: `/admin/knowledge/reindex/${docId}`, method: 'post' })
}
