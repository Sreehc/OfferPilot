<template>
  <section class="shell-section-card progress-summary p-5 sm:p-6">
    <div class="progress-summary__head">
      <div>
        <p class="section-kicker">当前进度</p>
        <h3 class="progress-summary__title mt-3">{{ title }}</h3>
      </div>
      <span class="progress-summary__rate">{{ task?.completionRate ?? stats?.completionRate ?? 0 }}%</span>
    </div>

    <div class="progress-summary__grid">
      <article>
        <span>累计掌握</span>
        <strong>{{ task?.masteredCards ?? stats?.masteredCards ?? 0 }}</strong>
      </article>
      <article>
        <span>今日完成</span>
        <strong>{{ task?.todayCompletedCount ?? 0 }}</strong>
      </article>
      <article>
        <span>明日待复习</span>
        <strong>{{ task?.tomorrowDueCount ?? 0 }}</strong>
      </article>
      <article>
        <span>完成率</span>
        <strong>{{ task?.completionRate ?? stats?.completionRate ?? 0 }}%</strong>
      </article>
    </div>

    <div v-if="task && !task.currentCard" class="progress-summary__done">
      <p>今天这组卡片已经处理完，明天预计复习 {{ task.tomorrowDueCount }} 张。</p>
      <button type="button" class="hard-button-secondary" @click="$emit('overview')">返回任务总览</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CardStatsSummary, TodayCardsTask } from '@/types/api'

const props = defineProps<{
  task: TodayCardsTask | null
  stats: CardStatsSummary | null
}>()

defineEmits<{
  overview: []
}>()

const title = computed(() => {
  if (!props.task) return '还没有今日卡片'
  if (!props.task.currentCard) return '今天这组已完成'
  return '当前进度'
})
</script>

<style scoped>
.progress-summary__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.progress-summary__title {
  color: var(--bc-ink);
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.progress-summary__rate {
  color: var(--bc-ink);
  font-size: 2rem;
  font-weight: 800;
  letter-spacing: -0.06em;
}

.progress-summary__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.progress-summary__grid article {
  border-radius: 20px;
  background: rgba(var(--bc-accent-rgb), 0.06);
  padding: 15px;
}

.dark .progress-summary__grid article {
  background: var(--panel-muted);
}

.progress-summary__grid span {
  display: block;
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 700;
}

.progress-summary__grid strong {
  display: block;
  margin-top: 10px;
  color: var(--bc-ink);
  font-size: 1.7rem;
  font-weight: 780;
  letter-spacing: -0.06em;
}

.progress-summary__done {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 18px;
  border-radius: 20px;
  background: rgba(var(--bc-accent-rgb), 0.08);
  padding: 16px;
}

.progress-summary__done p {
  color: var(--bc-ink);
  font-size: 14px;
  font-weight: 650;
}

@media (max-width: 520px) {
  .progress-summary__grid {
    grid-template-columns: 1fr;
  }
}
</style>
