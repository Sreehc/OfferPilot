<template>
  <div class="space-y-6">
    <section
      v-if="showGuideCard"
      class="paper-panel relative overflow-hidden bg-[linear-gradient(135deg,rgba(47,79,157,0.08),rgba(255,255,255,0.94))] p-6"
      style="border-radius: var(--radius-lg);"
    >
      <div
        class="absolute -right-8 top-0 h-28 w-40 rotate-[8deg] bg-[rgba(47,79,157,0.08)] blur-2xl"
        style="border-radius: var(--radius-lg);"
      ></div>
      <div class="relative flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
        <div class="max-w-3xl">
          <p class="section-kicker">First Session</p>
          <h3 class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink">从一个动作开始，不要让首页只是空白看板</h3>
          <p class="mt-3 text-sm leading-7 text-slate-600">
            先发起一次问答或一场面试，系统才会逐步生成错题、薄弱点和计划完成率。这个阶段的首页重点是帮助你找到第一步。
          </p>
          <div class="mt-5 flex flex-wrap gap-3">
            <RouterLink
              v-for="action in quickActions"
              :key="action.to"
              :to="action.to"
              class="hard-button-primary"
            >
              {{ action.label }}
            </RouterLink>
          </div>
        </div>
        <button
          type="button"
          class="hard-button-secondary"
          @click="dismissGuide"
        >
          知道了
        </button>
      </div>
    </section>

    <section v-if="loading" class="grid gap-4 xl:grid-cols-4 md:grid-cols-2">
      <article v-for="index in 4" :key="index" class="metric-card">
        <div class="h-4 w-20 animate-pulse bg-slate-200" style="border-radius: var(--radius-sm);"></div>
        <div class="mt-5 h-12 w-24 animate-pulse bg-slate-200" style="border-radius: var(--radius-md);"></div>
        <div class="mt-3 h-4 w-full animate-pulse bg-slate-100" style="border-radius: var(--radius-sm);"></div>
      </article>
    </section>

    <template v-else>
      <section class="grid gap-4 xl:grid-cols-4 md:grid-cols-2">
        <article v-for="metric in metrics" :key="metric.label" class="metric-card">
          <p class="metric-label">{{ metric.label }}</p>
          <p class="metric-value">{{ metric.value }}</p>
          <p class="mt-2 text-sm leading-6 text-slate-500">{{ metric.desc }}</p>
        </article>
      </section>

      <section class="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
        <article class="paper-panel p-6">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p class="section-kicker">Recent Interviews</p>
              <h3 class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink">最近面试结果</h3>
            </div>
            <RouterLink class="accent-link text-sm font-semibold" to="/interview">开始下一场</RouterLink>
          </div>

          <div v-if="overview.recentInterviews.length" class="mt-6 space-y-3">
            <div
              v-for="interview in overview.recentInterviews"
              :key="interview.sessionId"
              class="surface-card surface-card-hover p-4"
            >
              <div class="flex items-start justify-between gap-3">
                <div>
                  <div class="text-xs uppercase tracking-[0.28em] text-slate-500">{{ interview.direction }}</div>
                  <div class="mt-2 text-lg font-semibold text-ink">{{ interviewTitle(interview) }}</div>
                </div>
                <div class="text-right">
                  <div class="text-3xl font-semibold tracking-[-0.03em] text-ink">{{ formatScore(interview.totalScore) }}</div>
                  <div class="mt-1 text-xs uppercase tracking-[0.22em] text-slate-500">{{ statusLabel(interview.status) }}</div>
                </div>
              </div>
              <div class="mt-3 text-sm text-slate-500">{{ formatDate(interview.finishedAt) }}</div>
            </div>
          </div>

          <div v-else class="empty-state-card mt-6">
            <div class="font-semibold text-ink">还没有面试记录</div>
            <p class="mt-2 text-sm leading-6 text-slate-500">
              先完成一场 3-5 题的模拟面试，首页才会开始积累最近结果和平均分。
            </p>
          </div>
        </article>

        <article class="paper-panel p-6">
          <p class="section-kicker">Weak Points</p>
          <h3 class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink">薄弱点与计划节奏</h3>

          <div class="surface-muted mt-6 p-5">
            <div class="flex items-center justify-between">
              <div>
                <div class="text-sm font-semibold text-ink">计划完成率</div>
                <div class="mt-1 text-sm text-slate-500">当前激活计划的任务推进情况</div>
              </div>
              <div class="text-4xl font-semibold tracking-[-0.03em] text-ink">{{ overview.planCompletionRate }}%</div>
            </div>
            <div class="mt-4 h-3 rounded-full bg-slate-200/80">
              <div class="h-3 rounded-full bg-accent transition-all duration-300" :style="{ width: `${overview.planCompletionRate}%` }"></div>
            </div>
          </div>

          <div v-if="overview.weakPoints.length" class="mt-5">
            <div ref="radarChartRef" class="mx-auto" style="width: 100%; height: 280px;"></div>
            <div class="surface-muted mt-3 divide-y divide-slate-200/70 overflow-hidden">
              <div v-for="point in overview.weakPoints" :key="point.categoryName" class="px-4 py-4">
                <div class="flex items-center justify-between gap-3 text-sm">
                  <div>
                    <div class="font-semibold text-ink">{{ point.categoryName }}</div>
                    <div class="text-slate-500">错题 {{ point.wrongCount }} · 平均分 {{ formatScore(point.score) }}</div>
                  </div>
                  <div class="text-2xl font-semibold tracking-[-0.03em] text-accent">{{ point.wrongCount }}</div>
                </div>
                <div class="mt-2 h-2 rounded-full bg-slate-200">
                  <div
                    class="h-2 rounded-full bg-accent/80"
                    :style="{ width: `${Math.min(point.wrongCount * 20, 100)}%` }"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="empty-state-card mt-5">
            <div class="font-semibold text-ink">还没有可计算的薄弱点</div>
            <p class="mt-2 text-sm leading-6 text-slate-500">
              先进入问答、面试或错题练习，系统才会根据真实学习记录生成重点复习方向。
            </p>
          </div>
        </article>
      </section>

      <section class="grid gap-4 xl:grid-cols-[1fr_0.9fr]">
        <article class="paper-panel p-6">
          <p class="section-kicker">Quick Actions</p>
          <h3 class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink">把首页变成起点，而不是终点</h3>
          <div class="mt-6 grid gap-4 md:grid-cols-2">
            <RouterLink
              v-for="action in quickActions"
              :key="action.to"
              :to="action.to"
              class="surface-card surface-card-hover p-5"
            >
              <div class="text-xs uppercase tracking-[0.28em] text-slate-500">{{ action.kicker }}</div>
              <div class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">{{ action.label }}</div>
              <p class="mt-2 text-sm leading-6 text-slate-500">{{ action.desc }}</p>
            </RouterLink>
          </div>
        </article>

        <article class="paper-panel p-6">
          <p class="section-kicker">How To Move</p>
          <h3 class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink">建议的起步顺序</h3>
          <div class="surface-muted mt-6 divide-y divide-slate-200/70 overflow-hidden">
            <div v-for="step in steps" :key="step.index" class="px-4 py-4">
              <div class="flex items-start gap-4">
                <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-accent text-sm font-semibold text-white">
                  {{ step.index }}
                </div>
                <div>
                  <div class="font-semibold text-ink">{{ step.title }}</div>
                  <div class="mt-1 text-sm leading-6 text-slate-500">{{ step.desc }}</div>
                </div>
              </div>
            </div>
          </div>
        </article>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { computed, nextTick, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { fetchDashboardOverviewApi } from '@/api/dashboard'
import type { DashboardOverview, RecentInterviewItem } from '@/types/api'
import { useAuthStore } from '@/stores/auth'
import { storage } from '@/utils/storage'

const authStore = useAuthStore()
const loading = ref(true)
const guideDismissed = ref(false)
const radarChartRef = ref<HTMLElement | null>(null)
let radarChart: echarts.ECharts | null = null
const overview = ref<DashboardOverview>({
  learningCount: 0,
  averageScore: 0,
  wrongCount: 0,
  planCompletionRate: 0,
  recentInterviews: [],
  weakPoints: [],
  firstVisit: true
})

const metrics = computed(() => [
  { label: '学习总次数', value: String(overview.value.learningCount), desc: '问答与面试行为会共同构成你的学习节奏。' },
  { label: '平均面试分', value: formatScore(overview.value.averageScore), desc: '当前按面试作答记录聚合，没有记录时保持真实 0 值。' },
  { label: '错题数量', value: String(overview.value.wrongCount), desc: '低分题会自动沉淀到错题本，成为后续复习素材。' },
  { label: '计划完成率', value: `${overview.value.planCompletionRate}%`, desc: '当前激活计划下，已完成任务在总任务中的占比。' }
])

const quickActions = [
  { to: '/chat', label: '开始问答', kicker: 'Ask', desc: '先提一个问题，让系统开始积累你的知识盲点。' },
  { to: '/interview', label: '开始面试', kicker: 'Interview', desc: '完成一场短面试，首页就能获得更有用的分数和结果。' },
  { to: '/wrong', label: '查看错题', kicker: 'Review', desc: '把低分答案转成可追踪的复习资产。' },
  { to: '/plan', label: '生成计划', kicker: 'Plan', desc: '根据真实弱点拆出一个可以执行的学习节奏。' }
]

const steps = [
  { index: '01', title: '先问一个问题', desc: '普通问答和知识问答都可以作为你的第一条学习记录。' },
  { index: '02', title: '做一场短面试', desc: '3-5 题就足够生成初始分数、点评和错题。' },
  { index: '03', title: '回看错题', desc: '把失分点从即时回答转成可追踪的复习清单。' },
  { index: '04', title: '生成计划', desc: '让首页开始显示任务完成率，而不是停留在单次练习。' }
]

const showGuideCard = computed(() => {
  const userId = authStore.user?.id
  if (!userId || guideDismissed.value || !overview.value.firstVisit) return false
  return !storage.getGuideSeen(userId)
})

const dismissGuide = () => {
  if (authStore.user?.id) {
    storage.setGuideSeen(authStore.user.id)
  }
  guideDismissed.value = true
}

const loadOverview = async () => {
  loading.value = true
  try {
    const response = await fetchDashboardOverviewApi()
    overview.value = response.data
  } catch {
    ElMessage.error('首页数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const formatScore = (score: number) => {
  return Number.isInteger(score) ? String(score) : score.toFixed(2)
}

const formatDate = (dateTime: string) => {
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(dateTime))
}

const interviewTitle = (interview: RecentInterviewItem) => {
  return interview.status === 'finished' ? '已完成模拟面试' : '进行中的模拟面试'
}

const statusLabel = (status: string) => {
  return status === 'finished' ? 'finished' : status
}

const renderRadarChart = () => {
  if (!radarChartRef.value || !overview.value.weakPoints.length) return
  if (radarChart) radarChart.dispose()
  radarChart = echarts.init(radarChartRef.value)
  const points = overview.value.weakPoints
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

watch(() => overview.value.weakPoints, () => {
  nextTick(renderRadarChart)
}, { deep: true })

onBeforeUnmount(() => {
  if (radarChart) radarChart.dispose()
})

onMounted(() => {
  void loadOverview()
})
</script>
