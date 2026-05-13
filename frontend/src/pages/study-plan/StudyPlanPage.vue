<template>
  <div class="space-y-6">
    <AppShellHeader>
      <template #actions>
        <RouterLink
          to="/question"
          class="hard-button-primary"
        >
          去题库训练
        </RouterLink>
        <RouterLink
          to="/interview"
          class="hard-button-secondary"
        >
          安排模拟面试
        </RouterLink>
      </template>
    </AppShellHeader>

    <section class="shell-section-card plan-stage-card p-5 sm:p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div class="max-w-3xl">
          <div class="flex flex-wrap gap-2">
            <span class="hard-chip">Phase 0 入口已建立</span>
            <span class="detail-pill">Phase 3 接正式计划表与任务状态</span>
          </div>
          <h2 class="mt-5 font-display text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">
            先把训练动作排进路线，再逐阶段接持久化能力
          </h2>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-secondary">
            当前页面先承接“学习计划”入口、节奏展示和建议动作。正式的 7 / 14 / 30 天计划、每日任务勾选、完成状态和统计口径将在 Phase 3 一并落地。
          </p>
        </div>

        <div class="plan-stage-signals">
          <article class="plan-stage-signals__item">
            <span>待巩固</span>
            <strong>{{ reviewPending }}</strong>
          </article>
          <article class="plan-stage-signals__item">
            <span>弱项数</span>
            <strong>{{ weakPointCount }}</strong>
          </article>
          <article class="plan-stage-signals__item">
            <span>连续天数</span>
            <strong>{{ streakDays }}</strong>
          </article>
        </div>
      </div>
    </section>

    <section class="grid gap-4 xl:grid-cols-3">
      <article
        v-for="plan in planTracks"
        :key="plan.title"
        class="shell-section-card p-5 sm:p-6"
      >
        <p class="section-kicker">
          {{ plan.length }}
        </p>
        <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
          {{ plan.title }}
        </h3>
        <p class="mt-3 text-sm leading-7 text-secondary">
          {{ plan.description }}
        </p>
        <ul class="mt-5 space-y-3 text-sm leading-7 text-secondary">
          <li
            v-for="step in plan.steps"
            :key="step"
            class="plan-bullet"
          >
            {{ step }}
          </li>
        </ul>
      </article>
    </section>

    <section class="grid gap-4 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
      <article class="shell-section-card p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="section-kicker">
              今日建议
            </p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
              先推进这几步
            </h3>
          </div>
          <span class="detail-pill">当前为过渡版计划视图</span>
        </div>

        <div class="mt-5 space-y-3">
          <article
            v-for="task in suggestedTasks"
            :key="task.title"
            class="plan-task-card"
          >
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">
                  {{ task.label }}
                </div>
                <h4 class="mt-2 text-lg font-semibold text-ink">
                  {{ task.title }}
                </h4>
                <p class="mt-2 text-sm leading-7 text-secondary">
                  {{ task.description }}
                </p>
              </div>
              <RouterLink
                :to="task.to"
                class="hard-button-secondary text-sm"
              >
                {{ task.cta }}
              </RouterLink>
            </div>
          </article>
        </div>
      </article>

      <article class="shell-section-card p-5 sm:p-6">
        <p class="section-kicker">
          当前信号
        </p>
        <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
          为正式学习计划预热的数据
        </h3>

        <div class="mt-5 space-y-4">
          <div class="plan-signal-row">
            <span>今日卡片任务</span>
            <strong>{{ todayCardTotal }}</strong>
          </div>
          <div class="plan-signal-row">
            <span>最近面试场次</span>
            <strong>{{ recentInterviewCount }}</strong>
          </div>
          <div class="plan-signal-row">
            <span>重点弱项</span>
            <strong>{{ topWeakPoint }}</strong>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppShellHeader from '@/components/AppShellHeader.vue'
import { fetchDashboardOverviewApi } from '@/api/dashboard'
import { fetchReviewStatsApi } from '@/api/review'
import type { DashboardOverview, ReviewStats } from '@/types/api'

const overview = ref<DashboardOverview | null>(null)
const reviewStats = ref<ReviewStats | null>(null)

const planTracks = [
  {
    length: '7 天冲刺',
    title: '补齐基础高频题',
    description: '适合刚进入求职准备期，先完成知识盘点、基础刷题和第一次模拟面试。',
    steps: ['题库刷一轮高频基础题', '知识库补齐核心资料', '完成至少 1 次模拟面试']
  },
  {
    length: '14 天强化',
    title: '压缩薄弱点并稳定输出',
    description: '把错题、问答和模拟面试结果整合起来，开始形成稳定答题结构。',
    steps: ['围绕弱项做专题刷题', '用问答页补充标准回答', '安排 2 到 3 次岗位方向模拟面试']
  },
  {
    length: '30 天闭环',
    title: '联动简历与投递准备',
    description: '正式版本会在这里接上简历助手、投递管理和真实面试复盘能力。',
    steps: ['整理项目与简历素材', '围绕目标 JD 准备项目追问', '建立投递节奏与复盘清单']
  }
]

const reviewPending = computed(() => reviewStats.value?.todayPending ?? overview.value?.reviewDebtCount ?? 0)
const weakPointCount = computed(() => overview.value?.weakPoints?.length ?? 0)
const streakDays = computed(() => reviewStats.value?.currentStreak ?? overview.value?.studyStreak ?? 0)
const todayCardTotal = computed(() => (overview.value?.todayLearnCards ?? 0) + (overview.value?.todayReviewCards ?? 0))
const recentInterviewCount = computed(() => overview.value?.recentInterviews?.length ?? 0)
const topWeakPoint = computed(() => overview.value?.weakPoints?.[0]?.categoryName ?? '等待训练数据')

const suggestedTasks = computed(() => [
  {
    label: '题库',
    title: weakPointCount.value > 0 ? `先补 ${topWeakPoint.value} 方向` : '先建立题库训练节奏',
    description:
      weakPointCount.value > 0
        ? '从当前弱项开始做结构化刷题，把后续问答和面试动作建立在同一批知识点上。'
        : '先从题库里建立一轮基础训练，再决定后续的问答和模拟面试节奏。',
    to: '/question',
    cta: '去题库'
  },
  {
    label: '面试',
    title: recentInterviewCount.value > 0 ? '继续下一场模拟面试' : '开始第一场模拟面试',
    description: '用一次面试结果验证当前题库训练是否已经能转化为可表达、可追问的回答结构。',
    to: '/interview',
    cta: '去面试'
  },
  {
    label: '巩固',
    title: reviewPending.value > 0 ? `清理 ${reviewPending.value} 个待巩固项` : '保持训练节奏不中断',
    description: '卡片和复习已经降级为辅助入口，但仍然是稳定训练节奏和压缩遗忘曲线的重要补充。',
    to: '/review',
    cta: '去复习'
  }
])

onMounted(async () => {
  try {
    const [overviewResponse, reviewResponse] = await Promise.all([fetchDashboardOverviewApi(), fetchReviewStatsApi()])
    overview.value = overviewResponse.data
    reviewStats.value = reviewResponse.data
  } catch {
    overview.value = null
    reviewStats.value = null
  }
})
</script>

<style scoped>
.plan-stage-card {
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.12), transparent 28%),
    radial-gradient(circle at 84% 22%, rgba(var(--bc-cyan-rgb), 0.12), transparent 20%),
    var(--bc-surface-card);
}

.plan-stage-signals {
  display: grid;
  gap: 0.75rem;
}

.plan-stage-signals__item {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.36);
  padding: 0.95rem 1rem;
  backdrop-filter: blur(10px);
}

.plan-stage-signals__item span,
.plan-signal-row span {
  display: block;
  font-size: 0.8rem;
  color: var(--bc-ink-secondary);
}

.plan-stage-signals__item strong,
.plan-signal-row strong {
  display: block;
  margin-top: 0.45rem;
  font-size: 1.45rem;
  line-height: 1.1;
  color: var(--bc-ink);
}

.plan-bullet {
  position: relative;
  padding-left: 1rem;
}

.plan-bullet::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.75rem;
  height: 0.35rem;
  width: 0.35rem;
  border-radius: 999px;
  background: var(--bc-accent);
}

.plan-task-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 1rem 1.05rem;
}

.plan-signal-row {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid var(--bc-border-subtle);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.06), transparent 58%), var(--bc-surface-muted);
  padding: 1rem;
}

@media (min-width: 1024px) {
  .plan-stage-signals {
    grid-template-columns: repeat(3, minmax(0, 164px));
  }
}
</style>
