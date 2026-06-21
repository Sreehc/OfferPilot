import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import { DashboardPage } from '@/pages/dashboard/DashboardPage'
import { renderWithProviders } from '@/test/renderWithProviders'
import { fetchDashboardOverviewApi } from '@/api/modules/dashboard'
import { fetchAbilityProfileApi, fetchRecommendInterviewApi, fetchRecommendQuestionsApi } from '@/api/modules/adaptive'

vi.mock('@/api/modules/dashboard', () => ({
  fetchDashboardOverviewApi: vi.fn()
}))

vi.mock('@/api/modules/adaptive', () => ({
  fetchAbilityProfileApi: vi.fn(),
  fetchRecommendQuestionsApi: vi.fn(),
  fetchRecommendInterviewApi: vi.fn()
}))

vi.mock('@/components/charts/EChartCard', () => ({
  EChartCard: ({ title }: { title: string }) => <section aria-label={title}>{title}</section>
}))

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(fetchAbilityProfileApi).mockResolvedValue({ code: 0, message: 'ok', data: {} })
    vi.mocked(fetchRecommendQuestionsApi).mockResolvedValue({ code: 0, message: 'ok', data: [] })
    vi.mocked(fetchRecommendInterviewApi).mockResolvedValue({ code: 0, message: 'ok', data: {} })
  })

  it('shows today main task, progress, weak points and AI next action in the first screen', async () => {
    vi.mocked(fetchDashboardOverviewApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        learningCount: 18,
        averageScore: 82,
        wrongCount: 7,
        progress: 60,
        todayTaskCount: 5,
        completedTaskCount: 3,
        reviewDebtCount: 4,
        studyStreak: 6,
        nextAction: {
          title: '完成今天的计划',
          description: '今天还有 2 项计划任务待处理，先把它们推进完。',
          path: '/study-plan',
          reason: '当前已有计划且今天仍有未完成任务，应优先回到执行页。',
          priority: 'P0'
        },
        suggestedFocus: '先补齐 Redis 缓存一致性表达，再做一轮 JD 备面。',
        weakPoints: [
          { categoryName: 'Redis 缓存一致性', wrongCount: 4, score: 48 },
          { categoryName: '线程池参数', wrongCount: 3, score: 55 }
        ],
        workflowContinuations: [
          {
            key: 'agent_approval',
            label: '处理待审批 Agent',
            status: '待审批 1 项',
            description: '确认学习计划写回。',
            path: '/agent?listStatus=pending_approval'
          }
        ],
        recentActivities: [{ id: 1, title: '完成 HashMap 追问', time: '10:20' }],
        trend: [{ label: '周一', value: 3 }]
      }
    })

    renderWithProviders(<DashboardPage />, { route: '/dashboard' })

    expect(await screen.findByText('今日 AI 训练驾驶舱')).toBeInTheDocument()
    expect(screen.getByText('今日主任务')).toBeInTheDocument()
    expect(await screen.findByText('完成今天的计划')).toBeInTheDocument()
    expect(screen.getByText('训练进度')).toBeInTheDocument()
    expect(screen.getByText('完成 3 / 5')).toBeInTheDocument()
    expect(screen.getByText('薄弱项提醒')).toBeInTheDocument()
    expect(screen.getByText('AI 下一步建议')).toBeInTheDocument()
    expect(screen.getByText('先补齐 Redis 缓存一致性表达，再做一轮 JD 备面。')).toBeInTheDocument()

    const cockpit = screen.getByRole('region', { name: '今日 AI 训练驾驶舱' })
    expect(within(cockpit).getByText('Redis 缓存一致性')).toBeInTheDocument()
    expect(within(cockpit).getByRole('link', { name: '执行主任务' })).toHaveAttribute('href', '/study-plan')
    expect(within(cockpit).getByRole('link', { name: '开始面试' })).toHaveAttribute('href', '/interview')
    expect(within(cockpit).getByRole('link', { name: '刷题训练' })).toHaveAttribute('href', '/question')
    expect(within(cockpit).getByRole('link', { name: '上传简历' })).toHaveAttribute('href', '/resume')
  })

  it('guides empty dashboards toward interview, question practice and resume upload', async () => {
    vi.mocked(fetchDashboardOverviewApi).mockResolvedValue({ code: 0, message: 'ok', data: {} })

    renderWithProviders(<DashboardPage />, { route: '/dashboard' })

    expect(await screen.findByText('今天先建立第一条训练记录')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '开始面试' })).toHaveAttribute('href', '/interview')
    expect(screen.getByRole('link', { name: '刷题训练' })).toHaveAttribute('href', '/question')
    expect(screen.getByRole('link', { name: '上传简历' })).toHaveAttribute('href', '/resume')
    const panel = await screen.findByRole('region', { name: '自适应推荐' })
    expect(await within(panel).findByText('暂无自适应推荐')).toBeInTheDocument()
  })

  it('surfaces adaptive recommendations with reasons, weak points and target links', async () => {
    vi.mocked(fetchDashboardOverviewApi).mockResolvedValue({ code: 0, message: 'ok', data: {} })
    vi.mocked(fetchAbilityProfileApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        weakCategories: ['Redis 缓存一致性'],
        suggestedFocus: '先补 Redis 缓存一致性，再做一次专项面试。',
        recommendedDifficulty: 'medium'
      }
    })
    vi.mocked(fetchRecommendQuestionsApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: [
        {
          questionId: 77,
          title: 'Redis 缓存雪崩治理',
          categoryName: 'Redis 缓存一致性',
          difficulty: 'medium',
          reason: '错题集中在缓存一致性，先补同类题。'
        }
      ]
    })
    vi.mocked(fetchRecommendInterviewApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        direction: 'Redis 专项追问',
        questionCount: 5,
        difficulty: 'medium',
        reason: '面试复盘显示缓存一致性回答不稳定。'
      }
    })

    renderWithProviders(<DashboardPage />, { route: '/dashboard' })

    const panel = await screen.findByRole('region', { name: '自适应推荐' })
    expect(await within(panel).findByText('Redis 缓存雪崩治理')).toBeInTheDocument()
    expect(within(panel).getByText('错题集中在缓存一致性，先补同类题。')).toBeInTheDocument()
    expect(within(panel).getAllByText('关联薄弱项：Redis 缓存一致性').length).toBeGreaterThan(0)
    expect(within(panel).getByRole('link', { name: '去刷题' })).toHaveAttribute('href', '/question/77')
    expect(within(panel).getByText('Redis 专项追问')).toBeInTheDocument()
    expect(within(panel).getByText('面试复盘显示缓存一致性回答不稳定。')).toBeInTheDocument()
    expect(within(panel).getByRole('link', { name: '启动面试' })).toHaveAttribute('href', '/interview')
    expect(within(panel).getByRole('link', { name: '查看画像' })).toHaveAttribute('href', '/analytics')
  })
})
