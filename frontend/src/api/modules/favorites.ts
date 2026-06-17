import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const fetchFavoriteListApi = (params?: AnyRecord) => request<AnyRecord>({ url: '/api/favorites', params })
export const fetchFavoriteStatsApi = () => request<AnyRecord>({ url: '/api/favorites/stats' })
export const addFavoriteApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/favorites', method: 'POST', data: payload })
export const removeFavoriteApi = (id: number) => request<void>({ url: '/api/favorites/' + id, method: 'DELETE' })
export const batchRemoveFavoriteApi = (ids: number[]) => request<void>({ url: '/api/favorites/batch-delete', method: 'POST', data: { ids } })
export const checkFavoriteApi = (targetType: string, targetId: number) => request<AnyRecord>({ url: '/api/favorites/check', params: { targetType, targetId } })
export const fetchFavoriteTagsApi = () => request<AnyRecord>({ url: '/api/favorites/tags' })
export const createFavoriteTagApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/favorites/tags', method: 'POST', data: payload })
export const deleteFavoriteTagApi = (tagId: number) => request<void>({ url: '/api/favorites/tags/' + tagId, method: 'DELETE' })
export const updateFavoriteTagApi = (favoriteId: number, tagId: number | null) => request<AnyRecord>({ url: '/api/favorites/' + favoriteId + '/tag', method: 'PUT', params: { tagId } })
