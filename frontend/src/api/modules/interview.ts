import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const startInterviewApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/interview/start', method: 'POST', data: payload })
export const currentQuestionApi = (sessionId: string) => request<AnyRecord>({ url: '/api/interview/current/' + sessionId })
export const submitAnswerApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/interview/answer', method: 'POST', data: payload })
export const interviewDetailApi = (sessionId: string) => request<AnyRecord>({ url: '/api/interview/detail/' + sessionId })
export const fetchInterviewHistoryApi = (direction?: string, pageNum = 1, pageSize = 10) => request<AnyRecord>({ url: '/api/interview/history', params: { direction, pageNum, pageSize } })
export const fetchInterviewTrendApi = (limit = 20) => request<AnyRecord[]>({ url: '/api/interview/trend', params: { limit } })
export const createJobPrepSessionApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/interview/job-prep/sessions', method: 'POST', data: payload })
export const fetchJobPrepSessionApi = (sessionId: string) => request<AnyRecord>({ url: '/api/interview/job-prep/sessions/' + sessionId })
export const fetchLatestJobPrepSessionApi = () => request<AnyRecord>({ url: '/api/interview/job-prep/sessions/latest' })
export const createCopilotPrepSessionApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/interview/copilot/prep-sessions', method: 'POST', data: payload })
export const fetchCopilotPrepSessionApi = (sessionId: string) => request<AnyRecord>({ url: '/api/interview/copilot/prep-sessions/' + sessionId })
export const fetchLatestCopilotPrepSessionApi = () => request<AnyRecord>({ url: '/api/interview/copilot/prep-sessions/latest' })
export const createCopilotRealtimeSessionApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/interview/copilot/realtime-sessions', method: 'POST', data: payload })
export const fetchCopilotRealtimeSessionApi = (sessionId: string) => request<AnyRecord>({ url: '/api/interview/copilot/realtime-sessions/' + sessionId })
export const fetchLatestCopilotRealtimeSessionApi = () => request<AnyRecord>({ url: '/api/interview/copilot/realtime-sessions/latest' })
export const buildCopilotRealtimeWebSocketUrl = (sessionId: string) => '/api/interview/copilot/realtime-sessions/' + sessionId + '/ws'
export const createRecordingReviewApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/interview/recording-reviews', method: 'POST', data: payload })
export const fetchRecordingReviewApi = (sessionId: string) => request<AnyRecord>({ url: '/api/interview/recording-reviews/' + sessionId })
export const fetchLatestRecordingReviewApi = () => request<AnyRecord>({ url: '/api/interview/recording-reviews/latest' })
export const fetchVoiceStatusApi = () => request<AnyRecord>({ url: '/api/interview/voice/status' })
export const startVoiceInterviewApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/interview/voice/start', method: 'POST', data: payload })
export const submitVoiceAnswerApi = (sessionId: string, questionId: string, audioBlob: Blob) => { const data = new FormData(); data.append('sessionId', sessionId); data.append('questionId', questionId); data.append('audio', audioBlob); return request<AnyRecord>({ url: '/api/interview/voice/submit', method: 'POST', data }) }
