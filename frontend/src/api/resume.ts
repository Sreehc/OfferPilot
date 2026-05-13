import { request } from '@/utils/http'
import type { ResumeFileDetail, ResumeProjectItem, ResumeSummaryItem } from '@/types/api'

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

export const fetchResumeProjectQuestionsApi = (resumeId: string) => {
  return request<ResumeProjectItem[]>({ url: `/resume/${resumeId}/project-questions`, method: 'get' })
}

export const fetchResumeIntroApi = (resumeId: string) => {
  return request<{ content: string }>({ url: `/resume/${resumeId}/intro`, method: 'get' })
}

export const fetchInterviewResumeApi = (resumeId: string) => {
  return request<{ content: string }>({ url: `/resume/${resumeId}/interview-resume`, method: 'get' })
}
