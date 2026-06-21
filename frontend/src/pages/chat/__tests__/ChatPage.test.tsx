import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ChatPage } from '@/pages/chat/ChatPage'
import { renderWithProviders } from '@/test/renderWithProviders'
import {
  deleteChatSessionApi,
  feedbackChatMessageApi,
  fetchChatMessagesApi,
  fetchChatSessionsApi,
  regenerateChatMessageApi,
  renameChatSessionApi,
  sendChatApi,
  uploadChatAttachmentApi
} from '@/api/modules/chat'

const streamingMock = vi.hoisted(() => ({
  streaming: false,
  stop: vi.fn(),
  sendStreamingMessage: vi.fn(() => Promise.resolve())
}))

vi.mock('@/api/modules/chat', () => ({
  fetchChatSessionsApi: vi.fn(),
  fetchChatMessagesApi: vi.fn(),
  sendChatApi: vi.fn(() => Promise.resolve({ data: {} })),
  renameChatSessionApi: vi.fn(() => Promise.resolve({ data: {} })),
  regenerateChatMessageApi: vi.fn(() => Promise.resolve({ data: {} })),
  feedbackChatMessageApi: vi.fn(() => Promise.resolve({ data: {} })),
  uploadChatAttachmentApi: vi.fn(() => Promise.resolve({ data: {} })),
  deleteChatSessionApi: vi.fn(() => Promise.resolve({ data: undefined }))
}))

vi.mock('@/components/agent/useAgentStreaming', () => ({
  useAgentStreaming: () => ({
    streaming: streamingMock.streaming,
    stop: streamingMock.stop,
    sendStreamingMessage: streamingMock.sendStreamingMessage
  })
}))

const sessions = [
  {
    id: 1,
    title: 'JVM 面试冲刺',
    updateTime: '2026-06-02T10:00:00',
    summary: '围绕 GC、线程池和项目表达继续追问。'
  }
]

const multipleSessions = [
  sessions[0],
  {
    id: 2,
    title: '系统设计复盘',
    updateTime: '2026-06-01T09:00:00',
    summary: '整理微服务和缓存追问。'
  }
]

describe('ChatPage', () => {
  beforeEach(() => {
    streamingMock.streaming = false
    streamingMock.stop.mockReset()
    streamingMock.sendStreamingMessage.mockReset()
    streamingMock.sendStreamingMessage.mockResolvedValue(undefined)
    vi.mocked(sendChatApi).mockClear()
    vi.mocked(renameChatSessionApi).mockClear()
    vi.mocked(regenerateChatMessageApi).mockClear()
    vi.mocked(feedbackChatMessageApi).mockClear()
    vi.mocked(uploadChatAttachmentApi).mockClear()
    vi.mocked(deleteChatSessionApi).mockReset()
    vi.mocked(deleteChatSessionApi).mockResolvedValue({ code: 0, message: 'ok', data: undefined })
  })

  it('renders the chat workspace as session, message and context regions with empty prompts', async () => {
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: sessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({ code: 0, message: 'ok', data: [] })

    renderWithProviders(<ChatPage />, { route: '/chat' })

    expect(await screen.findByRole('region', { name: '会话列表' })).toBeInTheDocument()
    expect(screen.getByRole('region', { name: '消息流' })).toBeInTheDocument()
    expect(screen.getByRole('region', { name: '上下文面板' })).toBeInTheDocument()
    expect(await screen.findByText('JVM 面试冲刺')).toBeInTheDocument()

    const messageRegion = screen.getByRole('region', { name: '消息流' })
    expect(within(messageRegion).getByText('先从一个推荐问题开始')).toBeInTheDocument()
    expect(within(messageRegion).getByText('帮我用 STAR 法复盘最近一次面试')).toBeInTheDocument()
    expect(within(messageRegion).getByText('根据这段 JD 生成面试准备清单')).toBeInTheDocument()
  })

  it('switches the active mobile chat panel without losing the desktop regions', async () => {
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: sessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({ code: 0, message: 'ok', data: [] })
    const user = userEvent.setup()

    renderWithProviders(<ChatPage />, { route: '/chat' })

    const sessionsRegion = await screen.findByRole('region', { name: '会话列表' })
    const messageRegion = screen.getByRole('region', { name: '消息流' })
    const contextRegion = screen.getByRole('region', { name: '上下文面板' })

    expect(messageRegion).toHaveAttribute('data-mobile-active', 'true')
    expect(sessionsRegion).toHaveAttribute('data-mobile-active', 'false')
    expect(contextRegion).toHaveAttribute('data-mobile-active', 'false')

    await user.click(screen.getByRole('button', { name: '会话' }))
    expect(sessionsRegion).toHaveAttribute('data-mobile-active', 'true')
    expect(messageRegion).toHaveAttribute('data-mobile-active', 'false')
    expect(contextRegion).toHaveAttribute('data-mobile-active', 'false')

    await user.click(screen.getByRole('button', { name: '上下文' }))
    expect(contextRegion).toHaveAttribute('data-mobile-active', 'true')
    expect(sessionsRegion).toHaveAttribute('data-mobile-active', 'false')
    expect(messageRegion).toHaveAttribute('data-mobile-active', 'false')
  })

  it('renders user and assistant bubbles with markdown and code blocks', async () => {
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: sessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: [
        { id: 'm-user', role: 'user', content: '请解释 HashMap 扩容。' },
        { id: 'm-ai', role: 'assistant', content: '**重点**：先说触发条件。\n\n```java\nMap<String, String> map = new HashMap<>();\n```' }
      ]
    })
    const user = userEvent.setup()

    renderWithProviders(<ChatPage />, { route: '/chat' })

    await user.click(await screen.findByText('JVM 面试冲刺'))

    const messageRegion = screen.getByRole('region', { name: '消息流' })
    expect(await within(messageRegion).findByText('请解释 HashMap 扩容。')).toBeInTheDocument()
    expect(within(messageRegion).getByText('你')).toBeInTheDocument()
    expect(within(messageRegion).getByText('OfferPilot AI')).toBeInTheDocument()
    expect(within(messageRegion).getByText('重点')).toBeInTheDocument()
    const code = within(messageRegion).getByText('Map<String, String> map = new HashMap<>();')
    expect(code.closest('pre')).toHaveClass('chat-code-block')
    expect(code.closest('.chat-message-markdown')).toBeInTheDocument()
  })

  it('prevents blank submissions from reaching streaming or fallback send', async () => {
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: sessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({ code: 0, message: 'ok', data: [] })
    const user = userEvent.setup()

    renderWithProviders(<ChatPage />, { route: '/chat' })

    await screen.findByRole('region', { name: '消息流' })
    await user.type(screen.getByPlaceholderText('输入面试、简历、JD 或刷题问题'), '   ')
    await user.keyboard('{Enter}')

    expect(streamingMock.sendStreamingMessage).not.toHaveBeenCalled()
    expect(sendChatApi).not.toHaveBeenCalled()
  })

  it('shows streaming state, disables duplicate input and stops without fallback send', async () => {
    streamingMock.streaming = true
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: sessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({ code: 0, message: 'ok', data: [] })
    const user = userEvent.setup()

    renderWithProviders(<ChatPage />, { route: '/chat' })

    const input = await screen.findByPlaceholderText('输入面试、简历、JD 或刷题问题')
    expect(input).toBeDisabled()
    expect(screen.getByText('正在生成回复')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '停止生成' }))

    expect(streamingMock.stop).toHaveBeenCalledTimes(1)
    expect(sendChatApi).not.toHaveBeenCalled()
  })

  it('blocks empty session names and renames the selected session', async () => {
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: multipleSessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({ code: 0, message: 'ok', data: [] })
    vi.mocked(renameChatSessionApi).mockResolvedValue({ code: 0, message: 'ok', data: { ...multipleSessions[0], title: 'JVM 高频题复盘' } })
    const user = userEvent.setup()

    renderWithProviders(<ChatPage />, { route: '/chat' })

    await user.click(await screen.findByText('JVM 面试冲刺'))
    await user.click(screen.getByRole('button', { name: '重命名当前会话' }))

    const nameInput = screen.getByLabelText('会话名称')
    await user.clear(nameInput)
    await user.type(nameInput, '   ')
    await user.click(screen.getByRole('button', { name: '保存' }))

    expect(renameChatSessionApi).not.toHaveBeenCalled()
    expect(screen.getByText('会话名称不能为空')).toBeInTheDocument()

    await user.clear(nameInput)
    await user.type(nameInput, 'JVM 高频题复盘')
    await user.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(renameChatSessionApi).toHaveBeenCalledWith(1, 'JVM 高频题复盘')
    })
  })

  it('recovers to the most recent remaining session after deleting the current session', async () => {
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: multipleSessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({ code: 0, message: 'ok', data: [] })
    const user = userEvent.setup()

    renderWithProviders(<ChatPage />, { route: '/chat' })

    await user.click(await screen.findByText('JVM 面试冲刺'))
    await user.click(screen.getByRole('button', { name: '删除当前会话' }))
    await user.click(screen.getByRole('button', { name: /^(确定|OK)$/ }))

    await waitFor(() => {
      expect(deleteChatSessionApi).toHaveBeenCalledWith(1)
      expect(fetchChatMessagesApi).toHaveBeenCalledWith(2)
    })
    expect(within(screen.getByRole('region', { name: '上下文面板' })).getByText('系统设计复盘')).toBeInTheDocument()
  })

  it('keeps the session list and current selection when deleting a session fails', async () => {
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: multipleSessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({ code: 0, message: 'ok', data: [] })
    vi.mocked(deleteChatSessionApi).mockRejectedValue(new Error('delete failed'))
    const user = userEvent.setup()

    renderWithProviders(<ChatPage />, { route: '/chat' })

    await user.click(await screen.findByText('JVM 面试冲刺'))
    await user.click(screen.getByRole('button', { name: '删除当前会话' }))
    await user.click(screen.getByRole('button', { name: /^(确定|OK)$/ }))

    await waitFor(() => expect(deleteChatSessionApi).toHaveBeenCalledWith(1))
    expect(screen.getAllByText('JVM 面试冲刺').length).toBeGreaterThan(0)
    expect(screen.getAllByText('系统设计复盘').length).toBeGreaterThan(0)
    expect(screen.getByText('确认删除这条会话？')).toBeInTheDocument()
  })

  it('copies assistant messages without changing the conversation', async () => {
    const writeText = vi.fn(() => Promise.resolve())
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: sessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: [
        { id: 19, role: 'user', content: '请解释 HashMap 扩容。' },
        { id: 20, role: 'assistant', content: '先说触发条件，再说扩容过程。' }
      ]
    })
    const user = userEvent.setup()

    renderWithProviders(<ChatPage />, { route: '/chat' })

    await user.click(await screen.findByText('JVM 面试冲刺'))
    Object.defineProperty(window.navigator, 'clipboard', {
      configurable: true,
      writable: true,
      value: { writeText }
    })
    await user.click(await screen.findByRole('button', { name: '复制回复' }))

    await waitFor(() => expect(writeText).toHaveBeenCalledWith('先说触发条件，再说扩容过程。'))
    expect(screen.getByText('先说触发条件，再说扩容过程。')).toBeInTheDocument()
  })

  it('regenerates an assistant message without replacing the old answer', async () => {
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: sessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: [
        { id: 19, role: 'user', content: '请解释 HashMap 扩容。' },
        { id: 20, role: 'assistant', content: '旧回答。' }
      ]
    })
    vi.mocked(regenerateChatMessageApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: { messageId: 21, id: 21, role: 'assistant', content: '新回答。', answer: '新回答。' }
    })
    const user = userEvent.setup()

    renderWithProviders(<ChatPage />, { route: '/chat' })

    await user.click(await screen.findByText('JVM 面试冲刺'))
    await user.click(await screen.findByRole('button', { name: '重新生成回复' }))

    await waitFor(() => expect(regenerateChatMessageApi).toHaveBeenCalledWith(20))
    expect(screen.getByText('旧回答。')).toBeInTheDocument()
    expect(screen.getByText('新回答。')).toBeInTheDocument()
  })

  it('keeps the assistant message readable when feedback submission fails', async () => {
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: sessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: [
        { id: 19, role: 'user', content: '请解释 HashMap 扩容。' },
        { id: 20, role: 'assistant', content: '可读回答。' }
      ]
    })
    vi.mocked(feedbackChatMessageApi).mockRejectedValue(new Error('feedback failed'))
    const user = userEvent.setup()

    renderWithProviders(<ChatPage />, { route: '/chat' })

    await user.click(await screen.findByText('JVM 面试冲刺'))
    await user.click(await screen.findByRole('button', { name: '反馈：有帮助' }))

    await waitFor(() => expect(feedbackChatMessageApi).toHaveBeenCalledWith(20, 'positive'))
    expect(screen.getByText('可读回答。')).toBeInTheDocument()
  })

  it('uploads attachments, allows removal and sends uploaded attachment ids', async () => {
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: sessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({ code: 0, message: 'ok', data: [] })
    vi.mocked(uploadChatAttachmentApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: { id: 'file-1', fileId: 'file-1', filename: 'jvm-notes.txt', size: 11, contentType: 'text/plain' }
    })
    const user = userEvent.setup()

    renderWithProviders(<ChatPage />, { route: '/chat' })

    await user.click(await screen.findByText('JVM 面试冲刺'))
    await user.upload(screen.getByLabelText('上传附件'), new File(['hello notes'], 'jvm-notes.txt', { type: 'text/plain' }))

    expect(await screen.findByText('jvm-notes.txt')).toBeInTheDocument()
    expect(screen.getByText('11 B')).toBeInTheDocument()
    expect(screen.getByText('上传成功')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '移除附件 jvm-notes.txt' }))
    expect(screen.queryByText('jvm-notes.txt')).not.toBeInTheDocument()

    await user.upload(screen.getByLabelText('上传附件'), new File(['hello notes'], 'jvm-notes.txt', { type: 'text/plain' }))
    await screen.findByText('上传成功')
    await user.type(screen.getByPlaceholderText('输入面试、简历、JD 或刷题问题'), '结合附件总结重点')
    await user.keyboard('{Enter}')

    await waitFor(() => expect(streamingMock.sendStreamingMessage).toHaveBeenCalledWith(
      expect.objectContaining({
        sessionId: 1,
        message: '结合附件总结重点',
        mode: 'chat',
        answerMode: 'learning',
        attachmentIds: ['file-1'],
        clientMessageId: expect.any(String)
      }),
      expect.any(Function)
    ))
  })

  it('keeps failed attachments visible for retry or removal', async () => {
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: sessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({ code: 0, message: 'ok', data: [] })
    vi.mocked(uploadChatAttachmentApi).mockRejectedValueOnce(new Error('network failed'))
    vi.mocked(uploadChatAttachmentApi).mockResolvedValueOnce({
      code: 0,
      message: 'ok',
      data: { id: 'retry-file', filename: 'jd.pdf', size: 7, contentType: 'application/pdf' }
    })
    const user = userEvent.setup()

    renderWithProviders(<ChatPage />, { route: '/chat' })

    await user.click(await screen.findByText('JVM 面试冲刺'))
    await user.upload(screen.getByLabelText('上传附件'), new File(['pdf doc'], 'jd.pdf', { type: 'application/pdf' }))

    expect(await screen.findByText('上传失败')).toBeInTheDocument()
    expect(screen.getByText('network failed')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '重试上传 jd.pdf' }))
    expect(await screen.findByText('上传成功')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '移除附件 jd.pdf' }))
    expect(screen.queryByText('jd.pdf')).not.toBeInTheDocument()
  })

  it('renders chat sources, tool calls and artifacts in the context panel with fallbacks', async () => {
    vi.mocked(fetchChatSessionsApi).mockResolvedValue({ code: 0, message: 'ok', data: sessions })
    vi.mocked(fetchChatMessagesApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: [
        { id: 19, role: 'user', content: '帮我总结 Redis 缓存风险。' },
        {
          id: 20,
          role: 'assistant',
          content: '重点关注缓存穿透、击穿和雪崩。',
          references: [
            {
              docId: 4003,
              docTitle: 'Redis 缓存治理清单',
              snippet: '缓存穿透、击穿、雪崩的处理方式。',
              businessType: 'knowledge',
              fileType: 'markdown'
            },
            {
              sourceType: 'question',
              sourceId: 1001,
              content: '没有标题但有片段的题目引用。'
            }
          ],
          toolCalls: [
            {
              id: 'tool-1',
              name: 'knowledge.search',
              status: 'SUCCESS',
              totalDurationMs: 123,
              inputSummary: '搜索 Redis 缓存风险',
              outputSummary: '命中 2 条引用'
            }
          ],
          artifacts: [
            {
              id: 'artifact-1',
              title: '复盘清单',
              content: '1. 缓存穿透\n2. 缓存击穿',
              type: 'checklist',
              actionUrl: '/review'
            }
          ]
        }
      ]
    })
    const user = userEvent.setup()

    renderWithProviders(<ChatPage />, { route: '/chat' })

    await user.click(await screen.findByText('JVM 面试冲刺'))

    const contextRegion = screen.getByRole('region', { name: '上下文面板' })
    expect(await within(contextRegion).findByText('Redis 缓存治理清单')).toBeInTheDocument()
    expect(within(contextRegion).getByText('缓存穿透、击穿、雪崩的处理方式。')).toBeInTheDocument()
    expect(within(contextRegion).getByRole('link', { name: '打开 Redis 缓存治理清单' })).toHaveAttribute('href', '/knowledge?docId=4003')
    expect(within(contextRegion).getByText('question #1001')).toBeInTheDocument()
    expect(within(contextRegion).getByText('没有标题但有片段的题目引用。')).toBeInTheDocument()
    expect(within(contextRegion).getByText('knowledge.search')).toBeInTheDocument()
    expect(within(contextRegion).getByText('复盘清单')).toBeInTheDocument()
  })
})
