<template>
  <div class="cards-workbench space-y-5">
    <section class="cockpit-panel cards-topbar p-4 sm:p-5">
      <div>
        <p class="cards-topbar__kicker">记忆工作台</p>
        <h2>今日卡片</h2>
      </div>
      <div class="cards-topbar__actions">
        <span v-if="todayTask" class="detail-pill">{{ todayTask.deckTitle }}</span>
        <span v-if="stats" class="detail-pill">{{ stats.deckCount }} 组 deck</span>
        <RouterLink to="/knowledge" class="hard-button-secondary text-sm">知识库生成卡片</RouterLink>
      </div>
    </section>

    <section v-if="loading" class="cockpit-panel cards-loading p-10 text-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      <p class="mt-4 text-sm text-slate-500">加载今日记忆工作台...</p>
    </section>

    <template v-else>
      <section v-if="docId" class="cockpit-panel generate-panel p-5 sm:p-6">
        <div>
          <p class="generate-panel__kicker">从知识库生成</p>
          <h3>{{ docTitle || `文档 #${docId}` }}</h3>
          <p>生成后会创建一个知识库 deck，并直接切换为当前今日任务。</p>
        </div>
        <div class="generate-panel__control">
          <label for="days">计划天数</label>
          <el-input-number
            id="days"
            v-model="days"
            :min="1"
            :max="30"
            :step="1"
            size="large"
            controls-position="right"
          />
          <button type="button" class="hard-button-primary" :disabled="generating" @click="generateDeck">
            {{ generating ? '生成中...' : '生成并设为当前 deck' }}
          </button>
        </div>
      </section>

      <CardStudySession
        v-if="studyMode && todayTask?.currentCard"
        :task="todayTask"
        :card="todayTask.currentCard"
        :submitting="submitting"
        @rate="submitRating"
        @back="leaveStudyMode"
      />

      <template v-else>
        <TodayCardsPanel :task="todayTask" @start="enterStudyMode" />

        <div class="cards-workbench__grid">
          <CardDeckList :decks="decks" :activating-deck-id="activatingDeckId" @activate="activateDeck" />
          <CardProgressSummary :task="todayTask" :stats="stats" @overview="leaveStudyMode" />
        </div>

        <section v-if="!todayTask && decks.length === 0" class="cockpit-panel cards-empty p-8 sm:p-10">
          <div>
            <p class="cards-empty__kicker">还没有当前 deck</p>
            <h3>先从知识库生成第一组卡片</h3>
            <p>卡片页会直接展示今日待学、待复习和当前学习卡，不需要先进入其他模块。</p>
            <RouterLink to="/knowledge" class="hard-button-primary mt-6">去知识库生成卡片</RouterLink>
          </div>
        </section>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  activateCardDeckApi,
  fetchCardDecksApi,
  fetchCardStatsApi,
  fetchTodayCardsTaskApi,
  generateCardDeckApi,
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
const generating = ref(false)
const submitting = ref(false)
const activatingDeckId = ref<string | null>(null)
const todayTask = ref<TodayCardsTask | null>(null)
const decks = ref<CardDeckSummary[]>([])
const stats = ref<CardStatsSummary | null>(null)
const days = ref(7)

const docId = computed(() => {
  const raw = route.query.docId
  const value = Array.isArray(raw) ? raw[0] : raw
  return typeof value === 'string' && value.length > 0 ? value : null
})

const docTitle = computed(() => {
  const raw = route.query.title
  return typeof raw === 'string' ? raw : ''
})

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

const refreshWorkbench = async () => {
  const [todayResponse, decksResponse, statsResponse] = await Promise.all([
    fetchTodayCardsTaskApi(),
    fetchCardDecksApi(),
    fetchCardStatsApi()
  ])
  todayTask.value = todayResponse.data
  decks.value = decksResponse.data ?? []
  stats.value = statsResponse.data
}

const generateDeck = async () => {
  if (!docId.value) return
  const numericDocId = Number(docId.value)
  if (!Number.isFinite(numericDocId)) {
    ElMessage.error('文档 ID 无效')
    return
  }
  generating.value = true
  try {
    await generateCardDeckApi({ docId: numericDocId, days: days.value })
    await refreshWorkbench()
    await router.replace({ path: '/cards' })
    ElMessage.success('已生成并切换为当前 deck')
  } catch (error: any) {
    ElMessage.error(error?.message || '生成卡片 deck 失败')
  } finally {
    generating.value = false
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
    ElMessage.success('已切换当前 deck')
  } catch (error: any) {
    ElMessage.error(error?.message || '切换 deck 失败')
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

watch(
  () => route.query.docId,
  () => {
    void loadWorkbench()
  },
  { immediate: true }
)
</script>

<style scoped>
.cards-topbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px 18px;
}

.cards-topbar__kicker,
.generate-panel__kicker,
.cards-empty__kicker {
  color: var(--bc-ink-secondary);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.cards-topbar h2 {
  margin-top: 4px;
  color: var(--bc-ink);
  font-size: 28px;
  font-weight: 780;
  letter-spacing: -0.05em;
  line-height: 1.1;
}

.cards-topbar__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

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

.generate-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
}

.generate-panel h3,
.cards-empty h3 {
  margin-top: 8px;
  color: var(--bc-ink);
  font-size: clamp(1.45rem, 2.5vw, 2.25rem);
  font-weight: 780;
  letter-spacing: -0.06em;
}

.generate-panel p,
.cards-empty p {
  margin-top: 8px;
  color: rgb(100 116 139);
  line-height: 1.8;
}

.generate-panel__control {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.generate-panel__control label {
  color: var(--bc-ink);
  font-size: 13px;
  font-weight: 700;
}

.generate-panel__control :deep(.el-input-number) {
  width: 150px;
}

.cards-workbench__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.16fr) minmax(320px, 0.84fr);
  gap: 18px;
  align-items: start;
}

.cards-empty {
  display: grid;
  min-height: 320px;
  place-items: center;
}

.cards-empty > div {
  max-width: 34rem;
}

@media (max-width: 980px) {
  .cards-workbench__grid,
  .generate-panel {
    grid-template-columns: 1fr;
  }

  .generate-panel__control {
    justify-content: flex-start;
  }
}
</style>
