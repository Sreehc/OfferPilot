import type { AnyRecord } from '@/api/types'
import { normalizeRecords, pickText } from '@/modules/common'

export type AgentRunStatus = 'pending' | 'running' | 'approval_required' | 'success' | 'failed' | 'cancelled'
export type AgentStepStatus = 'done' | 'active' | 'wait'

export interface AgentMessage {
  id: string
  role: 'user' | 'assistant' | 'system' | 'tool'
  content: string
  status?: string
}

export interface ToolCall {
  id: string
  name: string
  status: AgentRunStatus
  summary?: string
  params?: Record<string, any>
  durationMs?: number
  error?: string
}

export interface AgentStep {
  title: string
  description?: string
  status: AgentStepStatus
}

export interface AgentArtifact {
  id: string
  title: string
  content: string
  type?: string
  actionUrl?: string
}

const statusAliases: Array<[AgentRunStatus, string[]]> = [
  ['approval_required', ['APPROVAL', 'REVIEW_REQUIRED', 'WAITING_USER', 'HUMAN']],
  ['cancelled', ['CANCEL', 'ABORT']],
  ['failed', ['FAIL', 'ERROR', 'REJECT']],
  ['success', ['SUCCESS', 'DONE', 'COMPLETE', 'FINISH']],
  ['running', ['RUN', 'PROCESS', 'STREAM', 'ACTIVE', 'START']],
  ['pending', ['PENDING', 'WAIT', 'QUEUE', 'CREATED']]
]

export function normalizeAgentStatus(value: unknown): AgentRunStatus {
  const text = String(value || '').trim().toUpperCase()
  if (!text) return 'pending'
  return statusAliases.find(([, aliases]) => aliases.some((alias) => text.includes(alias)))?.[0] || 'pending'
}

export function normalizeStepStatus(value: unknown): AgentStepStatus {
  const status = normalizeAgentStatus(value)
  if (status === 'success') return 'done'
  if (status === 'running' || status === 'approval_required') return 'active'
  return 'wait'
}

export function isApprovalRequired(record?: AnyRecord | null) {
  if (!record) return false
  return normalizeAgentStatus(record.approvalStatus || record.status || record.runStatus) === 'approval_required'
}

export function mapAgentMessages(input: unknown): AgentMessage[] {
  return normalizeRecords(input).map((item, index) => ({
    id: String(item.id || item.messageId || item.eventId || index),
    role: normalizeRole(item.role || item.senderRole || item.type),
    content: pickText(item, ['content', 'message', 'text', 'delta'], ''),
    status: item.status
  }))
}

export function mapAgentSteps(input: unknown): AgentStep[] {
  return normalizeRecords(input).map((step) => ({
    title: pickText(step, ['title', 'name', 'step', 'stage'], '执行步骤'),
    description: pickText(step, ['description', 'summary', 'content', 'message'], ''),
    status: normalizeStepStatus(step.status || step.state)
  }))
}

export function mapToolCalls(input: unknown): ToolCall[] {
  return normalizeRecords(input).map((call, index) => ({
    id: String(call.id || call.toolCallId || call.name || index),
    name: pickText(call, ['name', 'toolName', 'type'], 'tool'),
    status: normalizeAgentStatus(call.status || call.state),
    summary: pickText(call, ['summary', 'result', 'description', 'message'], ''),
    params: (call.params || call.arguments || call.input) as Record<string, any> | undefined,
    durationMs: Number.isFinite(Number(call.durationMs || call.latencyMs)) ? Number(call.durationMs || call.latencyMs) : undefined,
    error: pickText(call, ['error', 'errorMessage', 'failReason'], '')
  }))
}

export function mapAgentArtifacts(input: unknown): AgentArtifact[] {
  return normalizeRecords(input).map((item, index) => ({
    id: String(item.id || item.artifactId || index),
    title: pickText(item, ['title', 'name', 'summary'], 'Agent 产出'),
    content: pickText(item, ['content', 'markdown', 'description', 'summary'], ''),
    type: pickText(item, ['type', 'artifactType'], ''),
    actionUrl: pickText(item, ['actionUrl', 'url', 'href'], '')
  }))
}

export function parseStreamingChunk(raw: string): Partial<AgentMessage> | null {
  const text = raw.trim()
  if (!text || text === '[DONE]') return null
  const payload = text.startsWith('data:') ? text.slice(5).trim() : text
  if (!payload || payload === '[DONE]') return null
  try {
    const parsed = JSON.parse(payload) as AnyRecord
    return {
      id: String(parsed.id || parsed.messageId || 'streaming'),
      role: normalizeRole(parsed.role || parsed.type || 'assistant'),
      content: pickText(parsed, ['delta', 'content', 'message', 'text'], ''),
      status: parsed.status
    }
  } catch {
    return { id: 'streaming', role: 'assistant', content: payload, status: 'streaming' }
  }
}

function normalizeRole(value: unknown): AgentMessage['role'] {
  const text = String(value || '').toLowerCase()
  if (text.includes('user')) return 'user'
  if (text.includes('system')) return 'system'
  if (text.includes('tool')) return 'tool'
  return 'assistant'
}
