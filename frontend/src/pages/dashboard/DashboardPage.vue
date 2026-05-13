<template>
  <div class="dashboard-canvas space-y-6 pb-6">
    <section
      v-if="loading"
      class="shell-section-card min-h-[320px] p-6 sm:p-8"
    >
      <div class="skeleton-line h-4 w-24" />
      <div class="skeleton-line mt-6 h-14 w-3/4 rounded-[var(--radius-md)]" />
      <div class="skeleton-line mt-4 h-4 w-full" />
      <div class="mt-8 grid gap-3 sm:grid-cols-3">
        <div
          v-for="index in 3"
          :key="index"
          class="skeleton-line h-24 rounded-[var(--radius-md)]"
        />
      </div>
    </section>

    <template v-else>
      <section
        class="dashboard-hero shell-section-card relative overflow-hidden p-5 sm:p-6"
        :class="{ 'border-[var(--bc-line-hot)]': primaryMission.urgent }"
      >
        <div class="dashboard-hero__mesh" />
        <div class="relative grid gap-5 xl:grid-cols-[minmax(0,1fr)_320px] xl:items-end">
          <div>
            <div class="flex flex-wrap gap-2">
              <span class="hard-chip">连续 {{ reviewStats.currentStreak }} 天</span>
              <span class="hard-chip">待巩固 {{ reviewPending }} 项</span>
              <span class="detail-pill">Phase 0 已切换为求职训练主线</span>
            </div>

            <div class="mt-6 max-w-[48rem]">
              <p class="section-kicker">
                求职训练工作台
              </p>
              <h1 class="font-display text-4xl font-semibold leading-[1.02] tracking-[-0.05em] text-ink sm:text-5xl">
                {{ primaryMission.title }}
              </h1>
              <p class="mt-4 text-sm leading-7 text-secondary sm:text-base">
                {{ primaryMission.description }}
              </p>
            </div>

            <div class="mt-6 flex flex-wrap gap-3">
              <RouterLink
                :to="primaryMission.to"
                class="hard-button-primary"
              >
                {{ primaryMission.cta }}
              </RouterLink>
              <RouterLink
                to="/study-plan"
                class="hard-button-secondary"
              >
                查看训练计划入口
              </RouterLink>
            </div>
          </div>

          <div class="dashboard-hero__summary">
            <article class="dashboard-hero__summary-item">
              <span>最近均分</span>
              <strong>{{ formatScore(overview.averageScore) }}</strong>
            </article>
            <article class="dashboard-hero__summary-item">
              <span>最近面试</span>
              <strong>{{ overview.recentInterviews.length }}</strong>
            </article>
            <article class="dashboard-hero__summary-item">
              <span>重点弱项</span>
              <strong>{{ topWeakPoint }}</strong>
            </article>
          </div>
        </div>
      </section>

      <DashboardMetrics :metrics="trainingMetrics" />

      <section class="shell-section-card p-5 sm:p-6">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <p class="section-kicker">
              主线路径
            </p>
            <h2 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
              现在围绕这些模块推进求职准备
            </h2>
          </div>
          <span class="detail-pill">卡片与复习已降级为辅助入口</span>
        </div>

        <div class="dashboard-route-grid mt-5">
          <RouterLink
            v-for="item in primaryRoutes"
            :key="item.to"
            :to="item.to"
            class="dashboard-route-card"
          >
            <div class="flex items-center justify-between gap-3">
              <span class="dashboard-route-card__label">{{ item.label }}</span>
              <span class="dashboard-route-card__status">{{ item.status }}</span>
            </div>
            <h3 class="dashboard-route-card__title">
              {{ item.title }}
            </h3>
            <p class="dashboard-route-card__description">
              {{ item.description }}
            </p>
          </RouterLink>
        </div>
      </section>

      <section class="grid gap-4 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
        <article class="shell-section-card p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="section-kicker">
                当前重点
              </p>
              <h2 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
                优先补齐的训练方向
              </h2>
            </div>
            <RouterLink
              to="/question"
              class="accent-link text-sm font-semibold"
            >
              去题库 →
            </RouterLink>
          </div>

          <div
            v-if="overview.weakPoints.length"
            class="mt-5 space-y-3"
          >
            <article
              v-for="item in overview.weakPoints.slice(0, 4)"
              :key="item.categoryName"
              class="dashboard-weak-card"
            >
              <div class="flex items-center justify-between gap-4">
                <div>
                  <div class="text-sm font-semibold text-ink">
                    {{ item.categoryName }}
                  </div>
                  <div class="mt-1 text-xs text-secondary">
                    错题 {{ item.wrongCount }} · 当前得分 {{ formatScore(item.score) }}
                  </div>
                </div>
                <RouterLink
                  to="/chat"
                  class="hard-button-secondary text-sm"
                >
                  扩展回答
                </RouterLink>
              </div>
            </article>
          </div>

          <EmptyState
            v-else
            icon="chart"
            title="弱项数据还不够"
            description="完成题库训练和模拟面试后，这里会优先显示需要补齐的方向。"
            compact
            class="mt-5"
          />
        </article>

        <article class="shell-section-card p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="section-kicker">
                辅助入口
              </p>
              <h2 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
                仍然保留但不再主导心智
              </h2>
            </div>
          </div>

          <div class="mt-5 space-y-3">
            <RouterLink
              v-for="item in secondaryRoutes"
              :key="item.to"
              :to="item.to"
              class="dashboard-secondary-card"
            >
              <div class="dashboard-secondary-card__topline">
                <span>{{ item.label }}</span>
                <span>{{ item.hint }}</span>
              </div>
              <h3 class="dashboard-secondary-card__title">
                {{ item.title }}
              </h3>
            </RouterLink>
          </div>
        </article>
      </section>

      <section class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_380px]">
        <DashboardInterviewTrend
          :trend-data="interviewTrendData"
          :loading="false"
        />
        <DashboardInterviews :interviews="overview.recentInterviews" />
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { fetchDashboardOverviewApi } from '@/api/dashboard'
import { fetchReviewStatsApi } from '@/api/review'
import type { DashboardOverview, ReviewStats } from '@/types/api'
import DashboardInterviews from './DashboardInterviews.vue'
import DashboardInterviewTrend from './DashboardInterviewTrend.vue'
import DashboardMetrics from './DashboardMetrics.vue'

const loading = ref(true)
const reviewStats = ref<ReviewStats>({ totalReviews: 0, currentStreak: 0, todayPending: 0 })
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

const reviewPending = computed(() => reviewStats.value.todayPending ?? overview.value.reviewDebtCount ?? 0)
const todayCardTotal = computed(() => (overview.value.todayLearnCards ?? 0) + (overview.value.todayReviewCards ?? 0))
const topWeakPoint = computed(() => overview.value.weakPoints[0]?.categoryName ?? '等待训练数据')

const trainingMetrics = computed(() => [
  {
    label: '训练资产',
    value: String(overview.value.learningCount ?? 0),
    desc: '当前沉淀的学习记录和训练积累'
  },
  {
    label: '平均表现',
    value: formatScore(overview.value.averageScore),
    desc: '基于现有面试与训练结果的阶段性均分'
  },
  {
    label: '待巩固内容',
    value: String(overview.value.wrongCount ?? 0),
    desc: '需要继续补强的错题与低掌握内容'
  },
  {
    label: '连续训练',
    value: String(reviewStats.value.currentStreak ?? overview.value.studyStreak ?? 0),
    desc: '连续推进求职训练的天数'
  }
])

const primaryMission = computed(() => {
  const weakPoint = overview.value.weakPoints[0]

  if (weakPoint) {
    return {
      to: '/question',
      title: `先补 ${weakPoint.categoryName} 这一块`,
      description: `当前 ${weakPoint.categoryName} 是最需要优先补齐的方向。先用题库做结构化训练，再去问答页扩展回答，最后用模拟面试验证表达质量。`,
      cta: '去题库训练',
      urgent: weakPoint.wrongCount > 0
    }
  }

  if (!overview.value.recentInterviews.length) {
    return {
      to: '/interview',
      title: '先开始第一场模拟面试',
      description: '首页已经切到求职训练主线。用第一场模拟面试建立基线，后续题库、问答和计划模块都会围绕它来组织训练节奏。',
      cta: '开始模拟面试',
      urgent: false
    }
  }

  if (reviewPending.value > 0 || todayCardTotal.value > 0) {
    return {
      to: '/review',
      title: '把待巩固内容先清掉',
      description: `当前仍有 ${reviewPending.value} 项待复习内容和 ${todayCardTotal.value} 张卡片任务。它们不再是主线，但仍然是维持训练节奏的重要辅助动作。`,
      cta: '去复习巩固',
      urgent: reviewPending.value > 0
    }
  }

  return {
    to: '/study-plan',
    title: '开始规划下一阶段训练路线',
    description: '学习计划模块的正式闭环会在 Phase 3 落地；当前入口已经建立，可以先按阶段路线整理下一步训练方向。',
    cta: '查看学习计划',
    urgent: false
  }
})

const primaryRoutes = computed(() => [
  {
    to: '/question',
    label: '题库',
    title: '结构化刷题',
    description: '按技术分类、难度和岗位方向组织训练，形成稳定答题底座。',
    status: '已接主导航'
  },
  {
    to: '/knowledge',
    label: '知识库',
    title: '统一资料中心',
    description: '承接系统资料、个人文档和后续简历、JD、项目语料。',
    status: '已接主导航'
  },
  {
    to: '/chat',
    label: '问答',
    title: '带引用知识问答',
    description: '对题目、项目和资料继续深挖，让回答结构更完整。',
    status: '已接主导航'
  },
  {
    to: '/interview',
    label: '面试',
    title: '模拟面试',
    description: '用文字或语音训练真实表达，把知识点转化为可输出的答案。',
    status: '已接主导航'
  },
  {
    to: '/study-plan',
    label: '计划',
    title: '学习计划入口',
    description: '先搭页面和节奏入口，后续接正式计划、任务和统计。',
    status: 'Phase 3'
  },
  {
    to: '/resume',
    label: '简历',
    title: '简历助手入口',
    description: '为简历解析、项目问答和自我介绍保留独立主路径。',
    status: 'Phase 4'
  },
  {
    to: '/applications',
    label: '投递',
    title: '投递管理入口',
    description: '为 JD 分析、投递看板、真实面试与复盘建立主路径。',
    status: 'Phase 5'
  },
  {
    to: '/analytics',
    label: '洞察',
    title: '训练结果观察',
    description: '查看趋势、弱项和节奏变化，辅助决定下一步训练动作。',
    status: '已保留'
  }
])

const secondaryRoutes = computed(() => [
  {
    to: '/cards',
    label: '卡片强化',
    title: todayCardTotal.value > 0 ? `当前还有 ${todayCardTotal.value} 张卡片待处理` : '用卡片做专项强化',
    hint: '辅助'
  },
  {
    to: '/review',
    label: '复习巩固',
    title: reviewPending.value > 0 ? `当前还有 ${reviewPending.value} 项待巩固` : '处理错题与到期复习',
    hint: '辅助'
  },
  {
    to: '/community',
    label: '社区',
    title: '浏览问题、回答和排行榜',
    hint: '冻结'
  }
])

const interviewTrendData = computed(() =>
  overview.value.recentInterviews.map((item) => ({
    sessionId: item.sessionId,
    direction: item.direction,
    totalScore: item.totalScore,
    startTime: item.finishedAt
  }))
)

const loadOverview = async () => {
  loading.value = true
  try {
    const [overviewResponse, reviewResponse] = await Promise.all([fetchDashboardOverviewApi(), fetchReviewStatsApi()])
    overview.value = overviewResponse.data
    reviewStats.value = reviewResponse.data
  } catch {
    ElMessage.error('首页数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function formatScore(value: number): string {
  if (!Number.isFinite(value)) return '0'
  return Number.isInteger(value) ? String(value) : value.toFixed(0)
}

onMounted(() => {
  void loadOverview()
})
</script>

<style scoped>
.dashboard-hero {
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.12), transparent 28%),
    radial-gradient(circle at 88% 18%, rgba(var(--bc-cyan-rgb), 0.12), transparent 22%),
    var(--bc-surface-card);
}

.dashboard-hero__mesh {
  position: absolute;
  inset: 0;
  opacity: 0.35;
  background:
    linear-gradient(120deg, rgba(var(--bc-accent-rgb), 0.05), transparent 42%),
    linear-gradient(320deg, rgba(var(--bc-cyan-rgb), 0.04), transparent 38%);
}

.dashboard-hero__summary {
  display: grid;
  gap: 0.75rem;
}

.dashboard-hero__summary-item {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.16);
  background: rgba(255, 255, 255, 0.38);
  padding: 0.95rem 1rem;
  backdrop-filter: blur(10px);
}

.dashboard-hero__summary-item span {
  display: block;
  font-size: 0.78rem;
  color: var(--bc-ink-secondary);
}

.dashboard-hero__summary-item strong {
  display: block;
  margin-top: 0.45rem;
  font-size: 1.2rem;
  line-height: 1.15;
  color: var(--bc-ink);
}

.dashboard-route-grid {
  display: grid;
  gap: 0.9rem;
}

.dashboard-route-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.07), transparent 56%), var(--bc-surface-muted);
  box-shadow: var(--bc-shadow-soft);
  padding: 1rem 1.05rem;
  transition:
    transform var(--motion-fast) var(--ease-hard),
    box-shadow var(--motion-fast) var(--ease-hard);
}

.dashboard-route-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--bc-shadow-hover);
}

.dashboard-route-card__label,
.dashboard-route-card__status,
.dashboard-secondary-card__topline {
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dashboard-route-card__label {
  color: var(--bc-ink-secondary);
}

.dashboard-route-card__status {
  color: var(--bc-accent);
}

.dashboard-route-card__title,
.dashboard-secondary-card__title {
  margin-top: 0.85rem;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.dashboard-route-card__description {
  margin-top: 0.7rem;
  font-size: 0.92rem;
  line-height: 1.7;
  color: var(--bc-ink-secondary);
}

.dashboard-weak-card,
.dashboard-secondary-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 1rem;
}

.dashboard-secondary-card__topline {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  color: var(--bc-ink-secondary);
}

@media (min-width: 960px) {
  .dashboard-route-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1400px) {
  .dashboard-route-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
