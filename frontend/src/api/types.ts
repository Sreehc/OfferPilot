export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records?: T[]
  list?: T[]
  total?: number
  pageNum?: number
  pageSize?: number
  pages?: number
}

export interface UserInfo {
  id: number
  username: string
  email?: string
  nickname?: string
  avatarUrl?: string
  role?: 'USER' | 'ADMIN' | string
  status?: string
}

export interface LoginResponse {
  token: string
  userInfo: UserInfo
  deviceId?: number
  requires2fa?: boolean
  tempToken?: string
}

export type ID = string | number
export type AnyRecord = Record<string, any>
