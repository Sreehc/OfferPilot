<template>
  <div class="review-room space-y-6">
    <!-- Header -->
    <section class="cockpit-panel p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div class="flex items-center gap-3">
            <span class="state-pulse" aria-hidden="true"></span>
            <p class="section-kicker">今日复习</p>
          </div>
          <h3 class="mt-4 font-display text-4xl font-semibold leading-none tracking-[-0.04em] text-ink">
            {{ loading ? '加载中...' : `${todayCount} 道题待复习` }}
          </h3>
          <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
            先回忆，再翻看答案，再给自己评分。系统会据此安排下一次复习时间。
          </p>
        </div>
        <div class="flex items-center gap-4">
          <div v-if="stats" class="flex items-center gap-6 text-sm">
            <div class="text-center">
              <div class="text-2xl font-semibold text-ink">{{ stats.currentStreak }}</div>
              <div class="text-xs text-slate-500 dark:text-slate-400">连续打卡</div>
            </div>
            <div class="text-center">
              <div class="text-2xl font-semibold text-ink">{{ stats.totalReviews }}</div>
              <div class="text-xs text-slate-500 dark:text-slate-400">累计复习</div>
            </div>
          </div>
          <el-button size="large" class="hard-button-secondary" @click="loadData">刷新</el-button>
        </div>
      </div>
    </section>

    <!-- Loading -->
    <section v-if="loading" class="cockpit-panel p-8 text-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      <p class="mt-4 text-sm text-slate-500">加载复习题目...</p>
    </section>

    <!-- Empty State: No reviews due -->
    <section v-else-if="!items.length && !started" class="cockpit-panel p-8">
      <EmptyState
        icon="review"
        title="今日无待复习题目"
        description="完成面试后错题会自动加入复习队列，也可以前往错题本查看所有题目。"
      >
        <template #action>
          <div class="flex justify-center gap-3">
            <RouterLink to="/wrong" class="hard-button-secondary">查看错题本</RouterLink>
            <RouterLink to="/interview" class="hard-button-primary">开始面试</RouterLink>
          </div>
        </template>
      </EmptyState>
    </section>

    <!-- Start Screen -->
    <section v-else-if="!started" class="cockpit-panel overflow-hidden p-8">
      <div class="mx-auto max-w-2xl text-center">
        <div class="review-orbit mx-auto flex h-44 w-44 items-center justify-center rounded-full">
          <span class="font-mono text-5xl font-semibold text-ink">{{ todayCount }}</span>
        </div>
        <p class="section-kicker mt-8">开始前</p>
        <h3 class="mt-3 font-display text-4xl font-semibold leading-none text-ink">{{ todayCount }} 道题等待你完成复习</h3>
        <p class="mt-4 text-sm leading-7 text-slate-600 dark:text-slate-300">
          先回忆，再翻卡。根据真实掌握程度评分，系统会计算下一次复习日期。
        </p>
        <div class="mt-6">
          <button type="button" class="hard-button-primary" @click="startReview">
            开始复习
          </button>
        </div>
      </div>
    </section>

    <!-- Flashcard Review -->
    <section v-else-if="currentItem" class="mx-auto max-w-3xl px-2 sm:px-0">
      <!-- Progress -->
      <div class="mb-4 flex items-center justify-between text-sm text-slate-500 dark:text-slate-400">
        <span class="font-mono">{{ currentIndex + 1 }} / {{ items.length }}</span>
        <span v-if="currentItem.overdueDays > 0" class="rounded-full border border-coral/30 bg-coral/10 px-3 py-1 text-coral">
          逾期 {{ currentItem.overdueDays }} 天
        </span>
      </div>
      <div class="mb-6 h-1.5 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-white/10">
        <div
          class="h-full rounded-full bg-accent transition-all duration-300"
          :style="{ width: `${((currentIndex + 1) / items.length) * 100}%` }"
        ></div>
      </div>

      <!-- Swipe hint on mobile -->
      <p class="mb-3 text-center text-xs text-slate-400 dark:text-slate-500 sm:hidden">
        左滑重来 · 右滑良好 · 点击翻转
      </p>

      <!-- Flashcard -->
      <div
        ref="flashcardRef"
        class="flashcard-wrapper cursor-pointer"
        :class="{ flipped: showAnswer }"
        @click="flipCard"
        @touchstart="onTouchStart"
        @touchmove.passive="onTouchMove"
        @touchend="onTouchEnd"
      >
        <div class="flashcard">
          <!-- Front: Question -->
          <div class="flashcard-front memory-card p-5 sm:p-8">
            <div class="flex items-center justify-between gap-4">
              <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                先回忆问题
              </div>
              <span class="hard-chip">{{ masteryLabel(currentItem.masteryLevel) }}</span>
            </div>
            <h3 class="mt-8 text-xl font-semibold leading-relaxed text-ink sm:text-2xl">
              {{ currentItem.title }}
            </h3>
            <p class="mt-8 text-sm text-slate-400 dark:text-slate-500">
              点击翻转查看标准答案
            </p>
          </div>

          <!-- Back: Answer -->
          <div class="flashcard-back memory-card p-5 sm:p-8">
            <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
              标准答案
            </div>
            <p class="mt-4 text-sm leading-7 text-slate-700 dark:text-slate-200 whitespace-pre-wrap">
              {{ currentItem.standardAnswer || '暂无标准答案' }}
            </p>
            <div v-if="currentItem.errorReason" class="mt-4 border-t border-slate-200/60 dark:border-slate-700/60 pt-4">
              <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                之前错误原因
              </div>
              <p class="mt-1 text-sm text-red-500 dark:text-red-400">{{ currentItem.errorReason }}</p>
            </div>
            <div class="mt-4 border-t border-[var(--bc-line)] pt-4 text-xs text-slate-500 dark:text-slate-400">
              当前 EF：{{ (currentItem.easeFactor ?? 2.5).toFixed(2) }} · 当前间隔：{{ currentItem.intervalDays ?? 1 }} 天
            </div>
          </div>
        </div>
      </div>

      <!-- Rating Buttons -->
      <div v-if="showAnswer" class="mt-6 grid grid-cols-2 gap-2 sm:grid-cols-4 sm:gap-3">
        <button
          v-for="btn in ratingButtons"
          :key="btn.rating"
          type="button"
          class="rating-button flex flex-col items-center gap-1 p-3 text-sm font-semibold transition-all"
          :class="btn.class"
          :disabled="submitting"
          @click="handleRate(btn.rating)"
        >
          <component :is="btn.icon" class="h-5 w-5" />
          <span>{{ btn.label }}</span>
          <span class="text-xs opacity-70">{{ btn.interval }}</span>
        </button>
      </div>
    </section>

    <!-- Completion Summary -->
    <section v-else class="cockpit-panel p-8">
      <EmptyState
        icon="trophy"
        title="今日复习完成"
        :description="`共复习 ${items.length} 道题，其中 ${againCount} 道需要再次复习。`"
      >
        <template #action>
          <div class="flex justify-center gap-3">
            <RouterLink to="/wrong" class="hard-button-secondary">查看错题本</RouterLink>
            <RouterLink to="/dashboard" class="hard-button-primary">返回首页</RouterLink>
          </div>
        </template>
      </EmptyState>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, h, onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { fetchReviewStatsApi, fetchReviewTodayApi, submitReviewRateApi } from '@/api/review'
import type { ReviewStats, ReviewTodayItem } from '@/types/api'

const loading = ref(true)
const items = ref<ReviewTodayItem[]>([])
const stats = ref<ReviewStats | null>(null)
const started = ref(false)
const currentIndex = ref(0)
const showAnswer = ref(false)
const submitting = ref(false)
const againCount = ref(0)
const flashcardRef = ref<HTMLElement | null>(null)

// Touch swipe state
let touchStartX = 0
let touchStartY = 0
let touchStartTime = 0

const onTouchStart = (e: TouchEvent) => {
  const touch = e.touches.item(0)
  if (!touch) return
  touchStartX = touch.clientX
  touchStartY = touch.clientY
  touchStartTime = Date.now()
}

const onTouchMove = (e: TouchEvent) => {
  // Prevent default to avoid scrolling while swiping on the card
  const touch = e.touches.item(0)
  if (!touch) return
  const dx = Math.abs(touch.clientX - touchStartX)
  const dy = Math.abs(touch.clientY - touchStartY)
  if (dx > dy && dx > 10) {
    e.preventDefault()
  }
}

const onTouchEnd = (e: TouchEvent) => {
  const touch = e.changedTouches.item(0)
  if (!touch) return
  const touchEndX = touch.clientX
  const touchEndY = touch.clientY
  const dx = touchEndX - touchStartX
  const dy = touchEndY - touchStartY
  const dt = Date.now() - touchStartTime

  // Must be a horizontal swipe (not vertical scroll)
  if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 50 && dt < 500) {
    if (showAnswer.value) {
      if (dx < 0) {
        // Swipe left: rate "Again" (hard)
        void handleRate(1)
      } else {
        // Swipe right: rate "Good"
        void handleRate(3)
      }
    }
  }
}

const todayCount = computed(() => items.value.length)
const currentItem = computed(() => items.value[currentIndex.value] ?? null)

const IconRepeat = () => h('svg', { fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M4 4v6h6M20 20v-6h-6M5.6 16.5A7.5 7.5 0 0018.4 7.5M18.4 7.5H14M18.4 7.5V3' })
])

const IconFriction = () => h('svg', { fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M12 3l8 15H4L12 3z' }),
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M12 9v4m0 4h.01' })
])

const IconCheck = () => h('svg', { fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M5 13l4 4L19 7' })
])

const IconLift = () => h('svg', { fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M12 19V5m0 0l-6 6m6-6l6 6' }),
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M5 21h14' })
])

const ratingButtons = computed(() => {
  const item = currentItem.value
  const ef = item?.easeFactor ?? 2.5
  const interval = item?.intervalDays ?? 1
  const goodDays = Math.max(1, Math.round(interval * ef))
  const easyDays = Math.max(1, Math.round(interval * ef * 1.3))
  return [
    { rating: 1 as const, icon: IconRepeat, label: '重来', interval: '1 天', class: 'border-coral/30 bg-coral/10 text-coral hover:bg-coral/15' },
    { rating: 2 as const, icon: IconFriction, label: '困难', interval: '1 天', class: 'border-amber/30 bg-amber/10 text-amber hover:bg-amber/15' },
    { rating: 3 as const, icon: IconCheck, label: '良好', interval: `${goodDays} 天`, class: 'border-cyan/30 bg-cyan/10 text-cyan hover:bg-cyan/15' },
    { rating: 4 as const, icon: IconLift, label: '轻松', interval: `${easyDays} 天`, class: 'border-lime/30 bg-lime/10 text-lime hover:bg-lime/15' }
  ]
})

const masteryLabel = (level: string) => {
  const map: Record<string, string> = {
    not_started: '未开始',
    reviewing: '复习中',
    mastered: '已掌握'
  }
  return map[level] || level
}

const loadData = async () => {
  loading.value = true
  try {
    const [itemsRes, statsRes] = await Promise.all([
      fetchReviewTodayApi(),
      fetchReviewStatsApi()
    ])
    items.value = itemsRes.data
    stats.value = statsRes.data
  } catch {
    ElMessage.error('复习数据加载失败')
  } finally {
    loading.value = false
  }
}

const startReview = () => {
  started.value = true
  currentIndex.value = 0
  showAnswer.value = false
  againCount.value = 0
}

const flipCard = () => {
  if (!showAnswer.value) {
    showAnswer.value = true
  }
}

const handleRate = async (rating: 1 | 2 | 3 | 4) => {
  if (submitting.value) return
  if (!currentItem.value) return
  submitting.value = true

  if (rating <= 2) againCount.value++

  try {
    await submitReviewRateApi(currentItem.value.wrongQuestionId, { rating })

    // Move to next card
    showAnswer.value = false
    currentIndex.value++
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
.review-orbit {
  position: relative;
  border: 1px solid var(--bc-line-hot);
  background:
    radial-gradient(circle, rgba(var(--bc-accent-rgb), 0.18), transparent 58%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.08), transparent);
  box-shadow:
    0 0 0 12px rgba(var(--bc-accent-rgb), 0.04),
    0 0 60px rgba(var(--bc-accent-rgb), 0.18);
}

.review-orbit::before {
  content: '';
  position: absolute;
  inset: 18px;
  border: 1px dashed rgba(var(--bc-accent-rgb), 0.34);
  border-radius: inherit;
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
  backface-visibility: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow-y: auto;
}

.flashcard-back {
  transform: rotateY(180deg);
}

.memory-card {
  border: 1px solid var(--bc-line);
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 80% 20%, rgba(85, 214, 190, 0.1), transparent 30%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.08), transparent 34%),
    var(--bc-panel);
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

@media (prefers-reduced-motion: reduce) {
  .flashcard {
    transition-duration: 0.01ms;
  }
}
</style>
