<template>
  <div v-loading="loading" class="space-y-5">
    <div class="grid gap-5 xl:grid-cols-[minmax(0,1fr)_320px]">
      <section class="shell-section-card p-5 sm:p-6">
        <div class="overview-panel-head">
          <div>
            <p class="section-kicker">系统趋势</p>
            <h3 class="overview-panel-title">近 30 天用户趋势</h3>
          </div>
          <span class="overview-panel-range">最近 30 天</span>
        </div>

        <div class="mt-5 grid gap-3 sm:grid-cols-2">
          <article class="data-slab p-4">
            <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500">最近一天新增</p>
            <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ latestPoint?.newUsers ?? 0 }}</p>
          </article>
          <article class="data-slab p-4">
            <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500">最近一天活跃</p>
            <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ latestPoint?.activeUsers ?? 0 }}</p>
          </article>
        </div>

        <div v-if="trend.length" ref="chartRef" class="chart-shell mt-5 h-[320px] w-full"></div>
        <div v-else class="mt-5 flex h-[320px] items-center justify-center text-sm text-slate-500">
          暂无趋势数据
        </div>
      </section>

      <aside class="space-y-4">
        <section class="shell-section-card p-5 sm:p-6">
          <p class="section-kicker">系统概览</p>
          <h3 class="overview-panel-title mt-3">后台摘要</h3>
          <div class="mt-5 space-y-3">
            <article v-for="card in spotlightCards" :key="card.label" class="overview-spotlight">
              <p class="overview-spotlight__label">{{ card.label }}</p>
              <p class="overview-spotlight__value">{{ card.value }}</p>
            </article>
          </div>
        </section>

        <section class="shell-section-card p-5 sm:p-6">
          <p class="section-kicker">业务负载</p>
          <div class="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
            <article class="data-slab p-4">
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500">总面试次数</p>
              <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ overview?.totalInterviews ?? '-' }}</p>
            </article>
            <article class="data-slab p-4">
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500">总复习次数</p>
              <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ overview?.totalReviews ?? '-' }}</p>
            </article>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { fetchAdminOverviewApi, fetchAdminTrendApi } from '@/api/admin'
import type { AdminOverview, AdminTrendItem } from '@/api/admin'

const loading = ref(false)
const overview = ref<AdminOverview | null>(null)
const trend = ref<AdminTrendItem[]>([])
const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const latestPoint = computed(() => trend.value.at(-1) ?? null)
const spotlightCards = computed(() => [
  { label: '总用户数', value: overview.value?.totalUsers ?? '-' },
  { label: '今日活跃', value: overview.value?.todayActive ?? '-' },
  { label: '今日新增', value: overview.value?.todayNew ?? '-' }
])

const loadData = async () => {
  loading.value = true
  try {
    const [overviewRes, trendRes] = await Promise.all([fetchAdminOverviewApi(), fetchAdminTrendApi()])
    overview.value = overviewRes.data
    trend.value = trendRes.data
    renderChart()
  } catch {
    ElMessage.error('加载概览数据失败')
  } finally {
    loading.value = false
  }
}

const renderChart = () => {
  if (!chartRef.value || trend.value.length === 0) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: 'rgba(16, 35, 58, 0.08)',
      textStyle: { color: '#10233a' }
    },
    legend: { data: ['新增用户', '活跃用户'], bottom: 0, textStyle: { color: '#6b7b8d' } },
    grid: { left: '3%', right: '4%', bottom: '14%', top: '6%', containLabel: true },
    xAxis: {
      type: 'category',
      data: trend.value.map((t) => t.date),
      boundaryGap: false,
      axisLine: { lineStyle: { color: 'rgba(16, 35, 58, 0.12)' } },
      axisLabel: { color: '#6b7b8d' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisLabel: { color: '#6b7b8d' },
      splitLine: { lineStyle: { color: 'rgba(16, 35, 58, 0.08)' } }
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        lineStyle: { width: 3, color: '#b56a12' },
        itemStyle: { color: '#b56a12' },
        areaStyle: { color: 'rgba(181, 106, 18, 0.12)' },
        data: trend.value.map((t) => t.newUsers)
      },
      {
        name: '活跃用户',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        lineStyle: { width: 3, color: '#2f7f77' },
        itemStyle: { color: '#2f7f77' },
        areaStyle: { color: 'rgba(47, 127, 119, 0.08)' },
        data: trend.value.map((t) => t.activeUsers)
      }
    ]
  })
}

const handleResize = () => {
  chart?.resize()
}

onMounted(() => {
  void loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  chart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.overview-panel-head {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.overview-panel-title {
  color: var(--bc-ink);
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.overview-panel-range {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(var(--bc-accent-rgb), 0.1);
  padding: 8px 12px;
  color: var(--bc-ink-secondary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.overview-spotlight {
  border-radius: 20px;
  background: rgba(var(--bc-accent-rgb), 0.08);
  padding: 16px;
}

.overview-spotlight__label {
  color: var(--bc-ink-secondary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.overview-spotlight__value {
  margin-top: 10px;
  color: var(--bc-ink);
  font-family: theme('fontFamily.mono');
  font-size: 2rem;
  font-weight: 700;
}

.chart-shell {
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.5), rgba(255, 255, 255, 0.34));
}
</style>
