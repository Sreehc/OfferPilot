import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const fetchQuestionsApi = (params?: AnyRecord) => request<AnyRecord>({ url: '/api/question/list', params })
export const fetchQuestionDetailApi = (id: number) => request<AnyRecord>({ url: '/api/question/' + id })
export const addQuestionApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/admin/question/add', method: 'POST', data: payload })
export const updateQuestionApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/admin/question/update', method: 'PUT', data: payload })
export const deleteQuestionApi = (id: number) => request<void>({ url: '/api/admin/question/delete/' + id, method: 'DELETE' })
