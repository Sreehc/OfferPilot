import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Descriptions, Form, Input, Modal, Popconfirm, Select, Space, Tabs, Tag, Typography } from 'antd'
import {
  addAdminCategoryApi,
  addAdminQuestionApi,
  approveContentApi,
  banUserApi,
  fetchAdminAiLogSummaryApi,
  fetchAdminAiLogsApi,
  fetchAdminInterviewGovernanceApi,
  fetchAdminInterviewGovernanceSummaryApi,
  fetchAdminOverviewApi,
  fetchAdminRuntimeGovernanceSummaryApi,
  fetchAdminSystemConfigsApi,
  fetchAdminUsersApi,
  fetchPendingContentApi,
  rejectContentApi,
  unbanUserApi,
  updateAdminUserApi
} from '@/api/modules/admin'
import { exportQuestionsApi, exportUsersApi } from '@/api/modules/admin'
import type { AnyRecord } from '@/api/types'
import { getErrorMessage } from '@/api/client'
import { DataTableCard, downloadBlob, formatDateTime, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

const FIELD_LABELS: Record<string, string> = {
  userCount: '用户总数',
  totalUsers: '用户总数',
  totalUserCount: '用户总数',
  todayActive: '今日活跃',
  activeUsers: '活跃用户',
  todayActiveUsers: '今日活跃',
  todayNew: '今日新增',
  newUsers: '新增用户',
  todayNewUsers: '今日新增',
  weekNewUsers: '本周新增',
  questionCount: '题目数',
  totalQuestions: '题目数',
  knowledgeCount: '知识文档数',
  interviewCount: '面试场次',
  totalInterviews: '面试场次',
  resumeCount: '简历数',
  applicationCount: '投递数',
  communityCount: '社区内容数',
  status: '状态',
  uptime: '运行时长',
  version: '版本',
  cpuUsage: 'CPU 使用率',
  memoryUsage: '内存使用率',
  total: '总数',
  totalCalls: '调用量',
  successCalls: '成功数',
  failedCalls: '失败数',
  successRate: '成功率',
  avgLatencyMs: '平均耗时(ms)',
  avgLatency: '平均耗时',
  avgScore: '平均分',
  totalSessions: '会话总数',
  completedSessions: '已完成会话',
  pendingCount: '待处理'
}

const adminFieldLabel = (key: string) => FIELD_LABELS[key] || key

function readNumber(record: AnyRecord | undefined, keys: string[], fallback = 0) {
  if (!record) return fallback
  for (const key of keys) {
    const value = record[key]
    if (value === undefined || value === null || value === '') continue
    const parsed = Number(value)
    if (Number.isFinite(parsed)) return parsed
  }
  return fallback
}

function formatInteger(value: number) {
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 }).format(value)
}

function formatPercentValue(value: number) {
  return `${Math.round(value)}%`
}

function formatLatency(value: number) {
  return value > 0 ? `${formatInteger(value)}ms` : '-'
}

function formatCost(value: unknown) {
  const amount = Number(value)
  return Number.isFinite(amount) && amount > 0 ? `$${amount.toFixed(2)}` : '-'
}

function failureRate(summary: AnyRecord | undefined) {
  const explicit = readNumber(summary, ['failureRate'], Number.NaN)
  if (Number.isFinite(explicit)) return explicit <= 1 ? explicit * 100 : explicit
  const total = readNumber(summary, ['totalCalls', 'total'])
  const failed = readNumber(summary, ['failedCalls', 'failed'])
  return total > 0 ? (failed / total) * 100 : 0
}

function errorReasonLabels(summary: AnyRecord | undefined, logData: unknown) {
  const backendBuckets = normalizeRecords<AnyRecord>(summary?.errorReasonBuckets)
    .map((row) => {
      const reason = pickText(row, ['reason', 'errorMessage', 'message'], '')
      const count = readNumber(row, ['count', 'total'], 0)
      return reason && reason !== '-' ? (count > 0 ? `${reason} (${formatInteger(count)})` : reason) : ''
    })
    .filter(Boolean)
  if (backendBuckets.length) return backendBuckets.slice(0, 6)
  return normalizeRecords(logData)
    .map((row) => pickText(row, ['errorMessage', 'failReason'], ''))
    .filter(Boolean)
    .filter((reason, index, values) => values.indexOf(reason) === index)
    .slice(0, 4)
}

function SummaryCard({ title, data }: { title: string; data: unknown }) {
  const entries = Object.entries((data || {}) as Record<string, unknown>)
    .filter(([, value]) => typeof value !== 'object')
    .slice(0, 8)
  return (
    <Card title={title} className="surface-card">
      {entries.length ? (
        <Descriptions column={2} items={entries.map(([key, value]) => ({ label: adminFieldLabel(key), children: String(value ?? '-') }))} />
      ) : <div className="muted-text">暂无数据</div>}
    </Card>
  )
}

function MetricPanel({ items }: { items: Array<{ label: string; value: string | number; hint?: string }> }) {
  return (
    <div className="metric-strip">
      {items.map((item) => (
        <div className="metric-card" key={item.label}>
          <div className="label">{item.label}</div>
          <div className="value">{item.value}</div>
          {item.hint && <div className="muted-text" style={{ marginTop: 8, fontSize: 12 }}>{item.hint}</div>}
        </div>
      ))}
    </div>
  )
}

function TextList({ items, emptyText }: { items?: string[]; emptyText: string }) {
  const normalized = items?.filter(Boolean) ?? []
  if (!normalized.length) return <Typography.Paragraph className="muted-text">{emptyText}</Typography.Paragraph>
  return (
    <Space orientation="vertical" size={8} style={{ width: '100%' }}>
      {normalized.map((item) => (
        <Typography.Text key={item}>{item}</Typography.Text>
      ))}
    </Space>
  )
}

function AiObservabilityPanel({ summary, logData }: { summary?: AnyRecord; logData: unknown }) {
  const totalCalls = readNumber(summary, ['totalCalls', 'total'])
  const failedCalls = readNumber(summary, ['failedCalls', 'failed'])
  const successCalls = readNumber(summary, ['successCalls', 'success'])
  const chatCalls = readNumber(summary, ['chatCalls'])
  const embeddingCalls = readNumber(summary, ['embeddingCalls'])
  const avgLatency = readNumber(summary, ['avgLatencyMs', 'avgLatency'])
  const latencyP95 = readNumber(summary, ['latencyP95Ms', 'latencyP95'])
  const usage = (summary?.costSummary || summary?.usageSummary || {}) as AnyRecord
  const errorReasons = errorReasonLabels(summary, logData)

  return (
    <section role="region" aria-label="AI 可观测面板" className="workspace-grid">
      <MetricPanel
        items={[
          { label: '调用量', value: formatInteger(totalCalls), hint: `成功 ${formatInteger(successCalls)} / 失败 ${formatInteger(failedCalls)}` },
          { label: '失败率', value: formatPercentValue(failureRate(summary)), hint: '失败请求占总调用比例' },
          { label: '平均耗时', value: formatLatency(avgLatency), hint: latencyP95 > 0 ? `P95 ${formatLatency(latencyP95)}` : '最近可用耗时样本' },
          { label: 'Token / 成本', value: formatInteger(readNumber(usage, ['totalTokens'])), hint: `估算成本 ${formatCost(usage.estimatedCost)}` }
        ]}
      />
      <div className="workspace-grid two">
        <Card title="调用结构趋势" className="surface-card">
          <Typography.Title level={4} style={{ marginTop: 0 }}>{`Chat ${formatInteger(chatCalls)} / Embedding ${formatInteger(embeddingCalls)}`}</Typography.Title>
          <Typography.Paragraph className="muted-text">用调用类型结构判断近期成本、延迟和失败排查重点。</Typography.Paragraph>
        </Card>
        <Card title="异常原因" className="surface-card">
          {errorReasons.length ? (
            <Space wrap>
              {errorReasons.map((reason) => <Tag color="red" key={reason}>{reason}</Tag>)}
            </Space>
          ) : <Typography.Paragraph className="muted-text">暂无失败原因。</Typography.Paragraph>}
        </Card>
      </div>
    </section>
  )
}

function RuntimeGovernancePanel({ runtime, interviewSummary }: { runtime?: AnyRecord; interviewSummary?: AnyRecord }) {
  return (
    <div className="workspace-grid">
      <MetricPanel
        items={[
          { label: 'Agent Run', value: formatInteger(readNumber(runtime, ['totalAgentRuns'])), hint: `待审批 ${formatInteger(readNumber(runtime, ['pendingApprovalRuns']))}` },
          { label: 'Provider 阻断', value: formatInteger(readNumber(runtime, ['providerBlockedRuns', 'blockedCopilotRealtimeSessions'])), hint: `失败配置 ${formatInteger(readNumber(runtime, ['failedProviderConfigs']))}` },
          { label: 'AI 失败', value: formatInteger(readNumber(runtime, ['failedAiCalls'])), hint: `平均耗时 ${formatLatency(readNumber(runtime, ['avgAiLatencyMs']))}` },
          { label: '面试会话', value: formatInteger(readNumber(interviewSummary, ['totalSessions'])), hint: `完成 ${formatInteger(readNumber(interviewSummary, ['finishedSessions', 'completedSessions']))}` }
        ]}
      />
      <div className="workspace-grid two">
        <Card title="运行风险信号" className="surface-card">
          <TextList items={runtime?.riskSignals as string[] | undefined} emptyText="暂无运行风险信号。" />
        </Card>
        <Card title="处理建议" className="surface-card">
          <TextList items={runtime?.recommendations as string[] | undefined} emptyText="暂无额外处理建议。" />
        </Card>
      </div>
    </div>
  )
}

export function AdminPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [activeKey, setActiveKey] = useState('users')
  const [questionOpen, setQuestionOpen] = useState(false)
  const [categoryOpen, setCategoryOpen] = useState(false)
  const [editUser, setEditUser] = useState<Record<string, any> | null>(null)
  const [rejectTarget, setRejectTarget] = useState<number | null>(null)
  const [rejectReason, setRejectReason] = useState('')

  const overview = useQuery({ queryKey: ['admin', 'overview'], queryFn: () => fetchAdminOverviewApi().then((response) => response.data) })
  const users = useQuery({ queryKey: ['admin', 'users'], queryFn: () => fetchAdminUsersApi({ pageNum: 1, pageSize: 10 }).then((response) => response.data), enabled: activeKey === 'users' })
  const logs = useQuery({ queryKey: ['admin', 'ai-logs'], queryFn: () => fetchAdminAiLogSummaryApi().then((response) => response.data), enabled: activeKey === 'ai' })
  const logList = useQuery({ queryKey: ['admin', 'ai-log-list'], queryFn: () => fetchAdminAiLogsApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data), enabled: activeKey === 'ai' })
  const runtime = useQuery({ queryKey: ['admin', 'runtime'], queryFn: () => fetchAdminRuntimeGovernanceSummaryApi().then((response) => response.data), enabled: activeKey === 'governance' })
  const pending = useQuery({ queryKey: ['admin', 'pending'], queryFn: () => fetchPendingContentApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data), enabled: activeKey === 'content' })
  const sys = useQuery({ queryKey: ['admin', 'system-config'], queryFn: () => fetchAdminSystemConfigsApi().then((response) => response.data), enabled: activeKey === 'system' })
  const interview = useQuery({ queryKey: ['admin', 'interviews'], queryFn: () => fetchAdminInterviewGovernanceApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data), enabled: activeKey === 'governance' })
  const interviewSummary = useQuery({ queryKey: ['admin', 'interviews-summary'], queryFn: () => fetchAdminInterviewGovernanceSummaryApi().then((response) => response.data), enabled: activeKey === 'governance' })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['admin'] })
  const exportQuestions = useMutation({ mutationFn: exportQuestionsApi, onSuccess: (response) => { downloadBlob(response.data, 'questions.xlsx'); message.success('题库已导出') }, onError: (error) => message.error(getErrorMessage(error, '导出失败')) })
  const exportUsers = useMutation({ mutationFn: exportUsersApi, onSuccess: (response) => { downloadBlob(response.data, 'users.xlsx'); message.success('用户已导出') }, onError: (error) => message.error(getErrorMessage(error, '导出失败')) })
  const ban = useMutation({ mutationFn: (id: number) => banUserApi(id), onSuccess: () => { message.success('用户已封禁'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '封禁失败')) })
  const unban = useMutation({ mutationFn: (id: number) => unbanUserApi(id), onSuccess: () => { message.success('用户已解封'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '解封失败')) })
  const approve = useMutation({ mutationFn: (id: number) => approveContentApi(id), onSuccess: () => { message.success('已通过审核'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '审核失败')) })
  const reject = useMutation({ mutationFn: ({ id, reason }: { id: number; reason?: string }) => rejectContentApi(id, reason), onSuccess: () => { message.success('已拒绝内容'); setRejectTarget(null); setRejectReason(''); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '拒绝失败')) })
  const updateUser = useMutation({ mutationFn: ({ id, payload }: { id: number; payload: any }) => updateAdminUserApi(id, payload), onSuccess: () => { message.success('用户已更新'); setEditUser(null); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '更新失败')) })
  const addQuestion = useMutation({ mutationFn: addAdminQuestionApi, onSuccess: () => { message.success('题目已新增'); setQuestionOpen(false); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '新增失败')) })
  const addCategory = useMutation({ mutationFn: addAdminCategoryApi, onSuccess: () => { message.success('分类已新增'); setCategoryOpen(false); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '新增失败')) })

  const overviewData = overview.data as AnyRecord | undefined
  const aiSummary = logs.data as AnyRecord | undefined
  const runtimeData = runtime.data as AnyRecord | undefined

  return (
    <ModulePage
      title="管理后台"
      description="按用户、内容、AI、系统和治理拆分后台信息架构，集中观察 AI 调用、运行风险和系统配置。"
      actions={<Space wrap><Button type="primary" onClick={() => invalidate()}>刷新</Button><Popconfirm title="导出全部题库？" onConfirm={() => exportQuestions.mutate()} okText="导出" cancelText="取消"><Button loading={exportQuestions.isPending}>导出题库</Button></Popconfirm><Popconfirm title="导出全部用户数据？" description="包含用户敏感信息，请确认用途并妥善保管。" onConfirm={() => exportUsers.mutate()} okText="导出" cancelText="取消"><Button loading={exportUsers.isPending}>导出用户</Button></Popconfirm></Space>}
      metrics={[
        { label: '用户', value: readNumber(overviewData, ['userCount', 'totalUsers']) || '-', hint: '系统规模' },
        { label: 'AI 失败率', value: logs.data ? formatPercentValue(failureRate(aiSummary)) : '-', hint: '调用质量' },
        { label: '运行风险', value: (runtimeData?.riskSignals as string[] | undefined)?.length ?? '-', hint: '治理信号' },
        { label: '待审', value: normalizeRecords(pending.data).length || '-', hint: '社区内容' }
      ]}
    >
      <Card className="surface-card">
        <Tabs
          activeKey={activeKey}
          onChange={setActiveKey}
          items={[
            {
              key: 'users',
              label: '用户与权限',
              children: (
                <DataTableCard
                  title="用户列表"
                  data={users.data}
                  loading={users.isLoading}
                  error={users.error}
                  onRetry={() => users.refetch()}
                  columns={[
                    { title: '用户名', render: (_, row) => pickText(row, ['username', 'nickname', 'email']) },
                    { title: '角色', render: (_, row) => <Tag>{pickText(row, ['role'])}</Tag> },
                    { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> },
                    { title: '最后登录', render: (_, row) => formatDateTime(row.lastLoginTime) },
                    {
                      title: '操作',
                      render: (_, row) => (
                        <Space>
                          <Button size="small" onClick={() => setEditUser(row)}>编辑</Button>
                          <Popconfirm title="封禁用户" description="该用户将无法正常使用账号。" onConfirm={() => ban.mutate(Number(row.id))}>
                            <Button size="small" danger loading={ban.isPending}>封禁</Button>
                          </Popconfirm>
                          <Popconfirm title="解封用户" description="该用户将恢复账号访问。" onConfirm={() => unban.mutate(Number(row.id))}>
                            <Button size="small" loading={unban.isPending}>解封</Button>
                          </Popconfirm>
                        </Space>
                      )
                    }
                  ]}
                />
              )
            },
            {
              key: 'content',
              label: '内容治理',
              children: (
                <div className="workspace-grid">
                  <DataTableCard
                    title="待审内容"
                    data={pending.data}
                    loading={pending.isLoading}
                    error={pending.error}
                    onRetry={() => pending.refetch()}
                    columns={[
                      { title: '标题', render: (_, row) => pickText(row, ['title', 'contentTitle']) },
                      { title: '作者', render: (_, row) => pickText(row, ['authorName', 'username']) },
                      { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> },
                      {
                        title: '操作',
                        render: (_, row) => (
                          <Space>
                            <Popconfirm title="通过内容" description="内容会对用户可见。" onConfirm={() => approve.mutate(Number(row.id))}>
                              <Button size="small" loading={approve.isPending}>通过</Button>
                            </Popconfirm>
                            <Button size="small" danger onClick={() => { setRejectTarget(Number(row.id)); setRejectReason('') }}>拒绝</Button>
                          </Space>
                        )
                      }
                    ]}
                  />
                  <div className="workspace-grid two">
                    <Card title="题库管理" className="surface-card">
                      <Button type="primary" onClick={() => setQuestionOpen(true)}>新增题目</Button>
                    </Card>
                    <Card title="分类管理" className="surface-card">
                      <Button type="primary" onClick={() => setCategoryOpen(true)}>新增分类</Button>
                    </Card>
                  </div>
                </div>
              )
            },
            {
              key: 'ai',
              label: 'AI 观测',
              children: (
                <div className="workspace-grid">
                  <AiObservabilityPanel summary={aiSummary} logData={logList.data} />
                  <DataTableCard
                    title="AI 调用日志"
                    data={logList.data}
                    loading={logList.isLoading}
                    error={logList.error}
                    onRetry={() => logList.refetch()}
                    columns={[
                      { title: '场景', render: (_, row) => pickText(row, ['scene']) },
                      { title: '服务商', render: (_, row) => pickText(row, ['provider', 'providerName']) },
                      { title: '模型', render: (_, row) => pickText(row, ['model']) },
                      { title: '类型', render: (_, row) => pickText(row, ['callType']) },
                      { title: '状态', render: (_, row) => <StatusTag value={String(row.success === 1 || row.success === true ? 'SUCCESS' : 'FAILED')} /> },
                      { title: '耗时', render: (_, row) => formatLatency(readNumber(row, ['latencyMs', 'durationMs'])) },
                      { title: '错误', render: (_, row) => pickText(row, ['errorMessage', 'failReason']) }
                    ]}
                  />
                </div>
              )
            },
            {
              key: 'system',
              label: '系统配置',
              children: (
                <div className="workspace-grid">
                  <SummaryCard title="系统概览" data={overview.data} />
                  <DataTableCard
                    title="系统配置"
                    data={sys.data}
                    loading={sys.isLoading}
                    error={sys.error}
                    onRetry={() => sys.refetch()}
                    columns={[
                      { title: '分组', render: (_, row) => pickText(row, ['configGroup']) },
                      { title: '配置项', render: (_, row) => pickText(row, ['displayName', 'configKey', 'key', 'name']) },
                      { title: '值', render: (_, row) => pickText(row, ['configValue', 'value']) },
                      { title: '启用', render: (_, row) => <StatusTag value={row.enabled === false ? 'DISABLED' : 'ACTIVE'} /> },
                      { title: '说明', render: (_, row) => pickText(row, ['description']) }
                    ]}
                  />
                </div>
              )
            },
            {
              key: 'governance',
              label: '运行治理',
              children: (
                <div className="workspace-grid">
                  <RuntimeGovernancePanel runtime={runtimeData} interviewSummary={interviewSummary.data as AnyRecord | undefined} />
                  <SummaryCard title="面试治理摘要" data={interviewSummary.data} />
                  <DataTableCard
                    title="面试治理"
                    data={interview.data}
                    loading={interview.isLoading}
                    error={interview.error}
                    onRetry={() => interview.refetch()}
                    columns={[
                      { title: '会话', render: (_, row) => pickText(row, ['sessionId', 'title']) },
                      { title: '方向', render: (_, row) => pickText(row, ['direction', 'jobRole']) },
                      { title: '模式', render: (_, row) => pickText(row, ['mode']) },
                      { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> },
                      { title: '分数', render: (_, row) => pickText(row, ['totalScore', 'score']) }
                    ]}
                  />
                </div>
              )
            }
          ]}
        />
      </Card>

      <Modal open={Boolean(editUser)} title="编辑用户" footer={null} onCancel={() => setEditUser(null)} destroyOnHidden>
        <Form key={editUser?.id} layout="vertical" initialValues={{ nickname: editUser?.nickname, email: editUser?.email, role: editUser?.role || 'USER', status: editUser?.status || 'ACTIVE' }} onFinish={(values) => editUser && updateUser.mutate({ id: Number(editUser.id), payload: values })}>
          <Form.Item label="昵称" name="nickname"><Input /></Form.Item>
          <Form.Item label="邮箱" name="email" rules={[{ type: 'email', message: '邮箱格式不正确' }]}><Input /></Form.Item>
          <Form.Item label="角色" name="role"><Select options={[{ value: 'USER', label: '普通用户' }, { value: 'ADMIN', label: '管理员' }]} /></Form.Item>
          <Form.Item label="状态" name="status"><Select options={[{ value: 'ACTIVE', label: '正常' }, { value: 'BANNED', label: '封禁' }]} /></Form.Item>
          <Button type="primary" htmlType="submit" loading={updateUser.isPending}>保存修改</Button>
        </Form>
      </Modal>
      <Modal open={rejectTarget !== null} title="拒绝内容" okText="确认拒绝" okButtonProps={{ danger: true, loading: reject.isPending }} onCancel={() => setRejectTarget(null)} onOk={() => rejectTarget !== null && reject.mutate({ id: rejectTarget, reason: rejectReason })}>
        <p className="muted-text">拒绝原因会记录到审计日志，并可能通知作者。</p>
        <Input.TextArea value={rejectReason} onChange={(event) => setRejectReason(event.target.value)} rows={4} placeholder="填写拒绝原因（必填）" />
      </Modal>
      <Modal open={questionOpen} title="新增题目" footer={null} onCancel={() => setQuestionOpen(false)}>
        <Form layout="vertical" onFinish={(values) => addQuestion.mutate(values)}>
          <Form.Item label="标题" name="title" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item label="答案" name="standardAnswer"><Input.TextArea rows={4} /></Form.Item>
          <Button type="primary" htmlType="submit" loading={addQuestion.isPending}>保存</Button>
        </Form>
      </Modal>
      <Modal open={categoryOpen} title="新增分类" footer={null} onCancel={() => setCategoryOpen(false)}>
        <Form layout="vertical" onFinish={(values) => addCategory.mutate(values)}>
          <Form.Item label="名称" name="name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item label="排序" name="sortOrder"><Input /></Form.Item>
          <Button type="primary" htmlType="submit" loading={addCategory.isPending}>保存</Button>
        </Form>
      </Modal>
    </ModulePage>
  )
}
