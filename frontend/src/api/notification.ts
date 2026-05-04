import { request } from '@/utils/http'
import type { PageResult, NotificationItem } from '@/types/api'

export const fetchNotificationsApi = (pageNum = 1, pageSize = 20) => {
  return request<PageResult<NotificationItem>>({
    url: '/notification',
    method: 'get',
    params: { pageNum, pageSize },
  })
}

export const fetchUnreadCountApi = () => {
  return request<{ count: number }>({
    url: '/notification/unread-count',
    method: 'get',
  })
}

export const markNotificationsReadApi = (ids: number[]) => {
  return request<void>({
    url: '/notification/read',
    method: 'post',
    data: ids,
  })
}

export const markAllNotificationsReadApi = () => {
  return request<void>({
    url: '/notification/read-all',
    method: 'post',
  })
}
