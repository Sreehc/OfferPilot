import { describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/test/renderWithProviders'
import { DashboardPage } from '@/pages/dashboard/DashboardPage'
import { ChatPage } from '@/pages/chat/ChatPage'
import { AgentWorkbenchPage } from '@/pages/agent/AgentWorkbenchPage'
import { InterviewPage } from '@/pages/interview/InterviewPage'
import { ResumeAssistantPage } from '@/pages/resume/ResumeAssistantPage'
import { ApplicationBoardPage } from '@/pages/applications/ApplicationBoardPage'
import { AdminPage } from '@/pages/admin/AdminPage'

vi.mock('@/api/modules/dashboard', () => ({ fetchDashboardOverviewApi: vi.fn(() => Promise.resolve({ data: {} })) }))
vi.mock('@/api/modules/chat', () => ({
  fetchChatSessionsApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchChatMessagesApi: vi.fn(() => Promise.resolve({ data: [] })),
  sendChatApi: vi.fn(() => Promise.resolve({ data: {} })),
  deleteChatSessionApi: vi.fn(() => Promise.resolve({ data: undefined }))
}))
vi.mock('@/api/modules/agent', () => ({
  fetchAgentRunsApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchAgentRunDetailApi: vi.fn(() => Promise.resolve({ data: null })),
  createAgentRunApi: vi.fn(() => Promise.resolve({ data: {} })),
  approveAgentRunApi: vi.fn(() => Promise.resolve({ data: {} })),
  rejectAgentRunApi: vi.fn(() => Promise.resolve({ data: {} })),
  cancelAgentRunApi: vi.fn(() => Promise.resolve({ data: {} }))
}))
vi.mock('@/api/modules/interview', () => ({
  fetchInterviewHistoryApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchInterviewTrendApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchLatestCopilotRealtimeSessionApi: vi.fn(() => Promise.resolve({ data: null })),
  startInterviewApi: vi.fn(() => Promise.resolve({ data: {} })),
  createJobPrepSessionApi: vi.fn(() => Promise.resolve({ data: {} })),
  createCopilotPrepSessionApi: vi.fn(() => Promise.resolve({ data: {} })),
  createCopilotRealtimeSessionApi: vi.fn(() => Promise.resolve({ data: {} }))
}))
vi.mock('@/api/modules/resume', () => ({
  fetchResumeListApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchResumeDetailApi: vi.fn(() => Promise.resolve({ data: null })),
  fetchResumeProjectQuestionsApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchResumeScoreApi: vi.fn(() => Promise.resolve({ data: {} })),
  fetchResumeVersionsApi: vi.fn(() => Promise.resolve({ data: [] })),
  uploadResumeApi: vi.fn(() => Promise.resolve({ data: {} })),
  retryResumeParseApi: vi.fn(() => Promise.resolve({ data: {} }))
}))
vi.mock('@/api/modules/applications', () => ({
  fetchApplicationBoardApi: vi.fn(() => Promise.resolve({ data: [] })),
  createJobApplicationApi: vi.fn(() => Promise.resolve({ data: {} })),
  updateApplicationStatusApi: vi.fn(() => Promise.resolve({ data: {} })),
  refreshApplicationAnalysisApi: vi.fn(() => Promise.resolve({ data: {} }))
}))
vi.mock('@/api/modules/admin', () => ({
  fetchAdminOverviewApi: vi.fn(() => Promise.resolve({ data: {} })),
  fetchAdminUsersApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchAdminAiLogSummaryApi: vi.fn(() => Promise.resolve({ data: {} })),
  fetchAdminAiLogsApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchAdminRuntimeGovernanceSummaryApi: vi.fn(() => Promise.resolve({ data: {} })),
  fetchPendingContentApi: vi.fn(() => Promise.resolve({ data: [] })),
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
vi.mock('@/api/modules/category', () => ({ fetchCategoriesApi: vi.fn(() => Promise.resolve({ data: [] })) }))

describe('page smoke', () => {
  it('renders dashboard', async () => {
    renderWithProviders(<DashboardPage />)
    expect(await screen.findByText('今日工作台')).toBeInTheDocument()
  })

  it('renders chat', async () => {
    renderWithProviders(<ChatPage />)
    expect(await screen.findByText('AI 对话')).toBeInTheDocument()
  })

  it('renders agent workbench', async () => {
    renderWithProviders(<AgentWorkbenchPage />)
    expect(await screen.findByText('Agent 工作台')).toBeInTheDocument()
  })

  it('renders interview', async () => {
    renderWithProviders(<InterviewPage />)
    expect(await screen.findByText('模拟面试')).toBeInTheDocument()
  })

  it('renders resume assistant', async () => {
    renderWithProviders(<ResumeAssistantPage />)
    expect(await screen.findByText('简历助手')).toBeInTheDocument()
  })

  it('renders application board', async () => {
    renderWithProviders(<ApplicationBoardPage />)
    expect(await screen.findByText('投递看板')).toBeInTheDocument()
  })

  it('renders admin', async () => {
    renderWithProviders(<AdminPage />)
    expect(await screen.findByText('管理后台')).toBeInTheDocument()
  })
})
