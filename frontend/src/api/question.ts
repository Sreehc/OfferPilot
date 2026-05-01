import { request } from '@/utils/http'
import type { PageResult, QuestionItem } from '@/types/api'

export interface QuestionQuery {
  categoryId?: number
  difficulty?: QuestionItem['difficulty']
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export type QuestionPayload = Omit<QuestionItem, 'id' | 'categoryName' | 'createTime' | 'updateTime'> & {
  id?: number
}

export const fetchQuestionsApi = (params?: QuestionQuery) => {
  return request<PageResult<QuestionItem>>({ url: '/question/list', method: 'get', params })
}

export const fetchQuestionDetailApi = (id: number) => {
  return request<QuestionItem>({ url: `/question/${id}`, method: 'get' })
}

export const addQuestionApi = (payload: QuestionPayload) => {
  return request<QuestionItem>({ url: '/admin/question/add', method: 'post', data: payload })
}

export const updateQuestionApi = (payload: QuestionPayload) => {
  return request<QuestionItem>({ url: '/admin/question/update', method: 'put', data: payload })
}

export const deleteQuestionApi = (id: number) => {
  return request<null>({ url: `/admin/question/delete/${id}`, method: 'delete' })
}
