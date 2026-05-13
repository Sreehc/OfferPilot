import { request } from '@/utils/http'
import type { StudyPlan } from '@/types/api'

export interface StudyPlanGeneratePayload {
  durationDays: number
  focusDirection?: string
  targetRole?: string
  techStack?: string
}

export interface StudyPlanTaskStatusPayload {
  status: 'pending' | 'completed'
}

export const generateStudyPlanApi = (payload: StudyPlanGeneratePayload) => {
  return request<StudyPlan>({ url: '/plan/generate', method: 'post', data: payload })
}

export const fetchCurrentStudyPlanApi = () => {
  return request<StudyPlan | null>({ url: '/plan/current', method: 'get' })
}

export const updateStudyPlanTaskStatusApi = (taskId: string, payload: StudyPlanTaskStatusPayload) => {
  return request<StudyPlan>({ url: `/plan/task/${taskId}/status`, method: 'post', data: payload })
}

export const refreshStudyPlanApi = (planId: string) => {
  return request<StudyPlan>({ url: `/plan/${planId}/refresh`, method: 'post' })
}
