<template>
  <section class="study-session">
    <div class="study-session__top">
      <button type="button" class="cards-inline-link" @click="$emit('back')">返回任务总览</button>
      <div class="study-session__signals">
        <span class="detail-pill">待处理 {{ task.dueCount }}</span>
        <span class="detail-pill">今日已评 {{ task.reviewedTodayCount }}</span>
      </div>
    </div>

    <div class="study-session__progress">
      <div :style="{ width: `${task.completionRate}%` }"></div>
    </div>

    <p class="study-session__mobile-hint">左滑重来 · 右滑良好 · 点击翻牌</p>

    <div
      class="flashcard-wrapper"
      :class="{ flipped: showAnswer }"
      @click="flipCard"
      @touchstart="onTouchStart"
      @touchmove.passive="onTouchMove"
      @touchend="onTouchEnd"
    >
      <div class="flashcard">
        <div class="flashcard-front memory-card">
          <div class="cards-face">
            <div class="cards-face__bar">
              <span class="cards-face__eyebrow">先回忆问题</span>
              <span class="hard-chip">{{ stateLabel(card.state) }}</span>
            </div>
            <div class="cards-face__content">
              <span class="cards-face__label">问题</span>
              <h3 class="cards-face__title">{{ card.question }}</h3>
            </div>
            <p class="cards-face__hint">点击翻牌查看答案</p>
          </div>
        </div>

        <div class="flashcard-back memory-card">
          <div class="cards-face cards-face-back">
            <span class="cards-face__eyebrow">参考答案</span>
            <div class="cards-face__content cards-face__content-answer">
              <span class="cards-face__label">回答要点</span>
              <p class="cards-answer">{{ card.answer }}</p>
              <div v-if="card.explanation" class="cards-explanation">
                <span>解释</span>
                <p>{{ card.explanation }}</p>
              </div>
            </div>
            <p class="cards-face__hint">
              记忆系数 {{ formatEaseFactor(card.easeFactor) }} · 当前间隔 {{ card.intervalDays ?? 0 }} 天
            </p>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showAnswer" class="rating-grid">
      <button
        v-for="button in ratingButtons"
        :key="button.rating"
        type="button"
        class="rating-button"
        :class="button.className"
        :disabled="submitting"
        @click.stop="rate(button.rating)"
      >
        <span>{{ button.rating }}</span>
        <strong>{{ button.label }}</strong>
        <small>{{ button.hint }}</small>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { KnowledgeCardItem, TodayCardsTask } from '@/types/api'

const props = defineProps<{
  task: TodayCardsTask
  card: KnowledgeCardItem
  submitting: boolean
}>()

const emit = defineEmits<{
  rate: [rating: 1 | 2 | 3 | 4, responseTimeMs?: number]
  back: []
}>()

const showAnswer = ref(false)
const reviewStartedAt = ref<number | null>(null)
const touchStartX = ref(0)
const touchCurrentX = ref(0)

const ratingButtons = [
  { rating: 1 as const, label: '重来', hint: '今天再见', className: 'rating-button-again' },
  { rating: 2 as const, label: '困难', hint: '明天巩固', className: 'rating-button-hard' },
  { rating: 3 as const, label: '良好', hint: '正常推进', className: 'rating-button-good' },
  { rating: 4 as const, label: '轻松', hint: '拉长间隔', className: 'rating-button-easy' }
]

const flipCard = () => {
  showAnswer.value = !showAnswer.value
  if (showAnswer.value && !reviewStartedAt.value) {
    reviewStartedAt.value = Date.now()
  }
}

const rate = (rating: 1 | 2 | 3 | 4) => {
  const responseTimeMs = reviewStartedAt.value ? Math.max(0, Date.now() - reviewStartedAt.value) : undefined
  emit('rate', rating, responseTimeMs)
}

const onTouchStart = (event: TouchEvent) => {
  touchStartX.value = event.touches[0]?.clientX ?? 0
  touchCurrentX.value = touchStartX.value
}

const onTouchMove = (event: TouchEvent) => {
  touchCurrentX.value = event.touches[0]?.clientX ?? touchCurrentX.value
}

const onTouchEnd = () => {
  if (!showAnswer.value || props.submitting) return
  const deltaX = touchCurrentX.value - touchStartX.value
  if (deltaX <= -80) {
    rate(1)
    return
  }
  if (deltaX >= 80) {
    rate(4)
  }
}

const formatEaseFactor = (value?: number) => {
  return typeof value === 'number' ? value.toFixed(2) : '2.50'
}

const stateLabel = (state: KnowledgeCardItem['state']) => {
  if (state === 'mastered') return '已掌握'
  if (state === 'weak') return '薄弱'
  if (state === 'learning') return '学习中'
  return '新卡片'
}

watch(
  () => props.card.id,
  () => {
    showAnswer.value = false
    reviewStartedAt.value = Date.now()
  }
)
</script>

<style scoped>
.study-session {
  max-width: 980px;
  margin: 0 auto;
}

.study-session__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.study-session__signals {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.study-session__progress {
  height: 7px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.22);
}

.study-session__progress div {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--bc-amber), #55d6be);
  transition: width 220ms var(--ease-hard);
}

.study-session__mobile-hint {
  display: none;
  margin: 14px 0 10px;
  text-align: center;
  color: var(--bc-ink-secondary);
  font-size: 12px;
}

.flashcard-wrapper {
  margin-top: 18px;
  cursor: pointer;
  perspective: 1100px;
}

.flashcard {
  position: relative;
  min-height: min(640px, 68vh);
  transition: transform 0.42s var(--ease-hard);
  transform-style: preserve-3d;
}

.flashcard-wrapper.flipped .flashcard {
  transform: rotateY(180deg);
}

.flashcard-front,
.flashcard-back {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  backface-visibility: hidden;
}

.flashcard-back {
  transform: rotateY(180deg);
}

.memory-card {
  border: 1px solid var(--bc-line);
  border-radius: 32px;
  background:
    radial-gradient(circle at 80% 18%, rgba(var(--bc-cyan-rgb), 0.13), transparent 30%),
    linear-gradient(145deg, rgba(var(--bc-accent-rgb), 0.04), transparent 36%),
    var(--panel-bg);
  padding: clamp(22px, 4vw, 38px);
  box-shadow:
    var(--bc-shadow),
    inset 0 1px 0 rgba(var(--bc-ink-rgb), 0.04);
}

.cards-face {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 20px;
  height: 100%;
}

.cards-face__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.cards-face__eyebrow,
.cards-face__label,
.cards-explanation span {
  color: var(--bc-ink-secondary);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.cards-face__content {
  min-height: 0;
  overflow-y: auto;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.12);
  border-radius: 26px;
  background:
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.08), transparent 62%),
    var(--panel-muted);
  padding: clamp(20px, 3vw, 30px);
}

.dark .cards-face__content {
  background:
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.1), transparent 62%),
    var(--panel-muted);
}

.cards-face__title {
  margin-top: 16px;
  max-width: 48rem;
  color: var(--bc-ink);
  font-size: clamp(1.25rem, 2vw, 1.85rem);
  font-weight: 700;
  letter-spacing: -0.045em;
  line-height: 1.65;
  white-space: pre-wrap;
}

.cards-answer {
  margin-top: 16px;
  color: var(--bc-ink);
  font-size: clamp(1rem, 1.35vw, 1.2rem);
  line-height: 1.9;
  white-space: pre-wrap;
}

.cards-explanation {
  margin-top: 22px;
  border-top: 1px solid rgba(var(--bc-accent-rgb), 0.12);
  padding-top: 18px;
}

.cards-explanation p {
  margin-top: 10px;
  color: rgb(100 116 139);
  line-height: 1.85;
  white-space: pre-wrap;
}

.cards-face__hint {
  color: var(--bc-ink-secondary);
  font-size: 13px;
}

.rating-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.rating-button {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  min-height: 92px;
  border: 1px solid var(--bc-line);
  border-radius: 20px;
  background: var(--interactive-bg);
  padding: 14px;
  text-align: left;
  transition:
    transform 160ms var(--ease-hard),
    border-color 160ms var(--ease-hard),
    background-color 160ms var(--ease-hard);
}

.dark .rating-button {
  background: var(--interactive-bg);
}

.rating-button:hover:not(:disabled) {
  transform: translateY(-2px);
  border-color: rgba(var(--bc-accent-rgb), 0.35);
}

.rating-button span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: rgba(var(--bc-accent-rgb), 0.1);
  color: var(--bc-ink);
  font-size: 12px;
  font-weight: 800;
}

.rating-button strong {
  color: var(--bc-ink);
  font-size: 15px;
}

.rating-button small {
  color: var(--bc-ink-secondary);
  font-size: 12px;
}

.rating-button-again:hover:not(:disabled) { background: rgba(248, 113, 113, 0.12); }
.rating-button-hard:hover:not(:disabled) { background: rgba(251, 146, 60, 0.12); }
.rating-button-good:hover:not(:disabled) { background: rgba(85, 214, 190, 0.12); }
.rating-button-easy:hover:not(:disabled) { background: rgba(74, 222, 128, 0.12); }

.cards-inline-link,
.detail-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--bc-line);
  border-radius: 999px;
  background: var(--interactive-bg);
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 700;
}

.cards-inline-link {
  min-height: 34px;
  padding: 0 13px;
}

.detail-pill {
  min-height: 30px;
  padding: 0 10px;
}

@media (max-width: 720px) {
  .study-session__top {
    align-items: flex-start;
    flex-direction: column;
  }

  .study-session__mobile-hint {
    display: block;
  }

  .flashcard {
    min-height: 560px;
  }

  .rating-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
