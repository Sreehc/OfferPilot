import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { GeneratedArtifactCard, ThoughtTimeline, ToolCallCard, ToolCallList } from '../AgentComponents'

describe('AgentComponents', () => {
  it('renders tool call status through the agent status display mapping', () => {
    render(<ToolCallCard call={{ id: 'tool-1', name: 'plan.refresh', status: 'running', rawStatus: 'RUNNING', telemetryMissing: true }} />)

    expect(screen.getByText('运行中')).toBeInTheDocument()
    expect(screen.queryByText('RUNNING')).not.toBeInTheDocument()
  })

  it('shows unrecorded telemetry instead of inventing missing tool call values', () => {
    render(<ToolCallCard call={{ id: 'tool-1', name: 'plan.refresh', status: 'running', telemetryMissing: true }} />)

    expect(screen.getByText('plan.refresh')).toBeInTheDocument()
    expect(screen.getAllByText('未记录').length).toBeGreaterThanOrEqual(1)
  })

  it('renders timeline steps with current highlight, duration, fallback type and collapsible long text', () => {
    render(<ThoughtTimeline steps={[
      {
        title: '读取上下文',
        status: 'done',
        type: 'context',
        durationMs: 560,
        time: '2026-06-02T10:00:00',
        description: '已加载简历、岗位和学习计划。'
      },
      {
        title: '生成策略',
        status: 'active',
        durationMs: 1800,
        description: '这是一段很长的说明，用于验证时间线说明区域可以折叠展示，避免后端返回的大段工具说明撑破 Agent 工作台中间区域。'.repeat(4)
      }
    ]} />)

    expect(screen.getByText('读取上下文')).toBeInTheDocument()
    expect(screen.getByText('context')).toBeInTheDocument()
    expect(screen.getByText('耗时 560ms')).toBeInTheDocument()
    expect(screen.getByText('2026/06/02 10:00')).toBeInTheDocument()
    expect(screen.getByText('当前步骤')).toBeInTheDocument()
    expect(screen.getByText('通用步骤')).toBeInTheDocument()
    expect(screen.getByText('展开')).toBeInTheDocument()
  })

  it('renders an empty timeline state when there are no steps', () => {
    render(<ThoughtTimeline steps={[]} />)

    expect(screen.getByText('暂无执行步骤')).toBeInTheDocument()
    expect(screen.getByText('等待后端返回 steps 后展示 Agent 执行链路。')).toBeInTheDocument()
  })

  it('renders empty tool call and artifact states without fallback data', () => {
    render(
      <>
        <ToolCallList calls={[]} />
        <GeneratedArtifactCard title="产出" items={[]} />
      </>
    )

    expect(screen.getByText('暂无工具调用')).toBeInTheDocument()
    expect(screen.getByText('等待后端返回 toolCalls 后展示真实工具执行记录。')).toBeInTheDocument()
    expect(screen.getByText('暂无产出')).toBeInTheDocument()
    expect(screen.getByText('等待后端返回 artifacts 后展示结构化产物。')).toBeInTheDocument()
  })

  it('supports copying artifact content and opening business links', async () => {
    const user = userEvent.setup()
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(window.navigator, 'clipboard', {
      configurable: true,
      value: { writeText }
    })

    render(<GeneratedArtifactCard title="产出" items={[{
      id: 'artifact-1',
      title: '复盘报告',
      content: '建议强化 JVM 和并发表达。',
      type: 'interview_report',
      actionUrl: '/interview/1'
    }]} />)

    await user.click(screen.getByRole('button', { name: '复制产物：复盘报告' }))
    await waitFor(() => expect(writeText).toHaveBeenCalledWith('建议强化 JVM 和并发表达。'))
    expect(screen.getByRole('link', { name: '打开复盘报告' })).toHaveAttribute('href', '/interview/1')
  })
})
