<template>
  <div class="space-y-6">
    <!-- Metric cards -->
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

    <!-- Trend chart -->
    <section class="paper-panel p-6">
      <h4 class="text-lg font-semibold text-ink mb-4">近 30 天趋势</h4>
      <div ref="chartRef" class="h-72 w-full"></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { fetchAdminOverviewApi, fetchAdminTrendApi } from '@/api/admin'
import type { AdminOverview, AdminTrendItem } from '@/api/admin'

const loading = ref(false)
const overview = ref<AdminOverview | null>(null)
const trend = ref<AdminTrendItem[]>([])
const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

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
