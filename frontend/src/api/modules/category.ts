import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const fetchCategoriesApi = (params?: AnyRecord) => request<AnyRecord>({ url: '/api/category/list', params })
export const addCategoryApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/admin/category/add', method: 'POST', data: payload })
export const updateCategoryApi = (payload: AnyRecord) => request<AnyRecord>({ url: '/api/admin/category/update', method: 'PUT', data: payload })
export const deleteCategoryApi = (id: number) => request<void>({ url: '/api/admin/category/delete/' + id, method: 'DELETE' })
