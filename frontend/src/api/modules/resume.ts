import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const uploadResumeApi = (file: File) => { const data = new FormData(); data.append('file', file); return request<AnyRecord>({ url: '/api/resume/upload', method: 'POST', data }) }
export const fetchResumeListApi = () => request<AnyRecord>({ url: '/api/resume/list' })
export const fetchLatestResumeApi = () => request<AnyRecord>({ url: '/api/resume/latest' })
export const fetchResumeDetailApi = (resumeId: string) => request<AnyRecord>({ url: '/api/resume/' + resumeId })
export const updateResumeApi = (resumeId: string, payload: AnyRecord) => request<AnyRecord>({ url: '/api/resume/' + resumeId, method: 'PUT', data: payload })
export const fetchResumeProjectQuestionsApi = (resumeId: string) => request<AnyRecord>({ url: '/api/resume/' + resumeId + '/project-questions' })
export const fetchResumeIntroApi = (resumeId: string) => request<AnyRecord>({ url: '/api/resume/' + resumeId + '/intro' })
export const fetchInterviewResumeApi = (resumeId: string) => request<AnyRecord>({ url: '/api/resume/' + resumeId + '/interview-resume' })
export const retryResumeParseApi = (resumeId: string) => request<AnyRecord>({ url: '/api/resume/' + resumeId + '/retry-parse', method: 'POST' })
export const fetchResumeScoreApi = (resumeId: string) => request<AnyRecord>({ url: '/api/resume/' + resumeId + '/score' })
export const fetchResumeVersionsApi = (resumeId: string) => request<AnyRecord>({ url: '/api/resume/' + resumeId + '/versions' })
export const restoreResumeVersionApi = (versionId: string) => request<AnyRecord>({ url: '/api/resume/versions/' + versionId + '/restore', method: 'POST' })
