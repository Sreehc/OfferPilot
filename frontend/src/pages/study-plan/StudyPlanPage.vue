<template>
  <div class="space-y-6">
    <AppShellHeader>
      <template #actions>
        <el-button
          v-if="currentPlan"
          :loading="refreshing"
          size="large"
          class="hard-button-secondary"
          @click="handleRefresh"
        >
          刷新计划
        </el-button>
        <RouterLink to="/interview" class="hard-button-primary">
          {{ currentPlan ? '继续今天的训练' : '开始安排计划' }}
        </RouterLink>
      </template>
    </AppShellHeader>

    <section class="shell-section-card plan-hero p-5 sm:p-6">
      <div class="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
        <div class="max-w-3xl">
          <div class="flex flex-wrap gap-2">
            <span class="hard-chip">{{ currentPlan ? '今天有计划可执行' : '先生成一份计划' }}</span>
            <span class="detail-pill">{{ reviewPending }} 个待复习项</span>
            <span class="detail-pill">{{ weakPointCount }} 个弱项方向</span>
          </div>
          <p class="mt-5 font-display text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">
            {{ currentPlan ? currentPlan.title : '先排好接下来几天要练什么' }}
          </p>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-secondary">
            {{
              currentPlan
                ? currentPlan.reviewSuggestion
                : '选择 7 / 14 / 30 天模板后，直接生成每天要做的题库、问答、模拟面试和复习任务。'
            }}
          </p>
        </div>

        <div class="plan-hero__signals">
          <article class="plan-hero__signal">
            <span>连续天数</span>
            <strong>{{ streakDays }}</strong>
          </article>
          <article class="plan-hero__signal">
            <span>最近面试</span>
            <strong>{{ recentInterviewCount }}</strong>
          </article>
          <article class="plan-hero__signal">
            <span>{{ currentPlan ? '计划进度' : '待巩固' }}</span>
            <strong>{{ currentPlan ? `${Math.round(currentPlan.progressRate || 0)}%` : reviewPending }}</strong>
          </article>
        </div>
      </div>
    </section>

    <section class="shell-section-card p-5 sm:p-6">
      <div class="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
        <div class="max-w-2xl">
          <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">先定好这轮训练目标</h3>
          <p class="mt-3 text-sm leading-7 text-secondary">
            填好岗位、重点方向和技术范围后，再生成最适合当前阶段的计划。
          </p>
        </div>
        <div class="grid gap-3 md:grid-cols-3 xl:min-w-[760px]">
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
      </div>

      <div class="mt-5 grid gap-4 xl:grid-cols-3">
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

    <section v-if="currentPlan" class="grid gap-4 xl:grid-cols-[minmax(0,1.15fr)_minmax(0,0.85fr)]">
      <article class="shell-section-card p-5 sm:p-6">
        <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">
              Day {{ currentPlan.currentDay }} 的执行清单
            </h3>
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
          <span>日均目标 {{ currentPlan.dailyTargetMinutes }} 分钟</span>
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
                  打开模块
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
          当前筛选下没有任务，先生成计划或切换到“全部任务”查看。
        </div>
      </article>

      <div class="space-y-4">
        <article class="shell-section-card p-5 sm:p-6">
          <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">{{ currentPlan.focusDirection }}</h3>
          <div class="mt-5 space-y-3">
            <div class="plan-signal-row">
              <span>目标岗位</span>
              <strong>{{ currentPlan.targetRole }}</strong>
            </div>
            <div class="plan-signal-row">
              <span>技术范围</span>
              <strong>{{ currentPlan.techStack }}</strong>
            </div>
            <div class="plan-signal-row">
              <span>计划周期</span>
              <strong>{{ currentPlan.startDate }} 至 {{ currentPlan.endDate }}</strong>
            </div>
          </div>
        </article>

        <article class="shell-section-card p-5 sm:p-6">
          <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">这轮重点弱项</h3>
          <div class="mt-4 flex flex-wrap gap-2">
            <span
              v-for="tag in currentPlan.weakPoints"
              :key="tag"
              class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral"
            >
              {{ tag }}
            </span>
            <span v-if="!currentPlan.weakPoints.length" class="text-sm text-secondary">等待更多训练数据</span>
          </div>
          <p class="mt-4 text-sm leading-7 text-secondary">{{ currentPlan.reviewSuggestion }}</p>
        </article>

        <article class="shell-section-card p-5 sm:p-6">
          <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">生成计划时参考了这些信号</h3>
          <div class="mt-5 space-y-3">
            <div class="plan-signal-row">
              <span>今日卡片任务</span>
              <strong>{{ todayCardTotal }}</strong>
            </div>
            <div class="plan-signal-row">
              <span>待复习项</span>
              <strong>{{ reviewPending }}</strong>
            </div>
            <div class="plan-signal-row">
              <span>当前主弱项</span>
              <strong>{{ topWeakPoint }}</strong>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section v-else class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,0.9fr)]">
      <article class="shell-section-card p-5 sm:p-6">
        <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">先生成第一份训练计划</h3>
        <div class="mt-5 space-y-3">
          <article v-for="item in emptyStateActions" :key="item.title" class="plan-task-card">
            <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">{{ item.label }}</div>
            <h4 class="mt-2 text-lg font-semibold text-ink">{{ item.title }}</h4>
            <p class="mt-2 text-sm leading-7 text-secondary">{{ item.description }}</p>
          </article>
        </div>
      </article>

      <article class="shell-section-card p-5 sm:p-6">
        <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">这些数据会影响计划内容</h3>
        <div class="mt-5 space-y-3">
          <div class="plan-signal-row">
            <span>最近面试场次</span>
            <strong>{{ recentInterviewCount }}</strong>
          </div>
          <div class="plan-signal-row">
            <span>弱项方向数</span>
            <strong>{{ weakPointCount }}</strong>
          </div>
          <div class="plan-signal-row">
            <span>重点弱项</span>
            <strong>{{ topWeakPoint }}</strong>
          </div>
        </div>
      </article>
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
    title: '快速补齐高频基础题',
    description: '适合短期面试前突击，重点压缩薄弱点、补齐标准回答并安排至少 2 次检验。',
    points: ['高频题专项刷题', 'RAG 问答压缩口径', '至少 2 次模拟面试']
  },
  {
    days: 14,
    label: '14 天强化',
    title: '稳定表达并做专项提升',
    description: '适合已经有基础、但回答不稳的人群，把弱项和项目表达穿成一条训练线。',
    points: ['方向专题训练', '错题与复习闭环', '每 4 天一次模拟面试']
  },
  {
    days: 30,
    label: '30 天闭环',
    title: '建立完整求职准备节奏',
    description: '把题库、问答、模拟面试和复习任务排成长期节奏，适合拉长周期稳定推进。',
    points: ['长期节奏拆解', '弱项持续跟踪', '面试复盘持续迭代']
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
const weakPointCount = computed(() => overview.value?.weakPoints?.length ?? 0)
const streakDays = computed(() => reviewStats.value?.currentStreak ?? overview.value?.studyStreak ?? 0)
const todayCardTotal = computed(() => (overview.value?.todayLearnCards ?? 0) + (overview.value?.todayReviewCards ?? 0))
const recentInterviewCount = computed(() => overview.value?.recentInterviews?.length ?? 0)
const topWeakPoint = computed(() => overview.value?.weakPoints?.[0]?.categoryName ?? '等待训练数据')

const displayedTasks = computed(() => {
  if (!currentPlan.value) return []
  if (taskFilter.value === 'all') return currentPlan.value.tasks
  return currentPlan.value.tasks.filter((task) => task.dayIndex === currentPlan.value?.currentDay)
})

const emptyStateActions = computed(() => [
  {
    label: '题库',
    title: weakPointCount.value > 0 ? `围绕 ${topWeakPoint.value} 起一条计划` : '先选择一个主方向建立节奏',
    description: '先把当前最弱的一类题排进计划，再围绕这批知识点继续问答和模拟面试。'
  },
  {
    label: '问答',
    title: '用 RAG 问答把答案从会写变成会讲',
    description: '不只要刷题，更要把知识点整理成你能直接说出口的回答。'
  },
  {
    label: '面试',
    title: '用模拟面试检验计划是否有效',
    description: '每轮计划都该安排一次检验，确认你练的不只是知识点，还有表达能力。'
  }
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
.plan-hero {
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.14), transparent 28%),
    radial-gradient(circle at 86% 18%, rgba(var(--bc-cyan-rgb), 0.14), transparent 22%),
    var(--bc-surface-card);
}

.plan-hero__signals {
  display: grid;
  gap: 0.75rem;
}

.plan-hero__signal,
.plan-signal-row {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.38);
  padding: 0.95rem 1rem;
  backdrop-filter: blur(10px);
}

.plan-hero__signal span,
.plan-signal-row span {
  display: block;
  font-size: 0.8rem;
  color: var(--bc-ink-secondary);
}

.plan-hero__signal strong,
.plan-signal-row strong {
  display: block;
  margin-top: 0.45rem;
  font-size: 1.4rem;
  line-height: 1.15;
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
  .plan-hero__signals {
    grid-template-columns: repeat(3, minmax(0, 160px));
  }
}
</style>
