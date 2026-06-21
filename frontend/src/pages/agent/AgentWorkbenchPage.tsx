import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Descriptions, Form, Input, List, Modal, Select, Space, Tag, Typography } from 'antd'
import { approveAgentRunApi, cancelAgentRunApi, createAgentRunApi, fetchAgentRunDetailApi, fetchAgentRunsApi, rejectAgentRunApi } from '@/api/modules/agent'
import { getErrorMessage } from '@/api/client'
import { AgentComposer, AgentStatusTag, GeneratedArtifactCard, HumanApprovalBar, ThoughtTimeline, ToolCallList } from '@/components/agent/AgentComponents'
import { getAgentApprovalStatusMeta, mapAgentArtifacts, mapAgentMessages, mapAgentSteps, mapToolCalls, normalizeAgentStatus } from '@/components/agent/agentModel'
import { DataListCard, formatDateTime, normalizeRecords, pickArray, pickText } from '@/modules/common'
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

type QueueStatusFilter = 'all' | 'approval_required' | 'running' | 'failed' | 'success'

const queueStatusFilters: Array<{ label: string; value: QueueStatusFilter }> = [
  { label: '全部', value: 'all' },
  { label: '待审批', value: 'approval_required' },
  { label: '运行中', value: 'running' },
  { label: '失败', value: 'failed' },
  { label: '已完成', value: 'success' }
]

function resolveQueueRunStatus(run: any) {
  const approvalStatus = getAgentApprovalStatusMeta(run?.approvalStatus).status
  if (approvalStatus === 'approval_required') return approvalStatus
  return normalizeAgentStatus(run?.status || run?.runStatus)
}

export function AgentWorkbenchPage() {
  const queryClient = useQueryClient()
  const { message, modal } = AntApp.useApp()
  const [createForm] = Form.useForm()
  const [selectedRunId, setSelectedRunId] = useState<string | null>(null)
  const [rejectOpen, setRejectOpen] = useState(false)
  const [rejectReason, setRejectReason] = useState('')
  const [rejectReasonError, setRejectReasonError] = useState('')
  const [queueStatusFilter, setQueueStatusFilter] = useState<QueueStatusFilter>('all')
  const runsQuery = useQuery({ queryKey: ['agent', 'runs'], queryFn: () => fetchAgentRunsApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data) })
  const runs = normalizeRecords(runsQuery.data)
  const selectedRun = useQuery({
    queryKey: ['agent', 'run', selectedRunId],
    queryFn: () => selectedRunId ? fetchAgentRunDetailApi(selectedRunId).then((response) => response.data) : Promise.resolve(null),
    enabled: Boolean(selectedRunId)
  })
  const currentRun = selectedRun.data as any
  const runThoughts = mapAgentSteps(pickArray<Record<string, unknown>>(currentRun, ['thoughts', 'steps', 'timeline']))
  const runToolCalls = mapToolCalls(pickArray<Record<string, unknown>>(currentRun, ['toolCalls', 'tools', 'actions']))
  const runArtifacts = mapAgentArtifacts(pickArray<Record<string, unknown>>(currentRun, ['artifacts', 'outputs', 'generatedArtifacts']))
  const pendingRuns = useMemo(() => runs.filter((run: any) => resolveQueueRunStatus(run) === 'approval_required'), [runs])
  const runningRuns = useMemo(() => runs.filter((run: any) => resolveQueueRunStatus(run) === 'running'), [runs])
  const completedRuns = useMemo(() => runs.filter((run: any) => resolveQueueRunStatus(run) === 'success'), [runs])
  const filteredRuns = useMemo(() => {
    if (queueStatusFilter === 'all') return runs
    return runs.filter((run: any) => resolveQueueRunStatus(run) === queueStatusFilter)
  }, [queueStatusFilter, runs])

  const createRun = useMutation({
    mutationFn: createAgentRunApi,
    onSuccess: async (response) => {
      message.success('AI 任务已创建')
      createForm.resetFields()
      await queryClient.invalidateQueries({ queryKey: ['agent', 'runs'] })
      const nextId = String((response.data as any)?.id || (response.data as any)?.runId || '')
      if (nextId) setSelectedRunId(nextId)
    },
    onError: (error) => {
      message.error(getErrorMessage(error, '创建 AI 任务失败'))
    }
  })
  const approveRun = useMutation({
    mutationFn: (runId: string) => approveAgentRunApi(runId, { reason: '工作台批准' }),
    onSuccess: () => {
      message.success('已批准')
      queryClient.invalidateQueries({ queryKey: ['agent'] })
    },
    onError: (error) => {
      message.error(getErrorMessage(error, '批准失败'))
    }
  })
  const rejectRun = useMutation({
    mutationFn: ({ runId, reason }: { runId: string; reason?: string }) => rejectAgentRunApi(runId, { reason }),
    onSuccess: () => {
      message.success('已拒绝')
      setRejectOpen(false)
      setRejectReason('')
      setRejectReasonError('')
      queryClient.invalidateQueries({ queryKey: ['agent'] })
    },
    onError: (error) => {
      message.error(getErrorMessage(error, '拒绝失败'))
    }
  })
  const cancelRun = useMutation({
    mutationFn: (runId: string) => cancelAgentRunApi(runId, { reason: '工作台取消' }),
    onSuccess: () => {
      message.success('已取消')
      queryClient.invalidateQueries({ queryKey: ['agent'] })
    },
    onError: (error) => {
      message.error(getErrorMessage(error, '取消失败'))
    }
  })

  const currentMessages = mapAgentMessages(currentRun?.messages || currentRun?.chatMessages)
  const currentApprovalMeta = getAgentApprovalStatusMeta(currentRun?.approvalStatus)
  const currentRunStatus = normalizeAgentStatus(currentRun?.status || currentRun?.runStatus)
  const currentNeedsApproval = currentApprovalMeta.status === 'approval_required' || currentRunStatus === 'approval_required'

  const detailBlocks = currentRun ? (
    <Space orientation="vertical" style={{ width: '100%' }} size={16}>
      <Card title="任务详情" className="surface-card">
        <Descriptions column={1} items={[
          { label: '任务 ID', children: String(currentRun.id || currentRun.runId || '-') },
          { label: '类型', children: pickText(currentRun, ['agentType']) },
          { label: '状态', children: <AgentStatusTag value={currentRun.status || currentRun.runStatus} /> },
          { label: '来源', children: pickText(currentRun, ['triggerSource']) },
          { label: '创建时间', children: formatDateTime(currentRun.createTime || currentRun.createdAt) }
        ]} />
      </Card>
      <Card title="运行说明" className="surface-card">
        <Typography.Paragraph className="muted-text" style={{ marginBottom: 0 }}>{pickText(currentRun, ['summary', 'description', 'resultSummary'], '暂无运行说明')}</Typography.Paragraph>
      </Card>
      <Card title="结构化信息" className="surface-card">
        <Descriptions column={1} items={[
          { label: '审批状态', children: <Tag color={currentApprovalMeta.color}>{currentApprovalMeta.label}</Tag> },
          { label: '触发上下文', children: pickText(currentRun, ['contextSummary', 'contextRefs'], '-') },
          { label: '更新时间', children: formatDateTime(currentRun.updateTime || currentRun.lastUpdatedAt) }
        ]} />
      </Card>
      <HumanApprovalBar
        visible={currentNeedsApproval}
        loading={approveRun.isPending || rejectRun.isPending || cancelRun.isPending}
        onApprove={() => approveRun.mutate(String(currentRun.id || currentRun.runId))}
        onReject={() => {
          setRejectReason('')
          setRejectReasonError('')
          setRejectOpen(true)
        }}
        onCancel={() => modal.confirm({
          title: '取消该任务？',
          content: '取消后该任务将停止执行，且不可恢复。',
          okText: '取消任务',
          okButtonProps: { danger: true },
          cancelText: '再想想',
          onOk: () => cancelRun.mutateAsync(String(currentRun.id || currentRun.runId))
        })}
      />
      <GeneratedArtifactCard title="产出" items={runArtifacts} />
    </Space>
  ) : null

  return (
    <ModulePage
      title="Agent 工作台"
      description="统一发起 AI 任务、查看进度、处理审批、追踪工具调用并消费结果。"
      actions={<Space><Button type="primary" onClick={() => createForm.submit()}>发起任务</Button></Space>}
      metrics={[
        { label: '任务总数', value: runs.length, hint: '最近执行记录' },
        { label: '待审批', value: pendingRuns.length, hint: '需要人工确认的动作' },
        { label: '运行中', value: runningRuns.length, hint: '正在处理' },
        { label: '已完成', value: completedRuns.length, hint: '可消费结果' }
      ]}
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
            <Form.Item label="上下文引用" name="contextRefs" extra="可选，多个值用英文逗号分隔，例如 interview:latest, resume:current"><Input /></Form.Item>
            <Form.Item label="补充信息" name="input" extra="可选，描述你的具体需求或填写结构化内容。"><Input.TextArea rows={4} /></Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={createRun.isPending}>创建任务</Button>
              <Button onClick={() => createForm.resetFields()}>重置</Button>
            </Space>
          </Form>
        </Card>

        <div className="workspace-grid two">
          <DataListCard
            title="运行列表"
            data={filteredRuns}
            loading={runsQuery.isLoading}
            error={runsQuery.error}
            onRetry={() => runsQuery.refetch()}
            emptyTitle={queueStatusFilter === 'all' ? '当前没有 AI 任务' : '没有符合筛选的 AI 任务'}
            actions={(
              <Space size={4} wrap aria-label="运行状态筛选">
                {queueStatusFilters.map((filter) => (
                  <Button
                    key={filter.value}
                    size="small"
                    type={queueStatusFilter === filter.value ? 'primary' : 'default'}
                    aria-pressed={queueStatusFilter === filter.value}
                    onClick={() => setQueueStatusFilter(filter.value)}
                  >
                    {filter.label}
                  </Button>
                ))}
              </Space>
            )}
            renderItem={(item) => {
              const runStatus = resolveQueueRunStatus(item)
              const updateTime = formatDateTime(item.updateTime || item.lastUpdatedAt || item.updatedAt)
              return (
                <div
                  role="button"
                  tabIndex={0}
                  className={`agent-queue-card ${runStatus === 'approval_required' ? 'is-approval-required' : ''} ${runStatus === 'failed' ? 'is-failed' : ''}`}
                  onClick={() => setSelectedRunId(String(item.id || item.runId))}
                  onKeyDown={(event) => { if (event.key === 'Enter') setSelectedRunId(String(item.id || item.runId)) }}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold text-ink">{pickText(item, ['title', 'agentType'], 'agent')}</p>
                      <Space size={6} wrap className="mt-1">
                        <span className="agent-queue-source">{pickText(item, ['triggerSource'], 'manual')}</span>
                        <span className="agent-queue-time">{updateTime}</span>
                      </Space>
                    </div>
                    <AgentStatusTag value={runStatus} />
                  </div>
                  <Space size={6} wrap className="mt-3">
                    {runStatus === 'approval_required' ? <Tag color="warning">需要人工确认</Tag> : null}
                    {runStatus === 'failed' ? <Tag color="error">执行失败</Tag> : null}
                  </Space>
                  <p className="mt-3 text-sm leading-6 text-secondary">{pickText(item, ['summary', 'approvalSummary', 'resultSummary'], '等待详情')}</p>
                </div>
              )
            }}
          />
          <div className="agent-timeline">
            <Card title="当前选中" className="surface-card">
              {currentRun ? (
                <Space orientation="vertical" style={{ width: '100%' }} size={12}>
                  <ThoughtTimeline steps={runThoughts} />
                  <ToolCallList calls={runToolCalls} />
                  <Card title="运行详情" className="surface-card">
                    <Descriptions column={1} items={[
                      { label: '任务 ID', children: String(currentRun.id || currentRun.runId || '-') },
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
              <AgentComposer onSend={(value) => createRun.mutate({ agentType: createForm.getFieldValue('agentType') || 'resume', triggerSource: 'manual', input: { prompt: value } })} />
              <List style={{ marginTop: 16 }} dataSource={currentMessages} renderItem={(item) => <List.Item>{item.role}：{item.content}</List.Item>} />
            </Card>
          </div>
        </div>
      </div>

      <Modal
        open={rejectOpen}
        title="拒绝任务"
        okText="确认拒绝"
        confirmLoading={rejectRun.isPending}
        onCancel={() => {
          setRejectOpen(false)
          setRejectReason('')
          setRejectReasonError('')
        }}
        onOk={() => {
          const reason = rejectReason.trim()
          if (!reason) {
            setRejectReasonError('请填写拒绝原因')
            return
          }
          if (currentRun) rejectRun.mutate({ runId: String(currentRun.id || currentRun.runId), reason })
        }}
      >
        <Input.TextArea
          value={rejectReason}
          status={rejectReasonError ? 'error' : undefined}
          onChange={(event) => {
            setRejectReason(event.target.value)
            if (rejectReasonError) setRejectReasonError('')
          }}
          rows={4}
          placeholder="填写拒绝原因"
        />
        {rejectReasonError ? <Typography.Text type="danger">{rejectReasonError}</Typography.Text> : null}
      </Modal>
    </ModulePage>
  )
}
