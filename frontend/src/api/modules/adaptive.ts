import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const fetchAbilityProfileApi = () => request<AnyRecord>({ url: '/api/recommend/profile' })
export const fetchRecommendQuestionsApi = (limit = 10) => request<AnyRecord>({ url: '/api/recommend/questions', params: { limit } })
export const fetchRecommendInterviewApi = () => request<AnyRecord>({ url: '/api/recommend/interview' })
