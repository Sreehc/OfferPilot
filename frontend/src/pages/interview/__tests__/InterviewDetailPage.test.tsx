import { describe, expect, it, vi } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { InterviewDetailPage } from '@/pages/interview/InterviewDetailPage'
import { renderWithProviders } from '@/test/renderWithProviders'
import { interviewDetailApi } from '@/api/modules/interview'

vi.mock('@/api/modules/interview', () => ({
  interviewDetailApi: vi.fn()
}))

function renderInterviewDetail() {
  return renderWithProviders(
    <Routes>
      <Route path="/interview/detail/:id" element={<InterviewDetailPage />} />
    </Routes>,
    { route: '/interview/detail/88' }
  )
}

describe('InterviewDetailPage', () => {
  it('shows review summary, ability scores, weak points, next tasks and collapsible long Q&A', async () => {
    vi.mocked(interviewDetailApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        sessionId: 88,
        direction: 'Java 后端',
        jobRole: '高级 Java 工程师',
        status: 'completed',
        mode: 'standard',
        totalScore: 62,
        startTime: '2026-06-20T08:00:00Z',
        endTime: '2026-06-20T08:30:00Z',
        records: [
          {
            questionId: 1,
            questionTitle: '讲一下 JVM GC 调优排查过程',
            userAnswer: '这是一段很长的回答，包含了 GC 日志、堆内存、线程状态、线上压测、监控指标、回滚策略和复盘动作。'.repeat(6),
            score: 48,
            comment: '回答有知识点，但缺少排查顺序和指标闭环。',
            reviewSummary: '需要先讲现象、指标、定位路径，再讲调优参数。',
            followUp: '补一轮 GC 日志分析专项训练。',
            weakPointTags: ['定位链路不完整', '指标闭环不足'],
            isLowScore: true,
            scoreBreakdown: [
              { dimension: '表达结构', score: 42, summary: '结构不清晰' },
              { dimension: '工程深度', score: 56, summary: '案例细节不足' }
            ]
          },
          {
            questionId: 2,
            questionTitle: '线程池参数如何设置',
            userAnswer: '会结合 CPU、IO、队列长度和拒绝策略设置。',
            score: 76,
            comment: '基础完整。',
            reviewSummary: '可以继续补充监控指标。',
            followUp: '准备一个生产参数案例。',
            weakPointTags: ['监控指标'],
            scoreBreakdown: [
              { dimension: '表达结构', score: 72, summary: '基本完整' },
              { dimension: '工程深度', score: 78, summary: '有工程视角' }
            ]
          }
        ]
      }
    })

    const user = userEvent.setup()
    renderInterviewDetail()

    const review = await screen.findByRole('region', { name: 'AI 复盘报告' })
    expect(within(review).getByText('Java 后端 / 高级 Java 工程师')).toBeInTheDocument()
    expect(within(review).getByText('综合评分')).toBeInTheDocument()
    expect(within(review).getByText('62')).toBeInTheDocument()

    const ability = await screen.findByRole('region', { name: '能力图' })
    expect(within(ability).getByText('表达结构')).toBeInTheDocument()
    expect(within(ability).getByText('工程深度')).toBeInTheDocument()

    const weak = await screen.findByRole('region', { name: '薄弱项和改进任务' })
    expect(within(weak).getByText('定位链路不完整')).toBeInTheDocument()
    expect(within(weak).getByText('低分重点')).toBeInTheDocument()
    expect(within(weak).getByText('补一轮 GC 日志分析专项训练。')).toBeInTheDocument()

    const qa = await screen.findByRole('region', { name: '问答复盘' })
    expect(within(qa).getByText('讲一下 JVM GC 调优排查过程')).toBeInTheDocument()
    expect(within(qa).queryByText(/线上压测、监控指标、回滚策略和复盘动作/)).not.toBeInTheDocument()

    await user.click(within(qa).getByRole('button', { name: '展开完整问答' }))
    expect(within(qa).getByText(/线上压测、监控指标、回滚策略和复盘动作/)).toBeInTheDocument()
  })
})
