import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Descriptions, Form, Input, List, Result, Select, Space, Timeline } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { addApplicationEventApi, fetchApplicationDetailApi, refreshApplicationAnalysisApi, updateApplicationStatusApi } from '@/api/modules/applications'
import { getErrorMessage } from '@/api/client'
import { applicationStatusLabel, DataListCard, formatDateTime, normalizeRecords, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

const STATUS_OPTIONS = [
  { value: 'APPLIED', label: '已投递' },
  { value: 'SCREENING', label: '筛选中' },
  { value: 'INTERVIEW', label: '面试中' },
  { value: 'OFFER', label: 'Offer' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'CLOSED', label: '已关闭' }
]

const EVENT_TYPE_OPTIONS = [
  { value: 'FOLLOW_UP', label: '跟进沟通' },
  { value: 'INTERVIEW', label: '面试安排' },
  { value: 'ASSESSMENT', label: '笔试/测评' },
  { value: 'OFFER', label: 'Offer 进展' },
  { value: 'NOTE', label: '备注' }
]

export function ApplicationDetailPage() {
  const params = useParams()
  const id = String(params.id)
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [eventForm] = Form.useForm()
  const detail = useQuery({
    queryKey: ['applications', id],
    queryFn: () => fetchApplicationDetailApi(id).then((response) => response.data),
    enabled: Boolean(params.id)
  })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['applications', id] })
  const updateStatus = useMutation({ mutationFn: (status: string) => updateApplicationStatusApi(id, { status }), onSuccess: () => { message.success('状态已更新'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '状态更新失败')) })
  const addEvent = useMutation({ mutationFn: (payload: Record<string, unknown>) => addApplicationEventApi(id, payload), onSuccess: () => { message.success('跟进事件已记录'); eventForm.resetFields(); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '记录失败')) })
  const refresh = useMutation({ mutationFn: () => refreshApplicationAnalysisApi(id), onSuccess: () => { message.success('分析已刷新'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '刷新分析失败')) })

  const record = detail.data as any
  const events = normalizeRecords(record?.events || record?.timeline || record?.records)
  const suggestions = normalizeRecords(record?.suggestions || record?.analysis?.suggestions || record?.nextActions)
  const currentStatus = pickText(record, ['status'], 'APPLIED').toUpperCase()

  return (
    <ModulePage
      title="投递详情"
      description="查看并管理某一岗位的状态、跟进事件和分析建议。"
      actions={<Space wrap>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/applications')}>返回投递看板</Button>
        <Button type="primary" loading={refresh.isPending} onClick={() => refresh.mutate()}>刷新匹配分析</Button>
      </Space>}
      metrics={[
        { label: '状态', value: applicationStatusLabel(currentStatus), hint: '当前阶段' },
        { label: '匹配度', value: pickText(record, ['matchScore', 'score'], '-'), hint: '岗位匹配' },
        { label: '事件', value: events.length, hint: '跟进记录' },
        { label: '建议', value: suggestions.length, hint: '分析输出' }
      ]}
    >
      {detail.error ? (
        <Result status="warning" title="加载投递详情失败" subTitle={getErrorMessage(detail.error)} extra={<Button onClick={() => detail.refetch()}>重试</Button>} />
      ) : (
        <>
          <div className="workspace-grid two">
            <Card className="surface-card" loading={detail.isLoading} title="投递信息">
              <Descriptions column={1} items={[
                { label: '公司', children: pickText(record, ['companyName', 'company']) },
                { label: '岗位', children: pickText(record, ['position', 'jobTitle']) },
                { label: '当前状态', children: applicationStatusLabel(currentStatus) },
                { label: '更新时间', children: formatDateTime(record?.updateTime || record?.updatedAt) }
              ]} />
              <Space wrap style={{ marginTop: 16 }}>
                <span className="muted-text">更新状态：</span>
                <Select
                  style={{ width: 160 }}
                  value={currentStatus}
                  options={STATUS_OPTIONS}
                  loading={updateStatus.isPending}
                  onChange={(status) => updateStatus.mutate(status)}
                />
              </Space>
            </Card>
            <Card className="surface-card" title="添加跟进事件">
              <Form form={eventForm} layout="vertical" onFinish={(values) => addEvent.mutate(values)}>
                <Form.Item label="事件类型" name="type" rules={[{ required: true, message: '请选择事件类型' }]}>
                  <Select options={EVENT_TYPE_OPTIONS} placeholder="选择类型" />
                </Form.Item>
                <Form.Item label="标题" name="title" rules={[{ required: true, message: '请填写标题' }]}>
                  <Input placeholder="例如：HR 电话沟通、一面已约" />
                </Form.Item>
                <Form.Item label="详情" name="content">
                  <Input.TextArea rows={4} placeholder="补充本次跟进的细节（可选）" />
                </Form.Item>
                <Button type="primary" htmlType="submit" loading={addEvent.isPending}>记录事件</Button>
              </Form>
            </Card>
          </div>
          <Card className="surface-card" title="事件时间线" loading={detail.isLoading} style={{ marginTop: 16 }}>
            {events.length === 0 ? (
              <div className="muted-text">暂无跟进事件，使用上方表单记录第一条。</div>
            ) : (
              <Timeline items={events.map((item) => ({
                children: (
                  <div>
                    <strong>{pickText(item, ['title', 'type', 'status'])}</strong>
                    <div className="muted-text" style={{ fontSize: 12 }}>{formatDateTime(item.time || item.createTime || item.createdAt)}</div>
                    {pickText(item, ['content', 'description'], '') !== '-' && <div>{pickText(item, ['content', 'description'], '')}</div>}
                  </div>
                )
              }))} />
            )}
          </Card>
          <DataListCard
            title="分析建议"
            data={suggestions}
            loading={detail.isLoading}
            error={detail.error}
            onRetry={() => detail.refetch()}
            emptyTitle="暂无分析建议，点击右上角“刷新匹配分析”生成"
            renderItem={(item) => <List.Item.Meta title={pickText(item, ['title', 'name', 'summary'])} description={pickText(item, ['description', 'content'])} />}
          />
        </>
      )}
    </ModulePage>
  )
}
