<template>
  <div class="repair-workbench space-y-5">
    <section v-if="loading" class="shell-section-card p-8 text-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      <p class="mt-4 text-sm text-slate-500">正在加载今天的复习任务...</p>
    </section>

    <template v-else>
      <section v-if="!started" class="shell-section-card workspace-shell">
        <div class="workspace-head">
          <div class="workspace-head__top">
            <div class="workspace-head__main">
              <div class="flex items-center gap-2">
                <h1 class="workspace-title">复习巩固</h1>
                <el-tooltip content="更容易忘记和逾期更久的内容会排在前面。" placement="top">
                  <span
                    class="inline-flex h-4 w-4 cursor-help items-center justify-center rounded-full bg-[var(--interactive-bg)] text-[10px] font-bold text-tertiary"
                    >?</span
                  >
                </el-tooltip>
              </div>
              <p class="workspace-summary">{{ heroTitle }}。{{ heroSummary }}</p>
            </div>
            <div class="review-launch__actions">
              <span class="detail-pill">待复习 {{ stats?.todayPending ?? reviewData?.totalPending ?? 0 }}</span>
              <span class="detail-pill">逾期 {{ stats?.overdueCount ?? reviewData?.overdueCount ?? 0 }}</span>
              <button type="button" class="hard-button-primary" :disabled="!reviewItems.length" @click="startReview">
                {{ reviewItems.length ? '开始今日复习' : '今天没有待复习项' }}
              </button>
            </div>
          </div>

          <div class="review-launch__stats">
            <article class="review-launch__metric">
              <span>当前筛选</span>
              <strong>{{ selectedFilterLabel }}</strong>
            </article>
            <article class="review-launch__metric">
              <span>待复习</span>
              <strong>{{ reviewItems.length }}</strong>
            </article>
            <article class="review-launch__metric">
              <span>逾期</span>
              <strong>{{ stats?.overdueCount ?? reviewData?.overdueCount ?? 0 }}</strong>
            </article>
            <article class="review-launch__metric">
              <span>已完成</span>
              <strong>{{ reviewData?.todayCompleted ?? 0 }}</strong>
            </article>
          </div>
        </div>

        <div class="workspace-separator"></div>

        <div v-if="reviewItems.length" class="workspace-section">
          <div class="queue-head">
            <div>
              <h3 class="workspace-section-title">待复习列表</h3>
              <p class="workspace-section-summary">从最该回看的内容开始，处理完后会自动更新今天状态。</p>
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <span class="detail-pill">连续 {{ stats?.currentStreak ?? reviewData?.currentStreak ?? 0 }} 天</span>
              <RouterLink to="/wrong" class="hard-button-secondary">查看错题本</RouterLink>
            </div>
          </div>

          <div class="review-queue mt-5">
            <article
              v-for="item in reviewItems"
              :key="item.reviewItemId"
              class="repair-card p-4 sm:p-5"
              :class="repairCardClass(item)"
            >
              <div class="flex items-start justify-between gap-4">
                <div class="min-w-0 flex-1">
                  <div class="mb-2 flex flex-wrap items-center gap-2">
                    <span class="hard-chip !bg-amber-100 !text-amber-700">错题复习</span>
                  </div>
                  <h4 class="repair-card__title text-lg font-semibold leading-snug text-ink">{{ item.title }}</h4>
                  <p class="repair-card__summary mt-2 text-sm text-secondary">
                    {{ reviewItemSummary(item) }}
                  </p>
                </div>
                <span class="hard-chip shrink-0" :class="masteryChipClass(item.masteryLevel)">
                  {{ masteryLabel(item.masteryLevel) }}
                </span>
              </div>
            </article>
          </div>
        </div>

        <div v-else class="workspace-section">
          <EmptyState icon="review" :title="emptyStateTitle" :description="emptyStateDescription">
            <template #action>
              <div class="flex justify-center gap-3">
                <RouterLink to="/wrong" class="hard-button-primary">去错题本</RouterLink>
                <RouterLink to="/knowledge" class="hard-button-secondary">去知识库</RouterLink>
              </div>
            </template>
          </EmptyState>
        </div>
      </section>

      <section v-if="started && currentReviewItem" class="review-session-layout">
        <div class="space-y-4">
          <div class="shell-section-card p-4 sm:p-5">
            <div class="flex items-center justify-between text-sm text-secondary">
              <span class="font-mono">{{ currentIndex + 1 }} / {{ reviewItems.length }}</span>
              <div class="flex flex-wrap items-center gap-2">
                <span class="hard-chip !bg-amber-100 !text-amber-700">错题复习</span>
                <span
                  v-if="currentReviewItem.overdueDays > 0"
                  class="rounded-full border border-coral/30 bg-coral/10 px-3 py-1 text-coral"
                >
                  逾期 {{ currentReviewItem.overdueDays }} 天
                </span>
              </div>
            </div>

            <div class="mt-4 h-1.5 w-full overflow-hidden rounded-full bg-[var(--panel-muted)]">
              <div
                class="h-full rounded-full bg-accent transition-[width] duration-300"
                :style="{ width: `${((currentIndex + 1) / Math.max(reviewItems.length, 1)) * 100}%` }"
              ></div>
            </div>
          </div>

          <p class="text-center text-xs text-tertiary sm:hidden">左滑重来 · 右滑良好 · 点击翻转</p>

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
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-secondary">回忆问题</div>
                  <span class="hard-chip">{{ masteryLabel(currentReviewItem.masteryLevel) }}</span>
                </div>
                <div class="mt-6 flex flex-wrap items-center gap-2">
                  <span class="hard-chip !bg-amber-100 !text-amber-700">错题复习</span>
                </div>
                <h3 class="mt-8 text-xl font-semibold leading-relaxed text-ink sm:text-2xl">
                  {{ currentReviewItem.title }}
                </h3>
                <p class="mt-8 text-sm text-tertiary">点击翻转查看答案与解释</p>
              </div>

              <div class="flashcard-back memory-card p-5 sm:p-8">
                <div class="text-xs font-semibold uppercase tracking-[0.2em] text-secondary">标准答案</div>
                <p class="mt-4 whitespace-pre-wrap text-sm leading-7 text-primary">
                  {{ currentReviewItem.answer || '这道题暂时没有标准答案。可以结合题目和错误原因复盘。' }}
                </p>
                <div
                  v-if="currentReviewItem.explanation"
                  class="mt-4 border-t border-slate-200/60 pt-4 dark:border-slate-700/60"
                >
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-secondary">之前错误原因</div>
                  <p class="mt-1 text-sm text-primary">
                    {{ currentReviewItem.explanation }}
                  </p>
                </div>
                <div class="mt-4 border-t border-[var(--bc-line)] pt-4 text-xs text-secondary">
                  复盘系数 {{ formatEaseFactor(currentReviewItem.easeFactor) }} · 间隔
                  {{ currentReviewItem.intervalDays ?? 0 }} 天
                </div>
              </div>
            </div>
          </div>

          <div v-if="showAnswer" class="grid grid-cols-2 gap-2 sm:grid-cols-4 sm:gap-3">
            <button
              v-for="btn in ratingButtons"
              :key="btn.rating"
              type="button"
              class="rating-button flex flex-col items-center gap-1 p-3 text-sm font-semibold transition-[background-color,color,border-color,box-shadow,transform] duration-150"
              :class="btn.class"
              :disabled="submitting"
              @click="handleRate(btn.rating)"
            >
              <span class="text-base">{{ btn.symbol }}</span>
              <span>{{ btn.label }}</span>
            </button>
          </div>
        </div>

        <aside class="shell-section-card p-5 sm:p-6 review-session-aside">
          <p class="section-kicker">当前状态</p>
          <div class="mt-5 space-y-4">
            <div class="review-status-row">
              <span>当前筛选</span>
              <strong>{{ selectedFilterLabel }}</strong>
            </div>
            <div class="review-status-row">
              <span>待复习</span>
              <strong>{{ reviewItems.length }} 项</strong>
            </div>
            <div class="review-status-row">
              <span>逾期</span>
              <strong>{{ stats?.overdueCount ?? reviewData?.overdueCount ?? 0 }} 项</strong>
            </div>
            <div class="review-status-row">
              <span>连续</span>
              <strong>{{ stats?.currentStreak ?? reviewData?.currentStreak ?? 0 }} 天</strong>
            </div>
            <div class="review-status-row">
              <span>已完成</span>
              <strong>{{ reviewData?.todayCompleted ?? 0 }} 项</strong>
            </div>
          </div>
        </aside>
      </section>

      <section v-else-if="started && !currentReviewItem" class="shell-section-card p-8">
        <EmptyState
          icon="trophy"
          title="本轮复习已完成"
          :description="`已处理 ${reviewItems.length} 项 · 重来 ${againCount} 次`"
        >
          <template #action>
            <div class="flex justify-center gap-3">
              <button type="button" class="hard-button-secondary" @click="resetSession">返回任务总览</button>
              <RouterLink to="/wrong" class="hard-button-primary">回到错题本</RouterLink>
            </div>
          </template>
        </EmptyState>
      </section>

    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { ERROR_COPY } from '@/constants/productCopy'
import { fetchReviewStatsApi, fetchReviewTodayApi, submitReviewRateApi } from '@/api/review'
import type { ReviewContentType, ReviewStats, ReviewTodayData, UnifiedReviewItem } from '@/types/api'

const loading = ref(true)
const submitting = ref(false)
const started = ref(false)
const showAnswer = ref(false)
const currentIndex = ref(0)
const againCount = ref(0)
const selectedContentType = ref<ReviewContentType>('wrong_card')
const reviewData = ref<ReviewTodayData | null>(null)
const stats = ref<ReviewStats | null>(null)

let touchStartX = 0
let touchStartY = 0
let touchStartTime = 0

const reviewItems = computed(() => reviewData.value?.items ?? [])
const currentReviewItem = computed(() => reviewItems.value[currentIndex.value] ?? null)

const heroTitle = computed(() => {
  if (!reviewItems.value.length) {
    return '今天没有待复习错题'
  }
  return `今天需要复习 ${reviewItems.value.length} 项`
})

const heroSummary = computed(() => {
  if (!reviewItems.value.length) {
    return '可以回到错题本补题，或开始新的训练。'
  }
  return '处理今天到期和逾期的内容。'
})

const selectedFilterLabel = computed(() => '错题本')

const ratingButtons = [
  { rating: 1 as const, label: '重来', symbol: '↺', class: 'border-coral/30 bg-coral/10 text-coral hover:bg-coral/15' },
  { rating: 2 as const, label: '困难', symbol: '△', class: 'border-amber/30 bg-amber/10 text-amber hover:bg-amber/15' },
  { rating: 3 as const, label: '良好', symbol: '✓', class: 'border-cyan/30 bg-cyan/10 text-cyan hover:bg-cyan/15' },
  { rating: 4 as const, label: '轻松', symbol: '↑', class: 'border-lime/30 bg-lime/10 text-lime hover:bg-lime/15' }
]

const emptyStateTitle = computed(() => {
  return '今天的到期错题已经清空'
})

const emptyStateDescription = computed(() => {
  return '可以回到错题本补低分题，或开始新的模拟面试，为后续复习积累内容。'
})

const masteryLabel = (level: string) => {
  const map: Record<string, string> = {
    not_started: '未开始',
    reviewing: '复习中',
    mastered: '已掌握'
  }
  return map[level] || level
}

const masteryChipClass = (level: string) => {
  if (level === 'mastered') return '!bg-accent !text-white'
  if (level === 'reviewing') return '!bg-amber-100 !text-amber-700'
  return '!bg-[var(--interactive-bg)] !text-secondary'
}

const repairCardClass = (item: UnifiedReviewItem) => {
  if (item.overdueDays > 0) return 'repair-card-due'
  return 'repair-card-new'
}

const reviewItemSummary = (item: UnifiedReviewItem) => {
  const dueLabel = item.overdueDays > 0 ? `逾期 ${item.overdueDays} 天` : '今天处理'
  return dueLabel
}

const formatEaseFactor = (value?: number) => {
  return typeof value === 'number' ? value.toFixed(2) : '2.50'
}

const loadReviewData = async () => {
  const [reviewRes, statsRes] = await Promise.all([
    fetchReviewTodayApi(selectedContentType.value),
    fetchReviewStatsApi()
  ])
  reviewData.value = reviewRes.data
  stats.value = statsRes.data
}

const loadData = async () => {
  loading.value = true
  try {
    await loadReviewData()
  } catch {
    ElMessage.error(ERROR_COPY.reviewTodayLoadFailed)
  } finally {
    loading.value = false
  }
}

const resetSession = () => {
  started.value = false
  showAnswer.value = false
  currentIndex.value = 0
  againCount.value = 0
}

const startReview = () => {
  if (!reviewItems.value.length) return
  started.value = true
  currentIndex.value = 0
  showAnswer.value = false
  againCount.value = 0
}

const flipCard = () => {
  if (!showAnswer.value) showAnswer.value = true
}

const onTouchStart = (e: TouchEvent) => {
  const touch = e.touches.item(0)
  if (!touch) return
  touchStartX = touch.clientX
  touchStartY = touch.clientY
  touchStartTime = Date.now()
}

const onTouchMove = (e: TouchEvent) => {
  const touch = e.touches.item(0)
  if (!touch) return
  const dx = Math.abs(touch.clientX - touchStartX)
  const dy = Math.abs(touch.clientY - touchStartY)
  if (dx > dy && dx > 10) e.preventDefault()
}

const onTouchEnd = (e: TouchEvent) => {
  const touch = e.changedTouches.item(0)
  if (!touch) return
  const dx = touch.clientX - touchStartX
  const dy = touch.clientY - touchStartY
  const dt = Date.now() - touchStartTime
  if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 50 && dt < 500 && showAnswer.value) {
    if (dx < 0) void handleRate(1)
    else void handleRate(3)
  }
}

const handleRate = async (rating: 1 | 2 | 3 | 4) => {
  if (submitting.value || !currentReviewItem.value) return
  submitting.value = true

  if (rating <= 2) againCount.value++

  try {
    const { data } = await submitReviewRateApi(currentReviewItem.value.reviewItemId, {
      contentType: currentReviewItem.value.contentType,
      rating
    })
    reviewData.value = data
    const statsRes = await fetchReviewStatsApi()
    stats.value = statsRes.data
    showAnswer.value = false
    currentIndex.value++
    if (currentIndex.value >= reviewItems.value.length) {
      currentIndex.value = reviewItems.value.length
    }
  } catch {
    ElMessage.error(ERROR_COPY.reviewSubmitFailed)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void loadData()
})
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
  letter-spacing: 0;
  line-height: 1.1;
  text-wrap: balance;
}

.module-topbar__center {
  display: flex;
  flex: 1;
  min-width: min(100%, 320px);
}

.module-topbar__action {
  min-width: max-content;
}

.repair-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.review-workbench {
  display: grid;
  gap: 18px;
}

.review-session-layout {
  display: grid;
  gap: 18px;
}

.repair-filter-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  border: 1px solid var(--bc-line);
  border-radius: 999px;
  background: var(--interactive-bg);
  padding: 0 16px;
  color: var(--bc-ink-secondary);
  font-size: 13px;
  font-weight: 600;
  transition:
    border-color var(--motion-base) var(--ease-hard),
    color var(--motion-base) var(--ease-hard),
    background-color var(--motion-base) var(--ease-hard);
}

.dark .repair-filter-chip {
  background: var(--interactive-bg);
}

.repair-filter-chip-active {
  border-color: rgba(var(--bc-accent-rgb), 0.28);
  color: var(--bc-ink);
  background: rgba(var(--bc-accent-rgb), 0.1);
}

.repair-filter-chip__count {
  min-width: 18px;
  border-radius: 999px;
  background: rgba(var(--bc-accent-rgb), 0.14);
  padding: 2px 6px;
  font-size: 11px;
}

.review-launch {
  display: grid;
  gap: 14px;
}

.review-launch__head,
.queue-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px 16px;
}

.review-launch__copy {
  min-width: 0;
  flex: 1;
}

.review-launch__signals {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.review-launch__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.review-launch__metric {
  border-radius: 14px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 9px 11px;
}

.review-launch__metric span,
.review-launch__metric small {
  display: block;
  color: var(--bc-ink-secondary);
  font-size: 12px;
}

.review-launch__metric strong {
  display: block;
  margin-top: 6px;
  color: var(--bc-ink);
  font-size: 1.1rem;
  font-weight: 780;
  letter-spacing: 0;
  font-variant-numeric: tabular-nums;
}

.review-launch__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
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

.review-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.review-status-row span {
  color: var(--bc-ink-secondary);
  font-size: 13px;
}

.review-status-row strong {
  color: var(--bc-ink);
  font-size: 1.05rem;
  font-weight: 700;
}

.flashcard-wrapper {
  perspective: 1000px;
}

.flashcard {
  position: relative;
  width: 100%;
  min-height: 320px;
  max-height: 70vh;
  transition: transform 0.42s var(--ease-hard);
  transform-style: preserve-3d;
}

@media (min-width: 640px) {
  .flashcard {
    min-height: 360px;
  }
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
  justify-content: center;
  overflow-y: auto;
  backface-visibility: hidden;
}

.flashcard-back {
  transform: rotateY(180deg);
}

.memory-card {
  border: 1px solid var(--bc-line);
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 80% 20%, rgba(85, 214, 190, 0.1), transparent 30%),
    linear-gradient(145deg, rgba(var(--bc-accent-rgb), 0.04), transparent 34%), var(--panel-bg);
  box-shadow:
    var(--bc-shadow),
    inset 0 1px 0 rgba(var(--bc-ink-rgb), 0.04);
}

.rating-button {
  min-height: 92px;
  border-width: 1px;
  border-style: solid;
  border-radius: var(--radius-md);
  backdrop-filter: blur(10px);
}

.rating-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.repair-card {
  position: relative;
  overflow: hidden;
  border-left-width: 3px;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.review-queue {
  overflow: hidden;
  border-radius: 24px;
  background: rgba(var(--bc-ink-rgb), 0.02);
}

.review-queue .repair-card + .repair-card {
  border-top: 1px solid rgba(148, 163, 184, 0.16);
}

.repair-card-due {
  border-left-color: var(--bc-coral);
}

.repair-card-new {
  border-left-color: var(--bc-amber);
}

.repair-card-active {
  border-left-color: var(--bc-cyan);
}

.repair-card__summary {
  line-height: 1.7;
}

.repair-card__answer {
  border-radius: 18px;
  padding: 16px 16px 15px;
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.08), transparent 36%), var(--panel-muted);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.12);
}

.dark .repair-card__answer {
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.12), transparent 42%), var(--panel-muted);
}

.repair-card__answer-label,
.repair-card__reason-label {
  color: var(--bc-ink-secondary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

@media (max-width: 900px) {
  .module-topbar__center {
    order: 3;
    flex-basis: 100%;
    min-width: 0;
  }

  .review-launch__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1200px) {
  .review-workbench {
    grid-template-columns: minmax(0, 1.15fr) 300px;
    align-items: start;
  }

  .review-session-layout {
    grid-template-columns: minmax(0, 1.15fr) 300px;
    align-items: start;
  }

  .review-session-aside {
    position: sticky;
    top: 88px;
  }
}

@media (max-width: 640px) {
  .module-topbar__heading {
    font-size: 24px;
  }

  .review-launch {
    gap: 12px;
  }

  .review-launch__stats {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .flashcard {
    transition-duration: 0.01ms;
  }
}
</style>
