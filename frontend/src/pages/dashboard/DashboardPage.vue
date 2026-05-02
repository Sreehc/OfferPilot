<template>
  <div class="space-y-6">
    <DashboardGuideCard
      v-if="showGuideCard"
      :actions="quickActions"
      @dismiss="dismissGuide"
    />

    <section v-if="loading" class="grid gap-4 xl:grid-cols-4 md:grid-cols-2">
      <article v-for="index in 4" :key="index" class="metric-card">
        <div class="h-4 w-20 animate-pulse bg-slate-200 dark:bg-slate-700" style="border-radius: var(--radius-sm);"></div>
        <div class="mt-5 h-12 w-24 animate-pulse bg-slate-200 dark:bg-slate-700" style="border-radius: var(--radius-md);"></div>
        <div class="mt-3 h-4 w-full animate-pulse bg-slate-100 dark:bg-slate-800" style="border-radius: var(--radius-sm);"></div>
      </article>
    </section>

    <template v-else>
      <DashboardMetrics :metrics="metrics" />

      <section class="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
        <DashboardInterviews :interviews="overview.recentInterviews" />
        <DashboardWeakPoints :weak-points="overview.weakPoints" :plan-completion-rate="overview.planCompletionRate" :plan-health-score="overview.planHealthScore ?? 100" />
      </section>

      <DashboardInterviewTrend :trend-data="trendData" :loading="trendLoading" />

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
              <div class="text-xs uppercase tracking-[0.28em] text-slate-500 dark:text-slate-400">{{ action.kicker }}</div>
              <div class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">{{ action.label }}</div>
              <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">{{ action.desc }}</p>
            </RouterLink>
          </div>
        </article>

        <article class="paper-panel p-6">
          <p class="section-kicker">How To Move</p>
          <h3 class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink">建议的起步顺序</h3>
          <div class="surface-muted mt-6 divide-y divide-slate-200/70 dark:divide-slate-700/70 overflow-hidden">
            <div v-for="step in steps" :key="step.index" class="px-4 py-4">
              <div class="flex items-start gap-4">
                <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-accent text-sm font-semibold text-white">
                  {{ step.index }}
                </div>
                <div>
                  <div class="font-semibold text-ink">{{ step.title }}</div>
                  <div class="mt-1 text-sm leading-6 text-slate-500 dark:text-slate-400">{{ step.desc }}</div>
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
import { computed, onMounted, ref } from 'vue'
import DashboardGuideCard from './DashboardGuideCard.vue'
import DashboardMetrics from './DashboardMetrics.vue'
import DashboardInterviews from './DashboardInterviews.vue'
import DashboardWeakPoints from './DashboardWeakPoints.vue'
import DashboardInterviewTrend from './DashboardInterviewTrend.vue'
import { fetchDashboardOverviewApi } from '@/api/dashboard'
import { fetchInterviewTrendApi } from '@/api/interview'
import type { DashboardOverview, InterviewHistoryItem } from '@/types/api'
import { useAuthStore } from '@/stores/auth'
import { storage } from '@/utils/storage'

const authStore = useAuthStore()
const loading = ref(true)
const trendLoading = ref(true)
const trendData = ref<InterviewHistoryItem[]>([])
const guideDismissed = ref(false)
const overview = ref<DashboardOverview>({
  learningCount: 0,
  averageScore: 0,
  wrongCount: 0,
  planCompletionRate: 0,
  planHealthScore: 100,
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

const loadTrend = async () => {
  trendLoading.value = true
  try {
    const response = await fetchInterviewTrendApi(20)
    trendData.value = response.data || []
  } catch {
    // Silently fail — trend is supplementary
  } finally {
    trendLoading.value = false
  }
}

const formatScore = (score: number): string => {
  return Number.isInteger(score) ? String(score) : score.toFixed(2)
}

onMounted(() => {
  void loadOverview()
  void loadTrend()
})
</script>
