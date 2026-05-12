<template>
  <el-dialog
    :model-value="visible"
    title="生成卡片"
    width="640px"
    :close-on-click-modal="false"
    destroy-on-close
    @update:model-value="$event ? null : $emit('close')"
  >
    <div class="generate-panel">
      <div class="generate-panel__summary mb-5">
        <article class="generate-panel__metric">
          <span>来源文档</span>
          <strong class="truncate">{{ docTitle || `文档 #${docId}` }}</strong>
        </article>
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
      </div>

      <div v-if="matchedDocDeck" class="generate-panel__existing mb-5">
        <strong>这份资料已经生成过卡组：{{ matchedDocDeck.deckTitle }}</strong>
        <p>当前已有 {{ matchedDocDeck.totalCards }} 张卡片，已掌握 {{ matchedDocDeck.masteredCards }} 张。</p>
        <div class="generate-panel__existing-actions">
          <button
            type="button"
            class="hard-button-primary"
            :disabled="activating"
            @click="handleActivateExisting"
          >
            {{ activating ? '切换中...' : '设为当前卡组' }}
          </button>
          <button type="button" class="hard-button-secondary" @click="$emit('close')">
            返回
          </button>
        </div>
      </div>

      <div v-if="!matchedDocDeck" class="generate-panel__control">
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
      </div>
    </div>

    <template #footer>
      <div class="generate-panel__actions">
        <button type="button" class="hard-button-secondary" @click="$emit('close')">取消</button>
        <button
          v-if="!matchedDocDeck"
          type="button"
          class="hard-button-primary"
          :disabled="generating"
          @click="handleGenerate"
        >
          {{ generating ? '生成中...' : '生成并设为当前卡组' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { activateCardDeckApi, generateCardDeckApi } from '@/api/cards'
import type { CardDeckSummary, CardGenerateDifficulty, CardGenerateType, TodayCardsTask } from '@/types/api'

const props = defineProps<{
  visible: boolean
  docId: number
  docTitle: string
  decks: CardDeckSummary[]
}>()

const emit = defineEmits<{
  close: []
  generated: [task: TodayCardsTask]
  activated: [task: TodayCardsTask]
}>()

const generating = ref(false)
const activating = ref(false)
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

const matchedDocDeck = computed(() => {
  const expectedTitle = props.docTitle.trim()
  return (
    props.decks.find(
      (deck) =>
        deck.sourceType === 'knowledge_doc' &&
        (deck.deckTitle === expectedTitle || (!expectedTitle && false))
    ) ?? null
  )
})

const handleGenerate = async () => {
  if (selectedCardTypes.value.length === 0) {
    ElMessage.warning('至少选择一种卡片类型')
    return
  }
  generating.value = true
  try {
    await generateCardDeckApi({
      docId: props.docId,
      days: days.value,
      cardTypes: selectedCardTypes.value,
      cardCount: cardCount.value,
      difficulty: difficulty.value
    })
    ElMessage.success('已生成并切换为当前卡组')
    emit('generated', {} as TodayCardsTask)
    emit('close')
  } catch (error: any) {
    ElMessage.error(error?.message || '生成卡组失败')
  } finally {
    generating.value = false
  }
}

const handleActivateExisting = async () => {
  if (!matchedDocDeck.value) return
  activating.value = true
  try {
    const { data } = await activateCardDeckApi(matchedDocDeck.value.deckId)
    ElMessage.success('已切换当前卡组')
    emit('activated', data)
    emit('close')
  } catch (error: any) {
    ElMessage.error(error?.message || '切换卡组失败')
  } finally {
    activating.value = false
  }
}
</script>

<style scoped>
.generate-panel__summary {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.generate-panel__metric {
  border-radius: 16px;
  background: rgba(var(--bc-accent-rgb), 0.08);
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
  font-size: 1.1rem;
  font-weight: 780;
}

.generate-panel__control {
  display: grid;
  gap: 18px;
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
  border: 1px solid rgba(var(--bc-accent-rgb), 0.12);
  border-radius: 18px;
  background: rgba(var(--bc-accent-rgb), 0.04);
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
  justify-content: flex-end;
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
</style>
