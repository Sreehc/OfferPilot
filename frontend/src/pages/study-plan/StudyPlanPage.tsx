import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Form, Input, InputNumber, Modal, Progress, Space } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchCurrentStudyPlanApi, generateStudyPlanApi, refreshStudyPlanApi, updateStudyPlanTaskStatusApi } from '@/api/modules/plan'
import { getErrorMessage } from '@/api/client'
import { DataTableCard, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

function taskRoute(row: Record<string, any>): string {
  const hint = `${pickText(row, ['category', 'focusDirection', 'type', 'title', 'name'], '')}`
  if (/面试|interview/i.test(hint)) return '/interview'
  if (/复习|review/i.test(hint)) return '/review'
  if (/简历|resume/i.test(hint)) return '/resume'
  return '/question'
}

export function StudyPlanPage() {
  const { message } = AntApp.useApp()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const plan = useQuery({ queryKey: ['study-plan'], queryFn: () => fetchCurrentStudyPlanApi().then((response) => response.data) })
  const tasks = normalizeRecords((plan.data as any)?.tasks || (plan.data as any)?.dailyTasks || plan.data)
  const done = tasks.filter((task) => ['done', 'completed', 'finished'].includes(String(task.status || '').toLowerCase())).length
  const percent = tasks.length ? Math.round((done / tasks.length) * 100) : 0
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['study-plan'] })
  const generate = useMutation({ mutationFn: generateStudyPlanApi, onSuccess: () => { message.success('学习计划已生成'); setOpen(false); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '生成计划失败')) })
  const refresh = useMutation({ mutationFn: () => refreshStudyPlanApi(String((plan.data as any)?.id || (plan.data as any)?.planId)), onSuccess: () => { message.success('计划已刷新'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '刷新失败')) })
  const updateTask = useMutation({ mutationFn: ({ id, status }: { id: string; status: string }) => updateStudyPlanTaskStatusApi(id, { status }), onSuccess: () => { message.success('任务状态已更新'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '状态更新失败')) })
  return (
    <ModulePage
      title="学习计划"
      description="根据题库、面试和画像自动刷新今日行动。"
      metrics={[
        { label: '任务数', value: tasks.length, hint: '当前计划' },
        { label: '完成率', value: `${percent}%`, hint: '已完成任务占比' },
        { label: '计划周期', value: (plan.data as any)?.durationDays ?? '-', hint: '天' },
        { label: '状态', value: plan.isFetching ? '刷新中' : '已同步', hint: '当前数据' }
      ]}
      actions={<Space><Button type="primary" onClick={() => setOpen(true)}>生成计划</Button><Button loading={refresh.isPending} disabled={!(plan.data as any)?.id && !(plan.data as any)?.planId} onClick={() => refresh.mutate()}>刷新</Button></Space>}
    >
      <div className="workspace-grid two">
        <Card title="完成进度" className="surface-card"><Progress percent={percent} /><p className="muted-text">完成 {done} / {tasks.length} 个任务。</p></Card>
        <DataTableCard
          title="任务列表"
          data={tasks}
          loading={plan.isLoading}
          error={plan.error}
          onRetry={() => plan.refetch()}
          emptyTitle="还没有学习计划，点击右上角“生成计划”开始"
          columns={[
            { title: '任务', render: (_, row) => pickText(row, ['title', 'name', 'taskName'], '学习任务') },
            { title: '方向', render: (_, row) => pickText(row, ['category', 'focusDirection', 'type']) },
            { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> },
            { title: '操作', render: (_, row) => <Space wrap><Button size="small" type="primary" onClick={() => navigate(taskRoute(row))}>去练习</Button><Button size="small" onClick={() => updateTask.mutate({ id: String(row.id || row.taskId), status: 'completed' })}>完成</Button><Button size="small" onClick={() => updateTask.mutate({ id: String(row.id || row.taskId), status: 'in_progress' })}>进行中</Button><Button size="small" onClick={() => updateTask.mutate({ id: String(row.id || row.taskId), status: 'not_started' })}>重置</Button></Space> }
          ]}
        />
      </div>
      <Modal open={open} title="生成学习计划" footer={null} onCancel={() => setOpen(false)}>
        <Form layout="vertical" onFinish={(values) => generate.mutate(values)} initialValues={{ durationDays: 7 }}>
          <Form.Item label="周期天数" name="durationDays"><InputNumber min={7} max={30} style={{ width: '100%' }} /></Form.Item>
          <Form.Item label="目标岗位" name="targetRole"><Input /></Form.Item>
          <Form.Item label="技术栈" name="techStack"><Input /></Form.Item>
          <Form.Item label="重点方向" name="focusDirection"><Input /></Form.Item>
          <Button type="primary" htmlType="submit" loading={generate.isPending}>生成</Button>
        </Form>
      </Modal>
    </ModulePage>
  )
}
