import { CheckCircleOutlined, ClockCircleOutlined, CloseCircleOutlined, CodeOutlined, CopyOutlined, LinkOutlined, StopOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Input, List, Space, Tag, Timeline, Typography } from 'antd'
import { useMemo, useState } from 'react'
import type { AgentArtifact, AgentMessage, AgentStep, ToolCall } from './agentModel'
export type { AgentArtifact, AgentMessage, AgentStep, ToolCall } from './agentModel'

export function AgentComposer({ onSend, placeholder = '描述你的目标，OfferPilot Agent 会拆解下一步。' }: { onSend?: (value: string) => void; placeholder?: string }) {
  const [value, setValue] = useState('')
  const send = () => { const next = value.trim(); if (!next) return; onSend?.(next); setValue('') }
  return <Input.Search value={value} onChange={(event) => setValue(event.target.value)} onSearch={send} enterButton="发送" size="large" placeholder={placeholder} />
}

export function AgentChatPanel({ messages, onSend, streaming, onStop }: { messages: AgentMessage[]; onSend?: (value: string) => void; streaming?: boolean; onStop?: () => void }) {
  return <Card title="AI 对话" className="surface-card" extra={streaming ? <Button size="small" icon={<StopOutlined />} onClick={onStop}>停止</Button> : null}><List dataSource={messages} locale={{ emptyText: '还没有对话' }} renderItem={(item) => <List.Item><List.Item.Meta title={<Space><Tag color={item.role === 'user' ? 'blue' : item.role === 'tool' ? 'purple' : item.role === 'system' ? 'default' : 'green'}>{item.role}</Tag>{item.status && <Tag>{item.status}</Tag>}</Space>} description={<Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>{item.content}</Typography.Paragraph>} /></List.Item>} /><div style={{ marginTop: 16 }}><AgentComposer onSend={onSend} /></div></Card>
}

const statusIcon = { pending: <ClockCircleOutlined />, running: <ThunderboltOutlined />, success: <CheckCircleOutlined />, failed: <CloseCircleOutlined />, approval_required: <CodeOutlined />, cancelled: <StopOutlined /> }
const statusColor = { pending: 'default', running: 'processing', success: 'success', failed: 'error', approval_required: 'warning', cancelled: 'default' }

export function ToolCallCard({ call }: { call: ToolCall }) {
  return <Card size="small" className="tool-call-card" title={<Space>{statusIcon[call.status]}<span>{call.name}</span><Tag color={statusColor[call.status]}>{call.status}</Tag>{call.durationMs ? <Tag>{call.durationMs}ms</Tag> : null}</Space>}><Typography.Paragraph className="muted-text">{call.error || call.summary || '等待工具返回结构化结果。'}</Typography.Paragraph>{call.params && <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{JSON.stringify(call.params, null, 2)}</pre>}</Card>
}

export function ThoughtTimeline({ steps }: { steps: AgentStep[] }) {
  const items = useMemo(() => steps.map((step) => ({ color: step.status === 'done' ? 'green' : step.status === 'active' ? 'blue' : 'gray', children: <div><strong>{step.title}</strong>{step.description && <div className="muted-text">{step.description}</div>}</div> })), [steps])
  return <Card title="思考链路" className="surface-card"><Timeline items={items} /></Card>
}

export function HumanApprovalBar({ visible = true, loading, onApprove, onReject, onCancel }: { visible?: boolean; loading?: boolean; onApprove?: () => void; onReject?: () => void; onCancel?: () => void }) {
  if (!visible) return null
  return <Alert type="warning" showIcon message="Agent 需要你的确认" description={<Space style={{ marginTop: 12 }}><Button type="primary" loading={loading} onClick={onApprove}>批准执行</Button><Button danger disabled={loading} onClick={onReject}>拒绝</Button><Button disabled={loading} onClick={onCancel}>取消任务</Button></Space>} />
}

export function GeneratedArtifactCard({ title, items }: { title: string; items: Array<string | AgentArtifact> }) {
  return <Card title={title} className="surface-card"><List size="small" dataSource={items} locale={{ emptyText: '暂无产出' }} renderItem={(item) => {
    const artifact: AgentArtifact = typeof item === 'string' ? { id: item, title: item, content: item } : item
    return (
      <List.Item actions={[
        artifact.content ? <Typography.Text key="copy" copyable={{ text: artifact.content, icon: <CopyOutlined /> }} /> : null,
        artifact.actionUrl ? <Button key="open" size="small" icon={<LinkOutlined />} href={artifact.actionUrl}>打开</Button> : null
      ].filter(Boolean)}>
        <List.Item.Meta title={<Space><span>{artifact.title}</span>{artifact.type ? <Tag>{artifact.type}</Tag> : null}</Space>} description={<Typography.Paragraph ellipsis={{ rows: 3, expandable: true }} style={{ marginBottom: 0 }}>{artifact.content}</Typography.Paragraph>} />
      </List.Item>
    )
  }} /></Card>
}
