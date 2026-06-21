import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export interface ChatSendPayload {
  sessionId?: number
  clientMessageId?: string
  mode?: string
  answerMode?: string
  knowledgeScope?: string
  message: string
  contextType?: string
  contextId?: string
  attachmentIds?: string[]
}
export const fetchChatSessionsApi = (pageNum = 1, pageSize = 20) => request<AnyRecord>({ url: '/api/chat/sessions', params: { pageNum, pageSize } })
export const fetchChatMessagesApi = (sessionId: number) => request<AnyRecord[]>({ url: '/api/chat/messages/' + sessionId })
export const sendChatApi = (payload: ChatSendPayload) => request<AnyRecord>({ url: '/api/chat/send', method: 'POST', data: payload })
export const uploadChatAttachmentApi = (file: File) => { const data = new FormData(); data.append('file', file); return request<AnyRecord>({ url: '/api/chat/attachments', method: 'POST', data }) }
export const regenerateChatMessageApi = (messageId: number) => request<AnyRecord>({ url: '/api/chat/messages/' + messageId + '/regenerate', method: 'POST' })
export const feedbackChatMessageApi = (messageId: number, feedback: 'positive' | 'negative') => request<AnyRecord>({ url: '/api/chat/messages/' + messageId + '/feedback', method: 'POST', data: { feedback } })
export const renameChatSessionApi = (sessionId: number, title: string) => request<AnyRecord>({ url: '/api/chat/session/' + sessionId, method: 'PUT', data: { title } })
export const deleteChatSessionApi = (sessionId: number) => request<void>({ url: '/api/chat/session/' + sessionId, method: 'DELETE' })
