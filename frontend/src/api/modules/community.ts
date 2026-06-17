import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const fetchCommunityQuestionsApi = (params?: AnyRecord) => request<AnyRecord>({ url: '/api/community/questions', params })
export const fetchCommunityQuestionDetailApi = (id: number) => request<AnyRecord>({ url: '/api/community/questions/' + id })
export const createCommunityQuestionApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/community/questions', method: 'POST', data: payload })
export const updateCommunityQuestionApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/community/questions', method: 'PUT', data: payload })
export const deleteCommunityQuestionApi = (id: number) => request<void>({ url: '/api/community/questions/' + id, method: 'DELETE' })
export const submitCommunityAnswerApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/community/answers', method: 'POST', data: payload })
export const deleteCommunityAnswerApi = (id: number) => request<void>({ url: '/api/community/answers/' + id, method: 'DELETE' })
export const acceptCommunityAnswerApi = (questionId: number, answerId: number) => request<AnyRecord>({ url: '/api/community/answers/' + answerId + '/accept', method: 'POST', params: { questionId } })
export const voteCommunityApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/community/vote', method: 'POST', data: payload })
export const fetchLeaderboardApi = (limit = 50) => request<AnyRecord>({ url: '/api/community/leaderboard', params: { limit } })
