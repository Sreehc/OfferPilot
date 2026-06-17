import type { ReactNode } from 'react'
import { Space, Tag } from 'antd'

export interface MetricItem {
  label: string
  value: string | number
  hint?: string
  tone?: 'accent' | 'success' | 'warning' | 'danger'
}

interface WorkspacePageProps {
  title: string
  description?: string
  chips?: Array<{ label: string; color?: string }>
  metrics?: MetricItem[]
  actions?: ReactNode
  side?: ReactNode
  children: ReactNode
}

export function MetricStrip({ items }: { items: MetricItem[] }) {
  if (!items.length) return null
  return (
    <div className="metric-strip">
      {items.map((item) => <div className="metric-card" key={item.label}><div className="label">{item.label}</div><div className="value">{item.value}</div>{item.hint && <div className="muted-text" style={{ marginTop: 8, fontSize: 12 }}>{item.hint}</div>}</div>)}
    </div>
  )
}

export function WorkspacePage({ title, description, chips = [], metrics = [], actions, side, children }: WorkspacePageProps) {
  return (
    <section className="workspace-page">
      <header className="workspace-header">
        <div>
          <Space size={6} wrap>{chips.map((chip) => <Tag key={chip.label} color={chip.color}>{chip.label}</Tag>)}</Space>
          <h1 className="workspace-title">{title}</h1>
          {description && <p className="workspace-description">{description}</p>}
        </div>
        {actions && <div>{actions}</div>}
      </header>
      <MetricStrip items={metrics} />
      <div className={side ? 'workspace-grid two' : 'workspace-grid'}>
        <main>{children}</main>
        {side && <aside>{side}</aside>}
      </div>
    </section>
  )
}
