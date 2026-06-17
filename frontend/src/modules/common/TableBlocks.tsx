import type { ReactNode } from 'react'
import { Button, Card, List, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { Link } from 'react-router-dom'
import { StateView } from '@/components/feedback/StateView'

export function SimpleTable({ title, rows, actions }: { title: string; rows: Array<Record<string, any>>; actions?: ReactNode }) {
  const columns: ColumnsType<Record<string, any>> = [
    { title: '名称', dataIndex: 'name', key: 'name', render: (value) => <strong>{value}</strong> },
    { title: '状态', dataIndex: 'status', key: 'status', render: (value) => <Tag color={value === '完成' ? 'success' : value === '风险' ? 'error' : 'processing'}>{value || '进行中'}</Tag> },
    { title: '更新时间', dataIndex: 'time', key: 'time' }
  ]
  return <Card title={title} extra={actions} className="surface-card"><Table rowKey={(row) => row.id || row.name} size="middle" columns={columns} dataSource={rows} pagination={{ pageSize: 5 }} /></Card>
}

export function ActionList({ title, items }: { title: string; items: string[] }) {
  return <Card title={title} className="surface-card"><List dataSource={items} renderItem={(item) => <List.Item><Typography.Text>{item}</Typography.Text></List.Item>} /></Card>
}

export function PageDataState({ query, children }: { query: { isLoading: boolean; error: unknown; data: unknown; refetch: () => void }; children: ReactNode }) {
  return <StateView loading={query.isLoading} error={query.error} empty={!query.isLoading && !query.error && !query.data} onRetry={query.refetch}>{children}</StateView>
}

export function LinkButton({ to, children }: { to: string; children: ReactNode }) {
  return <Link to={to}><Button type="primary">{children}</Button></Link>
}
