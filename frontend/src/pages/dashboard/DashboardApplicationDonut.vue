<template>
  <section class="shell-section-card dashboard-donut-card p-4 sm:p-5">
    <div class="flex items-center justify-between gap-3">
      <div>
        <h3 class="dashboard-donut-card__title">投递进度总览</h3>
      </div>
      <RouterLink
        to="/applications"
        class="accent-link touch-link text-sm font-semibold"
      >
        查看全部
      </RouterLink>
    </div>

    <div class="dashboard-donut-card__body">
      <div
        ref="chartRef"
        class="dashboard-donut-card__chart"
      />

      <div class="dashboard-donut-card__legend">
        <div
          v-for="item in items"
          :key="item.status"
          class="dashboard-donut-card__legend-row"
        >
          <span class="dashboard-donut-card__legend-label">
            <span
              class="dashboard-donut-card__legend-dot"
              :style="{ backgroundColor: item.color }"
            />
            {{ item.label }}
          </span>
          <strong>{{ item.count }}</strong>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { EMPTY_STATE_COPY } from '@/constants/productCopy'
import { useTheme } from '@/composables/useTheme'

interface DonutItem {
  status: string
  label: string
  count: number
  color: string
}

const props = defineProps<{
  items: DonutItem[]
  total: number
}>()

const { theme } = useTheme()
const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const hasData = computed(() => props.items.some((item) => item.count > 0))

const readThemeToken = (name: string) => getComputedStyle(document.documentElement).getPropertyValue(name).trim()

const renderChart = () => {
  if (!chartRef.value) return

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const emptyColor = readThemeToken('--bc-border')
  const inkColor = readThemeToken('--bc-ink')
  const secondaryInkColor = readThemeToken('--bc-ink-secondary')

  const seriesData = hasData.value
    ? props.items.map((item) => ({
        value: item.count,
        name: item.label,
        itemStyle: { color: item.color }
      }))
    : [{ value: 1, name: '记录投递', itemStyle: { color: emptyColor } }]

  chart.setOption({
    animationDuration: 550,
    tooltip: {
      trigger: 'item',
      formatter: hasData.value
        ? '{b}: {c}'
        : `${EMPTY_STATE_COPY.dashboardApplicationDonut.title}，${EMPTY_STATE_COPY.dashboardApplicationDonut.description}`
    },
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '42%',
        style: {
          text: `${props.total}`,
          fill: inkColor,
          fontSize: 34,
          fontWeight: 700,
          fontFamily: 'Barlow Condensed, sans-serif'
        }
      },
      {
        type: 'text',
        left: 'center',
        top: '58%',
        style: {
          text: '总投递',
          fill: secondaryInkColor,
          fontSize: 12,
          fontWeight: 600,
          fontFamily: 'Noto Sans SC, sans-serif'
        }
      }
    ],
    series: [
      {
        type: 'pie',
        radius: ['66%', '84%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        label: { show: false },
        labelLine: { show: false },
        data: seriesData
      }
    ]
  })
}

watch(
  () => [props.items, props.total, theme.value],
  () => {
    nextTick(renderChart)
  },
  { deep: true, immediate: true }
)

onBeforeUnmount(() => {
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.dashboard-donut-card {
  display: flex;
  min-height: var(--dashboard-summary-side-height, 304px);
  flex-direction: column;
  border-radius: 10px;
  border: 1px solid rgba(18, 41, 76, 0.06);
  box-shadow: 0 7px 18px rgba(30, 48, 90, 0.03);
}

.dashboard-donut-card__title {
  color: #162b4a;
  font-size: 1.04rem;
  font-weight: 800;
  letter-spacing: 0;
  text-wrap: balance;
}

.dashboard-donut-card__body {
  display: grid;
  flex: 1 1 auto;
  grid-template-columns: minmax(0, 1.06fr) minmax(190px, 0.94fr);
  gap: 1rem;
  align-items: stretch;
  margin-top: 0.75rem;
}

.dashboard-donut-card__chart {
  width: 100%;
  min-height: 232px;
  height: 100%;
}

.dashboard-donut-card__legend {
  display: flex;
  height: 100%;
  min-width: 0;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.65rem;
}

.dashboard-donut-card__legend strong {
  font-variant-numeric: tabular-nums;
}

.dashboard-donut-card__legend-row {
  display: flex;
  min-height: 0;
  flex: 1 1 0;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
  border-radius: 8px;
  background: rgba(248, 251, 255, 0.92);
  padding: 0.56rem 0.72rem;
}

.dashboard-donut-card__legend-row strong {
  color: var(--text-primary);
  font-size: 0.9rem;
  flex-shrink: 0;
}

.dashboard-donut-card__legend-label {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-weight: 600;
  white-space: nowrap;
}

.dashboard-donut-card__legend-dot {
  width: 0.62rem;
  height: 0.62rem;
  flex-shrink: 0;
  border-radius: 999px;
}

@media (max-width: 767px) {
  .dashboard-donut-card__body {
    grid-template-columns: 1fr;
  }
}
</style>
