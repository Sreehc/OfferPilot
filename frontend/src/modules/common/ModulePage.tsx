import type { ReactNode } from 'react'
import { WorkspacePage, type MetricItem } from '@/components/layout/WorkspacePage'
import { StateView, type StateViewProps } from '@/components/feedback/StateView'
import { fallbackMetrics } from './constants'

type ModulePageState = Omit<StateViewProps, 'children'>

export function ModulePage({
  title,
  description,
  children,
  metrics = fallbackMetrics,
  actions,
  side,
  state
}: {
  title: string
  description: string
  children: ReactNode
  metrics?: MetricItem[]
  actions?: ReactNode
  side?: ReactNode
  state?: ModulePageState
}) {
  return (
    <WorkspacePage title={title} description={description} metrics={metrics} actions={actions} side={side}>
      {state ? <StateView {...state}>{children}</StateView> : children}
    </WorkspacePage>
  )
}
