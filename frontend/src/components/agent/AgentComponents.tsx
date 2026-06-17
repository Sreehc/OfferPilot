import { CheckCircleOutlined, ClockCircleOutlined, CloseCircleOutlined, CodeOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Input, List, Space, Tag, Timeline, Typography } from 'antd'
import { useMemo, useState } from 'react'

export interface AgentMessage { id: string; role: 'user' | 'assistant' | 'system' | 'tool'; content: string; status?: string }
export interface ToolCall { id: string; name: string; status: 'pending' | 'running' | 'success' | 'failed' | 'approval_required'; summary?: string; params?: Record<string, any> }

export function AgentComposer({ onSend, placeholder = '描述你的目标，OfferPilot Agent 会拆解下一步。' }: { onSend?: (value: string) => void; placeholder?: string }) {
  const [value, setValue] = useState('')
  const send = () => { const next = value.trim(); if (!next) return; onSend?.(next); setValue('') }
  return <Input.Search value={value} onChange={(event) => setValue(event.target.value)} onSearch={send} enterButton="发送" size="large" placeholder={placeholder} />
}

export function AgentChatPanel({ messages, onSend }: { messages: AgentMessage[]; onSend?: (value: string) => void }) {
  return <Card title="AI 对话" className="surface-card"><List dataSource={messages} locale={{ emptyText: '还没有对话' }} renderItem={(item) => <List.Item><List.Item.Meta title={<Space><Tag color={item.role === 'user' ? 'blue' : item.role === 'tool' ? 'purple' : 'green'}>{item.role}</Tag>{item.status && <Tag>{item.status}</Tag>}</Space>} description={<Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>{item.content}</Typography.Paragraph>} /></List.Item>} /><div style={{ marginTop: 16 }}><AgentComposer onSend={onSend} /></div></Card>
}

const statusIcon = { pending: <ClockCircleOutlined />, running: <ThunderboltOutlined />, success: <CheckCircleOutlined />, failed: <CloseCircleOutlined />, approval_required: <CodeOutlined /> }
const statusColor = { pending: 'default', running: 'processing', success: 'success', failed: 'error', approval_required: 'warning' }

export function ToolCallCard({ call }: { call: ToolCall }) {
  return <Card size="small" className="tool-call-card" title={<Space>{statusIcon[call.status]}<span>{call.name}</span><Tag color={statusColor[call.status]}>{call.status}</Tag></Space>}><Typography.Paragraph className="muted-text">{call.summary || '等待工具返回结构化结果。'}</Typography.Paragraph>{call.params && <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{JSON.stringify(call.params, null, 2)}</pre>}</Card>
}

export function ThoughtTimeline({ steps }: { steps: Array<{ title: string; description?: string; status?: 'done' | 'active' | 'wait' }> }) {
  const items = useMemo(() => steps.map((step) => ({ color: step.status === 'done' ? 'green' : step.status === 'active' ? 'blue' : 'gray', children: <div><strong>{step.title}</strong>{step.description && <div className="muted-text">{step.description}</div>}</div> })), [steps])
  return <Card title="思考链路" className="surface-card"><Timeline items={items} /></Card>
}

export function HumanApprovalBar({ onApprove, onReject, onCancel }: { onApprove?: () => void; onReject?: () => void; onCancel?: () => void }) {
  return <Alert type="warning" showIcon message="Agent 需要你的确认" description={<Space style={{ marginTop: 12 }}><Button type="primary" onClick={onApprove}>批准执行</Button><Button danger onClick={onReject}>拒绝</Button><Button onClick={onCancel}>取消任务</Button></Space>} />
}

export function GeneratedArtifactCard({ title, items }: { title: string; items: string[] }) {
  return <Card title={title} className="surface-card"><List size="small" dataSource={items} renderItem={(item) => <List.Item>{item}</List.Item>} /></Card>
}
