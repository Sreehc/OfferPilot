<template>
  <section v-if="insights.length > 0" class="shell-section-card p-4 sm:p-5">
    <p class="section-kicker">本周洞察</p>
    <div class="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      <RouterLink
        v-for="item in insights"
        :key="item.key"
        :to="item.to"
        class="insight-card"
        :class="item.toneClass"
      >
        <div class="flex items-start gap-3">
          <span class="insight-card__icon" :class="item.iconClass">{{ item.icon }}</span>
          <div class="min-w-0">
            <p class="text-sm font-semibold text-ink">{{ item.title }}</p>
            <p class="mt-1 text-xs leading-5 text-secondary">{{ item.description }}</p>
          </div>
        </div>
      </RouterLink>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import type { LearningInsights } from '@/types/api'

const props = defineProps<{ data: LearningInsights }>()

type Insight = {
  key: string
  icon: string
  title: string
  description: string
  to: RouteLocationRaw
  toneClass: string
  iconClass: string
}

const buildSeededProductLink = (path: string, workflow: 'applications' | 'resume', note: string): RouteLocationRaw => ({
  path,
  query: {
    seedWorkflow: workflow,
    seedNote: note
  }
})

const insights = computed<Insight[]>(() => {
  const list: Insight[] = []
  const d = props.data

  if (d.nextAction?.title && d.nextAction?.path) {
    list.push({
      key: 'next-action',
      icon: '→',
      title: d.nextAction.title,
      description: d.nextAction.description || '推进这一项，看看后续变化。',
      to: d.nextAction.path,
      toneClass: 'insight-card--info',
      iconClass: 'insight-icon--info'
    })
  }

  if (d.planExecutionStatus) {
    list.push({
      key: 'plan-status',
      icon: '□',
      title: '今天的计划',
      description: d.planExecutionStatus,
      to: '/study-plan',
      toneClass: 'insight-card--good',
      iconClass: 'insight-icon--good'
    })
  }

  if (d.applicationStatus) {
    list.push({
      key: 'application-status',
      icon: '◎',
      title: '投递进展',
      description: d.applicationStatus,
      to: buildSeededProductLink('/applications', 'applications', d.applicationStatus),
      toneClass: 'insight-card--info',
      iconClass: 'insight-icon--info'
    })
  }

  if (d.resumeReadinessStatus) {
    list.push({
      key: 'resume-status',
      icon: '◆',
      title: '简历准备',
      description: d.resumeReadinessStatus,
      to: buildSeededProductLink('/resume', 'resume', d.resumeReadinessStatus),
      toneClass: 'insight-card--info',
      iconClass: 'insight-icon--info'
    })
  }

  if (d.thisWeekAvgScore > 0 && d.lastWeekAvgScore > 0) {
    const diff = d.thisWeekAvgScore - d.lastWeekAvgScore
    if (diff < -5) {
      list.push({
        key: 'score-drop',
        icon: '▼',
        title: '本周面试分下降',
        description: `平均分 ${Math.round(d.thisWeekAvgScore)}，比上周低 ${Math.abs(Math.round(diff))} 分。建议补弱项，并安排一次模拟面试。`,
        to: '/question',
        toneClass: 'insight-card--warn',
        iconClass: 'insight-icon--warn'
      })
    } else if (diff > 5) {
      list.push({
        key: 'score-up',
        icon: '▲',
        title: '面试分数上升',
        description: `平均分 ${Math.round(d.thisWeekAvgScore)}，比上周高 ${Math.round(diff)} 分，保持节奏！`,
        to: '/analytics',
        toneClass: 'insight-card--good',
        iconClass: 'insight-icon--good'
      })
    }
  }

  if (d.reviewDebtStatus && d.reviewDebtStatus.includes('高')) {
    list.push({
      key: 'debt-high',
      icon: '!',
      title: '错题积压偏高',
      description: '处理到期错题后，安排新训练，节奏会更稳定。',
      to: '/review',
      toneClass: 'insight-card--warn',
      iconClass: 'insight-icon--warn'
    })
  }

  if (d.thisWeekInterviewCount >= 3 || (d.todayCompletionStatus && d.todayCompletionStatus.includes('完成'))) {
    list.push({
      key: 'streak',
      icon: '✓',
      title: '训练节奏稳定',
      description: `本周已完成 ${d.thisWeekInterviewCount} 场模拟面试，保持当前训练节奏。`,
      to: '/interview',
      toneClass: 'insight-card--good',
      iconClass: 'insight-icon--good'
    })
  }

  if (d.masteryGrowthStatus && d.masteryGrowthStatus.includes('放缓')) {
    list.push({
      key: 'mastery-slow',
      icon: '△',
      title: '掌握进度放缓',
      description: '减少新题，集中完成已有错题的复盘。',
      to: '/review',
      toneClass: 'insight-card--info',
      iconClass: 'insight-icon--info'
    })
  }

  return list.slice(0, 6)
})
</script>

<style scoped>
.insight-card {
  display: block;
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-bg);
  padding: 14px 16px;
  transition: transform 160ms var(--ease-hard), box-shadow 160ms var(--ease-hard);
  text-decoration: none;
}

.insight-card:hover {
  transform: translateY(-1px);
  box-shadow: var(--bc-shadow-hover);
}

.insight-card--warn {
  border-color: rgba(255, 183, 77, 0.35);
}

.insight-card--good {
  border-color: rgba(85, 214, 190, 0.35);
}

.insight-card--info {
  border-color: rgba(var(--bc-accent-rgb), 0.25);
}

.insight-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 800;
}

.insight-icon--warn {
  background: rgba(255, 183, 77, 0.14);
  color: var(--bc-amber);
}

.insight-icon--good {
  background: rgba(85, 214, 190, 0.14);
  color: var(--bc-cyan);
}

.insight-icon--info {
  background: rgba(var(--bc-accent-rgb), 0.12);
  color: var(--bc-amber);
}
</style>
