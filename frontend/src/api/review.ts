import { request } from '@/utils/http'
import type { ReviewTodayItem, ReviewStats } from '@/types/api'

export interface ReviewRatePayload {
  rating: 1 | 2 | 3 | 4
  responseTimeMs?: number
}

export const fetchReviewTodayApi = () => {
  return request<ReviewTodayItem[]>({ url: '/review/today', method: 'get' })
}

export const submitReviewRateApi = (wrongQuestionId: number, payload: ReviewRatePayload) => {
  return request<null>({ url: `/review/${wrongQuestionId}/rate`, method: 'post', data: payload })
}

export const fetchReviewStatsApi = () => {
  return request<ReviewStats>({ url: '/review/stats', method: 'get' })
}
