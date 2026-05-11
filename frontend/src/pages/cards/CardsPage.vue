<template>
  <div class="cards-workbench space-y-5">
    <AppShellHeader>
      <template #actions>
        <span v-if="todayTask" class="detail-pill">{{ todayTask.deckTitle }}</span>
        <span v-if="stats" class="detail-pill">{{ stats.deckCount }} 组 deck</span>
        <RouterLink to="/knowledge" class="hard-button-secondary text-sm">去知识库</RouterLink>
      </template>
    </AppShellHeader>

    <section v-if="loading" class="cockpit-panel cards-loading p-10 text-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      <p class="mt-4 text-sm text-slate-500">加载今日记忆工作台...</p>
    </section>

    <template v-else>
      <section v-if="docId" class="cockpit-panel generate-panel p-5 sm:p-6">
        <div>
          <h3>{{ docTitle || `文档 #${docId}` }}</h3>
          <p>从这份资料生成一组新的卡片。</p>
        </div>

        <div v-if="matchedDocDeck" class="generate-panel__existing">
          <strong>这份资料已经生成过 deck：{{ matchedDocDeck.deckTitle }}</strong>
          <p>当前已有 {{ matchedDocDeck.totalCards }} 张卡片，已掌握 {{ matchedDocDeck.masteredCards }} 张。</p>
          <div class="generate-panel__existing-actions">
            <button
              type="button"
              class="hard-button-primary"
              :disabled="activatingDeckId === matchedDocDeck.deckId"
              @click="activateDeck(matchedDocDeck.deckId)"
            >
              {{ activatingDeckId === matchedDocDeck.deckId ? '切换中...' : '设为当前 deck' }}
            </button>
            <button type="button" class="hard-button-secondary" @click="router.replace({ path: '/cards' })">
              返回工作台
            </button>
          </div>
        </div>

        <div v-else class="generate-panel__control">
          <div class="generate-panel__summary">
            <article class="generate-panel__metric">
              <span>卡片类型</span>
              <strong>{{ selectedCardTypes.length }} 种</strong>
            </article>
            <article class="generate-panel__metric">
              <span>目标数量</span>
              <strong>{{ cardCount }} 张</strong>
            </article>
            <article class="generate-panel__metric">
              <span>难度</span>
              <strong>{{ difficultyOptions.find((item) => item.value === difficulty)?.label }}</strong>
            </article>
            <article class="generate-panel__metric">
              <span>复习天数</span>
              <strong>{{ days }} 天</strong>
            </article>
          </div>

          <div class="generate-panel__form">
            <div class="generate-panel__field generate-panel__field-wide">
              <label>生成类型</label>
              <el-checkbox-group v-model="selectedCardTypes" class="generate-panel__types">
                <el-checkbox
                  v-for="option in cardTypeOptions"
                  :key="option.value"
                  :label="option.value"
                  class="generate-type-chip"
                >
                  <span>{{ option.label }}</span>
                  <small>{{ option.hint }}</small>
                </el-checkbox>
              </el-checkbox-group>
            </div>

            <div class="generate-panel__field">
              <label for="cardCount">生成数量</label>
              <el-input-number
                id="cardCount"
                v-model="cardCount"
                :min="4"
                :max="30"
                :step="2"
                size="large"
                controls-position="right"
              />
            </div>

            <div class="generate-panel__field">
              <label for="difficulty">难度</label>
              <el-select id="difficulty" v-model="difficulty" size="large">
                <el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>

            <div class="generate-panel__field">
              <label for="days">复习天数</label>
              <el-input-number
                id="days"
                v-model="days"
                :min="1"
                :max="30"
                :step="1"
                size="large"
                controls-position="right"
              />
            </div>
          </div>

          <div class="generate-panel__actions">
            <button type="button" class="hard-button-primary" :disabled="generating" @click="generateDeck">
              {{ generating ? '生成中...' : '生成并设为当前 deck' }}
            </button>
            <button type="button" class="hard-button-secondary" @click="router.replace({ path: '/cards' })">
              暂不生成
            </button>
          </div>
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
            <h3>先从知识库生成第一组卡片</h3>
            <p>生成后就能直接开始今天的学习。</p>
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
import AppShellHeader from '@/components/AppShellHeader.vue'
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
import type { CardDeckSummary, CardGenerateDifficulty, CardGenerateType, CardStatsSummary, TodayCardsTask } from '@/types/api'

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
const cardCount = ref(12)
const difficulty = ref<CardGenerateDifficulty>('auto')
const selectedCardTypes = ref<CardGenerateType[]>(['concept', 'qa', 'scenario', 'compare'])

const cardTypeOptions: Array<{ label: string; value: CardGenerateType; hint: string }> = [
  { label: '概念卡', value: 'concept', hint: '定义、原理、关键点' },
  { label: '问答卡', value: 'qa', hint: '高频面试问答' },
  { label: '场景题卡', value: 'scenario', hint: '案例、排查、设计场景' },
  { label: '易混淆点卡', value: 'compare', hint: '对比、边界、差异' }
]

const difficultyOptions: Array<{ label: string; value: CardGenerateDifficulty }> = [
  { label: '系统自动', value: 'auto' },
  { label: '基础', value: 'easy' },
  { label: '标准', value: 'medium' },
  { label: '进阶', value: 'hard' }
]

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
const matchedDocDeck = computed(() => {
  if (!docId.value) return null
  const expectedTitle = docTitle.value.trim()
  return (
    decks.value.find(
      (deck) =>
        deck.sourceType === 'knowledge_doc' &&
        (deck.deckTitle === expectedTitle || (!expectedTitle && deck.deckId === todayTask.value?.deckId))
    ) ?? null
  )
})

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
  if (selectedCardTypes.value.length === 0) {
    ElMessage.warning('至少选择一种卡片类型')
    return
  }
  generating.value = true
  try {
    await generateCardDeckApi({
      docId: numericDocId,
      days: days.value,
      cardTypes: selectedCardTypes.value,
      cardCount: cardCount.value,
      difficulty: difficulty.value
    })
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
  gap: 20px;
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
  display: grid;
  gap: 18px;
}

.generate-panel__summary {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.generate-panel__metric {
  border: 1px solid var(--bc-line);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.24);
  padding: 14px;
}

.dark .generate-panel__metric {
  background: rgba(255, 255, 255, 0.04);
}

.generate-panel__metric span {
  display: block;
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 700;
}

.generate-panel__metric strong {
  display: block;
  margin-top: 10px;
  color: var(--bc-ink);
  font-size: 1.2rem;
  font-weight: 780;
}

.generate-panel__form {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.generate-panel__field {
  display: grid;
  gap: 10px;
}

.generate-panel__field-wide {
  grid-column: 1 / -1;
}

.generate-panel__control label {
  color: var(--bc-ink);
  font-size: 13px;
  font-weight: 700;
}

.generate-panel__types {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.generate-type-chip {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  border: 1px solid var(--bc-line);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.22);
  padding: 12px 14px;
}

.generate-type-chip span {
  color: var(--bc-ink);
  font-size: 14px;
  font-weight: 760;
}

.generate-type-chip small {
  color: var(--bc-ink-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.generate-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.generate-panel__existing {
  border: 1px dashed rgba(var(--bc-accent-rgb), 0.28);
  border-radius: 20px;
  background: rgba(var(--bc-accent-rgb), 0.07);
  padding: 16px 18px;
}

.generate-panel__existing strong {
  display: block;
  color: var(--bc-ink);
  font-size: 1rem;
  font-weight: 760;
}

.generate-panel__existing p {
  margin-top: 8px;
  color: rgb(100 116 139);
  line-height: 1.7;
}

.generate-panel__existing-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
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
  .generate-panel__form,
  .generate-panel__summary {
    grid-template-columns: 1fr;
  }
}
</style>
