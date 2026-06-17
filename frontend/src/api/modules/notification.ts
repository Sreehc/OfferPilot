import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const fetchNotificationsApi = (pageNum = 1, pageSize = 20) => request<AnyRecord>({ url: '/api/notification', params: { pageNum, pageSize } })
export const fetchUnreadCountApi = () => request<number | AnyRecord>({ url: '/api/notification/unread-count' })
export const markNotificationsReadApi = (ids: number[]) => request<void>({ url: '/api/notification/read', method: 'POST', data: ids })
export const markAllNotificationsReadApi = () => request<void>({ url: '/api/notification/read-all', method: 'POST' })
