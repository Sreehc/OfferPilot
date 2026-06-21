import { CheckCircleOutlined, ClockCircleOutlined, CloseCircleOutlined, CodeOutlined, CopyOutlined, LinkOutlined, StopOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Descriptions, Empty, Input, List, Space, Tag, Timeline, Typography } from 'antd'
import type { ReactNode } from 'react'
import { useMemo, useState } from 'react'
import { getAgentRunStatusMeta, getAgentStepStatusMeta } from './agentModel'
import type { AgentArtifact, AgentMessage, AgentRunStatus, AgentStep, ToolCall } from './agentModel'
import { formatDateTime } from '@/modules/common'
export type { AgentArtifact, AgentMessage, AgentStep, ToolCall } from './agentModel'

export function AgentComposer({ onSend, placeholder = '描述你的目标，OfferPilot Agent 会拆解下一步。' }: { onSend?: (value: string) => void; placeholder?: string }) {
  const [value, setValue] = useState('')
  const send = () => { const next = value.trim(); if (!next) return; onSend?.(next); setValue('') }
  return <Input.Search value={value} onChange={(event) => setValue(event.target.value)} onSearch={send} enterButton="发送" size="large" placeholder={placeholder} />
}

export function AgentChatPanel({ messages, onSend, streaming, onStop }: { messages: AgentMessage[]; onSend?: (value: string) => void; streaming?: boolean; onStop?: () => void }) {
  return <Card title="AI 对话" className="surface-card" extra={streaming ? <Button size="small" icon={<StopOutlined />} onClick={onStop}>停止</Button> : null}><List dataSource={messages} locale={{ emptyText: '还没有对话' }} renderItem={(item) => <List.Item><List.Item.Meta title={<Space><Tag color={item.role === 'user' ? 'blue' : item.role === 'tool' ? 'purple' : item.role === 'system' ? 'default' : 'green'}>{item.role}</Tag>{item.status && <Tag>{item.status}</Tag>}</Space>} description={<Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>{item.content}</Typography.Paragraph>} /></List.Item>} /><div style={{ marginTop: 16 }}><AgentComposer onSend={onSend} /></div></Card>
}

const statusIcon: Record<AgentRunStatus, ReactNode> = { pending: <ClockCircleOutlined />, running: <ThunderboltOutlined />, success: <CheckCircleOutlined />, failed: <CloseCircleOutlined />, approval_required: <CodeOutlined />, cancelled: <StopOutlined /> }

export function AgentStatusTag({ value }: { value?: unknown }) {
  const meta = getAgentRunStatusMeta(value)
  return <Tag color={meta.color}>{meta.label}</Tag>
}

export function ToolCallCard({ call }: { call: ToolCall }) {
  const statusMeta = getAgentRunStatusMeta(call.rawStatus || call.status)
  const duration = call.totalDurationMs ?? call.durationMs
  const durationText = typeof duration === 'number' ? `${duration}ms` : '未记录'
  const retryText = typeof call.retryCount === 'number' ? String(call.retryCount) : '未记录'
  const phaseEntries = Object.entries(call.phaseDurations || {})
  return (
    <Card
      size="small"
      className="tool-call-card"
      title={<Space>{statusIcon[statusMeta.status]}<span>{call.name}</span><Tag color={statusMeta.color}>{statusMeta.label}</Tag><Tag>耗时 {durationText}</Tag></Space>}
    >
      <Typography.Paragraph className="muted-text">
        {call.errorMessage || call.error || call.outputSummary || call.summary || '等待工具返回结构化结果。'}
      </Typography.Paragraph>
      <Descriptions
        size="small"
        column={1}
        items={[
          { label: '开始时间', children: call.startedAt || '未记录' },
          { label: '结束时间', children: call.endedAt || '未记录' },
          { label: '重试次数', children: retryText },
          { label: '输入摘要', children: call.inputSummary || '未记录' },
          { label: '输出摘要', children: call.outputSummary || '未记录' },
          { label: '错误类型', children: call.errorType || '未记录' },
          { label: '阶段耗时', children: phaseEntries.length ? <Space wrap>{phaseEntries.map(([key, value]) => <Tag key={key}>{key}: {value}ms</Tag>)}</Space> : '未记录' }
        ]}
      />
      {call.params && <pre style={{ margin: 0, marginTop: 12, whiteSpace: 'pre-wrap' }}>{JSON.stringify(call.params, null, 2)}</pre>}
    </Card>
  )
}

export function ToolCallList({ calls }: { calls: ToolCall[] }) {
  return (
    <Card title="工具调用" className="surface-card">
      {calls.length ? (
        <Space orientation="vertical" style={{ width: '100%' }} size={12}>
          {calls.map((call) => <ToolCallCard key={call.id} call={call} />)}
        </Space>
      ) : (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={<Space orientation="vertical" size={4}><Typography.Text strong>暂无工具调用</Typography.Text><Typography.Text type="secondary">等待后端返回 toolCalls 后展示真实工具执行记录。</Typography.Text></Space>} />
      )}
    </Card>
  )
}

function TimelineStepDescription({ description }: { description?: string }) {
  const [expanded, setExpanded] = useState(false)
  if (!description) return <Typography.Text className="muted-text">暂无步骤说明</Typography.Text>
  const isLong = description.length > 120
  const visibleText = isLong && !expanded ? `${description.slice(0, 120)}...` : description
  return (
    <Typography.Paragraph className="muted-text" style={{ marginBottom: 0, marginTop: 8 }}>
      {visibleText}
      {isLong ? (
        <Button type="link" size="small" className="agent-timeline-expand" onClick={() => setExpanded((value) => !value)}>
          {expanded ? '收起' : '展开'}
        </Button>
      ) : null}
    </Typography.Paragraph>
  )
}

export function ThoughtTimeline({ steps }: { steps: AgentStep[] }) {
  const items = useMemo(() => steps.map((step) => {
    const meta = getAgentStepStatusMeta(step.rawStatus || step.status)
    const duration = typeof step.durationMs === 'number' ? `耗时 ${step.durationMs}ms` : '耗时未记录'
    const time = formatDateTime(step.time)
    const isCurrent = step.status === 'active'
    return {
      color: meta.color,
      content: (
        <div className={`agent-timeline-step ${isCurrent ? 'is-current' : ''}`}>
          <Space size={6} wrap>
            <strong>{step.title}</strong>
            <Tag color={meta.color}>{meta.label}</Tag>
            <Tag>{step.type || '通用步骤'}</Tag>
            <Tag>{duration}</Tag>
            {time !== '-' ? <Tag>{time}</Tag> : null}
            {isCurrent ? <Tag color="processing">当前步骤</Tag> : null}
          </Space>
          <TimelineStepDescription description={step.description} />
        </div>
      )
    }
  }), [steps])
  return (
    <Card title="思考链路" className="surface-card">
      {steps.length ? (
        <Timeline items={items} />
      ) : (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={<Space orientation="vertical" size={4}><Typography.Text strong>暂无执行步骤</Typography.Text><Typography.Text type="secondary">等待后端返回 steps 后展示 Agent 执行链路。</Typography.Text></Space>} />
      )}
    </Card>
  )
}

export function HumanApprovalBar({ visible = true, loading, onApprove, onReject, onCancel }: { visible?: boolean; loading?: boolean; onApprove?: () => void; onReject?: () => void; onCancel?: () => void }) {
  if (!visible) return null
  return <Alert type="warning" showIcon title="Agent 需要你的确认" description={<Space style={{ marginTop: 12 }}><Button type="primary" aria-label="批准执行" loading={loading} onClick={onApprove}>批准执行</Button><Button danger aria-label="拒绝" disabled={loading} onClick={onReject}>拒绝</Button><Button aria-label="取消任务" disabled={loading} onClick={onCancel}>取消任务</Button></Space>} />
}

export function GeneratedArtifactCard({ title, items }: { title: string; items: Array<string | AgentArtifact> }) {
  const copyArtifact = (content: string) => {
    const clipboard = window.navigator?.clipboard
    const writeText = clipboard?.writeText
    if (!content || !writeText) return
    void writeText.call(clipboard, content).catch(() => undefined)
  }
  if (!items.length) {
    return <Card title={title} className="surface-card"><Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={<Space orientation="vertical" size={4}><Typography.Text strong>暂无产出</Typography.Text><Typography.Text type="secondary">等待后端返回 artifacts 后展示结构化产物。</Typography.Text></Space>} /></Card>
  }
  return <Card title={title} className="surface-card"><List size="small" dataSource={items} locale={{ emptyText: '暂无产出' }} renderItem={(item) => {
    const artifact: AgentArtifact = typeof item === 'string' ? { id: item, title: item, content: item } : item
    return (
      <List.Item actions={[
        artifact.content ? <Button key="copy" size="small" icon={<CopyOutlined />} aria-label={`复制产物：${artifact.title}`} onClick={() => copyArtifact(artifact.content)}>复制</Button> : null,
        artifact.actionUrl ? <Button key="open" size="small" icon={<LinkOutlined />} aria-label={`打开${artifact.title}`} href={artifact.actionUrl}>打开</Button> : null
      ].filter(Boolean)}>
        <List.Item.Meta title={<Space><span>{artifact.title}</span>{artifact.type ? <Tag>{artifact.type}</Tag> : null}</Space>} description={<Typography.Paragraph ellipsis={{ rows: 3, expandable: true }} style={{ marginBottom: 0 }}>{artifact.content}</Typography.Paragraph>} />
      </List.Item>
    )
  }} /></Card>
}
