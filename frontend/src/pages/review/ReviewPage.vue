<template>
  <div class="repair-workbench space-y-6">
    <AppShellHeader />

    <section class="shell-section-card p-4 sm:p-5">
      <div class="repair-filter-row">
        <button
          v-for="filter in contentFilters"
          :key="filter.value"
          type="button"
          class="repair-filter-chip"
          :class="{ 'repair-filter-chip-active': selectedContentType === filter.value }"
          @click="changeContentType(filter.value)"
        >
          {{ filter.label }}
          <span class="repair-filter-chip__count">{{ contentCount(filter.value) }}</span>
        </button>
      </div>
    </section>

    <section v-if="loading" class="shell-section-card p-8 text-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      <p class="mt-4 text-sm text-slate-500">加载复习数据...</p>
    </section>

    <template v-else>
      <section v-if="!started" class="shell-section-card p-5 sm:p-6">
        <div class="review-launch">
          <div class="review-launch__head">
            <div>
              <div class="flex items-center gap-2">
                <p class="section-kicker">复习工作台</p>
                <el-tooltip content="系统根据遗忘曲线自动安排复习时间，逾期越久优先级越高。" placement="top">
                  <span class="inline-flex h-4 w-4 cursor-help items-center justify-center rounded-full bg-slate-200 text-[10px] font-bold text-slate-500 dark:bg-slate-700 dark:text-slate-400">?</span>
                </el-tooltip>
              </div>
              <p class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink sm:text-3xl">{{ heroTitle }}</p>
            </div>
            <div class="review-launch__signals">
              <span class="detail-pill">待复习 {{ stats?.todayPending ?? reviewData?.totalPending ?? 0 }}</span>
              <span class="detail-pill">逾期 {{ stats?.overdueCount ?? reviewData?.overdueCount ?? 0 }}</span>
              <span class="detail-pill">连续 {{ stats?.currentStreak ?? reviewData?.currentStreak ?? 0 }} 天</span>
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

          <div class="mt-6 flex flex-wrap gap-3">
            <button type="button" class="hard-button-primary" :disabled="!reviewItems.length" @click="startReview">
              {{ reviewItems.length ? '开始今日复习' : '当前无待复习项' }}
            </button>
            <RouterLink to="/cards" class="hard-button-secondary">去今日卡片</RouterLink>
          </div>
        </div>
      </section>

      <section v-if="started && currentReviewItem" class="review-session-layout">
        <div class="space-y-4">
          <div class="shell-section-card p-4 sm:p-5">
            <div class="flex items-center justify-between text-sm text-slate-500 dark:text-slate-400">
              <span class="font-mono">{{ currentIndex + 1 }} / {{ reviewItems.length }}</span>
              <div class="flex flex-wrap items-center gap-2">
                <span class="hard-chip" :class="contentChipClass(currentReviewItem.contentType)">
                  {{ contentTypeLabel(currentReviewItem.contentType) }}
                </span>
                <span
                  v-if="currentReviewItem.overdueDays > 0"
                  class="rounded-full border border-coral/30 bg-coral/10 px-3 py-1 text-coral"
                >
                  逾期 {{ currentReviewItem.overdueDays }} 天
                </span>
              </div>
            </div>

            <div class="mt-4 h-1.5 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-white/10">
              <div
                class="h-full rounded-full bg-accent transition-all duration-300"
                :style="{ width: `${((currentIndex + 1) / Math.max(reviewItems.length, 1)) * 100}%` }"
              ></div>
            </div>
          </div>

          <p class="text-center text-xs text-slate-400 dark:text-slate-500 sm:hidden">
            左滑重来 · 右滑良好 · 点击翻转
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
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                    先回忆问题
                  </div>
                  <span class="hard-chip">{{ masteryLabel(currentReviewItem.masteryLevel) }}</span>
                </div>
                <div class="mt-6 flex flex-wrap items-center gap-2">
                  <span class="hard-chip" :class="contentChipClass(currentReviewItem.contentType)">
                    {{ contentTypeLabel(currentReviewItem.contentType) }}
                  </span>
                  <span v-if="currentReviewItem.deckTitle" class="detail-pill">{{ currentReviewItem.deckTitle }}</span>
                </div>
                <h3 class="mt-8 text-xl font-semibold leading-relaxed text-ink sm:text-2xl">
                  {{ currentReviewItem.title }}
                </h3>
                <p class="mt-8 text-sm text-slate-400 dark:text-slate-500">点击翻转查看答案与解释</p>
              </div>

              <div class="flashcard-back memory-card p-5 sm:p-8">
                <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                  标准答案
                </div>
                <p class="mt-4 whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-slate-200">
                  {{ currentReviewItem.answer || '暂无标准答案' }}
                </p>
                <div
                  v-if="currentReviewItem.explanation"
                  class="mt-4 border-t border-slate-200/60 pt-4 dark:border-slate-700/60"
                >
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                    {{ currentReviewItem.contentType === 'knowledge_card' ? '解释说明' : '之前错误原因' }}
                  </div>
                  <p class="mt-1 text-sm text-slate-700 dark:text-slate-200">
                    {{ currentReviewItem.explanation }}
                  </p>
                </div>
                <div
                  v-if="currentReviewItem.sourceQuote && currentReviewItem.contentType === 'knowledge_card'"
                  class="mt-4 border-t border-[var(--bc-line)] pt-4"
                >
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                    来源片段
                  </div>
                  <p class="mt-1 text-sm leading-7 text-slate-600 dark:text-slate-300">
                    {{ currentReviewItem.sourceQuote }}
                  </p>
                </div>
                <div class="mt-4 border-t border-[var(--bc-line)] pt-4 text-xs text-slate-500 dark:text-slate-400">
                  记忆系数 {{ formatEaseFactor(currentReviewItem.easeFactor) }} · 间隔
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
              class="rating-button flex flex-col items-center gap-1 p-3 text-sm font-semibold transition-all"
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
              <RouterLink to="/cards" class="hard-button-primary">继续今日卡片</RouterLink>
            </div>
          </template>
        </EmptyState>
      </section>

      <section v-else-if="reviewItems.length" class="shell-section-card p-5 sm:p-6">
        <div class="queue-head">
          <div>
            <p class="section-kicker">待复习列表</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">今天先处理这些内容</h3>
          </div>
          <span class="detail-pill">{{ reviewItems.length }} 项</span>
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
                  <span class="hard-chip" :class="contentChipClass(item.contentType)">
                    {{ contentTypeLabel(item.contentType) }}
                  </span>
                  <span v-if="item.deckTitle" class="detail-pill">{{ item.deckTitle }}</span>
                </div>
                <h4 class="repair-card__title text-lg font-semibold leading-snug text-ink">{{ item.title }}</h4>
                <p class="repair-card__summary mt-2 text-sm text-slate-500 dark:text-slate-400">
                  {{ reviewItemSummary(item) }}
                </p>
              </div>
              <span class="hard-chip shrink-0" :class="masteryChipClass(item.masteryLevel)">
                {{ masteryLabel(item.masteryLevel) }}
              </span>
            </div>
          </article>
        </div>
      </section>

      <section v-else class="shell-section-card p-8">
        <EmptyState
          icon="review"
          :title="emptyStateTitle"
          :description="emptyStateDescription"
        >
          <template #action>
            <div class="flex justify-center gap-3">
              <RouterLink to="/cards" class="hard-button-primary">去今日卡片</RouterLink>
              <RouterLink to="/knowledge" class="hard-button-secondary">去知识库</RouterLink>
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
import AppShellHeader from '@/components/AppShellHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { fetchReviewStatsApi, fetchReviewTodayApi, submitReviewRateApi } from '@/api/review'
import type { ReviewContentType, ReviewStats, ReviewTodayData, UnifiedReviewItem } from '@/types/api'

const loading = ref(true)
const submitting = ref(false)
const started = ref(false)
const showAnswer = ref(false)
const currentIndex = ref(0)
const againCount = ref(0)
const selectedContentType = ref<ReviewContentType>('all')
const reviewData = ref<ReviewTodayData | null>(null)
const stats = ref<ReviewStats | null>(null)

let touchStartX = 0
let touchStartY = 0
let touchStartTime = 0

const contentFilters: Array<{ value: ReviewContentType; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'knowledge_card', label: '知识卡片' },
  { value: 'wrong_card', label: '错题卡片' },
  { value: 'interview_card', label: '面试卡片' }
]

const reviewItems = computed(() => reviewData.value?.items ?? [])
const currentReviewItem = computed(() => reviewItems.value[currentIndex.value] ?? null)

const heroTitle = computed(() => {
  if (!reviewItems.value.length) {
    return selectedContentType.value === 'interview_card' ? '当前没有面试卡片' : '今天没有待复习内容'
  }
  return `今天先复习这 ${reviewItems.value.length} 项`
})

const selectedFilterLabel = computed(() => {
  return contentFilters.find((item) => item.value === selectedContentType.value)?.label ?? '全部'
})

const ratingButtons = [
  { rating: 1 as const, label: '重来', symbol: '↺', class: 'border-coral/30 bg-coral/10 text-coral hover:bg-coral/15' },
  { rating: 2 as const, label: '困难', symbol: '△', class: 'border-amber/30 bg-amber/10 text-amber hover:bg-amber/15' },
  { rating: 3 as const, label: '良好', symbol: '✓', class: 'border-cyan/30 bg-cyan/10 text-cyan hover:bg-cyan/15' },
  { rating: 4 as const, label: '轻松', symbol: '↑', class: 'border-lime/30 bg-lime/10 text-lime hover:bg-lime/15' }
]

const emptyStateTitle = computed(() => {
  if (selectedContentType.value === 'interview_card') {
    return '当前没有来自面试错题的复习项'
  }
  if (selectedContentType.value === 'knowledge_card') {
    return '当前没有到期的知识卡片'
  }
  if (selectedContentType.value === 'wrong_card') {
    return '当前没有到期的错题卡片'
  }
  return '今日复习已完成'
})

const emptyStateDescription = computed(() => {
  if (selectedContentType.value === 'interview_card') {
    return '完成面试诊断后，相关复习会出现在这里。'
  }
  return '去知识库上传资料生成卡片，或去今日卡片开始学习。'
})

const contentTypeLabel = (contentType: string) => {
  const map: Record<string, string> = {
    knowledge_card: '知识卡片',
    wrong_card: '错题卡片',
    interview_card: '面试卡片'
  }
  return map[contentType] || '复习项'
}

const contentChipClass = (contentType: string) => {
  if (contentType === 'knowledge_card') return '!bg-cyan-100 !text-cyan-700'
  if (contentType === 'interview_card') return '!bg-coral/10 !text-coral'
  return '!bg-amber-100 !text-amber-700'
}

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
  return '!bg-white/80 dark:!bg-slate-700/80 !text-slate-600 dark:!text-slate-300'
}

const repairCardClass = (item: UnifiedReviewItem) => {
  if (item.overdueDays > 0) return 'repair-card-due'
  if (item.contentType === 'knowledge_card') return 'repair-card-active'
  if (item.contentType === 'interview_card') return 'repair-card-due'
  return 'repair-card-new'
}

const reviewItemSummary = (item: UnifiedReviewItem) => {
  const dueLabel = item.overdueDays > 0 ? `逾期 ${item.overdueDays} 天` : '今天处理'
  if (item.deckTitle) {
    return `${dueLabel} · 来自 ${item.deckTitle}`
  }
  return dueLabel
}

const contentCount = (contentType: ReviewContentType) => {
  if (contentType === 'all') {
    return reviewData.value?.totalPending ?? 0
  }
  return reviewData.value?.countsByContentType?.[contentType] ?? 0
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
    ElMessage.error('复习数据加载失败')
  } finally {
    loading.value = false
  }
}

const changeContentType = async (contentType: ReviewContentType) => {
  if (selectedContentType.value === contentType) return
  selectedContentType.value = contentType
  resetSession()
  await loadData()
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
    ElMessage.error('提交评分失败')
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
  letter-spacing: -0.04em;
  line-height: 1.1;
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
  background: rgba(255, 255, 255, 0.42);
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
  background: rgba(255, 255, 255, 0.05);
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
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.1), transparent 32%),
    rgba(255, 255, 255, 0.28);
  padding: 22px;
}

.dark .review-launch {
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.12), transparent 32%),
    rgba(255, 255, 255, 0.03);
}

.review-launch__head,
.queue-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
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
  margin-top: 18px;
}

.review-launch__metric {
  border-radius: 20px;
  background: rgba(var(--bc-accent-rgb), 0.06);
  padding: 14px;
}

.review-launch__metric span,
.review-launch__metric small {
  display: block;
  color: var(--bc-ink-secondary);
  font-size: 12px;
}

.review-launch__metric strong {
  display: block;
  margin-top: 8px;
  color: var(--bc-ink);
  font-size: 1.85rem;
  font-weight: 780;
  letter-spacing: -0.05em;
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
    linear-gradient(145deg, rgba(255, 255, 255, 0.08), transparent 34%), var(--bc-panel);
  box-shadow:
    var(--bc-shadow),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
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
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.08), transparent 36%),
    rgba(255, 255, 255, 0.42);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.12);
}

.dark .repair-card__answer {
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.12), transparent 42%),
    rgba(255, 255, 255, 0.04);
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
    padding: 18px;
    border-radius: 20px;
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
