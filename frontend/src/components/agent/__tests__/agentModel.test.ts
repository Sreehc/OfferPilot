import { describe, expect, it } from 'vitest'
import { mapAgentArtifacts, mapAgentMessages, mapAgentSteps, mapToolCalls, normalizeAgentStatus, parseStreamingChunk } from '../agentModel'

describe('agentModel', () => {
  it('normalizes run statuses from backend variants', () => {
    expect(normalizeAgentStatus('WAITING_USER_APPROVAL')).toBe('approval_required')
    expect(normalizeAgentStatus('RUNNING')).toBe('running')
    expect(normalizeAgentStatus('COMPLETED')).toBe('success')
    expect(normalizeAgentStatus('REJECTED')).toBe('failed')
    expect(normalizeAgentStatus('CANCELLED')).toBe('cancelled')
  })

  it('maps messages, steps, tools and artifacts to UI contracts', () => {
    expect(mapAgentMessages([{ senderRole: 'tool', text: 'done' }])[0]).toMatchObject({ role: 'tool', content: 'done' })
    expect(mapAgentSteps([{ stage: '检索', state: 'RUNNING' }])[0]).toMatchObject({ title: '检索', status: 'active' })
    expect(mapToolCalls([{ toolName: 'knowledge.search', state: 'SUCCESS', arguments: { q: 'React' } }])[0]).toMatchObject({ name: 'knowledge.search', status: 'success' })
    expect(mapAgentArtifacts([{ name: '简历建议', markdown: '修改项目描述' }])[0]).toMatchObject({ title: '简历建议', content: '修改项目描述' })
  })

  it('parses SSE data and plain text streaming chunks', () => {
    expect(parseStreamingChunk('data: {"delta":"你好","role":"assistant"}')).toMatchObject({ content: '你好', role: 'assistant' })
    expect(parseStreamingChunk('直接文本')).toMatchObject({ content: '直接文本', role: 'assistant' })
    expect(parseStreamingChunk('data: [DONE]')).toBeNull()
  })
})
