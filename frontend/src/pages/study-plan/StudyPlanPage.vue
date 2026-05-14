<template>
  <div v-loading="loading" class="space-y-6">
    <AppShellHeader />

    <section class="shell-section-card plan-state-card p-5 sm:p-6">
      <div class="grid gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(0,0.65fr)] xl:items-start">
        <div class="min-w-0">
          <div class="flex flex-wrap gap-2">
            <span class="hard-chip">{{ currentPlan ? planStateLabel : '先生成一份计划' }}</span>
            <span v-if="currentPlan" class="detail-pill">Day {{ currentPlan.currentDay }}</span>
            <span v-if="currentPlan" class="detail-pill">{{ currentPlan.todayTaskCount }} 个今日任务</span>
            <span v-else class="detail-pill">{{ reviewPending }} 个待复习项</span>
          </div>

          <p class="mt-5 font-display text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">
            {{ currentPlan ? todayTitle : '先生成这轮训练计划' }}
          </p>
          <p class="mt-4 max-w-3xl text-sm leading-7 text-secondary">
            {{ currentPlan ? todayReason : '填好目标岗位、重点方向和技术范围后，直接生成今天就能执行的训练清单。' }}
          </p>

          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink v-if="currentPlan" :to="primaryActionPath" class="hard-button-primary">
              {{ primaryActionLabel }}
            </RouterLink>
            <a v-else href="#plan-builder" class="hard-button-primary">开始生成计划</a>
            <button
              v-if="currentPlan"
              type="button"
              class="hard-button-secondary"
              :disabled="refreshing"
              @click="handleRefresh"
            >
              刷新今日安排
            </button>
          </div>

          <div v-if="currentPlan" class="mt-6 rounded-2xl border border-[var(--bc-border-subtle)] bg-white/60 p-4">
            <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">做完会得到什么</div>
            <p class="mt-2 text-sm leading-7 text-secondary">{{ todayOutcome }}</p>
          </div>
        </div>

        <div class="grid gap-3 sm:grid-cols-3 xl:grid-cols-1">
          <article class="plan-metric-card">
            <span>{{ currentPlan ? '今日进度' : '计划模板' }}</span>
            <strong>{{ currentPlan ? `${Math.round(currentPlan.progressRate || 0)}%` : '7 / 14 / 30 天' }}</strong>
          </article>
          <article class="plan-metric-card">
            <span>{{ currentPlan ? '今日目标' : '主攻方向' }}</span>
            <strong>{{ currentPlan ? `${currentPlan.todayTaskCount} 项任务` : topWeakPoint }}</strong>
          </article>
          <article class="plan-metric-card">
            <span>{{ currentPlan ? '日均目标' : '计划入口' }}</span>
            <strong>{{ currentPlan ? `${currentPlan.dailyTargetMinutes} 分钟` : '生成后直接执行' }}</strong>
          </article>
        </div>
      </div>
    </section>

    <section v-if="currentPlan" class="grid gap-4 xl:grid-cols-[minmax(0,1.15fr)_minmax(0,0.85fr)]">
      <article class="shell-section-card p-5 sm:p-6">
        <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">今天先做这些</h3>
            <p class="mt-2 text-sm leading-7 text-secondary">
              默认先看今天可执行的任务；如果你想回看整轮安排，可以切换到全部任务。
            </p>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              type="button"
              class="plan-filter-button"
              :class="taskFilter === 'today' ? 'plan-filter-button-active' : ''"
              @click="taskFilter = 'today'"
            >
              今日任务
            </button>
            <button
              type="button"
              class="plan-filter-button"
              :class="taskFilter === 'all' ? 'plan-filter-button-active' : ''"
              @click="taskFilter = 'all'"
            >
              全部任务
            </button>
          </div>
        </div>

        <div class="mt-5 h-2 overflow-hidden rounded-full bg-[var(--panel-muted)]">
          <div class="h-full rounded-full bg-accent transition-all duration-500" :style="{ width: `${currentPlan.progressRate || 0}%` }"></div>
        </div>
        <div class="mt-3 flex flex-wrap items-center gap-4 text-sm text-secondary">
          <span>已完成 {{ currentPlan.completedTaskCount }} / {{ currentPlan.totalTaskCount }}</span>
          <span>今日 {{ currentPlan.todayTaskCount }} 项</span>
          <span>当前阶段 {{ currentPlan.currentDay }} / {{ currentPlan.durationDays }}</span>
        </div>

        <div v-if="displayedTasks.length" class="mt-5 space-y-3">
          <article v-for="task in displayedTasks" :key="task.id" class="plan-task-card">
            <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="detail-pill">Day {{ task.dayIndex }}</span>
                  <span class="detail-pill">{{ moduleLabel(task.module) }}</span>
                  <span class="detail-pill">{{ task.estimatedMinutes }} 分钟</span>
                  <span class="plan-priority" :class="priorityClass(task.priority)">{{ priorityLabel(task.priority) }}</span>
                  <span class="plan-task-status" :class="task.status === 'completed' ? 'plan-task-status-done' : 'plan-task-status-pending'">
                    {{ task.status === 'completed' ? '已完成' : '待执行' }}
                  </span>
                </div>
                <h4 class="mt-3 text-lg font-semibold text-ink">{{ task.title }}</h4>
                <p class="mt-2 text-sm leading-7 text-secondary">{{ task.description }}</p>
                <p v-if="task.completedAt" class="mt-2 text-xs text-tertiary">
                  完成于 {{ formatDateTime(task.completedAt) }}
                </p>
              </div>

              <div class="flex shrink-0 flex-wrap gap-2">
                <RouterLink :to="task.actionPath" class="hard-button-secondary text-sm">
                  打开任务
                </RouterLink>
                <el-button
                  :loading="updatingTaskId === task.id"
                  size="large"
                  class="hard-button-primary"
                  @click="handleToggleTask(task)"
                >
                  {{ task.status === 'completed' ? '恢复待办' : '标记完成' }}
                </el-button>
              </div>
            </div>
          </article>
        </div>
        <div v-else class="mt-5 rounded-2xl border border-dashed border-[var(--bc-line)] p-6 text-center text-sm text-secondary">
          当前筛选下没有任务，切换到“全部任务”查看整轮安排。
        </div>
      </article>

      <div class="space-y-4">
        <article class="shell-section-card p-5 sm:p-6">
          <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">
            {{ currentPlan.planReasonSummary?.title || '这轮计划为什么先练这些' }}
          </h3>
          <p class="mt-3 text-sm leading-7 text-secondary">
            {{ currentPlan.planReasonSummary?.summary || currentPlan.reviewSuggestion }}
          </p>
          <div class="mt-5 flex flex-wrap gap-2">
            <span
              v-for="signal in currentPlan.planReasonSummary?.signals || []"
              :key="signal"
              class="rounded-full bg-[rgba(var(--bc-accent-rgb),0.1)] px-3 py-1 text-xs font-semibold text-accent"
            >
              {{ signal }}
            </span>
          </div>
        </article>

        <article class="shell-section-card p-5 sm:p-6">
          <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">
            {{ currentPlan.trendSummary?.title || '这轮计划的执行节奏' }}
          </h3>
          <p class="mt-3 text-sm leading-7 text-secondary">
            {{ currentPlan.trendSummary?.summary || '先把今天的任务做完，计划会自动回流到首页和分析页。' }}
          </p>
          <div class="mt-5 space-y-3">
            <div
              v-for="item in currentPlan.trendSummary?.highlights || defaultTrendHighlights"
              :key="item"
              class="plan-signal-row"
            >
              <strong>{{ item }}</strong>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section v-else id="plan-builder" class="space-y-4">
      <article class="shell-section-card p-5 sm:p-6">
        <div class="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
          <div class="max-w-2xl">
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">先定这轮训练目标</h3>
            <p class="mt-3 text-sm leading-7 text-secondary">
              填好岗位、重点方向和技术范围后，系统会直接生成每天要做的题库、问答、模拟面试和复习任务。
            </p>
          </div>
          <el-button
            :loading="generatingDuration === 7"
            size="large"
            class="hard-button-primary"
            @click="handleGenerate(7)"
          >
            直接生成 7 天计划
          </el-button>
        </div>

        <div class="mt-5 grid gap-4 md:grid-cols-3">
          <div class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.22em] text-tertiary">目标岗位</div>
            <el-input v-model="targetRole" class="mt-2" size="large" placeholder="Java 后端开发" />
          </div>
          <div class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.22em] text-tertiary">重点方向</div>
            <el-select v-model="focusDirection" class="mt-2 w-full" size="large" clearable placeholder="自动推断或手动指定">
              <el-option v-for="item in directions" :key="item" :label="item" :value="item" />
            </el-select>
          </div>
          <div class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.22em] text-tertiary">技术范围</div>
            <el-input v-model="techStack" class="mt-2" size="large" placeholder="Spring Boot, MySQL, Redis" />
          </div>
        </div>
      </article>

      <div class="grid gap-4 xl:grid-cols-3">
        <article v-for="plan in planTracks" :key="plan.days" class="plan-track-card p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <span class="plan-track-card__badge">{{ plan.label }}</span>
            <span class="text-xs uppercase tracking-[0.22em] text-tertiary">{{ plan.days }} 天</span>
          </div>
          <h4 class="mt-4 text-2xl font-semibold tracking-[-0.03em] text-ink">{{ plan.title }}</h4>
          <p class="mt-3 text-sm leading-7 text-secondary">{{ plan.description }}</p>
          <ul class="mt-5 space-y-3 text-sm leading-7 text-secondary">
            <li v-for="item in plan.points" :key="item" class="plan-bullet">{{ item }}</li>
          </ul>
          <el-button
            :loading="generatingDuration === plan.days"
            size="large"
            class="action-button mt-6 w-full"
            @click="handleGenerate(plan.days)"
          >
            生成 {{ plan.days }} 天计划
          </el-button>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import AppShellHeader from '@/components/AppShellHeader.vue'
import { fetchDashboardOverviewApi } from '@/api/dashboard'
import { fetchCurrentStudyPlanApi, generateStudyPlanApi, refreshStudyPlanApi, updateStudyPlanTaskStatusApi } from '@/api/plan'
import { fetchReviewStatsApi } from '@/api/review'
import type { DashboardOverview, ReviewStats, StudyPlan, StudyPlanTaskItem } from '@/types/api'

const directions = ['Spring', 'JVM', 'MySQL', 'Redis', '并发', '微服务']
const planTracks = [
  {
    days: 7,
    label: '7 天冲刺',
    title: '先补最急的短板',
    description: '适合短期面试前快速拉齐高频题、表达和面试检验。',
    points: ['高频题专项刷题', '问答压缩口径', '至少 2 次模拟面试']
  },
  {
    days: 14,
    label: '14 天强化',
    title: '一边练题一边稳表达',
    description: '适合已经有基础、但回答不够稳的阶段，把训练和面试检验串起来。',
    points: ['方向专题训练', '错题与复习闭环', '定期模拟面试']
  },
  {
    days: 30,
    label: '30 天闭环',
    title: '把节奏做成长期工作台',
    description: '适合拉长周期稳定推进，把题库、问答、面试和复习排成可持续节奏。',
    points: ['长期节奏拆解', '弱项持续跟踪', '面试复盘迭代']
  }
]

const overview = ref<DashboardOverview | null>(null)
const reviewStats = ref<ReviewStats | null>(null)
const currentPlan = ref<StudyPlan | null>(null)
const loading = ref(false)
const refreshing = ref(false)
const generatingDuration = ref<number | null>(null)
const updatingTaskId = ref<string | null>(null)
const taskFilter = ref<'today' | 'all'>('today')

const targetRole = ref('Java 后端开发')
const focusDirection = ref('')
const techStack = ref('Spring Boot, MySQL, Redis')

const reviewPending = computed(() => reviewStats.value?.todayPending ?? overview.value?.reviewDebtCount ?? 0)
const topWeakPoint = computed(() => overview.value?.weakPoints?.[0]?.categoryName ?? overview.value?.weakCategories?.[0] ?? '等待训练数据')

const todayTasks = computed(() => {
  if (!currentPlan.value) return []
  return currentPlan.value.tasks.filter((task) => task.dayIndex === currentPlan.value?.currentDay)
})

const todayPendingTasks = computed(() => todayTasks.value.filter((task) => task.status !== 'completed'))

const nextTask = computed(() => todayPendingTasks.value[0] ?? todayTasks.value[0] ?? currentPlan.value?.tasks.find((task) => task.status !== 'completed') ?? null)

const todayTitle = computed(() => currentPlan.value?.todayFocusSummary?.title || '今天先完成计划里的任务')
const todayReason = computed(() => currentPlan.value?.todayFocusSummary?.reason || currentPlan.value?.reviewSuggestion || '')
const todayOutcome = computed(() => currentPlan.value?.todayFocusSummary?.expectedOutcome || '做完后你会更清楚下一步要补哪类题、哪段表达。')
const planStateLabel = computed(() => {
  if (!currentPlan.value) return '待生成'
  if (currentPlan.value.todayFocusSummary?.state === 'completed') return '今日已完成'
  return '今日可执行'
})
const primaryActionPath = computed(() => {
  if (!currentPlan.value) return '#plan-builder'
  if (currentPlan.value.todayFocusSummary?.state === 'completed') return '/analytics'
  return nextTask.value?.actionPath || '/dashboard'
})
const primaryActionLabel = computed(() => {
  if (!currentPlan.value) return '开始生成计划'
  if (currentPlan.value.todayFocusSummary?.state === 'completed') return '查看进度走势'
  if (!nextTask.value) return '继续今天的训练'
  return `先做 ${moduleLabel(nextTask.value.module)}`
})

const displayedTasks = computed(() => {
  if (!currentPlan.value) return []
  const list = taskFilter.value === 'today' ? todayTasks.value : currentPlan.value.tasks
  return [...list].sort((left, right) => {
    const leftPriority = left.status === 'completed' ? 1 : 0
    const rightPriority = right.status === 'completed' ? 1 : 0
    if (leftPriority !== rightPriority) {
      return leftPriority - rightPriority
    }
    return left.dayIndex - right.dayIndex
  })
})

const defaultTrendHighlights = computed(() => [
  `已完成 ${currentPlan.value?.completedTaskCount ?? 0} / ${currentPlan.value?.totalTaskCount ?? 0} 项`,
  `今天还有 ${todayPendingTasks.value.length} 项待做`,
  `剩余 ${Math.max((currentPlan.value?.durationDays ?? 0) - (currentPlan.value?.currentDay ?? 0), 0)} 天`
])

const moduleLabel = (value: string) => {
  switch (value) {
    case 'question':
      return '题库'
    case 'chat':
      return '问答'
    case 'review':
      return '复习'
    case 'interview':
      return '面试'
    default:
      return value
  }
}

const priorityLabel = (value: string) => {
  switch (value) {
    case 'high':
      return '高优先'
    case 'medium':
      return '中优先'
    default:
      return '低优先'
  }
}

const priorityClass = (value: string) => {
  if (value === 'high') return 'plan-priority-high'
  if (value === 'medium') return 'plan-priority-medium'
  return 'plan-priority-low'
}

const formatDateTime = (value?: string) => {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

const syncConfigFromPlan = () => {
  if (!currentPlan.value) return
  targetRole.value = currentPlan.value.targetRole || targetRole.value
  focusDirection.value = currentPlan.value.focusDirection || focusDirection.value
  techStack.value = currentPlan.value.techStack || techStack.value
}

const loadSignals = async () => {
  const [overviewResponse, reviewResponse] = await Promise.all([fetchDashboardOverviewApi(), fetchReviewStatsApi()])
  overview.value = overviewResponse.data
  reviewStats.value = reviewResponse.data
}

const loadCurrentPlan = async () => {
  const response = await fetchCurrentStudyPlanApi()
  currentPlan.value = response.data
  syncConfigFromPlan()
}

const loadData = async () => {
  loading.value = true
  try {
    await Promise.all([loadSignals(), loadCurrentPlan()])
  } catch {
    ElMessage.error('加载学习计划失败')
  } finally {
    loading.value = false
  }
}

const handleGenerate = async (durationDays: number) => {
  generatingDuration.value = durationDays
  try {
    const response = await generateStudyPlanApi({
      durationDays,
      focusDirection: focusDirection.value || undefined,
      targetRole: targetRole.value.trim() || undefined,
      techStack: techStack.value.trim() || undefined
    })
    currentPlan.value = response.data
    taskFilter.value = 'today'
    syncConfigFromPlan()
    await loadSignals()
    ElMessage.success(`已生成 ${durationDays} 天学习计划`)
  } catch {
    ElMessage.error('生成学习计划失败')
  } finally {
    generatingDuration.value = null
  }
}

const handleRefresh = async () => {
  if (!currentPlan.value) return
  refreshing.value = true
  try {
    const response = await refreshStudyPlanApi(currentPlan.value.id)
    currentPlan.value = response.data
    taskFilter.value = 'today'
    syncConfigFromPlan()
    await loadSignals()
    ElMessage.success('已按最新训练信号刷新计划')
  } catch {
    ElMessage.error('刷新学习计划失败')
  } finally {
    refreshing.value = false
  }
}

const handleToggleTask = async (task: StudyPlanTaskItem) => {
  updatingTaskId.value = task.id
  try {
    const response = await updateStudyPlanTaskStatusApi(task.id, {
      status: task.status === 'completed' ? 'pending' : 'completed'
    })
    currentPlan.value = response.data
    await loadSignals()
  } catch {
    ElMessage.error('更新任务状态失败')
  } finally {
    updatingTaskId.value = null
  }
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped>
.plan-state-card {
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.14), transparent 28%),
    radial-gradient(circle at 86% 18%, rgba(var(--bc-cyan-rgb), 0.14), transparent 22%),
    var(--bc-surface-card);
}

.plan-metric-card,
.plan-signal-row {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.38);
  padding: 0.95rem 1rem;
  backdrop-filter: blur(10px);
}

.plan-metric-card span,
.plan-signal-row span {
  display: block;
  font-size: 0.8rem;
  color: var(--bc-ink-secondary);
}

.plan-metric-card strong,
.plan-signal-row strong {
  display: block;
  margin-top: 0.45rem;
  font-size: 1.15rem;
  line-height: 1.25;
  color: var(--bc-ink);
}

.plan-track-card {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.12);
  background:
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.05), transparent 48%),
    var(--bc-surface-card);
}

.plan-track-card__badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: rgba(var(--bc-accent-rgb), 0.1);
  padding: 0.4rem 0.75rem;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--bc-accent);
}

.plan-bullet {
  position: relative;
  padding-left: 1rem;
}

.plan-bullet::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.8rem;
  height: 0.35rem;
  width: 0.35rem;
  border-radius: 999px;
  background: var(--bc-accent);
}

.plan-task-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 1rem 1.05rem;
}

.plan-filter-button {
  min-height: 42px;
  border-radius: 999px;
  border: 1px solid var(--bc-border-subtle);
  padding: 0 1rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--bc-ink-secondary);
  transition: all 0.2s ease;
}

.plan-filter-button:hover {
  background: var(--interactive-hover);
}

.plan-filter-button-active {
  border-color: rgba(var(--bc-accent-rgb), 0.35);
  background: rgba(var(--bc-accent-rgb), 0.1);
  color: var(--bc-ink);
}

.plan-priority,
.plan-task-status {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 0.25rem 0.65rem;
  font-size: 0.72rem;
  font-weight: 700;
}

.plan-priority-high {
  background: rgba(255, 107, 107, 0.12);
  color: var(--bc-coral);
}

.plan-priority-medium {
  background: rgba(240, 176, 67, 0.14);
  color: #b7791f;
}

.plan-priority-low {
  background: rgba(var(--bc-accent-rgb), 0.1);
  color: var(--bc-accent);
}

.plan-task-status-done {
  background: rgba(74, 122, 73, 0.12);
  color: var(--bc-lime);
}

.plan-task-status-pending {
  background: rgba(var(--bc-accent-rgb), 0.1);
  color: var(--bc-accent);
}

@media (min-width: 1024px) {
  .plan-state-card {
    min-height: 260px;
  }
}
</style>
