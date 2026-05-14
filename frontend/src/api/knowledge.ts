import { request } from '@/utils/http'
import type {
  KnowledgeBusinessType,
  KnowledgeDocItem,
  KnowledgeDocStatus,
  KnowledgeIndexStatus,
  KnowledgeLibraryScope,
  KnowledgeParseStatus,
  KnowledgeSearchResult,
  PageResult
} from '@/types/api'

export interface KnowledgeListQuery {
  categoryId?: number
  keyword?: string
  libraryScope?: KnowledgeLibraryScope
  businessType?: KnowledgeBusinessType
  fileType?: string
  parseStatus?: KnowledgeParseStatus
  indexStatus?: KnowledgeIndexStatus
  status?: KnowledgeDocStatus
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

export const uploadKnowledgeDocApi = (file: File, categoryId?: number) => {
  const formData = new FormData()
  formData.append('file', file)
  if (categoryId != null) {
    formData.append('categoryId', String(categoryId))
  }
  return request<KnowledgeDocItem>({
    url: '/knowledge/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const fetchMyKnowledgeDocsApi = (params?: KnowledgeListQuery) => {
  return request<PageResult<KnowledgeDocItem>>({ url: '/knowledge/my', method: 'get', params })
}

export const deleteKnowledgeDocApi = (docId: number) => {
  return request<void>({ url: `/knowledge/${docId}`, method: 'delete' })
}
