import { describe, expect, it, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/test/renderWithProviders'
import { AdminPage } from '@/pages/admin/AdminPage'
import {
  fetchAdminAiLogSummaryApi,
  fetchAdminAiLogsApi,
  fetchAdminRuntimeGovernanceSummaryApi
} from '@/api/modules/admin'

vi.mock('@/api/modules/admin', () => ({
  fetchAdminOverviewApi: vi.fn(() => Promise.resolve({ data: { totalUsers: 12, todayActive: 5, todayNew: 2 } })),
  fetchAdminUsersApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchPendingContentApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchAdminAiLogSummaryApi: vi.fn(),
  fetchAdminAiLogsApi: vi.fn(),
  fetchAdminRuntimeGovernanceSummaryApi: vi.fn(),
  fetchAdminSystemConfigsApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchAdminInterviewGovernanceApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchAdminInterviewGovernanceSummaryApi: vi.fn(() => Promise.resolve({ data: {} })),
  exportQuestionsApi: vi.fn(() => Promise.resolve({ data: new Blob() })),
  exportUsersApi: vi.fn(() => Promise.resolve({ data: new Blob() })),
  banUserApi: vi.fn(() => Promise.resolve({ data: {} })),
  unbanUserApi: vi.fn(() => Promise.resolve({ data: {} })),
  approveContentApi: vi.fn(() => Promise.resolve({ data: {} })),
  rejectContentApi: vi.fn(() => Promise.resolve({ data: {} })),
  updateAdminUserApi: vi.fn(() => Promise.resolve({ data: {} })),
  addAdminQuestionApi: vi.fn(() => Promise.resolve({ data: {} })),
  addAdminCategoryApi: vi.fn(() => Promise.resolve({ data: {} }))
}))

describe('AdminPage governance architecture', () => {
  it('groups admin modules and shows AI observability before logs', async () => {
    vi.mocked(fetchAdminAiLogSummaryApi).mockResolvedValue({
      data: {
        totalCalls: 120,
        successCalls: 90,
        failedCalls: 30,
        avgLatencyMs: 850,
        chatCalls: 75,
        embeddingCalls: 45,
        usageSummary: {
          totalTokens: 18000,
          estimatedCost: 12.5
        }
      }
    } as any)
    vi.mocked(fetchAdminAiLogsApi).mockResolvedValue({
      data: {
        records: [
          {
            id: 1,
            scene: 'chat',
            provider: 'OpenAI',
            model: 'gpt-4o-mini',
            callType: 'chat',
            success: 0,
            latencyMs: 1250,
            errorMessage: 'quota exceeded'
          }
        ],
        total: 1
      }
    } as any)
    vi.mocked(fetchAdminRuntimeGovernanceSummaryApi).mockResolvedValue({
      data: {
        totalAgentRuns: 42,
        pendingApprovalRuns: 3,
        providerBlockedRuns: 2,
        failedAiCalls: 30,
        avgAiLatencyMs: 850,
        riskSignals: ['AI 调用日志里累计有 30 次失败请求，需要结合场景排查。'],
        recommendations: ['结合 AI 日志中的失败场景与耗时数据，先排查高频失败的模型调用路径。']
      }
    } as any)

    const user = userEvent.setup()
    renderWithProviders(<AdminPage />, { route: '/admin' })

    expect(await screen.findByRole('tab', { name: '用户与权限' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '内容治理' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'AI 观测' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '系统配置' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '运行治理' })).toBeInTheDocument()

    await user.click(screen.getByRole('tab', { name: 'AI 观测' }))

    const panel = await screen.findByRole('region', { name: 'AI 可观测面板' })
    expect(within(panel).getByText('调用量')).toBeInTheDocument()
    expect(within(panel).getByText('120')).toBeInTheDocument()
    expect(within(panel).getByText('失败率')).toBeInTheDocument()
    expect(within(panel).getByText('25%')).toBeInTheDocument()
    expect(within(panel).getByText('平均耗时')).toBeInTheDocument()
    expect(within(panel).getByText('850ms')).toBeInTheDocument()
    expect(within(panel).getByText('调用结构趋势')).toBeInTheDocument()
    expect(within(panel).getByText('Chat 75 / Embedding 45')).toBeInTheDocument()
    expect(within(panel).getByText('异常原因')).toBeInTheDocument()
    expect(within(panel).getByText('quota exceeded')).toBeInTheDocument()

    await user.click(screen.getByRole('tab', { name: '运行治理' }))

    expect(await screen.findByText('运行风险信号')).toBeInTheDocument()
    expect(screen.getByText('AI 调用日志里累计有 30 次失败请求，需要结合场景排查。')).toBeInTheDocument()
    expect(screen.getByText('结合 AI 日志中的失败场景与耗时数据，先排查高频失败的模型调用路径。')).toBeInTheDocument()
  })
})
