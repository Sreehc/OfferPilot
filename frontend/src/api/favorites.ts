import { request } from '@/utils/http'
import type {
  FavoriteItem,
  FavoriteStats,
  FavoriteTagItem,
  PageResult
} from '@/types/api'

export interface FavoriteUpsertPayload {
  targetType: string
  targetId: number
  tagId?: number
}

export interface FavoriteBatchDeletePayload {
  ids: number[]
}

export interface FavoriteTagUpsertPayload {
  name: string
  sortOrder?: number
}

export interface FavoriteListParams {
  targetType?: string
  tagId?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export const fetchFavoriteListApi = (params?: FavoriteListParams) => {
  return request<PageResult<FavoriteItem>>({ url: '/favorites', method: 'get', params })
}

export const fetchFavoriteStatsApi = () => {
  return request<FavoriteStats>({ url: '/favorites/stats', method: 'get' })
}

export const addFavoriteApi = (payload: FavoriteUpsertPayload) => {
  return request<FavoriteItem>({ url: '/favorites', method: 'post', data: payload })
}

export const removeFavoriteApi = (id: number) => {
  return request<null>({ url: `/favorites/${id}`, method: 'delete' })
}

export const batchRemoveFavoriteApi = (ids: number[]) => {
  return request<null>({ url: '/favorites/batch-delete', method: 'post', data: { ids } })
}

export const checkFavoriteApi = (targetType: string, targetId: number) => {
  return request<boolean>({ url: '/favorites/check', method: 'get', params: { targetType, targetId } })
}

export const fetchFavoriteTagsApi = () => {
  return request<FavoriteTagItem[]>({ url: '/favorites/tags', method: 'get' })
}

export const createFavoriteTagApi = (payload: FavoriteTagUpsertPayload) => {
  return request<FavoriteTagItem>({ url: '/favorites/tags', method: 'post', data: payload })
}

export const deleteFavoriteTagApi = (tagId: number) => {
  return request<null>({ url: `/favorites/tags/${tagId}`, method: 'delete' })
}

export const updateFavoriteTagApi = (favoriteId: number, tagId: number | null) => {
  return request<null>({ url: `/favorites/${favoriteId}/tag`, method: 'put', params: { tagId } })
}
