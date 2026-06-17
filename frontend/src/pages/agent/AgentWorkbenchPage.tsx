import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Descriptions, Form, Input, List, Modal, Select, Space, Tag, Typography } from 'antd'
import { approveAgentRunApi, cancelAgentRunApi, createAgentRunApi, fetchAgentRunDetailApi, fetchAgentRunsApi, rejectAgentRunApi } from '@/api/modules/agent'
import { getErrorMessage } from '@/api/client'
import { AgentComposer, AgentMessage, GeneratedArtifactCard, HumanApprovalBar, ThoughtTimeline, ToolCallCard } from '@/components/agent/AgentComponents'
import { AgentVendorsPanel } from '@/components/agent/AgentVendors'
import { DataListCard, EntitySummary, formatDateTime, normalizeRecords, pickArray, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

const agentOptions = [
  { label: '简历助手', value: 'resume' },
  { label: '投递 Copilot', value: 'application' },
  { label: '面试复盘', value: 'interview' },
  { label: '学习计划', value: 'study-plan' },
  { label: '题目解析', value: 'question' }
]

const triggerOptions = [
  { label: '手动发起', value: 'manual' },
  { label: '页面上下文', value: 'page-context' },
  { label: '训练结果', value: 'training-result' },
  { label: '待审批动作', value: 'approval-flow' }
]

export function AgentWorkbenchPage() {
  const queryClient = useQueryClient()
  const { message } = AntApp.useApp()
  const [createForm] = Form.useForm()
  const [selectedRunId, setSelectedRunId] = useState<string | null>(null)
  const [rejectOpen, setRejectOpen] = useState(false)
  const [rejectReason, setRejectReason] = useState('')
  const runsQuery = useQuery({ queryKey: ['agent', 'runs'], queryFn: () => fetchAgentRunsApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data) })
  const runs = normalizeRecords(runsQuery.data)
  const selectedRun = useQuery({
    queryKey: ['agent', 'run', selectedRunId],
    queryFn: () => selectedRunId ? fetchAgentRunDetailApi(selectedRunId).then((response) => response.data) : Promise.resolve(null),
    enabled: Boolean(selectedRunId)
  })
  const currentRun = selectedRun.data as any
  const runThoughts = pickArray<Record<string, unknown>>(currentRun, ['thoughts', 'steps', 'timeline'])
  const runToolCalls = pickArray<Record<string, unknown>>(currentRun, ['toolCalls', 'tools', 'actions'])
  const runArtifacts = pickArray<Record<string, unknown>>(currentRun, ['artifacts', 'outputs', 'generatedArtifacts'])
  const pendingRuns = useMemo(() => runs.filter((run: any) => String(run.status || '').includes('PENDING') || String(run.status || '').includes('APPROVAL')), [runs])

  const createRun = useMutation({
    mutationFn: createAgentRunApi,
    onSuccess: async (response) => {
      message.success('Agent Run 已创建')
      createForm.resetFields()
      await queryClient.invalidateQueries({ queryKey: ['agent', 'runs'] })
      const nextId = String((response.data as any)?.id || (response.data as any)?.runId || '')
      if (nextId) setSelectedRunId(nextId)
    },
    onError: (error) => message.error(getErrorMessage(error, '创建 Agent Run 失败'))
  })
  const approveRun = useMutation({
    mutationFn: (runId: string) => approveAgentRunApi(runId, { reason: 'approved from workbench' }),
    onSuccess: () => {
      message.success('已批准')
      queryClient.invalidateQueries({ queryKey: ['agent'] })
    },
    onError: (error) => message.error(getErrorMessage(error, '批准失败'))
  })
  const rejectRun = useMutation({
    mutationFn: ({ runId, reason }: { runId: string; reason?: string }) => rejectAgentRunApi(runId, { reason }),
    onSuccess: () => {
      message.success('已拒绝')
      setRejectOpen(false)
      setRejectReason('')
      queryClient.invalidateQueries({ queryKey: ['agent'] })
    },
    onError: (error) => message.error(getErrorMessage(error, '拒绝失败'))
  })
  const cancelRun = useMutation({
    mutationFn: (runId: string) => cancelAgentRunApi(runId, { reason: 'cancelled from workbench' }),
    onSuccess: () => {
      message.success('已取消')
      queryClient.invalidateQueries({ queryKey: ['agent'] })
    },
    onError: (error) => message.error(getErrorMessage(error, '取消失败'))
  })

  const currentMessages: AgentMessage[] = normalizeRecords(currentRun?.messages || currentRun?.chatMessages).map((item, index) => ({
    id: String(item.id || item.messageId || index),
    role: (item.role || item.senderRole || 'assistant') as AgentMessage['role'],
    content: pickText(item, ['content', 'message', 'text'], ''),
    status: item.status
  }))

  const detailBlocks = currentRun ? (
    <Space direction="vertical" style={{ width: '100%' }} size={16}>
      <Card title="Run 详情" className="surface-card">
        <EntitySummary record={currentRun} fields={[
          { label: 'Run ID', keys: ['id', 'runId'] },
          { label: '类型', keys: ['agentType'] },
          { label: '状态', keys: ['status'], tag: true },
          { label: '来源', keys: ['triggerSource'] },
          { label: '创建时间', keys: ['createTime', 'createdAt'] }
        ]} />
      </Card>
      <Card title="运行说明" className="surface-card">
        <Typography.Paragraph className="muted-text" style={{ marginBottom: 0 }}>{pickText(currentRun, ['summary', 'description', 'resultSummary'], '暂无运行说明')}</Typography.Paragraph>
      </Card>
      <Card title="结构化信息" className="surface-card">
        <Descriptions column={1} items={[
          { label: '审批状态', children: <StatusTag value={pickText(currentRun, ['approvalStatus'])} /> },
          { label: '触发上下文', children: pickText(currentRun, ['contextSummary', 'contextRefs'], '-') },
          { label: '更新时间', children: formatDateTime(currentRun.updateTime || currentRun.lastUpdatedAt) }
        ]} />
      </Card>
      <HumanApprovalBar
        onApprove={() => approveRun.mutate(String(currentRun.id || currentRun.runId))}
        onReject={() => setRejectOpen(true)}
        onCancel={() => cancelRun.mutate(String(currentRun.id || currentRun.runId))}
      />
      <GeneratedArtifactCard title="产出" items={runArtifacts.length ? runArtifacts.map((item) => pickText(item, ['title', 'name', 'summary', 'content'])) : [pickText(currentRun, ['outputTitle', 'summary'], '暂无产出')]} />
    </Space>
  ) : null

  return (
    <ModulePage
      title="Agent 工作台"
      description="统一发起任务、查看 run、处理审批、追踪工具调用并消费结果。"
      actions={<Space><Button type="primary" onClick={() => createForm.submit()}>发起 Run</Button></Space>}
      metrics={[
        { label: 'Run 总数', value: runs.length, hint: '最近执行记录' },
        { label: '待审批', value: pendingRuns.length, hint: '需要人工确认的动作' },
        { label: '运行中', value: runs.filter((run: any) => String(run.status || '').includes('RUN')).length, hint: '正在处理' },
        { label: '已完成', value: runs.filter((run: any) => String(run.status || '').includes('SUCCESS') || String(run.status || '').includes('DONE')).length, hint: '可消费结果' }
      ]}
      side={<AgentVendorsPanel />}
    >
      <div className="workspace-grid">
        <Card title="快速发起" className="surface-card">
          <Form
            form={createForm}
            layout="vertical"
            initialValues={{ agentType: 'resume', triggerSource: 'manual', input: '{}' }}
            onFinish={(values) => {
              let input: Record<string, any> = {}
              try { input = values.input ? JSON.parse(values.input) : {} } catch { input = { raw: values.input } }
              createRun.mutate({
                agentType: values.agentType,
                triggerSource: values.triggerSource,
                input,
                contextRefs: values.contextRefs ? String(values.contextRefs).split(',').map((item: string) => ({ ref: item.trim() })).filter((item: { ref: string }) => item.ref) : []
              })
            }}
          >
            <div className="workspace-grid two">
              <Form.Item label="Agent 类型" name="agentType"><Select options={agentOptions} /></Form.Item>
              <Form.Item label="触发来源" name="triggerSource"><Select options={triggerOptions} /></Form.Item>
            </div>
            <Form.Item label="上下文引用" name="contextRefs" extra="多个值用英文逗号分隔，例如 interview:latest, resume:current"><Input /></Form.Item>
            <Form.Item label="输入内容" name="input" extra="可填写 JSON。后端会收到结构化 input。"><Input.TextArea rows={4} /></Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={createRun.isPending}>创建 Run</Button>
              <Button onClick={() => createForm.resetFields()}>重置</Button>
            </Space>
          </Form>
        </Card>

        <div className="workspace-grid two">
          <DataListCard
            title="运行列表"
            data={runsQuery.data}
            loading={runsQuery.isLoading}
            error={runsQuery.error}
            onRetry={() => runsQuery.refetch()}
            emptyTitle="当前没有 Agent Run"
            actions={<Tag color="blue">按最新结果排序</Tag>}
            renderItem={(item) => (
              <button type="button" className="agent-queue-card" onClick={() => setSelectedRunId(String(item.id || item.runId))}>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-ink">{pickText(item, ['title', 'agentType'], 'agent')}</p>
                    <p className="mt-1 text-xs uppercase tracking-[0.18em] text-tertiary">{pickText(item, ['triggerSource'], 'manual')}</p>
                  </div>
                  <StatusTag value={pickText(item, ['status'])} />
                </div>
                <p className="mt-3 text-sm leading-6 text-secondary">{pickText(item, ['summary', 'approvalSummary', 'resultSummary'], '等待详情')}</p>
              </button>
            )}
          />
          <div className="agent-timeline">
            <Card title="当前选中" className="surface-card">
              {currentRun ? (
                <Space direction="vertical" style={{ width: '100%' }} size={12}>
                  <ThoughtTimeline steps={runThoughts.length ? runThoughts.map((step) => ({
                    title: pickText(step, ['title', 'name', 'step'], '执行步骤'),
                    description: pickText(step, ['description', 'summary', 'content'], ''),
                    status: String(step.status || '').toLowerCase().includes('done') ? 'done' : String(step.status || '').toLowerCase().includes('run') ? 'active' : 'wait'
                  })) : [{ title: '暂无步骤', description: '等待后端返回 run steps', status: 'wait' }]} />
                  {runToolCalls.length ? runToolCalls.map((call, index) => (
                    <ToolCallCard key={String(call.id || call.name || index)} call={{
                      id: String(call.id || index),
                      name: pickText(call, ['name', 'toolName', 'type'], 'tool'),
                      status: String(call.status || '').toUpperCase().includes('APPROVAL') ? 'approval_required' : String(call.status || '').toUpperCase().includes('FAIL') ? 'failed' : String(call.status || '').toUpperCase().includes('SUCCESS') ? 'success' : 'running',
                      summary: pickText(call, ['summary', 'result', 'description'], '等待工具调用结果'),
                      params: (call.params || call.arguments || call.input) as Record<string, any> | undefined
                    }} />
                  )) : <ToolCallCard call={{
                    id: 'agent-tool',
                    name: pickText(currentRun, ['agentType'], 'agent-tool'),
                    status: String(currentRun.status || '').includes('APPROVAL') ? 'approval_required' : String(currentRun.status || '').includes('FAIL') ? 'failed' : 'running',
                    summary: pickText(currentRun, ['approvalSummary', 'summary', 'resultSummary'], '等待工具调用结果'),
                    params: (currentRun.input || { contextRefs: currentRun.contextRefs || [] }) as Record<string, any>
                  }} />}
                  <Card title="运行详情" className="surface-card">
                    <Descriptions column={1} items={[
                      { label: 'Run ID', children: String(currentRun.id || currentRun.runId || '-') },
                      { label: '类型', children: pickText(currentRun, ['agentType']) },
                      { label: '创建时间', children: formatDateTime(currentRun.createTime || currentRun.createdAt) }
                    ]} />
                  </Card>
                </Space>
              ) : (
                <Typography.Paragraph className="muted-text">从左侧选择一个 run 查看详情。</Typography.Paragraph>
              )}
            </Card>
            {detailBlocks}
            <Card title="对话 / 工具回流" className="surface-card">
              <AgentComposer onSend={(value) => createRun.mutate({ agentType: 'resume', triggerSource: 'manual', input: { prompt: value } })} />
              <List style={{ marginTop: 16 }} dataSource={currentMessages} renderItem={(item) => <List.Item>{item.role}：{item.content}</List.Item>} />
            </Card>
          </div>
        </div>
      </div>

      <Modal
        open={rejectOpen}
        title="拒绝 Run"
        okText="确认拒绝"
        onCancel={() => setRejectOpen(false)}
        onOk={() => currentRun && rejectRun.mutate({ runId: String(currentRun.id || currentRun.runId), reason: rejectReason })}
      >
        <Input.TextArea value={rejectReason} onChange={(event) => setRejectReason(event.target.value)} rows={4} placeholder="填写拒绝原因" />
      </Modal>
    </ModulePage>
  )
}
