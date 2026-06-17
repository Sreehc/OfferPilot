import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const generateStudyPlanApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/plan/generate', method: 'POST', data: payload })
export const fetchCurrentStudyPlanApi = () => request<AnyRecord>({ url: '/api/plan/current' })
export const updateStudyPlanTaskStatusApi = (taskId: string, payload: AnyRecord) => request<AnyRecord>({ url: '/api/plan/task/' + taskId + '/status', method: 'POST', data: payload })
export const refreshStudyPlanApi = (planId: string) => request<AnyRecord>({ url: '/api/plan/' + planId + '/refresh', method: 'POST' })
