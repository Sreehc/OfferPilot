<template>
  <div class="cards-workbench space-y-5">
    <section class="cockpit-panel p-4 sm:p-5">
      <div class="module-topbar">
        <div class="module-topbar__title">
          <span class="state-pulse" aria-hidden="true"></span>
          <h2 class="module-topbar__heading">今日卡片</h2>
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
      <p class="mt-4 text-sm text-slate-500">加载今日卡片任务...</p>
    </section>

    <template v-else>
      <section v-if="!task && !docId" class="cockpit-panel cards-empty p-8 sm:p-10">
        <div class="cards-empty__box">
          <h3 class="cards-empty__title">还没有今日卡片任务</h3>
          <p class="cards-empty__text">从知识库选择一份资料，生成一轮按天推进的记忆任务。</p>
          <RouterLink to="/knowledge" class="hard-button-primary mt-6">从知识库选择资料</RouterLink>
        </div>
      </section>

      <section v-else-if="docId && !task" class="cockpit-panel p-5 sm:p-6">
        <div class="cards-setup">
          <div class="cards-setup__grid">
            <div class="cards-setup__main">
              <div class="cards-setup__head">
                <div>
                  <p class="cards-setup__label">已选择资料</p>
                  <h3 class="cards-setup__title">{{ docTitle || `文档 #${docId}` }}</h3>
                  <p class="cards-setup__subtitle">系统会自动生成问题与答案卡片，你只需要决定几天完成这一轮记忆。</p>
                </div>
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
                    <p class="text-sm font-semibold text-ink">系统会自动整理核心问题、答案和记忆顺序</p>
                    <p class="mt-1 text-sm text-slate-600 dark:text-slate-300">第一版只处理当前这份资料，不混入其他文档。</p>
                  </div>
                </div>
              </div>

              <div class="cards-setup__form">
                <div class="cards-setup__field cards-setup__field-inline">
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
                  <span>按 {{ days }} 天完成，系统会自动平均分配每日卡片量。</span>
                </div>
              </div>

              <div class="cards-setup__actions">
                <button type="button" class="hard-button-primary" :disabled="creatingTask" @click="createTask">
                  {{ creatingTask ? '生成中...' : '生成今日卡片' }}
                </button>
                <RouterLink to="/knowledge" class="hard-button-secondary">返回知识库</RouterLink>
              </div>
            </div>

            <aside class="cards-setup__summary">
              <div class="cards-summary-card">
                <div class="cards-summary-card__header">
                  <p class="cards-summary-card__label">任务摘要</p>
                  <span class="detail-pill">单文档任务</span>
                </div>

                <div class="cards-summary-card__list">
                  <div class="cards-summary-card__item">
                    <span class="cards-summary-card__name">完成周期</span>
                    <strong>{{ days }} 天</strong>
                  </div>
                  <div class="cards-summary-card__item">
                    <span class="cards-summary-card__name">任务节奏</span>
                    <strong>系统自动平均分配</strong>
                  </div>
                  <div class="cards-summary-card__item">
                    <span class="cards-summary-card__name">复习方式</span>
                    <strong>翻牌后评分推进</strong>
                  </div>
                  <div class="cards-summary-card__item">
                    <span class="cards-summary-card__name">评分标准</span>
                    <strong>重来 / 困难 / 良好 / 轻松</strong>
                  </div>
                </div>
              </div>
            </aside>
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

      <section v-else-if="task?.status === 'active' && !studyMode" class="cockpit-panel p-5 sm:p-6">
        <div class="cards-launch">
          <div class="cards-launch__head">
            <div>
              <p class="cards-setup__label">当前任务</p>
              <h3 class="cards-setup__title">{{ task.docTitle }}</h3>
              <p class="cards-launch__text">
                第 {{ task.currentDay }} / {{ task.days }} 天，已推进 {{ task.masteredCards }} / {{ task.totalCards }} 张卡片，
                当前还有 {{ task.dueCount }} 张待处理。
              </p>
            </div>

            <div class="cards-launch__signals">
              <span class="detail-pill">今日已评 {{ task.reviewedTodayCount }}</span>
              <span class="detail-pill">累计评分 {{ task.reviewCount }}</span>
            </div>
          </div>

          <div class="mt-6 flex flex-wrap gap-3">
            <button type="button" class="hard-button-primary" :disabled="startingTask" @click="enterStudyMode">
              继续学习
            </button>
            <button type="button" class="hard-button-secondary" :disabled="creatingTask" @click="restartTask">
              重新开始本轮
            </button>
            <RouterLink to="/knowledge" class="hard-button-secondary">返回知识库</RouterLink>
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

      <section v-else-if="task && currentCard && studyMode" class="mx-auto max-w-5xl px-1 sm:px-0">
        <div class="mb-4 flex items-center justify-between gap-3 text-sm text-slate-500 dark:text-slate-400">
          <span class="font-mono">{{ currentCard.sortOrder }} / {{ task.totalCards }}</span>
          <div class="flex flex-wrap items-center justify-end gap-2">
            <button type="button" class="cards-inline-link" @click="exitStudyMode">返回任务页</button>
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
              <div class="cards-face">
                <div class="flex items-center justify-between gap-4">
                  <div class="cards-face__eyebrow">先回忆问题</div>
                  <span class="hard-chip">{{ stateLabel(currentCard.state) }}</span>
                </div>
                <div class="cards-face__content cards-face__content-question">
                  <div class="cards-question-shell">
                    <span class="cards-question-shell__label">问题</span>
                    <h3 class="cards-face__title cards-face__title-question">
                      {{ currentCard.question }}
                    </h3>
                  </div>
                </div>
                <p class="cards-face__hint">点击翻牌查看答案</p>
              </div>
            </div>

            <div class="flashcard-back memory-card p-5 sm:p-8">
              <div class="cards-face cards-face-back">
                <div class="cards-face__eyebrow">参考答案</div>
                <div class="cards-face__content cards-face__content-answer">
                  <div class="cards-answer-shell">
                    <span class="cards-answer-shell__label">回答要点</span>
                    <p class="cards-answer">
                      {{ currentCard.answer }}
                    </p>
                  </div>
                </div>
                <div class="cards-answer__meta">
                  当前记忆系数 {{ formatEaseFactor(currentCard.easeFactor) }} · 当前间隔 {{ currentCard.intervalDays ?? 0 }} 天
                </div>
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

      <section v-else-if="task?.status === 'active' && studyMode" class="cockpit-panel p-6 sm:p-8">
        <div class="cards-state cards-state-rest">
          <h3 class="cards-state__title">当前这一组已经学完</h3>
          <p class="cards-state__text">
            当前没有可继续评分的卡片了。你可以先返回任务页查看进度，或重新开始这一轮任务。
          </p>
          <div class="mt-5 flex flex-wrap gap-3">
            <button type="button" class="hard-button-primary" @click="exitStudyMode">
              返回任务页
            </button>
            <button type="button" class="hard-button-secondary" :disabled="creatingTask" @click="restartTask">
              重新开始本轮
            </button>
            <RouterLink to="/knowledge" class="hard-button-secondary">返回知识库</RouterLink>
          </div>
        </div>
      </section>

      <section v-else-if="task" class="cockpit-panel p-6 sm:p-8">
        <div class="cards-state">
          <h3 class="cards-state__title">当前卡片暂时没有加载出来</h3>
          <p class="cards-state__text">可以刷新页面重试，或重新开始本轮任务。</p>
          <div class="mt-5 flex flex-wrap gap-3">
            <button type="button" class="hard-button-primary" :disabled="creatingTask" @click="restartTask">
              重新开始本轮
            </button>
            <button
              v-if="task.status === 'active'"
              type="button"
              class="hard-button-secondary"
              @click="exitStudyMode"
            >
              返回任务页
            </button>
            <RouterLink to="/knowledge" class="hard-button-secondary">返回知识库</RouterLink>
          </div>
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
  const value = Array.isArray(raw) ? raw[0] : raw
  return typeof value === 'string' && value.length > 0 ? value : null
})

const taskId = computed(() => {
  const raw = route.query.taskId
  const value = Array.isArray(raw) ? raw[0] : raw
  return typeof value === 'string' && value.length > 0 ? value : null
})

const docTitle = computed(() => {
  const raw = route.query.title
  return typeof raw === 'string' ? raw : ''
})

const studyMode = computed(() => route.query.view === 'study')

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
      syncRouteToTask(studyMode.value ? 'study' : undefined)
      return
    }
    const { data } = await fetchActiveCardTaskApi()
    task.value = data
    syncRouteToTask(studyMode.value ? 'study' : undefined)
  } catch (error: any) {
    task.value = null
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    loading.value = false
  }
}

const syncRouteToTask = (view?: 'study') => {
  if (!task.value) return
  if (
    String(route.query.taskId ?? '') === String(task.value.id) &&
    String(route.query.docId ?? '') === String(task.value.docId) &&
    String(route.query.title ?? '') === task.value.docTitle &&
    String(route.query.view ?? '') === String(view ?? '')
  ) {
    return
  }
  router.replace({
    path: '/cards',
    query: {
      taskId: String(task.value.id),
      docId: String(task.value.docId),
      title: task.value.docTitle,
      ...(view ? { view } : {})
    }
  })
}

const createTask = async () => {
  if (!docId.value) return
  creatingTask.value = true
  try {
    const numericDocId = Number(docId.value)
    if (!Number.isFinite(numericDocId)) {
      throw new Error('文档 ID 无效')
    }
    const { data } = await createCardTaskApi({ docId: numericDocId, days: days.value })
    task.value = data
    showAnswer.value = false
    syncRouteToTask()
    ElMessage.success('今日卡片已生成')
  } catch (error: any) {
    ElMessage.error(error?.message || '生成今日卡片失败')
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
    syncRouteToTask('study')
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
    syncRouteToTask()
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
  const activeCardId = currentCard.value.id
  submitting.value = true
  try {
    const responseTimeMs = reviewStartedAt.value ? Math.max(0, Date.now() - reviewStartedAt.value) : undefined
    const { data } = await submitCardRateApi(task.value.id, {
      cardId: activeCardId,
      rating,
      responseTimeMs
    })
    task.value = data
    showAnswer.value = false
    reviewStartedAt.value = data.currentCard ? Date.now() : null
    syncRouteToTask(task.value.status === 'active' ? 'study' : undefined)
  } catch (error: any) {
    ElMessage.error(error?.message || '提交评分失败')
  } finally {
    submitting.value = false
  }
}

const enterStudyMode = () => {
  syncRouteToTask('study')
}

const exitStudyMode = () => {
  syncRouteToTask()
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

.cards-launch__head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.cards-launch__signals {
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

.cards-setup__grid {
  display: grid;
  gap: 1.1rem;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.7fr);
  align-items: start;
}

.cards-setup__main {
  min-width: 0;
}

.cards-setup__summary {
  min-width: 0;
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
  gap: 10px;
}

.cards-setup__subtitle {
  margin-top: 0.75rem;
  max-width: 40rem;
  color: rgb(100 116 139);
  font-size: 0.95rem;
  line-height: 1.8;
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
  gap: 14px;
  margin-top: 1.15rem;
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

.cards-setup__field-inline {
  gap: 18px;
}

.cards-setup__field-inline :deep(.el-input-number) {
  width: 13rem;
}

.cards-setup__hint {
  color: rgb(100 116 139);
  font-size: 0.9rem;
}

.cards-setup__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1.35rem;
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

.cards-inline-link {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  border: 1px solid var(--bc-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.42);
  padding: 0 12px;
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 600;
  transition:
    border-color var(--motion-base) var(--ease-hard),
    color var(--motion-base) var(--ease-hard),
    background-color var(--motion-base) var(--ease-hard);
}

.cards-inline-link:hover {
  border-color: rgba(var(--bc-accent-rgb), 0.28);
  color: var(--bc-ink);
  background: rgba(var(--bc-accent-rgb), 0.08);
}

.cards-summary-card {
  border: 1px solid rgba(var(--bc-accent-rgb), 0.12);
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.08), transparent 55%),
    rgba(255, 255, 255, 0.48);
  padding: 1.1rem;
  box-shadow: var(--bc-shadow-soft);
}

.dark .cards-summary-card {
  background:
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.1), transparent 55%),
    rgba(255, 255, 255, 0.04);
}

.cards-summary-card__header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.cards-summary-card__label {
  color: var(--bc-ink);
  font-size: 0.95rem;
  font-weight: 700;
}

.cards-summary-card__list {
  display: grid;
  gap: 0.8rem;
  margin-top: 1rem;
}

.cards-summary-card__item {
  display: flex;
  flex-direction: column;
  gap: 0.28rem;
  border-top: 1px solid rgba(var(--bc-accent-rgb), 0.08);
  padding-top: 0.8rem;
}

.cards-summary-card__item:first-child {
  border-top: 0;
  padding-top: 0;
}

.cards-summary-card__name {
  color: rgb(100 116 139);
  font-size: 0.82rem;
}

.cards-summary-card__item strong {
  color: var(--bc-ink);
  font-size: 0.96rem;
  font-weight: 700;
  line-height: 1.5;
}

.flashcard-wrapper {
  perspective: 1000px;
}

.flashcard {
  position: relative;
  width: 100%;
  min-height: 420px;
  height: min(68vh, 640px);
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
  border-radius: 30px;
  background:
    radial-gradient(circle at 82% 18%, rgba(85, 214, 190, 0.12), transparent 28%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.08), transparent 34%), var(--bc-panel);
  box-shadow:
    var(--bc-shadow),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.cards-face {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 1.25rem;
  height: 100%;
  min-height: 0;
}

.cards-face-back {
  gap: 1rem;
}

.cards-face__content {
  min-height: 0;
  overflow-y: auto;
  padding-right: 0.35rem;
}

.cards-face__content-question {
  display: flex;
  align-items: flex-start;
}

.cards-face__content-answer {
  display: block;
}

.cards-question-shell,
.cards-answer-shell {
  width: 100%;
  border-radius: 24px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.1);
  background:
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.08), transparent 60%),
    rgba(255, 255, 255, 0.34);
  padding: 1.1rem 1.15rem 1.2rem;
}

.dark .cards-question-shell,
.dark .cards-answer-shell {
  background:
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.1), transparent 60%),
    rgba(255, 255, 255, 0.04);
}

.cards-question-shell__label,
.cards-answer-shell__label {
  display: inline-flex;
  margin-bottom: 0.8rem;
  color: var(--bc-ink-secondary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.cards-face__title {
  max-width: none;
  color: var(--bc-ink);
  font-size: clamp(1.16rem, 1.6vw, 1.56rem);
  font-weight: 650;
  letter-spacing: -0.04em;
  line-height: 1.72;
  text-wrap: pretty;
  white-space: pre-wrap;
}

.cards-face__title-question {
  max-width: 42rem;
}

.cards-face__hint {
  color: rgb(148 163 184);
  font-size: 0.85rem;
}

.cards-answer {
  max-width: 44rem;
  white-space: pre-wrap;
  color: rgb(51 65 85);
  font-size: 1rem;
  line-height: 1.88;
}

.dark .cards-answer {
  color: rgb(226 232 240);
}

.cards-answer__meta {
  border-top: 1px solid var(--bc-line);
  padding-top: 0.95rem;
  color: rgb(100 116 139);
  font-size: 0.82rem;
}

.cards-state-rest {
  max-width: 36rem;
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

  .cards-setup__grid {
    grid-template-columns: minmax(0, 1fr);
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

  .cards-setup__field-inline {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .cards-setup__field-inline :deep(.el-input-number) {
    width: 100%;
    max-width: 12rem;
  }

  .flashcard {
    min-height: 520px;
    height: min(74vh, 760px);
  }

  .cards-face__title {
    max-width: none;
    font-size: clamp(1.08rem, 4.8vw, 1.35rem);
    line-height: 1.74;
  }

  .cards-answer {
    font-size: 0.96rem;
    line-height: 1.82;
  }
}

@media (prefers-reduced-motion: reduce) {
  .flashcard {
    transition-duration: 0.01ms;
  }
}
</style>
