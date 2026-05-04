import { request } from '@/utils/http'
import type { AbilityTrend, EfficiencyData, LearningInsights } from '@/types/api'

export const fetchAbilityTrendApi = (weeks = 12, categoryIds?: number[]) => {
  const params: Record<string, unknown> = { weeks }
  if (categoryIds && categoryIds.length > 0) {
    params.categoryIds = categoryIds.join(',')
  }
  return request<AbilityTrend>({
    url: '/analytics/trend',
    method: 'get',
    params,
  })
}

export const fetchEfficiencyApi = () => {
  return request<EfficiencyData>({
    url: '/analytics/efficiency',
    method: 'get',
  })
}

export const fetchLearningInsightsApi = () => {
  return request<LearningInsights>({
    url: '/analytics/insights',
    method: 'get',
  })
}
