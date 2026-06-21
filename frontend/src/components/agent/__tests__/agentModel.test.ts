import { describe, expect, it } from 'vitest'
import {
  getAgentApprovalStatusMeta,
  getAgentRunStatusMeta,
  getAgentStepStatusMeta,
  mapAgentArtifacts,
  mapAgentMessages,
  mapAgentSteps,
  mapToolCalls,
  normalizeAgentStatus,
  parseStreamingChunk
} from '../agentModel'

describe('agentModel', () => {
  it('normalizes run statuses from backend variants', () => {
    expect(normalizeAgentStatus('WAITING_USER_APPROVAL')).toBe('approval_required')
    expect(normalizeAgentStatus('RUNNING')).toBe('running')
    expect(normalizeAgentStatus('COMPLETED')).toBe('success')
    expect(normalizeAgentStatus('REJECTED')).toBe('failed')
    expect(normalizeAgentStatus('CANCELLED')).toBe('cancelled')
  })

  it('provides stable display metadata for run, step, tool and approval statuses', () => {
    expect(getAgentRunStatusMeta('PENDING')).toMatchObject({ status: 'pending', label: '待运行', color: 'default' })
    expect(getAgentRunStatusMeta('RUNNING')).toMatchObject({ status: 'running', label: '运行中', color: 'processing' })
    expect(getAgentRunStatusMeta('WAITING_USER_APPROVAL')).toMatchObject({ status: 'approval_required', label: '待审批', color: 'warning' })
    expect(getAgentRunStatusMeta('COMPLETED')).toMatchObject({ status: 'success', label: '已成功', color: 'success' })
    expect(getAgentRunStatusMeta('FAILED')).toMatchObject({ status: 'failed', label: '已失败', color: 'error' })
    expect(getAgentRunStatusMeta('CANCELLED')).toMatchObject({ status: 'cancelled', label: '已取消', color: 'default' })
    expect(getAgentRunStatusMeta('PAUSED_BY_SCHEDULER')).toMatchObject({ status: 'pending', label: 'PAUSED_BY_SCHEDULER', color: 'default' })

    expect(getAgentStepStatusMeta('completed')).toMatchObject({ status: 'done', label: '已完成', color: 'green' })
    expect(getAgentStepStatusMeta('running')).toMatchObject({ status: 'active', label: '进行中', color: 'blue' })
    expect(getAgentStepStatusMeta('waiting')).toMatchObject({ status: 'wait', label: '等待中', color: 'gray' })

    expect(getAgentApprovalStatusMeta('not_required')).toMatchObject({ status: 'success', label: '无需审批', color: 'success' })
    expect(getAgentApprovalStatusMeta('waiting')).toMatchObject({ status: 'approval_required', label: '待审批', color: 'warning' })
    expect(getAgentApprovalStatusMeta('rejected')).toMatchObject({ status: 'failed', label: '已拒绝', color: 'error' })
  })

  it('maps messages, steps, tools and artifacts to UI contracts', () => {
    expect(mapAgentMessages([{ senderRole: 'tool', text: 'done' }])[0]).toMatchObject({ role: 'tool', content: 'done' })
    expect(mapAgentSteps([{ stage: '检索', state: 'RUNNING' }])[0]).toMatchObject({ title: '检索', status: 'active' })
    expect(mapToolCalls([{ toolName: 'knowledge.search', state: 'SUCCESS', arguments: { q: 'React' } }])[0]).toMatchObject({ name: 'knowledge.search', status: 'success' })
    expect(mapAgentArtifacts([{ name: '简历建议', markdown: '修改项目描述' }])[0]).toMatchObject({ title: '简历建议', content: '修改项目描述' })
  })

  it('maps agent steps with duration, time and type fallback for the timeline', () => {
    expect(mapAgentSteps([{
      stage: '检索上下文',
      state: 'RUNNING',
      durationMs: 1450,
      startTime: '2026-06-02T10:00:00',
      stepType: 'retrieval',
      message: '正在读取简历和投递上下文。'
    }, {
      name: '未知后端阶段',
      state: 'PAUSED_BY_RATE_LIMIT',
      elapsedMs: 300
    }])).toEqual([
      expect.objectContaining({
        title: '检索上下文',
        status: 'active',
        durationMs: 1450,
        time: '2026-06-02T10:00:00',
        type: 'retrieval',
        description: '正在读取简历和投递上下文。'
      }),
      expect.objectContaining({
        title: '未知后端阶段',
        status: 'wait',
        rawStatus: 'PAUSED_BY_RATE_LIMIT',
        durationMs: 300,
        type: '通用步骤'
      })
    ])
  })

  it('maps persisted tool telemetry and marks historical calls with missing telemetry', () => {
    expect(mapToolCalls([{
      id: 'tool-1',
      name: 'plan.refresh',
      status: 'failed',
      startedAt: '2026-06-02T10:00:00',
      endedAt: '2026-06-02T10:00:02',
      totalDurationMs: 2000,
      phaseDurations: { loadContext: 300, writePlan: 1700 },
      retryCount: 2,
      inputSummary: '基于画像刷新学习计划',
      outputSummary: '写回失败',
      errorType: 'TimeoutException',
      errorMessage: '计划服务超时',
      rawErrorStack: 'java.util.concurrent.TimeoutException: timeout'
    }])[0]).toMatchObject({
      id: 'tool-1',
      name: 'plan.refresh',
      status: 'failed',
      startedAt: '2026-06-02T10:00:00',
      endedAt: '2026-06-02T10:00:02',
      totalDurationMs: 2000,
      phaseDurations: { loadContext: 300, writePlan: 1700 },
      retryCount: 2,
      inputSummary: '基于画像刷新学习计划',
      outputSummary: '写回失败',
      errorType: 'TimeoutException',
      errorMessage: '计划服务超时',
      rawErrorStack: 'java.util.concurrent.TimeoutException: timeout',
      telemetryMissing: false
    })

    expect(mapToolCalls([{ toolName: 'legacy.approval', state: 'WAITING_USER' }])[0]).toMatchObject({
      name: 'legacy.approval',
      telemetryMissing: true
    })
  })

  it('parses SSE data and plain text streaming chunks', () => {
    expect(parseStreamingChunk('data: {"delta":"你好","role":"assistant"}')).toMatchObject({ content: '你好', role: 'assistant' })
    expect(parseStreamingChunk('直接文本')).toMatchObject({ content: '直接文本', role: 'assistant' })
    expect(parseStreamingChunk('data: [DONE]')).toBeNull()
  })
})
