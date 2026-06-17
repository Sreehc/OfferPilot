import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Form, Input, Modal, Space } from 'antd'
import { useState } from 'react'
import { createCopilotPrepSessionApi, createCopilotRealtimeSessionApi, createJobPrepSessionApi, fetchInterviewHistoryApi, fetchInterviewTrendApi, fetchLatestCopilotRealtimeSessionApi, startInterviewApi } from '@/api/modules/interview'
import { getErrorMessage } from '@/api/client'
import { DataTableCard, formatDateTime, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { EChartCard } from '@/components/charts/EChartCard'
import { ModulePage } from '@/modules/common'

export function InterviewPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const history = useQuery({ queryKey: ['interview', 'history'], queryFn: () => fetchInterviewHistoryApi().then((response) => response.data) })
  const trend = useQuery({ queryKey: ['interview', 'trend'], queryFn: () => fetchInterviewTrendApi().then((response) => response.data) })
  const latestRealtime = useQuery({ queryKey: ['interview', 'copilot', 'latest'], queryFn: () => fetchLatestCopilotRealtimeSessionApi().then((response) => response.data) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['interview'] })
  const start = useMutation({ mutationFn: startInterviewApi, onSuccess: () => { message.success('已开始模拟面试'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '开始失败')) })
  const prep = useMutation({ mutationFn: createJobPrepSessionApi, onSuccess: () => { message.success('已创建 JD 备面会话'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '创建失败')) })
  const copilotPrep = useMutation({ mutationFn: createCopilotPrepSessionApi, onSuccess: () => { message.success('已创建 Copilot 备面会话'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '创建失败')) })
  const copilotRealtime = useMutation({ mutationFn: createCopilotRealtimeSessionApi, onSuccess: () => { message.success('已创建实时会话'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '创建失败')) })
  const rows = normalizeRecords(history.data)
  const trendRows = normalizeRecords(trend.data)
  const trendOption = {
    xAxis: { type: 'category', data: trendRows.map((row, index) => pickText(row, ['date', 'time', 'label'], String(index + 1))) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: trendRows.map((row) => Number(row.score || row.value || row.count || 0)) }]
  }
  return (
    <ModulePage
      title="模拟面试"
      description="覆盖模拟面试、JD 备面、实时 Copilot 与录音复盘。"
      actions={<Space><Button type="primary" onClick={() => setOpen(true)}>开始模拟面试</Button><Button onClick={() => prep.mutate({})}>JD 备面</Button><Button onClick={() => copilotPrep.mutate({})}>Copilot 备面</Button><Button onClick={() => copilotRealtime.mutate({})}>实时 Copilot</Button></Space>}
      metrics={[
        { label: '历史记录', value: rows.length, hint: '面试会话' },
        { label: '趋势点', value: trendRows.length, hint: '能力变化' },
        { label: '最新会话', value: rows[0] ? formatDateTime(rows[0].createTime || rows[0].updateTime) : '-', hint: '最近活跃' },
        { label: '状态', value: latestRealtime.data ? '实时会话可用' : (history.isFetching ? '刷新中' : '已同步'), hint: '会话列表' }
      ]}
    >
      <div className="workspace-grid two">
        <div className="workspace-grid">
          <EChartCard title="面试趋势" option={trendOption} />
          <DataTableCard
            title="历史记录"
            data={history.data}
            loading={history.isLoading}
            error={history.error}
            onRetry={() => history.refetch()}
            columns={[
              { title: '会话', render: (_, row) => pickText(row, ['title', 'sessionTitle'], '面试会话') },
              { title: '模式', render: (_, row) => pickText(row, ['mode'], 'standard') },
              { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> },
              { title: '时间', render: (_, row) => formatDateTime(row.time || row.createTime) }
            ]}
          />
        </div>
        <Card title="实时 Copilot" className="surface-card">
          <Space direction="vertical">
            <div>{latestRealtime.data ? '当前存在实时 Copilot 会话，可继续跟进。' : '当前没有实时 Copilot 会话。'}</div>
            <Button onClick={() => start.mutate({ mode: 'standard' })}>进入 Copilot</Button>
          </Space>
        </Card>
      </div>
      <Modal open={open} title="开始模拟面试" footer={null} onCancel={() => setOpen(false)}>
        <Form layout="vertical" onFinish={(values) => start.mutate(values)}>
          <Form.Item label="岗位" name="jobTitle" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item label="公司" name="company"><Input /></Form.Item>
          <Form.Item label="说明" name="description"><Input.TextArea rows={5} /></Form.Item>
          <Button type="primary" htmlType="submit" loading={start.isPending}>开始</Button>
        </Form>
      </Modal>
    </ModulePage>
  )
}
