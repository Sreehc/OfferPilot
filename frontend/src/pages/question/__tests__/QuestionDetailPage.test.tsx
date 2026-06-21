import { describe, expect, it, vi } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QuestionDetailPage } from '@/pages/question/QuestionDetailPage'
import { renderWithProviders } from '@/test/renderWithProviders'
import { fetchQuestionDetailApi } from '@/api/modules/question'

vi.mock('@/api/modules/question', () => ({
  fetchQuestionDetailApi: vi.fn()
}))

vi.mock('@/api/modules/adaptive', () => ({
  fetchAbilityProfileApi: vi.fn(() => Promise.resolve({ data: {} })),
  fetchRecommendQuestionsApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchRecommendInterviewApi: vi.fn(() => Promise.resolve({ data: {} }))
}))

function renderQuestionDetail(route = '/question/42') {
  return renderWithProviders(
    <Routes>
      <Route path="/question/:id" element={<QuestionDetailPage />} />
    </Routes>,
    { route }
  )
}

describe('QuestionDetailPage', () => {
  it('keeps analysis secondary until the user answers, then shows follow-ups, similar questions and collection entries', async () => {
    vi.mocked(fetchQuestionDetailApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        id: 42,
        title: 'HashMap 为什么线程不安全？',
        content: '说明 HashMap 在并发扩容时可能出现的问题。',
        difficulty: 'medium',
        categoryName: 'Java 集合',
        tags: 'HashMap,并发',
        standardAnswer: 'HashMap 并发写入可能导致数据覆盖、链表环或扩容状态异常。',
        aiAnalysis: '先说非线程安全的根因，再补充 ConcurrentHashMap 的替代方案。',
        followUpPrompts: [
          '如果换成 ConcurrentHashMap，JDK 8 的并发控制有什么变化？',
          '面试官追问 resize 时你会怎么解释？'
        ],
        relatedQuestions: [
          { id: 43, title: 'ConcurrentHashMap 如何保证线程安全？', difficulty: 'hard' }
        ]
      }
    })
    const user = userEvent.setup()

    renderQuestionDetail()

    expect(await screen.findByText('HashMap 为什么线程不安全？')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: '训练作答区' })).toBeInTheDocument()
    expect(screen.queryByText('HashMap 并发写入可能导致数据覆盖、链表环或扩容状态异常。')).not.toBeInTheDocument()
    expect(screen.getByText('未作答前先隐藏参考解析')).toBeInTheDocument()

    await user.type(screen.getByLabelText('我的作答'), 'HashMap 多线程 put 会互相覆盖，扩容时也可能结构异常。')
    await user.click(screen.getByRole('button', { name: '提交作答并查看解析' }))

    expect(await screen.findByText('参考解析')).toBeInTheDocument()
    expect(screen.getByText('HashMap 并发写入可能导致数据覆盖、链表环或扩容状态异常。')).toBeInTheDocument()
    expect(screen.getByText('AI 解析')).toBeInTheDocument()
    expect(screen.getByText('先说非线程安全的根因，再补充 ConcurrentHashMap 的替代方案。')).toBeInTheDocument()
    expect(screen.getByText('AI 追问')).toBeInTheDocument()
    expect(screen.getByText('如果换成 ConcurrentHashMap，JDK 8 的并发控制有什么变化？')).toBeInTheDocument()
    expect(screen.getByText('同类题')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'ConcurrentHashMap 如何保证线程安全？' })).toHaveAttribute('href', '/question/43')

    const actionRegion = screen.getByRole('region', { name: '训练沉淀入口' })
    expect(within(actionRegion).getByRole('link', { name: '收藏本题' })).toHaveAttribute('href', '/favorites?targetType=question&targetId=42')
    expect(within(actionRegion).getByRole('link', { name: '加入错题复习' })).toHaveAttribute('href', '/wrong?questionId=42')
  })
})
