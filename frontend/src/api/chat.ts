import { request } from '@/utils/http'
import type { ChatMessageItem, ChatSendResult, ChatSessionItem, PageResult } from '@/types/api'

export interface ChatSendPayload {
  sessionId?: number
  mode: 'chat' | 'rag'
  message: string
}

export const fetchChatSessionsApi = (pageNum = 1, pageSize = 20) => {
  return request<PageResult<ChatSessionItem>>({ url: '/chat/sessions', method: 'get', params: { pageNum, pageSize } })
}

export const fetchChatMessagesApi = (sessionId: number) => {
  return request<ChatMessageItem[]>({ url: `/chat/messages/${sessionId}`, method: 'get' })
}

export const sendChatApi = (payload: ChatSendPayload) => {
  return request<ChatSendResult>({ url: '/chat/send', method: 'post', data: payload })
}

export const deleteChatSessionApi = (sessionId: number) => {
  return request<null>({ url: `/chat/session/${sessionId}`, method: 'delete' })
}
