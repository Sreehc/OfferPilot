import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const fetchReviewTodayApi = (contentType = 'all') => request<AnyRecord>({ url: '/api/review/today', params: { contentType } })
export const submitReviewRateApi = (reviewItemId: string, payload: AnyRecord) => request<AnyRecord>({ url: '/api/review/' + reviewItemId + '/rate', method: 'POST', data: payload })
export const fetchReviewStatsApi = () => request<AnyRecord>({ url: '/api/review/stats' })
