<template>
  <div class="space-y-6">
    <!-- New user guide -->
    <DashboardGuideCard
      v-if="showGuideCard"
      :actions="quickActions"
      @dismiss="dismissGuide"
    />

    <!-- Loading skeleton -->
    <section v-if="loading" class="grid gap-4 grid-cols-2 xl:grid-cols-4">
      <article v-for="index in 4" :key="index" class="metric-card">
        <div class="h-4 w-20 animate-pulse bg-slate-200 dark:bg-slate-700" style="border-radius: var(--radius-sm);"></div>
        <div class="mt-5 h-12 w-24 animate-pulse bg-slate-200 dark:bg-slate-700" style="border-radius: var(--radius-md);"></div>
        <div class="mt-3 h-4 w-full animate-pulse bg-slate-100 dark:bg-slate-800" style="border-radius: var(--radius-sm);"></div>
      </article>
    </section>

    <template v-else>
      <!-- ════════════ FIRST SCREEN: Metrics + Action Bar ════════════ -->
      <DashboardMetrics :metrics="metrics" />

      <!-- Action Bar: 3 prominent action cards -->
      <section class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <!-- Review action -->
        <RouterLink to="/review" class="paper-panel p-5 group cursor-pointer transition hover:shadow-md">
          <div class="flex items-start justify-between gap-3">
            <div>
              <p class="section-kicker">间隔复习</p>
              <h3 class="mt-3 text-xl font-semibold tracking-[-0.03em] text-ink">
                {{ reviewStats.todayPending > 0 ? `${reviewStats.todayPending} 道待复习` : '今日已复习完毕' }}
              </h3>
              <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">
                累计 {{ reviewStats.totalReviews }} 次 · 连续 {{ reviewStats.currentStreak }} 天
              </p>
            </div>
            <div
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full transition"
              :class="reviewStats.todayPending > 0 ? 'bg-accent text-white animate-pulse' : 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400'"
            >
              <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
              </svg>
            </div>
          </div>
        </RouterLink>

        <!-- Interview recommendation -->
        <RouterLink to="/interview" class="paper-panel p-5 group cursor-pointer transition hover:shadow-md">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="section-kicker">推荐面试</p>
              <h3 class="mt-3 text-xl font-semibold tracking-[-0.03em] text-ink">
                {{ recommendInterview ? `练习${recommendInterview.direction}` : '开始第一场面试' }}
              </h3>
              <p class="mt-2 text-sm text-slate-500 dark:text-slate-400 line-clamp-2">
                {{ recommendInterview?.reason || '完成面试后系统会推荐适合你的方向。' }}
              </p>
              <div v-if="overview.overallAbility !== undefined" class="mt-3 flex items-center gap-3">
                <span class="text-xs text-slate-400">能力 <span class="font-semibold text-accent">{{ overview.overallAbility }}</span></span>
                <span
                  class="px-2 py-0.5 text-xs font-medium rounded-full"
                  :class="difficultyClass(overview.recommendedDifficulty)"
                >
                  {{ difficultyLabel(overview.recommendedDifficulty) }}
                </span>
              </div>
            </div>
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-accent/10 text-accent">
              <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
        </RouterLink>

        <!-- Quick actions -->
        <article class="paper-panel p-5">
          <p class="section-kicker">快捷入口</p>
          <div class="mt-3 grid grid-cols-2 gap-2">
            <RouterLink
              v-for="action in quickActions"
              :key="action.to"
              :to="action.to"
              class="rounded-lg px-3 py-2.5 text-sm font-medium text-ink transition hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              {{ action.label }}
            </RouterLink>
          </div>
        </article>
      </section>

      <!-- Weak categories (if any) -->
      <div v-if="overview.weakCategories && overview.weakCategories.length > 0" class="flex flex-wrap items-center gap-2">
        <span class="text-xs text-slate-500">薄弱分类：</span>
        <span
          v-for="cat in overview.weakCategories"
          :key="cat"
          class="px-2.5 py-1 text-xs font-medium rounded-full bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400"
        >
          {{ cat }}
        </span>
        <RouterLink to="/analytics" class="text-xs text-accent hover:underline ml-1">查看分析 →</RouterLink>
      </div>

      <!-- ════════════ BELOW FOLD: Collapsible Analysis ════════════ -->
      <el-collapse v-model="activeCollapse" class="space-y-4">
        <!-- Learning Analysis -->
        <el-collapse-item name="analysis" class="!border-0">
          <template #title>
            <div class="flex items-center gap-3 py-1">
              <div class="flex h-8 w-8 items-center justify-center rounded-full bg-accent/10 text-accent">
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
              <div>
                <span class="text-lg font-semibold text-ink">学习分析</span>
                <span class="ml-2 text-sm text-slate-400">最近面试、薄弱点、能力分布、学习洞察</span>
              </div>
            </div>
          </template>

          <div class="space-y-4 pt-2">
            <section class="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
              <DashboardInterviews :interviews="overview.recentInterviews" />
              <DashboardWeakPoints :weak-points="overview.weakPoints" :plan-completion-rate="overview.planCompletionRate" :plan-health-score="overview.planHealthScore ?? 100" />
            </section>

            <section class="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
              <!-- Learning Insights -->
              <article class="paper-panel p-4 sm:p-6">
                <p class="section-kicker">学习洞察</p>
                <h3 class="mt-3 text-xl sm:text-2xl font-semibold tracking-[-0.03em] text-ink">本周表现</h3>
                <div class="mt-4 flex items-end gap-6">
                  <div>
                    <p class="text-xs text-slate-500">本周平均分</p>
                    <p class="mt-1 text-3xl font-bold" :class="weekComparisonClass">
                      {{ overview.thisWeekAvgScore ?? '-' }}
                    </p>
                  </div>
                  <div>
                    <p class="text-xs text-slate-500">上周平均分</p>
                    <p class="mt-1 text-2xl font-semibold text-slate-400">
                      {{ overview.lastWeekAvgScore ?? '-' }}
                    </p>
                  </div>
                  <div v-if="weekDiff !== null" class="flex items-center gap-1 pb-1">
                    <span class="text-sm font-medium" :class="weekDiff >= 0 ? 'text-green-600' : 'text-red-500'">
                      {{ weekDiff >= 0 ? '+' : '' }}{{ weekDiff.toFixed(1) }}
                    </span>
                    <svg v-if="weekDiff >= 0" class="h-4 w-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 10l7-7m0 0l7 7m-7-7v18" />
                    </svg>
                    <svg v-else class="h-4 w-4 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
                    </svg>
                  </div>
                </div>
                <p class="mt-3 text-sm text-slate-500 dark:text-slate-400">
                  本周已完成 {{ overview.thisWeekInterviewCount ?? 0 }} 场面试。
                </p>
                <div v-if="overview.categoryChanges && overview.categoryChanges.length > 0" class="mt-5">
                  <p class="text-xs font-medium text-slate-500 mb-2">分类变化</p>
                  <div class="space-y-2">
                    <div
                      v-for="c in overview.categoryChanges.slice(0, 4)"
                      :key="c.categoryId"
                      class="flex items-center justify-between text-sm"
                    >
                      <span class="text-slate-600 dark:text-slate-300 truncate">{{ c.categoryName }}</span>
                      <div class="flex items-center gap-2">
                        <span class="text-xs text-slate-400">{{ c.lastWeekScore }}→{{ c.thisWeekScore }}</span>
                        <span class="text-xs font-medium" :class="c.change >= 0 ? 'text-green-600' : 'text-red-500'">
                          {{ c.change >= 0 ? '+' : '' }}{{ c.change.toFixed(1) }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
                <RouterLink to="/analytics" class="mt-4 inline-flex items-center gap-1 text-xs font-medium text-accent hover:underline">
                  查看详细分析
                  <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                  </svg>
                </RouterLink>
              </article>

              <!-- Ability Radar -->
              <article class="paper-panel p-4 sm:p-6">
                <p class="section-kicker">能力雷达</p>
                <h3 class="mt-3 text-lg font-semibold text-ink">各分类能力分布</h3>
                <div class="mt-4">
                  <div v-if="overview.categoryAbilities && overview.categoryAbilities.length > 0" class="space-y-3">
                    <div
                      v-for="cat in overview.categoryAbilities.slice(0, 6)"
                      :key="cat.categoryId"
                      class="flex items-center gap-3"
                    >
                      <span class="w-20 text-xs text-slate-500 truncate">{{ cat.categoryName }}</span>
                      <div class="flex-1 h-3 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                        <div
                          class="h-full rounded-full transition-all duration-500"
                          :class="cat.isWeak ? 'bg-red-400' : 'bg-accent'"
                          :style="{ width: `${Math.min(cat.abilityScore, 100)}%` }"
                        ></div>
                      </div>
                      <span class="w-10 text-right text-xs font-medium" :class="cat.isWeak ? 'text-red-500' : 'text-slate-700 dark:text-slate-200'">
                        {{ cat.abilityScore }}
                      </span>
                    </div>
                  </div>
                  <div v-else class="py-8 text-center text-sm text-slate-400">
                    完成面试后显示能力分布
                  </div>
                </div>
              </article>
            </section>

            <!-- Best study hours (hidden on mobile) -->
            <article class="paper-panel p-4 sm:p-6 hidden md:block">
              <p class="section-kicker">最佳学习时段</p>
              <h3 class="mt-3 text-lg font-semibold text-ink">你的高效时间段</h3>
              <div v-if="overview.bestStudyHours && overview.bestStudyHours.length > 0" class="mt-4 flex flex-wrap gap-4">
                <div
                  v-for="(h, idx) in overview.bestStudyHours"
                  :key="h.timeSlot"
                  class="flex items-center gap-3 rounded-lg border border-slate-200 dark:border-slate-700 px-4 py-3"
                >
                  <span
                    class="flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold"
                    :class="idx === 0 ? 'bg-yellow-100 text-yellow-700' : 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400'"
                  >
                    {{ idx + 1 }}
                  </span>
                  <span class="text-sm text-slate-600 dark:text-slate-300">{{ h.timeSlot }}</span>
                  <span class="text-xs text-slate-400">{{ h.sessionCount }}次</span>
                  <span class="text-sm font-medium text-ink">{{ h.avgScore }}分</span>
                </div>
              </div>
              <div v-else class="mt-4 py-6 text-center text-sm text-slate-400">
                完成更多面试后显示
              </div>
            </article>
          </div>
        </el-collapse-item>

        <!-- Data Trends -->
        <el-collapse-item name="trends" class="!border-0">
          <template #title>
            <div class="flex items-center gap-3 py-1">
              <div class="flex h-8 w-8 items-center justify-center rounded-full bg-accent/10 text-accent">
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                </svg>
              </div>
              <div>
                <span class="text-lg font-semibold text-ink">数据趋势</span>
                <span class="ml-2 text-sm text-slate-400">面试成绩趋势、复习热力图</span>
              </div>
            </div>
          </template>

          <div class="space-y-4 pt-2">
            <DashboardInterviewTrend :trend-data="trendData" :loading="trendLoading" />

            <DashboardReviewHeatmap
              :heatmap="reviewStats.heatmap"
              :streak="reviewStats.currentStreak"
              :today-pending="reviewStats.todayPending"
            />
          </div>
        </el-collapse-item>
      </el-collapse>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import DashboardGuideCard from './DashboardGuideCard.vue'
import DashboardMetrics from './DashboardMetrics.vue'
import DashboardInterviews from './DashboardInterviews.vue'
import DashboardWeakPoints from './DashboardWeakPoints.vue'
import DashboardInterviewTrend from './DashboardInterviewTrend.vue'
import DashboardReviewHeatmap from './DashboardReviewHeatmap.vue'
import { fetchDashboardOverviewApi } from '@/api/dashboard'
import { fetchInterviewTrendApi } from '@/api/interview'
import { fetchReviewStatsApi } from '@/api/review'
import { fetchRecommendInterviewApi } from '@/api/adaptive'
import type { DashboardOverview, InterviewHistoryItem, ReviewStats, RecommendInterview } from '@/types/api'
import { useAuthStore } from '@/stores/auth'
import { storage } from '@/utils/storage'

const authStore = useAuthStore()
const loading = ref(true)
const trendLoading = ref(true)
const trendData = ref<InterviewHistoryItem[]>([])
const reviewStats = ref<ReviewStats>({ totalReviews: 0, currentStreak: 0, todayPending: 0 })
const guideDismissed = ref(false)
const recommendInterview = ref<RecommendInterview | null>(null)
const activeCollapse = ref<string[]>([])
const overview = ref<DashboardOverview>({
  learningCount: 0,
  averageScore: 0,
  wrongCount: 0,
  planCompletionRate: 0,
  planHealthScore: 100,
  recentInterviews: [],
  weakPoints: [],
  firstVisit: true
})

const weekDiff = computed(() => {
  const tw = overview.value.thisWeekAvgScore
  const lw = overview.value.lastWeekAvgScore
  if (tw == null || lw == null) return null
  return Number(tw) - Number(lw)
})

const weekComparisonClass = computed(() => {
  if (weekDiff.value === null) return 'text-ink'
  return weekDiff.value >= 0 ? 'text-green-600' : 'text-red-500'
})

const metrics = computed(() => [
  { label: '学习总次数', value: String(overview.value.learningCount), desc: '问答与面试行为共同构成学习节奏。' },
  { label: '平均面试分', value: formatScore(overview.value.averageScore), desc: '面试作答记录聚合，无记录时为 0。' },
  { label: '错题数量', value: String(overview.value.wrongCount), desc: '低分题自动沉淀到错题本。' },
  { label: '计划完成率', value: `${overview.value.planCompletionRate}%`, desc: '当前计划已完成任务占比。' }
])

const quickActions = [
  { to: '/chat', label: '问答', kicker: 'Ask', desc: '先提一个问题，让系统开始积累你的知识盲点。' },
  { to: '/wrong', label: '错题本', kicker: 'Review', desc: '把低分答案转成可追踪的复习资产。' },
  { to: '/plan', label: '学习计划', kicker: 'Plan', desc: '根据真实弱点拆出一个可以执行的学习节奏。' },
  { to: '/knowledge', label: '知识库', kicker: 'KB', desc: '浏览和搜索内置学习资料。' }
]

const showGuideCard = computed(() => {
  const userId = authStore.user?.id
  if (!userId || guideDismissed.value || !overview.value.firstVisit) return false
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
    trendData.value = response.data || []
  } catch {
    // Silently fail — trend is supplementary
  } finally {
    trendLoading.value = false
  }
}

const loadReviewStats = async () => {
  try {
    const response = await fetchReviewStatsApi()
    reviewStats.value = response.data
  } catch {
    // Silently fail
  }
}

const loadRecommendInterview = async () => {
  try {
    const response = await fetchRecommendInterviewApi()
    recommendInterview.value = response.data
  } catch {
    // Silently fail
  }
}

const difficultyLabel = (d?: string) => {
  const map: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难' }
  return d ? map[d] || d : '-'
}

const difficultyClass = (d?: string) => {
  const map: Record<string, string> = {
    easy: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
    medium: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
    hard: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  }
  return d ? map[d] || '' : ''
}

const formatScore = (score: number): string => {
  return Number.isInteger(score) ? String(score) : score.toFixed(2)
}

onMounted(() => {
  void loadOverview()
  void loadTrend()
  void loadReviewStats()
  void loadRecommendInterview()
})
</script>
