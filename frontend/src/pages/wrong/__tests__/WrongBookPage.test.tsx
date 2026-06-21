import { describe, expect, it, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { WrongBookPage } from '@/pages/wrong/WrongBookPage'
import { renderWithProviders } from '@/test/renderWithProviders'
import { deleteWrongApi, exportWrongMarkdownApi, fetchWrongListApi, updateMasteryApi } from '@/api/modules/wrong'

vi.mock('@/api/modules/wrong', () => ({
  fetchWrongListApi: vi.fn(),
  updateMasteryApi: vi.fn(),
  deleteWrongApi: vi.fn(),
  exportWrongMarkdownApi: vi.fn()
}))

describe('WrongBookPage', () => {
  it('uses a card practice flow and keeps management actions secondary', async () => {
    vi.mocked(fetchWrongListApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        records: [
          {
            id: 201,
            questionId: 77,
            title: 'Redis 缓存雪崩治理',
            masteryLevel: 'reviewing',
            standardAnswer: '随机过期、限流降级、多级缓存和预热。',
            errorReason: '回答缺少限流降级和监控兜底。',
            nextReviewDate: '2026-06-21',
            reviewCount: 2,
            streak: 1
          },
          {
            id: 202,
            questionId: 88,
            title: 'JVM Full GC 排查',
            masteryLevel: 'not_started',
            standardAnswer: '先看 GC 日志、堆转储、分配速率和大对象。',
            errorReason: '定位链路不完整。',
            nextReviewDate: '2026-06-22',
            reviewCount: 0,
            streak: 0
          }
        ]
      }
    })
    vi.mocked(updateMasteryApi).mockResolvedValue({ code: 0, message: 'ok', data: {} })
    vi.mocked(deleteWrongApi).mockResolvedValue({ code: 0, message: 'ok', data: undefined })
    vi.mocked(exportWrongMarkdownApi).mockResolvedValue({ data: new Blob(['# wrong']) } as any)
    const user = userEvent.setup()

    renderWithProviders(<WrongBookPage />, { route: '/wrong' })

    const flow = await screen.findByRole('region', { name: '错题卡片复习流' })
    expect(within(flow).getByText('Redis 缓存雪崩治理')).toBeInTheDocument()
    expect(within(flow).getByText('回答缺少限流降级和监控兜底。')).toBeInTheDocument()
    expect(within(flow).getByRole('link', { name: '重新作答' })).toHaveAttribute('href', '/question/77')
    expect(within(flow).getByRole('button', { name: '查看 AI 解释' })).toBeInTheDocument()

    await user.click(within(flow).getByRole('button', { name: '标记已掌握' }))
    expect(updateMasteryApi).toHaveBeenCalledWith(201, { masteryLevel: 'mastered' })

    await user.click(within(flow).getByRole('button', { name: '下一题' }))
    expect(within(flow).getByText('JVM Full GC 排查')).toBeInTheDocument()

    const management = screen.getByRole('region', { name: '错题管理列表' })
    expect(within(management).getByText('错题管理')).toBeInTheDocument()
  })
})
