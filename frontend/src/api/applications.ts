import { request } from '@/utils/http'
import type { JobApplicationDetail, JobApplicationItem } from '@/types/api'

export interface JobApplicationCreatePayload {
  company: string
  jobTitle: string
  city?: string
  source?: string
  jdText: string
  resumeFileId?: string
  applyDate?: string
}

export interface JobApplicationStatusPayload {
  status: string
  note?: string
  nextStepDate?: string
}

export interface JobApplicationEventPayload {
  eventType: string
  title: string
  content?: string
  eventTime?: string
  result?: string
  interviewRound?: number
  interviewer?: string
  feedbackTags?: string[]
}

export const createJobApplicationApi = (payload: JobApplicationCreatePayload) => {
  return request<JobApplicationDetail>({ url: '/applications', method: 'post', data: payload })
}

export const fetchApplicationBoardApi = () => {
  return request<JobApplicationItem[]>({ url: '/applications/board', method: 'get' })
}

export const fetchApplicationDetailApi = (applicationId: string) => {
  return request<JobApplicationDetail>({ url: `/applications/${applicationId}`, method: 'get' })
}

export const updateApplicationStatusApi = (applicationId: string, payload: JobApplicationStatusPayload) => {
  return request<JobApplicationDetail>({ url: `/applications/${applicationId}/status`, method: 'put', data: payload })
}

export const addApplicationEventApi = (applicationId: string, payload: JobApplicationEventPayload) => {
  return request<JobApplicationDetail>({ url: `/applications/${applicationId}/events`, method: 'post', data: payload })
}

export const refreshApplicationAnalysisApi = (applicationId: string) => {
  return request<JobApplicationDetail>({ url: `/applications/${applicationId}/analysis`, method: 'post' })
}
