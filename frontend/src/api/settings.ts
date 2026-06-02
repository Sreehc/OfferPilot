import { request } from '@/utils/http'
import type { ProviderScope, UserProviderConfigItem } from '@/types/api'

export interface ProviderConfigUpdateItemPayload {
  scope: ProviderScope
  enabled: boolean
  providerName?: string
  baseUrl?: string
  model?: string
  apiKey?: string
  clearApiKey?: boolean
  accessKey?: string
  clearAccessKey?: boolean
  secretKey?: string
  clearSecretKey?: boolean
  endpoint?: string
  bucket?: string
  regionName?: string
  dimensions?: number | null
}

export const fetchProviderConfigsApi = () => {
  return request<UserProviderConfigItem[]>({ url: '/settings/providers', method: 'get' })
}

export const updateProviderConfigsApi = (configs: ProviderConfigUpdateItemPayload[]) => {
  return request<UserProviderConfigItem[]>({
    url: '/settings/providers',
    method: 'put',
    data: { configs }
  })
}
