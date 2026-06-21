import { describe, expect, it, vi } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ApplicationBoardPage } from '@/pages/applications/ApplicationBoardPage'
import { renderWithProviders } from '@/test/renderWithProviders'
import { fetchApplicationBoardApi } from '@/api/modules/applications'

vi.mock('@/api/modules/applications', () => ({
  createJobApplicationApi: vi.fn(),
  fetchApplicationBoardApi: vi.fn(),
  fetchApplicationDetailApi: vi.fn(),
  updateApplicationStatusApi: vi.fn(),
  addApplicationEventApi: vi.fn(),
  refreshApplicationAnalysisApi: vi.fn()
}))

function renderApplicationBoard() {
  return renderWithProviders(
    <Routes>
      <Route path="/applications" element={<ApplicationBoardPage />} />
      <Route path="/applications/:id" element={<div>application detail</div>} />
    </Routes>,
    { route: '/applications' }
  )
}

describe('ApplicationBoardPage', () => {
  it('uses Kanban as the main view and keeps an advanced table view for filtering and batch actions', async () => {
    vi.mocked(fetchApplicationBoardApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        records: [
          {
            id: 11,
            companyName: '字节跳动',
            position: 'Java 后端工程师',
            status: 'INTERVIEW',
            matchScore: 88,
            nextAction: '准备系统设计二面复盘',
            recentEvent: { title: '二面已约', time: '2026-06-21T10:00:00Z' }
          },
          {
            id: 12,
            companyName: '腾讯云',
            position: '中间件开发',
            status: 'SCREENING',
            matchScore: 73,
            analysis: { nextAction: '补充 Redis 项目量化指标' },
            events: [{ title: 'HR 已读简历', createTime: '2026-06-18T08:00:00Z' }]
          },
          {
            id: 13,
            companyName: '阿里云',
            jobTitle: '平台研发',
            status: 'APPLIED',
            score: 66
          }
        ]
      }
    })

    const user = userEvent.setup()
    renderApplicationBoard()

    const kanban = await screen.findByRole('region', { name: '投递 Kanban 看板' })
    expect(within(kanban).getByRole('heading', { name: '面试中' })).toBeInTheDocument()
    expect(within(kanban).getByText('字节跳动')).toBeInTheDocument()
    expect(within(kanban).getByText('Java 后端工程师')).toBeInTheDocument()
    expect(within(kanban).getByText('88')).toBeInTheDocument()
    expect(within(kanban).getByText('准备系统设计二面复盘')).toBeInTheDocument()
    expect(within(kanban).getByText('二面已约')).toBeInTheDocument()
    expect(within(kanban).getByRole('link', { name: '查看详情 字节跳动 Java 后端工程师' })).toHaveAttribute('href', '/applications/11')

    await user.click(screen.getByRole('button', { name: '表格高级视图' }))

    const tableView = await screen.findByRole('region', { name: '表格高级视图' })
    expect(within(tableView).getByText('高级筛选')).toBeInTheDocument()
    expect(within(tableView).getByPlaceholderText('搜索公司或岗位')).toBeInTheDocument()
    expect(within(tableView).getByRole('button', { name: '批量刷新分析' })).toBeInTheDocument()
    expect(within(tableView).getAllByText('腾讯云')).toHaveLength(2)
  })
})
