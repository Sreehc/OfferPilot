import { Button, Empty, Result, Skeleton, Spin } from 'antd'
import type { ReactNode } from 'react'

interface StateViewProps {
  loading?: boolean
  error?: unknown
  empty?: boolean
  emptyTitle?: string
  onRetry?: () => void
  children: ReactNode
}

export function StateView({ loading, error, empty, emptyTitle = '暂无数据', onRetry, children }: StateViewProps) {
  if (loading) return <div className="surface-card" style={{ padding: 24 }}><Skeleton active paragraph={{ rows: 5 }} /></div>
  if (error) return <Result status="warning" title="加载失败" subTitle={String((error as Error)?.message || '请稍后重试')} extra={onRetry && <Button onClick={onRetry}>重试</Button>} />
  if (empty) return <Empty description={emptyTitle} />
  return <>{children}</>
}

export function PageSpin() {
  return <div style={{ minHeight: 320, display: 'grid', placeItems: 'center' }}><Spin /></div>
}
