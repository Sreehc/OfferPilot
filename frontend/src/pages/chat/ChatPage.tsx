import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Modal, Space, Tag } from 'antd'
import { deleteChatSessionApi, fetchChatMessagesApi, fetchChatSessionsApi, sendChatApi } from '@/api/modules/chat'
import { getErrorMessage } from '@/api/client'
import { AgentChatPanel, GeneratedArtifactCard, ThoughtTimeline, ToolCallCard } from '@/components/agent/AgentComponents'
import { mapAgentArtifacts, mapAgentMessages, mapToolCalls } from '@/components/agent/agentModel'
import { useAgentStreaming } from '@/components/agent/useAgentStreaming'
import { DataListCard, formatDateTime, normalizeRecords, pickArray, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function ChatPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [sessionId, setSessionId] = useState<number | null>(null)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const { streaming, stop, sendStreamingMessage } = useAgentStreaming()
  const sessions = useQuery({ queryKey: ['chat', 'sessions'], queryFn: () => fetchChatSessionsApi().then((response) => response.data) })
  const messages = useQuery({
    queryKey: ['chat', 'messages', sessionId],
    queryFn: () => sessionId ? fetchChatMessagesApi(sessionId).then((response) => response.data) : Promise.resolve([]),
    enabled: Boolean(sessionId)
  })
  const sendMessage = useMutation({
    mutationFn: sendChatApi,
    onSuccess: () => message.success('消息已发送'),
    onError: (error) => message.error(getErrorMessage(error, '发送消息失败'))
  })
  const removeSession = useMutation({
    mutationFn: (id: number) => deleteChatSessionApi(id),
    onSuccess: async () => {
      message.success('会话已删除')
      await queryClient.invalidateQueries({ queryKey: ['chat', 'sessions'] })
      if (deleteId === sessionId) setSessionId(null)
      setDeleteId(null)
    },
    onError: (error) => message.error(getErrorMessage(error, '删除会话失败'))
  })

  const sessionRows = normalizeRecords(sessions.data)
  const messageRows = normalizeRecords(messages.data)
  const selectedSession = sessionRows.find((item) => Number(item.id || item.sessionId) === sessionId)
  const toolCalls = mapToolCalls(messageRows.flatMap((item) => normalizeRecords(item.toolCalls || item.tools || item.actions)))
  const thoughtSteps = pickArray<Record<string, unknown>>(selectedSession, ['thoughts', 'steps', 'timeline'])
  const artifacts = mapAgentArtifacts(messageRows.flatMap((item) => normalizeRecords(item.artifacts || item.outputs || item.generatedArtifacts)))
  const agentMessages = mapAgentMessages(messageRows)

  return (
    <ModulePage
      title="AI 问答"
      description="把聊天做成可解释、可追踪、可落地的 Agent 对话界面。"
      metrics={[
        { label: '会话数', value: sessionRows.length, hint: '历史对话' },
        { label: '当前消息', value: messageRows.length, hint: '当前会话内容' },
        { label: '运行状态', value: sessionId ? '活跃' : '未选中', hint: '选中会话后查看消息' },
        { label: 'Agent 输出', value: artifacts.length, hint: '计划、动作、结果' }
      ]}
    >
      <div className="workspace-grid two">
        <AgentChatPanel
          messages={agentMessages}
          streaming={streaming}
          onStop={stop}
          onSend={async (value) => {
            const payload = { sessionId: sessionId || undefined, message: value }
            try {
              await sendStreamingMessage(payload, (delta) => {
                queryClient.setQueryData(['chat', 'messages', sessionId], (current: unknown) => {
                  const currentRows = normalizeRecords(current)
                  const last = currentRows.at(-1)
                  const nextRows = last && last.role === delta.role
                    ? [...currentRows.slice(0, -1), { ...last, content: `${pickText(last as Record<string, unknown>, ['content', 'message', 'text'], '')}${delta.content}` }]
                    : [...currentRows, delta]
                  return nextRows
                })
              })
            } catch {
              sendMessage.mutate(payload)
              await queryClient.invalidateQueries({ queryKey: ['chat', 'messages', sessionId] })
            }
          }}
        />
        <div className="agent-timeline">
          <Card title="当前会话" className="surface-card">
            {sessionId ? (
              <Space direction="vertical" style={{ width: '100%' }}>
                <Tag color="blue">Session #{sessionId}</Tag>
                <Button onClick={() => setSessionId(null)}>取消选中</Button>
              </Space>
            ) : (
              <Tag>未选择会话</Tag>
            )}
          </Card>
          <ThoughtTimeline steps={[
            ...thoughtSteps.map((step) => ({
              title: pickText(step, ['title', 'name', 'step'], '执行步骤'),
              description: pickText(step, ['description', 'summary', 'content'], ''),
              status: String(step.status || '').toLowerCase().includes('done') ? 'done' as const : String(step.status || '').toLowerCase().includes('run') ? 'active' as const : 'wait' as const
            }))
          ]} />
          {toolCalls.map((call) => (
            <ToolCallCard key={call.id} call={call} />
          ))}
          <GeneratedArtifactCard title="可交付结果" items={artifacts} />
          <DataListCard
            title="会话列表"
            data={sessions.data}
            loading={sessions.isLoading}
            error={sessions.error}
            onRetry={() => sessions.refetch()}
            emptyTitle="暂无会话"
            renderItem={(item) => (
              <button type="button" className="agent-queue-card" onClick={() => setSessionId(Number(item.id || item.sessionId))}>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-ink">{pickText(item, ['title', 'sessionTitle'], '未命名会话')}</p>
                    <p className="mt-1 text-xs text-tertiary">{formatDateTime(item.updateTime || item.lastMessageTime)}</p>
                  </div>
                  <StatusTag value={pickText(item, ['mode', 'status'])} />
                </div>
                <p className="mt-3 text-sm text-secondary">{pickText(item, ['summary', 'contextSummary'], '等待详情')}</p>
                <Space className="mt-3" size={8}>
                  <Button size="small" onClick={() => setSessionId(Number(item.id || item.sessionId))}>查看</Button>
                  <Button size="small" danger onClick={(event) => { event.stopPropagation(); setDeleteId(Number(item.id || item.sessionId)); }}>删除</Button>
                </Space>
              </button>
            )}
          />
        </div>
      </div>
      <Modal
        title="删除会话"
        open={deleteId !== null}
        okButtonProps={{ danger: true, loading: removeSession.isPending }}
        onCancel={() => setDeleteId(null)}
        onOk={() => deleteId && removeSession.mutate(deleteId)}
      >
        确认删除这条会话？
      </Modal>
    </ModulePage>
  )
}
