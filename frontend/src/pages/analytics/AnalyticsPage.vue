<template>
  <div class="analytics-cockpit space-y-5">
    <AppShellHeader>
      <template #actions>
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
      </template>
    </AppShellHeader>

    <AnalyticsInsightBar :data="learningInsights" />

    <section class="shell-section-card p-4 sm:p-5">
      <div class="grid gap-4 xl:grid-cols-[288px_minmax(0,1fr)] xl:items-stretch">
        <aside class="analytics-overview">
          <div>
            <p class="section-kicker">学习概览</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">先看整体节奏</h3>
          </div>

          <div class="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
            <article
              v-for="signal in summarySignals"
              :key="signal.label"
              class="analytics-overview-card"
              :class="signal.toneClass"
            >
              <p class="analytics-overview-card__label">{{ signal.label }}</p>
              <p class="analytics-overview-card__value">{{ signal.value }}</p>
            </article>
          </div>
        </aside>

        <div class="analytics-main-chart">
          <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">记忆趋势</h3>
              <p class="mt-1 text-sm text-secondary">完成率、复习负债和掌握增长</p>
            </div>
          </div>

          <div v-if="trendLoading" class="mt-4 flex h-[340px] items-center justify-center">
            <div class="h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
          </div>
          <div
            v-else-if="
              !trendData.completionRateTrend?.length &&
              !trendData.reviewDebtTrend?.length &&
              !trendData.masteredGrowthTrend?.length
            "
            class="mt-4 flex h-[340px] items-center justify-center"
          >
            <EmptyState icon="chart" title="暂无记忆趋势数据" description="开始学习后查看趋势。" compact />
          </div>
          <div v-else class="mt-4">
            <div ref="trendChartRef" class="chart-shell h-[340px] w-full"></div>
          </div>
        </div>
      </div>
    </section>

    <section class="grid gap-4 xl:grid-cols-2">
      <article class="shell-section-card p-4 sm:p-5">
        <div class="flex items-center justify-between gap-3">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">记忆强度</h3>
          </div>
        </div>

        <div v-if="efficiencyLoading" class="mt-5 flex h-[260px] items-center justify-center">
          <div class="h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
        </div>
        <div v-else-if="!efficiencyData.efTrend?.length" class="mt-5 flex h-[260px] items-center justify-center">
          <EmptyState icon="review" title="暂无复习数据" description="完成复习后查看记忆系数。" compact />
        </div>
        <div v-else class="mt-5">
          <div class="grid gap-3 sm:grid-cols-3">
            <article class="data-slab p-4">
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">平均记忆系数</p>
              <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ efficiencyData.avgEaseFactor }}</p>
            </article>
            <article class="data-slab p-4">
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">复习完成率</p>
              <p class="mt-3 font-mono text-3xl font-semibold text-ink">
                {{ formatPercent(efficiencyData.reviewCompletionRate) }}%
              </p>
            </article>
            <article class="data-slab p-4">
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">连续天数</p>
              <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ efficiencyData.currentStreak }} 天</p>
            </article>
          </div>
          <div ref="efChartRef" class="chart-shell mt-4 h-[240px] w-full"></div>
        </div>
      </article>

      <article class="shell-section-card p-4 sm:p-5">
        <div class="flex items-center justify-between gap-3">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">遗忘率</h3>
          </div>
        </div>

        <div v-if="efficiencyLoading" class="mt-5 flex h-[260px] items-center justify-center">
          <div class="h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
        </div>
        <div
          v-else-if="!efficiencyData.forgettingRateTrend?.length"
          class="mt-5 flex h-[260px] items-center justify-center"
        >
          <EmptyState icon="review" title="暂无复习数据" description="完成复习后查看遗忘率。" compact />
        </div>
        <div v-else class="mt-5">
          <div class="flex flex-wrap gap-2">
            <div v-for="(label, key) in ratingLabels" :key="key" class="rating-chip">
              <span class="h-2.5 w-2.5 rounded-full" :class="ratingColor(Number(key))"></span>
              <span>{{ label }}</span>
              <span class="font-mono text-ink">{{ efficiencyData.ratingDistribution?.[key] ?? 0 }}</span>
            </div>
          </div>
          <div ref="frChartRef" class="chart-shell mt-4 h-[240px] w-full"></div>
        </div>
      </article>
    </section>

    <section class="grid gap-4 xl:grid-cols-2">
      <article class="shell-section-card p-4 sm:p-5">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">分类掌握度</h3>
          </div>
        </div>

        <div v-if="!efficiencyLoading && categoryMasteryItems.length" class="mt-6 space-y-4">
          <article
            v-for="item in categoryMasteryItems"
            :key="`${item.categoryName}-${item.categoryId ?? 'na'}`"
            class="mastery-card"
          >
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="text-lg font-semibold text-ink">{{ item.categoryName }}</p>
                <p class="mt-1 text-sm text-secondary">
                  已掌握 {{ item.masteredCards }}/{{ item.totalCards }} · 待复习 {{ item.dueCount }}
                </p>
              </div>
              <div class="text-right">
                <p class="font-mono text-3xl font-semibold text-ink">{{ Math.round(item.masteryRate) }}%</p>
                <p class="text-xs font-semibold uppercase tracking-[0.18em] text-accent">掌握率</p>
              </div>
            </div>
            <div class="mastery-track mt-4">
              <span
                class="mastery-track__fill mastery-fill-cyan"
                :style="{ width: `${Math.round(item.masteryRate)}%` }"
              ></span>
            </div>
          </article>
        </div>
        <div v-else class="mt-5">
          <EmptyState icon="chart" title="暂无分类掌握度" description="生成并复习卡片后查看掌握度。" compact />
        </div>
      </article>

      <article class="shell-section-card p-4 sm:p-5">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">掌握分布</h3>
            <p class="mt-1 text-sm text-secondary">{{ totalMasteryCount }} 道题</p>
          </div>
        </div>

        <div v-if="!efficiencyLoading && hasMasteryData" class="mt-6 space-y-4">
          <article v-for="item in masteryItems" :key="item.label" class="mastery-card" :class="item.toneClass">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="text-lg font-semibold text-ink">{{ item.label }}</p>
              </div>
              <div class="text-right">
                <p class="font-mono text-3xl font-semibold text-ink">{{ item.count }}</p>
                <p class="text-xs font-semibold uppercase tracking-[0.18em]" :class="item.textClass">
                  {{ item.percent }}%
                </p>
              </div>
            </div>
            <div class="mastery-track mt-4">
              <span class="mastery-track__fill" :class="item.fillClass" :style="{ width: `${item.percent}%` }"></span>
            </div>
          </article>
        </div>
        <div v-else class="mt-5">
          <EmptyState icon="chart" title="暂无掌握分布" description="完成复习后查看分布。" compact />
        </div>
      </article>
    </section>

    <section class="shell-section-card p-4 sm:p-5">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div class="min-w-0">
          <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">面试趋势</h3>
        </div>
      </div>

      <div v-if="trendLoading" class="mt-5 flex h-[300px] items-center justify-center">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      </div>
      <div v-else-if="!trendData.overallTrend?.length" class="mt-5 flex h-[300px] items-center justify-center">
        <EmptyState icon="chart" title="暂无面试数据" description="完成面试后查看趋势。" compact />
      </div>
      <div v-else class="mt-5">
        <div v-if="normalizedCategoryTrends.length" class="mb-4 flex flex-wrap gap-2">
          <button
            type="button"
            class="category-chip"
            :class="{ 'category-chip-active': selectedCategories.length === 0 }"
            @click="selectedCategories = []"
          >
            全部分类
          </button>
          <button
            v-for="cat in normalizedCategoryTrends"
            :key="cat.categoryId"
            type="button"
            class="category-chip"
            :class="{ 'category-chip-active': selectedCategories.includes(cat.categoryId) }"
            @click="toggleCategory(cat.categoryId)"
          >
            {{ cat.displayName }}
          </button>
        </div>
        <div ref="interviewTrendChartRef" class="chart-shell h-[300px] w-full"></div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AppShellHeader from '@/components/AppShellHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useTheme } from '@/composables/useTheme'
import { readThemePalette } from '@/utils/theme'
import AnalyticsInsightBar from './AnalyticsInsightBar.vue'
import { fetchAbilityTrendApi, fetchEfficiencyApi, fetchLearningInsightsApi } from '@/api/analytics'
import type { AbilityTrend, EfficiencyData, LearningInsights } from '@/types/api'

const weekOptions = [
  { label: '4 周', value: 4 },
  { label: '8 周', value: 8 },
  { label: '12 周', value: 12 }
]

const selectedWeeks = ref(12)
const selectedCategories = ref<number[]>([])
const trendLoading = ref(true)
const efficiencyLoading = ref(true)
const trendData = ref<AbilityTrend>({
  weeks: [],
  completionRateTrend: [],
  reviewDebtTrend: [],
  masteredGrowthTrend: [],
  overallTrend: [],
  categoryTrends: []
})
const efficiencyData = ref<EfficiencyData>({
  avgEaseFactor: 2.5,
  efTrend: [],
  ratingDistribution: {},
  forgettingRateTrend: [],
  completionRateTrend: [],
  reviewDebtTrend: [],
  masteredGrowthTrend: [],
  masteryDistribution: {},
  contentTypeDistribution: {},
  categoryMastery: [],
  totalReviews: 0,
  currentStreak: 0,
  reviewCompletionRate: 0,
  forgettingRate: 0
})
const learningInsights = ref<LearningInsights>({
  thisWeekAvgScore: 0,
  lastWeekAvgScore: 0,
  thisWeekInterviewCount: 0,
  lastWeekInterviewCount: 0,
  todayCompletionStatus: '',
  reviewDebtStatus: '',
  masteryGrowthStatus: '',
  categoryChanges: [],
  bestStudyHours: []
})
const { theme } = useTheme()

const trendChartRef = ref<HTMLElement | null>(null)
const interviewTrendChartRef = ref<HTMLElement | null>(null)
const efChartRef = ref<HTMLElement | null>(null)
const frChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null
let interviewTrendChart: echarts.ECharts | null = null
let efChart: echarts.ECharts | null = null
let frChart: echarts.ECharts | null = null

const ratingLabels: Record<number, string> = { 1: '重来', 2: '困难', 3: '良好', 4: '轻松' }
const ratingColor = (key: number) => {
  const map: Record<number, string> = {
    1: 'bg-[var(--bc-coral)]',
    2: 'bg-[var(--bc-amber)]',
    3: 'bg-[var(--bc-cyan)]',
    4: 'bg-[var(--bc-lime)]'
  }
  return map[key] || 'bg-[var(--text-tertiary)]'
}

const hasMasteryData = computed(() => {
  const d = efficiencyData.value.masteryDistribution
  return d && Object.values(d).some((v) => v > 0)
})

const totalMasteryCount = computed(() => {
  const d = efficiencyData.value.masteryDistribution
  return Object.values(d || {}).reduce((sum, value) => sum + value, 0)
})

const normalizedCategoryTrends = computed(() => {
  const byId = new Map<number, AbilityTrend['categoryTrends'][number]>()
  for (const trend of trendData.value.categoryTrends || []) {
    if (!byId.has(trend.categoryId)) {
      byId.set(trend.categoryId, trend)
    }
  }
  const deduped = [...byId.values()]
  const nameCount = deduped.reduce<Record<string, number>>((acc, item) => {
    const name = item.categoryName || `分类 ${item.categoryId}`
    acc[name] = (acc[name] || 0) + 1
    return acc
  }, {})
  return deduped.map((item) => ({
    ...item,
    displayName: (() => {
      const name = item.categoryName || `分类 ${item.categoryId}`
      return (nameCount[name] ?? 0) > 1 ? `${name} #${item.categoryId}` : name
    })()
  }))
})

const latestCompletionRate = computed(() => {
  const data = trendData.value.completionRateTrend
  return data.length ? data[data.length - 1]!.value : null
})

const masteryItems = computed(() => {
  const d = efficiencyData.value.masteryDistribution || {}
  const total = totalMasteryCount.value || 1
  return [
    {
      label: '未开始',
      count: d.not_started ?? 0,
      percent: Math.round(((d.not_started ?? 0) / total) * 100),
      description: '',
      toneClass: 'mastery-card-coral',
      textClass: 'text-[var(--bc-coral)]',
      fillClass: 'mastery-fill-coral'
    },
    {
      label: '复习中',
      count: d.reviewing ?? 0,
      percent: Math.round(((d.reviewing ?? 0) / total) * 100),
      description: '',
      toneClass: 'mastery-card-amber',
      textClass: 'text-[var(--bc-amber)]',
      fillClass: 'mastery-fill-amber'
    },
    {
      label: '已掌握',
      count: d.mastered ?? 0,
      percent: Math.round(((d.mastered ?? 0) / total) * 100),
      description: '',
      toneClass: 'mastery-card-lime',
      textClass: 'text-[var(--bc-lime)]',
      fillClass: 'mastery-fill-lime'
    }
  ]
})

const categoryMasteryItems = computed(() => efficiencyData.value.categoryMastery || [])

const summarySignals = computed(() => [
  {
    label: '观察周数',
    value: selectedWeeks.value,
    detail: '',
    toneClass: ''
  },
  {
    label: '当前完成率',
    value: `${formatPercent(latestCompletionRate.value)}%`,
    detail: '',
    toneClass: 'summary-slab-cyan'
  },
  {
    label: '总复习次数',
    value: efficiencyData.value.totalReviews,
    detail: '',
    toneClass: 'summary-slab-lime'
  },
  {
    label: '连续复习',
    value: `${efficiencyData.value.currentStreak} 天`,
    detail: '',
    toneClass: 'summary-slab-amber'
  }
])

const formatPercent = (value?: number | null) => {
  if (value == null || Number.isNaN(value)) return '0'
  return Number.isInteger(value) ? String(value) : Number(value).toFixed(0)
}

const changeWeeks = (w: number) => {
  selectedWeeks.value = w
  void loadTrend()
}

const toggleCategory = (catId: number) => {
  const idx = selectedCategories.value.indexOf(catId)
  if (idx >= 0) {
    selectedCategories.value.splice(idx, 1)
  } else {
    selectedCategories.value.push(catId)
  }
}

const disposeTrendCharts = () => {
  trendChart?.dispose()
  interviewTrendChart?.dispose()
  trendChart = null
  interviewTrendChart = null
}

const loadTrend = async () => {
  trendLoading.value = true
  disposeTrendCharts()
  try {
    const res = await fetchAbilityTrendApi(
      selectedWeeks.value,
      selectedCategories.value.length > 0 ? selectedCategories.value : undefined
    )
    trendData.value = res.data
  } finally {
    trendLoading.value = false
    await nextTick()
    renderTrendChart()
    renderInterviewTrendChart()
  }
}

const loadEfficiency = async () => {
  efficiencyLoading.value = true
  try {
    const [efficiencyRes, insightsRes] = await Promise.all([fetchEfficiencyApi(), fetchLearningInsightsApi()])
    efficiencyData.value = efficiencyRes.data
    learningInsights.value = insightsRes.data
    nextTick(() => {
      renderEFChart()
      renderFRChart()
    })
  } finally {
    efficiencyLoading.value = false
  }
}

const buildChartBase = () => {
  const palette = readThemePalette()
  return {
    textStyle: { color: palette.textSecondary, fontFamily: 'JetBrains Mono, monospace' },
    grid: { left: 44, right: 48, top: 24, bottom: 38, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: palette.surfaceCard,
      borderColor: palette.borderSubtle,
      textStyle: { color: palette.textPrimary }
    },
    xAxis: {
      axisLine: { lineStyle: { color: palette.borderSubtle } },
      axisLabel: { color: palette.textSecondary, fontSize: 11 }
    },
    yAxis: {
      splitLine: { lineStyle: { color: palette.borderSubtle } },
      axisLabel: { color: palette.textSecondary, fontSize: 11 }
    }
  }
}

const renderTrendChart = () => {
  if (!trendChartRef.value) return
  const weeks = trendData.value.weeks || []
  const completion = trendData.value.completionRateTrend || []
  const debts = trendData.value.reviewDebtTrend || []
  const mastered = trendData.value.masteredGrowthTrend || []
  if (!weeks.length && !completion.length && !debts.length && !mastered.length) return

  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const xAxisData = weeks.length ? weeks : completion.map((item) => item.week)
  const palette = readThemePalette()
  const base = buildChartBase()
  trendChart.setOption(
    {
      ...base,
      legend: {
        data: ['完成率', '复习负债', '已掌握卡片'],
        bottom: 0,
        textStyle: { fontSize: 11, color: palette.textSecondary }
      },
      xAxis: {
        ...(base.xAxis as object),
        type: 'category',
        data: xAxisData
      },
      yAxis: [
        {
          ...(base.yAxis as object),
          type: 'value',
          min: 0,
          max: 100,
          axisLabel: { color: palette.textSecondary, fontSize: 11, formatter: '{value}%', margin: 10 }
        },
        {
          ...(base.yAxis as object),
          type: 'value',
          min: 0,
          position: 'right',
          splitLine: { show: false },
          axisLabel: {
            color: palette.textSecondary,
            fontSize: 11,
            margin: 12,
            formatter: (value: number) => Number(value).toLocaleString('zh-CN')
          }
        }
      ],
      series: [
        {
          name: '完成率',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          data: xAxisData.map((week) => completion.find((item) => item.week === week)?.value ?? null),
          lineStyle: { width: 3, color: palette.cyan },
          itemStyle: { color: palette.cyan }
        },
        {
          name: '复习负债',
          type: 'bar',
          yAxisIndex: 1,
          data: xAxisData.map((week) => debts.find((item) => item.week === week)?.value ?? null),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: palette.coral },
              { offset: 1, color: `${palette.coral}48` }
            ]),
            borderRadius: [8, 8, 0, 0]
          }
        },
        {
          name: '已掌握卡片',
          type: 'line',
          yAxisIndex: 1,
          smooth: true,
          symbol: 'circle',
          data: xAxisData.map((week) => mastered.find((item) => item.week === week)?.value ?? null),
          lineStyle: { width: 2, color: palette.lime },
          itemStyle: { color: palette.lime }
        }
      ]
    },
    true
  )
}

const renderInterviewTrendChart = () => {
  if (!interviewTrendChartRef.value || !trendData.value.overallTrend?.length) return

  if (!interviewTrendChart) {
    interviewTrendChart = echarts.init(interviewTrendChartRef.value)
  }

  const palette = readThemePalette()
  const chartColors = palette.chartColors
  const base = buildChartBase()
  const weeks = trendData.value.weeks || []
  const series: echarts.SeriesOption[] = [
    {
      name: '综合分数',
      type: 'line',
      data: weeks.map((w) => trendData.value.overallTrend.find((p) => p.week === w)?.score ?? null),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 3, color: chartColors[0] },
      itemStyle: { color: chartColors[0] }
    }
  ]

  const catTrends =
    selectedCategories.value.length > 0
      ? normalizedCategoryTrends.value.filter((c) => selectedCategories.value.includes(c.categoryId))
      : normalizedCategoryTrends.value

  catTrends.forEach((cat, idx) => {
    series.push({
      name: cat.displayName,
      type: 'line',
      data: weeks.map((w) => cat.points.find((p) => p.week === w)?.score ?? null),
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      lineStyle: { width: 2, color: chartColors[(idx + 1) % chartColors.length] },
      itemStyle: { color: chartColors[(idx + 1) % chartColors.length] }
    })
  })

  interviewTrendChart.setOption(
    {
      ...base,
      legend: {
        data: series.map((item) => item.name as string),
        bottom: 0,
        textStyle: { fontSize: 11, color: palette.textSecondary }
      },
      xAxis: {
        ...(base.xAxis as object),
        type: 'category',
        data: weeks
      },
      yAxis: {
        ...(base.yAxis as object),
        type: 'value',
        min: 0,
        max: 100
      },
      series
    },
    true
  )
}

const renderEFChart = () => {
  if (!efChartRef.value || !efficiencyData.value.efTrend?.length) return
  if (!efChart) {
    efChart = echarts.init(efChartRef.value)
  }

  const palette = readThemePalette()
  const base = buildChartBase()
  const data = efficiencyData.value.efTrend
  efChart.setOption(
    {
      ...base,
      xAxis: {
        ...(base.xAxis as object),
        type: 'category',
        data: data.map((d) => d.week),
        axisLabel: { color: palette.textSecondary, fontSize: 10, rotate: 24 }
      },
      yAxis: {
        ...(base.yAxis as object),
        type: 'value',
        min: 1.3,
        max: 3.2
      },
      series: [
        {
          type: 'line',
          data: data.map((d) => d.avgEF),
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { color: palette.cyan, width: 3 },
          itemStyle: { color: palette.cyan },
          markLine: {
            symbol: 'none',
            lineStyle: { type: 'dashed', color: palette.borderStrong },
            data: [
              { yAxis: 2.5, label: { formatter: '2.5', color: palette.amber } },
              { yAxis: 1.3, label: { formatter: '1.3', color: palette.coral }, lineStyle: { color: palette.coral } }
            ]
          }
        }
      ]
    },
    true
  )
}

const renderFRChart = () => {
  if (!frChartRef.value || !efficiencyData.value.forgettingRateTrend?.length) return
  if (!frChart) {
    frChart = echarts.init(frChartRef.value)
  }

  const palette = readThemePalette()
  const base = buildChartBase()
  const data = efficiencyData.value.forgettingRateTrend
  frChart.setOption(
    {
      ...base,
      xAxis: {
        ...(base.xAxis as object),
        type: 'category',
        data: data.map((d) => d.week),
        axisLabel: { color: palette.textSecondary, fontSize: 10, rotate: 24 }
      },
      yAxis: {
        ...(base.yAxis as object),
        type: 'value',
        min: 0,
        max: 100,
        axisLabel: { color: palette.textSecondary, fontSize: 11, formatter: '{value}%' }
      },
      series: [
        {
          name: '遗忘率',
          type: 'bar',
          data: data.map((d) => +(d.forgettingRate * 100).toFixed(1)),
          barWidth: '48%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: palette.coral },
              { offset: 1, color: `${palette.coral}4d` }
            ]),
            borderRadius: [8, 8, 0, 0]
          }
        },
        {
          name: '重来次数',
          type: 'line',
          data: data.map((d) => d.againCount),
          smooth: true,
          symbol: 'circle',
          symbolSize: 5,
          lineStyle: { width: 2, color: palette.amber },
          itemStyle: { color: palette.amber }
        }
      ]
    },
    true
  )
}

const handleResize = () => {
  trendChart?.resize()
  interviewTrendChart?.resize()
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
  interviewTrendChart?.dispose()
  efChart?.dispose()
  frChart?.dispose()
  trendChart = null
  interviewTrendChart = null
  efChart = null
  frChart = null
})

watch(selectedCategories, () => {
  void loadTrend()
})

watch(theme, () => {
  nextTick(() => {
    renderTrendChart()
    renderInterviewTrendChart()
    renderEFChart()
    renderFRChart()
  })
})
</script>

<style scoped>
.mode-switch {
  border: 1px solid var(--bc-border-subtle);
  border-radius: 999px;
  background: var(--interactive-bg);
  padding: 4px;
}

.dark .mode-switch {
  background: var(--interactive-bg);
}

.mode-switch__item {
  min-height: 36px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--bc-ink-secondary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
  transition: all 180ms ease;
}

.mode-switch__item:hover {
  background: rgba(var(--bc-accent-rgb), 0.08);
  color: var(--bc-ink);
}

.mode-switch__item-active {
  background: rgba(var(--bc-accent-rgb), 0.12);
  color: var(--bc-ink);
}

.analytics-overview {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.analytics-overview-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 0.95rem 1rem;
}

.analytics-overview-card__label {
  font-size: 0.78rem;
  color: var(--bc-ink-secondary);
}

.analytics-overview-card__value {
  margin-top: 0.55rem;
  font-family: theme('fontFamily.mono');
  font-size: clamp(1.8rem, 2vw, 2.4rem);
  font-weight: 700;
  line-height: 1;
  color: var(--bc-ink);
}

.analytics-main-chart {
  min-width: 0;
}

.insight-card {
  border-radius: calc(var(--radius-md) + 2px);
  border: 1px solid var(--bc-line);
  background: var(--panel-muted);
  padding: 20px;
}

.insight-card-risk {
  border-color: rgba(255, 107, 107, 0.35);
}

.insight-card-cyan {
  border-color: rgba(85, 214, 190, 0.35);
}

.insight-card-lime {
  border-color: rgba(159, 232, 112, 0.45);
}

.signal-lane {
  border-radius: calc(var(--radius-sm) + 2px);
  border: 1px solid var(--bc-line);
  background: var(--panel-muted);
  padding: 14px 16px;
}

.chart-shell {
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.04), transparent 26%), var(--panel-bg);
}

.category-chip {
  min-height: 36px;
  border-radius: 999px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--interactive-bg);
  padding: 0 14px;
  font-size: 12px;
  font-weight: 700;
  color: var(--bc-ink-secondary);
}

.category-chip-active {
  border-color: rgba(var(--bc-accent-rgb), 0.18);
  background: rgba(var(--bc-accent-rgb), 0.12);
  color: var(--bc-ink);
}

.rating-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--interactive-bg);
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 700;
  color: var(--bc-ink-secondary);
}

.mastery-card {
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-bg);
  padding: 18px;
}

.mastery-card-coral {
  border-color: rgba(255, 107, 107, 0.25);
}

.mastery-card-amber {
  border-color: rgba(255, 183, 77, 0.25);
}

.mastery-card-lime {
  border-color: rgba(159, 232, 112, 0.28);
}

.mastery-track {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
}

.mastery-track__fill {
  display: block;
  height: 100%;
  border-radius: 999px;
}

.mastery-fill-coral {
  background: linear-gradient(90deg, rgba(255, 107, 107, 0.68), rgba(255, 107, 107, 0.95));
}

.mastery-fill-amber {
  background: linear-gradient(90deg, rgba(255, 183, 77, 0.68), rgba(255, 183, 77, 0.95));
}

.mastery-fill-lime {
  background: linear-gradient(90deg, rgba(159, 232, 112, 0.68), rgba(159, 232, 112, 0.95));
}

.mastery-fill-cyan {
  background: linear-gradient(90deg, rgba(var(--bc-cyan-rgb), 0.62), var(--bc-cyan));
}
</style>
