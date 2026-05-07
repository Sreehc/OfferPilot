import { request } from '@/utils/http'
import type { KnowledgeCardTask } from '@/types/api'

export interface CardTaskCreatePayload {
  docId: number
  days: number
}

export interface CardRatePayload {
  cardId: number
  rating: 1 | 2 | 3 | 4
  responseTimeMs?: number
}

export const createCardTaskApi = (payload: CardTaskCreatePayload) => {
  return request<KnowledgeCardTask>({ url: '/cards/task', method: 'post', data: payload })
}

export const fetchActiveCardTaskApi = () => {
  return request<KnowledgeCardTask | null>({ url: '/cards/active', method: 'get' })
}

export const fetchCardTaskApi = (taskId: number) => {
  return request<KnowledgeCardTask>({ url: `/cards/task/${taskId}`, method: 'get' })
}

export const startCardTaskApi = (taskId: number) => {
  return request<KnowledgeCardTask>({ url: `/cards/task/${taskId}/start`, method: 'post' })
}

export const submitCardRateApi = (taskId: number, payload: CardRatePayload) => {
  return request<KnowledgeCardTask>({ url: `/cards/task/${taskId}/rate`, method: 'post', data: payload })
}

export const restartCardTaskApi = (taskId: number) => {
  return request<KnowledgeCardTask>({ url: `/cards/task/${taskId}/restart`, method: 'post' })
}
