import type { LocationQueryRaw, RouteLocationRaw } from 'vue-router'

export interface AgentWorkbenchPrefill {
  agentType: string
  triggerSource: string
  contextRefs?: string[]
  streamMode?: string
  userPrompt?: string
}

export const buildAgentWorkbenchLocation = (prefill: AgentWorkbenchPrefill): RouteLocationRaw => ({
  path: '/agent',
  query: buildAgentWorkbenchQuery(prefill)
})

export const buildAgentWorkbenchQuery = (prefill: AgentWorkbenchPrefill): LocationQueryRaw => ({
  agentType: prefill.agentType,
  triggerSource: prefill.triggerSource,
  ...(prefill.contextRefs?.length ? { contextRefs: prefill.contextRefs } : {}),
  ...(prefill.streamMode ? { streamMode: prefill.streamMode } : {}),
  ...(prefill.userPrompt ? { userPrompt: prefill.userPrompt } : {})
})
