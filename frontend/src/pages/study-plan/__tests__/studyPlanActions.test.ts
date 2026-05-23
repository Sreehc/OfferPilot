import { describe, expect, it } from 'vitest'
import type { DashboardOverview, StudyPlan, StudyPlanTaskItem } from '@/types/api'
import { getStudyPlanPrimaryActionLabel, getStudyPlanPrimaryActionPath } from '../studyPlanActions'

const moduleLabel = (value: string) => {
  if (value === 'chat') return '问答'
  if (value === 'interview') return '面试'
  return value
}

describe('studyPlanActions', () => {
  it('prefers dashboard nextAction when it exists', () => {
    const overview = {
      learningCount: 0,
      averageScore: 0,
      wrongCount: 0,
      recentInterviews: [],
      weakPoints: [],
      firstVisit: false,
      nextAction: {
        key: 'complete_today_plan',
        title: '完成今天计划',
        description: '先把今天的计划任务做完',
        path: '/study-plan',
        reason: 'today_plan_pending',
        priority: 'P0'
      }
    } satisfies DashboardOverview

    expect(getStudyPlanPrimaryActionPath(overview, null, null)).toBe('/study-plan')
    expect(getStudyPlanPrimaryActionLabel(overview, null, null, moduleLabel)).toBe('完成今天计划')
  })

  it('falls back to plan builder when no plan exists', () => {
    expect(getStudyPlanPrimaryActionPath(null, null, null)).toBe('/study-plan#plan-builder')
    expect(getStudyPlanPrimaryActionLabel(null, null, null, moduleLabel)).toBe('开始生成计划')
  })

  it('uses next task when plan exists without a global nextAction', () => {
    const plan = {
      id: '1',
      title: '一周计划',
      durationDays: 7,
      focusDirection: 'JVM',
      targetRole: 'Java 后端',
      techStack: 'Spring Boot',
      weakPoints: [],
      reviewSuggestion: '',
      status: 'active',
      startDate: '2026-05-20',
      endDate: '2026-05-26',
      currentDay: 2,
      progressRate: 25,
      totalTaskCount: 4,
      completedTaskCount: 1,
      todayTaskCount: 2,
      dailyTargetMinutes: 60,
      tasks: []
    } satisfies StudyPlan
    const task = {
      id: 'task-1',
      dayIndex: 2,
      taskDate: '2026-05-24',
      module: 'chat',
      title: '围绕 JVM 继续追问',
      description: '',
      actionPath: '/chat?sourceQuestionTitle=JVM',
      estimatedMinutes: 20,
      priority: 'high',
      status: 'pending'
    } satisfies StudyPlanTaskItem

    expect(getStudyPlanPrimaryActionPath(null, plan, task)).toBe('/chat?sourceQuestionTitle=JVM')
    expect(getStudyPlanPrimaryActionLabel(null, plan, task, moduleLabel)).toBe('先做 问答')
  })

  it('shows progress label when today focus is completed', () => {
    const plan = {
      id: '1',
      title: '一周计划',
      durationDays: 7,
      focusDirection: 'JVM',
      targetRole: 'Java 后端',
      techStack: 'Spring Boot',
      weakPoints: [],
      reviewSuggestion: '',
      status: 'active',
      startDate: '2026-05-20',
      endDate: '2026-05-26',
      currentDay: 2,
      progressRate: 100,
      totalTaskCount: 4,
      completedTaskCount: 4,
      todayTaskCount: 2,
      dailyTargetMinutes: 60,
      todayFocusSummary: {
        state: 'completed',
        title: '今天完成了',
        reason: '',
        expectedOutcome: ''
      },
      tasks: []
    } satisfies StudyPlan

    expect(getStudyPlanPrimaryActionLabel(null, plan, null, moduleLabel)).toBe('查看进度走势')
  })
})
