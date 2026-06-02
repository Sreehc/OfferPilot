import { request } from '@/utils/http'
import type {
  CopilotPrepSession,
  InterviewAnswerResult,
  InterviewCurrentQuestion,
  InterviewDetail,
  InterviewHistoryItem,
  JobPrepSession,
  RecordingReviewSession,
  VoiceSubmitResult
} from '@/types/api'
import type { PageResult } from '@/types/api'

export interface InterviewStartPayload {
  direction: string
  jobRole?: string
  experienceLevel?: string
  techStack?: string
  resumeId?: string
  projectId?: string
  durationMinutes?: number
  includeResumeProject?: boolean
  questionCount?: number
  reanswerQuestionId?: number
}

export interface InterviewAnswerPayload {
  sessionId: string
  questionId: string
  answer: string
}

export interface VoiceStartPayload {
  direction: string
  jobRole?: string
  experienceLevel?: string
  techStack?: string
  resumeId?: string
  projectId?: string
  durationMinutes?: number
  includeResumeProject?: boolean
  questionCount?: number
  reanswerQuestionId?: number
}

export interface JobPrepSessionCreatePayload {
  applicationId?: string
  resumeId?: string
  company?: string
  jobTitle?: string
  jdText?: string
}

export interface CopilotPrepSessionCreatePayload {
  applicationId?: string
  resumeId?: string
  jobPrepSessionId?: string
  company?: string
  jobTitle?: string
  jdText?: string
  notes?: string
}

export interface RecordingReviewCreatePayload {
  direction?: string
  jobRole?: string
  notes?: string
  audioFile: File
}

export const startInterviewApi = (payload: InterviewStartPayload) => {
  return request<InterviewCurrentQuestion>({ url: '/interview/start', method: 'post', data: payload })
}

export const currentQuestionApi = (sessionId: string) => {
  return request<InterviewCurrentQuestion>({ url: `/interview/current/${sessionId}`, method: 'get' })
}

export const submitAnswerApi = (payload: InterviewAnswerPayload) => {
  return request<InterviewAnswerResult>({ url: '/interview/answer', method: 'post', data: payload })
}

export const interviewDetailApi = (sessionId: string) => {
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

export const createJobPrepSessionApi = (payload: JobPrepSessionCreatePayload) => {
  return request<JobPrepSession>({ url: '/interview/job-prep/sessions', method: 'post', data: payload })
}

export const fetchJobPrepSessionApi = (sessionId: string) => {
  return request<JobPrepSession>({ url: `/interview/job-prep/sessions/${sessionId}`, method: 'get' })
}

export const createCopilotPrepSessionApi = (payload: CopilotPrepSessionCreatePayload) => {
  return request<CopilotPrepSession>({ url: '/interview/copilot/prep-sessions', method: 'post', data: payload })
}

export const fetchCopilotPrepSessionApi = (sessionId: string) => {
  return request<CopilotPrepSession>({ url: `/interview/copilot/prep-sessions/${sessionId}`, method: 'get' })
}

export const createRecordingReviewApi = (payload: RecordingReviewCreatePayload) => {
  const formData = new FormData()
  if (payload.direction) formData.append('direction', payload.direction)
  if (payload.jobRole) formData.append('jobRole', payload.jobRole)
  if (payload.notes) formData.append('notes', payload.notes)
  formData.append('audio', payload.audioFile)
  return request<RecordingReviewSession>({
    url: '/interview/recording-reviews',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const fetchRecordingReviewApi = (sessionId: string) => {
  return request<RecordingReviewSession>({ url: `/interview/recording-reviews/${sessionId}`, method: 'get' })
}

// ── Voice Interview APIs ──────────────────────────

export const fetchVoiceStatusApi = () => {
  return request<{ available: boolean }>({ url: '/interview/voice/status', method: 'get' })
}

export const startVoiceInterviewApi = (payload: VoiceStartPayload) => {
  return request<InterviewCurrentQuestion>({ url: '/interview/voice/start', method: 'post', data: payload })
}

export const submitVoiceAnswerApi = (sessionId: string, questionId: string, audioBlob: Blob) => {
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
