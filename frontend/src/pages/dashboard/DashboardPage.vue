<template>
  <div class="dashboard-canvas space-y-6 pb-6">
    <DashboardGuideCard v-if="showGuideCard" :actions="quickActions" @dismiss="dismissGuide" />

    <section v-if="loading" class="grid gap-4 xl:grid-cols-[minmax(0,1.35fr)_minmax(320px,0.9fr)]">
      <article class="paper-panel min-h-[320px] p-6 sm:p-8">
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
      </article>
      <article class="cockpit-panel p-6 sm:p-7">
        <div class="h-4 w-20 animate-pulse rounded-full bg-slate-200/80 dark:bg-slate-700"></div>
        <div class="mt-4 space-y-3">
          <div
            v-for="index in 3"
            :key="index"
            class="h-16 animate-pulse rounded-[var(--radius-md)] bg-slate-100/90 dark:bg-slate-800"
          ></div>
        </div>
      </article>
    </section>

    <template v-else>
      <section class="grid gap-4 xl:grid-cols-[minmax(0,1.35fr)_minmax(320px,0.9fr)]">
        <article
          class="paper-panel relative overflow-hidden p-6 sm:p-8"
          :class="{ 'border-[var(--bc-line-hot)]': primaryMission.urgent }"
        >
          <div
            class="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(var(--bc-accent-rgb),0.16),transparent_34%),radial-gradient(circle_at_92%_12%,rgba(85,214,190,0.12),transparent_28%)]"
          ></div>
          <div class="relative flex h-full flex-col gap-8">
            <div class="flex flex-wrap items-center gap-2">
              <p class="section-kicker">今日记忆任务</p>
              <span class="hard-chip">连续 {{ reviewStats.currentStreak }} 天</span>
              <span class="hard-chip">待复习 {{ todayReviewCards }} 张</span>
            </div>

            <div class="max-w-3xl">
              <h3 class="font-display text-4xl font-semibold leading-[1.02] tracking-[-0.04em] text-ink sm:text-5xl">
                {{ primaryMission.title }}
              </h3>
              <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-300 sm:text-base">
                {{ primaryMission.description }}
              </p>
              <p class="mt-3 text-xs text-slate-400 dark:text-slate-500">
                {{ primaryMission.supporting }}
              </p>
            </div>

            <div class="flex flex-wrap items-center gap-3">
              <RouterLink :to="primaryMission.to" class="hard-button-primary">
                {{ primaryMission.cta }}
              </RouterLink>
              <RouterLink to="/review" class="hard-button-secondary"> 查看复习中心 </RouterLink>
            </div>

            <div class="grid gap-3 sm:grid-cols-3">
              <article
                v-for="signal in todaySignals"
                :key="signal.label"
                class="rounded-[var(--radius-md)] border border-[var(--bc-line)] bg-white/55 p-4 backdrop-blur-sm dark:bg-white/5"
              >
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0">
                    <div class="flex items-center gap-2">
                      <span class="h-2.5 w-2.5 rounded-full" :class="signal.dotClass"></span>
                      <span class="text-sm font-semibold text-ink">{{ signal.label }}</span>
                    </div>
                    <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">
                      {{ signal.detail }}
                    </p>
                  </div>
                  <div class="shrink-0 text-right">
                    <p class="font-mono text-lg font-semibold text-ink">{{ signal.value }}</p>
                    <RouterLink :to="signal.to" class="text-xs font-medium text-accent hover:underline">
                      {{ signal.action }}
                    </RouterLink>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </article>

        <aside class="cockpit-panel p-5 sm:p-6">
          <div class="flex items-start justify-between gap-3">
            <div>
              <p class="section-kicker">今天先看</p>
              <h3 class="mt-3 text-xl font-semibold text-ink">4 个关键信号</h3>
            </div>
            <RouterLink to="/analytics" class="text-xs font-semibold text-accent hover:underline">
              详细分析
            </RouterLink>
          </div>
          <div class="mt-4 space-y-3">
            <article
              v-for="signal in todaySignals"
              :key="signal.label"
              class="rounded-2xl border border-[var(--bc-line)] bg-white/35 px-4 py-3 dark:bg-white/5"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <div class="flex items-center gap-2">
                    <span class="h-2.5 w-2.5 rounded-full" :class="signal.dotClass"></span>
                    <span class="text-sm font-semibold text-ink">{{ signal.label }}</span>
                  </div>
                  <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">
                    {{ signal.detail }}
                  </p>
                </div>
                <div class="shrink-0 text-right">
                  <p class="font-mono text-lg font-semibold text-ink">{{ signal.value }}</p>
                  <RouterLink :to="signal.to" class="text-xs font-medium text-accent hover:underline">
                    {{ signal.action }}
                  </RouterLink>
                </div>
              </div>
            </article>
          </div>

          <div class="mt-4 rounded-[var(--radius-md)] border border-[var(--bc-line)] bg-white/45 p-4 dark:bg-white/5">
            <p class="section-kicker">辅助建议</p>
            <p class="mt-3 text-lg font-semibold text-ink">{{ supportingMission.title }}</p>
            <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
              {{ supportingMission.desc }}
            </p>
            <RouterLink :to="supportingMission.to" class="mt-3 inline-flex text-sm font-semibold text-accent hover:underline">
              {{ supportingMission.action }}
            </RouterLink>
          </div>
        </aside>
      </section>

      <DashboardMetrics :metrics="metrics" />

      <section class="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <RouterLink v-for="action in quickActions" :key="action.to" :to="action.to" class="mission-card block p-5">
          <div class="flex items-center justify-between gap-3">
            <p class="text-sm font-semibold text-ink">{{ action.label }}</p>
            <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ action.badge }}</span>
          </div>
          <h3 class="mt-4 text-lg font-semibold tracking-[-0.02em] text-ink">{{ action.title }}</h3>
          <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">{{ action.desc }}</p>
        </RouterLink>
      </section>

      <section
        v-if="overview.weakCategories && overview.weakCategories.length > 0"
        class="paper-panel flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between"
      >
        <div class="min-w-0">
          <p class="section-kicker">当前薄弱分类</p>
          <div class="mt-2 flex flex-wrap gap-2">
            <span
              v-for="cat in overview.weakCategories"
              :key="cat"
              class="rounded-full bg-red-100 px-2.5 py-1 text-xs font-medium text-red-700 dark:bg-red-900/30 dark:text-red-400"
            >
              {{ cat }}
            </span>
          </div>
        </div>
        <RouterLink to="/analytics" class="text-sm font-semibold text-accent hover:underline">
          去看原因与建议
        </RouterLink>
      </section>

      <section class="grid gap-4 xl:grid-cols-[minmax(0,1.08fr)_minmax(0,0.92fr)]">
        <article class="paper-panel p-4 sm:p-6">
          <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <p class="section-kicker">更多详情</p>
              <h3 class="mt-2 text-lg font-semibold text-ink">查看更多学习记录</h3>
              <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">展开后可查看复盘、趋势和历史记录。</p>
            </div>
            <div class="flex flex-wrap gap-2">
              <button
                type="button"
                class="hard-button-secondary !min-h-10 !px-4 text-sm"
                @click="toggleSection('analysis')"
              >
                {{ activeCollapse.includes('analysis') ? '收起进阶分析' : '展开进阶分析' }}
              </button>
              <button
                type="button"
                class="hard-button-secondary !min-h-10 !px-4 text-sm"
                @click="toggleSection('trends')"
              >
                {{ activeCollapse.includes('trends') ? '收起趋势记录' : '展开趋势记录' }}
              </button>
            </div>
          </div>
        </article>

        <article class="paper-panel p-4 sm:p-6">
          <p class="section-kicker">能力分布</p>
          <h3 class="mt-3 text-lg font-semibold text-ink">各分类当前水平</h3>
          <div class="mt-4">
            <div v-if="overview.categoryAbilities && overview.categoryAbilities.length > 0" class="space-y-3">
              <div
                v-for="cat in overview.categoryAbilities.slice(0, 6)"
                :key="cat.categoryId"
                class="flex items-center gap-3"
              >
                <span class="w-20 truncate text-xs text-slate-500">{{ cat.categoryName }}</span>
                <div class="h-3 flex-1 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
                  <div
                    class="h-full rounded-full transition-all duration-500"
                    :class="cat.isWeak ? 'bg-red-400' : 'bg-accent'"
                    :style="{ width: `${Math.min(cat.abilityScore, 100)}%` }"
                  ></div>
                </div>
                <span
                  class="w-10 text-right text-xs font-medium"
                  :class="cat.isWeak ? 'text-red-500' : 'text-slate-700 dark:text-slate-200'"
                >
                  {{ cat.abilityScore }}
                </span>
              </div>
            </div>
            <div v-else class="py-8 text-center text-sm text-slate-400">完成面试后显示能力分布</div>
          </div>
        </article>
      </section>

      <el-collapse v-model="activeCollapse" class="space-y-3">
        <el-collapse-item name="analysis" class="!border-0">
          <template #title>
            <div class="flex items-center gap-3 py-1">
              <div class="flex h-8 w-8 items-center justify-center rounded-full bg-accent/10 text-accent">
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
                  />
                </svg>
              </div>
              <div>
                <span class="text-lg font-semibold text-ink">进阶分析</span>
                <span class="ml-2 text-sm text-slate-400">
                  {{
                    overview.recentInterviews?.length
                      ? `最近 ${overview.recentInterviews.length} 场诊断`
                      : '暂无诊断记录'
                  }}
                  · {{ overview.categoryMasterySummary?.length || 0 }} 个分类已形成掌握度
                </span>
              </div>
            </div>
          </template>

          <div class="space-y-4 pt-2">
            <section class="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
              <DashboardInterviews :interviews="overview.recentInterviews" />
              <DashboardWeakPoints :items="overview.categoryMasterySummary || []" />
            </section>

            <section class="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
              <article class="paper-panel p-4 sm:p-6">
                <p class="section-kicker">记忆成长摘要</p>
                <h3 class="mt-3 text-xl font-semibold tracking-[-0.03em] text-ink sm:text-2xl">最近的记忆推进状态</h3>
                <div class="mt-4 grid gap-4 sm:grid-cols-2">
                  <div>
                    <p class="text-xs text-slate-500">今日完成状态</p>
                    <p class="mt-1 text-xl font-semibold text-ink">
                      {{ overview.todayCompletionStatus || '等待今日任务开始' }}
                    </p>
                  </div>
                  <div>
                    <p class="text-xs text-slate-500">连续学习</p>
                    <p class="mt-1 text-3xl font-bold text-ink">
                      {{ overview.studyStreak ?? reviewStats.currentStreak ?? 0 }} 天
                    </p>
                  </div>
                </div>
                <p class="mt-3 text-sm text-slate-500 dark:text-slate-400">
                  今天优先看完成率、连续天数和分类掌握度，面试平均分留作辅助参考。
                </p>
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
                <RouterLink
                  to="/analytics"
                  class="mt-4 inline-flex items-center gap-1 text-xs font-medium text-accent hover:underline"
                >
                  查看详细分析
                  <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                  </svg>
                </RouterLink>
              </article>

              <article class="paper-panel p-4 sm:p-6">
                <p class="section-kicker">辅助面试数据</p>
                <h3 class="mt-3 text-lg font-semibold text-ink">面试诊断作为辅助参考</h3>
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
                </div>
                <p class="mt-3 text-sm text-slate-500 dark:text-slate-400">
                  本周已完成 {{ overview.thisWeekInterviewCount ?? 0 }} 场诊断，面试表现用于辅助判断是否需要补充训练。
                </p>
                <div
                  v-if="overview.bestStudyHours && overview.bestStudyHours.length > 0"
                  class="mt-4 flex flex-wrap gap-4"
                >
                  <div
                    v-for="(h, idx) in overview.bestStudyHours"
                    :key="h.timeSlot"
                    class="flex items-center gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-700"
                  >
                    <span
                      class="flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold"
                      :class="
                        idx === 0
                          ? 'bg-yellow-100 text-yellow-700'
                          : 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400'
                      "
                    >
                      {{ idx + 1 }}
                    </span>
                    <span class="text-sm text-slate-600 dark:text-slate-300">{{ h.timeSlot }}</span>
                    <span class="text-xs text-slate-400">{{ h.sessionCount }} 次</span>
                    <span class="text-sm font-medium text-ink">{{ h.avgScore }} 分</span>
                  </div>
                </div>
                <div v-else class="mt-4 py-6 text-center text-sm text-slate-400">完成更多面试后显示</div>
              </article>
            </section>
          </div>
        </el-collapse-item>

        <el-collapse-item name="trends" class="!border-0">
          <template #title>
            <div class="flex items-center gap-3 py-1">
              <div class="flex h-8 w-8 items-center justify-center rounded-full bg-accent/10 text-accent">
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                </svg>
              </div>
              <div>
                <span class="text-lg font-semibold text-ink">趋势记录</span>
                <span class="ml-2 text-sm text-slate-400">
                  {{ trendData.length }} 次练习记录 · 连续 {{ reviewStats.currentStreak }} 天复习
                </span>
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
import type { DashboardOverview, ReviewStats, RecommendInterview } from '@/types/api'
import { useAuthStore } from '@/stores/auth'
import { storage } from '@/utils/storage'

const authStore = useAuthStore()
const loading = ref(true)
const trendLoading = ref(true)
const trendData = ref<Array<{ sessionId: string; direction: string; totalScore: number; startTime?: string }>>([])
const reviewStats = ref<ReviewStats>({ totalReviews: 0, currentStreak: 0, todayPending: 0 })
const guideDismissed = ref(false)
const recommendInterview = ref<RecommendInterview | null>(null)
const activeCollapse = ref<string[]>([])
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
const todayCompletedCards = computed(() => overview.value.todayCompletedCards ?? 0)
const todayCardCompletionRate = computed(() => overview.value.todayCardCompletionRate ?? 0)
const masteredCardCount = computed(() => overview.value.masteredCardCount ?? 0)
const reviewDebtCount = computed(() => overview.value.reviewDebtCount ?? 0)
const todayCardTotal = computed(() => todayLearnCards.value + todayReviewCards.value)

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
  {
    label: '今日卡片',
    value: todayCardTotal.value > 0 ? `${todayCardTotal.value} 张` : '待开始',
    desc: todayCardTotal.value > 0 ? `今日待学 ${todayLearnCards.value} 张，待复习 ${todayReviewCards.value} 张。` : '先从知识库生成一轮卡片任务。'
  },
  {
    label: '今日完成率',
    value: `${formatPercent(todayCardCompletionRate.value)}%`,
    desc: todayCardTotal.value > 0 ? `今天已完成 ${todayCompletedCards.value} / ${todayCardTotal.value} 张卡片。` : '今天还没有需要处理的卡片。'
  },
  {
    label: '连续天数',
    value: `${overview.value.studyStreak ?? reviewStats.value.currentStreak} 天`,
    desc: (overview.value.studyStreak ?? reviewStats.value.currentStreak) > 0 ? '保持连续推进，比突击更容易形成长期记忆。' : '今天开始后就会重新累计。'
  },
  {
    label: '已掌握卡片',
    value: `${masteredCardCount.value} 张`,
    desc: masteredCardCount.value > 0 ? '已经进入 mastered 状态的卡片总量。' : '持续评分推进后会在这里累计。'
  },
  {
    label: '复习负债',
    value: reviewDebtCount.value > 0 ? `${reviewDebtCount.value} 项` : '无积压',
    desc: reviewDebtCount.value > 0 ? '当前存在逾期未处理的复习项，建议先清掉。' : '当前没有逾期复习积压。'
  }
])

const primaryMission = computed(() => {
  if (todayCardTotal.value > 0) {
    return {
      to: '/cards',
      title: '今天先完成你的记忆任务',
      description: `今天共有 ${todayCardTotal.value} 张卡片需要处理，其中待学 ${todayLearnCards.value} 张、待复习 ${todayReviewCards.value} 张。`,
      supporting: `当前完成率 ${formatPercent(todayCardCompletionRate.value)}%，先把今天的卡片做完，再进入其他模块。`,
      cta: '开始今日卡片',
      urgent: todayReviewCards.value > 0 || reviewDebtCount.value > 0
    }
  }

  return {
    to: '/cards',
    title: '今天先完成你的记忆任务',
    description: '你还没有正在推进的卡片任务。先从知识库生成一轮卡片，再按天开始记忆。',
    supporting: '首页会在你开始卡片学习后，持续追踪待学、待复习和完成率。',
    cta: '开始今日卡片',
    urgent: false
  }
})

const todaySignals = computed(() => [
  {
    label: '今日待学',
    value: `${todayLearnCards.value} 张`,
    detail: todayLearnCards.value > 0 ? '这些是今天首次进入学习流程的新卡片。' : '当前没有新的待学卡片。',
    action: '去做卡片',
    to: '/cards',
    dotClass:
      todayLearnCards.value > 0 ? 'bg-cyan shadow-[0_0_16px_rgba(var(--bc-accent-rgb),0.5)]' : 'bg-lime'
  },
  {
    label: '今日待复习',
    value: `${todayReviewCards.value} 张`,
    detail: todayReviewCards.value > 0 ? '这些卡片已经到期，今天优先把它们清掉。' : '今天没有到期的卡片复习任务。',
    action: '去复习',
    to: '/cards',
    dotClass: todayReviewCards.value > 0 ? 'bg-amber shadow-[0_0_16px_rgba(var(--bc-accent-rgb),0.5)]' : 'bg-lime'
  },
  {
    label: '连续学习天数',
    value: `${overview.value.studyStreak ?? reviewStats.value.currentStreak} 天`,
    detail: (overview.value.studyStreak ?? reviewStats.value.currentStreak) > 0 ? '保持连续节奏，比突击更容易留下长期记忆。' : '今天开始后就会重新累计。',
    action: '查看复习中心',
    to: '/review',
    dotClass: (overview.value.studyStreak ?? reviewStats.value.currentStreak) > 0 ? 'bg-cyan' : 'bg-slate-400'
  },
  {
    label: '今日完成率',
    value: `${formatPercent(todayCardCompletionRate.value)}%`,
    detail: todayCardTotal.value > 0 ? `今天已经完成 ${todayCompletedCards.value} 张卡片。` : '开始卡片任务后会在这里持续更新。',
    action: '查看进度',
    to: '/cards',
    dotClass: todayCardCompletionRate.value >= 100 ? 'bg-lime' : 'bg-accent'
  }
])

const quickActions = computed(() => [
  {
    to: '/cards',
    label: '今日卡片',
    title: todayCardTotal.value > 0 ? `处理今天的 ${todayCardTotal.value} 张卡片` : '开始一轮卡片任务',
    badge: '主线',
    desc: '先完成今天的记忆任务，再进入其他辅助模块。'
  },
  {
    to: '/review?tab=all',
    label: '复习中心',
    title: reviewDebtCount.value > 0 ? `清理 ${reviewDebtCount.value} 项复习积压` : '查看到期复习与错题记录',
    badge: '复习',
    desc: '处理到期复习，减少积压项继续扩散。'
  },
  {
    to: '/knowledge',
    label: '知识库',
    title: '上传或查找资料',
    badge: '资料',
    desc: '卡片和问答都从这里拿到可靠的内容来源。'
  },
  {
    to: '/chat',
    label: '问答',
    title: '带着资料上下文提问',
    badge: '辅助',
    desc: '适合在卡片学习后，快速澄清还没吃透的问题。'
  },
  {
    to: '/interview',
    label: '面试诊断',
    title: recommendInterview.value?.direction ? `诊断 ${recommendInterview.value.direction}` : '做一次进阶诊断',
    badge: '进阶',
    desc: '用限时问答验证你是否真的理解。'
  }
])

const supportingMission = computed(() => {
  if (recommendInterview.value?.direction) {
    return {
      title: `之后可以做一次 ${recommendInterview.value.direction} 面试诊断`,
      desc: recommendInterview.value.reason || '系统根据最近表现，给出了当前更值得验证的方向。',
      action: '去做诊断',
      to: '/interview'
    }
  }

  return {
    title: '卡片后用问答快速补盲',
    desc: '如果卡片中有没完全理解的概念，可以立刻带着资料上下文发问。',
    action: '去问答',
    to: '/chat'
  }
})

const toggleSection = (name: string) => {
  activeCollapse.value = activeCollapse.value.includes(name)
    ? activeCollapse.value.filter((item) => item !== name)
    : [...activeCollapse.value, name]
}

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
    trendData.value = (response.data || []).map((item) => ({
      sessionId: item.sessionId,
      direction: item.direction,
      totalScore: item.totalScore,
      startTime: item.startTime
    }))
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

const formatPercent = (value: number): string => {
  return Number.isInteger(value) ? String(value) : value.toFixed(0)
}

onMounted(() => {
  void loadOverview()
  void loadTrend()
  void loadReviewStats()
  void loadRecommendInterview()
})
</script>
