<template>
  <div class="cards-workbench space-y-5">
    <AppShellHeader>
      <template #actions>
        <span v-if="todayTask" class="detail-pill">{{ todayTask.deckTitle }}</span>
        <span v-if="stats && stats.deckCount > 0" class="detail-pill">{{ stats.deckCount }} 组卡组</span>
      </template>
    </AppShellHeader>

    <section v-if="loading" class="shell-section-card cards-loading p-10 text-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      <p class="mt-4 text-sm text-slate-500">加载今日记忆工作台...</p>
    </section>

    <template v-else>
      <CardStudySession
        v-if="studyMode && todayTask?.currentCard"
        :task="todayTask"
        :card="todayTask.currentCard"
        :submitting="submitting"
        @rate="submitRating"
        @back="leaveStudyMode"
      />

      <template v-else>
        <section class="cards-workbench__hero-grid">
          <TodayCardsPanel :task="todayTask" @start="enterStudyMode" />
          <CardProgressSummary :task="todayTask" :stats="stats" @overview="leaveStudyMode" />
        </section>

        <CardDeckList :decks="decks" :activating-deck-id="activatingDeckId" @activate="activateDeck" />
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppShellHeader from '@/components/AppShellHeader.vue'
import {
  activateCardDeckApi,
  fetchCardDecksApi,
  fetchCardStatsApi,
  fetchTodayCardsTaskApi,
  reviewDeckCardApi
} from '@/api/cards'
import CardDeckList from './CardDeckList.vue'
import CardProgressSummary from './CardProgressSummary.vue'
import CardStudySession from './CardStudySession.vue'
import TodayCardsPanel from './TodayCardsPanel.vue'
import type { CardDeckSummary, CardStatsSummary, TodayCardsTask } from '@/types/api'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const submitting = ref(false)
const activatingDeckId = ref<string | null>(null)
const todayTask = ref<TodayCardsTask | null>(null)
const decks = ref<CardDeckSummary[]>([])
const stats = ref<CardStatsSummary | null>(null)

const studyMode = computed(() => route.query.view === 'study')

const loadWorkbench = async () => {
  loading.value = true
  try {
    const [todayResponse, decksResponse, statsResponse] = await Promise.all([
      fetchTodayCardsTaskApi(),
      fetchCardDecksApi(),
      fetchCardStatsApi()
    ])
    todayTask.value = todayResponse.data
    decks.value = decksResponse.data ?? []
    stats.value = statsResponse.data
  } catch (error: any) {
    todayTask.value = null
    decks.value = []
    stats.value = null
    ElMessage.error(error?.message || '加载卡片工作台失败')
  } finally {
    loading.value = false
  }
}

const activateDeck = async (deckId: string) => {
  activatingDeckId.value = deckId
  try {
    const { data } = await activateCardDeckApi(deckId)
    todayTask.value = data
    const [decksResponse, statsResponse] = await Promise.all([fetchCardDecksApi(), fetchCardStatsApi()])
    decks.value = decksResponse.data ?? []
    stats.value = statsResponse.data
    ElMessage.success('已切换当前卡组')
  } catch (error: any) {
    ElMessage.error(error?.message || '切换卡组失败')
  } finally {
    activatingDeckId.value = null
  }
}

const submitRating = async (rating: 1 | 2 | 3 | 4, responseTimeMs?: number) => {
  if (!todayTask.value?.currentCard) return
  submitting.value = true
  try {
    const { data } = await reviewDeckCardApi(todayTask.value.deckId, {
      cardId: todayTask.value.currentCard.id,
      rating,
      responseTimeMs
    })
    todayTask.value = data
    const [decksResponse, statsResponse] = await Promise.all([fetchCardDecksApi(), fetchCardStatsApi()])
    decks.value = decksResponse.data ?? []
    stats.value = statsResponse.data
    if (!data.currentCard) {
      await router.replace({ path: '/cards' })
      ElMessage.success('今日任务已完成')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '提交评分失败')
  } finally {
    submitting.value = false
  }
}

const enterStudyMode = async () => {
  if (!todayTask.value?.currentCard) return
  await router.replace({ path: '/cards', query: { view: 'study' } })
}

const leaveStudyMode = async () => {
  await router.replace({ path: '/cards' })
}

void loadWorkbench()
</script>

<style scoped>
.detail-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--bc-line);
  border-radius: 999px;
  padding: 6px 10px;
  color: var(--bc-ink-secondary);
  font-size: 11px;
  font-weight: 700;
}

.cards-loading {
  min-height: 260px;
}

.cards-workbench__hero-grid {
  display: grid;
  gap: 18px;
}

@media (min-width: 1200px) {
  .cards-workbench__hero-grid {
    grid-template-columns: minmax(0, 1.15fr) 360px;
    align-items: start;
  }
}
</style>
