import { request } from '@/api/client'
import type { AnyRecord } from '@/api/types'
export interface AgentRunCreatePayload { agentType: string; triggerSource?: string; input?: AnyRecord; contextRefs?: AnyRecord[] }
export interface AgentRunDecisionPayload { reason?: string }
export interface AgentRunListQuery { status?: string; agentType?: string; pageNum?: number; pageSize?: number }
export const createAgentRunApi = (payload: AgentRunCreatePayload) => request<AnyRecord>({ url: '/api/agent/runs', method: 'POST', data: payload })
export const fetchAgentRunsApi = (params?: AgentRunListQuery) => request<AnyRecord>({ url: '/api/agent/runs', params })
export const fetchAgentRunDetailApi = (runId: string) => request<AnyRecord>({ url: '/api/agent/runs/' + runId })
export const approveAgentRunApi = (runId: string, payload?: AgentRunDecisionPayload) => request<AnyRecord>({ url: '/api/agent/runs/' + runId + '/approve', method: 'POST', data: payload })
export const rejectAgentRunApi = (runId: string, payload?: AgentRunDecisionPayload) => request<AnyRecord>({ url: '/api/agent/runs/' + runId + '/reject', method: 'POST', data: payload })
export const cancelAgentRunApi = (runId: string, payload?: AgentRunDecisionPayload) => request<AnyRecord>({ url: '/api/agent/runs/' + runId + '/cancel', method: 'POST', data: payload })
