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
  if (!currentPlan) return '生成学习计划'
  if (currentPlan.todayFocusSummary?.state === 'completed') return '查看进度走势'
  if (!nextTask) return '查看今日任务'
  return `打开${moduleLabel(nextTask.module)}任务`
}
