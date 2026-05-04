import { request } from '@/utils/http'
import type { InterviewAnswerResult, InterviewCurrentQuestion, InterviewDetail, InterviewHistoryItem, VoiceSubmitResult } from '@/types/api'
import type { PageResult } from '@/types/api'

export interface InterviewStartPayload {
  direction: string
  questionCount?: number
  reanswerQuestionId?: number
}

export interface InterviewAnswerPayload {
  sessionId: number
  questionId: number
  answer: string
}

export interface VoiceStartPayload {
  direction: string
  questionCount?: number
  reanswerQuestionId?: number
}

export const startInterviewApi = (payload: InterviewStartPayload) => {
  return request<InterviewCurrentQuestion>({ url: '/interview/start', method: 'post', data: payload })
}

export const currentQuestionApi = (sessionId: number) => {
  return request<InterviewCurrentQuestion>({ url: `/interview/current/${sessionId}`, method: 'get' })
}

export const submitAnswerApi = (payload: InterviewAnswerPayload) => {
  return request<InterviewAnswerResult>({ url: '/interview/answer', method: 'post', data: payload })
}

export const interviewDetailApi = (sessionId: number) => {
  return request<InterviewDetail>({ url: `/interview/detail/${sessionId}`, method: 'get' })
}

export const fetchInterviewHistoryApi = (direction?: string, pageNum = 1, pageSize = 10) => {
  const params: Record<string, string | number> = { pageNum, pageSize }
  if (direction) params.direction = direction
  return request<PageResult<InterviewHistoryItem>>({ url: '/interview/history', method: 'get', params })
}

export const fetchInterviewTrendApi = (limit = 20) => {
  return request<InterviewHistoryItem[]>({ url: '/interview/trend', method: 'get', params: { limit } })
}

// ── Voice Interview APIs ──────────────────────────

export const fetchVoiceStatusApi = () => {
  return request<{ available: boolean }>({ url: '/interview/voice/status', method: 'get' })
}

export const startVoiceInterviewApi = (payload: VoiceStartPayload) => {
  return request<InterviewCurrentQuestion>({ url: '/interview/voice/start', method: 'post', data: payload })
}

export const submitVoiceAnswerApi = (sessionId: number, questionId: number, audioBlob: Blob) => {
  const formData = new FormData()
  formData.append('sessionId', String(sessionId))
  formData.append('questionId', String(questionId))
  formData.append('audio', audioBlob, 'recording.webm')
  return request<VoiceSubmitResult>({
    url: '/interview/voice/submit',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
