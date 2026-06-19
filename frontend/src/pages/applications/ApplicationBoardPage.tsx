import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Form, Input, Modal, Popconfirm, Select, Space, Tag } from 'antd'
import { Link } from 'react-router-dom'
import { createJobApplicationApi, fetchApplicationBoardApi, refreshApplicationAnalysisApi, updateApplicationStatusApi } from '@/api/modules/applications'
import { getErrorMessage } from '@/api/client'
import { applicationStatusLabel, DataTableCard, normalizeRecords, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

const STATUS_OPTIONS = [
  { value: 'APPLIED', label: '已投递' },
  { value: 'SCREENING', label: '筛选中' },
  { value: 'INTERVIEW', label: '面试中' },
  { value: 'OFFER', label: 'Offer' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'CLOSED', label: '已关闭' }
]

export function ApplicationBoardPage() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [filters, setFilters] = useState({ keyword: '', status: undefined as string | undefined })
  const query = useQuery({ queryKey: ['applications', filters], queryFn: () => fetchApplicationBoardApi(filters).then((response) => response.data) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['applications'] })
  const create = useMutation({ mutationFn: createJobApplicationApi, onSuccess: () => { message.success('投递已创建'); setOpen(false); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '创建失败')) })
  const updateStatus = useMutation({ mutationFn: ({ id, status }: { id: string; status: string }) => updateApplicationStatusApi(id, { status }), onSuccess: () => { message.success('状态已更新'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '状态更新失败')) })
  const refresh = useMutation({ mutationFn: (id: string) => refreshApplicationAnalysisApi(id), onSuccess: () => { message.success('分析已刷新'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '刷新分析失败')) })
  const rows = normalizeRecords(query.data)
  const confirmStatusChange = (id: string, status: string) => modal.confirm({ title: '确认更新投递状态？', content: `将状态调整为「${applicationStatusLabel(status)}」。`, okText: '更新', cancelText: '取消', onOk: () => updateStatus.mutate({ id, status }) })
  return (
    <ModulePage
      title="投递看板"
      description="追踪岗位、状态、事件和分析。"
      metrics={[
        { label: '投递数', value: rows.length, hint: '当前看板' },
        { label: '面试中', value: rows.filter((row) => pickText(row, ['status']).toUpperCase().includes('INTERVIEW')).length, hint: '重点跟进' },
        { label: '待分析', value: rows.filter((row) => !row.analysis).length, hint: '可刷新分析' },
        { label: '状态', value: query.isFetching ? '刷新中' : '已同步', hint: '看板数据' }
      ]}
      actions={<Space wrap><Input.Search allowClear placeholder="搜索公司或岗位" onSearch={(keyword) => setFilters((current) => ({ ...current, keyword }))} /><Select allowClear placeholder="状态" style={{ width: 130 }} options={STATUS_OPTIONS} onChange={(status) => setFilters((current) => ({ ...current, status }))} /><Button type="primary" onClick={() => setOpen(true)}>新增投递</Button></Space>}
    >
      <DataTableCard
        title="投递列表"
        data={query.data}
        loading={query.isLoading}
        error={query.error}
        onRetry={() => query.refetch()}
        columns={[
          { title: '公司 / 岗位', render: (_, row) => <Link to={`/applications/${row.id || row.applicationId}`}>{pickText(row, ['companyName', 'company'])} / {pickText(row, ['position', 'jobTitle'])}</Link> },
          { title: '状态', render: (_, row) => <Tag color="blue">{applicationStatusLabel(pickText(row, ['status']))}</Tag> },
          { title: '匹配度', render: (_, row) => pickText(row, ['matchScore', 'score'], '-') },
          { title: '操作', render: (_, row) => <Space><Select size="small" style={{ width: 120 }} value={pickText(row, ['status'], 'APPLIED')} options={STATUS_OPTIONS} onChange={(status) => confirmStatusChange(String(row.id || row.applicationId), status)} /><Popconfirm title="刷新投递分析" description="系统会重新生成匹配度和行动建议。" onConfirm={() => refresh.mutate(String(row.id || row.applicationId))}><Button size="small" loading={refresh.isPending}>刷新分析</Button></Popconfirm></Space> }
        ]}
      />
      <Modal open={open} title="新增投递" footer={null} onCancel={() => setOpen(false)}>
        <Form layout="vertical" onFinish={(values) => create.mutate(values)}>
          <Form.Item label="公司" name="companyName" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item label="岗位" name="position" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item label="JD" name="jd"><Input.TextArea rows={5} /></Form.Item>
          <Button type="primary" htmlType="submit" loading={create.isPending}>保存</Button>
        </Form>
      </Modal>
    </ModulePage>
  )
}
