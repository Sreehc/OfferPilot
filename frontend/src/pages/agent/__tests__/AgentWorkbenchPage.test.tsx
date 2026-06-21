import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AgentWorkbenchPage } from '@/pages/agent/AgentWorkbenchPage'
import { renderWithProviders } from '@/test/renderWithProviders'
import { approveAgentRunApi, cancelAgentRunApi, fetchAgentRunDetailApi, fetchAgentRunsApi, rejectAgentRunApi } from '@/api/modules/agent'
import { formatDateTime } from '@/modules/common'

vi.mock('@/api/modules/agent', () => ({
  fetchAgentRunsApi: vi.fn(),
  fetchAgentRunDetailApi: vi.fn(),
  createAgentRunApi: vi.fn(() => Promise.resolve({ data: {} })),
  approveAgentRunApi: vi.fn(() => Promise.resolve({ data: {} })),
  rejectAgentRunApi: vi.fn(() => Promise.resolve({ data: {} })),
  cancelAgentRunApi: vi.fn(() => Promise.resolve({ data: {} }))
}))

const agentRuns = [
  {
    id: 'run-approval',
    title: '学习计划代理',
    agentType: 'study_planner',
    triggerSource: 'dashboard',
    status: 'pending_approval',
    approvalStatus: 'waiting',
    updateTime: '2026-06-02T10:00:00',
    summary: '等待你确认写回学习计划。'
  },
  {
    id: 'run-failed',
    title: '投递策略代理',
    agentType: 'application_strategist',
    triggerSource: 'applications',
    status: 'failed',
    updateTime: '2026-06-02T09:30:00',
    summary: '工具调用失败。'
  },
  {
    id: 'run-success',
    title: '简历助手',
    agentType: 'resume_coach',
    triggerSource: 'resume',
    status: 'completed',
    updateTime: '2026-06-02T09:00:00',
    summary: '已生成简历建议。'
  }
]

function mockApprovalRunDetail() {
  vi.mocked(fetchAgentRunsApi).mockResolvedValue({ code: 0, message: 'ok', data: agentRuns })
  vi.mocked(fetchAgentRunDetailApi).mockResolvedValue({
    code: 0,
    message: 'ok',
    data: {
      ...agentRuns[0],
      id: 'run-approval',
      status: 'WAITING_USER_APPROVAL',
      approvalStatus: 'waiting',
      approvalSummary: '即将写回新的学习计划。'
    }
  })
}

async function renderSelectedApprovalRun(user: ReturnType<typeof userEvent.setup>) {
  renderWithProviders(<AgentWorkbenchPage />, { route: '/agent' })
  const approvalRun = await screen.findByText('学习计划代理')
  await user.click(approvalRun.closest('.agent-queue-card') as HTMLElement)
}

describe('AgentWorkbenchPage', () => {
  it('shows queue metadata and filters runs by status', async () => {
    vi.mocked(fetchAgentRunsApi).mockResolvedValue({ code: 0, message: 'ok', data: agentRuns })
    vi.mocked(fetchAgentRunDetailApi).mockResolvedValue({ code: 0, message: 'ok', data: {} })
    const user = userEvent.setup()

    renderWithProviders(<AgentWorkbenchPage />, { route: '/agent' })

    await screen.findByText('学习计划代理')
    const queuePanel = screen.getByText('运行列表').closest('.ant-card')
    expect(queuePanel).not.toBeNull()
    const queue = within(queuePanel as HTMLElement)

    const approvalRun = queue.getByText('学习计划代理')
    const approvalCard = approvalRun.closest('.agent-queue-card')
    expect(approvalCard).not.toBeNull()
    expect(within(approvalCard as HTMLElement).getByText('dashboard')).toBeInTheDocument()
    expect(within(approvalCard as HTMLElement).getByText('待审批')).toBeInTheDocument()
    expect(within(approvalCard as HTMLElement).getByText(formatDateTime('2026-06-02T10:00:00'))).toBeInTheDocument()
    expect(within(approvalCard as HTMLElement).getByText('等待你确认写回学习计划。')).toBeInTheDocument()
    expect(within(approvalCard as HTMLElement).getByText('需要人工确认')).toBeInTheDocument()

    const failedRun = queue.getByText('投递策略代理')
    const failedCard = failedRun.closest('.agent-queue-card')
    expect(failedCard).not.toBeNull()
    expect(within(failedCard as HTMLElement).getByText('applications')).toBeInTheDocument()
    expect(within(failedCard as HTMLElement).getByText('已失败')).toBeInTheDocument()
    expect(within(failedCard as HTMLElement).getByText(formatDateTime('2026-06-02T09:30:00'))).toBeInTheDocument()
    expect(within(failedCard as HTMLElement).getByText('工具调用失败。')).toBeInTheDocument()
    expect(within(failedCard as HTMLElement).getByText('执行失败')).toBeInTheDocument()

    expect(queue.getByText('简历助手')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '待审批' }))
    expect(queue.getByText('学习计划代理')).toBeInTheDocument()
    expect(queue.queryByText('投递策略代理')).not.toBeInTheDocument()
    expect(queue.queryByText('简历助手')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /失\s*败/ }))
    expect(queue.queryByText('学习计划代理')).not.toBeInTheDocument()
    expect(queue.getByText('投递策略代理')).toBeInTheDocument()
    expect(queue.queryByText('简历助手')).not.toBeInTheDocument()
  })

  it('keeps approval and rejection actions recoverable after failures', async () => {
    mockApprovalRunDetail()
    vi.mocked(approveAgentRunApi).mockResolvedValue({ code: 0, message: 'ok', data: {} })
    vi.mocked(rejectAgentRunApi).mockRejectedValue(new Error('reject failed'))
    vi.mocked(cancelAgentRunApi).mockResolvedValue({ code: 0, message: 'ok', data: {} })
    const user = userEvent.setup()

    await renderSelectedApprovalRun(user)

    expect(await screen.findByText('Agent 需要你的确认')).toBeInTheDocument()
    expect(screen.getByText('暂无工具调用')).toBeInTheDocument()
    expect(screen.getByText('等待后端返回 toolCalls 后展示真实工具执行记录。')).toBeInTheDocument()
    expect(screen.getByText('等待后端返回 artifacts 后展示结构化产物。')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '批准执行' }))
    expect(approveAgentRunApi).toHaveBeenCalledWith('run-approval', { reason: '工作台批准' })

    await user.click(screen.getByRole('button', { name: '拒绝' }))
    expect(await screen.findByText('拒绝任务')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '确认拒绝' }))
    expect(screen.getByText('请填写拒绝原因')).toBeInTheDocument()
    expect(rejectAgentRunApi).not.toHaveBeenCalled()

    await user.type(screen.getByPlaceholderText('填写拒绝原因'), '需要重新检查计划')
    await user.click(screen.getByRole('button', { name: '确认拒绝' }))
    await waitFor(() => expect(rejectAgentRunApi).toHaveBeenCalledWith('run-approval', { reason: '需要重新检查计划' }))
    await waitFor(() => expect(screen.getByRole('button', { name: /确认拒绝/ })).not.toBeDisabled())
    expect(screen.getByText('拒绝任务')).toBeInTheDocument()

    const rejectDialog = screen.getByRole('dialog', { name: '拒绝任务' })
    await user.click(within(rejectDialog).getByRole('button', { name: /取\s*消/ }))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: '拒绝任务' })).not.toBeInTheDocument())
  })

  it('keeps cancel actions confirmable', async () => {
    mockApprovalRunDetail()
    vi.mocked(cancelAgentRunApi).mockResolvedValue({ code: 0, message: 'ok', data: {} })
    const user = userEvent.setup()

    await renderSelectedApprovalRun(user)

    expect(await screen.findByText('Agent 需要你的确认')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('button', { name: '取消任务' })).not.toBeDisabled())
    await user.click(screen.getByRole('button', { name: '取消任务' }))
    expect((await screen.findAllByText('取消该任务？')).length).toBeGreaterThan(0)
    const cancelTaskButtons = screen.getAllByRole('button', { name: '取消任务' })
    await user.click(cancelTaskButtons[cancelTaskButtons.length - 1])
    expect(cancelAgentRunApi).toHaveBeenCalledWith('run-approval', { reason: '工作台取消' })
  })
})
