<template>
  <section class="today-panel">
    <div class="today-panel__copy">
      <h2 class="today-panel__title">{{ task ? '先处理今天这组卡片' : '先生成第一组卡片' }}</h2>
      <p class="today-panel__text">
        <template v-if="task">
          今天还有 {{ task.dueCount }} 张待处理。
        </template>
        <template v-else>
          去知识库生成第一组卡片。
        </template>
      </p>

      <div class="today-panel__actions">
        <button v-if="task?.currentCard" type="button" class="hard-button-primary" @click="$emit('start')">开始学习</button>
        <RouterLink v-else to="/knowledge" class="hard-button-primary">去知识库</RouterLink>
        <RouterLink v-if="task" to="/knowledge" class="hard-button-secondary">去知识库</RouterLink>
      </div>
    </div>

    <div v-if="task" class="today-panel__metrics">
      <article v-for="metric in metrics" :key="metric.label" class="today-panel__metric">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.hint }}</small>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TodayCardsTask } from '@/types/api'

const props = defineProps<{
  task: TodayCardsTask | null
}>()

defineEmits<{
  start: []
}>()

const metrics = computed(() => {
  const task = props.task
  return [
    {
      label: '今日待学',
      value: task ? `${task.todayLearnCount}` : '0',
      hint: '新卡片'
    },
    {
      label: '今日待复习',
      value: task ? `${task.todayReviewCount}` : '0',
      hint: '到期卡片'
    },
    {
      label: '预计用时',
      value: task ? `${task.estimatedMinutes} 分钟` : '待开始',
      hint: ''
    },
    {
      label: '连续天数',
      value: task ? `${task.streak} 天` : '0 天',
      hint: ''
    }
  ]
})
</script>

<style scoped>
.today-panel {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, 0.95fr);
  gap: 22px;
  border: 1px solid var(--bc-line);
  border-radius: 30px;
  background:
    radial-gradient(circle at 78% 12%, rgba(85, 214, 190, 0.16), transparent 28%),
    linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.13), transparent 42%),
    var(--bc-panel);
  padding: clamp(22px, 3vw, 34px);
  box-shadow: var(--bc-shadow-soft);
}

.today-panel::before {
  content: '';
  position: absolute;
  inset: auto -8% -34% 42%;
  height: 190px;
  border-radius: 999px;
  background: rgba(var(--bc-accent-rgb), 0.16);
  filter: blur(34px);
  transform: rotate(-8deg);
}

.today-panel__copy,
.today-panel__metrics {
  position: relative;
  z-index: 1;
}

.today-panel__title {
  max-width: 520px;
  color: var(--bc-ink);
  font-size: clamp(1.75rem, 3vw, 2.5rem);
  font-weight: 700;
  letter-spacing: -0.05em;
  line-height: 1.02;
}

.today-panel__text {
  margin-top: 14px;
  max-width: 30rem;
  color: rgb(100 116 139);
  font-size: 0.95rem;
  line-height: 1.7;
}

.today-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.today-panel__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-content: end;
}

.today-panel__metric {
  min-height: 128px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.14);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.13), transparent),
    rgba(255, 255, 255, 0.28);
  padding: 18px;
}

.dark .today-panel__metric {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), transparent),
    rgba(255, 255, 255, 0.04);
}

.today-panel__metric span,
.today-panel__metric small {
  display: block;
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 700;
}

.today-panel__metric strong {
  display: block;
  margin-top: 14px;
  color: var(--bc-ink);
  font-size: clamp(1.5rem, 2.5vw, 2.25rem);
  font-weight: 760;
  letter-spacing: -0.06em;
}

.today-panel__metric small {
  margin-top: 8px;
  font-weight: 500;
}

@media (max-width: 900px) {
  .today-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 540px) {
  .today-panel__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
