import { useState, type ChangeEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bubble, Conversations, Prompts, Sender } from '@ant-design/x'
import { App as AntApp, Button, Card, Empty, Input, Modal, Space, Tag, Typography } from 'antd'
import { CopyOutlined, DeleteOutlined, DislikeOutlined, LikeOutlined, LinkOutlined, PaperClipOutlined, ReloadOutlined } from '@ant-design/icons'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
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
import { getErrorMessage } from '@/api/client'
import type { AnyRecord } from '@/api/types'
import { GeneratedArtifactCard, ThoughtTimeline, ToolCallList } from '@/components/agent/AgentComponents'
import { mapAgentArtifacts, mapAgentMessages, mapToolCalls } from '@/components/agent/agentModel'
import { useAgentStreaming } from '@/components/agent/useAgentStreaming'
import { formatDateTime, normalizeRecords, pickArray, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

type ChatMobilePanel = 'sessions' | 'messages' | 'context'
type ChatAttachmentStatus = 'uploading' | 'success' | 'error'

interface ChatAttachmentItem {
  uid: string
  file: File
  filename: string
  size: number
  contentType: string
  status: ChatAttachmentStatus
  fileId?: string
  error?: string
}

interface ChatSourceItem {
  id: string
  title: string
  type: string
  snippet: string
  href: string
  fileType: string
}

const chatMobilePanels: Array<{ label: string; value: ChatMobilePanel }> = [
  { label: '会话', value: 'sessions' },
  { label: '对话', value: 'messages' },
  { label: '上下文', value: 'context' }
]

const promptItems = [
  {
    key: 'interview-review',
    label: '帮我用 STAR 法复盘最近一次面试',
    description: '把回答拆成情境、任务、行动、结果，并给出表达改进。'
  },
  {
    key: 'jd-prep',
    label: '根据这段 JD 生成面试准备清单',
    description: '提炼岗位能力点、追问题和准备材料。'
  },
  {
    key: 'resume-polish',
    label: '检查我的简历项目描述是否有量化结果',
    description: '找出表达空泛的位置，并改成更适合 Java 岗位的版本。'
  }
]

function getSessionId(record: Record<string, unknown> | undefined) {
  if (!record) return null
  const id = Number(record.id || record.sessionId)
  return Number.isFinite(id) ? id : null
}

function updateSessionRecords(current: unknown, updater: (records: AnyRecord[]) => AnyRecord[]) {
  if (Array.isArray(current)) return updater(current as AnyRecord[])
  if (!current || typeof current !== 'object') return current
  const value = current as AnyRecord
  for (const key of ['records', 'list', 'items', 'data']) {
    if (Array.isArray(value[key])) return { ...value, [key]: updater(value[key] as AnyRecord[]) }
  }
  return current
}

function updateMessageRecords(current: unknown, updater: (records: AnyRecord[]) => AnyRecord[]) {
  if (Array.isArray(current)) return updater(current as AnyRecord[])
  if (!current || typeof current !== 'object') return current
  const value = current as AnyRecord
  for (const key of ['records', 'list', 'items', 'data']) {
    if (Array.isArray(value[key])) return { ...value, [key]: updater(value[key] as AnyRecord[]) }
  }
  return current
}

function renderMessageContent(content: unknown) {
  const rawContent = String(content || '')
  const rawHtml = String(marked.parse(rawContent, { async: false }))
  const htmlWithCodeClass = rawHtml.replace(/<pre><code/g, '<pre class="chat-code-block"><code')
  return <div className="chat-message-markdown" dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(htmlWithCodeClass) }} />
}

function formatFileSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(size < 10 * 1024 ? 1 : 0)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function readSourceId(record: AnyRecord) {
  const value = record.sourceId ?? record.docId ?? record.questionId ?? record.resumeId ?? record.applicationId ?? record.interviewId ?? record.sessionId ?? record.id
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? String(numberValue) : pickText(record, ['sourceId', 'docId', 'questionId', 'resumeId', 'applicationId', 'interviewId', 'sessionId', 'id'], '')
}

function normalizeSourceType(record: AnyRecord) {
  return pickText(record, ['type', 'sourceType', 'contextType', 'businessType', 'libraryScope'], 'reference').toLowerCase()
}

function buildSourceHref(record: AnyRecord, type: string) {
  const explicitHref = pickText(record, ['url', 'href', 'actionUrl', 'link', 'path'], '')
  if (explicitHref) return explicitHref
  const id = readSourceId(record)
  if (type.includes('knowledge') || type.includes('doc') || record.docId) return record.docId ? `/knowledge?docId=${record.docId}` : '/knowledge'
  if (type.includes('question')) return id ? `/question/${id}` : '/question'
  if (type.includes('resume')) return '/resume'
  if (type.includes('application') || type.includes('job') || type === 'jd') return id ? `/applications/${id}` : '/applications'
  if (type.includes('interview')) return id ? `/interview/detail/${id}` : '/interview'
  return ''
}

function mapChatSources(input: unknown): ChatSourceItem[] {
  return normalizeRecords(input).map((source, index) => {
    const type = normalizeSourceType(source)
    const sourceId = readSourceId(source)
    const title = pickText(source, ['title', 'name', 'sourceTitle', 'documentTitle', 'docTitle', 'questionTitle', 'resumeTitle', 'jobTitle', 'companyName'], '')
    const fallbackTitle = sourceId ? `${type || 'reference'} #${sourceId}` : `引用来源 ${index + 1}`
    return {
      id: String(source.id || source.referenceId || source.docId || source.sourceId || `${type}-${index}`),
      title: title || fallbackTitle,
      type: type || 'reference',
      snippet: pickText(source, ['snippet', 'content', 'summary', 'text', 'excerpt'], '暂无引用片段'),
      href: buildSourceHref(source, type),
      fileType: pickText(source, ['fileType', 'mimeType', 'contentType'], '')
    }
  })
}

function createClientMessageId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `msg-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function SourceReferenceCard({ sources }: { sources: ChatSourceItem[] }) {
  return (
    <Card title="来源引用" className="surface-card">
      {sources.length ? (
        <Space orientation="vertical" style={{ width: '100%' }} size={10}>
          {sources.map((source) => (
            <div key={source.id} className="chat-source-item">
              <div className="chat-source-header">
                <Typography.Text strong>{source.title}</Typography.Text>
                <Space size={4} wrap>
                  <Tag>{source.type}</Tag>
                  {source.fileType ? <Tag>{source.fileType}</Tag> : null}
                </Space>
              </div>
              <Typography.Paragraph className="muted-text" ellipsis={{ rows: 3, expandable: true }} style={{ marginBottom: 0 }}>
                {source.snippet}
              </Typography.Paragraph>
              {source.href ? (
                <Button size="small" icon={<LinkOutlined />} href={source.href} aria-label={`打开 ${source.title}`}>
                  打开
                </Button>
              ) : (
                <Typography.Text className="muted-text">暂无可跳转链接，可根据标题或编号定位。</Typography.Text>
              )}
            </div>
          ))}
        </Space>
      ) : (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={(
            <Space orientation="vertical" size={4}>
              <Typography.Text strong>等待上下文引用</Typography.Text>
              <Typography.Text type="secondary">后续会在这里展示知识库、题目、简历、JD 或面试记录来源。</Typography.Text>
            </Space>
          )}
        />
      )}
    </Card>
  )
}

export function ChatPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [sessionId, setSessionId] = useState<number | null>(null)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [renameId, setRenameId] = useState<number | null>(null)
  const [renameTitle, setRenameTitle] = useState('')
  const [renameError, setRenameError] = useState('')
  const [mobilePanel, setMobilePanel] = useState<ChatMobilePanel>('messages')
  const [draft, setDraft] = useState('')
  const [attachments, setAttachments] = useState<ChatAttachmentItem[]>([])
  const { streaming, stop, sendStreamingMessage } = useAgentStreaming()
  const sessions = useQuery({ queryKey: ['chat', 'sessions'], queryFn: () => fetchChatSessionsApi().then((response) => response.data) })
  const messages = useQuery({
    queryKey: ['chat', 'messages', sessionId],
    queryFn: () => sessionId ? fetchChatMessagesApi(sessionId).then((response) => response.data) : Promise.resolve([]),
    enabled: Boolean(sessionId)
  })
  const sendMessage = useMutation({
    mutationFn: sendChatApi,
    onSuccess: (response) => {
      const responseSessionId = getSessionId(response.data as AnyRecord)
      if (responseSessionId) setSessionId(responseSessionId)
      message.success('消息已发送')
      queryClient.invalidateQueries({ queryKey: ['chat', 'sessions'] })
      queryClient.invalidateQueries({ queryKey: ['chat', 'messages', responseSessionId || sessionId] })
    },
    onError: (error) => message.error(getErrorMessage(error, '发送消息失败'))
  })
  const regenerateMessage = useMutation({
    mutationFn: (messageId: number) => regenerateChatMessageApi(messageId),
    onSuccess: (response, messageId) => {
      const responseRecord = (response.data || {}) as AnyRecord
      const content = pickText(responseRecord, ['content', 'answer', 'message', 'text'], '')
      const id = responseRecord.id || responseRecord.messageId || `regenerated-${messageId}-${Date.now()}`
      if (content) {
        queryClient.setQueryData(['chat', 'messages', sessionId], (current: unknown) => updateMessageRecords(current, (records) => [
          ...records,
          {
            ...responseRecord,
            id,
            role: responseRecord.role || 'assistant',
            content
          }
        ]))
      } else {
        queryClient.invalidateQueries({ queryKey: ['chat', 'messages', sessionId] })
      }
      message.success('已重新生成回复')
    },
    onError: (error) => message.error(getErrorMessage(error, '重新生成失败'))
  })
  const feedbackMessage = useMutation({
    mutationFn: ({ messageId, feedback }: { messageId: number; feedback: 'positive' | 'negative' }) => feedbackChatMessageApi(messageId, feedback),
    onSuccess: (response, variables) => {
      const nextFeedback = pickText(response.data as AnyRecord, ['feedback'], variables.feedback)
      queryClient.setQueryData(['chat', 'messages', sessionId], (current: unknown) => updateMessageRecords(current, (records) => records.map((item) => (
        String(item.id || item.messageId) === String(variables.messageId) ? { ...item, feedback: nextFeedback } : item
      ))))
      message.success('感谢反馈')
    },
    onError: (error) => message.error(getErrorMessage(error, '反馈提交失败'))
  })
  const renameSession = useMutation({
    mutationFn: ({ id, title }: { id: number; title: string }) => renameChatSessionApi(id, title),
    onSuccess: (response, variables) => {
      const nextTitle = pickText(response.data as AnyRecord, ['title', 'sessionTitle'], variables.title)
      message.success('会话已重命名')
      queryClient.setQueryData(['chat', 'sessions'], (current: unknown) => updateSessionRecords(current, (records) => records.map((item) => (
        getSessionId(item) === variables.id ? { ...item, title: nextTitle } : item
      ))))
      queryClient.invalidateQueries({ queryKey: ['chat', 'sessions'] })
      setRenameId(null)
      setRenameTitle('')
      setRenameError('')
    },
    onError: (error) => message.error(getErrorMessage(error, '重命名会话失败'))
  })
  const removeSession = useMutation({
    mutationFn: (id: number) => deleteChatSessionApi(id),
    onSuccess: async () => {
      const deletedId = deleteId
      const deletedIndex = sessionRows.findIndex((item) => getSessionId(item) === deletedId)
      const remainingSessions = sessionRows.filter((item) => getSessionId(item) !== deletedId)
      const fallbackSession = deletedIndex > 0 ? remainingSessions[deletedIndex - 1] : remainingSessions[0]
      const fallbackId = getSessionId(fallbackSession)
      message.success('会话已删除')
      if (deletedId !== null) {
        queryClient.setQueryData(['chat', 'sessions'], (current: unknown) => updateSessionRecords(current, (records) => records.filter((item) => getSessionId(item) !== deletedId)))
      }
      await queryClient.invalidateQueries({ queryKey: ['chat', 'sessions'] })
      if (deletedId === sessionId) setSessionId(fallbackId)
      setDeleteId(null)
    },
    onError: (error) => message.error(getErrorMessage(error, '删除会话失败'))
  })

  const uploadAttachment = async (file: File, existingUid?: string) => {
    const uid = existingUid || `${file.name}-${file.size}-${file.lastModified}-${Date.now()}`
    const uploadingItem: ChatAttachmentItem = {
      uid,
      file,
      filename: file.name,
      size: file.size,
      contentType: file.type,
      status: 'uploading'
    }
    setAttachments((current) => existingUid ? current.map((item) => item.uid === existingUid ? uploadingItem : item) : [...current, uploadingItem])
    try {
      const response = await uploadChatAttachmentApi(file)
      const data = (response.data || {}) as AnyRecord
      const fileId = pickText(data, ['fileId', 'id', 'storageKey'], '')
      setAttachments((current) => current.map((item) => item.uid === uid ? {
        ...item,
        filename: pickText(data, ['filename', 'name', 'originalFilename'], file.name),
        size: Number(data.size || file.size),
        contentType: pickText(data, ['contentType', 'type'], file.type),
        status: 'success',
        fileId,
        error: undefined
      } : item))
    } catch (error) {
      setAttachments((current) => current.map((item) => item.uid === uid ? {
        ...item,
        status: 'error',
        error: getErrorMessage(error, '上传失败')
      } : item))
    }
  }

  const handleAttachmentChange = (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files || [])
    files.forEach((file) => { void uploadAttachment(file) })
    event.target.value = ''
  }

  const retryAttachment = (item: ChatAttachmentItem) => {
    void uploadAttachment(item.file, item.uid)
  }

  const removeAttachment = (uid: string) => {
    setAttachments((current) => current.filter((item) => item.uid !== uid))
  }

  const sessionRows = normalizeRecords(sessions.data)
  const messageRows = normalizeRecords(messages.data)
  const selectedSession = sessionRows.find((item) => Number(item.id || item.sessionId) === sessionId)
  const sources = mapChatSources(messageRows.flatMap((item) => normalizeRecords(item.references || item.sources || item.citations || item.contextSources)))
  const toolCalls = mapToolCalls(messageRows.flatMap((item) => normalizeRecords(item.toolCalls || item.tools || item.actions)))
  const thoughtSteps = pickArray<Record<string, unknown>>(selectedSession, ['thoughts', 'steps', 'timeline'])
  const artifacts = mapAgentArtifacts(messageRows.flatMap((item) => normalizeRecords(item.artifacts || item.outputs || item.generatedArtifacts)))
  const agentMessages = mapAgentMessages(messageRows)
  const messageById = new Map(messageRows.map((item) => [String(item.id || item.messageId), item]))
  const selectedTitle = pickText(selectedSession as Record<string, unknown> | undefined, ['title', 'sessionTitle'], '当前会话')
  const conversationItems = sessionRows.map((item) => {
    const key = String(getSessionId(item) || '')
    const title = pickText(item, ['title', 'sessionTitle'], '未命名会话')
    const time = formatDateTime(item.updateTime || item.lastMessageTime)
    const summary = pickText(item, ['summary', 'contextSummary'], '等待详情')
    return {
      key,
      label: (
        <div className="chat-conversation-label">
          <div className="chat-conversation-title">{title}</div>
          <div className="chat-conversation-meta">{time}</div>
          <div className="chat-conversation-summary">{summary}</div>
        </div>
      )
    }
  })
  const bubbleItems = agentMessages.map((item) => ({
    key: item.id,
    role: item.role,
    content: item.content || ' ',
    extraInfo: messageById.get(String(item.id)),
    header: item.role === 'user' ? '你' : item.role === 'tool' ? '工具' : item.role === 'system' ? '系统' : 'OfferPilot AI'
    }))

  const copyMessage = async (content: string) => {
    const clipboard = window.navigator?.clipboard
    const writeText = clipboard?.writeText
    if (!writeText) {
      message.error('当前浏览器不支持复制')
      return
    }
    try {
      await writeText.call(clipboard, content)
      message.success('已复制回复')
    } catch (error) {
      message.error(getErrorMessage(error, '复制失败'))
    }
  }

  const renderMessageActions = (record?: AnyRecord) => {
    const messageId = Number(record?.id || record?.messageId)
    if (!record || !Number.isFinite(messageId)) return null
    const role = pickText(record, ['role', 'senderRole', 'type'], '')
    if (role !== 'assistant') return null
    const content = pickText(record, ['content', 'answer', 'message', 'text'], '')
    const feedback = pickText(record, ['feedback'], '')
    return (
      <Space className="chat-message-actions" size={6} wrap>
        <Button size="small" icon={<CopyOutlined />} aria-label="复制回复" onClick={() => copyMessage(content)}>复制</Button>
        <Button
          size="small"
          icon={<ReloadOutlined />}
          aria-label="重新生成回复"
          loading={regenerateMessage.isPending}
          onClick={() => regenerateMessage.mutate(messageId)}
        >
          重新生成
        </Button>
        <Button
          size="small"
          icon={<LikeOutlined />}
          aria-label="反馈：有帮助"
          aria-pressed={feedback === 'positive'}
          type={feedback === 'positive' ? 'primary' : 'default'}
          loading={feedbackMessage.isPending}
          onClick={() => feedbackMessage.mutate({ messageId, feedback: 'positive' })}
        >
          有帮助
        </Button>
        <Button
          size="small"
          icon={<DislikeOutlined />}
          aria-label="反馈：没帮助"
          aria-pressed={feedback === 'negative'}
          type={feedback === 'negative' ? 'primary' : 'default'}
          loading={feedbackMessage.isPending}
          onClick={() => feedbackMessage.mutate({ messageId, feedback: 'negative' })}
        >
          没帮助
        </Button>
      </Space>
    )
  }

  const openRenameModal = (targetId: number) => {
    const target = sessionRows.find((item) => getSessionId(item) === targetId)
    setRenameId(targetId)
    setRenameTitle(pickText(target, ['title', 'sessionTitle'], '未命名会话'))
    setRenameError('')
  }

  const confirmRename = () => {
    const nextTitle = renameTitle.trim()
    if (!nextTitle) {
      setRenameError('会话名称不能为空')
      return
    }
    if (renameId !== null) renameSession.mutate({ id: renameId, title: nextTitle })
  }

  const handleSubmit = async (value: string) => {
    const nextMessage = value.trim()
    if (streaming || sendMessage.isPending) return
    if (!nextMessage) return
    if (attachments.some((item) => item.status === 'uploading')) {
      message.error('附件仍在上传，请稍后发送')
      return
    }
    setDraft('')
    const attachmentIds = attachments.filter((item) => item.status === 'success' && item.fileId).map((item) => item.fileId as string)
    const clientMessageId = createClientMessageId()
    const payload = {
      sessionId: sessionId || undefined,
      clientMessageId,
      mode: 'chat',
      answerMode: 'learning',
      message: nextMessage,
      ...(attachmentIds.length ? { attachmentIds } : {})
    }
    try {
      await sendStreamingMessage(payload, (delta) => {
        queryClient.setQueryData(['chat', 'messages', sessionId], (current: unknown) => {
          const currentRows = normalizeRecords(current)
          const last = currentRows.at(-1)
          const lastRole = pickText(last as Record<string, unknown> | undefined, ['role', 'senderRole', 'type'], '')
          const nextRows = last && lastRole === delta.role
            ? [...currentRows.slice(0, -1), { ...last, content: `${pickText(last as Record<string, unknown>, ['content', 'message', 'text'], '')}${delta.content}` }]
            : [...currentRows, delta]
          return nextRows
        })
      })
    } catch {
      sendMessage.mutate(payload)
      await queryClient.invalidateQueries({ queryKey: ['chat', 'messages', sessionId] })
    } finally {
      setAttachments((current) => current.filter((item) => item.status === 'error'))
    }
  }

  return (
    <ModulePage
      title="AI 问答"
      description="把聊天做成可解释、可追踪、可落地的 Agent 对话界面。"
      actions={<Button type="primary" onClick={() => setSessionId(null)}>新建对话</Button>}
      metrics={[
        { label: '会话数', value: sessionRows.length, hint: '历史对话' },
        { label: '当前消息', value: messageRows.length, hint: '当前会话内容' },
        { label: '运行状态', value: sessionId ? '活跃' : '新对话', hint: '选中会话后查看消息' },
        { label: '可交付结果', value: artifacts.length, hint: '计划、动作、结果' }
      ]}
    >
      <div className="chat-mobile-switch" role="group" aria-label="移动端 Chat 面板">
        {chatMobilePanels.map((panel) => (
          <Button
            key={panel.value}
            size="small"
            type={mobilePanel === panel.value ? 'primary' : 'default'}
            aria-label={panel.label}
            aria-pressed={mobilePanel === panel.value}
            onClick={() => setMobilePanel(panel.value)}
          >
            {panel.label}
          </Button>
        ))}
      </div>

      <div className="chat-workbench" data-layout="chat-three-column">
        <aside className="chat-panel chat-sessions-panel" role="region" aria-label="会话列表" data-mobile-active={mobilePanel === 'sessions'}>
          <Card
            title="会话列表"
            className="surface-card chat-panel-card"
            extra={<Button size="small" type="primary" onClick={() => { setSessionId(null); setMobilePanel('messages') }}>新建</Button>}
            loading={sessions.isLoading}
          >
            {conversationItems.length ? (
              <Conversations
                items={conversationItems}
                activeKey={sessionId ? String(sessionId) : undefined}
                onActiveChange={(key) => {
                  setSessionId(Number(key))
                  setMobilePanel('messages')
                }}
                menu={(item) => ({
                  items: [{ key: 'delete', label: '删除会话', danger: true }],
                  onClick: ({ domEvent }) => {
                    domEvent.stopPropagation()
                    setDeleteId(Number(item.key))
                  }
                })}
              />
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无会话" />
            )}
          </Card>
        </aside>

        <main className="chat-panel chat-message-panel" role="region" aria-label="消息流" data-mobile-active={mobilePanel === 'messages'}>
          <Card title="AI 对话" className="surface-card chat-message-card">
            <div className="chat-message-scroll">
              {bubbleItems.length ? (
                <Bubble.List
                  items={bubbleItems}
                  role={{
                    user: { placement: 'end', variant: 'filled', contentRender: renderMessageContent },
                    assistant: { placement: 'start', variant: 'outlined', contentRender: renderMessageContent, footer: (_, info) => renderMessageActions(info.extraInfo as AnyRecord | undefined) },
                    tool: { placement: 'start', variant: 'borderless', contentRender: renderMessageContent },
                    system: { placement: 'start', variant: 'borderless', contentRender: renderMessageContent }
                  }}
                />
              ) : (
                <div className="chat-empty-prompts">
                  <Typography.Title level={4}>先从一个推荐问题开始</Typography.Title>
                  <Typography.Paragraph className="muted-text">选择一个求职训练场景，AI 会把建议、引用和产出拆开呈现。</Typography.Paragraph>
                  <Prompts
                    title={null}
                    items={promptItems}
                    wrap
                    onItemClick={({ data }) => setDraft(String(data.label || ''))}
                  />
                </div>
              )}
            </div>
            <Sender
              value={draft}
              loading={streaming || sendMessage.isPending}
              autoSize={false}
              disabled={streaming || sendMessage.isPending}
              placeholder="输入面试、简历、JD 或刷题问题"
              onChange={(value) => setDraft(value)}
              onSubmit={handleSubmit}
              onCancel={stop}
            />
            <div className="chat-attachment-area">
              <label className="chat-attachment-picker">
                <PaperClipOutlined />
                <span>添加附件</span>
                <input aria-label="上传附件" type="file" multiple onChange={handleAttachmentChange} />
              </label>
              {attachments.length ? (
                <div className="chat-attachment-list" aria-label="附件列表">
                  {attachments.map((item) => (
                    <div key={item.uid} className={`chat-attachment-item is-${item.status}`}>
                      <div className="chat-attachment-main">
                        <Typography.Text strong ellipsis>{item.filename}</Typography.Text>
                        <Space size={6} wrap>
                          <Typography.Text className="muted-text">{formatFileSize(item.size)}</Typography.Text>
                          <Tag color={item.status === 'success' ? 'success' : item.status === 'error' ? 'error' : 'processing'}>
                            {item.status === 'success' ? '上传成功' : item.status === 'error' ? '上传失败' : '上传中'}
                          </Tag>
                          {item.error ? <Typography.Text type="danger">{item.error}</Typography.Text> : null}
                        </Space>
                      </div>
                      <Space size={4}>
                        {item.status === 'error' ? (
                          <Button size="small" icon={<ReloadOutlined />} aria-label={`重试上传 ${item.filename}`} onClick={() => retryAttachment(item)}>重试</Button>
                        ) : null}
                        <Button size="small" icon={<DeleteOutlined />} aria-label={`移除附件 ${item.filename}`} onClick={() => removeAttachment(item.uid)}>移除</Button>
                      </Space>
                    </div>
                  ))}
                </div>
              ) : null}
            </div>
            {streaming ? (
              <div className="chat-streaming-status">
                <Typography.Text>正在生成回复</Typography.Text>
                <Button size="small" onClick={stop}>停止生成</Button>
              </div>
            ) : null}
          </Card>
        </main>

        <aside className="chat-panel chat-context-panel" role="region" aria-label="上下文面板" data-mobile-active={mobilePanel === 'context'}>
          <Space orientation="vertical" style={{ width: '100%' }} size={12}>
            <Card title="当前会话" className="surface-card">
              {sessionId ? (
                <Space orientation="vertical" style={{ width: '100%' }}>
                  <Tag color="blue">{pickText(selectedSession as Record<string, unknown>, ['title', 'sessionTitle'], '当前会话')}</Tag>
                  <Typography.Text className="muted-text">{pickText(selectedSession as Record<string, unknown>, ['summary', 'contextSummary'], '暂无会话摘要')}</Typography.Text>
                  <Space wrap>
                    <Button onClick={() => sessionId !== null && openRenameModal(sessionId)}>重命名当前会话</Button>
                    <Button danger onClick={() => setDeleteId(sessionId)}>删除当前会话</Button>
                    <Button onClick={() => setSessionId(null)}>取消选中</Button>
                  </Space>
                </Space>
              ) : (
                <Tag>新对话（可直接输入或选择推荐问题）</Tag>
              )}
            </Card>
            <SourceReferenceCard sources={sources} />
            <ThoughtTimeline steps={thoughtSteps.map((step) => ({
              title: pickText(step, ['title', 'name', 'step'], '执行步骤'),
              description: pickText(step, ['description', 'summary', 'content'], ''),
              status: String(step.status || '').toLowerCase().includes('done') ? 'done' as const : String(step.status || '').toLowerCase().includes('run') ? 'active' as const : 'wait' as const
            }))} />
            <ToolCallList calls={toolCalls} />
            <GeneratedArtifactCard title="可交付结果" items={artifacts} />
          </Space>
        </aside>
      </div>
      <Modal
        title="删除会话"
        open={deleteId !== null}
        okText="确定"
        cancelText="取消"
        okButtonProps={{ danger: true, loading: removeSession.isPending, 'aria-label': '确定' }}
        onCancel={() => setDeleteId(null)}
        onOk={() => deleteId && removeSession.mutate(deleteId)}
      >
        确认删除这条会话？
      </Modal>
      <Modal
        title="重命名会话"
        open={renameId !== null}
        okText="保存"
        cancelText="取消"
        okButtonProps={{ loading: renameSession.isPending, 'aria-label': '保存' }}
        onCancel={() => {
          setRenameId(null)
          setRenameTitle('')
          setRenameError('')
        }}
        onOk={confirmRename}
      >
        <Space orientation="vertical" style={{ width: '100%' }}>
          <label htmlFor="chat-session-title">会话名称</label>
          <Input
            id="chat-session-title"
            aria-label="会话名称"
            value={renameTitle}
            maxLength={80}
            status={renameError ? 'error' : undefined}
            placeholder={selectedTitle}
            onChange={(event) => {
              setRenameTitle(event.target.value)
              if (renameError) setRenameError('')
            }}
            onPressEnter={confirmRename}
          />
          {renameError ? <Typography.Text type="danger">{renameError}</Typography.Text> : null}
        </Space>
      </Modal>
    </ModulePage>
  )
}
