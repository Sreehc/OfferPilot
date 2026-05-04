<template>
  <div class="space-y-6">
    <!-- Header with time range filter -->
    <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="text-xl font-semibold text-ink">数据分析</h2>
        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">深入了解你的学习模式和进步趋势</p>
      </div>
      <div class="flex items-center gap-2">
        <button
          v-for="w in weekOptions"
          :key="w.value"
          type="button"
          class="rounded-lg px-3 py-1.5 text-xs font-medium transition"
          :class="selectedWeeks === w.value
            ? 'bg-accent text-white'
            : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700'"
          @click="changeWeeks(w.value)"
        >
          {{ w.label }}
        </button>
      </div>
    </div>

    <!-- Ability Trend Chart -->
    <article class="paper-panel p-4 sm:p-6">
      <p class="section-kicker">能力趋势</p>
      <h3 class="mt-3 text-lg font-semibold text-ink">面试得分变化</h3>
      <div v-if="trendLoading" class="mt-4 flex h-[300px] items-center justify-center">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      </div>
      <div v-else-if="!trendData.overallTrend?.length" class="mt-4 flex h-[300px] items-center justify-center">
        <EmptyState icon="chart" title="暂无面试数据" description="完成面试后将显示趋势" compact />
      </div>
      <div v-else class="mt-4">
        <!-- Category filter chips -->
        <div v-if="trendData.categoryTrends?.length" class="mb-3 flex flex-wrap gap-2">
          <button
            type="button"
            class="rounded-full px-2.5 py-1 text-xs font-medium transition"
            :class="selectedCategories.length === 0
              ? 'bg-accent text-white'
              : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-300'"
            @click="selectedCategories = []"
          >
            全部
          </button>
          <button
            v-for="cat in trendData.categoryTrends"
            :key="cat.categoryId"
            type="button"
            class="rounded-full px-2.5 py-1 text-xs font-medium transition"
            :class="selectedCategories.includes(cat.categoryId)
              ? 'bg-accent text-white'
              : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-300'"
            @click="toggleCategory(cat.categoryId)"
          >
            {{ cat.categoryName }}
          </button>
        </div>
        <div ref="trendChartRef" class="h-[300px] w-full"></div>
      </div>
    </article>

    <!-- Efficiency Section -->
    <section class="grid gap-4 lg:grid-cols-2">
      <!-- EF Trend -->
      <article class="paper-panel p-4 sm:p-6">
        <p class="section-kicker">复习效率</p>
        <h3 class="mt-3 text-lg font-semibold text-ink">记忆强度变化</h3>
        <div v-if="efficiencyLoading" class="mt-4 flex h-[240px] items-center justify-center">
          <div class="h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
        </div>
        <div v-else-if="!efficiencyData.efTrend?.length" class="mt-4 flex h-[240px] items-center justify-center">
          <EmptyState icon="review" title="暂无复习数据" description="完成复习后将显示数据" compact />
        </div>
        <div v-else>
          <div class="mt-3 flex items-center gap-4">
            <div>
              <p class="text-xs text-slate-500">当前平均 EF</p>
              <p class="mt-1 text-2xl font-bold text-accent">{{ efficiencyData.avgEaseFactor }}</p>
            </div>
            <div>
              <p class="text-xs text-slate-500">总复习次数</p>
              <p class="mt-1 text-2xl font-bold text-ink">{{ efficiencyData.totalReviews }}</p>
            </div>
            <div>
              <p class="text-xs text-slate-500">连续复习</p>
              <p class="mt-1 text-2xl font-bold" :class="efficiencyData.currentStreak > 0 ? 'text-green-600' : 'text-slate-400'">
                {{ efficiencyData.currentStreak }} 天
              </p>
            </div>
          </div>
          <div ref="efChartRef" class="mt-4 h-[200px] w-full"></div>
        </div>
      </article>

      <!-- Forgetting Rate -->
      <article class="paper-panel p-4 sm:p-6">
        <p class="section-kicker">遗忘分析</p>
        <h3 class="mt-3 text-lg font-semibold text-ink">遗忘率趋势</h3>
        <div v-if="efficiencyLoading" class="mt-4 flex h-[240px] items-center justify-center">
          <div class="h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
        </div>
        <div v-else-if="!efficiencyData.forgettingRateTrend?.length" class="mt-4 flex h-[240px] items-center justify-center">
          <EmptyState icon="review" title="暂无复习数据" description="完成复习后将显示数据" compact />
        </div>
        <div v-else>
          <!-- Rating distribution summary -->
          <div class="mt-3 flex items-center gap-3">
            <div v-for="(label, key) in ratingLabels" :key="key" class="flex items-center gap-1.5">
              <span class="h-2.5 w-2.5 rounded-full" :class="ratingColor(Number(key))"></span>
              <span class="text-xs text-slate-500">{{ label }}</span>
              <span class="text-xs font-medium text-ink">{{ efficiencyData.ratingDistribution?.[key] ?? 0 }}</span>
            </div>
          </div>
          <div ref="frChartRef" class="mt-4 h-[200px] w-full"></div>
        </div>
      </article>
    </section>

    <!-- Mastery Distribution -->
    <article v-if="!efficiencyLoading && hasMasteryData" class="paper-panel p-4 sm:p-6">
      <p class="section-kicker">掌握程度</p>
      <h3 class="mt-3 text-lg font-semibold text-ink">错题掌握分布</h3>
      <div class="mt-4 grid gap-4 sm:grid-cols-3">
        <div
          v-for="item in masteryItems"
          :key="item.label"
          class="flex items-center gap-3 rounded-lg p-3"
          :class="item.bgClass"
        >
          <span class="text-2xl">{{ item.emoji }}</span>
          <div>
            <p class="text-2xl font-bold" :class="item.textClass">{{ item.count }}</p>
            <p class="text-xs" :class="item.labelClass">{{ item.label }}</p>
          </div>
        </div>
      </div>
    </article>
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

const ratingLabels: Record<number, string> = { 1: 'Again', 2: 'Hard', 3: 'Good', 4: 'Easy' }

const ratingColor = (key: number) => {
  const map: Record<number, string> = {
    1: 'bg-red-400',
    2: 'bg-orange-400',
    3: 'bg-green-400',
    4: 'bg-blue-400',
  }
  return map[key] || 'bg-slate-400'
}

const getChartPoint = <T extends { week: string }>(items: T[], dataIndex: number) => items[dataIndex] ?? null

const hasMasteryData = computed(() => {
  const d = efficiencyData.value.masteryDistribution
  return d && Object.values(d).some((v) => v > 0)
})

const masteryItems = computed(() => {
  const d = efficiencyData.value.masteryDistribution || {}
  return [
    { label: '未开始', emoji: '🔴', count: d.not_started ?? 0, bgClass: 'bg-red-50 dark:bg-red-900/10', textClass: 'text-red-600', labelClass: 'text-red-500' },
    { label: '复习中', emoji: '🟡', count: d.reviewing ?? 0, bgClass: 'bg-yellow-50 dark:bg-yellow-900/10', textClass: 'text-yellow-600', labelClass: 'text-yellow-500' },
    { label: '已掌握', emoji: '🟢', count: d.mastered ?? 0, bgClass: 'bg-green-50 dark:bg-green-900/10', textClass: 'text-green-600', labelClass: 'text-green-500' },
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
  loadTrend()
}

const loadTrend = async () => {
  trendLoading.value = true
  try {
    const res = await fetchAbilityTrendApi(selectedWeeks.value, selectedCategories.value.length > 0 ? selectedCategories.value : undefined)
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

const chartColors = ['#365ab0', '#ef4444', '#22c55e', '#f59e0b', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316']

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
      data: weeks.map((w) => {
        const pt = trendData.value.overallTrend.find((p) => p.week === w)
        return pt ? pt.score : null
      }),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 3, color: chartColors[0] },
      itemStyle: { color: chartColors[0] },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(54,90,176,0.2)' },
          { offset: 1, color: 'rgba(54,90,176,0.02)' },
        ]),
      },
    },
  ]

  // Add category lines if filtered
  const catTrends = selectedCategories.value.length > 0
    ? trendData.value.categoryTrends.filter((c) => selectedCategories.value.includes(c.categoryId))
    : trendData.value.categoryTrends

  catTrends.forEach((cat, idx) => {
    series.push({
      name: cat.categoryName,
      type: 'line',
      data: weeks.map((w) => {
        const pt = cat.points.find((p) => p.week === w)
        return pt ? pt.score : null
      }),
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      lineStyle: { width: 2, color: chartColors[(idx + 1) % chartColors.length] },
      itemStyle: { color: chartColors[(idx + 1) % chartColors.length] },
    })
  })

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: series.map((s) => s.name as string), bottom: 0, textStyle: { fontSize: 11, color: '#94a3b8' } },
    grid: { left: 40, right: 20, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: weeks,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 },
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
    tooltip: {
      trigger: 'axis',
      formatter: (params: { dataIndex: number; value: number }[]) => {
        const first = params[0]
        if (!first) return ''
        const item = getChartPoint(data, first.dataIndex)
        if (!item) return ''
        return `${item.week}<br/>EF: <strong>${item.avgEF}</strong><br/>复习: ${item.reviewCount} 次`
      },
    },
    grid: { left: 40, right: 20, top: 10, bottom: 30 },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.week),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#94a3b8', fontSize: 10, rotate: 30 },
    },
    yAxis: {
      type: 'value',
      min: 1.3,
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 },
    },
    series: [
      {
        type: 'line',
        data: data.map((d) => d.avgEF),
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { color: '#22c55e', width: 2 },
        itemStyle: { color: '#22c55e' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(34,197,94,0.15)' },
            { offset: 1, color: 'rgba(34,197,94,0.02)' },
          ]),
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
    tooltip: {
      trigger: 'axis',
      formatter: (params: { dataIndex: number; value: number }[]) => {
        const first = params[0]
        if (!first) return ''
        const item = getChartPoint(data, first.dataIndex)
        if (!item) return ''
        return `${item.week}<br/>遗忘率: <strong>${(item.forgettingRate * 100).toFixed(1)}%</strong><br/>Again: ${item.againCount}/${item.totalRatings}`
      },
    },
    grid: { left: 40, right: 20, top: 10, bottom: 30 },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.week),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#94a3b8', fontSize: 10, rotate: 30 },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#94a3b8', fontSize: 11, formatter: '{value}%' },
    },
    series: [
      {
        type: 'bar',
        data: data.map((d) => +(d.forgettingRate * 100).toFixed(1)),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#ef4444' },
            { offset: 1, color: '#fca5a5' },
          ]),
          borderRadius: [4, 4, 0, 0],
        },
        barWidth: '60%',
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
