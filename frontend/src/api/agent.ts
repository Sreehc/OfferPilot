import { request } from '@/utils/http'
import type { AgentRun } from '@/types/api'

export interface AgentRunCreatePayload {
  agentType: string
  triggerSource: string
  contextRefs?: string[]
  streamMode?: string
  userPrompt?: string
}

export interface AgentRunDecisionPayload {
  note?: string
}

export interface AgentRunListQuery {
  agentType?: string
  status?: string
  triggerSource?: string
}

export const createAgentRunApi = (payload: AgentRunCreatePayload) => {
  return request<AgentRun>({ url: '/agent/runs', method: 'post', data: payload })
}

export const fetchAgentRunsApi = (params?: AgentRunListQuery) => {
  return request<AgentRun[]>({ url: '/agent/runs', method: 'get', params })
}

export const fetchAgentRunDetailApi = (runId: string) => {
  return request<AgentRun>({ url: `/agent/runs/${runId}`, method: 'get' })
}

export const approveAgentRunApi = (runId: string, payload?: AgentRunDecisionPayload) => {
  return request<AgentRun>({ url: `/agent/runs/${runId}/approve`, method: 'post', data: payload })
}

export const rejectAgentRunApi = (runId: string, payload?: AgentRunDecisionPayload) => {
  return request<AgentRun>({ url: `/agent/runs/${runId}/reject`, method: 'post', data: payload })
}

export const cancelAgentRunApi = (runId: string, payload?: AgentRunDecisionPayload) => {
  return request<AgentRun>({ url: `/agent/runs/${runId}/cancel`, method: 'post', data: payload })
}
