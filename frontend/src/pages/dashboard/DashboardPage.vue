<template>
  <div class="dashboard-home">
    <div class="dashboard-home__body">
      <template v-if="loading">
        <section class="dashboard-home__loading-grid">
          <div class="shell-section-card p-6 sm:p-7">
            <div class="dashboard-home__skeleton-line h-5 w-40" />
            <div class="dashboard-home__skeleton-line mt-4 h-11 w-3/4" />
            <div class="dashboard-home__skeleton-line mt-3 h-4 w-full" />
            <div class="dashboard-home__skeleton-line mt-6 h-[220px] w-full rounded-[28px]" />
          </div>
          <div class="shell-section-card min-h-[360px] p-6 sm:p-7">
            <div class="dashboard-home__skeleton-line h-5 w-28" />
            <div class="dashboard-home__skeleton-line mt-6 h-[260px] w-full rounded-[24px]" />
          </div>
        </section>
      </template>

      <template v-else>
        <div v-if="showFirstVisitGuide" class="dashboard-home__first-visit">
          <section class="dashboard-home__welcome">
            <h1 class="dashboard-home__welcome-title">{{ greetingText }} 👋</h1>
            <p class="dashboard-home__welcome-kicker">从这里开始你的求职训练。</p>
          </section>

          <DashboardGuideCard
            :eyebrow="firstVisitGuide.eyebrow"
            :title="firstVisitGuide.title"
            :description="firstVisitGuide.description"
            :action-label="firstVisitGuide.actionLabel"
            :action-to="firstVisitGuide.actionTo"
            :hint="firstVisitGuide.hint"
          />

          <section class="shell-section-card dashboard-first-visit-notes p-5 sm:p-6">
            <div class="dashboard-section-head">
              <div>
                <h3 class="dashboard-section-title">首次流程</h3>
              </div>
            </div>
            <div class="dashboard-first-visit-steps">
              <article
                v-for="step in firstVisitSteps"
                :key="step.key"
                class="dashboard-first-visit-step"
                :class="`dashboard-first-visit-step--${step.state}`"
              >
                <span class="dashboard-first-visit-step__index">{{ step.index }}</span>
                <div class="min-w-0 flex-1">
                  <div class="dashboard-first-visit-step__title">{{ step.title }}</div>
                  <p class="dashboard-first-visit-step__description">{{ step.description }}</p>
                </div>
                <span class="dashboard-first-visit-step__state">{{ step.stateLabel }}</span>
              </article>
            </div>
          </section>
        </div>

        <div v-else class="dashboard-home__body-grid">
          <div class="dashboard-home__content">
            <section class="dashboard-home__welcome">
              <h1 class="dashboard-home__welcome-title">{{ greetingText }} 👋</h1>
              <p class="dashboard-home__welcome-kicker">今天重点完成这一项。</p>
            </section>

            <section class="dashboard-hero">
              <div class="dashboard-hero__content">
                <span class="dashboard-hero__eyebrow">TODAY&apos;S NEXT ACTION</span>
                <h2 class="dashboard-hero__title">{{ dashboardNextActionTitle }}</h2>
                <p class="dashboard-hero__description">{{ dashboardNextActionDescription }}</p>
                <p v-if="dashboardNextActionReason" class="dashboard-hero__reason">{{ dashboardNextActionReason }}</p>

                <div class="dashboard-hero__actions">
                  <RouterLink :to="dashboardNextActionPath" class="dashboard-hero__cta">
                    {{ dashboardNextActionTitle }}
                  </RouterLink>
                  <RouterLink :to="dashboardAgentLink" class="hard-button-secondary">
                    交给 Agent 统筹
                  </RouterLink>
                </div>

                <div class="dashboard-hero__signals">
                  <span class="dashboard-hero__signal">任务等级 {{ dashboardNextActionPriority }}</span>
                  <span class="dashboard-hero__signal">连续 {{ overview.studyStreak ?? 0 }} 天训练</span>
                  <span class="dashboard-hero__signal">待巩固 {{ overview.reviewDebtCount ?? 0 }} 项</span>
                  <span class="dashboard-hero__signal">进行中 {{ overview.applicationSummary?.activeCount ?? 0 }} 条</span>
                </div>
              </div>

              <div class="dashboard-hero__visual" aria-hidden="true">
                <div class="dashboard-hero__visual-badge dashboard-hero__visual-badge--left">
                  <span class="dashboard-hero__visual-badge-dot" />
                  当前主任务
                </div>
                <div class="dashboard-hero__visual-badge dashboard-hero__visual-badge--right">
                  <span class="dashboard-hero__visual-badge-dot" />
                  {{ dashboardNextActionPriority }}
                </div>
                <img :src="heroIllustrationUrl" alt="" class="dashboard-hero__illustration">
              </div>
            </section>

            <section class="shell-section-card dashboard-card-panel dashboard-card-panel--compact dashboard-quick-panel p-4 sm:p-5">
              <div class="dashboard-section-head">
                <div>
                  <h3 class="dashboard-section-title">主线入口</h3>
                </div>
              </div>

              <div class="dashboard-quick-grid">
                <RouterLink
                  v-for="entry in quickEntries"
                  :key="entry.label"
                  :to="entry.path"
                  class="dashboard-quick-card"
                >
                  <span class="dashboard-quick-card__icon" :class="`dashboard-quick-card__icon--${entry.tone}`">
                    <svg
                      v-if="entry.icon === 'interview'"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="1.9"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M7.5 8.25h9m-9 3h5.25m-6.75 7.5 1.462-2.435a1.5 1.5 0 0 1 1.286-.73H18A2.25 2.25 0 0 0 20.25 13.5v-6A2.25 2.25 0 0 0 18 5.25H6A2.25 2.25 0 0 0 3.75 7.5v6A2.25 2.25 0 0 0 6 15.75h.75v3Z"
                      />
                    </svg>
                    <svg
                      v-else-if="entry.icon === 'knowledge'"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="1.9"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M4.5 6.75A2.25 2.25 0 0 1 6.75 4.5h9A2.25 2.25 0 0 1 18 6.75v8.25A2.25 2.25 0 0 1 15.75 17.25h-4.5l-3.75 2.25v-2.25h-.75A2.25 2.25 0 0 1 4.5 15V6.75Z"
                      />
                      <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 8.625h6m-6 3h4.5" />
                    </svg>
                    <svg
                      v-else-if="entry.icon === 'resume'"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="1.9"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M15.75 3.75H7.5A2.25 2.25 0 0 0 5.25 6v12A2.25 2.25 0 0 0 7.5 20.25h9A2.25 2.25 0 0 0 18.75 18V6.75l-3-3Z"
                      />
                      <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 3.75V7.5h3M8.25 11.25h7.5m-7.5 3h5.25" />
                    </svg>
                    <svg
                      v-else-if="entry.icon === 'plan'"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="1.9"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M8.25 3.75v2.25m7.5-2.25v2.25M5.25 8.25h13.5M6.75 5.25h10.5A1.5 1.5 0 0 1 18.75 6.75v10.5a1.5 1.5 0 0 1-1.5 1.5H6.75a1.5 1.5 0 0 1-1.5-1.5V6.75a1.5 1.5 0 0 1 1.5-1.5Z"
                      />
                      <path stroke-linecap="round" stroke-linejoin="round" d="m8.625 12 1.875 1.875L15.375 9" />
                    </svg>
                    <svg
                      v-else-if="entry.icon === 'applications'"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="1.9"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M9 6.75V5.625A1.875 1.875 0 0 1 10.875 3.75h2.25A1.875 1.875 0 0 1 15 5.625V6.75m-9 1.5h12A2.25 2.25 0 0 1 20.25 10.5v6.75A2.25 2.25 0 0 1 18 19.5H6A2.25 2.25 0 0 1 3.75 17.25V10.5A2.25 2.25 0 0 1 6 8.25Z"
                      />
                      <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 12h3" />
                    </svg>
                    <svg
                      v-else
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="1.9"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M8.25 6.75h7.5M8.25 10.5h7.5M8.25 14.25h4.5M6 3.75h12A2.25 2.25 0 0 1 20.25 6v12A2.25 2.25 0 0 1 18 20.25H6A2.25 2.25 0 0 1 3.75 18V6A2.25 2.25 0 0 1 6 3.75Z"
                      />
                    </svg>
                  </span>
                  <div class="min-w-0">
                    <div class="dashboard-quick-card__title">{{ entry.label }}</div>
                    <div class="dashboard-quick-card__description">{{ entry.description }}</div>
                  </div>
                </RouterLink>
              </div>
            </section>

            <section class="shell-section-card dashboard-card-panel dashboard-card-panel--compact dashboard-loop-panel p-4 sm:p-5">
              <div class="dashboard-section-head">
                <div>
                  <h3 class="dashboard-section-title">面试闭环</h3>
                  <p class="dashboard-section-subtitle">让 JD 备面、Prep、实时阶段和录音复盘进入同一条工作流。</p>
                </div>
              </div>

              <div class="dashboard-loop-grid">
                <RouterLink
                  v-for="entry in interviewWorkflowEntries"
                  :key="entry.stage"
                  :to="entry.path"
                  class="dashboard-loop-card"
                >
                  <div class="dashboard-loop-card__head">
                    <span class="dashboard-loop-card__badge" :class="`dashboard-loop-card__badge--${entry.tone}`">
                      {{ entry.stage }}
                    </span>
                    <span class="dashboard-loop-card__status">{{ entry.status }}</span>
                  </div>
                  <div class="dashboard-loop-card__title">{{ entry.label }}</div>
                  <div class="dashboard-loop-card__description">{{ entry.description }}</div>
                  <div class="dashboard-loop-card__hint">{{ entry.hint }}</div>
                </RouterLink>
              </div>
            </section>

            <section class="shell-section-card dashboard-card-panel dashboard-card-panel--compact dashboard-continuation-panel p-4 sm:p-5">
              <div class="dashboard-section-head">
                <div>
                  <h3 class="dashboard-section-title">工作流接续</h3>
                  <p class="dashboard-section-subtitle">待审批、转写处理中或未完成的会话会优先回到这里。</p>
                </div>
                <RouterLink :to="dashboardAgentLink" class="accent-link touch-link text-sm font-semibold">查看 Agent</RouterLink>
              </div>

              <div v-if="workflowContinuations.length" class="dashboard-continuation-grid">
                <RouterLink
                  v-for="entry in workflowContinuationEntries"
                  :key="`${entry.key}-${entry.path}`"
                  :to="entry.path"
                  class="dashboard-loop-card dashboard-continuation-card"
                >
                  <div class="dashboard-loop-card__head">
                    <span class="dashboard-loop-card__badge" :class="`dashboard-loop-card__badge--${entry.tone}`">
                      {{ resolveContinuationStage(entry.key) }}
                    </span>
                    <span class="dashboard-loop-card__status">{{ entry.status }}</span>
                  </div>
                  <div class="dashboard-loop-card__title">{{ entry.label }}</div>
                  <div class="dashboard-loop-card__description">{{ entry.description }}</div>
                </RouterLink>
              </div>
              <div v-else class="dashboard-inline-empty dashboard-inline-empty--compact">
                <div class="dashboard-inline-empty__title">当前没有待接续工作流</div>
                <p class="dashboard-inline-empty__desc">待审批项、实时 Copilot 会话或录音复盘进度，会在这里统一回流。</p>
                <RouterLink :to="dashboardAgentLink" class="hard-button-secondary mt-4 inline-flex">让 Agent 安排下一步</RouterLink>
              </div>
            </section>

            <section class="dashboard-summary-grid">
              <article class="shell-section-card dashboard-card-panel dashboard-card-panel--compact p-4 sm:p-5">
                <div class="dashboard-section-head">
                  <div>
                    <h3 class="dashboard-section-title">最近模拟面试</h3>
                  </div>
                  <RouterLink :to="interviewHistoryLink" class="accent-link touch-link text-sm font-semibold">查看全部</RouterLink>
                </div>

                <template v-if="recentInterviewCard">
                  <div class="dashboard-interview-card">
                    <div class="dashboard-interview-card__top">
                      <div class="dashboard-interview-card__headline">
                        <div class="dashboard-interview-card__headline-main">
                          <div class="dashboard-interview-card__title">{{ recentInterviewTitle }}</div>
                          <span class="dashboard-interview-card__level">{{ recentInterviewLevel }}</span>
                        </div>
                        <div class="dashboard-interview-card__meta">{{ recentInterviewTime }}</div>
                      </div>
                    </div>

                    <div class="dashboard-interview-card__stats">
                      <article
                        v-for="item in recentInterviewStats"
                        :key="item.label"
                        class="dashboard-interview-card__stat"
                      >
                        <strong>{{ item.value }}</strong>
                        <span>{{ item.label }}</span>
                      </article>
                    </div>

                    <div v-if="recentInterviewTags.length" class="dashboard-interview-card__tags">
                      <div class="dashboard-interview-card__tags-title">薄弱点分析</div>
                      <span v-for="tag in recentInterviewTags" :key="tag" class="dashboard-interview-card__tag">
                        {{ tag }}
                      </span>
                    </div>

                    <div class="dashboard-interview-card__footer">
                      <RouterLink
                        :to="recentInterviewDetailLink"
                        class="dashboard-interview-card__detail-link touch-link"
                      >
                        查看详细分析
                        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.9">
                          <path stroke-linecap="round" stroke-linejoin="round" d="m13.5 4.5 6 7.5-6 7.5M19.5 12H4.5" />
                        </svg>
                      </RouterLink>
                    </div>
                  </div>
                </template>

                <div v-else class="dashboard-inline-empty">
                  <div class="dashboard-inline-empty__title">{{ EMPTY_STATE_COPY.dashboardRecentInterview.title }}</div>
                  <p class="dashboard-inline-empty__desc">{{ EMPTY_STATE_COPY.dashboardRecentInterview.description }}</p>
                  <RouterLink :to="mockInterviewEntryLink" class="hard-button-primary mt-4 inline-flex">开始模拟面试</RouterLink>
                </div>
              </article>

              <article class="shell-section-card dashboard-card-panel dashboard-card-panel--compact p-4 sm:p-5">
                <div class="dashboard-section-head">
                  <div>
                    <h3 class="dashboard-section-title">学习进度</h3>
                  </div>
                  <RouterLink to="/study-plan" class="accent-link touch-link text-sm font-semibold">查看全部</RouterLink>
                </div>

                <div class="dashboard-progress-list">
                  <article
                    v-for="item in studyProgressItems"
                    :key="item.label"
                    class="dashboard-progress-item"
                  >
                    <div class="dashboard-progress-item__head">
                      <span>{{ item.label }}</span>
                      <strong>{{ item.value }}%</strong>
                    </div>
                    <div class="dashboard-progress-item__track">
                      <div class="dashboard-progress-item__bar" :style="{ width: `${item.value}%` }" />
                    </div>
                  </article>
                </div>
              </article>
            </section>

            <section class="shell-section-card dashboard-card-panel dashboard-card-panel--compact dashboard-recommend-panel p-4 sm:p-5">
              <div class="dashboard-section-head">
                <div>
                  <h3 class="dashboard-section-title">推荐学习内容</h3>
                </div>
              </div>

              <div class="dashboard-recommend-grid">
                <RouterLink
                  v-for="item in recommendations"
                  :key="item.title"
                  :to="item.path"
                  class="dashboard-recommend-card"
                >
                  <span class="dashboard-recommend-card__label" :class="`dashboard-recommend-card__label--${item.tone}`">
                    {{ item.type }}
                  </span>
                  <div class="dashboard-recommend-card__title">{{ item.title }}</div>
                  <div class="dashboard-recommend-card__meta">{{ item.category }}</div>
                  <div class="dashboard-recommend-card__hint">{{ item.hint }}</div>
                </RouterLink>
              </div>
            </section>
          </div>

          <div class="dashboard-home__rail">
            <section class="shell-section-card dashboard-plan-card p-4 sm:p-5">
              <div class="dashboard-section-head dashboard-section-head--rail">
                <div>
                  <h3 class="dashboard-section-title">今日计划</h3>
                </div>
                <div class="dashboard-plan-card__date">{{ todayFormatted }}</div>
              </div>

              <div class="dashboard-calendar">
                <div class="dashboard-calendar__weekdays">
                  <span v-for="weekday in weekDays" :key="weekday">
                    {{ weekday }}
                  </span>
                </div>
                <div class="dashboard-calendar__grid">
                  <span
                    v-for="cell in calendarCells"
                    :key="cell.key"
                    class="dashboard-calendar__cell"
                    :class="{
                      'dashboard-calendar__cell--muted': !cell.isCurrentMonth,
                      'dashboard-calendar__cell--today': cell.isToday,
                      'dashboard-calendar__cell--task': cell.hasTask
                    }"
                  >
                    {{ cell.label }}
                  </span>
                </div>
              </div>

              <div class="dashboard-plan-card__summary">
                <span>今日任务</span>
                <strong>{{ completedTodayTaskCount }}/{{ planTaskItems.length }}</strong>
              </div>

              <div class="dashboard-plan-card__tasks">
                <article
                  v-for="task in planTaskItems"
                  :key="task.id"
                  class="dashboard-plan-card__task"
                >
                  <span
                    class="dashboard-plan-card__checkbox"
                    :class="{ 'dashboard-plan-card__checkbox--checked': task.status === 'completed' }"
                  />
                  <div class="min-w-0 flex-1">
                    <div class="dashboard-plan-card__task-title">{{ task.title }}</div>
                    <div class="dashboard-plan-card__task-meta">{{ task.estimatedMinutes }} 分钟</div>
                  </div>
                </article>
              </div>

              <RouterLink to="/study-plan" class="accent-link touch-link mt-4 inline-flex text-sm font-semibold">
                查看完整计划
              </RouterLink>
            </section>

            <DashboardApplicationDonut :items="applicationStats" :total="applications.length" :link-to="applicationBoardEntryLink" />
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { fetchApplicationBoardApi } from '@/api/applications'
import { fetchRecommendQuestionsApi } from '@/api/adaptive'
import { fetchDashboardOverviewApi } from '@/api/dashboard'
import { EMPTY_STATE_COPY, ERROR_COPY } from '@/constants/productCopy'
import { interviewDetailApi } from '@/api/interview'
import { fetchCurrentStudyPlanApi } from '@/api/plan'
import DashboardGuideCard from '@/pages/dashboard/DashboardGuideCard.vue'
import type {
  DashboardWorkflowContinuation,
  DashboardOverview,
  InterviewDetail,
  JobApplicationItem,
  RecommendQuestion,
  StudyPlan,
  StudyPlanTaskItem,
  UserInfo
} from '@/types/api'
import { buildAgentWorkbenchLocation } from '@/utils/agent'
import { storage } from '@/utils/storage'
import DashboardApplicationDonut from './DashboardApplicationDonut.vue'
import heroIllustrationUrl from '@/assets/dashboard-hero-illustration.svg'

type DashboardQuickEntry = {
  label: string
  description: string
  path: RouteLocationRaw
  icon: 'interview' | 'knowledge' | 'resume' | 'plan' | 'applications' | 'question'
  tone: 'blue' | 'green' | 'violet' | 'indigo' | 'cyan' | 'sky'
}

type DashboardInterviewWorkflowEntry = {
  stage: string
  label: string
  description: string
  hint: string
  status: string
  path: RouteLocationRaw
  tone: 'blue' | 'teal' | 'violet' | 'amber'
}

type DashboardProgressItem = {
  label: string
  value: number
}

type DashboardContinuationEntry = DashboardWorkflowContinuation

type DashboardRecommendationItem = {
  type: string
  title: string
  category: string
  hint: string
  path: RouteLocationRaw
  tone: 'violet' | 'blue' | 'indigo' | 'green'
}

type ApplicationStatItem = {
  status: string
  label: string
  count: number
  color: string
}

type CalendarCell = {
  key: string
  label: string
  isCurrentMonth: boolean
  isToday: boolean
  hasTask: boolean
}

type FirstVisitGuideState = {
  eyebrow: string
  title: string
  description: string
  actionLabel: string
  actionTo: string
  hint: string
}

type FirstVisitStep = {
  key: 'resume' | 'plan' | 'applications' | 'workbench'
  index: string
  title: string
  description: string
  state: 'done' | 'active' | 'pending'
  stateLabel: string
}

const loading = ref(true)
const overview = ref<DashboardOverview>({
  learningCount: 0,
  averageScore: 0,
  wrongCount: 0,
  recentInterviews: [],
  weakPoints: [],
  firstVisit: true,
  reviewDebtCount: 0,
  studyStreak: 0
})
const currentPlan = ref<StudyPlan | null>(null)
const applications = ref<JobApplicationItem[]>([])
const recentInterviewDetail = ref<InterviewDetail | null>(null)
const recommendedQuestions = ref<RecommendQuestion[]>([])
const currentUser = ref<UserInfo | null>(storage.getUser())

const weekDays = ['一', '二', '三', '四', '五', '六', '日']

const dashboardSeedTopic = computed(() => overview.value.suggestedFocus || overview.value.weakPoints[0]?.categoryName || '')

const buildSeedQuery = (workflow?: string, note?: string) => ({
  seedTopic: dashboardSeedTopic.value || undefined,
  seedWorkflow: workflow || undefined,
  seedNote: note || undefined
})

const buildSeededPath = (path: string, workflow?: string, note?: string): RouteLocationRaw => ({
  path,
  query: buildSeedQuery(workflow, note)
})

const buildSeededAgentWorkbenchLocation = (
  prefill: Parameters<typeof buildAgentWorkbenchLocation>[0],
  workflow = 'dashboard',
  note?: string
): RouteLocationRaw => {
  const location = buildAgentWorkbenchLocation(prefill) as {
    path: string
    query?: Record<string, string>
  }
  return {
    path: location.path,
    query: {
      ...(location.query || {}),
      ...buildSeedQuery(workflow, note)
    }
  }
}

const buildInterviewWorkspaceLink = (
  workspace: 'mock-interview' | 'job-prep' | 'copilot-prep' | 'copilot-live' | 'recording-review',
  note?: string
): RouteLocationRaw => ({
  path: '/interview',
  query: {
    workspace,
    ...buildSeedQuery(workspace, note)
  }
})

const appendDashboardSeedToPath = (path?: string | null): string => {
  if (!path) return '/dashboard'
  if (!dashboardSeedTopic.value) return path
  if (!path.startsWith('/interview') && !path.startsWith('/resume') && !path.startsWith('/applications')) {
    return path
  }
  const hashSplit = path.split('#')
  const pathWithoutHash = hashSplit[0] || path
  const hash = hashSplit[1] || ''
  const pathQuerySplit = pathWithoutHash.split('?')
  const pathname = pathQuerySplit[0] || pathWithoutHash
  const queryString = pathQuerySplit[1] || ''
  const query = new URLSearchParams(queryString)
  if (!query.get('seedTopic')) {
    query.set('seedTopic', dashboardSeedTopic.value)
  }
  if (pathname === '/interview' && !query.get('seedWorkflow') && query.get('workspace')) {
    query.set('seedWorkflow', query.get('workspace') || 'interview')
  }
  const nextPath = query.toString() ? `${pathname}?${query.toString()}` : pathname
  return hash ? `${nextPath}#${hash}` : nextPath
}

const mockInterviewEntryLink = computed<RouteLocationRaw>(() =>
  buildInterviewWorkspaceLink(
    'mock-interview',
    dashboardSeedTopic.value ? `当前从工作台进入，优先验证「${dashboardSeedTopic.value}」是否已经讲稳。` : undefined
  )
)
const interviewHistoryLink = computed<string>(() => appendDashboardSeedToPath('/interview?workspace=history'))
const applicationBoardEntryLink = computed<RouteLocationRaw>(() =>
  buildSeededPath(
    '/applications',
    'applications',
    dashboardSeedTopic.value ? `当前从工作台进入，优先把「${dashboardSeedTopic.value}」带到真实岗位推进里验证。` : undefined
  )
)

const quickEntries = computed<DashboardQuickEntry[]>(() => [
  { label: '模拟面试', description: 'AI 全真模拟', path: mockInterviewEntryLink.value, icon: 'interview', tone: 'blue' },
  { label: '知识库', description: '查看推荐资料和个人文档', path: '/knowledge', icon: 'knowledge', tone: 'green' },
  {
    label: '简历助手',
    description: '整理简历与面试提纲',
    path: buildSeededPath(
      '/resume',
      'resume',
      dashboardSeedTopic.value ? `当前从工作台进入，优先把「${dashboardSeedTopic.value}」补进简历表达和面试口径。` : undefined
    ),
    icon: 'resume',
    tone: 'violet'
  },
  { label: '学习计划', description: '定制学习路径', path: '/study-plan', icon: 'plan', tone: 'cyan' },
  {
    label: '投递管理',
    description: '追踪求职进度',
    path: applicationBoardEntryLink.value,
    icon: 'applications',
    tone: 'indigo'
  },
  { label: '题库训练', description: '筛题、看答案和练表达', path: '/question', icon: 'question', tone: 'sky' }
])

const greetingText = computed(() => resolveGreetingLabel())
const dashboardNextAction = computed(() => overview.value.nextAction)
const guideSeen = computed(() => {
  if (!currentUser.value?.id) return false
  return storage.getGuideSeen(currentUser.value.id)
})
const hasResume = computed(() => {
  const actionKey = dashboardNextAction.value?.key
  if (!actionKey) return false
  return actionKey !== 'upload_resume'
})
const hasPlan = computed(() => Boolean(currentPlan.value))
const hasApplications = computed(() => applications.value.length > 0)
const showFirstVisitGuide = computed(() => {
  if (!overview.value.firstVisit) return false
  if (!guideSeen.value) return true
  return !hasResume.value || !hasPlan.value || !hasApplications.value
})

const firstVisitGuide = computed<FirstVisitGuideState>(() => {
  if (!hasResume.value) {
    return {
      eyebrow: '首次使用',
      title: '上传简历',
      description: '上传简历后，计划、投递和模拟面试会更贴近你的目标岗位。',
      actionLabel: '去上传简历',
      actionTo: appendDashboardSeedToPath('/resume#resume-upload'),
      hint: '上传完成后，可以完善计划和岗位信息。'
    }
  }
  if (!hasPlan.value) {
    return {
      eyebrow: '首次使用',
      title: '生成学习计划',
      description: '生成一份学习计划，明确接下来几天的训练安排。',
      actionLabel: '去生成计划',
      actionTo: '/study-plan#plan-builder',
      hint: '计划生成后，可以开始准备岗位信息。'
    }
  }
  return {
    eyebrow: '首次使用',
    title: '记录目标岗位',
    description: '添加一个目标岗位，后续建议会更贴近你的求职方向。',
    actionLabel: '去记录岗位',
    actionTo: appendDashboardSeedToPath('/applications#application-create'),
    hint: '岗位记录完成后，就可以进入标准工作台。'
  }
})

const firstVisitSteps = computed<FirstVisitStep[]>(() => [
  {
    key: 'resume',
    index: '01',
    title: '上传简历',
    description: '让后续训练和求职动作有基础资料。',
    state: hasResume.value ? 'done' : 'active',
    stateLabel: hasResume.value ? '已完成' : '进行中'
  },
  {
    key: 'plan',
    index: '02',
    title: '生成计划',
    description: '把题库、问答和模拟面试安排到同一条训练主线上。',
    state: hasPlan.value ? 'done' : hasResume.value ? 'active' : 'pending',
    stateLabel: hasPlan.value ? '已完成' : hasResume.value ? '待完成' : '待开始'
  },
  {
    key: 'applications',
    index: '03',
    title: '记录岗位',
    description: '补充一个目标岗位，让后续建议更贴近你的求职方向。',
    state: hasApplications.value ? 'done' : hasPlan.value ? 'active' : 'pending',
    stateLabel: hasApplications.value ? '已完成' : hasPlan.value ? '待完成' : '待开始'
  },
  {
    key: 'workbench',
    index: '04',
    title: '进入工作台',
    description: '完成前面三步后，这里会显示日常训练工作台。',
    state: showFirstVisitGuide.value ? 'pending' : 'done',
    stateLabel: showFirstVisitGuide.value ? '未开启' : '已进入'
  }
])

const dashboardNextActionTitle = computed(() => dashboardNextAction.value?.title || '查看今日训练')
const dashboardNextActionDescription = computed(
  () => dashboardNextAction.value?.description || '完成当前任务后，处理其他模块。'
)
const dashboardNextActionReason = computed(() => dashboardNextAction.value?.reason || '')
const dashboardNextActionPath = computed<string>(() => appendDashboardSeedToPath(dashboardNextAction.value?.path || '/dashboard'))
const dashboardNextActionPriority = computed(() => dashboardNextAction.value?.priority || 'P1')
const dashboardAgentLink = computed(() => {
  const contextRefs = ['dashboard:overview', 'analytics:profile', 'study-plan:active']
  if (overview.value.recentInterviews.length) {
    contextRefs.push('interview:latest')
  }
  if (applications.value.length) {
    contextRefs.push('application:board')
  }
  return buildSeededAgentWorkbenchLocation({
    agentType: 'coordinator',
    triggerSource: 'dashboard',
    contextRefs,
    userPrompt: dashboardNextAction.value?.title
      ? `围绕“${dashboardNextAction.value.title}”统筹今天的训练与求职动作。`
      : '结合当前工作台状态，整理今天最值得推进的训练与求职动作。'
  }, 'dashboard', dashboardSeedTopic.value ? `当前从工作台发起，优先围绕「${dashboardSeedTopic.value}」统筹下一步动作。` : undefined)
})
const workflowContinuationEntries = computed<DashboardContinuationEntry[]>(() =>
  workflowContinuations.value.map((entry) => ({
    ...entry,
    path: appendDashboardSeedToPath(entry.path)
  }))
)
const workflowContinuations = computed<DashboardContinuationEntry[]>(() => overview.value.workflowContinuations || [])
const interviewWorkflowEntries = computed<DashboardInterviewWorkflowEntry[]>(() => {
  const hasRecentInterview = overview.value.recentInterviews.length > 0
  const hasWeakSignals = overview.value.weakPoints.length > 0 || (overview.value.reviewDebtCount ?? 0) > 0
  const hasApplicationContext = applications.value.length > 0
  const focusTopic = overview.value.weakPoints[0]?.categoryName || overview.value.suggestedFocus || '当前岗位方向'

  return [
    {
      stage: 'JD 备面',
      label: '定向拆解岗位要求',
      description: '结合岗位信息、简历和投递记录，先补一轮针对性的备面草案。',
      hint: hasApplicationContext ? '当前已有岗位上下文，可直接进入定向准备。' : '还没有投递记录时，也可以先手动填入 JD 做预演。',
      status: hasApplicationContext ? '有岗位上下文' : '可手动开始',
      path: buildInterviewWorkspaceLink(
        'job-prep',
        dashboardSeedTopic.value ? `当前从工作台进入，优先把「${focusTopic}」压到岗位语境下验证。` : undefined
      ),
      tone: 'blue'
    },
    {
      stage: 'Copilot Prep',
      label: '整理实时阶段提纲',
      description: '把开场提纲、风险点和追问建议整理成实时阶段可直接消费的准备结果。',
      hint: hasApplicationContext ? '建议先基于目标岗位准备开场和追问。' : '即使没有投递记录，也可以先用当前简历预演。',
      status: hasApplicationContext ? '建议接续 JD 备面' : '可先独立准备',
      path: buildInterviewWorkspaceLink(
        'copilot-prep',
        dashboardSeedTopic.value ? `当前从工作台进入，优先暴露「${focusTopic}」在实时追问中的风险。` : undefined
      ),
      tone: 'teal'
    },
    {
      stage: 'Realtime',
      label: '进入实时 Copilot',
      description: '承接 Prep 结果，连接实时阶段并记录事件时间线与面后复盘入口。',
      hint: hasRecentInterview ? '最近已有面试记录，适合把实时阶段纳入正式工作流。' : '建议先完成 Prep，再进入实时阶段。',
      status: hasRecentInterview ? '已有训练上下文' : '先做 Prep',
      path: buildInterviewWorkspaceLink(
        'copilot-live',
        dashboardSeedTopic.value ? `当前从工作台进入，重点观察「${focusTopic}」在实时追问里是否已经站稳。` : undefined
      ),
      tone: 'violet'
    },
    {
      stage: '录音复盘',
      label: '上传真实录音做异步复盘',
      description: '把真实音频转写、评分并回写成下一轮训练动作和画像证据。',
      hint: hasWeakSignals ? `当前弱项集中在 ${focusTopic}，适合用真实录音补证据。` : '当你需要复盘真实表达状态时，这里会承接整条异步链路。',
      status: hasWeakSignals ? '建议补证据' : '随时可用',
      path: buildInterviewWorkspaceLink(
        'recording-review',
        dashboardSeedTopic.value ? `当前从工作台进入，优先回听「${focusTopic}」相关表达片段。` : undefined
      ),
      tone: 'amber'
    }
  ]
})

const resolveContinuationStage = (key: string) => {
  switch (key) {
    case 'agent_approval':
      return '审批'
    case 'copilot_realtime':
      return 'Realtime'
    case 'recording_review':
      return '录音复盘'
    case 'copilot_prep':
      return 'Prep'
    case 'job_prep':
      return 'JD 备面'
    default:
      return '接续'
  }
}
const todayFormatted = computed(() =>
  new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  }).format(new Date())
)

const recentInterviewCard = computed(() => overview.value.recentInterviews[0] ?? null)
const recentInterviewTitle = computed(() => {
  if (recentInterviewDetail.value?.jobRole) return recentInterviewDetail.value.jobRole
  if (recentInterviewDetail.value?.direction) return `${recentInterviewDetail.value.direction} 模拟面试`
  if (recentInterviewCard.value?.direction) return `${recentInterviewCard.value.direction} 模拟面试`
  return '最近模拟面试'
})

const recentInterviewLevel = computed(() => {
  const level = recentInterviewDetail.value?.experienceLevel || ''
  if (level.includes('初')) return '初级'
  if (level.includes('中')) return '中等'
  if (level.includes('高') || level.includes('资深')) return '高级'
  return '中等'
})

const recentInterviewTime = computed(() => {
  if (recentInterviewCard.value?.finishedAt) {
    return formatDateTime(recentInterviewCard.value.finishedAt)
  }
  if (recentInterviewDetail.value?.endTime) {
    return formatDateTime(recentInterviewDetail.value.endTime)
  }
  return '最近完成'
})

const recentInterviewStats = computed(() => {
  if (!recentInterviewCard.value) return []
  const questionCount = recentInterviewDetail.value?.questionCount ?? 0
  const followUpCount = recentInterviewDetail.value?.records.filter((record) => Boolean(record.followUp?.trim())).length ?? 0
  const duration = deriveInterviewDuration(recentInterviewDetail.value)
  return [
    {
      label: '综合评分',
      value: `${formatScore(Number(recentInterviewCard.value.totalScore))}/100`
    },
    {
      label: '答题题数',
      value: questionCount ? `${questionCount} 题` : '待生成'
    },
    {
      label: '追问提示',
      value: `${followUpCount} 条`
    },
    {
      label: '面试时长',
      value: duration
    }
  ]
})

const recentInterviewTags = computed(() => {
  const tags = new Set<string>()
  recentInterviewDetail.value?.records.forEach((record) => {
    record.weakPointTags?.forEach((tag) => tags.add(tag))
  })
  if (tags.size === 0) {
    overview.value.weakPoints.slice(0, 3).forEach((item) => tags.add(item.categoryName))
  }
  return Array.from(tags).slice(0, 3)
})

const recentInterviewDetailLink = computed<string>(() => {
  if (!recentInterviewCard.value?.sessionId) return interviewHistoryLink.value
  return appendDashboardSeedToPath(`/interview/detail/${recentInterviewCard.value.sessionId}`)
})

const studyProgressItems = computed<DashboardProgressItem[]>(() => {
  const preferredOrder = ['Java基础', 'JVM', '多线程并发', 'Spring框架', 'MySQL', 'Redis']
  const progressMap = new Map<string, number>()

  overview.value.categoryAbilities?.forEach((item) => {
    progressMap.set(normalizeTopicName(item.categoryName), normalizePercent(item.abilityScore))
  })

  overview.value.weakPoints.forEach((item) => {
    const normalized = normalizeTopicName(item.categoryName)
    if (!progressMap.has(normalized)) {
      progressMap.set(normalized, normalizePercent(Number(item.score)))
    }
  })

  const ordered = preferredOrder
    .map((label) => ({
      label,
      value: clampProgress(progressMap.get(label) ?? 0)
    }))
    .filter((item) => item.value > 0)

  if (ordered.length >= 4) return ordered.slice(0, 6)

  const fallback = preferredOrder
    .filter((label) => !ordered.some((item) => item.label === label))
    .slice(0, Math.max(0, 5 - ordered.length))
    .map((label) => ({
      label,
      value: 0
    }))

  return [...ordered, ...fallback].slice(0, 6)
})

const todayTasks = computed<StudyPlanTaskItem[]>(() => {
  if (!currentPlan.value) return []
  return currentPlan.value.tasks.filter((task) => task.dayIndex === currentPlan.value?.currentDay)
})

const fallbackTasks = computed<StudyPlanTaskItem[]>(() => {
  const topWeak = overview.value.weakPoints[0]?.categoryName || overview.value.suggestedFocus || 'Java基础'
  const hasApplications = applications.value.length > 0
  return [
    {
      id: 'fallback-question',
      dayIndex: 0,
      taskDate: toDateKey(new Date()),
      module: 'question',
      title: `刷一组 ${topWeak} 面试题`,
      description: '',
      actionPath: '/question',
      estimatedMinutes: 20,
      priority: 'high',
      status: 'pending'
    },
    {
      id: 'fallback-knowledge',
      dayIndex: 0,
      taskDate: toDateKey(new Date()),
      module: 'knowledge',
      title: `复盘 ${topWeak} 知识点`,
      description: '',
      actionPath: '/knowledge',
      estimatedMinutes: 15,
      priority: 'medium',
      status: 'pending'
    },
    {
      id: 'fallback-interview',
      dayIndex: 0,
      taskDate: toDateKey(new Date()),
      module: 'interview',
      title: '完成一次模拟面试训练',
      description: '',
      actionPath: appendDashboardSeedToPath('/interview?workspace=mock-interview'),
      estimatedMinutes: 30,
      priority: 'high',
      status: overview.value.recentInterviews.length > 0 ? 'completed' : 'pending'
    },
    {
      id: 'fallback-application',
      dayIndex: 0,
      taskDate: toDateKey(new Date()),
      module: 'applications',
      title: hasApplications ? '跟进一条投递进展' : '建立第一条投递记录',
      description: '',
      actionPath: appendDashboardSeedToPath('/applications'),
      estimatedMinutes: 10,
      priority: 'low',
      status: hasApplications ? 'pending' : 'pending'
    }
  ]
})

const planTaskItems = computed<StudyPlanTaskItem[]>(() => {
  const source = todayTasks.value.length > 0 ? todayTasks.value : fallbackTasks.value
  return source.slice(0, 3)
})

const completedTodayTaskCount = computed(() => planTaskItems.value.filter((task) => task.status === 'completed').length)

const applicationStats = computed<ApplicationStatItem[]>(() => {
  const counts = {
    applied: 0,
    written: 0,
    interview: 0,
    offer: 0,
    rejected: 0
  }

  applications.value.forEach((item) => {
    const status = (item.status || '').toLowerCase()
    if (status === 'saved' || status === 'applied') counts.applied += 1
    if (status === 'written') counts.written += 1
    if (status === 'interview') counts.interview += 1
    if (status === 'offer') counts.offer += 1
    if (status === 'rejected') counts.rejected += 1
  })

  return [
    { status: 'applied', label: '已投递', count: counts.applied, color: '#4870ff' },
    { status: 'written', label: '笔试', count: counts.written, color: '#4fa3ff' },
    { status: 'interview', label: '面试中', count: counts.interview, color: '#6c88ff' },
    { status: 'offer', label: 'Offer', count: counts.offer, color: '#45b97c' },
    { status: 'rejected', label: '已拒绝', count: counts.rejected, color: '#8ea2c8' }
  ]
})

const recommendations = computed<DashboardRecommendationItem[]>(() => {
  if (recommendedQuestions.value.length > 0) {
    return recommendedQuestions.value.slice(0, 4).map((item, index) => {
      const query = new URLSearchParams({
        focus: 'recommended',
        keyword: item.title,
        difficulty: item.difficulty || '',
        categoryId: item.categoryId ? String(item.categoryId) : ''
      })
      return {
        type: '推荐题目',
        title: item.title,
        category: [item.categoryName, difficultyLabel(item.difficulty)].filter(Boolean).join(' · ') || '面试题',
        hint: item.reason || '适合作为今天的热身题目',
        path: `/question?${query.toString()}`,
        tone: recommendationTone(index)
      }
    })
  }

  const topWeak = overview.value.weakPoints[0]?.categoryName || overview.value.weakCategories?.[0] || 'Java基础'

  return [
    {
      type: '面试题',
      title: topWeak.includes('JVM') ? '深入理解 JVM 内存模型' : 'Java 中 == 和 equals 的区别',
      category: topWeak.includes('JVM') ? 'JVM' : 'Java基础',
      hint: '适合作为今天的热身题目',
      path: '/question',
      tone: 'violet'
    },
    {
      type: '知识点',
      title: 'Spring Boot 自动配置原理',
      category: 'Spring',
      hint: '适合配合面试题复习',
      path: '/knowledge',
      tone: 'blue'
    },
    {
      type: '训练建议',
      title: 'Redis 缓存穿透的解决方案',
      category: 'Redis',
      hint: '高频场景题',
      path: '/question',
      tone: 'indigo'
    },
    {
      type: '专题阅读',
      title: '多线程并发面试答题框架',
      category: '并发',
      hint: '适合补弱项',
      path: '/knowledge',
      tone: 'green'
    }
  ]
})

const calendarCells = computed<CalendarCell[]>(() => {
  const today = new Date()
  const currentYear = today.getFullYear()
  const currentMonth = today.getMonth()
  const firstOfMonth = new Date(currentYear, currentMonth, 1)
  const mondayBasedStart = (firstOfMonth.getDay() + 6) % 7
  const taskDateSet = new Set(planTaskItems.value.map((task) => task.taskDate))
  const cells: CalendarCell[] = []

  for (let index = 0; index < 35; index += 1) {
    const cellDate = new Date(currentYear, currentMonth, index - mondayBasedStart + 1)
    const isCurrentMonth = cellDate.getMonth() === currentMonth
    const isoDate = toDateKey(cellDate)
    cells.push({
      key: `${currentYear}-${currentMonth + 1}-${index}`,
      label: String(cellDate.getDate()),
      isCurrentMonth,
      isToday:
        cellDate.getFullYear() === today.getFullYear() &&
        cellDate.getMonth() === today.getMonth() &&
        cellDate.getDate() === today.getDate(),
      hasTask: taskDateSet.has(isoDate)
    })
  }

  return cells
})

const loadDashboard = async () => {
  loading.value = true

  const [overviewResult, planResult, applicationsResult, recommendResult] = await Promise.allSettled([
    fetchDashboardOverviewApi(),
    fetchCurrentStudyPlanApi(),
    fetchApplicationBoardApi(),
    fetchRecommendQuestionsApi(4)
  ])

  let overviewLoaded = false

  if (overviewResult.status === 'fulfilled') {
    overview.value = overviewResult.value.data
    overviewLoaded = true
  }

  if (planResult.status === 'fulfilled') {
    currentPlan.value = planResult.value.data
  } else {
    currentPlan.value = null
  }

  if (applicationsResult.status === 'fulfilled') {
    applications.value = applicationsResult.value.data ?? []
  } else {
    applications.value = []
  }

  if (recommendResult.status === 'fulfilled') {
    recommendedQuestions.value = recommendResult.value.data ?? []
  } else {
    recommendedQuestions.value = []
  }

  if (overviewLoaded && overview.value.recentInterviews[0]?.sessionId) {
    try {
      const { data } = await interviewDetailApi(overview.value.recentInterviews[0].sessionId)
      recentInterviewDetail.value = data
    } catch {
      recentInterviewDetail.value = null
    }
  } else {
    recentInterviewDetail.value = null
  }

  if (!overviewLoaded) {
    ElMessage.error(ERROR_COPY.dashboardCoreLoadFailed)
  }

  loading.value = false
}

function resolveGreetingLabel(): string {
  const hour = new Date().getHours()
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
}

function normalizeTopicName(name?: string): string {
  const text = (name || '').trim()
  const lower = text.toLowerCase()
  if (!text) return 'Java基础'
  if (lower.includes('java')) return 'Java基础'
  if (lower.includes('jvm')) return 'JVM'
  if (text.includes('线程') || text.includes('并发')) return '多线程并发'
  if (lower.includes('spring')) return 'Spring框架'
  if (lower.includes('mysql')) return 'MySQL'
  if (lower.includes('redis')) return 'Redis'
  return text
}

function normalizePercent(value: number): number {
  if (!Number.isFinite(value)) return 0
  if (value > 1) return clampProgress(Math.round(value))
  return clampProgress(Math.round(value * 100))
}

function clampProgress(value: number): number {
  if (!Number.isFinite(value)) return 0
  return Math.max(0, Math.min(100, Math.round(value)))
}

function formatDateTime(value?: string): string {
  if (!value) return '最近完成'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function formatScore(value: number): string {
  if (!Number.isFinite(value)) return '0'
  return Number.isInteger(value) ? String(value) : value.toFixed(0)
}

function difficultyLabel(value?: string): string {
  if (value === 'easy') return '简单'
  if (value === 'hard') return '困难'
  if (value === 'medium') return '中等'
  return ''
}

function recommendationTone(index: number): DashboardRecommendationItem['tone'] {
  return ['violet', 'blue', 'indigo', 'green'][index % 4] as DashboardRecommendationItem['tone']
}

function deriveInterviewDuration(detail: InterviewDetail | null): string {
  if (detail?.durationMinutes) return `${detail.durationMinutes} 分钟`
  if (detail?.startTime && detail?.endTime) {
    const durationMs = new Date(detail.endTime).getTime() - new Date(detail.startTime).getTime()
    const durationMinutes = Math.max(1, Math.round(durationMs / 60000))
    return `${durationMinutes} 分钟`
  }
  return '待计算'
}

function toDateKey(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

onMounted(() => {
  void loadDashboard()
})
</script>

<style scoped>
.dashboard-home {
  --dashboard-card-radius: 10px;
  --dashboard-inner-radius: 8px;
  --dashboard-pill-radius: 999px;
  --dashboard-layout-gap: 8px;
  --dashboard-summary-card-height: 320px;
  --dashboard-summary-side-height: 336px;
  --dashboard-border: rgba(18, 41, 76, 0.06);
  --dashboard-border-strong: rgba(18, 41, 76, 0.08);
  --dashboard-panel-bg: rgba(255, 255, 255, 0.96);
  --dashboard-soft-bg: #f8fbff;
  --dashboard-link: #466cff;
  --dashboard-link-hover: #365ce8;
  min-height: 0;
  background: transparent;
  padding: 0;
}

.dashboard-home__body {
  display: flex;
  flex-direction: column;
  gap: var(--dashboard-layout-gap);
}

.dashboard-home__first-visit {
  display: grid;
  gap: var(--dashboard-layout-gap);
}

.dashboard-first-visit-notes {
  border-radius: var(--dashboard-card-radius);
  border: 1px solid var(--dashboard-border);
  background: var(--dashboard-panel-bg);
  box-shadow: 0 7px 18px rgba(30, 48, 90, 0.03);
}

.dashboard-first-visit-steps {
  display: grid;
  gap: 0.65rem;
}

.dashboard-first-visit-step {
  display: flex;
  align-items: flex-start;
  gap: 0.9rem;
  border-radius: var(--dashboard-card-radius);
  border: 1px solid var(--dashboard-border-strong);
  background: var(--dashboard-soft-bg);
  padding: 0.95rem 1rem;
}

.dashboard-first-visit-step--active {
  border-color: rgba(61, 105, 255, 0.2);
  background: linear-gradient(180deg, rgba(61, 105, 255, 0.08), rgba(61, 105, 255, 0.03));
}

.dashboard-first-visit-step--done {
  border-color: rgba(69, 185, 124, 0.18);
  background: linear-gradient(180deg, rgba(69, 185, 124, 0.08), rgba(69, 185, 124, 0.03));
}

.dashboard-first-visit-step__index {
  display: inline-flex;
  min-width: 2.2rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(61, 105, 255, 0.1);
  color: #3d69ff;
  font-size: 0.74rem;
  font-weight: 700;
  line-height: 2.2rem;
}

.dashboard-first-visit-step__title {
  color: #182d4a;
  font-size: 0.94rem;
  font-weight: 700;
}

.dashboard-first-visit-step__description {
  margin-top: 0.25rem;
  color: #7f8da4;
  font-size: 0.82rem;
  line-height: 1.65;
}

.dashboard-first-visit-step__state {
  flex-shrink: 0;
  border-radius: 999px;
  background: rgba(61, 105, 255, 0.08);
  color: #4a6eff;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.35rem 0.7rem;
}

.dashboard-home__body-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 430px;
  gap: var(--dashboard-layout-gap);
  align-items: start;
}

.dashboard-home__content,
.dashboard-home__rail {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: var(--dashboard-layout-gap);
}

.dashboard-home__welcome {
  padding: 0 0.1rem;
}

.dashboard-home__welcome-kicker {
  color: #8c97aa;
  font-size: 0.82rem;
  font-weight: 600;
  margin-top: 0.22rem;
}

.dashboard-home__welcome-title {
  margin: 0;
  color: #152b4b;
  font-size: 1.46rem;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.12;
  text-wrap: balance;
}

.dashboard-card-panel {
  border-radius: var(--dashboard-card-radius);
  border: 1px solid var(--dashboard-border);
  background: var(--dashboard-panel-bg);
  box-shadow: 0 7px 18px rgba(30, 48, 90, 0.03);
}

.dashboard-card-panel--compact {
  box-shadow: 0 6px 14px rgba(30, 48, 90, 0.024);
}

.dashboard-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(280px, 0.9fr);
  gap: 1rem;
  overflow: hidden;
  border-radius: var(--dashboard-card-radius);
  background:
    radial-gradient(circle at 14% 18%, rgba(255, 255, 255, 0.18), transparent 22%),
    linear-gradient(135deg, #3f67ff 0%, #4d79ff 42%, #4a90b8 100%);
  padding: 1.2rem 1.35rem;
  box-shadow: 0 12px 26px rgba(63, 103, 255, 0.14);
}

.dashboard-hero__content,
.dashboard-hero__visual {
  position: relative;
  z-index: 1;
}

.dashboard-hero__content {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.dashboard-hero__eyebrow {
  color: rgba(255, 255, 255, 0.72);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.2em;
}

.dashboard-hero__title {
  margin-top: 0.58rem;
  color: #fff;
  font-size: clamp(1.58rem, 2.3vw, 2.12rem);
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.04;
  text-wrap: balance;
}

.dashboard-hero__description {
  margin-top: 0.5rem;
  max-width: 32rem;
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.86rem;
  line-height: 1.55;
  text-wrap: pretty;
}

.dashboard-hero__reason {
  margin-top: 0.5rem;
  max-width: 32rem;
  color: rgba(255, 255, 255, 0.72);
  font-size: 0.74rem;
  line-height: 1.5;
  text-wrap: pretty;
}

.dashboard-hero__actions {
  margin-top: 0.8rem;
}

.dashboard-hero__cta {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  border-radius: var(--dashboard-pill-radius);
  background: #fff;
  color: #3864f7;
  font-size: 0.9rem;
  font-weight: 700;
  padding: 0 1.2rem;
}

.dashboard-hero__signals {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  margin-top: 0.7rem;
}

.dashboard-hero__signal {
  border-radius: var(--dashboard-pill-radius);
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.92);
  font-size: 0.74rem;
  font-weight: 600;
  padding: 0.36rem 0.64rem;
  font-variant-numeric: tabular-nums;
}

.dashboard-hero__visual {
  min-height: 164px;
}

.dashboard-hero__visual-badge {
  position: absolute;
  display: inline-flex;
  align-items: center;
  gap: 0.42rem;
  border-radius: var(--dashboard-pill-radius);
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.4rem 0.7rem;
  backdrop-filter: blur(10px);
}

.dashboard-hero__visual-badge--left {
  left: 0.8rem;
  top: 0.8rem;
}

.dashboard-hero__visual-badge--right {
  right: 1rem;
  top: 1.2rem;
}

.dashboard-hero__visual-badge-dot {
  width: 0.45rem;
  height: 0.45rem;
  border-radius: var(--dashboard-pill-radius);
  background: #fff;
}

.dashboard-hero__illustration {
  position: absolute;
  right: -0.15rem;
  bottom: 0;
  width: min(100%, 392px);
  max-width: 100%;
  object-fit: contain;
  filter: drop-shadow(0 10px 24px rgba(28, 51, 153, 0.12));
}

.dashboard-home :deep(.accent-link) {
  color: var(--dashboard-link);
}

.dashboard-home :deep(.accent-link:hover) {
  color: var(--dashboard-link-hover);
}

.dashboard-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
  margin-bottom: 0.6rem;
}

.dashboard-section-head--rail {
  margin-bottom: 0.55rem;
}

.dashboard-section-title {
  color: #162b4a;
  font-size: 0.96rem;
  font-weight: 800;
  letter-spacing: 0;
  text-wrap: balance;
}

.dashboard-section-subtitle {
  margin-top: 0.22rem;
  color: #7f8da4;
  font-size: 0.8rem;
  line-height: 1.55;
}

.dashboard-quick-grid,
.dashboard-loop-grid,
.dashboard-continuation-grid,
.dashboard-summary-grid,
.dashboard-recommend-grid {
  display: grid;
  gap: var(--dashboard-layout-gap);
}

.dashboard-quick-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.dashboard-summary-grid {
  grid-template-columns: minmax(0, 1fr) minmax(0, 0.95fr);
  align-items: stretch;
}

.dashboard-loop-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.dashboard-continuation-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.dashboard-summary-grid > .dashboard-card-panel {
  display: flex;
  min-height: var(--dashboard-summary-card-height);
  flex-direction: column;
}

.dashboard-recommend-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.dashboard-recommend-panel {
  margin-top: 0;
}

.dashboard-quick-card,
.dashboard-loop-card,
.dashboard-recommend-card {
  display: flex;
  min-width: 0;
  gap: 0.72rem;
  border: 1px solid var(--dashboard-border-strong);
  border-radius: var(--dashboard-card-radius);
  background: var(--dashboard-soft-bg);
  padding: 0.66rem 0.72rem;
  transition:
    transform var(--motion-fast) var(--ease-hard),
    box-shadow var(--motion-fast) var(--ease-hard);
}

.dashboard-quick-card {
  min-height: 118px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 0.58rem;
}

.dashboard-quick-card:hover,
.dashboard-loop-card:hover,
.dashboard-recommend-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(35, 58, 110, 0.045);
}

.dashboard-quick-card__icon {
  display: inline-flex;
  width: 2.7rem;
  height: 2.7rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: var(--dashboard-inner-radius);
  color: #fff;
  font-size: 0.95rem;
  font-weight: 800;
  box-shadow: 0 8px 16px rgba(64, 98, 188, 0.14);
}

.dashboard-quick-card__icon svg {
  width: 1.2rem;
  height: 1.2rem;
}

.dashboard-quick-card__icon--blue {
  background: linear-gradient(180deg, #4f86ff, #3568ff);
}

.dashboard-quick-card__icon--green {
  background: linear-gradient(180deg, #77d36a, #48b362);
}

.dashboard-quick-card__icon--violet {
  background: linear-gradient(180deg, #8d73ff, #6e59ff);
}

.dashboard-quick-card__icon--indigo {
  background: linear-gradient(180deg, #6d8fff, #4f68ff);
}

.dashboard-quick-card__icon--cyan {
  background: linear-gradient(180deg, #7bcfff, #57a8ff);
}

.dashboard-quick-card__icon--sky {
  background: linear-gradient(180deg, #8ab6ff, #5f87ff);
}

.dashboard-quick-card__title {
  color: #182d4a;
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1.25;
}

.dashboard-quick-card__description {
  margin-top: 0.16rem;
  color: #8b98ab;
  font-size: 0.68rem;
  line-height: 1.2;
}

.dashboard-loop-card {
  min-height: 144px;
  flex-direction: column;
  gap: 0.55rem;
  justify-content: space-between;
  padding: 0.9rem 0.95rem;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.98), rgba(255, 255, 255, 0.98));
}

.dashboard-loop-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.7rem;
}

.dashboard-loop-card__badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  padding: 0.32rem 0.58rem;
}

.dashboard-loop-card__badge--blue {
  background: rgba(63, 103, 255, 0.1);
  color: #3f67ff;
}

.dashboard-loop-card__badge--teal {
  background: rgba(29, 160, 170, 0.12);
  color: #11838b;
}

.dashboard-loop-card__badge--violet {
  background: rgba(116, 92, 255, 0.12);
  color: #6952f0;
}

.dashboard-loop-card__badge--amber {
  background: rgba(255, 174, 76, 0.15);
  color: #bf6f16;
}

.dashboard-loop-card__status {
  color: #6e7f96;
  font-size: 0.72rem;
  font-weight: 600;
  text-align: right;
}

.dashboard-loop-card__title {
  color: #152b4b;
  font-size: 0.9rem;
  font-weight: 700;
  line-height: 1.3;
}

.dashboard-loop-card__description {
  color: #60728d;
  font-size: 0.8rem;
  line-height: 1.6;
}

.dashboard-loop-card__hint {
  color: #446185;
  font-size: 0.74rem;
  line-height: 1.55;
}

.dashboard-continuation-card {
  min-height: 132px;
}

.dashboard-interview-card {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  border: 1px solid var(--dashboard-border-strong);
  border-radius: var(--dashboard-card-radius);
  background: #fff;
  padding: 0.82rem 0.86rem;
}

.dashboard-interview-card__top {
  display: flex;
  align-items: flex-start;
  gap: 0.8rem;
}

.dashboard-interview-card__headline {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
}

.dashboard-interview-card__headline-main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 0.5rem;
}

.dashboard-interview-card__title {
  color: #182d4a;
  font-size: 0.92rem;
  font-weight: 700;
  white-space: nowrap;
}

.dashboard-interview-card__level {
  flex-shrink: 0;
  border-radius: var(--dashboard-pill-radius);
  background: rgba(75, 114, 255, 0.12);
  color: #4a6df4;
  font-size: 0.66rem;
  font-weight: 700;
  padding: 0.22rem 0.48rem;
}

.dashboard-interview-card__meta {
  color: #8a96aa;
  font-size: 0.76rem;
  white-space: nowrap;
}

.dashboard-interview-card__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  margin-top: 1rem;
  padding-bottom: 0.18rem;
}

.dashboard-interview-card__stat {
  padding: 0 0.52rem;
  border-right: 1px solid rgba(24, 41, 76, 0.08);
}

.dashboard-interview-card__stat:last-child {
  border-right: 0;
}

.dashboard-interview-card__stat strong {
  display: block;
  color: #1b2f4c;
  font-size: 1.08rem;
  font-weight: 800;
  letter-spacing: 0;
  font-variant-numeric: tabular-nums;
}

.dashboard-interview-card__stat span {
  display: block;
  margin-top: 0.12rem;
  color: #8a96aa;
  font-size: 0.66rem;
  font-weight: 600;
}

.dashboard-interview-card__tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.45rem;
  margin-top: 1rem;
  padding-top: 0.78rem;
  border-top: 1px solid rgba(24, 41, 76, 0.08);
}

.dashboard-interview-card__tags:empty {
  display: none;
}

.dashboard-interview-card__footer {
  display: flex;
  justify-content: flex-end;
  margin-top: auto;
  padding-top: 0.95rem;
}

.dashboard-progress-list {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.72rem;
}

.dashboard-progress-item {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  justify-content: center;
}

.dashboard-progress-item:not(:last-child) {
  padding-bottom: 0.08rem;
}

.dashboard-progress-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
  color: #182d4a;
  font-size: 0.82rem;
  font-weight: 600;
}

.dashboard-progress-item__head strong {
  color: #778499;
  font-size: 0.74rem;
  font-weight: 700;
}

.dashboard-progress-item__track {
  margin-top: 0.34rem;
  height: 8px;
  overflow: hidden;
  border-radius: var(--dashboard-pill-radius);
  background: rgba(66, 108, 214, 0.12);
}

.dashboard-progress-item__bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #3f73ff 0%, #5a7cff 100%);
}

.dashboard-interview-card__tags-title {
  width: 100%;
  color: #445269;
  font-size: 0.74rem;
  font-weight: 700;
}

.dashboard-interview-card__tag {
  border-radius: var(--dashboard-pill-radius);
  background: #f3f6ff;
  color: #6677aa;
  font-size: 0.68rem;
  font-weight: 600;
  padding: 0.32rem 0.62rem;
}

.dashboard-interview-card__detail-link {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  color: #4b6dff;
  font-size: 0.74rem;
  font-weight: 700;
}

.dashboard-interview-card__detail-link svg {
  width: 0.8rem;
  height: 0.8rem;
}

.dashboard-inline-empty {
  display: flex;
  flex: 1 1 auto;
  min-height: 156px;
  flex-direction: column;
  justify-content: center;
  border: 1px dashed rgba(18, 41, 76, 0.12);
  border-radius: var(--dashboard-card-radius);
  background: var(--dashboard-soft-bg);
  padding: 0.9rem;
}

.dashboard-inline-empty--compact {
  min-height: 144px;
  padding: 1rem;
}

.dashboard-inline-empty__title {
  color: #182d4a;
  font-size: 1rem;
  font-weight: 700;
}

.dashboard-inline-empty__desc {
  margin-top: 0.42rem;
  color: #8a96aa;
  font-size: 0.88rem;
  line-height: 1.7;
}

.dashboard-plan-card {
  border-radius: var(--dashboard-card-radius);
  border: 1px solid var(--dashboard-border);
  background: var(--dashboard-panel-bg);
  box-shadow: 0 7px 18px rgba(30, 48, 90, 0.03);
}

.dashboard-plan-card__date {
  color: #8a96aa;
  font-size: 0.74rem;
  font-weight: 600;
  white-space: nowrap;
}

.dashboard-calendar {
  border: 1px solid var(--dashboard-border-strong);
  border-radius: var(--dashboard-card-radius);
  background: var(--dashboard-soft-bg);
  padding: 0.6rem;
}

.dashboard-calendar__weekdays,
.dashboard-calendar__grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 0.42rem;
}

.dashboard-calendar__weekdays {
  margin-bottom: 0.34rem;
  color: #8b98ab;
  font-size: 0.62rem;
  font-weight: 700;
  text-align: center;
}

.dashboard-calendar__cell {
  position: relative;
  display: inline-flex;
  min-height: 1.28rem;
  align-items: center;
  justify-content: center;
  border-radius: var(--dashboard-inner-radius);
  color: #1b2f4c;
  font-size: 0.62rem;
  font-weight: 600;
}

.dashboard-calendar__cell--muted {
  color: #bcc4d3;
}

.dashboard-calendar__cell--today {
  background: linear-gradient(180deg, #4e7cff, #315eff);
  color: #fff;
}

.dashboard-calendar__cell--task::after {
  content: '';
  position: absolute;
  bottom: 0.2rem;
  left: 50%;
  width: 4px;
  height: 4px;
  border-radius: var(--dashboard-pill-radius);
  background: #4a72ff;
  transform: translateX(-50%);
}

.dashboard-calendar__cell--today.dashboard-calendar__cell--task::after {
  background: rgba(255, 255, 255, 0.9);
}

.dashboard-plan-card__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-top: 0.42rem;
  color: #172d4b;
  font-size: 0.76rem;
  font-weight: 700;
}

.dashboard-plan-card__summary strong {
  color: #3d69ff;
  font-size: 1.02rem;
  font-weight: 800;
  letter-spacing: 0;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.dashboard-plan-card__tasks {
  display: flex;
  flex-direction: column;
  gap: 0.34rem;
  margin-top: 0.4rem;
}

.dashboard-plan-card__task {
  display: flex;
  align-items: flex-start;
  gap: 0.56rem;
  border-radius: var(--dashboard-inner-radius);
  background: var(--dashboard-soft-bg);
  padding: 0.44rem 0.54rem;
}

.dashboard-plan-card__checkbox {
  width: 0.82rem;
  height: 0.82rem;
  flex-shrink: 0;
  border: 1.5px solid rgba(61, 105, 255, 0.32);
  border-radius: 5px;
  margin-top: 0.1rem;
}

.dashboard-plan-card__checkbox--checked {
  background: linear-gradient(180deg, #4f7cff, #355fff);
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.9);
}

.dashboard-plan-card__task-title {
  color: #182d4a;
  font-size: 0.7rem;
  font-weight: 600;
  line-height: 1.22;
}

.dashboard-plan-card__task-meta {
  margin-top: 0.08rem;
  color: #8a96aa;
  font-size: 0.6rem;
}

.dashboard-recommend-card {
  flex-direction: column;
  min-height: 94px;
  justify-content: space-between;
}

.dashboard-recommend-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 18px rgba(35, 58, 110, 0.05);
}

.dashboard-recommend-card {
  flex-direction: column;
}

.dashboard-recommend-card__label {
  display: inline-flex;
  align-self: flex-start;
  border-radius: var(--dashboard-pill-radius);
  color: #fff;
  font-size: 0.58rem;
  font-weight: 700;
  padding: 0.18rem 0.38rem;
}

.dashboard-recommend-card__label--violet {
  background: linear-gradient(90deg, #7a61ff, #966eff);
}

.dashboard-recommend-card__label--blue {
  background: linear-gradient(90deg, #4f86ff, #4d6dff);
}

.dashboard-recommend-card__label--indigo {
  background: linear-gradient(90deg, #688cff, #4d70ff);
}

.dashboard-recommend-card__label--green {
  background: linear-gradient(90deg, #61c973, #46b55f);
}

.dashboard-recommend-card__title {
  color: #182d4a;
  font-size: 0.74rem;
  font-weight: 700;
  line-height: 1.16;
  margin-top: 0.12rem;
}

.dashboard-recommend-card__meta {
  color: #6f7d93;
  font-size: 0.64rem;
  font-weight: 600;
  margin-top: 0.1rem;
}

.dashboard-recommend-card__hint {
  color: #94a0b2;
  font-size: 0.58rem;
  margin-top: 0.16rem;
}

.dashboard-home__loading-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--dashboard-layout-gap);
}

.dashboard-home__skeleton-line {
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(227, 233, 245, 0.86), rgba(243, 246, 252, 0.96), rgba(227, 233, 245, 0.86));
  background-size: 200% 100%;
  animation: dashboard-skeleton 1.5s infinite linear;
}

@keyframes dashboard-skeleton {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

@media (max-width: 1440px) {
  .dashboard-home__body-grid {
    grid-template-columns: minmax(0, 1fr) 390px;
  }

  .dashboard-quick-grid,
  .dashboard-loop-grid,
  .dashboard-continuation-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .dashboard-recommend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1279px) {
  .dashboard-home__body-grid,
  .dashboard-home__loading-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1100px) {
  .dashboard-summary-grid,
  .dashboard-quick-grid,
  .dashboard-loop-grid,
  .dashboard-continuation-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-hero {
    grid-template-columns: 1fr;
  }

  .dashboard-hero__visual {
    min-height: 220px;
  }

  .dashboard-recommend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .dashboard-first-visit-step {
    flex-wrap: wrap;
  }

  .dashboard-home__content {
    gap: calc(var(--dashboard-layout-gap) * 0.72);
  }

  .dashboard-home__welcome {
    order: 1;
  }

  .dashboard-quick-grid,
  .dashboard-loop-grid,
  .dashboard-continuation-grid,
  .dashboard-summary-grid,
  .dashboard-recommend-grid,
  .dashboard-interview-card__stats {
    grid-template-columns: 1fr;
  }

  .dashboard-home__welcome-title {
    font-size: 1.56rem;
  }

  .dashboard-quick-panel {
    order: 2;
  }

  .dashboard-loop-panel {
    order: 3;
  }

  .dashboard-continuation-panel {
    order: 4;
  }

  .dashboard-hero {
    order: 5;
    gap: 0.75rem;
    padding: 1rem 1rem 0.92rem;
  }

  .dashboard-summary-grid {
    order: 6;
  }

  .dashboard-recommend-panel {
    order: 7;
  }

  .dashboard-hero__title {
    margin-top: 0.42rem;
    font-size: 1.34rem;
    line-height: 1.12;
  }

  .dashboard-hero__description {
    margin-top: 0.4rem;
    font-size: 0.82rem;
    line-height: 1.5;
  }

  .dashboard-hero__reason {
    margin-top: 0.35rem;
    font-size: 0.72rem;
  }

  .dashboard-hero__signals {
    margin-top: 0.6rem;
    gap: 0.5rem;
  }

  .dashboard-hero__signal:nth-child(n + 3) {
    display: none;
  }

  .dashboard-hero__visual {
    display: none;
  }

  .dashboard-card-panel,
  .dashboard-plan-card,
  .dashboard-donut-card {
    box-shadow: 0 5px 12px rgba(30, 48, 90, 0.025);
  }
}
</style>
