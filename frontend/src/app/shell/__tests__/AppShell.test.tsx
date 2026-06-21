import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AppShell } from '../AppShell'
import { useAuthStore } from '@/features/auth/authStore'
import { renderWithProviders } from '@/test/renderWithProviders'

vi.mock('@/api/modules/notification', () => ({
  fetchNotificationsApi: vi.fn().mockResolvedValue({ data: [] }),
  fetchUnreadCountApi: vi.fn().mockResolvedValue({ data: { count: 0 } }),
  markAllNotificationsReadApi: vi.fn().mockResolvedValue({ data: undefined }),
  markNotificationsReadApi: vi.fn().mockResolvedValue({ data: undefined })
}))

vi.mock('@/components/feedback/NotificationCenter', () => ({
  NotificationCenter: ({ open }: { open: boolean }) => open ? <div role="dialog">通知中心</div> : null
}))

const originalGetComputedStyle = window.getComputedStyle

beforeAll(() => {
  vi.spyOn(window, 'getComputedStyle').mockImplementation((element: Element) => originalGetComputedStyle(element))
})

afterAll(() => {
  vi.restoreAllMocks()
})

function setUser(role: 'USER' | 'ADMIN') {
  useAuthStore.getState().persistFromResponse({
    token: 'token',
    userInfo: { id: role === 'ADMIN' ? 1 : 2, username: role.toLowerCase(), role }
  })
}

function renderShell(route = '/dashboard') {
  return renderWithProviders(
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/dashboard" element={<div>dashboard content</div>} />
        <Route path="/chat" element={<div>chat content</div>} />
        <Route path="/agent" element={<div>agent content</div>} />
        <Route path="/knowledge" element={<div>knowledge content</div>} />
        <Route path="/interview" element={<div>interview content</div>} />
        <Route path="/resume" element={<div>resume content</div>} />
        <Route path="/applications/:id" element={<div>application detail</div>} />
        <Route path="/admin" element={<div>admin content</div>} />
      </Route>
    </Routes>,
    { route }
  )
}

describe('AppShell desktop navigation', () => {
  beforeEach(() => {
    localStorage.clear()
    useAuthStore.getState().clear()
  })

  it('groups desktop navigation and marks the active parent route', async () => {
    setUser('USER')
    renderShell('/applications/42')

    const nav = await screen.findByRole('navigation', { name: '桌面主导航' })
    for (const group of ['今日', '训练', '求职资产', '知识与 AI', '社区', '系统']) {
      expect(within(nav).getAllByText(group).length).toBeGreaterThan(0)
    }

    const applicationsLink = within(nav).getByRole('link', { name: '投递' })
    expect(applicationsLink).toHaveAttribute('href', '/applications')
    await waitFor(() => expect(applicationsLink).toHaveAttribute('aria-current', 'page'))
  })

  it('hides admin navigation for regular users and shows it for admins', async () => {
    setUser('USER')
    const { unmount } = renderShell('/dashboard')
    expect(screen.queryByRole('link', { name: '管理后台' })).not.toBeInTheDocument()
    unmount()

    useAuthStore.getState().clear()
    setUser('ADMIN')
    renderShell('/admin')

    const adminLink = await screen.findByRole('link', { name: '管理后台' })
    expect(adminLink).toHaveAttribute('href', '/admin')
    expect(adminLink).toHaveAttribute('aria-current', 'page')
  })
})

describe('AppShell mobile navigation', () => {
  beforeEach(() => {
    localStorage.clear()
    useAuthStore.getState().clear()
  })

  it('keeps core mobile entries reachable and groups the more panel', async () => {
    setUser('USER')
    renderShell('/dashboard')

    const mobileNav = screen.getByRole('navigation', { name: '主导航' })
    for (const item of ['今日工作台', '题库', '投递', '社区', '更多']) {
      expect(within(mobileNav).getByRole('button', { name: new RegExp(item) })).toBeInTheDocument()
    }

    await userEvent.click(within(mobileNav).getByRole('button', { name: /更多/ }))
    const morePanel = await screen.findByRole('dialog', { name: '全部功能' })

    for (const group of ['训练', '求职资产', '知识与 AI', '系统']) {
      expect(within(morePanel).getByText(group)).toBeInTheDocument()
    }
    expect(within(morePanel).getByRole('button', { name: /模拟面试/ })).toBeInTheDocument()
    expect(within(morePanel).getByRole('button', { name: /简历/ })).toBeInTheDocument()
    expect(within(morePanel).getByRole('button', { name: /AI 问答/ })).toBeInTheDocument()
    expect(within(morePanel).queryByRole('button', { name: /管理后台/ })).not.toBeInTheDocument()
  })
})

describe('AppShell command panel', () => {
  beforeEach(() => {
    localStorage.clear()
    useAuthStore.getState().clear()
  })

  it('opens a command panel with page search and key actions', async () => {
    setUser('USER')
    renderShell('/dashboard')

    await userEvent.click(screen.getByRole('button', { name: /命令面板/ }))
    const panel = await screen.findByRole('dialog', { name: '命令面板' })

    for (const action of ['开始面试', '上传简历', '创建 Agent 任务', '发起提问']) {
      expect(within(panel).getByRole('button', { name: new RegExp(action) })).toBeInTheDocument()
    }

    await userEvent.type(within(panel).getByPlaceholderText('搜索页面或输入动作关键词'), '知识')
    expect(within(panel).getByRole('button', { name: /知识库/ })).toBeInTheDocument()
  })

  it('runs key actions through route navigation', async () => {
    setUser('USER')
    renderShell('/dashboard')

    await userEvent.click(screen.getByRole('button', { name: /命令面板/ }))
    await userEvent.click(await screen.findByRole('button', { name: /创建 Agent 任务/ }))

    expect(await screen.findByText('agent content')).toBeInTheDocument()
  })
})
