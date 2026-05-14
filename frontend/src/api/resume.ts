import { request } from '@/utils/http'
import type { EditableInterviewResume, ResumeFileDetail, ResumeProjectItem, ResumeSummaryItem } from '@/types/api'

export interface ResumeProjectUpdatePayload {
  id?: string
  projectName: string
  roleName: string
  techStack: string
  responsibility: string
  achievement: string
  projectSummary: string
  sortOrder?: number
}

export interface ResumeUpdatePayload {
  title?: string
  summary?: string
  skills?: string[]
  education?: string
  selfIntro?: string
  projects?: ResumeProjectUpdatePayload[]
}

export const uploadResumeApi = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request<ResumeFileDetail>({
    url: '/resume/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const fetchResumeListApi = () => {
  return request<ResumeSummaryItem[]>({ url: '/resume/list', method: 'get' })
}

export const fetchLatestResumeApi = () => {
  return request<ResumeFileDetail | null>({ url: '/resume/latest', method: 'get' })
}

export const fetchResumeDetailApi = (resumeId: string) => {
  return request<ResumeFileDetail>({ url: `/resume/${resumeId}`, method: 'get' })
}

export const updateResumeApi = (resumeId: string, payload: ResumeUpdatePayload) => {
  return request<ResumeFileDetail>({ url: `/resume/${resumeId}`, method: 'put', data: payload })
}

export const fetchResumeProjectQuestionsApi = (resumeId: string) => {
  return request<ResumeProjectItem[]>({ url: `/resume/${resumeId}/project-questions`, method: 'get' })
}

export const fetchResumeIntroApi = (resumeId: string) => {
  return request<{ content: string }>({ url: `/resume/${resumeId}/intro`, method: 'get' })
}

export const fetchInterviewResumeApi = (resumeId: string) => {
  return request<EditableInterviewResume>({ url: `/resume/${resumeId}/interview-resume`, method: 'get' })
}

export const retryResumeParseApi = (resumeId: string) => {
  return request<ResumeFileDetail>({ url: `/resume/${resumeId}/retry-parse`, method: 'post' })
}
