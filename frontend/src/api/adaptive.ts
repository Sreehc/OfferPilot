import { request } from '@/utils/http'
import type { AbilityProfile, RecommendInterview, RecommendQuestion } from '@/types/api'

export const fetchAbilityProfileApi = () => {
  return request<AbilityProfile>({ url: '/recommend/profile', method: 'get' })
}

export const fetchRecommendQuestionsApi = (limit = 10) => {
  return request<RecommendQuestion[]>({ url: '/recommend/questions', method: 'get', params: { limit } })
}

export const fetchRecommendInterviewApi = () => {
  return request<RecommendInterview>({ url: '/recommend/interview', method: 'get' })
}
