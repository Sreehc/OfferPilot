import { Card, Typography } from 'antd'
import type { ReactNode } from 'react'

export function AuthFrame({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return <div style={{ minHeight: '100dvh', display: 'grid', placeItems: 'center', padding: 24, background: 'var(--op-bg)' }}><Card style={{ width: 'min(460px, 100%)' }} className="surface-card"><div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 18 }}><span className="app-logo-mark">OP</span><div><Typography.Title level={3} style={{ margin: 0 }}>{title}</Typography.Title><Typography.Text type="secondary">{description}</Typography.Text></div></div>{children}</Card></div>
}
