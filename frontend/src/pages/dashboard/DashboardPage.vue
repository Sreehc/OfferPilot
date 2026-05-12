<template>
  <div class="dashboard-canvas space-y-6 pb-6">
    <DashboardGuideCard v-if="showGuideCard" :actions="guideActions" @dismiss="dismissGuide" />

    <section v-if="loading" class="shell-section-card min-h-[320px] p-6 sm:p-8">
      <div class="skeleton-line h-4 w-24"></div>
      <div class="skeleton-line mt-6 h-14 w-3/4 rounded-[var(--radius-md)]"></div>
      <div class="skeleton-line mt-4 h-4 w-full"></div>
      <div class="mt-8 grid gap-3 sm:grid-cols-3">
        <div v-for="index in 3" :key="index" class="skeleton-line h-24 rounded-[var(--radius-md)]"></div>
      </div>
    </section>

    <template v-else>
      <section class="grid gap-4">
        <div class="space-y-4">
          <section
            class="shell-section-card relative overflow-hidden p-5 sm:p-6"
            :class="{ 'border-[var(--bc-line-hot)]': primaryMission.urgent }"
          >
            <div
              class="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(var(--bc-accent-rgb),0.1),transparent_30%),radial-gradient(circle_at_92%_12%,rgba(var(--bc-cyan-rgb),0.08),transparent_24%)]"
            ></div>
            <div class="relative grid gap-5 lg:grid-cols-[minmax(0,1fr)_236px] lg:items-end">
              <div>
                <div class="flex flex-wrap items-center gap-2">
                  <span class="hard-chip">连续 {{ reviewStats.currentStreak }} 天</span>
                  <span class="hard-chip">待复习 {{ todayReviewCards }} 张</span>
                </div>

                <div class="mt-5 max-w-[42rem]">
                  <h2
                    class="font-display text-4xl font-semibold leading-[1.02] tracking-[-0.04em] text-ink sm:text-5xl"
                  >
                    {{ primaryMission.title }}
                  </h2>
                  <p class="mt-4 max-w-2xl text-sm leading-7 sm:text-base" style="color: var(--bc-ink-secondary)">
                    {{ primaryMission.description }}
                  </p>
                </div>

                <div class="mt-5 flex flex-wrap items-center gap-3">
                  <RouterLink :to="primaryMission.to" class="hard-button-primary">
                    {{ primaryMission.cta }}
                  </RouterLink>
                  <RouterLink to="/review" class="hard-button-secondary">去复习</RouterLink>
                </div>
              </div>

              <div class="dashboard-hero-summary">
                <div class="dashboard-hero-summary__item">
                  <span>今日任务</span>
                  <strong>{{ todayCardTotal > 0 ? `${todayCardTotal} 张` : '待生成' }}</strong>
                </div>
                <div class="dashboard-hero-summary__item">
                  <span>完成率</span>
                  <strong>{{ formatPercent(todayCardCompletionRate) }}%</strong>
                </div>
                <div class="dashboard-hero-summary__item">
                  <span>复习负债</span>
                  <strong>{{ reviewDebtCount > 0 ? `${reviewDebtCount} 项` : '无积压' }}</strong>
                </div>
              </div>
            </div>
          </section>

          <article class="shell-section-card p-4 sm:p-5">
            <div class="flex items-center justify-between gap-3">
              <p class="section-kicker">下一步动作</p>
              <RouterLink to="/analytics" class="text-sm font-semibold text-accent hover:underline">
                更多数据 →
              </RouterLink>
            </div>
            <div class="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <RouterLink v-for="action in quickActions" :key="action.to" :to="action.to" class="dashboard-action-card">
                <p class="dashboard-action-card__label">{{ action.label }}</p>
                <h3 class="dashboard-action-card__title">{{ action.title }}</h3>
              </RouterLink>
            </div>
          </article>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import DashboardGuideCard from './DashboardGuideCard.vue'
import { fetchDashboardOverviewApi } from '@/api/dashboard'
import { fetchReviewStatsApi } from '@/api/review'
import type { DashboardOverview, ReviewStats } from '@/types/api'
import { useAuthStore } from '@/stores/auth'
import { storage } from '@/utils/storage'

const authStore = useAuthStore()
const loading = ref(true)
const reviewStats = ref<ReviewStats>({ totalReviews: 0, currentStreak: 0, todayPending: 0 })
const guideDismissed = ref(false)
const overview = ref<DashboardOverview>({
  learningCount: 0,
  averageScore: 0,
  wrongCount: 0,
  recentInterviews: [],
  weakPoints: [],
  firstVisit: true,
  todayLearnCards: 0,
  todayReviewCards: 0,
  todayCompletedCards: 0,
  todayCardCompletionRate: 0,
  masteredCardCount: 0,
  reviewDebtCount: 0,
  studyStreak: 0,
  categoryMasterySummary: []
})

const todayLearnCards = computed(() => overview.value.todayLearnCards ?? 0)
const todayReviewCards = computed(() => overview.value.todayReviewCards ?? 0)
const todayCardCompletionRate = computed(() => overview.value.todayCardCompletionRate ?? 0)
const reviewDebtCount = computed(() => overview.value.reviewDebtCount ?? 0)
const todayCardTotal = computed(() => todayLearnCards.value + todayReviewCards.value)

const primaryMission = computed(() => {
  if (todayCardTotal.value > 0) {
    return {
      to: '/cards',
      title: '今天先完成今日卡片',
      description: `今天共有 ${todayCardTotal.value} 张卡片要处理，其中待学 ${todayLearnCards.value} 张、待复习 ${todayReviewCards.value} 张。`,
      cta: '开始今日卡片',
      urgent: todayReviewCards.value > 0 || reviewDebtCount.value > 0
    }
  }

  return {
    to: '/knowledge',
    title: '上传资料，开始学习',
    description: '你还没有学习资料。上传文档后 AI 会自动生成卡片，开始间隔记忆。',
    cta: '去知识库',
    urgent: false
  }
})

const quickActions = computed(() => [
  {
    to: '/cards',
    label: '今日卡片',
    title: todayCardTotal.value > 0 ? `处理今天的 ${todayCardTotal.value} 张卡片` : '开始今天的卡片任务'
  },
  {
    to: '/review?tab=all',
    label: '复习',
    title: reviewDebtCount.value > 0 ? `清理 ${reviewDebtCount.value} 项积压` : '查看今天的复习'
  },
  {
    to: '/knowledge',
    label: '知识库',
    title: '上传资料或生成卡片'
  },
  {
    to: '/chat',
    label: '问答',
    title: '继续追问没吃透的问题'
  }
])

const guideActions = computed(() => [
  {
    to: '/knowledge',
    label: '去知识库'
  }
])

const showGuideCard = computed(() => {
  const userId = authStore.user?.id
  if (!userId || guideDismissed.value || !overview.value.firstVisit) return false
  if (todayCardTotal.value === 0) return false
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

const loadReviewStats = async () => {
  try {
    const response = await fetchReviewStatsApi()
    reviewStats.value = response.data
  } catch {
    // silently fail
  }
}

const formatPercent = (value: number): string => {
  return Number.isInteger(value) ? String(value) : value.toFixed(0)
}

onMounted(() => {
  void loadOverview()
  void loadReviewStats()
})
</script>

<style scoped>
.dashboard-action-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.08), transparent 56%), var(--bc-surface-muted);
  box-shadow: var(--bc-shadow-soft);
  padding: 1rem 1.1rem;
  transition:
    transform var(--motion-fast) var(--ease-hard),
    box-shadow var(--motion-fast) var(--ease-hard),
    background-color var(--motion-fast) var(--ease-hard);
}

.dashboard-action-card:hover {
  transform: translateY(-1px);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.12), transparent 56%), var(--bc-surface-card-hover);
  box-shadow: var(--bc-shadow-hover);
}

.dashboard-action-card__label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--bc-ink-secondary);
}

.dashboard-action-card__title {
  margin-top: 0.8rem;
  font-size: 1.1rem;
  font-weight: 700;
  line-height: 1.35;
  color: var(--bc-ink);
}

.dashboard-hero-summary {
  display: grid;
  gap: 0.75rem;
}

.dashboard-hero-summary__item {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.16);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.12), transparent 72%), var(--bc-surface-muted);
  box-shadow: var(--bc-shadow-soft);
  padding: 0.9rem 1rem;
}

.dashboard-hero-summary__item span {
  display: block;
  font-size: 0.78rem;
  color: var(--bc-ink-secondary);
}

.dashboard-hero-summary__item strong {
  display: block;
  margin-top: 0.45rem;
  font-size: 1.2rem;
  line-height: 1.2;
  color: var(--bc-ink);
}
</style>
