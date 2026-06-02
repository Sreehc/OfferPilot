import { request } from '@/utils/http'
import type { AgentRun } from '@/types/api'

export interface AgentRunCreatePayload {
  agentType: string
  triggerSource: string
  contextRefs?: string[]
  streamMode?: string
  userPrompt?: string
}

export const createAgentRunApi = (payload: AgentRunCreatePayload) => {
  return request<AgentRun>({ url: '/agent/runs', method: 'post', data: payload })
}

export const fetchAgentRunsApi = () => {
  return request<AgentRun[]>({ url: '/agent/runs', method: 'get' })
}

export const fetchAgentRunDetailApi = (runId: string) => {
  return request<AgentRun>({ url: `/agent/runs/${runId}`, method: 'get' })
}
