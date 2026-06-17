import { request, http } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const fetchWrongListApi = (pageNum = 1, pageSize = 20) => request<AnyRecord>({ url: '/api/wrong/list', params: { pageNum, pageSize } })
export const fetchWrongDetailApi = (id: number) => request<AnyRecord>({ url: '/api/wrong/' + id })
export const updateMasteryApi = (id: number, payload: AnyRecord) => request<AnyRecord>({ url: '/api/wrong/mastery/' + id, method: 'PUT', data: payload })
export const deleteWrongApi = (id: number) => request<void>({ url: '/api/wrong/delete/' + id, method: 'DELETE' })
export const exportWrongMarkdownApi = () => http.get('/api/wrong/export', { responseType: 'blob' })
