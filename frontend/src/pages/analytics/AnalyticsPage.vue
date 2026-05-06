<template>
  <div class="analytics-cockpit space-y-6">
    <section class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div class="min-w-0 max-w-3xl">
          <div class="flex items-center gap-3">
            <span class="state-pulse" aria-hidden="true"></span>
            <p class="section-kicker">学习分析</p>
          </div>
          <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">先看结论，再决定下一步怎么学</h2>
          <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
            这一页先告诉你最近是在进步、遗忘率是否偏高、当前最该处理哪类题，再展开图表细看趋势。
          </p>
        </div>

        <div class="mode-switch grid grid-cols-3 gap-2">
          <button
            v-for="w in weekOptions"
            :key="w.value"
            type="button"
            class="mode-switch__item"
            :class="{ 'mode-switch__item-active': selectedWeeks === w.value }"
            @click="changeWeeks(w.value)"
          >
            {{ w.label }}
          </button>
        </div>
      </div>

      <div class="mt-6 grid gap-3 md:grid-cols-3">
        <article v-for="insight in headlineInsights" :key="insight.label" class="insight-card" :class="insight.toneClass">
          <div class="flex items-start justify-between gap-3">
            <div>
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ insight.label }}</p>
              <p class="mt-3 text-xl font-semibold text-ink">{{ insight.title }}</p>
            </div>
            <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ insight.badge }}</span>
          </div>
          <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">{{ insight.detail }}</p>
          <p class="mt-4 text-xs font-semibold uppercase tracking-[0.18em]" :class="insight.ctaClass">{{ insight.cta }}</p>
        </article>
      </div>

      <div class="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <article v-for="signal in summarySignals" :key="signal.label" class="data-slab p-4" :class="signal.toneClass">
          <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ signal.label }}</p>
          <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ signal.value }}</p>
          <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ signal.detail }}</p>
        </article>
      </div>

      <div class="mt-5 space-y-3">
        <article v-for="lane in signalLanes" :key="lane.label" class="signal-lane">
          <div class="flex items-center justify-between gap-3">
            <div class="flex items-center gap-3">
              <span class="inline-flex h-2.5 w-2.5 rounded-full" :class="lane.dotClass"></span>
              <div>
                <p class="text-sm font-semibold text-ink">{{ lane.label }}</p>
                <p class="text-xs text-slate-500 dark:text-slate-400">{{ lane.detail }}</p>
              </div>
            </div>
            <span class="font-mono text-sm font-semibold text-ink">{{ lane.value }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div class="min-w-0">
          <p class="section-kicker">能力趋势</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">最近几周的面试表现变化</h3>
          <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-300">
            先看整体分数变化，再按分类筛选。哪条线掉得最明显，哪类问题就该优先补。
          </p>
        </div>
      </div>

      <div v-if="trendLoading" class="mt-5 flex h-[380px] items-center justify-center">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      </div>
      <div v-else-if="!trendData.overallTrend?.length" class="mt-5 flex h-[380px] items-center justify-center">
        <EmptyState icon="chart" title="暂无面试数据" description="完成面试后，能力地形图会在这里生成。" compact />
      </div>
      <div v-else class="mt-5">
        <div v-if="trendData.categoryTrends?.length" class="mb-4 flex flex-wrap gap-2">
          <button
            type="button"
            class="category-chip"
            :class="{ 'category-chip-active': selectedCategories.length === 0 }"
            @click="selectedCategories = []"
          >
            全部分类
          </button>
          <button
            v-for="cat in trendData.categoryTrends"
            :key="cat.categoryId"
            type="button"
            class="category-chip"
            :class="{ 'category-chip-active': selectedCategories.includes(cat.categoryId) }"
            @click="toggleCategory(cat.categoryId)"
          >
            {{ cat.categoryName }}
          </button>
        </div>
        <div ref="trendChartRef" class="chart-shell h-[380px] w-full"></div>
      </div>
    </section>

    <section class="grid gap-4 xl:grid-cols-2">
      <article class="cockpit-panel p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="section-kicker">记忆强度</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">复习后的稳定程度</h3>
          </div>
          <span class="detail-pill">2.5 初始值 / 1.3 风险线</span>
        </div>
        <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
          EF 越高代表题目越稳。2.5 是初始值，低于 1.3 往往意味着你反复忘记或评分过低，需要尽快回到复习链路。
        </p>

        <div v-if="efficiencyLoading" class="mt-5 flex h-[300px] items-center justify-center">
          <div class="h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
        </div>
        <div v-else-if="!efficiencyData.efTrend?.length" class="mt-5 flex h-[300px] items-center justify-center">
          <EmptyState icon="review" title="暂无复习数据" description="完成复习后，EF 强度曲线会出现在这里。" compact />
        </div>
        <div v-else class="mt-5">
          <div class="grid gap-3 sm:grid-cols-3">
            <article class="data-slab p-4">
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">平均 EF</p>
              <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ efficiencyData.avgEaseFactor }}</p>
            </article>
            <article class="data-slab border-l-[var(--bc-cyan)] p-4">
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">复习次数</p>
              <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ efficiencyData.totalReviews }}</p>
            </article>
            <article class="data-slab border-l-[var(--bc-lime)] p-4">
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">连续天数</p>
              <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ efficiencyData.currentStreak }} 天</p>
            </article>
          </div>
          <div ref="efChartRef" class="chart-shell mt-4 h-[280px] w-full"></div>
        </div>
      </article>

      <article class="cockpit-panel p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="section-kicker">遗忘率</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">哪一段最容易忘</h3>
          </div>
          <span class="detail-pill">重来次数 / 总评分</span>
        </div>
        <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
          遗忘率越低越好。这里同时展示每周遗忘率和评分分布，帮助判断是整体复习不足，还是只在某个区间持续掉分。
        </p>

        <div v-if="efficiencyLoading" class="mt-5 flex h-[300px] items-center justify-center">
          <div class="h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
        </div>
        <div v-else-if="!efficiencyData.forgettingRateTrend?.length" class="mt-5 flex h-[300px] items-center justify-center">
          <EmptyState icon="review" title="暂无复习数据" description="完成复习后，遗忘率趋势和评分分布会出现在这里。" compact />
        </div>
        <div v-else class="mt-5">
          <div class="flex flex-wrap gap-2">
            <div v-for="(label, key) in ratingLabels" :key="key" class="rating-chip">
              <span class="h-2.5 w-2.5 rounded-full" :class="ratingColor(Number(key))"></span>
              <span>{{ label }}</span>
              <span class="font-mono text-ink">{{ efficiencyData.ratingDistribution?.[key] ?? 0 }}</span>
            </div>
          </div>
          <div ref="frChartRef" class="chart-shell mt-4 h-[280px] w-full"></div>
        </div>
      </article>
    </section>

    <section v-if="!efficiencyLoading && hasMasteryData" class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="section-kicker">掌握分布</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">掌握分布</h3>
          <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
            不再用 emoji，而是直接展示每种掌握状态的数量和占比，快速判断错题池当前更接近“待启动”还是“已稳定”。
          </p>
        </div>
        <span class="detail-pill">{{ totalMasteryCount }} 道题</span>
      </div>

      <div class="mt-6 grid gap-4 lg:grid-cols-[minmax(0,1fr)_340px]">
        <div class="space-y-4">
          <article
            v-for="item in masteryItems"
            :key="item.label"
            class="mastery-card"
            :class="item.toneClass"
          >
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="text-lg font-semibold text-ink">{{ item.label }}</p>
                <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">{{ item.description }}</p>
              </div>
              <div class="text-right">
                <p class="font-mono text-3xl font-semibold text-ink">{{ item.count }}</p>
                <p class="text-xs font-semibold uppercase tracking-[0.18em]" :class="item.textClass">{{ item.percent }}%</p>
              </div>
            </div>
            <div class="mastery-track mt-4">
              <span class="mastery-track__fill" :class="item.fillClass" :style="{ width: `${item.percent}%` }"></span>
            </div>
          </article>
        </div>

        <aside class="cockpit-panel p-5">
          <p class="section-kicker">阅读建议</p>
          <h4 class="mt-3 text-xl font-semibold text-ink">如何读这张图</h4>
          <div class="mt-4 space-y-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
            <p>“未开始”高：说明错题池堆积，需要先清理入口和安排首轮复习。</p>
            <p>“复习中”高：当前最需要维持节奏，避免集中回落到“重来”。</p>
            <p>“已掌握”高：说明复习体系有效，可以把精力转移到新方向或更高难度面试。</p>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { fetchAbilityTrendApi, fetchEfficiencyApi } from '@/api/analytics'
import type { AbilityTrend, EfficiencyData } from '@/types/api'

const weekOptions = [
  { label: '4 周', value: 4 },
  { label: '8 周', value: 8 },
  { label: '12 周', value: 12 },
]

const selectedWeeks = ref(12)
const selectedCategories = ref<number[]>([])
const trendLoading = ref(true)
const efficiencyLoading = ref(true)
const trendData = ref<AbilityTrend>({ weeks: [], overallTrend: [], categoryTrends: [] })
const efficiencyData = ref<EfficiencyData>({
  avgEaseFactor: 2.5,
  efTrend: [],
  ratingDistribution: {},
  forgettingRateTrend: [],
  masteryDistribution: {},
  totalReviews: 0,
  currentStreak: 0,
})

const trendChartRef = ref<HTMLElement | null>(null)
const efChartRef = ref<HTMLElement | null>(null)
const frChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null
let efChart: echarts.ECharts | null = null
let frChart: echarts.ECharts | null = null

const ratingLabels: Record<number, string> = { 1: '重来', 2: '困难', 3: '良好', 4: '轻松' }
const chartColors = ['#ffb74d', '#55d6be', '#ff6b6b', '#9fe870', '#76b4ff', '#f59e0b', '#22c55e']

const ratingColor = (key: number) => {
  const map: Record<number, string> = {
    1: 'bg-[var(--bc-coral)]',
    2: 'bg-[var(--bc-amber)]',
    3: 'bg-[var(--bc-cyan)]',
    4: 'bg-[var(--bc-lime)]',
  }
  return map[key] || 'bg-slate-400'
}

const getChartPoint = <T extends { week: string }>(items: T[], dataIndex: number) => items[dataIndex] ?? null

const hasMasteryData = computed(() => {
  const d = efficiencyData.value.masteryDistribution
  return d && Object.values(d).some((v) => v > 0)
})

const totalMasteryCount = computed(() => {
  const d = efficiencyData.value.masteryDistribution
  return Object.values(d || {}).reduce((sum, value) => sum + value, 0)
})

const weeklyScoreDelta = computed(() => {
  const overallTrend = trendData.value.overallTrend
  if (overallTrend.length < 2) return null
  return +(overallTrend[overallTrend.length - 1]!.score - overallTrend[0]!.score).toFixed(1)
})

const latestForgettingRate = computed(() => {
  const data = efficiencyData.value.forgettingRateTrend
  if (!data.length) return null
  return +(data[data.length - 1]!.forgettingRate * 100).toFixed(1)
})

const weakestMastery = computed(() => {
  const dist = efficiencyData.value.masteryDistribution || {}
  const entries = [
    { key: 'not_started', label: '未开始', value: dist.not_started ?? 0 },
    { key: 'reviewing', label: '复习中', value: dist.reviewing ?? 0 },
    { key: 'mastered', label: '已掌握', value: dist.mastered ?? 0 },
  ]
  return entries.sort((a, b) => b.value - a.value)[0] ?? null
})

const headlineInsights = computed(() => [
  {
    label: '能力变化',
    title: weeklyScoreDelta.value == null ? '等待数据' : `${weeklyScoreDelta.value >= 0 ? '+' : ''}${weeklyScoreDelta.value} 分`,
    detail: weeklyScoreDelta.value == null
      ? '完成更多面试后，这里会显示当前窗口内的能力变化。'
      : weeklyScoreDelta.value >= 0
        ? '本窗口内综合得分在上升，可以继续沿当前训练方向推进。'
        : '综合得分出现回落，建议先回看近几周掉分最大的分类。',
    badge: weeklyScoreDelta.value == null ? '待生成' : weeklyScoreDelta.value >= 0 ? '上升' : '风险',
    cta: weeklyScoreDelta.value == null ? '先完成更多面试' : weeklyScoreDelta.value >= 0 ? '继续强化当前方向' : '优先修复掉分分类',
    ctaClass: weeklyScoreDelta.value == null ? 'text-slate-400' : weeklyScoreDelta.value >= 0 ? 'text-[var(--bc-lime)]' : 'text-[var(--bc-coral)]',
    toneClass: weeklyScoreDelta.value != null && weeklyScoreDelta.value < 0 ? 'insight-card-risk' : '',
  },
  {
    label: '遗忘率',
    title: latestForgettingRate.value == null ? '等待数据' : `${latestForgettingRate.value}%`,
    detail: latestForgettingRate.value == null
      ? '完成复习后，这里会生成最新遗忘率。'
      : latestForgettingRate.value <= 20
        ? '遗忘率处于较稳区间，当前复习节奏可以继续保持。'
        : '“重来”比例偏高，需要降低新题输入或补齐旧题回顾。',
    badge: latestForgettingRate.value == null ? '待生成' : latestForgettingRate.value <= 20 ? '稳定' : '偏高',
    cta: latestForgettingRate.value == null ? '先完成复习' : latestForgettingRate.value <= 20 ? '维持复习节奏' : '优先安排记忆修复',
    ctaClass: latestForgettingRate.value == null ? 'text-slate-400' : latestForgettingRate.value <= 20 ? 'text-[var(--bc-cyan)]' : 'text-[var(--bc-coral)]',
    toneClass: latestForgettingRate.value != null && latestForgettingRate.value > 20 ? 'insight-card-risk' : 'insight-card-cyan',
  },
  {
    label: '修复重点',
    title: weakestMastery.value?.label || '等待数据',
    detail: weakestMastery.value
      ? `${weakestMastery.value.label} 当前数量最高，说明错题池的主要压力集中在这一状态。`
      : '完成复习后，这里会显示最需要优先处理的掌握状态。'
    ,
    badge: weakestMastery.value ? '重点' : '待生成',
    cta: weakestMastery.value?.label === '未开始' ? '先清空待启动错题' : weakestMastery.value?.label === '复习中' ? '维持复习节奏' : '转向新方向训练',
    ctaClass: weakestMastery.value?.label === '已掌握' ? 'text-[var(--bc-lime)]' : 'text-[var(--bc-amber)]',
    toneClass: weakestMastery.value?.label === '已掌握' ? 'insight-card-lime' : '',
  },
])

const summarySignals = computed(() => [
  {
    label: '观察周数',
    value: trendData.value.weeks.length,
    detail: '当前趋势图覆盖的周数。',
    toneClass: '',
  },
  {
    label: '平均 EF',
    value: efficiencyData.value.avgEaseFactor,
    detail: '当前错题池平均记忆强度。',
    toneClass: 'summary-slab-cyan',
  },
  {
    label: '总复习次数',
    value: efficiencyData.value.totalReviews,
    detail: '累计完成的复习次数。',
    toneClass: 'summary-slab-lime',
  },
  {
    label: '连续复习',
    value: `${efficiencyData.value.currentStreak} 天`,
    detail: '连续复习天数。',
    toneClass: 'summary-slab-amber',
  },
])

const signalLanes = computed(() => [
  {
    label: '趋势判断',
    value: weeklyScoreDelta.value == null ? '待生成' : weeklyScoreDelta.value >= 0 ? '上升中' : '回落中',
    detail: weeklyScoreDelta.value == null ? '等待更多面试数据。' : '根据窗口首尾分数估算整体走势。',
    dotClass: weeklyScoreDelta.value == null ? 'bg-slate-400' : weeklyScoreDelta.value >= 0 ? 'bg-[var(--bc-lime)]' : 'bg-[var(--bc-coral)]',
  },
  {
    label: '遗忘风险',
    value: latestForgettingRate.value == null ? '待生成' : `${latestForgettingRate.value}%`,
    detail: '最近一次遗忘率快照。',
    dotClass: latestForgettingRate.value == null ? 'bg-slate-400' : latestForgettingRate.value <= 20 ? 'bg-[var(--bc-cyan)]' : 'bg-[var(--bc-coral)]',
  },
  {
    label: '掌握压力',
    value: weakestMastery.value?.label || '待生成',
    detail: '当前最需要优先处理的掌握状态。',
    dotClass: weakestMastery.value?.label === '已掌握' ? 'bg-[var(--bc-lime)]' : 'bg-[var(--bc-amber)]',
  },
])

const masteryItems = computed(() => {
  const d = efficiencyData.value.masteryDistribution || {}
  const total = totalMasteryCount.value || 1
  return [
    {
      label: '未开始',
      count: d.not_started ?? 0,
      percent: Math.round(((d.not_started ?? 0) / total) * 100),
      description: '进入错题池后还没开始复习的题目。',
      toneClass: 'mastery-card-coral',
      textClass: 'text-[var(--bc-coral)]',
      fillClass: 'mastery-fill-coral',
    },
    {
      label: '复习中',
      count: d.reviewing ?? 0,
      percent: Math.round(((d.reviewing ?? 0) / total) * 100),
      description: '已经进入复习轨道，但还未稳定掌握。',
      toneClass: 'mastery-card-amber',
      textClass: 'text-[var(--bc-amber)]',
      fillClass: 'mastery-fill-amber',
    },
    {
      label: '已掌握',
      count: d.mastered ?? 0,
      percent: Math.round(((d.mastered ?? 0) / total) * 100),
      description: '已经进入稳定掌握区间的题目。',
      toneClass: 'mastery-card-lime',
      textClass: 'text-[var(--bc-lime)]',
      fillClass: 'mastery-fill-lime',
    },
  ]
})

const toggleCategory = (catId: number) => {
  const idx = selectedCategories.value.indexOf(catId)
  if (idx >= 0) {
    selectedCategories.value.splice(idx, 1)
  } else {
    selectedCategories.value.push(catId)
  }
}

const changeWeeks = (w: number) => {
  selectedWeeks.value = w
  void loadTrend()
}

const loadTrend = async () => {
  trendLoading.value = true
  try {
    const res = await fetchAbilityTrendApi(
      selectedWeeks.value,
      selectedCategories.value.length > 0 ? selectedCategories.value : undefined
    )
    trendData.value = res.data
    nextTick(renderTrendChart)
  } catch {
    // silently fail
  } finally {
    trendLoading.value = false
  }
}

const loadEfficiency = async () => {
  efficiencyLoading.value = true
  try {
    const res = await fetchEfficiencyApi()
    efficiencyData.value = res.data
    nextTick(() => {
      renderEFChart()
      renderFRChart()
    })
  } catch {
    // silently fail
  } finally {
    efficiencyLoading.value = false
  }
}

const chartTextColor = '#8ca6bf'
const chartGridColor = 'rgba(142, 196, 255, 0.12)'

const buildChartBase = () => ({
  textStyle: { color: chartTextColor, fontFamily: 'JetBrains Mono, monospace' },
  grid: { left: 42, right: 20, top: 24, bottom: 42 },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(7, 17, 31, 0.96)',
    borderColor: 'rgba(142, 196, 255, 0.18)',
    textStyle: { color: '#eef6ff' },
  },
  xAxis: {
    axisLine: { lineStyle: { color: chartGridColor } },
    axisLabel: { color: chartTextColor, fontSize: 11 },
  },
  yAxis: {
    splitLine: { lineStyle: { color: chartGridColor } },
    axisLabel: { color: chartTextColor, fontSize: 11 },
  },
})

const renderTrendChart = () => {
  if (!trendChartRef.value || !trendData.value.overallTrend?.length) return

  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const weeks = trendData.value.weeks || []
  const series: echarts.SeriesOption[] = [
    {
      name: '综合',
      type: 'line',
      data: weeks.map((w) => trendData.value.overallTrend.find((p) => p.week === w)?.score ?? null),
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      lineStyle: { width: 3, color: chartColors[0] },
      itemStyle: { color: chartColors[0] },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(255,183,77,0.24)' },
          { offset: 1, color: 'rgba(255,183,77,0.02)' },
        ]),
      },
    },
  ]

  const catTrends = selectedCategories.value.length > 0
    ? trendData.value.categoryTrends.filter((c) => selectedCategories.value.includes(c.categoryId))
    : trendData.value.categoryTrends

  catTrends.forEach((cat, idx) => {
    series.push({
      name: cat.categoryName,
      type: 'line',
      data: weeks.map((w) => cat.points.find((p) => p.week === w)?.score ?? null),
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2, color: chartColors[(idx + 1) % chartColors.length] },
      itemStyle: { color: chartColors[(idx + 1) % chartColors.length] },
    })
  })

  trendChart.setOption({
    ...buildChartBase(),
    legend: {
      data: series.map((s) => s.name as string),
      bottom: 0,
      textStyle: { fontSize: 11, color: chartTextColor },
      itemWidth: 12,
      itemHeight: 12,
    },
    xAxis: {
      ...(buildChartBase().xAxis as object),
      type: 'category',
      data: weeks,
    },
    yAxis: {
      ...(buildChartBase().yAxis as object),
      type: 'value',
      min: 0,
      max: 100,
    },
    series,
  }, true)
}

const renderEFChart = () => {
  if (!efChartRef.value || !efficiencyData.value.efTrend?.length) return

  if (!efChart) {
    efChart = echarts.init(efChartRef.value)
  }

  const data = efficiencyData.value.efTrend
  efChart.setOption({
    ...buildChartBase(),
    tooltip: {
      ...(buildChartBase().tooltip as object),
      formatter: (params: { dataIndex: number }[]) => {
        const first = params[0]
        if (!first) return ''
        const item = getChartPoint(data, first.dataIndex)
        if (!item) return ''
        return `${item.week}<br/>EF: <strong>${item.avgEF}</strong><br/>复习: ${item.reviewCount} 次`
      },
    },
    xAxis: {
      ...(buildChartBase().xAxis as object),
      type: 'category',
      data: data.map((d) => d.week),
      axisLabel: { color: chartTextColor, fontSize: 10, rotate: 24 },
    },
    yAxis: {
      ...(buildChartBase().yAxis as object),
      type: 'value',
      min: 1.3,
      max: 3.2,
      markLine: undefined,
    },
    series: [
      {
        type: 'line',
        data: data.map((d) => d.avgEF),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#55d6be', width: 3 },
        itemStyle: { color: '#55d6be' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(85,214,190,0.22)' },
            { offset: 1, color: 'rgba(85,214,190,0.02)' },
          ]),
        },
        markLine: {
          symbol: 'none',
          lineStyle: { type: 'dashed', color: 'rgba(255, 183, 77, 0.55)' },
          data: [
            { yAxis: 2.5, label: { formatter: '初始值 2.5', color: '#ffb74d' } },
            { yAxis: 1.3, label: { formatter: '风险线 1.3', color: '#ff6b6b' }, lineStyle: { color: 'rgba(255,107,107,0.55)' } },
          ],
        },
      },
    ],
  }, true)
}

const renderFRChart = () => {
  if (!frChartRef.value || !efficiencyData.value.forgettingRateTrend?.length) return

  if (!frChart) {
    frChart = echarts.init(frChartRef.value)
  }

  const data = efficiencyData.value.forgettingRateTrend
  frChart.setOption({
    ...buildChartBase(),
    tooltip: {
      ...(buildChartBase().tooltip as object),
      formatter: (params: { dataIndex: number }[]) => {
        const first = params[0]
        if (!first) return ''
        const item = getChartPoint(data, first.dataIndex)
        if (!item) return ''
        return `${item.week}<br/>遗忘率: <strong>${(item.forgettingRate * 100).toFixed(1)}%</strong><br/>重来: ${item.againCount}/${item.totalRatings}`
      },
    },
    xAxis: {
      ...(buildChartBase().xAxis as object),
      type: 'category',
      data: data.map((d) => d.week),
      axisLabel: { color: chartTextColor, fontSize: 10, rotate: 24 },
    },
    yAxis: {
      ...(buildChartBase().yAxis as object),
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: { color: chartTextColor, fontSize: 11, formatter: '{value}%' },
    },
    series: [
      {
        name: '遗忘率',
        type: 'bar',
        data: data.map((d) => +(d.forgettingRate * 100).toFixed(1)),
        barWidth: '48%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#ff6b6b' },
            { offset: 1, color: 'rgba(255,107,107,0.42)' },
          ]),
          borderRadius: [8, 8, 0, 0],
        },
      },
      {
        name: '重来次数',
        type: 'line',
        yAxisIndex: 0,
        data: data.map((d) => d.againCount),
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { width: 2, color: '#ffb74d' },
        itemStyle: { color: '#ffb74d' },
      },
    ],
  }, true)
}

const handleResize = () => {
  trendChart?.resize()
  efChart?.resize()
  frChart?.resize()
}

onMounted(() => {
  void loadTrend()
  void loadEfficiency()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  efChart?.dispose()
  frChart?.dispose()
  trendChart = null
  efChart = null
  frChart = null
})

watch(selectedCategories, () => {
  void loadTrend()
})
</script>

<style scoped>
.mode-switch {
  border: 1px solid var(--bc-line);
  border-radius: calc(var(--radius-md) + 4px);
  background: rgba(255, 255, 255, 0.32);
  padding: 4px;
}

.dark .mode-switch {
  background: rgba(255, 255, 255, 0.04);
}

.mode-switch__item {
  min-height: 44px;
  border: 0;
  border-radius: calc(var(--radius-sm) + 2px);
  background: transparent;
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  transition:
    background-color var(--motion-base) var(--ease-hard),
    color var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard);
}

.mode-switch__item-active {
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.18), rgba(var(--bc-accent-rgb), 0.08));
  color: var(--bc-ink);
  box-shadow: inset 0 0 0 1px rgba(var(--bc-accent-rgb), 0.2);
}

.insight-card,
.signal-lane,
.mastery-card {
  border-radius: 24px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.34);
  padding: 16px;
}

.dark .insight-card,
.dark .signal-lane,
.dark .mastery-card {
  background: rgba(255, 255, 255, 0.05);
}

.insight-card-risk {
  border-color: rgba(255, 107, 107, 0.28);
}

.insight-card-cyan {
  border-color: rgba(85, 214, 190, 0.26);
}

.insight-card-lime {
  border-color: rgba(159, 232, 112, 0.26);
}

.summary-slab-cyan {
  border-left-color: var(--bc-cyan);
}

.summary-slab-lime {
  border-left-color: var(--bc-lime);
}

.summary-slab-amber {
  border-left-color: var(--bc-amber);
}

.chart-shell {
  border-radius: 24px;
  border: 1px solid var(--bc-line);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.03), transparent 36%),
    rgba(6, 16, 29, 0.52);
}

.category-chip,
.detail-pill,
.rating-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 38px;
  border-radius: 999px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.3);
  padding: 0 12px;
  font-size: 12px;
  color: var(--bc-ink-secondary);
}

.dark .category-chip,
.dark .detail-pill,
.dark .rating-chip {
  background: rgba(255, 255, 255, 0.04);
}

.category-chip-active {
  border-color: rgba(var(--bc-accent-rgb), 0.24);
  background: rgba(var(--bc-accent-rgb), 0.12);
  color: var(--bc-ink);
}

.mastery-card-coral {
  border-color: rgba(255, 107, 107, 0.24);
}

.mastery-card-amber {
  border-color: rgba(255, 183, 77, 0.26);
}

.mastery-card-lime {
  border-color: rgba(159, 232, 112, 0.24);
}

.mastery-track {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(140, 166, 191, 0.18);
}

.mastery-track__fill {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.mastery-fill-coral {
  background: linear-gradient(90deg, rgba(255,107,107,0.48), var(--bc-coral));
}

.mastery-fill-amber {
  background: linear-gradient(90deg, rgba(255,183,77,0.48), var(--bc-amber));
}

.mastery-fill-lime {
  background: linear-gradient(90deg, rgba(159,232,112,0.48), var(--bc-lime));
}
</style>
