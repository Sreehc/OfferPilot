import { useMemo, useState, type Key } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Empty, Form, Input, Modal, Popconfirm, Select, Space, Tag, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { createJobApplicationApi, fetchApplicationBoardApi, refreshApplicationAnalysisApi, updateApplicationStatusApi } from '@/api/modules/applications'
import { getErrorMessage } from '@/api/client'
import type { AnyRecord } from '@/api/types'
import { applicationStatusLabel, DataTableCard, formatDateTime, getRecordId, normalizeRecords, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

const { Paragraph, Text } = Typography

const STATUS_OPTIONS = [
  { value: 'SAVED', label: '待投递' },
  { value: 'APPLIED', label: '已投递' },
  { value: 'SCREENING', label: '筛选中' },
  { value: 'INTERVIEW', label: '面试中' },
  { value: 'OFFER', label: 'Offer' },
  { value: 'REJECTED', label: '已拒绝' }
]

const KANBAN_COLUMNS = [...STATUS_OPTIONS, { value: 'UNKNOWN', label: '未知状态' }]

const STATUS_ALIASES: Record<string, string> = {
  saved: 'SAVED',
  draft: 'SAVED',
  applied: 'APPLIED',
  written: 'SCREENING',
  screening: 'SCREENING',
  interview: 'INTERVIEW',
  interviewing: 'INTERVIEW',
  offer: 'OFFER',
  rejected: 'REJECTED',
  closed: 'REJECTED'
}

function applicationId(record: AnyRecord) {
  return String(record.id ?? record.applicationId ?? '')
}

function normalizedStatus(record: AnyRecord) {
  const raw = pickText(record, ['kanbanStatus', 'boardStatus', 'status'], 'UNKNOWN')
  const normalized = STATUS_ALIASES[raw.trim().toLowerCase()] || raw.trim().toUpperCase()
  return KANBAN_COLUMNS.some((column) => column.value === normalized) ? normalized : 'UNKNOWN'
}

function displayCompany(record: AnyRecord) {
  return pickText(record, ['companyName', 'company'], '未命名公司')
}

function displayPosition(record: AnyRecord) {
  return pickText(record, ['position', 'jobTitle', 'title'], '未命名岗位')
}

function displayMatchScore(record: AnyRecord) {
  return pickText(record, ['matchScoreDisplay', 'matchScore', 'score'], '-')
}

function latestEvent(record: AnyRecord) {
  const direct = record.recentEvent || record.latestEvent
  if (direct && typeof direct === 'object') return direct as AnyRecord
  const events = normalizeRecords<AnyRecord>(record.events || record.timeline || record.records)
  return events[0]
}

function nextAction(record: AnyRecord) {
  const analysis = record.analysis && typeof record.analysis === 'object' ? record.analysis as AnyRecord : undefined
  const suggestion = normalizeRecords<AnyRecord>(record.suggestions || analysis?.suggestions || record.nextActions)[0]
  return pickText(record, ['nextAction', 'nextStepSuggestion', 'suggestion'], pickText(analysis, ['nextAction', 'suggestion'], pickText(suggestion, ['title', 'summary', 'content'], '暂无下一步建议')))
}

function displayStatusLabel(record: AnyRecord) {
  return pickText(record, ['statusLabel'], applicationStatusLabel(normalizedStatus(record)))
}

export function ApplicationBoardPage() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [view, setView] = useState<'kanban' | 'table'>('kanban')
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([])
  const [filters, setFilters] = useState({ keyword: '', status: undefined as string | undefined })
  const query = useQuery({ queryKey: ['applications', filters], queryFn: () => fetchApplicationBoardApi(filters).then((response) => response.data) })
  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['applications'] })
  }
  const create = useMutation({
    mutationFn: createJobApplicationApi,
    onSuccess: () => { message.success('投递已创建'); setOpen(false); invalidate() },
    onError: (error) => message.error(getErrorMessage(error, '创建失败'))
  })
  const updateStatus = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) => updateApplicationStatusApi(id, { status }),
    onSuccess: () => { message.success('状态已更新'); invalidate() },
    onError: (error) => message.error(getErrorMessage(error, '状态更新失败'))
  })
  const refresh = useMutation({
    mutationFn: (id: string) => refreshApplicationAnalysisApi(id),
    onSuccess: () => { message.success('分析已刷新'); invalidate() },
    onError: (error) => message.error(getErrorMessage(error, '刷新分析失败'))
  })
  const rows = normalizeRecords<AnyRecord>(query.data)
  const grouped = useMemo(() => {
    const map = new Map<string, AnyRecord[]>()
    KANBAN_COLUMNS.forEach((column) => map.set(column.value, []))
    rows.forEach((row) => {
      const status = normalizedStatus(row)
      const list = map.get(status) || map.get('UNKNOWN') || []
      list.push(row)
    })
    return map
  }, [rows])

  const confirmStatusChange = (id: string, status: string) => modal.confirm({
    title: '确认更新投递状态？',
    content: `将状态调整为「${applicationStatusLabel(status)}」。`,
    okText: '更新',
    cancelText: '取消',
    onOk: () => updateStatus.mutate({ id, status })
  })
  const batchRefresh = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要刷新分析的投递')
      return
    }
    selectedRowKeys.forEach((id) => refresh.mutate(String(id)))
  }

  return (
    <ModulePage
      title="投递看板"
      description="追踪岗位、状态、事件和分析。"
      metrics={[
        { label: '投递数', value: rows.length, hint: '当前看板' },
        { label: '面试中', value: rows.filter((row) => normalizedStatus(row) === 'INTERVIEW').length, hint: '重点跟进' },
        { label: '待分析', value: rows.filter((row) => !row.analysis && displayMatchScore(row) === '-').length, hint: '可刷新分析' },
        { label: '状态', value: query.isFetching ? '刷新中' : '已同步', hint: '看板数据' }
      ]}
      actions={<Button type="primary" onClick={() => setOpen(true)}>新增投递</Button>}
      state={{
        loading: query.isLoading,
        error: query.error,
        onRetry: () => { void query.refetch() },
        empty: !query.isLoading && !query.error && rows.length === 0,
        emptyTitle: '暂无投递',
        emptyDescription: '新增第一条岗位投递后，可在 Kanban 看板中跟进状态和 AI 建议。'
      }}
    >
      <div className="application-toolbar surface-card">
        <div className="application-view-switch" aria-label="视图切换">
          <Button type={view === 'kanban' ? 'primary' : 'default'} onClick={() => setView('kanban')}>Kanban 看板</Button>
          <Button type={view === 'table' ? 'primary' : 'default'} onClick={() => setView('table')}>表格高级视图</Button>
        </div>
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="搜索公司或岗位"
            onSearch={(keyword) => setFilters((current) => ({ ...current, keyword }))}
          />
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 140 }}
            options={STATUS_OPTIONS}
            onChange={(status) => setFilters((current) => ({ ...current, status }))}
          />
        </Space>
      </div>

      {view === 'kanban' ? (
        <section className="application-kanban" role="region" aria-label="投递 Kanban 看板">
          {KANBAN_COLUMNS.map((column) => {
            const columnRows = grouped.get(column.value) || []
            return (
              <section key={column.value} className="application-kanban-column" aria-label={column.label}>
                <div className="application-kanban-head">
                  <Typography.Title level={3}>{column.label}</Typography.Title>
                  <Tag>{columnRows.length}</Tag>
                </div>
                <div className="application-card-list">
                  {columnRows.length === 0 ? (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无岗位" />
                  ) : columnRows.map((row) => {
                    const id = applicationId(row)
                    const event = latestEvent(row)
                    return (
                      <article key={id || getRecordId(row)} className="application-kanban-card">
                        <div className="application-card-head">
                          <div>
                            <Text strong>{displayCompany(row)}</Text>
                            <Paragraph>{displayPosition(row)}</Paragraph>
                          </div>
                          <div className="application-match-score" aria-label="AI 匹配分">
                            <span>{displayMatchScore(row)}</span>
                            <small>匹配分</small>
                          </div>
                        </div>
                        <div className="application-next-action">
                          <Text type="secondary">下一步建议</Text>
                          <Paragraph>{nextAction(row)}</Paragraph>
                        </div>
                        <div className="application-recent-event">
                          <Text type="secondary">最近事件</Text>
                          <Paragraph>{event ? pickText(event, ['title', 'type', 'status'], '跟进事件') : '暂无跟进事件'}</Paragraph>
                          {event && <Text type="secondary">{formatDateTime(event.eventTime || event.time || event.createTime || event.createdAt)}</Text>}
                        </div>
                        <div className="application-card-actions">
                          <Select
                            aria-label={`更新状态：${displayCompany(row)} ${displayPosition(row)}`}
                            size="small"
                            value={normalizedStatus(row)}
                            options={STATUS_OPTIONS}
                            loading={updateStatus.isPending}
                            onChange={(status) => confirmStatusChange(id, status)}
                          />
                          <Popconfirm title="刷新投递分析" description="系统会重新生成匹配度和行动建议。" onConfirm={() => refresh.mutate(id)}>
                            <Button size="small" loading={refresh.isPending}>刷新分析</Button>
                          </Popconfirm>
                          <Link className="application-detail-link" aria-label={`查看详情 ${displayCompany(row)} ${displayPosition(row)}`} to={`/applications/${id}`}>查看详情</Link>
                        </div>
                      </article>
                    )
                  })}
                </div>
              </section>
            )
          })}
        </section>
      ) : (
        <section role="region" aria-label="表格高级视图" className="application-table-view">
          <Card className="surface-card application-advanced-filter">
            <div className="application-advanced-filter-head">
              <Text strong>高级筛选</Text>
              <Button onClick={batchRefresh} loading={refresh.isPending}>批量刷新分析</Button>
            </div>
            <Space wrap>
              <Input.Search
                allowClear
                placeholder="搜索公司或岗位"
                onSearch={(keyword) => setFilters((current) => ({ ...current, keyword }))}
              />
              <Select
                allowClear
                placeholder="状态"
                style={{ width: 160 }}
                options={STATUS_OPTIONS}
                onChange={(status) => setFilters((current) => ({ ...current, status }))}
              />
            </Space>
          </Card>
          <DataTableCard
            title="投递列表"
            data={query.data}
            loading={query.isLoading}
            error={query.error}
            onRetry={() => query.refetch()}
            rowSelection={{
              selectedRowKeys,
              onChange: (keys) => setSelectedRowKeys(keys)
            }}
            columns={[
              { title: '公司', sorter: (a, b) => displayCompany(a).localeCompare(displayCompany(b)), render: (_, row) => <Link to={`/applications/${applicationId(row)}`}>{displayCompany(row)}</Link> },
              { title: '岗位', sorter: (a, b) => displayPosition(a).localeCompare(displayPosition(b)), render: (_, row) => displayPosition(row) },
              { title: '状态', filters: STATUS_OPTIONS.map((item) => ({ text: item.label, value: item.value })), onFilter: (value, row) => normalizedStatus(row) === value, render: (_, row) => <Tag color="blue">{displayStatusLabel(row)}</Tag> },
              { title: '匹配度', sorter: (a, b) => Number(displayMatchScore(a)) - Number(displayMatchScore(b)), render: (_, row) => displayMatchScore(row) },
              { title: '下一步建议', render: (_, row) => nextAction(row) },
              { title: '操作', render: (_, row) => <Space><Select size="small" style={{ width: 120 }} value={normalizedStatus(row)} options={STATUS_OPTIONS} onChange={(status) => confirmStatusChange(applicationId(row), status)} /><Popconfirm title="刷新投递分析" description="系统会重新生成匹配度和行动建议。" onConfirm={() => refresh.mutate(applicationId(row))}><Button size="small" loading={refresh.isPending}>刷新分析</Button></Popconfirm></Space> }
            ]}
          />
        </section>
      )}

      <Modal open={open} title="新增投递" footer={null} onCancel={() => setOpen(false)}>
        <Form layout="vertical" onFinish={(values) => create.mutate(values)}>
          <Form.Item label="公司" name="companyName" rules={[{ required: true, message: '请填写公司' }]}><Input /></Form.Item>
          <Form.Item label="岗位" name="position" rules={[{ required: true, message: '请填写岗位' }]}><Input /></Form.Item>
          <Form.Item label="JD" name="jd"><Input.TextArea rows={5} /></Form.Item>
          <Button type="primary" htmlType="submit" loading={create.isPending}>保存</Button>
        </Form>
      </Modal>
    </ModulePage>
  )
}
