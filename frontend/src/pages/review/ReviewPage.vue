<template>
  <div class="space-y-6">
    <!-- Header -->
    <section class="paper-panel p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="section-kicker">间隔复习</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
            {{ loading ? '加载中...' : `${todayCount} 道题待复习` }}
          </h3>
          <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
            基于遗忘曲线自动调度，复习间隔随掌握程度动态调整。
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
    <section v-if="loading" class="paper-panel p-8 text-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      <p class="mt-4 text-sm text-slate-500">加载复习题目...</p>
    </section>

    <!-- Empty State: No reviews due -->
    <section v-else-if="!items.length && !started" class="paper-panel p-8">
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
    <section v-else-if="!started" class="paper-panel p-8">
      <EmptyState
        icon="review"
        :title="`${todayCount} 道题等待复习`"
        :description="`预计 ${estimatedMinutes} 分钟完成。每道题翻转后回忆答案，再根据掌握程度评分。`"
      >
        <template #action>
          <button type="button" class="hard-button-primary" @click="startReview">
            开始复习
          </button>
        </template>
      </EmptyState>
    </section>

    <!-- Flashcard Review -->
    <section v-else-if="currentItem" class="mx-auto max-w-2xl px-2 sm:px-0">
      <!-- Progress -->
      <div class="mb-4 flex items-center justify-between text-sm text-slate-500 dark:text-slate-400">
        <span>{{ currentIndex + 1 }} / {{ items.length }}</span>
        <span v-if="currentItem.overdueDays > 0" class="text-amber-500">
          逾期 {{ currentItem.overdueDays }} 天
        </span>
      </div>
      <div class="mb-6 h-1 w-full rounded-full bg-slate-200 dark:bg-slate-700">
        <div
          class="h-1 rounded-full bg-accent transition-all duration-300"
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
          <div class="flashcard-front paper-panel p-5 sm:p-8">
            <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
              题目
            </div>
            <h3 class="mt-4 text-lg sm:text-xl font-semibold leading-relaxed text-ink">
              {{ currentItem.title }}
            </h3>
            <p class="mt-6 text-sm text-slate-400 dark:text-slate-500">
              点击翻转查看标准答案
            </p>
          </div>

          <!-- Back: Answer -->
          <div class="flashcard-back paper-panel p-5 sm:p-8">
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
          </div>
        </div>
      </div>

      <!-- Rating Buttons -->
      <div v-if="showAnswer" class="mt-6 grid grid-cols-2 gap-2 sm:grid-cols-4 sm:gap-3">
        <button
          v-for="btn in ratingButtons"
          :key="btn.rating"
          type="button"
          class="flex flex-col items-center gap-1 rounded-xl p-3 text-sm font-medium transition-all"
          :class="btn.class"
          :disabled="submitting"
          @click="handleRate(btn.rating)"
        >
          <span class="text-lg">{{ btn.emoji }}</span>
          <span>{{ btn.label }}</span>
          <span class="text-xs opacity-70">{{ btn.interval }}</span>
        </button>
      </div>
    </section>

    <!-- Completion Summary -->
    <section v-else class="paper-panel p-8">
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
import { computed, onMounted, ref } from 'vue'
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
const estimatedMinutes = computed(() => Math.max(1, Math.ceil(items.value.length * 0.5)))
const currentItem = computed(() => items.value[currentIndex.value] ?? null)

const ratingButtons = [
  { rating: 1 as const, emoji: '🔄', label: '重来', interval: '1 天', class: 'bg-red-50 text-red-600 hover:bg-red-100 dark:bg-red-900/20 dark:text-red-400 dark:hover:bg-red-900/30' },
  { rating: 2 as const, emoji: '😓', label: '困难', interval: '1 天', class: 'bg-amber-50 text-amber-600 hover:bg-amber-100 dark:bg-amber-900/20 dark:text-amber-400 dark:hover:bg-amber-900/30' },
  { rating: 3 as const, emoji: '👍', label: '良好', interval: '按算法', class: 'bg-blue-50 text-blue-600 hover:bg-blue-100 dark:bg-blue-900/20 dark:text-blue-400 dark:hover:bg-blue-900/30' },
  { rating: 4 as const, emoji: '🌟', label: '轻松', interval: '按算法', class: 'bg-green-50 text-green-600 hover:bg-green-100 dark:bg-green-900/20 dark:text-green-400 dark:hover:bg-green-900/30' }
]

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
.flashcard-wrapper {
  perspective: 1000px;
}

.flashcard {
  position: relative;
  width: 100%;
  min-height: 240px;
  transition: transform 0.5s;
  transform-style: preserve-3d;
}

@media (min-width: 640px) {
  .flashcard {
    min-height: 280px;
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
}

.flashcard-back {
  transform: rotateY(180deg);
}
</style>
