import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const fetchAbilityTrendApi = (weeks = 12, categoryIds?: number[]) => request<AnyRecord>({ url: '/api/analytics/trend', params: { weeks, categoryIds } })
export const fetchEfficiencyApi = () => request<AnyRecord>({ url: '/api/analytics/efficiency' })
export const fetchLearningInsightsApi = () => request<AnyRecord>({ url: '/api/analytics/insights' })
export const fetchAnalyticsProfileApi = () => request<AnyRecord>({ url: '/api/analytics/profile' })
export const fetchAnalyticsTopicProfileApi = (topicId: string) => request<AnyRecord>({ url: '/api/analytics/profile/topics/' + topicId })
export const createAnalyticsTopicRetrospectiveApi = (topicId: string) => request<AnyRecord>({ url: '/api/analytics/profile/topics/' + topicId + '/retrospectives', method: 'POST' })
