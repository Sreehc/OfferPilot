import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export const fetchProviderConfigsApi = () => request<AnyRecord>({ url: '/api/settings/providers' })
export const updateProviderConfigsApi = (configs: AnyRecord[]) => request<AnyRecord>({ url: '/api/settings/providers', method: 'PUT', data: { configs } })
export const checkProviderConfigsApi = () => request<AnyRecord>({ url: '/api/settings/providers/check', method: 'POST' })
