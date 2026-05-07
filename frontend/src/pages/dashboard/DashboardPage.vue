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
              <p class="section-kicker">今日主任务</p>
              <span class="hard-chip">连续 {{ reviewStats.currentStreak }} 天</span>
              <span class="hard-chip">待复习 {{ reviewStats.todayPending }} 题</span>
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
              <RouterLink to="/analytics" class="hard-button-secondary"> 查看全局趋势 </RouterLink>
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
              <h3 class="mt-3 text-xl font-semibold text-ink">2 个关键信号</h3>
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
            <p class="section-kicker">下一步</p>
            <p class="mt-3 text-lg font-semibold text-ink">{{ primaryMission.cta }}</p>
            <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
              {{ primaryMission.supporting }}
            </p>
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
                {{ activeCollapse.includes('analysis') ? '收起学习分析' : '展开学习分析' }}
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
                <span class="text-lg font-semibold text-ink">学习分析</span>
                <span class="ml-2 text-sm text-slate-400">
                  {{
                    overview.recentInterviews?.length
                      ? `最近 ${overview.recentInterviews.length} 场练习`
                      : '暂无练习记录'
                  }}
                  · {{ overview.weakPoints?.length || 0 }} 个待补强知识点
                </span>
              </div>
            </div>
          </template>

          <div class="space-y-4 pt-2">
            <section class="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
              <DashboardInterviews :interviews="overview.recentInterviews" />
              <DashboardWeakPoints :weak-points="overview.weakPoints" />
            </section>

            <section class="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
              <article class="paper-panel p-4 sm:p-6">
                <p class="section-kicker">本周变化</p>
                <h3 class="mt-3 text-xl font-semibold tracking-[-0.03em] text-ink sm:text-2xl">本周表现</h3>
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
                    <svg
                      v-if="weekDiff >= 0"
                      class="h-4 w-4 text-green-600"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 10l7-7m0 0l7 7m-7-7v18" />
                    </svg>
                    <svg
                      v-else
                      class="h-4 w-4 text-red-500"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
                    </svg>
                  </div>
                </div>
                <p class="mt-3 text-sm text-slate-500 dark:text-slate-400">
                  本周已完成 {{ overview.thisWeekInterviewCount ?? 0 }} 场练习。
                </p>
                <div v-if="overview.categoryChanges && overview.categoryChanges.length > 0" class="mt-5">
                  <p class="mb-2 text-xs font-medium text-slate-500">变化最大的分类</p>
                  <div class="space-y-2">
                    <div
                      v-for="c in overview.categoryChanges.slice(0, 4)"
                      :key="c.categoryId"
                      class="flex items-center justify-between text-sm"
                    >
                      <span class="truncate text-slate-600 dark:text-slate-300">{{ c.categoryName }}</span>
                      <div class="flex items-center gap-2">
                        <span class="text-xs text-slate-400">{{ c.lastWeekScore }}→{{ c.thisWeekScore }}</span>
                        <span class="text-xs font-medium" :class="c.change >= 0 ? 'text-green-600' : 'text-red-500'">
                          {{ c.change >= 0 ? '+' : '' }}{{ c.change.toFixed(1) }}
                        </span>
                      </div>
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
                <p class="section-kicker">高效时间段</p>
                <h3 class="mt-3 text-lg font-semibold text-ink">你更适合在哪些时间练习</h3>
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
  {
    label: '累计练习',
    value: `${overview.value.learningCount} 次`,
    desc: overview.value.learningCount > 0 ? '这里会显示你的累计练习次数。' : '完成一次练习后会显示在这里。'
  },
  {
    label: '平均面试分',
    value: overview.value.averageScore > 0 ? formatScore(overview.value.averageScore) : '暂无',
    desc: overview.value.averageScore > 0 ? '按所有面试结果计算。' : '完成面试后开始统计。'
  },
  {
    label: '错题待处理',
    value: overview.value.wrongCount > 0 ? `${overview.value.wrongCount} 题` : '已清空',
    desc: overview.value.wrongCount > 0 ? '低分题可在错题复习中继续处理。' : '当前没有待处理题目。'
  }
])

const primaryMission = computed(() => {
  if (reviewStats.value.todayPending > 0) {
    return {
      to: '/review',
      title: `先完成今天的 ${reviewStats.value.todayPending} 道复习题`,
      description: `这些题今天需要复习，完成后可继续问答或面试。`,
      supporting: '完成后可继续练习或追问薄弱点。',
      cta: '开始复习',
      urgent: true
    }
  }

  if (recommendInterview.value?.direction) {
    return {
      to: '/interview',
      title: `开始一场 ${recommendInterview.value.direction} 面试`,
      description: recommendInterview.value.reason || '系统根据最近表现，给出了当前最值得练的方向。',
      supporting: '完成后可查看错题复习安排。',
      cta: '开始面试',
      urgent: false
    }
  }

  return {
    to: '/chat',
    title: '先从一个 Java 问题开始',
    description: '如果今天还没开始，可以先提一个问题热身。',
    supporting: '从最近最容易卡住的知识点开始。',
    cta: '去提问',
    urgent: false
  }
})

const todaySignals = computed(() => [
  {
    label: '今日待复习',
    value: `${reviewStats.value.todayPending} 题`,
    detail: reviewStats.value.todayPending > 0 ? '今天有待处理的复习题目。' : '今天的复习任务已完成。',
    action: reviewStats.value.todayPending > 0 ? '去复习' : '查看记录',
    to: '/review',
    dotClass:
      reviewStats.value.todayPending > 0 ? 'bg-amber shadow-[0_0_16px_rgba(var(--bc-accent-rgb),0.5)]' : 'bg-lime'
  },
  {
    label: '连续学习',
    value: `${reviewStats.value.currentStreak} 天`,
    detail: reviewStats.value.currentStreak > 0 ? '保持连续节奏，比突击更有用。' : '今天开始后就会重新累计。',
    action: '查看错题复习',
    to: '/review',
    dotClass: reviewStats.value.currentStreak > 0 ? 'bg-cyan' : 'bg-slate-400'
  }
])

const quickActions = computed(() => [
  { to: '/chat', label: '智能问答', title: '提一个问题', badge: '提问', desc: '适合热身或快速定位知识盲点。' },
  {
    to: '/interview',
    label: '模拟面试',
    title: recommendInterview.value?.direction ? `练习 ${recommendInterview.value.direction}` : '开始一场模拟面试',
    badge: '练习',
    desc: '用限时问答验证你是否真的理解。'
  },
  {
    to: '/review?tab=all',
    label: '错题复习',
    title: overview.value.wrongCount > 0 ? `处理 ${overview.value.wrongCount} 道错题` : '查看错题与复习记录',
    badge: '修复',
    desc: '处理低分题，减少同类错误重复出现。'
  },
  {
    to: '/knowledge',
    label: '知识库',
    title: '上传或查找资料',
    badge: '资料',
    desc: '管理你的文档，并为问答提供可引用内容。'
  }
])

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
