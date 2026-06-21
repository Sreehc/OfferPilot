import { describe, expect, it, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ReviewPage } from '@/pages/review/ReviewPage'
import { renderWithProviders } from '@/test/renderWithProviders'
import { fetchReviewStatsApi, fetchReviewTodayApi, submitReviewRateApi } from '@/api/modules/review'

vi.mock('@/api/modules/review', () => ({
  fetchReviewTodayApi: vi.fn(),
  fetchReviewStatsApi: vi.fn(),
  submitReviewRateApi: vi.fn()
}))

describe('ReviewPage', () => {
  it('prioritizes a card review flow with reveal, rating and next-item actions', async () => {
    vi.mocked(fetchReviewTodayApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        totalPending: 2,
        overdueCount: 1,
        todayCompleted: 0,
        currentStreak: 3,
        items: [
          {
            reviewItemId: '101',
            contentType: 'wrong_card',
            title: 'Redis 缓存击穿怎么处理？',
            answer: '使用互斥锁、热点 key 预热、逻辑过期等方式。',
            explanation: '先说明高并发访问同一过期 key 的风险，再给出工程兜底。',
            masteryLevel: 'reviewing',
            overdueDays: 2,
            streak: 1
          },
          {
            reviewItemId: '102',
            contentType: 'wrong_card',
            title: '线程池拒绝策略有哪些？',
            answer: 'Abort、CallerRuns、Discard、DiscardOldest。',
            explanation: '结合业务降级和监控告警说明策略选择。',
            masteryLevel: 'not_started',
            overdueDays: 0,
            streak: 0
          }
        ]
      }
    })
    vi.mocked(fetchReviewStatsApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: { completionRate: '0%', avgScore: 0, todayPending: 2, todayCompleted: 0 }
    })
    vi.mocked(submitReviewRateApi).mockResolvedValue({ code: 0, message: 'ok', data: {} })
    const user = userEvent.setup()

    renderWithProviders(<ReviewPage />, { route: '/review' })

    const flow = await screen.findByRole('region', { name: '卡片式复习流' })
    expect(within(flow).getByText('Redis 缓存击穿怎么处理？')).toBeInTheDocument()
    expect(within(flow).queryByText('使用互斥锁、热点 key 预热、逻辑过期等方式。')).not.toBeInTheDocument()

    await user.click(within(flow).getByRole('button', { name: '查看答案和 AI 解释' }))

    expect(within(flow).getByText('使用互斥锁、热点 key 预热、逻辑过期等方式。')).toBeInTheDocument()
    expect(within(flow).getByText('先说明高并发访问同一过期 key 的风险，再给出工程兜底。')).toBeInTheDocument()

    await user.click(within(flow).getByRole('button', { name: '掌握一般' }))
    expect(screen.getByText('确认提交本次评分？')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '提交' }))
    expect(submitReviewRateApi).toHaveBeenCalledWith('101', { score: 3 })

    await user.click(within(flow).getByRole('button', { name: '下一题' }))
    expect(within(flow).getByText('线程池拒绝策略有哪些？')).toBeInTheDocument()
  })
})
