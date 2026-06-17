import type { ReactNode } from 'react'
import { WorkspacePage, type MetricItem } from '@/components/layout/WorkspacePage'
import { fallbackMetrics } from './constants'

export function ModulePage({ title, description, children, metrics = fallbackMetrics, actions, side }: { title: string; description: string; children: ReactNode; metrics?: MetricItem[]; actions?: ReactNode; side?: ReactNode }) {
  return <WorkspacePage title={title} description={description} metrics={metrics} actions={actions} side={side}>{children}</WorkspacePage>
}
