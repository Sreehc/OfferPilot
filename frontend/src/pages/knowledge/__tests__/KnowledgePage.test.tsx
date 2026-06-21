import { describe, expect, it, vi } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { KnowledgePage } from '@/pages/knowledge/KnowledgePage'
import { renderWithProviders } from '@/test/renderWithProviders'
import { fetchCategoriesApi } from '@/api/modules/category'
import {
  deleteKnowledgeDocApi,
  fetchKnowledgeDocDetailApi,
  fetchKnowledgeDocsApi,
  reindexKnowledgeDocApi,
  searchKnowledgeApi,
  uploadKnowledgeDocApi
} from '@/api/modules/knowledge'

vi.mock('@/api/modules/category', () => ({
  fetchCategoriesApi: vi.fn()
}))

vi.mock('@/api/modules/knowledge', () => ({
  deleteKnowledgeDocApi: vi.fn(),
  fetchKnowledgeDocDetailApi: vi.fn(),
  fetchKnowledgeDocsApi: vi.fn(),
  reindexKnowledgeDocApi: vi.fn(),
  searchKnowledgeApi: vi.fn(),
  uploadKnowledgeDocApi: vi.fn()
}))

function mockKnowledgeList() {
  vi.mocked(fetchKnowledgeDocsApi).mockResolvedValue({
    code: 0,
    message: 'ok',
    data: {
      records: [
        {
          id: 1001,
          title: 'JVM 面试手册',
          categoryName: 'Java 基础',
          status: 'READY',
          summary: '覆盖 JVM 内存模型、锁和 GC 的面试材料。',
          updateTime: '2026-06-20T10:00:00Z'
        }
      ]
    }
  })
  vi.mocked(fetchCategoriesApi).mockResolvedValue({
    code: 0,
    message: 'ok',
    data: [{ id: 1, name: 'Java 基础' }]
  })
  vi.mocked(fetchKnowledgeDocDetailApi).mockResolvedValue({ code: 0, message: 'ok', data: {} })
  vi.mocked(deleteKnowledgeDocApi).mockResolvedValue({ code: 0, message: 'ok', data: undefined })
  vi.mocked(reindexKnowledgeDocApi).mockResolvedValue({ code: 0, message: 'ok', data: {} })
  vi.mocked(uploadKnowledgeDocApi).mockResolvedValue({ code: 0, message: 'ok', data: {} })
}

function renderKnowledgePage() {
  return renderWithProviders(
    <Routes>
      <Route path="/knowledge" element={<KnowledgePage />} />
    </Routes>,
    { route: '/knowledge' }
  )
}

describe('KnowledgePage', () => {
  it('shows AI search results with summary, hit snippets, source references and follow-up entry', async () => {
    mockKnowledgeList()
    vi.mocked(searchKnowledgeApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        records: [
          {
            docId: 1001,
            title: 'Synchronized 与 ReentrantLock 对比',
            sourceTitle: 'JVM 面试手册',
            categoryName: 'Java 基础',
            summary: '适合面试先说语义差异，再说可中断、公平锁和条件队列。',
            snippet: 'ReentrantLock 支持可中断锁、超时获取锁和多个 Condition 队列。',
            score: 0.92
          }
        ]
      }
    })
    const user = userEvent.setup()

    renderKnowledgePage()

    const aiSearch = await screen.findByRole('region', { name: 'AI 知识检索' })
    await user.type(within(aiSearch).getByLabelText('AI 检索问题'), 'ReentrantLock 和 synchronized 怎么答')
    await user.click(within(aiSearch).getByRole('button', { name: '检索知识库' }))

    expect(searchKnowledgeApi).toHaveBeenCalledWith('ReentrantLock 和 synchronized 怎么答')
    expect(await within(aiSearch).findByText('Synchronized 与 ReentrantLock 对比')).toBeInTheDocument()
    expect(within(aiSearch).getByText('适合面试先说语义差异，再说可中断、公平锁和条件队列。')).toBeInTheDocument()
    expect(within(aiSearch).getByText('ReentrantLock 支持可中断锁、超时获取锁和多个 Condition 队列。')).toBeInTheDocument()
    expect(within(aiSearch).getByText('来源：JVM 面试手册')).toBeInTheDocument()
    expect(within(aiSearch).getByRole('link', { name: '打开来源' })).toHaveAttribute('href', '/knowledge?docId=1001')
    expect(within(aiSearch).getByRole('link', { name: '继续追问 AI' })).toHaveAttribute(
      'href',
      '/chat?source=knowledge&query=ReentrantLock%20%E5%92%8C%20synchronized%20%E6%80%8E%E4%B9%88%E7%AD%94'
    )
  })

  it('shows a next action when AI search has no matches', async () => {
    mockKnowledgeList()
    vi.mocked(searchKnowledgeApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: { records: [] }
    })
    const user = userEvent.setup()

    renderKnowledgePage()

    const aiSearch = await screen.findByRole('region', { name: 'AI 知识检索' })
    await user.type(within(aiSearch).getByLabelText('AI 检索问题'), '没有资料的冷门问题')
    await user.click(within(aiSearch).getByRole('button', { name: '检索知识库' }))

    expect(await within(aiSearch).findByText('没有匹配结果')).toBeInTheDocument()
    expect(within(aiSearch).getByText('可以换一个关键词，或上传相关资料后重试。')).toBeInTheDocument()
    expect(within(aiSearch).getByRole('button', { name: '上传文档' })).toBeInTheDocument()
  })
})
