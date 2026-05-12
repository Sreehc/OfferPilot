<template>
  <div class="dashboard-canvas space-y-6 pb-6">
    <DashboardGuideCard v-if="showGuideCard" :actions="guideActions" @dismiss="dismissGuide" />

    <section v-if="loading" class="shell-section-card min-h-[320px] p-6 sm:p-8">
      <div class="h-4 w-24 animate-pulse rounded-full bg-slate-200 dark:bg-slate-700"></div>
      <div class="mt-6 h-14 w-3/4 animate-pulse rounded-[var(--radius-md)] bg-slate-200 dark:bg-slate-700"></div>
      <div class="mt-4 h-4 w-full animate-pulse rounded-full bg-slate-100 dark:bg-slate-800"></div>
      <div class="mt-8 grid gap-3 sm:grid-cols-3">
        <div
          v-for="index in 3"
          :key="index"
          class="h-24 animate-pulse rounded-[var(--radius-md)] bg-slate-100 dark:bg-slate-800"
        ></div>
      </div>
    </section>

    <template v-else>
      <section class="grid gap-4 xl:grid-cols-[minmax(0,1.48fr)_332px]">
        <div class="space-y-4">
          <section class="shell-section-card relative overflow-hidden p-5 sm:p-6" :class="{ 'border-[var(--bc-line-hot)]': primaryMission.urgent }">
            <div
              class="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(var(--bc-accent-rgb),0.1),transparent_30%),radial-gradient(circle_at_92%_12%,rgba(47,127,119,0.08),transparent_24%)]"
            ></div>
            <div class="relative grid gap-5 lg:grid-cols-[minmax(0,1fr)_236px] lg:items-end">
              <div>
                <div class="flex flex-wrap items-center gap-2">
                  <span class="hard-chip">连续 {{ reviewStats.currentStreak }} 天</span>
                  <span class="hard-chip">待复习 {{ todayReviewCards }} 张</span>
                </div>

                <div class="mt-5 max-w-[42rem]">
                  <h2 class="font-display text-4xl font-semibold leading-[1.02] tracking-[-0.04em] text-ink sm:text-5xl">
                    {{ primaryMission.title }}
                  </h2>
                  <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-300 sm:text-base">
                    {{ primaryMission.description }}
                  </p>
                </div>

                <div class="mt-5 flex flex-wrap items-center gap-3">
                  <RouterLink :to="primaryMission.to" class="hard-button-primary">
                    {{ primaryMission.cta }}
                  </RouterLink>
                  <RouterLink to="/review" class="hard-button-secondary">去复习</RouterLink>
                </div>
              </div>

              <div class="dashboard-hero-summary">
                <div class="dashboard-hero-summary__item">
                  <span>今日任务</span>
                  <strong>{{ todayCardTotal > 0 ? `${todayCardTotal} 张` : '待生成' }}</strong>
                </div>
                <div class="dashboard-hero-summary__item">
                  <span>完成率</span>
                  <strong>{{ formatPercent(todayCardCompletionRate) }}%</strong>
                </div>
                <div class="dashboard-hero-summary__item">
                  <span>复习负债</span>
                  <strong>{{ reviewDebtCount > 0 ? `${reviewDebtCount} 项` : '无积压' }}</strong>
                </div>
              </div>
            </div>
          </section>

          <section class="grid gap-4 lg:grid-cols-[minmax(0,1.08fr)_minmax(0,0.92fr)]">
            <article class="shell-section-card p-4 sm:p-5">
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="section-kicker">学习概览</p>
                  <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">今天先处理核心任务</h3>
                </div>
                <RouterLink to="/analytics" class="text-sm font-semibold text-accent hover:underline">
                  详细分析
                </RouterLink>
              </div>
              <div class="mt-6 grid gap-3 sm:grid-cols-2">
                <article
                  v-for="metric in metrics"
                  :key="metric.label"
                  class="dashboard-summary-card"
                  :class="metric.label === '今日卡片' ? 'dashboard-summary-card-primary sm:col-span-2' : ''"
                >
                  <p class="dashboard-summary-card__label">{{ metric.label }}</p>
                  <p class="dashboard-summary-card__value">{{ metric.value }}</p>
                </article>
              </div>
            </article>

            <article class="shell-section-card p-4 sm:p-5">
              <p class="section-kicker">下一步动作</p>
              <div class="mt-6 grid gap-3 sm:grid-cols-2">
                <RouterLink v-for="action in quickActions" :key="action.to" :to="action.to" class="dashboard-action-card">
                  <p class="dashboard-action-card__label">{{ action.label }}</p>
                  <h3 class="dashboard-action-card__title">{{ action.title }}</h3>
                </RouterLink>
              </div>
            </article>
          </section>
        </div>

        <aside class="space-y-4">
          <section class="shell-section-card p-4 sm:p-5">
            <p class="section-kicker">今日状态</p>
            <div class="mt-5 space-y-4">
              <div class="dashboard-status-row">
                <span>当前状态</span>
                <strong>{{ overview.todayCompletionStatus || '等待开始' }}</strong>
              </div>
              <div class="dashboard-status-row">
                <span>连续学习</span>
                <strong>{{ overview.studyStreak ?? reviewStats.currentStreak ?? 0 }} 天</strong>
              </div>
              <div class="dashboard-status-row">
                <span>最近诊断</span>
                <strong>{{ overview.recentInterviews?.length ? `${overview.recentInterviews.length} 场` : '暂无' }}</strong>
              </div>
            </div>
          </section>

          <section class="shell-section-card p-4 sm:p-5">
            <div class="flex items-center justify-between gap-3">
              <div>
                <p class="section-kicker">压力分类</p>
                <h3 class="mt-3 text-xl font-semibold tracking-[-0.03em] text-ink">优先处理的分类</h3>
              </div>
              <RouterLink to="/review" class="text-sm font-semibold text-accent hover:underline">
                去处理
              </RouterLink>
            </div>

            <div v-if="overview.categoryMasterySummary && overview.categoryMasterySummary.length > 0" class="mt-6 space-y-3">
              <div
                v-for="c in overview.categoryMasterySummary.slice(0, 3)"
                :key="`${c.categoryName}-${c.categoryId ?? 'na'}`"
                class="dashboard-pressure-row"
              >
                <div>
                  <p class="font-semibold text-ink">{{ c.categoryName }}</p>
                  <p class="text-xs text-slate-500">待复习 {{ c.dueCount }} · 已掌握 {{ c.masteredCards }}/{{ c.totalCards }}</p>
                </div>
                <strong>{{ Math.round(c.masteryRate) }}%</strong>
              </div>
            </div>
            <EmptyState
              v-else
              class="mt-6"
              icon="chart"
              title="暂无压力分类"
              description="生成并复习卡片后查看。"
              compact
            />
          </section>
        </aside>
      </section>

      <section class="shell-section-card p-5 sm:p-6">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div class="flex items-center gap-3">
            <div class="flex h-9 w-9 items-center justify-center rounded-full bg-accent/10 text-accent">
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
                />
              </svg>
            </div>
            <div>
              <p class="text-xl font-semibold tracking-[-0.03em] text-ink">进阶分析</p>
              <p class="text-sm text-slate-500">{{ overview.recentInterviews?.length ? `最近 ${overview.recentInterviews.length} 场诊断` : '暂无诊断记录' }}</p>
            </div>
          </div>
          <RouterLink to="/analytics" class="text-sm font-semibold text-accent hover:underline">查看全部分析</RouterLink>
        </div>

        <div class="mt-6 space-y-4">
          <section class="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
            <DashboardInterviews :interviews="overview.recentInterviews" />
            <DashboardWeakPoints :items="overview.categoryMasterySummary || []" />
          </section>

          <section class="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
            <article class="shell-section-card p-4 sm:p-6">
              <p class="section-kicker">记忆成长摘要</p>
              <h3 class="mt-3 text-xl font-semibold tracking-[-0.03em] text-ink sm:text-2xl">最近的记忆推进状态</h3>
              <div class="mt-4 grid gap-4 sm:grid-cols-2">
                <div>
                  <p class="text-xs text-slate-500">今日完成状态</p>
                  <p class="mt-1 text-xl font-semibold text-ink">
                    {{ overview.todayCompletionStatus || '等待今日卡片开始' }}
                  </p>
                </div>
                <div>
                  <p class="text-xs text-slate-500">连续学习</p>
                  <p class="mt-1 text-3xl font-bold text-ink">
                    {{ overview.studyStreak ?? reviewStats.currentStreak ?? 0 }} 天
                  </p>
                </div>
              </div>
              <div v-if="overview.categoryMasterySummary && overview.categoryMasterySummary.length > 0" class="mt-5 space-y-2">
                <p class="mb-2 text-xs font-medium text-slate-500">当前最有压力的分类</p>
                <div
                  v-for="c in overview.categoryMasterySummary.slice(0, 4)"
                  :key="`${c.categoryName}-${c.categoryId ?? 'na'}`"
                  class="flex items-center justify-between text-sm"
                >
                  <span class="truncate text-slate-600 dark:text-slate-300">{{ c.categoryName }}</span>
                  <div class="flex items-center gap-2">
                    <span class="text-xs text-slate-400">待复习 {{ c.dueCount }}</span>
                    <span class="text-xs font-medium text-accent">{{ Math.round(c.masteryRate) }}%</span>
                  </div>
                </div>
              </div>
            </article>

            <article class="shell-section-card p-4 sm:p-6">
              <p class="section-kicker">趋势记录</p>
              <DashboardInterviewTrend :trend-data="trendData" :loading="trendLoading" />
              <DashboardReviewHeatmap
                class="mt-4"
                :heatmap="reviewStats.heatmap"
                :streak="reviewStats.currentStreak"
                :today-pending="reviewStats.todayPending"
              />
            </article>
          </section>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import DashboardGuideCard from './DashboardGuideCard.vue'
import DashboardInterviews from './DashboardInterviews.vue'
import DashboardWeakPoints from './DashboardWeakPoints.vue'
import DashboardInterviewTrend from './DashboardInterviewTrend.vue'
import DashboardReviewHeatmap from './DashboardReviewHeatmap.vue'
import { fetchDashboardOverviewApi } from '@/api/dashboard'
import { fetchInterviewTrendApi } from '@/api/interview'
import { fetchReviewStatsApi } from '@/api/review'
import type { DashboardOverview, ReviewStats } from '@/types/api'
import { useAuthStore } from '@/stores/auth'
import { storage } from '@/utils/storage'

const authStore = useAuthStore()
const loading = ref(true)
const trendLoading = ref(true)
const trendData = ref<Array<{ sessionId: string; direction: string; totalScore: number; startTime?: string }>>([])
const reviewStats = ref<ReviewStats>({ totalReviews: 0, currentStreak: 0, todayPending: 0 })
const guideDismissed = ref(false)
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

const todayLearnCards = computed(() => overview.value.todayLearnCards ?? 0)
const todayReviewCards = computed(() => overview.value.todayReviewCards ?? 0)
const todayCardCompletionRate = computed(() => overview.value.todayCardCompletionRate ?? 0)
const reviewDebtCount = computed(() => overview.value.reviewDebtCount ?? 0)
const todayCardTotal = computed(() => todayLearnCards.value + todayReviewCards.value)

const metrics = computed(() => [
  {
    label: '今日卡片',
    value: todayCardTotal.value > 0 ? `${todayCardTotal.value} 张` : '待开始'
  },
  {
    label: '完成率',
    value: `${formatPercent(todayCardCompletionRate.value)}%`
  },
  {
    label: '连续天数',
    value: `${overview.value.studyStreak ?? reviewStats.value.currentStreak} 天`
  },
  {
    label: '复习负债',
    value: reviewDebtCount.value > 0 ? `${reviewDebtCount.value} 项` : '无积压'
  }
])

const primaryMission = computed(() => {
  if (todayCardTotal.value > 0) {
    return {
      to: '/cards',
      title: '今天先完成今日卡片',
      description: `今天共有 ${todayCardTotal.value} 张卡片要处理，其中待学 ${todayLearnCards.value} 张、待复习 ${todayReviewCards.value} 张。`,
      cta: '开始今日卡片',
      urgent: todayReviewCards.value > 0 || reviewDebtCount.value > 0
    }
  }

  return {
    to: '/knowledge',
    title: '先生成第一组卡片',
    description: '你今天还没有可处理的卡片，先从知识库生成一组内容。',
    cta: '去知识库',
    urgent: false
  }
})

const quickActions = computed(() => [
  {
    to: '/cards',
    label: '今日卡片',
    title: todayCardTotal.value > 0 ? `处理今天的 ${todayCardTotal.value} 张卡片` : '开始今天的卡片任务'
  },
  {
    to: '/review?tab=all',
    label: '复习',
    title: reviewDebtCount.value > 0 ? `清理 ${reviewDebtCount.value} 项积压` : '查看今天的复习'
  },
  {
    to: '/knowledge',
    label: '知识库',
    title: '上传资料或生成卡片'
  },
  {
    to: '/chat',
    label: '问答',
    title: '继续追问没吃透的问题'
  }
])

const guideActions = computed(() => [
  {
    to: '/knowledge',
    label: '去知识库'
  }
])

const showGuideCard = computed(() => {
  const userId = authStore.user?.id
  if (!userId || guideDismissed.value || !overview.value.firstVisit) return false
  if (todayCardTotal.value === 0) return false
  return !storage.getGuideSeen(userId)
})

const dismissGuide = () => {
  if (authStore.user?.id) {
    storage.setGuideSeen(authStore.user.id)
  }
  guideDismissed.value = true
}

const loadOverview = async () => {
  loading.value = true
  try {
    const response = await fetchDashboardOverviewApi()
    overview.value = response.data
  } catch {
    ElMessage.error('首页数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const loadTrend = async () => {
  trendLoading.value = true
  try {
    const response = await fetchInterviewTrendApi(20)
    trendData.value = (response.data || []).map((item) => ({
      sessionId: item.sessionId,
      direction: item.direction,
      totalScore: item.totalScore,
      startTime: item.startTime
    }))
  } catch {
    // supplementary only
  } finally {
    trendLoading.value = false
  }
}

const loadReviewStats = async () => {
  try {
    const response = await fetchReviewStatsApi()
    reviewStats.value = response.data
  } catch {
    // silently fail
  }
}

const formatPercent = (value: number): string => {
  return Number.isInteger(value) ? String(value) : value.toFixed(0)
}

onMounted(() => {
  void loadOverview()
  void loadTrend()
  void loadReviewStats()
})
</script>

<style scoped>
.dashboard-summary-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.74);
  padding: 1rem 1.1rem;
}

.dashboard-summary-card-primary {
  background:
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.08), transparent 42%),
    rgba(255, 255, 255, 0.82);
}

.dashboard-summary-card__label {
  font-size: 0.78rem;
  color: var(--bc-ink-secondary);
}

.dashboard-summary-card__value {
  margin-top: 0.5rem;
  font-family: theme('fontFamily.mono');
  font-size: clamp(1.8rem, 2vw, 2.5rem);
  font-weight: 700;
  line-height: 1;
  color: var(--bc-ink);
}

.dashboard-action-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.72);
  padding: 1rem 1.1rem;
  transition:
    transform var(--motion-fast) var(--ease-hard),
    box-shadow var(--motion-fast) var(--ease-hard),
    background-color var(--motion-fast) var(--ease-hard);
}

.dashboard-action-card:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--bc-shadow-hover);
}

.dashboard-action-card__label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--bc-ink-secondary);
}

.dashboard-action-card__title {
  margin-top: 0.8rem;
  font-size: 1.1rem;
  font-weight: 700;
  line-height: 1.35;
  color: var(--bc-ink);
}

.dashboard-hero-summary {
  display: grid;
  gap: 0.75rem;
}

.dashboard-hero-summary__item {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.1);
  background: rgba(255, 255, 255, 0.74);
  padding: 0.9rem 1rem;
}

.dashboard-hero-summary__item span {
  display: block;
  font-size: 0.78rem;
  color: var(--bc-ink-secondary);
}

.dashboard-hero-summary__item strong {
  display: block;
  margin-top: 0.45rem;
  font-size: 1.2rem;
  line-height: 1.2;
  color: var(--bc-ink);
}

.dashboard-status-row,
.dashboard-pressure-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.dashboard-status-row span,
.dashboard-pressure-row p:last-child {
  color: var(--bc-ink-secondary);
}

.dashboard-status-row strong,
.dashboard-pressure-row strong {
  color: var(--bc-ink);
  font-weight: 700;
}
</style>
