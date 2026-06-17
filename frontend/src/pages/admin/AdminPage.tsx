import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Descriptions, Form, Input, Modal, Space, Tabs, Tag } from 'antd'
import { approveContentApi, banUserApi, fetchAdminAiLogSummaryApi, fetchAdminAiLogsApi, fetchAdminInterviewGovernanceApi, fetchAdminInterviewGovernanceSummaryApi, fetchAdminOverviewApi, fetchAdminRuntimeGovernanceSummaryApi, fetchAdminSystemConfigsApi, fetchAdminUsersApi, fetchPendingContentApi, rejectContentApi, unbanUserApi, updateAdminUserApi, addAdminQuestionApi, addAdminCategoryApi } from '@/api/modules/admin'
import { exportQuestionsApi, exportUsersApi } from '@/api/modules/admin'
import { getErrorMessage } from '@/api/client'
import { DataTableCard, downloadBlob, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function AdminPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [questionOpen, setQuestionOpen] = useState(false)
  const [categoryOpen, setCategoryOpen] = useState(false)
  const overview = useQuery({ queryKey: ['admin', 'overview'], queryFn: () => fetchAdminOverviewApi().then((response) => response.data) })
  const users = useQuery({ queryKey: ['admin', 'users'], queryFn: () => fetchAdminUsersApi({ pageNum: 1, pageSize: 10 }).then((response) => response.data) })
  const logs = useQuery({ queryKey: ['admin', 'ai-logs'], queryFn: () => fetchAdminAiLogSummaryApi().then((response) => response.data) })
  const logList = useQuery({ queryKey: ['admin', 'ai-log-list'], queryFn: () => fetchAdminAiLogsApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data) })
  const runtime = useQuery({ queryKey: ['admin', 'runtime'], queryFn: () => fetchAdminRuntimeGovernanceSummaryApi().then((response) => response.data) })
  const pending = useQuery({ queryKey: ['admin', 'pending'], queryFn: () => fetchPendingContentApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data) })
  const sys = useQuery({ queryKey: ['admin', 'system-config'], queryFn: () => fetchAdminSystemConfigsApi().then((response) => response.data) })
  const interview = useQuery({ queryKey: ['admin', 'interviews'], queryFn: () => fetchAdminInterviewGovernanceApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data) })
  const interviewSummary = useQuery({ queryKey: ['admin', 'interviews-summary'], queryFn: () => fetchAdminInterviewGovernanceSummaryApi().then((response) => response.data) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['admin'] })
  const exportQuestions = useMutation({ mutationFn: exportQuestionsApi, onSuccess: (response) => { downloadBlob(response.data, 'questions.xlsx'); message.success('题库已导出') }, onError: (error) => message.error(getErrorMessage(error, '导出失败')) })
  const exportUsers = useMutation({ mutationFn: exportUsersApi, onSuccess: (response) => { downloadBlob(response.data, 'users.xlsx'); message.success('用户已导出') }, onError: (error) => message.error(getErrorMessage(error, '导出失败')) })
  const ban = useMutation({ mutationFn: (id: number) => banUserApi(id), onSuccess: () => { message.success('用户已封禁'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '封禁失败')) })
  const unban = useMutation({ mutationFn: (id: number) => unbanUserApi(id), onSuccess: () => { message.success('用户已解封'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '解封失败')) })
  const approve = useMutation({ mutationFn: (id: number) => approveContentApi(id), onSuccess: () => { message.success('已通过审核'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '审核失败')) })
  const reject = useMutation({ mutationFn: ({ id, reason }: { id: number; reason?: string }) => rejectContentApi(id, reason), onSuccess: () => { message.success('已拒绝内容'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '拒绝失败')) })
  const updateUser = useMutation({ mutationFn: ({ id, payload }: { id: number; payload: any }) => updateAdminUserApi(id, payload), onSuccess: () => { message.success('用户已更新'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '更新失败')) })
  const addQuestion = useMutation({ mutationFn: addAdminQuestionApi, onSuccess: () => { message.success('题目已新增'); setQuestionOpen(false); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '新增失败')) })
  const addCategory = useMutation({ mutationFn: addAdminCategoryApi, onSuccess: () => { message.success('分类已新增'); setCategoryOpen(false); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '新增失败')) })
  return (
    <ModulePage
      title="管理后台"
      description="面向用户、内容、AI 日志、系统配置和运行治理。"
      actions={<Space><Button type="primary" onClick={() => invalidate()}>刷新</Button><Button onClick={() => exportQuestions.mutate()}>导出题库</Button><Button onClick={() => exportUsers.mutate()}>导出用户</Button></Space>}
      metrics={[
        { label: '用户', value: (overview.data as any)?.userCount ?? normalizeRecords(users.data).length, hint: '系统规模' },
        { label: 'AI 日志', value: (logs.data as any)?.total ?? '-', hint: '调用摘要' },
        { label: '运行时', value: (runtime.data as any)?.status ?? 'OK', hint: '治理状态' },
        { label: '待审', value: normalizeRecords(pending.data).length, hint: '社区内容' }
      ]}
    >
      <Card className="surface-card">
        <Tabs
          items={[
            { key: 'overview', label: '概览', children: <Card title="系统概览" className="surface-card"><Descriptions column={2} items={Object.entries((overview.data || {}) as Record<string, unknown>).slice(0, 8).map(([key, value]) => ({ label: key, children: String(value ?? '-') }))} /></Card> },
            { key: 'users', label: '用户', children: <DataTableCard title="用户列表" data={users.data} loading={users.isLoading} error={users.error} onRetry={() => users.refetch()} columns={[{ title: '用户名', render: (_, row) => pickText(row, ['username', 'nickname', 'email']) }, { title: '角色', render: (_, row) => <Tag>{pickText(row, ['role'])}</Tag> }, { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> }, { title: '操作', render: (_, row) => <Space><Button size="small" onClick={() => updateUser.mutate({ id: Number(row.id), payload: row })}>编辑</Button><Button size="small" danger onClick={() => ban.mutate(Number(row.id))}>封禁</Button><Button size="small" onClick={() => unban.mutate(Number(row.id))}>解封</Button></Space> }]} /> },
            { key: 'content', label: '内容审核', children: <DataTableCard title="待审内容" data={pending.data} loading={pending.isLoading} error={pending.error} onRetry={() => pending.refetch()} columns={[{ title: '标题', render: (_, row) => pickText(row, ['title', 'contentTitle']) }, { title: '作者', render: (_, row) => pickText(row, ['authorName', 'username']) }, { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> }, { title: '操作', render: (_, row) => <Space><Button size="small" onClick={() => approve.mutate(Number(row.id))}>通过</Button><Button size="small" danger onClick={() => reject.mutate({ id: Number(row.id), reason: 'admin review' })}>拒绝</Button></Space> }]} /> },
            { key: 'question', label: '题库管理', children: <Card className="surface-card"><Button type="primary" onClick={() => setQuestionOpen(true)}>新增题目</Button></Card> },
            { key: 'category', label: '分类管理', children: <Card className="surface-card"><Button type="primary" onClick={() => setCategoryOpen(true)}>新增分类</Button></Card> },
            { key: 'knowledge', label: '知识库治理', children: <DataTableCard title="系统配置" data={sys.data} loading={sys.isLoading} error={sys.error} onRetry={() => sys.refetch()} columns={[{ title: '配置项', render: (_, row) => pickText(row, ['configKey', 'key', 'name']) }, { title: '值', render: (_, row) => pickText(row, ['configValue', 'value']) }, { title: '说明', render: (_, row) => pickText(row, ['description']) }]} /> },
            { key: 'ai', label: 'AI 日志', children: <DataTableCard title="AI 调用日志" data={logList.data} loading={logList.isLoading} error={logList.error} onRetry={() => logList.refetch()} columns={[{ title: '场景', render: (_, row) => pickText(row, ['scene']) }, { title: '类型', render: (_, row) => pickText(row, ['callType']) }, { title: '状态', render: (_, row) => <StatusTag value={String(row.success === 1 || row.success === true ? 'SUCCESS' : 'FAILED')} /> }, { title: '耗时', render: (_, row) => pickText(row, ['latencyMs', 'durationMs']) }]} /> },
            { key: 'runtime', label: 'Runtime', children: <Card title="运行时摘要" className="surface-card"><Descriptions column={2} items={Object.entries((runtime.data || {}) as Record<string, unknown>).slice(0, 8).map(([key, value]) => ({ label: key, children: String(value ?? '-') }))} /></Card> },
            { key: 'interview', label: '面试治理', children: <DataTableCard title="面试治理" data={interview.data} loading={interview.isLoading} error={interview.error} onRetry={() => interview.refetch()} columns={[{ title: '会话', render: (_, row) => pickText(row, ['sessionId', 'title']) }, { title: '模式', render: (_, row) => pickText(row, ['mode']) }, { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> }, { title: '分数', render: (_, row) => pickText(row, ['score']) }]} /> },
            { key: 'interview-summary', label: '面试摘要', children: <Card title="面试治理摘要" className="surface-card"><Descriptions column={2} items={Object.entries((interviewSummary.data || {}) as Record<string, unknown>).slice(0, 8).map(([key, value]) => ({ label: key, children: String(value ?? '-') }))} /></Card> }
          ]}
        />
      </Card>
      <Modal open={questionOpen} title="新增题目" footer={null} onCancel={() => setQuestionOpen(false)}><Form layout="vertical" onFinish={(values) => addQuestion.mutate(values)}><Form.Item label="标题" name="title" rules={[{ required: true }]}><Input /></Form.Item><Form.Item label="答案" name="standardAnswer"><Input.TextArea rows={4} /></Form.Item><Button type="primary" htmlType="submit" loading={addQuestion.isPending}>保存</Button></Form></Modal>
      <Modal open={categoryOpen} title="新增分类" footer={null} onCancel={() => setCategoryOpen(false)}><Form layout="vertical" onFinish={(values) => addCategory.mutate(values)}><Form.Item label="名称" name="name" rules={[{ required: true }]}><Input /></Form.Item><Form.Item label="排序" name="sortOrder"><Input /></Form.Item><Button type="primary" htmlType="submit" loading={addCategory.isPending}>保存</Button></Form></Modal>
    </ModulePage>
  )
}
