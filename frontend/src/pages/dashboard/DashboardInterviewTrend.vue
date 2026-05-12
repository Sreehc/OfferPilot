<template>
  <article class="shell-section-card p-6">
    <p class="section-kicker">成绩趋势</p>
    <h3 class="mt-3 text-lg font-semibold text-ink">分数变化</h3>

    <div v-if="loading" class="mt-4 flex h-[260px] items-center justify-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
    </div>

    <div v-else-if="!trendData.length" class="mt-4 flex h-[260px] items-center justify-center">
      <EmptyState icon="chart" title="暂无面试数据" description="完成面试后将显示趋势" compact />
    </div>

    <div v-else ref="chartRef" class="mt-4 h-[260px] w-full"></div>
  </article>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import EmptyState from '@/components/EmptyState.vue'
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useTheme } from '@/composables/useTheme'

interface TrendItem {
  sessionId: string
  direction: string
  totalScore: number
  startTime?: string
}

const props = defineProps<{
  trendData: TrendItem[]
  loading: boolean
}>()

const { theme } = useTheme()
const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const readThemeToken = (name: string) => getComputedStyle(document.documentElement).getPropertyValue(name).trim()

const formatTime = (time?: string): string => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const renderChart = () => {
  if (!chartRef.value || !props.trendData.length) return

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const xData = props.trendData.map((_, i) => `#${i + 1}`)
  const scores = props.trendData.map((item) => Number(item.totalScore) || 0)
  const tooltips = props.trendData.map((item) => `${item.direction} - ${formatTime(item.startTime)}`)
  const accentColor = readThemeToken('--bc-accent')
  const accentRgb = readThemeToken('--bc-accent-rgb')
  const coralColor = readThemeToken('--bc-coral')
  const inkColor = readThemeToken('--bc-ink')
  const secondaryInkColor = readThemeToken('--bc-ink-secondary')
  const borderColor = readThemeToken('--bc-border')
  const surfaceColor = readThemeToken('--bc-surface-card')

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: surfaceColor,
      borderColor,
      textStyle: { color: inkColor },
      formatter: (params: { dataIndex: number; value: number }[]) => {
        const p = params[0]
        if (!p) return ''
        return `${tooltips[p.dataIndex]}<br/>分数: <strong>${p.value}</strong>`
      }
    },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: xData,
      axisLine: { lineStyle: { color: borderColor } },
      axisLabel: { color: secondaryInkColor, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      splitLine: { lineStyle: { color: borderColor } },
      axisLabel: { color: secondaryInkColor, fontSize: 11 }
    },
    series: [
      {
        type: 'line',
        data: scores,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: accentColor, width: 2 },
        itemStyle: { color: accentColor },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: `rgba(${accentRgb}, 0.22)` },
            { offset: 1, color: `rgba(${accentRgb}, 0.02)` }
          ])
        },
        markLine: {
          silent: true,
          lineStyle: { color: coralColor, type: 'dashed', width: 1 },
          data: [{ yAxis: 60, label: { formatter: '60', color: coralColor, fontSize: 10 } }]
        }
      }
    ]
  })
}

watch(
  () => [props.trendData, props.loading, theme.value],
  () => {
    if (!props.loading && props.trendData.length) {
      nextTick(renderChart)
    }
  },
  { deep: true }
)

onBeforeUnmount(() => {
  chart?.dispose()
  chart = null
})
</script>
