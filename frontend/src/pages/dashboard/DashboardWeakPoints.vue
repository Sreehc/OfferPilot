<template>
  <article class="paper-panel p-6">
    <p class="section-kicker">薄弱点</p>
    <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">优先补强的知识点</h3>

    <div class="surface-muted mt-6 p-5">
      <div class="flex items-center justify-between">
        <div>
          <div class="text-sm font-semibold text-ink">计划完成率</div>
          <div class="mt-1 text-sm text-slate-500 dark:text-slate-400">当前激活计划的任务推进情况</div>
        </div>
        <div class="text-4xl font-semibold tracking-[-0.03em] text-ink">{{ planCompletionRate }}%</div>
      </div>
      <div class="mt-4 h-3 rounded-full bg-slate-200/80 dark:bg-slate-700/80">
        <div class="h-3 rounded-full bg-accent transition-all duration-300" :style="{ width: `${planCompletionRate}%` }"></div>
      </div>
    </div>

    <div class="surface-muted mt-3 p-5">
      <div class="flex items-center justify-between">
        <div>
          <div class="text-sm font-semibold text-ink">计划健康分</div>
          <div class="mt-1 text-sm text-slate-500 dark:text-slate-400">基于近 7 天完成率的综合评估</div>
        </div>
        <div class="flex items-center gap-2">
          <span
            class="inline-flex h-10 w-10 items-center justify-center rounded-full text-sm font-bold text-white"
            :class="healthBadgeColor"
          >
            {{ planHealthScore }}
          </span>
        </div>
      </div>
      <div class="mt-4 h-3 rounded-full bg-slate-200/80 dark:bg-slate-700/80">
        <div
          class="h-3 rounded-full transition-all duration-300"
          :class="healthBarColor"
          :style="{ width: `${planHealthScore}%` }"
        ></div>
      </div>
    </div>

    <div v-if="weakPoints.length" class="mt-5">
      <div ref="radarChartRef" class="mx-auto" style="width: 100%; height: 280px;"></div>
      <div class="surface-muted mt-3 divide-y divide-slate-200/70 dark:divide-slate-700/70 overflow-hidden">
        <div v-for="point in weakPoints" :key="point.categoryName" class="px-4 py-4">
          <div class="flex items-center justify-between gap-3 text-sm">
            <div>
              <div class="font-semibold text-ink">{{ point.categoryName }}</div>
              <div class="text-slate-500 dark:text-slate-400">错题 {{ point.wrongCount }} · 平均分 {{ formatScore(point.score) }}</div>
            </div>
            <div class="text-2xl font-semibold tracking-[-0.03em] text-accent">{{ point.wrongCount }}</div>
          </div>
          <div class="mt-2 h-2 rounded-full bg-slate-200 dark:bg-slate-700">
            <div
              class="h-2 rounded-full bg-accent/80"
              :style="{ width: `${Math.min(point.wrongCount * 20, 100)}%` }"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <EmptyState
      v-else
      icon="chart"
      title="还没有可计算的薄弱点"
      description="先进入问答、面试或错题练习，系统才会根据真实学习记录生成重点复习方向。"
      compact
      class="mt-5"
    />
  </article>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import EmptyState from '@/components/EmptyState.vue'
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import type { WeakPointItem } from '@/types/api'

const props = defineProps<{
  weakPoints: WeakPointItem[]
  planCompletionRate: number
  planHealthScore: number
}>()

const radarChartRef = ref<HTMLElement | null>(null)
let radarChart: echarts.ECharts | null = null

const healthBadgeColor = computed(() => {
  if (props.planHealthScore >= 70) return 'bg-green-500'
  if (props.planHealthScore >= 40) return 'bg-amber-500'
  return 'bg-red-500'
})

const healthBarColor = computed(() => {
  if (props.planHealthScore >= 70) return 'bg-green-500'
  if (props.planHealthScore >= 40) return 'bg-amber-500'
  return 'bg-red-500'
})

const formatScore = (score: number): string => {
  return Number.isInteger(score) ? String(score) : score.toFixed(2)
}

const renderRadarChart = () => {
  if (!radarChartRef.value || !props.weakPoints.length) return
  if (radarChart) radarChart.dispose()
  radarChart = echarts.init(radarChartRef.value)
  const points = props.weakPoints
  const maxWrong = Math.max(...points.map((p) => p.wrongCount), 1)
  radarChart.setOption({
    radar: {
      indicator: points.map((p) => ({ name: p.categoryName, max: maxWrong + 2 })),
      shape: 'circle',
      splitNumber: 4,
      axisName: { color: '#64748b', fontSize: 12 },
      splitArea: { areaStyle: { color: ['rgba(47,79,157,0.04)', 'rgba(47,79,157,0.08)'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: points.map((p) => p.wrongCount),
        name: '错题数',
        areaStyle: { color: 'rgba(47,79,157,0.2)' },
        lineStyle: { color: '#2f4f9d', width: 2 },
        itemStyle: { color: '#2f4f9d' }
      }]
    }]
  })
}

watch(() => props.weakPoints, () => {
  nextTick(renderRadarChart)
}, { deep: true })

onBeforeUnmount(() => {
  if (radarChart) radarChart.dispose()
})
</script>
