<template>
  <section class="cockpit-panel deck-list p-5 sm:p-6">
    <div class="deck-list__head">
      <div>
        <p class="deck-list__kicker">Deck 列表</p>
        <h3 class="deck-list__title">选择今天要推进的一组卡片</h3>
      </div>
      <span class="deck-list__count">{{ decks.length }} 组</span>
    </div>

    <div v-if="decks.length" class="deck-list__items">
      <article
        v-for="deck in decks"
        :key="deck.deckId"
        class="deck-card"
        :class="{ 'deck-card-current': deck.isCurrent === 1 }"
      >
        <div class="deck-card__main">
          <div class="deck-card__title-row">
            <h4>{{ deck.deckTitle }}</h4>
            <span class="deck-card__source" :class="{ 'deck-card__source-system': deck.sourceType === 'wrong_auto' }">
              {{ sourceLabel(deck.sourceType) }}
            </span>
          </div>
          <p>
            {{ deck.masteredCards }} / {{ deck.totalCards }} 已掌握 · 今日待处理 {{ deck.dueCount }} 张 · 今日已评
            {{ deck.reviewedTodayCount }} 张
          </p>
          <div class="deck-card__bar">
            <div :style="{ width: `${progress(deck)}%` }"></div>
          </div>
        </div>

        <button
          type="button"
          class="deck-card__action"
          :disabled="deck.isCurrent === 1 || activatingDeckId === deck.deckId"
          @click="$emit('activate', deck.deckId)"
        >
          <template v-if="deck.isCurrent === 1">当前</template>
          <template v-else-if="activatingDeckId === deck.deckId">切换中</template>
          <template v-else>切换</template>
        </button>
      </article>
    </div>

    <div v-else class="deck-list__empty">
      <p>还没有 deck。先从知识库选择资料生成第一组卡片。</p>
      <RouterLink to="/knowledge" class="hard-button-secondary">去知识库</RouterLink>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { CardDeckSummary } from '@/types/api'

defineProps<{
  decks: CardDeckSummary[]
  activatingDeckId?: string | null
}>()

defineEmits<{
  activate: [deckId: string]
}>()

const sourceLabel = (sourceType: string) => {
  if (sourceType === 'wrong_auto') return '系统错题 deck'
  return '知识库 deck'
}

const progress = (deck: CardDeckSummary) => {
  if (!deck.totalCards) return 0
  return Math.min(100, Math.round((deck.masteredCards / deck.totalCards) * 100))
}
</script>

<style scoped>
.deck-list__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.deck-list__kicker {
  color: var(--bc-ink-secondary);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.deck-list__title {
  margin-top: 7px;
  color: var(--bc-ink);
  font-size: 1.3rem;
  font-weight: 760;
  letter-spacing: -0.045em;
}

.deck-list__count {
  border: 1px solid var(--bc-line);
  border-radius: 999px;
  padding: 6px 10px;
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 700;
}

.deck-list__items {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.deck-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  border: 1px solid var(--bc-line);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.24);
  padding: 14px;
}

.dark .deck-card {
  background: rgba(255, 255, 255, 0.035);
}

.deck-card-current {
  border-color: rgba(var(--bc-accent-rgb), 0.36);
  background:
    linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.12), transparent 62%),
    rgba(255, 255, 255, 0.25);
}

.deck-card__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.deck-card h4 {
  color: var(--bc-ink);
  font-size: 1rem;
  font-weight: 760;
}

.deck-card p {
  margin-top: 6px;
  color: rgb(100 116 139);
  font-size: 13px;
  line-height: 1.7;
}

.deck-card__source {
  border-radius: 999px;
  background: rgba(var(--bc-accent-rgb), 0.1);
  padding: 4px 8px;
  color: var(--bc-ink-secondary);
  font-size: 11px;
  font-weight: 700;
}

.deck-card__source-system {
  background: rgba(85, 214, 190, 0.12);
}

.deck-card__bar {
  height: 6px;
  overflow: hidden;
  margin-top: 10px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.22);
}

.deck-card__bar div {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--bc-amber), #55d6be);
}

.deck-card__action {
  min-width: 72px;
  min-height: 38px;
  border: 1px solid var(--bc-line);
  border-radius: 999px;
  color: var(--bc-ink);
  font-size: 13px;
  font-weight: 800;
}

.deck-card__action:not(:disabled):hover {
  border-color: rgba(var(--bc-accent-rgb), 0.35);
  background: rgba(var(--bc-accent-rgb), 0.1);
}

.deck-list__empty {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 18px;
  border: 1px dashed rgba(var(--bc-accent-rgb), 0.24);
  border-radius: 20px;
  padding: 18px;
  color: var(--bc-ink-secondary);
}

@media (max-width: 540px) {
  .deck-card {
    grid-template-columns: 1fr;
  }

  .deck-card__action {
    width: 100%;
  }
}
</style>
