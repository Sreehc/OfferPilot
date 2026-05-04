import { request } from '@/utils/http'
import type { PageResult } from '@/types/api'
import type {
  CommunityQuestion,
  CommunityQuestionDetail,
  LeaderboardEntry,
} from '@/types/api'

export interface CommunityQuestionUpsertPayload {
  id?: number
  title: string
  content: string
  categoryId?: number
}

export interface CommunityAnswerPayload {
  questionId: number
  content: string
}

export interface CommunityVotePayload {
  targetType: 'question' | 'answer'
  targetId: number
}

export const fetchCommunityQuestionsApi = (
  page = 1,
  size = 20,
  sort: 'new' | 'hot' = 'new',
  categoryId?: number,
  keyword?: string
) => {
  const params: Record<string, string | number> = { page, size, sort }
  if (categoryId) params.categoryId = categoryId
  if (keyword) params.keyword = keyword
  return request<PageResult<CommunityQuestion>>({ url: '/community/questions', method: 'get', params })
}

export const fetchCommunityQuestionDetailApi = (id: number) => {
  return request<CommunityQuestionDetail>({ url: `/community/questions/${id}`, method: 'get' })
}

export const createCommunityQuestionApi = (payload: CommunityQuestionUpsertPayload) => {
  return request<number>({ url: '/community/questions', method: 'post', data: payload })
}

export const updateCommunityQuestionApi = (payload: CommunityQuestionUpsertPayload) => {
  return request<void>({ url: '/community/questions', method: 'put', data: payload })
}

export const deleteCommunityQuestionApi = (id: number) => {
  return request<void>({ url: `/community/questions/${id}`, method: 'delete' })
}

export const submitCommunityAnswerApi = (payload: CommunityAnswerPayload) => {
  return request<number>({ url: '/community/answers', method: 'post', data: payload })
}

export const deleteCommunityAnswerApi = (id: number) => {
  return request<void>({ url: `/community/answers/${id}`, method: 'delete' })
}

export const acceptCommunityAnswerApi = (questionId: number, answerId: number) => {
  return request<void>({
    url: `/community/answers/${answerId}/accept`,
    method: 'post',
    params: { questionId },
  })
}

export const voteCommunityApi = (payload: CommunityVotePayload) => {
  return request<void>({ url: '/community/vote', method: 'post', data: payload })
}

export const fetchLeaderboardApi = (limit = 50) => {
  return request<LeaderboardEntry[]>({ url: '/community/leaderboard', method: 'get', params: { limit } })
}
