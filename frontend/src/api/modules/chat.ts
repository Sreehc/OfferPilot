import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export interface ChatSendPayload { sessionId?: number; message: string; contextType?: string; contextId?: string }
export const fetchChatSessionsApi = (pageNum = 1, pageSize = 20) => request<AnyRecord>({ url: '/api/chat/sessions', params: { pageNum, pageSize } })
export const fetchChatMessagesApi = (sessionId: number) => request<AnyRecord[]>({ url: '/api/chat/messages/' + sessionId })
export const sendChatApi = (payload: ChatSendPayload) => request<AnyRecord>({ url: '/api/chat/send', method: 'POST', data: payload })
export const deleteChatSessionApi = (sessionId: number) => request<void>({ url: '/api/chat/session/' + sessionId, method: 'DELETE' })
