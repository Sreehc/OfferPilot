import { describe, expect, it, vi } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { cleanup, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { InterviewPage } from '@/pages/interview/InterviewPage'
import { renderWithProviders } from '@/test/renderWithProviders'
import {
  createCopilotPrepSessionApi,
  createCopilotRealtimeSessionApi,
  createJobPrepSessionApi,
  createRecordingReviewApi,
  fetchInterviewHistoryApi,
  fetchInterviewTrendApi,
  fetchLatestCopilotRealtimeSessionApi,
  startInterviewApi,
  startVoiceInterviewApi
} from '@/api/modules/interview'

vi.mock('@/api/modules/interview', () => ({
  createCopilotPrepSessionApi: vi.fn(),
  createCopilotRealtimeSessionApi: vi.fn(),
  createJobPrepSessionApi: vi.fn(),
  createRecordingReviewApi: vi.fn(),
  fetchInterviewHistoryApi: vi.fn(),
  fetchInterviewTrendApi: vi.fn(),
  fetchLatestCopilotRealtimeSessionApi: vi.fn(),
  startInterviewApi: vi.fn(),
  startVoiceInterviewApi: vi.fn()
}))

vi.mock('@/components/charts/EChartCard', () => ({
  EChartCard: ({ title }: { title: string }) => <section>{title}</section>
}))

function mockInterviewApis() {
  vi.mocked(fetchInterviewHistoryApi).mockResolvedValue({
    code: 0,
    message: 'ok',
    data: {
      records: [
        { id: 11, title: 'Java 后端模拟面试', mode: 'standard', status: 'COMPLETED', createTime: '2026-06-20T08:00:00Z' }
      ]
    }
  })
  vi.mocked(fetchInterviewTrendApi).mockResolvedValue({
    code: 0,
    message: 'ok',
    data: [{ date: '06-20', score: 82 }]
  })
  vi.mocked(fetchLatestCopilotRealtimeSessionApi).mockResolvedValue({
    code: 0,
    message: 'ok',
    data: {}
  })
  vi.mocked(startInterviewApi).mockResolvedValue({ code: 0, message: 'ok', data: { sessionId: 101 } })
  vi.mocked(createJobPrepSessionApi).mockResolvedValue({ code: 0, message: 'ok', data: { sessionId: 102 } })
  vi.mocked(createCopilotPrepSessionApi).mockResolvedValue({ code: 0, message: 'ok', data: { sessionId: 103 } })
  vi.mocked(createCopilotRealtimeSessionApi).mockResolvedValue({ code: 0, message: 'ok', data: { sessionId: 104 } })
  vi.mocked(startVoiceInterviewApi).mockResolvedValue({ code: 0, message: 'ok', data: { sessionId: 105 } })
  vi.mocked(createRecordingReviewApi).mockResolvedValue({ code: 0, message: 'ok', data: { sessionId: 106 } })
}

function renderInterviewPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/interview" element={<InterviewPage />} />
      <Route path="/interview/detail/:id" element={<div>interview detail</div>} />
    </Routes>,
    { route: '/interview' }
  )
}

describe('InterviewPage', () => {
  it('shows a first-screen mode launcher with five interview modes before history and trend', async () => {
    mockInterviewApis()

    renderInterviewPage()

    const launcher = await screen.findByRole('region', { name: '面试模式启动器' })
    expect(within(launcher).getByRole('button', { name: '开始标准面试' })).toBeInTheDocument()
    expect(within(launcher).getByRole('button', { name: '启动 JD 备面' })).toBeInTheDocument()
    expect(within(launcher).getByRole('button', { name: '打开实时 Copilot' })).toBeInTheDocument()
    expect(within(launcher).getByRole('button', { name: '开始语音面试' })).toBeInTheDocument()
    expect(within(launcher).getByRole('button', { name: '上传录屏复盘' })).toBeInTheDocument()

    const trend = await screen.findByText('面试趋势')
    const history = await screen.findByText('历史记录')
    expect(launcher.compareDocumentPosition(trend) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(launcher.compareDocumentPosition(history) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('starts voice interview and recording review from the mode launcher', async () => {
    mockInterviewApis()
    const user = userEvent.setup()

    renderInterviewPage()

    const launcher = await screen.findByRole('region', { name: '面试模式启动器' })
    await user.click(within(launcher).getByRole('button', { name: '开始语音面试' }))
    expect(vi.mocked(startVoiceInterviewApi).mock.calls[0]?.[0]).toEqual(expect.objectContaining({
      direction: 'Java 后端',
      durationMinutes: 20,
      questionCount: 3
    }))

    cleanup()
    mockInterviewApis()
    renderInterviewPage()

    const recordingLauncher = await screen.findByRole('region', { name: '面试模式启动器' })
    await user.click(within(recordingLauncher).getByRole('button', { name: '上传录屏复盘' }))
    const dialog = await screen.findByRole('dialog', { name: '上传录屏复盘' })
    await user.type(within(dialog).getByLabelText('文字转写'), '面试官问了 JVM 调优，我回答了 GC 日志和堆内存参数。')
    await user.click(within(dialog).getByRole('button', { name: '生成复盘' }))

    expect(vi.mocked(createRecordingReviewApi).mock.calls[0]?.[0]).toEqual(expect.objectContaining({
      transcriptText: '面试官问了 JVM 调优，我回答了 GC 日志和堆内存参数。'
    }))
  })
})
