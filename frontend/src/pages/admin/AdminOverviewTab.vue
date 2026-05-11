<template>
  <div class="space-y-6">
    <div v-loading="loading" class="grid grid-cols-2 gap-4 md:grid-cols-5">
      <article class="metric-card text-center">
        <p class="metric-label">总用户数</p>
        <p class="metric-value">{{ overview?.totalUsers ?? '-' }}</p>
      </article>
      <article class="metric-card text-center">
        <p class="metric-label">今日活跃</p>
        <p class="metric-value">{{ overview?.todayActive ?? '-' }}</p>
      </article>
      <article class="metric-card text-center">
        <p class="metric-label">今日新增</p>
        <p class="metric-value">{{ overview?.todayNew ?? '-' }}</p>
      </article>
      <article class="metric-card text-center">
        <p class="metric-label">总面试次数</p>
        <p class="metric-value">{{ overview?.totalInterviews ?? '-' }}</p>
      </article>
      <article class="metric-card text-center">
        <p class="metric-label">总复习次数</p>
        <p class="metric-value">{{ overview?.totalReviews ?? '-' }}</p>
      </article>
    </div>

    <section class="overview-trend-panel p-6">
      <div class="overview-trend-head">
        <div>
          <h3 class="overview-trend-title">近 30 天用户趋势</h3>
        </div>
        <span class="overview-trend-range">最近 30 天</span>
      </div>

      <div v-if="latestPoint" class="overview-trend-summary">
        <article class="overview-trend-node">
          <p class="overview-trend-node__label">最近一天新增</p>
          <p class="overview-trend-node__value">{{ latestPoint.newUsers }}</p>
        </article>
        <article class="overview-trend-node">
          <p class="overview-trend-node__label">最近一天活跃</p>
          <p class="overview-trend-node__value">{{ latestPoint.activeUsers }}</p>
        </article>
      </div>

      <div v-if="trend.length" ref="chartRef" class="h-72 w-full"></div>
      <div v-else class="flex h-72 items-center justify-center text-sm text-slate-500 dark:text-slate-400">
        暂无趋势数据
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { fetchAdminOverviewApi, fetchAdminTrendApi } from '@/api/admin'
import type { AdminOverview, AdminTrendItem } from '@/api/admin'

const loading = ref(false)
const overview = ref<AdminOverview | null>(null)
const trend = ref<AdminTrendItem[]>([])
const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null
const latestPoint = computed(() => trend.value.at(-1) ?? null)

const loadData = async () => {
  loading.value = true
  try {
    const [overviewRes, trendRes] = await Promise.all([fetchAdminOverviewApi(), fetchAdminTrendApi()])
    overview.value = overviewRes.data
    trend.value = trendRes.data
    renderChart()
  } catch { ElMessage.error('加载概览数据失败') } finally { loading.value = false }
}

const renderChart = () => {
  if (!chartRef.value || trend.value.length === 0) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增用户', '活跃用户'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '12%', top: '5%', containLabel: true },
    xAxis: { type: 'category', data: trend.value.map((t) => t.date), boundaryGap: false },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '新增用户', type: 'line', smooth: true, data: trend.value.map((t) => t.newUsers), itemStyle: { color: '#2F4F9D' } },
      { name: '活跃用户', type: 'line', smooth: true, data: trend.value.map((t) => t.activeUsers), itemStyle: { color: '#82b1ff' } }
    ]
  })
}

const handleResize = () => { chart?.resize() }

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
.overview-trend-panel {
  border-radius: 28px;
  border: 1px solid var(--bc-line);
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.08), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.62), rgba(255, 255, 255, 0.42));
}

.dark .overview-trend-panel {
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.12), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.03));
}

.overview-trend-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.overview-trend-title {
  color: var(--bc-ink);
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.overview-trend-range {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid var(--bc-line);
  padding: 6px 10px;
  color: var(--bc-ink-secondary);
  font-size: 11px;
  font-weight: 600;
}

.overview-trend-summary {
  display: grid;
  gap: 12px;
  margin-top: 18px;
  margin-bottom: 10px;
}

.overview-trend-node {
  border-radius: 20px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.34);
  padding: 14px;
}

.dark .overview-trend-node {
  background: rgba(255, 255, 255, 0.04);
}

.overview-trend-node__label {
  color: var(--bc-ink-secondary);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.overview-trend-node__value {
  margin-top: 8px;
  color: var(--bc-ink);
  font-family: theme('fontFamily.mono');
  font-size: 1.75rem;
  font-weight: 700;
}

@media (min-width: 768px) {
  .overview-trend-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
