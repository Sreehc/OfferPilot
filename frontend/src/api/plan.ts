import { request } from '@/utils/http'
import type { StudyPlanItem, StudyPlanTaskItem } from '@/types/api'

export interface GeneratePlanPayload {
  direction: string
  days?: number
  dailyMinutes?: number
}

export interface TaskStatusPayload {
  status: 'todo' | 'done'
}

export const generatePlanApi = (payload: GeneratePlanPayload) => {
  return request<StudyPlanItem>({ url: '/plan/generate', method: 'post', data: payload })
}

export const fetchCurrentPlanApi = () => {
  return request<StudyPlanItem | null>({ url: '/plan/current', method: 'get' })
}

export const fetchPlanTasksApi = (planId: number) => {
  return request<StudyPlanTaskItem[]>({ url: `/plan/${planId}/tasks`, method: 'get' })
}

export const updateTaskStatusApi = (taskId: number, payload: TaskStatusPayload) => {
  return request<null>({ url: `/plan/task/${taskId}/status`, method: 'put', data: payload })
}

export const adjustPlanApi = (planId: number) => {
  return request<StudyPlanItem>({ url: `/plan/${planId}/adjust`, method: 'post' })
}

export const fetchPlanHealthApi = (planId: number) => {
  return request<number>({ url: `/plan/${planId}/health`, method: 'get' })
}

export const fetchPlanHistoryApi = () => {
  return request<StudyPlanItem[]>({ url: '/plan/history', method: 'get' })
}
