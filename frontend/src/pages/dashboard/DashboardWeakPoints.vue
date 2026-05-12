<template>
  <article class="shell-section-card p-6">
    <p class="section-kicker">分类掌握度</p>
    <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">优先补齐的记忆分类</h3>

    <div v-if="items.length" class="mt-6 space-y-4">
      <div ref="chartRef" class="mx-auto h-[280px] w-full"></div>
      <div class="surface-muted overflow-hidden divide-y divide-slate-200/70 dark:divide-slate-700/70">
        <div v-for="item in items" :key="item.categoryName" class="px-4 py-4">
          <div class="flex items-center justify-between gap-3 text-sm">
            <div>
              <div class="font-semibold text-ink">{{ item.categoryName }}</div>
              <div class="text-slate-500 dark:text-slate-400">
                待复习 {{ item.dueCount }} · 已掌握 {{ item.masteredCards }}/{{ item.totalCards }}
              </div>
            </div>
            <div class="text-right">
              <div class="text-2xl font-semibold tracking-[-0.03em] text-accent">{{ Math.round(item.masteryRate) }}%</div>
              <div class="text-xs text-slate-400">掌握率</div>
            </div>
          </div>
          <div class="mt-3 h-2 rounded-full bg-slate-200 dark:bg-slate-700">
            <div
              class="h-2 rounded-full bg-accent/80"
              :style="{ width: `${Math.min(Math.max(item.masteryRate, 0), 100)}%` }"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <EmptyState
      v-else
      icon="chart"
      title="还没有可计算的分类掌握度"
      description="生成并复习卡片后查看分类掌握度。"
      compact
      class="mt-5"
    />
  </article>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import EmptyState from '@/components/EmptyState.vue'
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import type { CategoryMasteryItem } from '@/types/api'

const props = defineProps<{
  items: CategoryMasteryItem[]
}>()

const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const renderChart = () => {
  if (!chartRef.value || !props.items.length) return
  if (chart) chart.dispose()
  chart = echarts.init(chartRef.value)

  const items = props.items.slice(0, 6)
  chart.setOption({
    grid: { left: 12, right: 12, top: 12, bottom: 12, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: Array<{ seriesName: string; value: number }>) => {
        const first = params[0]
        const idx = params.findIndex((item) => item.seriesName === '掌握率')
        const item = items[idx >= 0 ? idx : 0]
        if (!first || !item) return ''
        return `${item.categoryName}<br/>掌握率: <strong>${Math.round(item.masteryRate)}%</strong><br/>待复习: ${item.dueCount}<br/>已掌握: ${item.masteredCards}/${item.totalCards}`
      }
    },
    xAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: { formatter: '{value}%' },
      splitLine: { lineStyle: { color: '#e2e8f0' } }
    },
    yAxis: {
      type: 'category',
      data: items.map((item) => item.categoryName),
      axisTick: { show: false },
      axisLine: { show: false }
    },
    series: [
      {
        name: '掌握率',
        type: 'bar',
        data: items.map((item) => Number(item.masteryRate.toFixed(2))),
        barWidth: 14,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: '#365ab0' },
            { offset: 1, color: '#55d6be' }
          ]),
          borderRadius: [0, 8, 8, 0]
        }
      }
    ]
  })
}

watch(
  () => props.items,
  () => {
    nextTick(renderChart)
  },
  { deep: true, immediate: true }
)

onBeforeUnmount(() => {
  if (chart) chart.dispose()
})
</script>
