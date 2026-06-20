import { Button, Empty, Result, Skeleton, Space, Spin, Typography } from 'antd'
import type { ReactNode } from 'react'

export interface StateViewProps {
  loading?: boolean
  error?: unknown
  empty?: boolean
  permission?: boolean | string
  emptyTitle?: string
  emptyDescription?: string
  emptyAction?: ReactNode
  errorTitle?: string
  errorDescription?: string
  errorAction?: ReactNode
  permissionTitle?: string
  permissionDescription?: string
  permissionAction?: ReactNode
  onRetry?: () => void
  children: ReactNode
}

function errorMessage(error: unknown, fallback: string) {
  if (typeof error === 'object' && error && 'message' in error) {
    return String((error as { message?: unknown }).message || fallback)
  }
  return fallback
}

export function StateView({
  loading,
  error,
  empty,
  permission,
  emptyTitle = '暂无数据',
  emptyDescription,
  emptyAction,
  errorTitle = '加载失败',
  errorDescription,
  errorAction,
  permissionTitle = '无权限访问',
  permissionDescription = '当前账号没有访问该内容的权限。',
  permissionAction,
  onRetry,
  children
}: StateViewProps) {
  if (loading) {
    return (
      <div className="surface-card state-view state-view-loading" aria-busy="true" aria-live="polite">
        <Skeleton active paragraph={{ rows: 5 }} />
      </div>
    )
  }
  if (permission) {
    return (
      <Result
        className="state-view state-view-permission"
        status="403"
        title={permissionTitle}
        subTitle={permissionDescription}
        extra={permissionAction}
      />
    )
  }
  if (error) {
    return (
      <Result
        className="state-view state-view-error"
        status="warning"
        title={errorTitle}
        subTitle={errorDescription || errorMessage(error, '请稍后重试')}
        extra={(
          <Space wrap>
            {onRetry && <Button aria-label="重试" onClick={onRetry}>重试</Button>}
            {errorAction}
          </Space>
        )}
      />
    )
  }
  if (empty) {
    return (
      <div className="state-view state-view-empty">
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={(
            <Space orientation="vertical" size={8}>
              <Typography.Text strong>{emptyTitle}</Typography.Text>
              {emptyDescription && <Typography.Text type="secondary">{emptyDescription}</Typography.Text>}
              {emptyAction}
            </Space>
          )}
        />
      </div>
    )
  }
  return <>{children}</>
}

export function PageSpin() {
  return <div className="state-view-page-spin" role="status" aria-label="页面加载中"><Spin /></div>
}
