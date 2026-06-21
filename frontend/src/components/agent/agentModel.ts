import type { AnyRecord } from '@/api/types'
import { normalizeRecords, pickText } from '@/modules/common'

export type AgentRunStatus = 'pending' | 'running' | 'approval_required' | 'success' | 'failed' | 'cancelled'
export type AgentStepStatus = 'done' | 'active' | 'wait'

export interface AgentStatusMeta<TStatus extends string = string> {
  status: TStatus
  label: string
  color: string
  description: string
}

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
  rawStatus?: string
  summary?: string
  params?: Record<string, any>
  durationMs?: number
  startedAt?: string
  endedAt?: string
  totalDurationMs?: number
  phaseDurations?: Record<string, number>
  retryCount?: number
  inputSummary?: string
  outputSummary?: string
  errorType?: string
  errorMessage?: string
  rawErrorStack?: string
  telemetryMissing?: boolean
  error?: string
}

export interface AgentStep {
  title: string
  description?: string
  status: AgentStepStatus
  rawStatus?: string
  type?: string
  durationMs?: number
  time?: string
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
  return matchAgentRunStatus(value).status
}

export function getAgentRunStatusMeta(value: unknown): AgentStatusMeta<AgentRunStatus> {
  const matched = matchAgentRunStatus(value)
  if (!matched.known && matched.raw) {
    return {
      ...agentRunStatusMeta.pending,
      label: matched.raw,
      description: '后端返回了暂未归类的 Agent 状态，保留原始状态文本。'
    }
  }
  return agentRunStatusMeta[matched.status]
}

export function getAgentStepStatusMeta(value: unknown): AgentStatusMeta<AgentStepStatus> {
  return agentStepStatusMeta[normalizeStepStatus(value)]
}

export function getAgentApprovalStatusMeta(value: unknown): AgentStatusMeta<AgentRunStatus> {
  const text = String(value || '').trim()
  const normalized = text.toLowerCase()
  if (!normalized) return agentApprovalStatusMeta.not_required
  if (normalized === 'not_required') return agentApprovalStatusMeta.not_required
  if (normalized === 'waiting' || normalized.includes('approval') || normalized.includes('pending')) return agentApprovalStatusMeta.waiting
  if (normalized === 'approved') return agentApprovalStatusMeta.approved
  if (normalized === 'rejected') return agentApprovalStatusMeta.rejected
  if (normalized === 'canceled' || normalized === 'cancelled') return agentApprovalStatusMeta.canceled
  if (normalized === 'completed') return agentApprovalStatusMeta.completed
  return {
    ...agentRunStatusMeta.pending,
    label: text,
    description: '后端返回了暂未归类的审批状态，保留原始状态文本。'
  }
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
    status: normalizeStepStatus(step.status || step.state),
    rawStatus: pickText(step, ['status', 'state'], ''),
    type: pickText(step, ['type', 'stepType', 'stageType'], '通用步骤'),
    durationMs: readNumber(step.durationMs ?? step.elapsedMs ?? step.costMs ?? step.latencyMs),
    time: pickText(step, ['time', 'startTime', 'startedAt', 'createTime', 'createdAt', 'updateTime', 'updatedAt'], '')
  }))
}

export function mapToolCalls(input: unknown): ToolCall[] {
  return normalizeRecords(input).map((call, index) => {
    const totalDurationMs = readNumber(call.totalDurationMs ?? call.durationMs ?? call.latencyMs)
    const phaseDurations = normalizePhaseDurations(call.phaseDurations || call.phaseDurationMs)
    const retryCount = readNumber(call.retryCount ?? call.retries)
    const startedAt = pickText(call, ['startedAt', 'startTime', 'startAt'], '')
    const endedAt = pickText(call, ['endedAt', 'endTime', 'endAt'], '')
    const inputSummary = pickText(call, ['inputSummary'], '')
    const outputSummary = pickText(call, ['outputSummary'], '')
    const errorType = pickText(call, ['errorType'], '')
    const errorMessage = pickText(call, ['errorMessage', 'error', 'failReason'], '')
    const rawErrorStack = pickText(call, ['rawErrorStack', 'errorStack', 'stackTrace'], '')
    const hasPersistedTelemetry = Boolean(
      startedAt ||
      endedAt ||
      totalDurationMs !== undefined ||
      Object.keys(phaseDurations).length ||
      retryCount !== undefined ||
      inputSummary ||
      outputSummary ||
      errorType ||
      rawErrorStack
    )

    return {
      id: String(call.id || call.toolCallId || call.name || index),
      name: pickText(call, ['name', 'toolName', 'type'], 'tool'),
      status: normalizeAgentStatus(call.status || call.state),
      rawStatus: pickText(call, ['status', 'state'], ''),
      summary: pickText(call, ['summary', 'result', 'description', 'message'], ''),
      params: (call.params || call.arguments || call.input) as Record<string, any> | undefined,
      durationMs: totalDurationMs,
      startedAt,
      endedAt,
      totalDurationMs,
      phaseDurations,
      retryCount,
      inputSummary,
      outputSummary,
      errorType,
      errorMessage,
      rawErrorStack,
      telemetryMissing: !hasPersistedTelemetry,
      error: errorMessage
    }
  })
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

function readNumber(value: unknown): number | undefined {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : undefined
}

function normalizePhaseDurations(value: unknown): Record<string, number> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return {}
  return Object.entries(value as Record<string, unknown>).reduce<Record<string, number>>((result, [key, item]) => {
    const numberValue = readNumber(item)
    if (numberValue !== undefined) result[key] = numberValue
    return result
  }, {})
}

function matchAgentRunStatus(value: unknown): { status: AgentRunStatus; known: boolean; raw: string } {
  const raw = String(value || '').trim()
  const text = raw.toUpperCase()
  if (!text) return { status: 'pending', known: true, raw }
  const matched = statusAliases.find(([, aliases]) => aliases.some((alias) => text.includes(alias)))
  return { status: matched?.[0] || 'pending', known: Boolean(matched), raw }
}

const agentRunStatusMeta: Record<AgentRunStatus, AgentStatusMeta<AgentRunStatus>> = {
  pending: {
    status: 'pending',
    label: '待运行',
    color: 'default',
    description: '任务已进入队列，等待调度或后端返回下一步状态。'
  },
  running: {
    status: 'running',
    label: '运行中',
    color: 'processing',
    description: 'Agent 正在执行步骤或调用工具。'
  },
  approval_required: {
    status: 'approval_required',
    label: '待审批',
    color: 'warning',
    description: '存在需要人工确认的写操作或敏感动作。'
  },
  success: {
    status: 'success',
    label: '已成功',
    color: 'success',
    description: '任务已完成，结果和产物可以继续消费。'
  },
  failed: {
    status: 'failed',
    label: '已失败',
    color: 'error',
    description: '任务执行失败，需要查看错误或重试。'
  },
  cancelled: {
    status: 'cancelled',
    label: '已取消',
    color: 'default',
    description: '任务已取消，不会继续执行。'
  }
}

const agentStepStatusMeta: Record<AgentStepStatus, AgentStatusMeta<AgentStepStatus>> = {
  done: {
    status: 'done',
    label: '已完成',
    color: 'green',
    description: '该步骤已完成。'
  },
  active: {
    status: 'active',
    label: '进行中',
    color: 'blue',
    description: '该步骤正在执行或等待人工审批。'
  },
  wait: {
    status: 'wait',
    label: '等待中',
    color: 'gray',
    description: '该步骤尚未开始或缺少明确执行状态。'
  }
}

const agentApprovalStatusMeta: Record<string, AgentStatusMeta<AgentRunStatus>> = {
  not_required: {
    status: 'success',
    label: '无需审批',
    color: 'success',
    description: '当前 Run 不需要人工审批。'
  },
  waiting: {
    status: 'approval_required',
    label: '待审批',
    color: 'warning',
    description: '当前 Run 等待人工确认。'
  },
  approved: {
    status: 'success',
    label: '已批准',
    color: 'success',
    description: '审批已通过，后续动作可以执行或已经执行。'
  },
  rejected: {
    status: 'failed',
    label: '已拒绝',
    color: 'error',
    description: '审批已被拒绝。'
  },
  canceled: {
    status: 'cancelled',
    label: '已取消',
    color: 'default',
    description: '审批或任务已取消。'
  },
  completed: {
    status: 'success',
    label: '已完成',
    color: 'success',
    description: '审批流程已经完成。'
  }
}
