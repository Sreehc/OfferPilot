<template>
  <article class="paper-panel p-6">
    <p class="section-kicker">成绩趋势</p>
    <h3 class="mt-3 text-lg font-semibold text-ink">分数变化</h3>

    <div v-if="loading" class="mt-4 flex h-[260px] items-center justify-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
    </div>

    <div v-else-if="!trendData.length" class="mt-4 flex h-[260px] items-center justify-center">
      <EmptyState
        icon="chart"
        title="暂无面试数据"
        description="完成面试后将显示趋势"
        compact
      />
    </div>

    <div v-else ref="chartRef" class="mt-4 h-[260px] w-full"></div>
  </article>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import EmptyState from '@/components/EmptyState.vue'
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'

interface TrendItem {
  sessionId: number
  direction: string
  totalScore: number
  startTime?: string
}

const props = defineProps<{
  trendData: TrendItem[]
  loading: boolean
}>()

const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

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
  const tooltips = props.trendData.map(
    (item) => `${item.direction} - ${formatTime(item.startTime)}`
  )

  chart.setOption({
    tooltip: {
      trigger: 'axis',
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
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    series: [
      {
        type: 'line',
        data: scores,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#365ab0', width: 2 },
        itemStyle: { color: '#365ab0' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(54,90,176,0.25)' },
            { offset: 1, color: 'rgba(54,90,176,0.02)' }
          ])
        },
        markLine: {
          silent: true,
          lineStyle: { color: '#ef4444', type: 'dashed', width: 1 },
          data: [{ yAxis: 60, label: { formatter: '60', color: '#ef4444', fontSize: 10 } }]
        }
      }
    ]
  })
}

watch(
  () => [props.trendData, props.loading],
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
