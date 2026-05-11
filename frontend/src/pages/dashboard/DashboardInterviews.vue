<template>
  <article class="paper-panel p-6">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <p class="section-kicker">诊断记录</p>
        <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">最近面试诊断结果</h3>
      </div>
      <div class="flex gap-3">
        <RouterLink class="accent-link text-sm font-semibold" to="/interview/history">全部历史</RouterLink>
        <RouterLink class="accent-link text-sm font-semibold" to="/interview">开始下一次诊断</RouterLink>
      </div>
    </div>

    <div v-if="interviews.length" class="mt-6 space-y-3">
      <RouterLink
        v-for="interview in interviews"
        :key="interview.sessionId"
        :to="`/interview/detail/${interview.sessionId}`"
        class="surface-card surface-card-hover block p-4"
      >
        <div class="flex items-start justify-between gap-3">
          <div>
            <div class="text-xs uppercase tracking-[0.28em] text-slate-500 dark:text-slate-400">{{ interview.direction }}</div>
            <div class="mt-2 text-lg font-semibold text-ink">{{ interviewTitle(interview) }}</div>
          </div>
          <div class="text-right">
            <div class="text-3xl font-semibold tracking-[-0.03em] text-ink">{{ formatScore(interview.totalScore) }}</div>
            <div class="mt-1 text-xs uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ statusLabel(interview.status) }}</div>
          </div>
        </div>
        <div class="mt-3 text-sm text-slate-500 dark:text-slate-400">{{ formatDate(interview.finishedAt) }}</div>
      </RouterLink>
    </div>

    <EmptyState
      v-else
      icon="clipboard"
      title="还没有诊断记录"
      description="完成面试后查看最近结果。"
      compact
      class="mt-6"
    />
  </article>
</template>

<script setup lang="ts">
import EmptyState from '@/components/EmptyState.vue'
import type { RecentInterviewItem } from '@/types/api'

defineProps<{
  interviews: RecentInterviewItem[]
}>()

const formatScore = (score: number): string => {
  return Number.isInteger(score) ? String(score) : score.toFixed(2)
}

const formatDate = (dateTime: string): string => {
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(dateTime))
}

const interviewTitle = (interview: RecentInterviewItem): string => {
  return interview.status === 'finished' ? '已完成面试诊断' : '进行中的面试诊断'
}

const statusLabel = (status: string): string => {
  return status === 'finished' ? '已完成' : status
}
</script>
