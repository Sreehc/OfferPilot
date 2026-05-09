import { request } from '@/utils/http'
import type { ReviewContentType, ReviewStats, ReviewTodayData } from '@/types/api'

export interface ReviewRatePayload {
  contentType?: ReviewContentType | string
  rating: 1 | 2 | 3 | 4
  responseTimeMs?: number
}

export const fetchReviewTodayApi = (contentType: ReviewContentType = 'all') => {
  return request<ReviewTodayData>({
    url: '/review/today',
    method: 'get',
    params: { contentType }
  })
}

export const submitReviewRateApi = (reviewItemId: string, payload: ReviewRatePayload) => {
  return request<ReviewTodayData>({ url: `/review/${reviewItemId}/rate`, method: 'post', data: payload })
}

export const fetchReviewStatsApi = () => {
  return request<ReviewStats>({ url: '/review/stats', method: 'get' })
}
