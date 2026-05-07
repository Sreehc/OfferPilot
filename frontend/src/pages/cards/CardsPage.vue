<template>
  <div class="cards-workbench space-y-5">
    <section class="cockpit-panel p-4 sm:p-5">
      <div class="module-topbar">
        <div class="module-topbar__title">
          <span class="state-pulse" aria-hidden="true"></span>
          <h2 class="module-topbar__heading">知识卡片</h2>
        </div>

        <div v-if="task" class="module-topbar__center">
          <div class="cards-toolbar">
            <span class="detail-pill">{{ task.docTitle }}</span>
            <span class="detail-pill">第 {{ task.currentDay }} / {{ task.days }} 天</span>
            <span class="detail-pill">{{ task.masteredCards }} / {{ task.totalCards }} 已推进</span>
          </div>
        </div>

        <div class="module-topbar__action">
          <RouterLink to="/knowledge" class="hard-button-secondary text-sm">从知识库选择资料</RouterLink>
        </div>
      </div>
    </section>

    <section v-if="loading" class="cockpit-panel p-8 text-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      <p class="mt-4 text-sm text-slate-500">加载知识卡片任务...</p>
    </section>

    <template v-else>
      <section v-if="!task && !docId" class="cockpit-panel cards-empty p-8 sm:p-10">
        <div class="cards-empty__box">
          <h3 class="cards-empty__title">还没有知识卡片任务</h3>
          <p class="cards-empty__text">从知识库选择一份资料，生成一轮按天推进的记忆任务。</p>
          <RouterLink to="/knowledge" class="hard-button-primary mt-6">从知识库选择资料</RouterLink>
        </div>
      </section>

      <section v-else-if="docId && !task" class="cockpit-panel p-5 sm:p-6">
        <div class="cards-setup">
          <div class="cards-setup__head">
            <div>
              <p class="cards-setup__label">已选择资料</p>
              <h3 class="cards-setup__title">{{ docTitle || `文档 #${docId}` }}</h3>
            </div>
            <span class="detail-pill">单文档任务</span>
          </div>

          <div class="cards-setup__dropzone">
            <div class="cards-setup__dropzone-copy">
              <span class="cards-setup__dropzone-icon" aria-hidden="true">
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                  />
                </svg>
              </span>
              <div>
                <p class="text-sm font-semibold text-ink">系统会自动生成问题与答案卡片</p>
                <p class="mt-1 text-sm text-slate-600 dark:text-slate-300">你只需要决定几天完成这一轮记忆。</p>
              </div>
            </div>
          </div>

          <div class="cards-setup__form">
            <div class="cards-setup__field">
              <label for="days" class="cards-setup__field-label">计划天数</label>
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
            <div class="cards-setup__field cards-setup__hint">
              <span>{{ days }} 天内平均推进，系统会自动分配每天的卡片量。</span>
            </div>
          </div>

          <div class="mt-6 flex flex-wrap gap-3">
            <button type="button" class="hard-button-primary" :disabled="creatingTask" @click="createTask">
              {{ creatingTask ? '生成中...' : '生成知识卡片' }}
            </button>
            <RouterLink to="/knowledge" class="hard-button-secondary">返回知识库</RouterLink>
          </div>
        </div>
      </section>

      <section v-else-if="task?.status === 'invalid'" class="cockpit-panel p-6 sm:p-8">
        <div class="cards-state cards-state-invalid">
          <h3 class="cards-state__title">当前任务已失效</h3>
          <p class="cards-state__text">{{ task.invalidReason || '来源文档已删除，当前任务不可继续。' }}</p>
          <div class="mt-5 flex flex-wrap gap-3">
            <RouterLink to="/knowledge" class="hard-button-primary">重新选择资料</RouterLink>
          </div>
        </div>
      </section>

      <section v-else-if="task?.status === 'draft'" class="cockpit-panel p-5 sm:p-6">
        <div class="cards-launch">
          <div>
            <p class="cards-setup__label">准备开始</p>
            <h3 class="cards-setup__title">{{ task.docTitle }}</h3>
            <p class="cards-launch__text">
              共 {{ task.totalCards }} 张卡片，计划 {{ task.days }} 天完成，每天约 {{ task.dailyTarget }} 张。
            </p>
          </div>

          <div class="mt-6 flex flex-wrap gap-3">
            <button type="button" class="hard-button-primary" :disabled="startingTask" @click="startTask">
              {{ startingTask ? '进入中...' : '开始学习' }}
            </button>
            <button type="button" class="hard-button-secondary" :disabled="creatingTask" @click="restartTask">
              重新生成本轮
            </button>
          </div>
        </div>
      </section>

      <section v-else-if="task?.status === 'completed'" class="cockpit-panel p-6 sm:p-8">
        <div class="cards-state">
          <h3 class="cards-state__title">这一轮已经完成</h3>
          <p class="cards-state__text">
            {{ task.docTitle }} 的 {{ task.totalCards }} 张卡片已经全部推进完成，需要时可以重新开始一轮。
          </p>
          <div class="mt-5 flex flex-wrap gap-3">
            <button type="button" class="hard-button-primary" :disabled="creatingTask" @click="restartTask">
              再来一轮
            </button>
            <RouterLink to="/knowledge" class="hard-button-secondary">返回知识库</RouterLink>
          </div>
        </div>
      </section>

      <section v-else-if="task && currentCard" class="mx-auto max-w-4xl px-1 sm:px-0">
        <div class="mb-4 flex items-center justify-between gap-3 text-sm text-slate-500 dark:text-slate-400">
          <span class="font-mono">{{ currentCard.sortOrder }} / {{ task.totalCards }}</span>
          <div class="flex flex-wrap justify-end gap-2">
            <span class="detail-pill">待处理 {{ task.dueCount }}</span>
            <span class="detail-pill">今日已评 {{ task.reviewedTodayCount }}</span>
          </div>
        </div>

        <div class="mb-6 h-1.5 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-white/10">
          <div
            class="h-full rounded-full bg-accent transition-all duration-300"
            :style="{ width: `${progressPercent}%` }"
          ></div>
        </div>

        <p class="mb-3 text-center text-xs text-slate-400 dark:text-slate-500 sm:hidden">
          左滑重来 · 右滑轻松 · 点击翻牌
        </p>

        <div
          class="flashcard-wrapper cursor-pointer"
          :class="{ flipped: showAnswer }"
          @click="flipCard"
          @touchstart="onTouchStart"
          @touchmove.passive="onTouchMove"
          @touchend="onTouchEnd"
        >
          <div class="flashcard">
            <div class="flashcard-front memory-card p-5 sm:p-8">
              <div class="flex items-center justify-between gap-4">
                <div class="cards-face__eyebrow">先回忆问题</div>
                <span class="hard-chip">{{ stateLabel(currentCard.state) }}</span>
              </div>
              <h3 class="cards-face__title">
                {{ currentCard.question }}
              </h3>
              <p class="cards-face__hint">点击翻牌查看答案</p>
            </div>

            <div class="flashcard-back memory-card p-5 sm:p-8">
              <div class="cards-face__eyebrow">答案</div>
              <p class="cards-answer">
                {{ currentCard.answer }}
              </p>
              <div class="cards-answer__meta">
                当前记忆系数 {{ formatEaseFactor(currentCard.easeFactor) }} · 当前间隔 {{ currentCard.intervalDays ?? 0 }} 天
              </div>
            </div>
          </div>
        </div>

        <div v-if="showAnswer" class="mt-6 grid grid-cols-2 gap-2 sm:grid-cols-4 sm:gap-3">
          <button
            v-for="btn in ratingButtons"
            :key="btn.rating"
            type="button"
            class="rating-button flex flex-col items-center gap-1 p-3 text-sm font-semibold transition-all"
            :class="btn.className"
            :disabled="submitting"
            @click="handleRate(btn.rating)"
          >
            <span class="rating-button__index">{{ btn.rating }}</span>
            <span>{{ btn.label }}</span>
          </button>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createCardTaskApi,
  fetchActiveCardTaskApi,
  fetchCardTaskApi,
  restartCardTaskApi,
  startCardTaskApi,
  submitCardRateApi
} from '@/api/cards'
import type { KnowledgeCardItem, KnowledgeCardTask } from '@/types/api'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const creatingTask = ref(false)
const startingTask = ref(false)
const submitting = ref(false)
const task = ref<KnowledgeCardTask | null>(null)
const days = ref(7)
const showAnswer = ref(false)
const reviewStartedAt = ref<number | null>(null)
const touchStartX = ref(0)
const touchCurrentX = ref(0)

const docId = computed(() => {
  const raw = route.query.docId
  const parsed = Number(Array.isArray(raw) ? raw[0] : raw)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
})

const taskId = computed(() => {
  const raw = route.query.taskId
  const parsed = Number(Array.isArray(raw) ? raw[0] : raw)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
})

const docTitle = computed(() => {
  const raw = route.query.title
  return typeof raw === 'string' ? raw : ''
})

const currentCard = computed<KnowledgeCardItem | null>(() => task.value?.currentCard ?? null)

const progressPercent = computed(() => {
  if (!task.value || task.value.totalCards <= 0) return 0
  return Math.min(100, Math.round((task.value.masteredCards / task.value.totalCards) * 100))
})

const ratingButtons = [
  { rating: 1 as const, label: '重来', className: 'rating-button-again' },
  { rating: 2 as const, label: '困难', className: 'rating-button-hard' },
  { rating: 3 as const, label: '良好', className: 'rating-button-good' },
  { rating: 4 as const, label: '轻松', className: 'rating-button-easy' }
]

const loadTask = async () => {
  loading.value = true
  try {
    if (docId.value && !taskId.value) {
      task.value = null
      return
    }
    if (taskId.value) {
      const { data } = await fetchCardTaskApi(taskId.value)
      task.value = data
      syncRouteToTask()
      return
    }
    const { data } = await fetchActiveCardTaskApi()
    task.value = data
    syncRouteToTask()
  } catch (error: any) {
    task.value = null
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    loading.value = false
  }
}

const syncRouteToTask = () => {
  if (!task.value) return
  if (
    String(route.query.taskId ?? '') === String(task.value.id) &&
    String(route.query.docId ?? '') === String(task.value.docId) &&
    String(route.query.title ?? '') === task.value.docTitle
  ) {
    return
  }
  router.replace({
    path: '/cards',
    query: {
      taskId: String(task.value.id),
      docId: String(task.value.docId),
      title: task.value.docTitle
    }
  })
}

const createTask = async () => {
  if (!docId.value) return
  creatingTask.value = true
  try {
    const { data } = await createCardTaskApi({ docId: docId.value, days: days.value })
    task.value = data
    showAnswer.value = false
    syncRouteToTask()
    ElMessage.success('知识卡片已生成')
  } catch (error: any) {
    ElMessage.error(error?.message || '生成知识卡片失败')
  } finally {
    creatingTask.value = false
  }
}

const startTask = async () => {
  if (!task.value) return
  startingTask.value = true
  try {
    const { data } = await startCardTaskApi(task.value.id)
    task.value = data
    showAnswer.value = false
    reviewStartedAt.value = Date.now()
  } catch (error: any) {
    ElMessage.error(error?.message || '开始学习失败')
  } finally {
    startingTask.value = false
  }
}

const restartTask = async () => {
  if (!task.value) return
  creatingTask.value = true
  try {
    const { data } = await restartCardTaskApi(task.value.id)
    task.value = data
    showAnswer.value = false
    reviewStartedAt.value = null
    ElMessage.success('已重置本轮任务')
  } catch (error: any) {
    ElMessage.error(error?.message || '重置任务失败')
  } finally {
    creatingTask.value = false
  }
}

const flipCard = () => {
  if (!currentCard.value) return
  showAnswer.value = !showAnswer.value
  if (showAnswer.value && !reviewStartedAt.value) {
    reviewStartedAt.value = Date.now()
  }
}

const handleRate = async (rating: 1 | 2 | 3 | 4) => {
  if (!task.value || !currentCard.value) return
  submitting.value = true
  try {
    const responseTimeMs = reviewStartedAt.value ? Math.max(0, Date.now() - reviewStartedAt.value) : undefined
    const { data } = await submitCardRateApi(task.value.id, {
      cardId: currentCard.value.id,
      rating,
      responseTimeMs
    })
    task.value = data
    showAnswer.value = false
    reviewStartedAt.value = Date.now()
  } catch (error: any) {
    ElMessage.error(error?.message || '提交评分失败')
  } finally {
    submitting.value = false
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

const onTouchStart = (event: TouchEvent) => {
  touchStartX.value = event.touches[0]?.clientX ?? 0
  touchCurrentX.value = touchStartX.value
}

const onTouchMove = (event: TouchEvent) => {
  touchCurrentX.value = event.touches[0]?.clientX ?? touchCurrentX.value
}

const onTouchEnd = () => {
  if (!showAnswer.value || submitting.value) return
  const deltaX = touchCurrentX.value - touchStartX.value
  if (deltaX <= -80) {
    void handleRate(1)
    return
  }
  if (deltaX >= 80) {
    void handleRate(4)
  }
}

watch([taskId, docId], () => {
  void loadTask()
}, { immediate: true })
</script>

<style scoped>
.module-topbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px 18px;
}

.module-topbar__title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: max-content;
}

.module-topbar__heading {
  color: var(--bc-ink);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.1;
}

.module-topbar__center {
  display: flex;
  flex: 1;
  min-width: min(100%, 260px);
}

.module-topbar__action {
  min-width: max-content;
}

.cards-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.cards-empty {
  display: grid;
  min-height: 420px;
  place-items: center;
}

.cards-empty__box,
.cards-state {
  max-width: 32rem;
}

.cards-empty__title,
.cards-state__title,
.cards-setup__title {
  color: var(--bc-ink);
  font-size: clamp(1.6rem, 2vw, 2.2rem);
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.08;
}

.cards-empty__text,
.cards-state__text,
.cards-launch__text {
  margin-top: 0.85rem;
  color: rgb(100 116 139);
  font-size: 0.95rem;
  line-height: 1.9;
}

.cards-setup,
.cards-launch {
  border-radius: 24px;
  border: 1px solid var(--bc-line);
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.1), transparent 30%),
    rgba(255, 255, 255, 0.24);
  padding: 24px;
}

.dark .cards-setup,
.dark .cards-launch {
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.12), transparent 30%),
    rgba(255, 255, 255, 0.03);
}

.cards-setup__head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.cards-setup__label,
.cards-face__eyebrow {
  color: var(--bc-ink-secondary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.cards-setup__dropzone {
  margin-top: 1.35rem;
  border: 1px dashed rgba(var(--bc-accent-rgb), 0.32);
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.08), transparent 60%),
    rgba(255, 255, 255, 0.34);
  padding: 18px 18px 17px;
}

.dark .cards-setup__dropzone {
  background:
    linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.12), transparent 60%),
    rgba(255, 255, 255, 0.04);
}

.cards-setup__dropzone-copy {
  display: flex;
  align-items: center;
  gap: 14px;
}

.cards-setup__dropzone-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 14px;
  background: rgba(var(--bc-accent-rgb), 0.12);
  color: var(--bc-accent);
}

.cards-setup__form {
  display: grid;
  gap: 18px;
  margin-top: 1.35rem;
}

.cards-setup__field {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
}

.cards-setup__field-label {
  min-width: 4.5rem;
  color: var(--bc-ink);
  font-size: 0.95rem;
  font-weight: 600;
}

.cards-setup__hint {
  color: rgb(100 116 139);
  font-size: 0.9rem;
}

.detail-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid var(--bc-line);
  padding: 6px 10px;
  font-size: 11px;
  color: var(--bc-ink-secondary);
}

.flashcard-wrapper {
  perspective: 1000px;
}

.flashcard {
  position: relative;
  width: 100%;
  min-height: 360px;
  max-height: 72vh;
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
  overflow-y: auto;
  backface-visibility: hidden;
}

.flashcard-front {
  justify-content: space-between;
}

.flashcard-back {
  justify-content: center;
  transform: rotateY(180deg);
}

.memory-card {
  border: 1px solid var(--bc-line);
  border-radius: 30px;
  background:
    radial-gradient(circle at 82% 18%, rgba(85, 214, 190, 0.12), transparent 28%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.08), transparent 34%), var(--bc-panel);
  box-shadow:
    var(--bc-shadow),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.cards-face__title {
  margin-top: 2rem;
  color: var(--bc-ink);
  font-size: clamp(1.25rem, 2.5vw, 2rem);
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.55;
}

.cards-face__hint {
  margin-top: 2rem;
  color: rgb(148 163 184);
  font-size: 0.9rem;
}

.cards-answer {
  margin-top: 1rem;
  white-space: pre-wrap;
  color: rgb(51 65 85);
  font-size: 0.96rem;
  line-height: 1.9;
}

.dark .cards-answer {
  color: rgb(226 232 240);
}

.cards-answer__meta {
  margin-top: 1.2rem;
  border-top: 1px solid var(--bc-line);
  padding-top: 1rem;
  color: rgb(100 116 139);
  font-size: 0.82rem;
}

.rating-button {
  min-height: 88px;
  border-width: 1px;
  border-style: solid;
  border-radius: 20px;
  backdrop-filter: blur(10px);
}

.rating-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.rating-button__index {
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.rating-button-again {
  border-color: rgba(244, 63, 94, 0.2);
  background: rgba(244, 63, 94, 0.08);
  color: rgb(190 24 93);
}

.rating-button-hard {
  border-color: rgba(245, 158, 11, 0.22);
  background: rgba(245, 158, 11, 0.1);
  color: rgb(180 83 9);
}

.rating-button-good {
  border-color: rgba(59, 130, 246, 0.2);
  background: rgba(59, 130, 246, 0.08);
  color: rgb(29 78 216);
}

.rating-button-easy {
  border-color: rgba(16, 185, 129, 0.22);
  background: rgba(16, 185, 129, 0.1);
  color: rgb(4 120 87);
}

.cards-state-invalid .cards-state__title {
  color: rgb(190 24 93);
}

@media (max-width: 900px) {
  .module-topbar__center {
    order: 3;
    flex-basis: 100%;
    min-width: 0;
  }
}

@media (max-width: 640px) {
  .module-topbar__heading {
    font-size: 24px;
  }

  .cards-setup,
  .cards-launch {
    padding: 18px;
    border-radius: 20px;
  }

  .flashcard {
    min-height: 420px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .flashcard {
    transition-duration: 0.01ms;
  }
}
</style>
