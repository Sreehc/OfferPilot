import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const createJobApplicationApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/applications', method: 'POST', data: payload })
export const fetchApplicationBoardApi = () => request<AnyRecord>({ url: '/api/applications/board' })
export const fetchApplicationDetailApi = (applicationId: string) => request<AnyRecord>({ url: '/api/applications/' + applicationId })
export const updateApplicationStatusApi = (applicationId: string, payload: AnyRecord) => request<AnyRecord>({ url: '/api/applications/' + applicationId + '/status', method: 'PUT', data: payload })
export const addApplicationEventApi = (applicationId: string, payload: AnyRecord) => request<AnyRecord>({ url: '/api/applications/' + applicationId + '/events', method: 'POST', data: payload })
export const refreshApplicationAnalysisApi = (applicationId: string) => request<AnyRecord>({ url: '/api/applications/' + applicationId + '/analysis', method: 'POST' })
