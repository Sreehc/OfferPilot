<template>
  <div class="dashboard-canvas space-y-6 pb-6">
    <section
      v-if="loading"
      class="shell-section-card min-h-[320px] p-6 sm:p-8"
    >
      <div class="skeleton-line h-4 w-24" />
      <div class="skeleton-line mt-6 h-14 w-3/4 rounded-[var(--radius-md)]" />
      <div class="skeleton-line mt-4 h-4 w-full" />
      <div class="mt-8 grid gap-3 sm:grid-cols-3">
        <div
          v-for="index in 3"
          :key="index"
          class="skeleton-line h-24 rounded-[var(--radius-md)]"
        />
      </div>
    </section>

    <template v-else>
      <section
        class="dashboard-hero shell-section-card relative overflow-hidden p-5 sm:p-6"
        :class="{ 'border-[var(--bc-line-hot)]': primaryMission.urgent }"
      >
        <div class="dashboard-hero__mesh" />
        <div class="relative grid gap-5 xl:grid-cols-[minmax(0,1fr)_320px] xl:items-end">
          <div>
            <div class="flex flex-wrap gap-2">
              <span class="hard-chip">下一步</span>
              <span class="detail-pill">连续 {{ reviewStats.currentStreak }} 天</span>
              <span class="detail-pill">待巩固 {{ reviewPending }} 项</span>
            </div>

            <div class="mt-6 max-w-[48rem]">
              <h1 class="font-display text-4xl font-semibold leading-[1.02] tracking-[-0.05em] text-ink sm:text-5xl">
                {{ primaryMission.title }}
              </h1>
              <p class="mt-4 text-sm leading-7 text-secondary sm:text-base">
                {{ primaryMission.description }}
              </p>
            </div>

            <div class="mt-6 flex flex-wrap gap-3">
              <RouterLink
                :to="primaryMission.to"
                class="hard-button-primary"
              >
                {{ primaryMission.cta }}
              </RouterLink>
              <RouterLink
                to="/analytics"
                class="accent-link text-sm font-semibold"
              >
                查看整体趋势
              </RouterLink>
            </div>
          </div>

          <div class="dashboard-hero__summary">
            <article class="dashboard-hero__summary-item">
              <span>当前计划</span>
              <strong>{{ dashboardHeroSummary.plan }}</strong>
            </article>
            <article class="dashboard-hero__summary-item">
              <span>简历准备</span>
              <strong>{{ dashboardHeroSummary.resume }}</strong>
            </article>
            <article class="dashboard-hero__summary-item">
              <span>投递进展</span>
              <strong>{{ dashboardHeroSummary.application }}</strong>
            </article>
          </div>
        </div>
      </section>

      <DashboardMetrics :metrics="trainingMetrics" />

      <section class="shell-section-card p-5 sm:p-6">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div class="min-w-0">
            <h2 class="text-2xl font-semibold tracking-[-0.03em] text-ink">
              接下来可以直接去这里
            </h2>
            <p class="mt-2 text-sm leading-7 text-secondary">
              只保留现在就能直接开始的主线动作，先选一个入口继续推进。
            </p>
          </div>
        </div>

        <div class="dashboard-route-grid mt-5">
          <RouterLink
            v-for="item in primaryRoutes"
            :key="item.to"
            :to="item.to"
            class="dashboard-route-card"
          >
            <div class="dashboard-route-card__topline">
              <span class="dashboard-route-card__label">{{ item.label }}</span>
              <span class="dashboard-route-card__status">{{ item.status }}</span>
            </div>
            <div class="dashboard-route-card__body">
              <h3 class="dashboard-route-card__title">
                {{ item.title }}
              </h3>
              <p class="dashboard-route-card__description">
                {{ item.description }}
              </p>
            </div>
          </RouterLink>
        </div>
      </section>

      <section class="grid gap-4 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
        <article class="shell-section-card p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <div>
              <h2 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
                优先补齐的训练方向
              </h2>
            </div>
            <RouterLink
              to="/question"
              class="accent-link text-sm font-semibold"
            >
              去题库 →
            </RouterLink>
          </div>

          <div
            v-if="overview.weakPoints.length"
            class="mt-5 space-y-3"
          >
            <article
              v-for="item in overview.weakPoints.slice(0, 4)"
              :key="item.categoryName"
              class="dashboard-weak-card"
            >
              <div class="flex items-center justify-between gap-4">
                <div>
                  <div class="text-sm font-semibold text-ink">
                    {{ item.categoryName }}
                  </div>
                  <div class="mt-1 text-xs text-secondary">
                    错题 {{ item.wrongCount }} · 当前得分 {{ formatScore(item.score) }}
                  </div>
                </div>
                <RouterLink
                  to="/chat"
                  class="hard-button-secondary text-sm"
                >
                  扩展回答
                </RouterLink>
              </div>
            </article>
          </div>

          <EmptyState
            v-else
            icon="chart"
            title="弱项数据还不够"
            description="完成题库训练和模拟面试后，这里会优先显示需要补齐的方向。"
            compact
            class="mt-5"
          />
        </article>

        <article class="shell-section-card p-5 sm:p-6">
          <div class="min-w-0">
            <h2 class="text-2xl font-semibold tracking-[-0.03em] text-ink">
              需要时再补这些准备
            </h2>
            <p class="mt-2 text-sm leading-7 text-secondary">
              这些入口不是当前主任务，但你需要补资料、补简历或补巩固时可以直接回到这里。
            </p>
          </div>

          <div class="mt-5 space-y-3">
            <RouterLink
              v-for="item in secondaryRoutes"
              :key="item.to"
              :to="item.to"
              class="dashboard-secondary-card"
            >
              <div class="dashboard-secondary-card__content">
                <div class="dashboard-secondary-card__topline">
                  <span>{{ item.label }}</span>
                  <span class="dashboard-secondary-card__hint">{{ item.hint }}</span>
                </div>
                <h3 class="dashboard-secondary-card__title">
                  {{ item.title }}
                </h3>
              </div>
            </RouterLink>
          </div>
        </article>
      </section>

      <section class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_380px]">
        <DashboardInterviewTrend
          :trend-data="interviewTrendData"
          :loading="false"
        />
        <DashboardInterviews :interviews="overview.recentInterviews" />
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { fetchDashboardOverviewApi } from '@/api/dashboard'
import { fetchReviewStatsApi } from '@/api/review'
import type { DashboardOverview, ReviewStats } from '@/types/api'
import DashboardInterviews from './DashboardInterviews.vue'
import DashboardInterviewTrend from './DashboardInterviewTrend.vue'
import DashboardMetrics from './DashboardMetrics.vue'

const loading = ref(true)
const reviewStats = ref<ReviewStats>({ totalReviews: 0, currentStreak: 0, todayPending: 0 })
const overview = ref<DashboardOverview>({
  learningCount: 0,
  averageScore: 0,
  wrongCount: 0,
  recentInterviews: [],
  weakPoints: [],
  firstVisit: true,
  todayLearnCards: 0,
  todayReviewCards: 0,
  todayCompletedCards: 0,
  todayCardCompletionRate: 0,
  masteredCardCount: 0,
  reviewDebtCount: 0,
  studyStreak: 0,
  categoryMasterySummary: []
})

const reviewPending = computed(() => reviewStats.value.todayPending ?? overview.value.reviewDebtCount ?? 0)
const todayCardTotal = computed(() => (overview.value.todayLearnCards ?? 0) + (overview.value.todayReviewCards ?? 0))
const todayPlanMetric = computed(() => {
  const plan = overview.value.planSummary
  if (!plan?.planId) return '未开始'
  const todayCount = plan.todayTaskCount ?? plan.totalTaskCount ?? 0
  return `${plan.completedTaskCount ?? 0}/${todayCount}`
})

const trainingMetrics = computed(() => [
  {
    label: '今日计划',
    value: todayPlanMetric.value,
    desc: overview.value.planSummary?.planId
      ? `Day ${overview.value.planSummary.currentDay ?? 1}，先把今天的任务推进完`
      : '先生成一份训练计划'
  },
  {
    label: '简历准备',
    value: `${overview.value.resumeSummary?.resumeCount ?? 0} 份`,
    desc: overview.value.resumeSummary?.latestResumeTitle || '先上传一份简历开始整理项目表达'
  },
  {
    label: '投递推进',
    value: `${overview.value.applicationSummary?.activeCount ?? 0} 条`,
    desc: overview.value.applicationSummary?.totalCount
      ? `共记录 ${overview.value.applicationSummary.totalCount} 条投递`
      : '先录入第一条岗位信息'
  },
  {
    label: '本周面试',
    value: String(overview.value.thisWeekInterviewCount ?? overview.value.recentInterviews.length ?? 0),
    desc: `${formatScore(overview.value.averageScore)} 分最近均分`
  }
])

const dashboardHeroSummary = computed(() => ({
  plan: overview.value.planSummary?.planId
    ? `${Math.round(overview.value.planSummary.progressRate ?? 0)}%`
    : '先生成计划',
  resume: overview.value.resumeSummary?.resumeCount
    ? `${overview.value.resumeSummary.resumeCount} 份`
    : '先上传简历',
  application: overview.value.applicationSummary?.activeCount
    ? `${overview.value.applicationSummary.activeCount} 条进行中`
    : '先记录岗位'
}))

const primaryMission = computed(() => {
  if (overview.value.nextStep?.title) {
    return {
      to: overview.value.nextStep.actionPath || '/dashboard',
      title: overview.value.nextStep.title,
      description: overview.value.nextStep.description,
      cta: routeActionLabel(overview.value.nextStep.actionPath),
      urgent: Boolean(overview.value.planSummary?.todayTaskCount || overview.value.applicationSummary?.activeCount)
    }
  }

  const weakPoint = overview.value.weakPoints[0]

  if (weakPoint) {
    return {
      to: '/question',
      title: `先补 ${weakPoint.categoryName} 这一块`,
      description: `先用题库把 ${weakPoint.categoryName} 练顺，再去问答页补全回答结构，最后用模拟面试检查表达是否稳定。`,
      cta: '去题库训练',
      urgent: weakPoint.wrongCount > 0
    }
  }

  if (!overview.value.recentInterviews.length) {
    return {
      to: '/interview',
      title: '先开始第一场模拟面试',
      description: '先做一场模拟面试拿到基线，再决定接下来是补题库、练问答，还是安排下一轮计划。',
      cta: '开始模拟面试',
      urgent: false
    }
  }

  if (reviewPending.value > 0 || todayCardTotal.value > 0) {
    return {
      to: '/review',
      title: '把待巩固内容先清掉',
      description: `你还有 ${reviewPending.value} 项待复习内容和 ${todayCardTotal.value} 张卡片任务。先清掉这些积压，再继续新的训练会更稳。`,
      cta: '去复习巩固',
      urgent: reviewPending.value > 0
    }
  }

  return {
    to: '/study-plan',
    title: '安排下一段训练节奏',
    description: '把接下来几天要练的题目、问答和模拟面试排好顺序，避免训练断档。',
    cta: '查看学习计划',
    urgent: false
  }
})

const primaryRoutes = computed(() => [
  {
    to: '/question',
    label: '题库训练',
    title: '先刷今天最该补的题',
    description: '按分类、难度和岗位方向快速找到当前最该练的题目。',
    status: '立即开始'
  },
  {
    to: '/chat',
    label: '问答',
    title: '把答案讲完整',
    description: '围绕题目、项目和资料继续追问，把会写变成会讲。',
    status: '继续深挖'
  },
  {
    to: '/interview',
    label: '面试',
    title: '检验你的表达',
    description: '用文字或语音做一次完整演练，看看回答是否稳定。',
    status: '做一轮'
  },
  {
    to: '/study-plan',
    label: '计划',
    title: '排好接下来几天的训练',
    description: '把题库、问答、复习和面试排成连续节奏，减少临场决策。',
    status: '安排节奏'
  },
  {
    to: '/applications',
    label: '投递',
    title: '推进你的投递',
    description: '记录岗位、跟进状态，再把面试过程和复盘沉淀下来。',
    status: '记录进展'
  },
  {
    to: '/analytics',
    label: '洞察',
    title: '回看你的趋势',
    description: '看看弱项、得分和节奏怎么变化，再决定下一步优先级。',
    status: '查看变化'
  }
])

const secondaryRoutes = computed(() => [
  {
    to: '/resume',
    label: '简历助手',
    title: overview.value.resumeSummary?.latestResumeTitle || '继续整理你的项目表达',
    hint: '整理简历'
  },
  {
    to: '/knowledge',
    label: '知识库',
    title: '回看资料并继续追问',
    hint: '补资料'
  },
  {
    to: '/cards',
    label: '卡片与复习',
    title: reviewPending.value > 0 ? `先清掉 ${reviewPending.value} 项待巩固内容` : '需要时再做专项强化',
    hint: '补巩固'
  }
])

const routeActionLabel = (path?: string) => {
  switch (path) {
    case '/resume':
      return '去上传简历'
    case '/study-plan':
      return '去安排计划'
    case '/applications':
      return '去推进投递'
    case '/interview':
      return '去模拟面试'
    case '/question':
      return '去题库训练'
    default:
      return '继续处理'
  }
}

const interviewTrendData = computed(() =>
  overview.value.recentInterviews.map((item) => ({
    sessionId: item.sessionId,
    direction: item.direction,
    totalScore: item.totalScore,
    startTime: item.finishedAt
  }))
)

const loadOverview = async () => {
  loading.value = true
  try {
    const [overviewResponse, reviewResponse] = await Promise.all([fetchDashboardOverviewApi(), fetchReviewStatsApi()])
    overview.value = overviewResponse.data
    reviewStats.value = reviewResponse.data
  } catch {
    ElMessage.error('首页数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function formatScore(value: number): string {
  if (!Number.isFinite(value)) return '0'
  return Number.isInteger(value) ? String(value) : value.toFixed(0)
}

onMounted(() => {
  void loadOverview()
})
</script>

<style scoped>
.dashboard-hero {
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.12), transparent 28%),
    radial-gradient(circle at 88% 18%, rgba(var(--bc-cyan-rgb), 0.12), transparent 22%),
    var(--bc-surface-card);
}

.dashboard-hero__mesh {
  position: absolute;
  inset: 0;
  opacity: 0.35;
  background:
    linear-gradient(120deg, rgba(var(--bc-accent-rgb), 0.05), transparent 42%),
    linear-gradient(320deg, rgba(var(--bc-cyan-rgb), 0.04), transparent 38%);
}

.dashboard-hero__summary {
  display: grid;
  gap: 0.75rem;
}

.dashboard-hero__summary-item {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.16);
  background: rgba(255, 255, 255, 0.38);
  padding: 0.95rem 1rem;
  backdrop-filter: blur(10px);
}

.dashboard-hero__summary-item span {
  display: block;
  font-size: 0.78rem;
  color: var(--bc-ink-secondary);
}

.dashboard-hero__summary-item strong {
  display: block;
  margin-top: 0.45rem;
  font-size: 1.2rem;
  line-height: 1.15;
  color: var(--bc-ink);
}

.dashboard-route-grid {
  display: grid;
  gap: 0.9rem;
}

.dashboard-route-card {
  display: flex;
  min-width: 0;
  min-height: 100%;
  flex-direction: column;
  gap: 0.85rem;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.07), transparent 56%), var(--bc-surface-muted);
  box-shadow: var(--bc-shadow-soft);
  padding: 1rem 1.05rem;
  transition:
    transform var(--motion-fast) var(--ease-hard),
    box-shadow var(--motion-fast) var(--ease-hard);
}

.dashboard-route-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--bc-shadow-hover);
}

.dashboard-route-card__topline {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.dashboard-route-card__label,
.dashboard-route-card__status,
.dashboard-secondary-card__topline {
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dashboard-route-card__label {
  color: var(--bc-ink-secondary);
}

.dashboard-route-card__status {
  color: var(--bc-accent);
}

.dashboard-route-card__body {
  min-width: 0;
}

.dashboard-route-card__title,
.dashboard-secondary-card__title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--bc-ink);
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.dashboard-route-card__description {
  font-size: 0.92rem;
  line-height: 1.7;
  color: var(--bc-ink-secondary);
}

.dashboard-weak-card,
.dashboard-secondary-card {
  display: flex;
  min-width: 0;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 1rem;
}

.dashboard-secondary-card__content {
  min-width: 0;
}

.dashboard-secondary-card__topline {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.75rem;
  color: var(--bc-ink-secondary);
}

.dashboard-secondary-card__hint {
  white-space: nowrap;
}

@media (min-width: 960px) {
  .dashboard-route-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1400px) {
  .dashboard-route-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
