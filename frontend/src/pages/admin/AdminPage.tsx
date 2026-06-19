import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Descriptions, Form, Input, Modal, Popconfirm, Select, Space, Tabs, Tag } from 'antd'
import { approveContentApi, banUserApi, fetchAdminAiLogSummaryApi, fetchAdminAiLogsApi, fetchAdminInterviewGovernanceApi, fetchAdminInterviewGovernanceSummaryApi, fetchAdminOverviewApi, fetchAdminRuntimeGovernanceSummaryApi, fetchAdminSystemConfigsApi, fetchAdminUsersApi, fetchPendingContentApi, rejectContentApi, unbanUserApi, updateAdminUserApi, addAdminQuestionApi, addAdminCategoryApi } from '@/api/modules/admin'
import { exportQuestionsApi, exportUsersApi } from '@/api/modules/admin'
import { getErrorMessage } from '@/api/client'
import { DataTableCard, downloadBlob, labelOf, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

const FIELD_LABELS: Record<string, string> = {
  userCount: '用户总数', totalUsers: '用户总数', totalUserCount: '用户总数',
  todayActive: '今日活跃', activeUsers: '活跃用户', todayActiveUsers: '今日活跃',
  newUsers: '新增用户', todayNewUsers: '今日新增', weekNewUsers: '本周新增',
  questionCount: '题目数', totalQuestions: '题目数', knowledgeCount: '知识文档数',
  interviewCount: '面试场次', resumeCount: '简历数', applicationCount: '投递数', communityCount: '社区内容数',
  status: '状态', uptime: '运行时长', version: '版本', cpuUsage: 'CPU 使用率', memoryUsage: '内存使用率',
  total: '总数', success: '成功数', failed: '失败数', successRate: '成功率', avgLatencyMs: '平均耗时(ms)', avgLatency: '平均耗时',
  avgScore: '平均分', totalSessions: '会话总数', completedSessions: '已完成会话', pendingCount: '待处理'
}
const adminFieldLabel = (key: string) => FIELD_LABELS[key] || key

function SummaryCard({ title, data }: { title: string; data: unknown }) {
  const entries = Object.entries((data || {}) as Record<string, unknown>).filter(([, value]) => typeof value !== 'object').slice(0, 8)
  return (
    <Card title={title} className="surface-card">
      {entries.length ? (
        <Descriptions column={2} items={entries.map(([key, value]) => ({ label: adminFieldLabel(key), children: String(value ?? '-') }))} />
      ) : <div className="muted-text">暂无数据</div>}
    </Card>
  )
}

export function AdminPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [activeKey, setActiveKey] = useState('overview')
  const [questionOpen, setQuestionOpen] = useState(false)
  const [categoryOpen, setCategoryOpen] = useState(false)
  const [editUser, setEditUser] = useState<Record<string, any> | null>(null)
  const [rejectTarget, setRejectTarget] = useState<number | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const overview = useQuery({ queryKey: ['admin', 'overview'], queryFn: () => fetchAdminOverviewApi().then((response) => response.data) })
  const users = useQuery({ queryKey: ['admin', 'users'], queryFn: () => fetchAdminUsersApi({ pageNum: 1, pageSize: 10 }).then((response) => response.data), enabled: activeKey === 'users' })
  const logs = useQuery({ queryKey: ['admin', 'ai-logs'], queryFn: () => fetchAdminAiLogSummaryApi().then((response) => response.data), enabled: activeKey === 'ai' })
  const logList = useQuery({ queryKey: ['admin', 'ai-log-list'], queryFn: () => fetchAdminAiLogsApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data), enabled: activeKey === 'ai' })
  const runtime = useQuery({ queryKey: ['admin', 'runtime'], queryFn: () => fetchAdminRuntimeGovernanceSummaryApi().then((response) => response.data), enabled: activeKey === 'runtime' })
  const pending = useQuery({ queryKey: ['admin', 'pending'], queryFn: () => fetchPendingContentApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data), enabled: activeKey === 'content' })
  const sys = useQuery({ queryKey: ['admin', 'system-config'], queryFn: () => fetchAdminSystemConfigsApi().then((response) => response.data), enabled: activeKey === 'knowledge' })
  const interview = useQuery({ queryKey: ['admin', 'interviews'], queryFn: () => fetchAdminInterviewGovernanceApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data), enabled: activeKey === 'interview' })
  const interviewSummary = useQuery({ queryKey: ['admin', 'interviews-summary'], queryFn: () => fetchAdminInterviewGovernanceSummaryApi().then((response) => response.data), enabled: activeKey === 'interview-summary' })
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
  return (
    <ModulePage
      title="管理后台"
      description="管理用户、内容审核、AI 日志、系统配置与运行治理。"
      actions={<Space wrap><Button type="primary" onClick={() => invalidate()}>刷新</Button><Popconfirm title="导出全部题库？" onConfirm={() => exportQuestions.mutate()} okText="导出" cancelText="取消"><Button loading={exportQuestions.isPending}>导出题库</Button></Popconfirm><Popconfirm title="导出全部用户数据？" description="包含用户敏感信息，请确认用途并妥善保管。" onConfirm={() => exportUsers.mutate()} okText="导出" cancelText="取消"><Button loading={exportUsers.isPending}>导出用户</Button></Popconfirm></Space>}
      metrics={[
        { label: '用户', value: (overview.data as any)?.userCount ?? (overview.data as any)?.totalUsers ?? '-', hint: '系统规模' },
        { label: 'AI 日志', value: (logs.data as any)?.total ?? '-', hint: '调用摘要' },
        { label: '运行时', value: labelOf((runtime.data as any)?.status, '-'), hint: '治理状态' },
        { label: '待审', value: normalizeRecords(pending.data).length || '-', hint: '社区内容' }
      ]}
    >
      <Card className="surface-card">
        <Tabs
          activeKey={activeKey}
          onChange={setActiveKey}
          items={[
            { key: 'overview', label: '概览', children: <SummaryCard title="系统概览" data={overview.data} /> },
            { key: 'users', label: '用户', children: <DataTableCard title="用户列表" data={users.data} loading={users.isLoading} error={users.error} onRetry={() => users.refetch()} columns={[{ title: '用户名', render: (_, row) => pickText(row, ['username', 'nickname', 'email']) }, { title: '角色', render: (_, row) => <Tag>{pickText(row, ['role'])}</Tag> }, { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> }, { title: '操作', render: (_, row) => <Space><Button size="small" onClick={() => setEditUser(row)}>编辑</Button><Popconfirm title="封禁用户" description="该用户将无法正常使用账号。" onConfirm={() => ban.mutate(Number(row.id))}><Button size="small" danger loading={ban.isPending}>封禁</Button></Popconfirm><Popconfirm title="解封用户" description="该用户将恢复账号访问。" onConfirm={() => unban.mutate(Number(row.id))}><Button size="small" loading={unban.isPending}>解封</Button></Popconfirm></Space> }]} /> },
            { key: 'content', label: '内容审核', children: <DataTableCard title="待审内容" data={pending.data} loading={pending.isLoading} error={pending.error} onRetry={() => pending.refetch()} columns={[{ title: '标题', render: (_, row) => pickText(row, ['title', 'contentTitle']) }, { title: '作者', render: (_, row) => pickText(row, ['authorName', 'username']) }, { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> }, { title: '操作', render: (_, row) => <Space><Popconfirm title="通过内容" description="内容会对用户可见。" onConfirm={() => approve.mutate(Number(row.id))}><Button size="small" loading={approve.isPending}>通过</Button></Popconfirm><Button size="small" danger onClick={() => { setRejectTarget(Number(row.id)); setRejectReason('') }}>拒绝</Button></Space> }]} /> },
            { key: 'question', label: '题库管理', children: <Card className="surface-card"><Button type="primary" onClick={() => setQuestionOpen(true)}>新增题目</Button></Card> },
            { key: 'category', label: '分类管理', children: <Card className="surface-card"><Button type="primary" onClick={() => setCategoryOpen(true)}>新增分类</Button></Card> },
            { key: 'knowledge', label: '系统配置', children: <DataTableCard title="系统配置" data={sys.data} loading={sys.isLoading} error={sys.error} onRetry={() => sys.refetch()} columns={[{ title: '配置项', render: (_, row) => pickText(row, ['configKey', 'key', 'name']) }, { title: '值', render: (_, row) => pickText(row, ['configValue', 'value']) }, { title: '说明', render: (_, row) => pickText(row, ['description']) }]} /> },
            { key: 'ai', label: 'AI 日志', children: <DataTableCard title="AI 调用日志" data={logList.data} loading={logList.isLoading} error={logList.error} onRetry={() => logList.refetch()} columns={[{ title: '场景', render: (_, row) => pickText(row, ['scene']) }, { title: '服务商', render: (_, row) => pickText(row, ['provider', 'providerName']) }, { title: '模型', render: (_, row) => pickText(row, ['model']) }, { title: '类型', render: (_, row) => pickText(row, ['callType']) }, { title: '状态', render: (_, row) => <StatusTag value={String(row.success === 1 || row.success === true ? 'SUCCESS' : 'FAILED')} /> }, { title: '耗时', render: (_, row) => pickText(row, ['latencyMs', 'durationMs']) }, { title: '错误', render: (_, row) => pickText(row, ['errorMessage', 'failReason']) }]} /> },
            { key: 'runtime', label: '运行治理', children: <SummaryCard title="运行时摘要" data={runtime.data} /> },
            { key: 'interview', label: '面试治理', children: <DataTableCard title="面试治理" data={interview.data} loading={interview.isLoading} error={interview.error} onRetry={() => interview.refetch()} columns={[{ title: '会话', render: (_, row) => pickText(row, ['sessionId', 'title']) }, { title: '模式', render: (_, row) => pickText(row, ['mode']) }, { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> }, { title: '分数', render: (_, row) => pickText(row, ['score']) }]} /> },
            { key: 'interview-summary', label: '面试摘要', children: <SummaryCard title="面试治理摘要" data={interviewSummary.data} /> }
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
      <Modal open={questionOpen} title="新增题目" footer={null} onCancel={() => setQuestionOpen(false)}><Form layout="vertical" onFinish={(values) => addQuestion.mutate(values)}><Form.Item label="标题" name="title" rules={[{ required: true }]}><Input /></Form.Item><Form.Item label="答案" name="standardAnswer"><Input.TextArea rows={4} /></Form.Item><Button type="primary" htmlType="submit" loading={addQuestion.isPending}>保存</Button></Form></Modal>
      <Modal open={categoryOpen} title="新增分类" footer={null} onCancel={() => setCategoryOpen(false)}><Form layout="vertical" onFinish={(values) => addCategory.mutate(values)}><Form.Item label="名称" name="name" rules={[{ required: true }]}><Input /></Form.Item><Form.Item label="排序" name="sortOrder"><Input /></Form.Item><Button type="primary" htmlType="submit" loading={addCategory.isPending}>保存</Button></Form></Modal>
    </ModulePage>
  )
}
