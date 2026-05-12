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
            <p class="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">{{ item.description }}</p>
          </div>
        </div>
      </RouterLink>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { LearningInsights } from '@/types/api'

const props = defineProps<{ data: LearningInsights }>()

type Insight = {
  key: string
  icon: string
  title: string
  description: string
  to: string
  toneClass: string
  iconClass: string
}

const insights = computed<Insight[]>(() => {
  const list: Insight[] = []
  const d = props.data

  // Score trend
  if (d.thisWeekAvgScore > 0 && d.lastWeekAvgScore > 0) {
    const diff = d.thisWeekAvgScore - d.lastWeekAvgScore
    if (diff < -5) {
      list.push({
        key: 'score-drop',
        icon: '▼',
        title: '本周面试分下降',
        description: `平均分 ${Math.round(d.thisWeekAvgScore)}，比上周低 ${Math.abs(Math.round(diff))} 分。建议减少新卡片，优先复习积压项。`,
        to: '/review',
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

  // Review debt
  if (d.reviewDebtStatus && d.reviewDebtStatus.includes('高')) {
    list.push({
      key: 'debt-high',
      icon: '!',
      title: '复习积压较多',
      description: '建议今天先处理复习再学习新卡，避免积压持续增长。',
      to: '/review',
      toneClass: 'insight-card--warn',
      iconClass: 'insight-icon--warn'
    })
  }

  // Streak
  if (d.thisWeekInterviewCount >= 3 || (d.todayCompletionStatus && d.todayCompletionStatus.includes('完成'))) {
    list.push({
      key: 'streak',
      icon: '✓',
      title: '学习节奏良好',
      description: `本周已完成 ${d.thisWeekInterviewCount} 场面试诊断，继续保持！`,
      to: '/cards',
      toneClass: 'insight-card--good',
      iconClass: 'insight-icon--good'
    })
  }

  // Mastery growth slow
  if (d.masteryGrowthStatus && d.masteryGrowthStatus.includes('放缓')) {
    list.push({
      key: 'mastery-slow',
      icon: '◆',
      title: '掌握度增长放缓',
      description: '试试减少每日新卡数量，把精力集中在复习已有内容上。',
      to: '/cards',
      toneClass: 'insight-card--info',
      iconClass: 'insight-icon--info'
    })
  }

  return list
})
</script>

<style scoped>
.insight-card {
  display: block;
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.72);
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
  border-color: rgba(77, 163, 255, 0.25);
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
  background: rgba(77, 163, 255, 0.12);
  color: #2563eb;
}
</style>
