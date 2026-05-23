import type { DashboardOverview, StudyPlan, StudyPlanTaskItem } from '@/types/api'

export const getStudyPlanPrimaryActionPath = (
  overview: DashboardOverview | null,
  currentPlan: StudyPlan | null,
  nextTask: StudyPlanTaskItem | null
) => {
  return overview?.nextAction?.path || (currentPlan ? nextTask?.actionPath || '/dashboard' : '/study-plan#plan-builder')
}

export const getStudyPlanPrimaryActionLabel = (
  overview: DashboardOverview | null,
  currentPlan: StudyPlan | null,
  nextTask: StudyPlanTaskItem | null,
  moduleLabel: (value: string) => string
) => {
  if (overview?.nextAction?.title) return overview.nextAction.title
  if (!currentPlan) return '开始生成计划'
  if (currentPlan.todayFocusSummary?.state === 'completed') return '查看进度走势'
  if (!nextTask) return '继续今天的训练'
  return `先做 ${moduleLabel(nextTask.module)}`
}
