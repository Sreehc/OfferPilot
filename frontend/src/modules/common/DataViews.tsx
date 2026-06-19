import type { ReactNode } from 'react'
import { Card, Empty, List, Space, Table, Tag, Typography, type TableProps } from 'antd'
import type { AnyRecord } from '@/api/types'
import { getRecordId, normalizeRecords, pickText } from './data'
import { labelOf } from './labels'
import { StateView } from '@/components/feedback/StateView'

export function DataListCard({
  title,
  data,
  loading,
  error,
  onRetry,
  emptyTitle = '暂无数据',
  renderItem,
  actions
}: {
  title: string
  data: unknown
  loading?: boolean
  error?: unknown
  onRetry?: () => void
  emptyTitle?: string
  renderItem: (item: AnyRecord) => ReactNode
  actions?: ReactNode
}) {
  const rows = normalizeRecords(data)
  return (
    <Card title={title} extra={actions} className="surface-card">
      <StateView loading={loading} error={error} empty={!loading && !error && rows.length === 0} emptyTitle={emptyTitle} onRetry={onRetry}>
        <List dataSource={rows} renderItem={(item) => <List.Item key={getRecordId(item)}>{renderItem(item)}</List.Item>} />
      </StateView>
    </Card>
  )
}

export function DataTableCard<T extends AnyRecord>({
  title,
  data,
  loading,
  error,
  onRetry,
  columns,
  actions,
  emptyTitle = '暂无数据',
  rowSelection
}: {
  title: string
  data: unknown
  loading?: boolean
  error?: unknown
  onRetry?: () => void
  columns: TableProps<T>['columns']
  actions?: ReactNode
  emptyTitle?: string
  rowSelection?: TableProps<T>['rowSelection']
}) {
  const rows = normalizeRecords<T>(data)
  return (
    <Card title={title} extra={actions} className="surface-card">
      <StateView loading={loading} error={error} empty={!loading && !error && rows.length === 0} emptyTitle={emptyTitle} onRetry={onRetry}>
        <Table<T>
          rowKey={(row) => getRecordId(row)}
          columns={columns}
          dataSource={rows}
          pagination={{ pageSize: 8, showSizeChanger: false }}
          rowSelection={rowSelection}
          size="middle"
          scroll={{ x: 'max-content' }}
        />
      </StateView>
    </Card>
  )
}

export function EntitySummary({ record, fields }: { record?: AnyRecord; fields: Array<{ label: string; keys: string[]; tag?: boolean }> }) {
  if (!record) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无详情" />
  return (
    <div className="entity-summary">
      {fields.map((field) => {
        const value = pickText(record, field.keys)
        return (
          <div className="entity-summary-item" key={field.label}>
            <Typography.Text type="secondary">{field.label}</Typography.Text>
            <div>{field.tag ? <Tag color="blue">{value}</Tag> : <Typography.Text strong>{value}</Typography.Text>}</div>
          </div>
        )
      })}
    </div>
  )
}

export function StatusTag({ value }: { value?: string }) {
  const text = value || 'UNKNOWN'
  const normalized = text.toUpperCase()
  const color = normalized.includes('FAIL') || normalized.includes('REJECT') || normalized.includes('BAN') ? 'red' : normalized.includes('PENDING') || normalized.includes('WAIT') ? 'gold' : normalized.includes('RUN') || normalized.includes('PROCESS') ? 'blue' : normalized.includes('DONE') || normalized.includes('SUCCESS') || normalized.includes('ACTIVE') ? 'green' : 'default'
  return <Tag color={color}>{labelOf(text)}</Tag>
}

export function InlineActions({ children }: { children: ReactNode }) {
  return <Space size={6} wrap>{children}</Space>
}
