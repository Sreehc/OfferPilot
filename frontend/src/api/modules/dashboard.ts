import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const fetchDashboardOverviewApi = () => request<AnyRecord>({ url: '/api/dashboard/overview' })
