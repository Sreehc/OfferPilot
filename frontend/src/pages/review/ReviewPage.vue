<template>
  <div class="repair-workbench space-y-6">
    <section class="cockpit-panel p-4 sm:p-5">
      <div class="module-topbar">
        <div class="module-topbar__title">
          <span class="state-pulse" aria-hidden="true"></span>
          <h2 class="module-topbar__heading">错题复习</h2>
        </div>

        <div class="module-topbar__center">
          <div class="repair-workbench__tabs">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              type="button"
              class="repair-tab"
              :class="{ 'repair-tab-active': activeTab === tab.key }"
              @click="switchTab(tab.key)"
            >
              {{ tab.label }}
            </button>
          </div>
        </div>

        <div class="module-topbar__action">
          <button
            v-if="activeTab === 'review'"
            type="button"
            class="hard-button-primary"
            :disabled="!reviewItems.length"
            @click="startReview"
          >
            开始复习
          </button>
          <button v-else type="button" class="hard-button-primary" @click="switchTab('review')">去今日复习</button>
        </div>
      </div>
    </section>

    <section v-if="loading" class="cockpit-panel p-8 text-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      <p class="mt-4 text-sm text-slate-500">加载错题与复习数据...</p>
    </section>

    <template v-else>
      <section v-if="activeTab === 'review'" class="space-y-6">
        <section v-if="!reviewItems.length && !started" class="cockpit-panel p-8">
          <EmptyState icon="review" title="今日无待复习题目" description="可以切到全部错题查看仍需巩固的题目。">
            <template #action>
              <div class="flex justify-center gap-3">
                <button type="button" class="hard-button-secondary" @click="switchTab('all')">查看全部错题</button>
                <RouterLink to="/interview" class="hard-button-primary">开始面试</RouterLink>
              </div>
            </template>
          </EmptyState>
        </section>

        <section v-else-if="!started" class="cockpit-panel p-5 sm:p-6">
          <div class="review-launch">
            <div class="review-launch__head">
              <div>
                <p class="section-kicker">今日复习</p>
                <h3 class="mt-2 text-3xl font-semibold tracking-[-0.04em] text-ink">
                  {{ reviewItems.length }} 道题等待处理
                </h3>
                <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
                  先回忆，再看答案，然后按掌握程度评分。
                </p>
              </div>
              <div class="review-launch__signals">
                <span class="detail-pill">逾期 {{ overdueCount }} 道</span>
                <span class="detail-pill">累计复习 {{ stats?.totalReviews ?? 0 }} 次</span>
              </div>
            </div>

            <div class="mt-6 flex flex-wrap gap-3">
              <button type="button" class="hard-button-primary" @click="startReview">开始复习</button>
              <button type="button" class="hard-button-secondary" @click="switchTab('all')">查看全部错题</button>
            </div>
          </div>
        </section>

        <section v-else-if="currentReviewItem" class="mx-auto max-w-3xl px-2 sm:px-0">
          <div class="mb-4 flex items-center justify-between text-sm text-slate-500 dark:text-slate-400">
            <span class="font-mono">{{ currentIndex + 1 }} / {{ reviewItems.length }}</span>
            <span
              v-if="currentReviewItem.overdueDays > 0"
              class="rounded-full border border-coral/30 bg-coral/10 px-3 py-1 text-coral"
            >
              逾期 {{ currentReviewItem.overdueDays }} 天
            </span>
          </div>

          <div class="mb-6 h-1.5 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-white/10">
            <div
              class="h-full rounded-full bg-accent transition-all duration-300"
              :style="{ width: `${((currentIndex + 1) / reviewItems.length) * 100}%` }"
            ></div>
          </div>

          <p class="mb-3 text-center text-xs text-slate-400 dark:text-slate-500 sm:hidden">
            左滑重来 · 右滑良好 · 点击翻转
          </p>

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
              <div class="flashcard-front memory-card p-5 sm:p-8">
                <div class="flex items-center justify-between gap-4">
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                    先回忆问题
                  </div>
                  <span class="hard-chip">{{ masteryLabel(currentReviewItem.masteryLevel) }}</span>
                </div>
                <h3 class="mt-8 text-xl font-semibold leading-relaxed text-ink sm:text-2xl">
                  {{ currentReviewItem.title }}
                </h3>
                <p class="mt-8 text-sm text-slate-400 dark:text-slate-500">点击翻转查看标准答案</p>
              </div>

              <div class="flashcard-back memory-card p-5 sm:p-8">
                <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                  标准答案
                </div>
                <p class="mt-4 whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-slate-200">
                  {{ currentReviewItem.standardAnswer || '暂无标准答案' }}
                </p>
                <div
                  v-if="currentReviewItem.errorReason"
                  class="mt-4 border-t border-slate-200/60 pt-4 dark:border-slate-700/60"
                >
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                    之前错误原因
                  </div>
                  <p class="mt-1 text-sm text-red-500 dark:text-red-400">{{ currentReviewItem.errorReason }}</p>
                </div>
                <div class="mt-4 border-t border-[var(--bc-line)] pt-4 text-xs text-slate-500 dark:text-slate-400">
                  当前记忆系数：{{ (currentReviewItem.easeFactor ?? 2.5).toFixed(2) }} · 当前间隔：
                  {{ currentReviewItem.intervalDays ?? 1 }} 天
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
              :class="btn.class"
              :disabled="submitting"
              @click="handleRate(btn.rating)"
            >
              <component :is="btn.icon" class="h-5 w-5" />
              <span>{{ btn.label }}</span>
            </button>
          </div>
        </section>

        <section v-else class="cockpit-panel p-8">
          <EmptyState
            icon="trophy"
            title="今日复习完成"
            :description="`共复习 ${reviewItems.length} 道题，其中 ${againCount} 道需要再次复习。`"
          >
            <template #action>
              <div class="flex justify-center gap-3">
                <button type="button" class="hard-button-secondary" @click="switchTab('all')">查看全部错题</button>
                <RouterLink to="/dashboard" class="hard-button-primary">返回首页</RouterLink>
              </div>
            </template>
          </EmptyState>
        </section>
      </section>

      <section v-else class="space-y-6">
        <section class="cockpit-panel p-4 sm:p-5">
          <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p class="section-kicker">全部错题</p>
              <h3 class="mt-3 text-3xl font-semibold tracking-[-0.04em] text-ink">错题池</h3>
            </div>

            <div class="repair-filter-row">
              <button
                v-for="filter in wrongFilters"
                :key="filter.value"
                type="button"
                class="repair-filter-chip"
                :class="{ 'repair-filter-chip-active': wrongStatusFilter === filter.value }"
                @click="wrongStatusFilter = filter.value"
              >
                {{ filter.label }}
              </button>
            </div>
          </div>
        </section>

        <section v-if="!prioritizedWrongItems.length" class="cockpit-panel p-6">
          <EmptyState icon="review" title="错题池为空" description="完成练习后，需要继续复习的题目会显示在这里。">
            <template #action>
              <RouterLink to="/interview" class="hard-button-primary inline-flex">开始面试</RouterLink>
            </template>
          </EmptyState>
        </section>

        <section v-else>
          <div class="space-y-3">
            <article
              v-for="item in prioritizedWrongItems"
              :key="item.id"
              class="repair-card cursor-pointer p-4 sm:p-5"
              :class="[repairCardClass(item), expandedId === item.id ? 'is-expanded' : '']"
              @click="toggleExpand(item.id)"
            >
              <div class="flex items-start justify-between gap-4">
                <div class="min-w-0 flex-1">
                  <h4 class="repair-card__title text-lg font-semibold leading-snug text-ink">{{ item.title }}</h4>
                  <p class="repair-card__summary mt-2 text-sm text-slate-500 dark:text-slate-400">
                    {{ wrongItemSummary(item) }}
                  </p>
                </div>
                <span class="hard-chip shrink-0" :class="masteryChipClass(item.masteryLevel)">
                  {{ masteryLabel(item.masteryLevel) }}
                </span>
              </div>

              <div class="repair-card__answer mt-4">
                <div class="repair-card__answer-label">参考答案</div>
                <p class="mt-2 text-sm leading-7 text-slate-700 dark:text-slate-200">
                  {{ item.standardAnswer || '暂无参考答案。' }}
                </p>
              </div>

              <div v-if="expandedId === item.id" class="mt-4 space-y-4 border-t border-[var(--bc-line)] pt-4">
                <div class="repair-card__reason">
                  <div class="repair-card__reason-label">本题卡点</div>
                  <p class="mt-2 text-sm leading-7 text-slate-700 dark:text-slate-200">
                    {{ item.errorReason || '还没有记录具体错误原因。' }}
                  </p>
                </div>

                <div class="flex flex-wrap gap-3">
                  <button
                    v-if="isWrongItemDue(item)"
                    type="button"
                    class="hard-button-primary !min-h-10 !px-4 text-xs"
                    @click.stop="jumpToReviewItem(item)"
                  >
                    去复习这题
                  </button>
                </div>
              </div>

              <div v-else class="mt-3 text-xs tracking-[0.16em] text-slate-400 dark:text-slate-500">点击查看详情</div>
            </article>
          </div>

          <div v-if="wrongTotalPages > 1" class="mt-6 flex justify-center">
            <el-pagination
              v-model:current-page="wrongCurrentPage"
              :page-size="wrongPageSize"
              :total="wrongTotal"
              layout="prev, pager, next"
              @current-change="handleWrongPageChange"
            />
          </div>
        </section>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, h, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'
import { fetchReviewStatsApi, fetchReviewTodayApi, submitReviewRateApi } from '@/api/review'
import { fetchWrongListApi } from '@/api/wrong'
import type { ReviewStats, ReviewTodayItem, WrongQuestionItem } from '@/types/api'

type WorkbenchTab = 'review' | 'all'
type WrongFilter = 'all' | 'due_today' | 'not_started' | 'reviewing' | 'mastered'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const reviewItems = ref<ReviewTodayItem[]>([])
const wrongItems = ref<WrongQuestionItem[]>([])
const stats = ref<ReviewStats | null>(null)
const started = ref(false)
const currentIndex = ref(0)
const showAnswer = ref(false)
const submitting = ref(false)
const againCount = ref(0)
const flashcardRef = ref<HTMLElement | null>(null)
const activeTab = ref<WorkbenchTab>('review')
const wrongStatusFilter = ref<WrongFilter>('all')
const expandedId = ref<number | null>(null)
const wrongCurrentPage = ref(1)
const wrongPageSize = ref(20)
const wrongTotal = ref(0)
const wrongTotalPages = ref(0)

let touchStartX = 0
let touchStartY = 0
let touchStartTime = 0

const tabs = [
  { key: 'review' as const, label: '今日复习' },
  { key: 'all' as const, label: '全部错题' }
]

const wrongFilters = [
  { value: 'all' as const, label: '全部' },
  { value: 'due_today' as const, label: '今日待复习' },
  { value: 'not_started' as const, label: '未开始' },
  { value: 'reviewing' as const, label: '复习中' },
  { value: 'mastered' as const, label: '已掌握' }
]

const todayCount = computed(() => reviewItems.value.length)
const overdueCount = computed(() => reviewItems.value.filter((item) => item.overdueDays > 0).length)
const wrongTotalDisplay = computed(() => wrongTotal.value || wrongItems.value.length)
const currentReviewItem = computed(() => reviewItems.value[currentIndex.value] ?? null)

const filteredWrongItems = computed(() => {
  let result = wrongItems.value

  if (wrongStatusFilter.value === 'due_today') {
    result = result.filter((item) => isWrongItemDue(item))
  } else if (wrongStatusFilter.value !== 'all') {
    result = result.filter((item) => item.masteryLevel === wrongStatusFilter.value)
  }

  return result
})

const prioritizedWrongItems = computed(() =>
  [...filteredWrongItems.value].sort((a, b) => {
    const priorityDiff = wrongPriority(a) - wrongPriority(b)
    if (priorityDiff !== 0) return priorityDiff
    return (b.reviewCount ?? 0) - (a.reviewCount ?? 0)
  })
)

const IconRepeat = () =>
  h('svg', { fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      d: 'M4 4v6h6M20 20v-6h-6M5.6 16.5A7.5 7.5 0 0018.4 7.5M18.4 7.5H14M18.4 7.5V3'
    })
  ])

const IconFriction = () =>
  h('svg', { fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M12 3l8 15H4L12 3z' }),
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M12 9v4m0 4h.01' })
  ])

const IconCheck = () =>
  h('svg', { fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M5 13l4 4L19 7' })
  ])

const IconLift = () =>
  h('svg', { fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M12 19V5m0 0l-6 6m6-6l6 6' }),
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M5 21h14' })
  ])

const ratingButtons = computed(() => [
  {
    rating: 1 as const,
    icon: IconRepeat,
    label: '重来',
    class: 'border-coral/30 bg-coral/10 text-coral hover:bg-coral/15'
  },
  {
    rating: 2 as const,
    icon: IconFriction,
    label: '困难',
    class: 'border-amber/30 bg-amber/10 text-amber hover:bg-amber/15'
  },
  {
    rating: 3 as const,
    icon: IconCheck,
    label: '良好',
    class: 'border-cyan/30 bg-cyan/10 text-cyan hover:bg-cyan/15'
  },
  {
    rating: 4 as const,
    icon: IconLift,
    label: '轻松',
    class: 'border-lime/30 bg-lime/10 text-lime hover:bg-lime/15'
  }
])

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

const todayIso = () => new Date().toISOString().slice(0, 10)

const isWrongItemDue = (item: WrongQuestionItem) => Boolean(item.nextReviewDate && item.nextReviewDate <= todayIso())

const wrongPriority = (item: WrongQuestionItem) => {
  if (isWrongItemDue(item)) return 0
  if (item.masteryLevel === 'not_started') return 1
  if (item.masteryLevel === 'reviewing') return 2
  return 3
}

const wrongItemSummary = (item: WrongQuestionItem) => {
  if (isWrongItemDue(item)) return `今天复习 · 已复习 ${item.reviewCount ?? 0} 次`
  if (item.nextReviewDate) return `下次 ${formatDate(item.nextReviewDate)} · 已复习 ${item.reviewCount ?? 0} 次`
  return `待安排复习 · 已复习 ${item.reviewCount ?? 0} 次`
}

const repairCardClass = (item: WrongQuestionItem) => {
  if (isWrongItemDue(item)) return 'repair-card-due'
  if (item.masteryLevel === 'not_started') return 'repair-card-new'
  if (item.masteryLevel === 'reviewing') return 'repair-card-active'
  return 'repair-card-mastered'
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const month = d.getMonth() + 1
  const day = d.getDate()
  return `${month}/${day}`
}

const syncTabFromRoute = () => {
  const queryTab = route.query.tab
  activeTab.value = queryTab === 'all' ? 'all' : 'review'
}

const switchTab = (tab: WorkbenchTab) => {
  activeTab.value = tab
  router.replace({ path: '/review', query: tab === 'all' ? { tab: 'all' } : {} })
}

const loadReviewData = async () => {
  const [itemsRes, statsRes] = await Promise.all([fetchReviewTodayApi(), fetchReviewStatsApi()])
  reviewItems.value = itemsRes.data
  stats.value = statsRes.data
}

const loadWrongData = async () => {
  const response = await fetchWrongListApi(wrongCurrentPage.value, wrongPageSize.value)
  wrongItems.value = response.data.records
  wrongTotal.value = response.data.total
  wrongTotalPages.value = response.data.totalPages
}

const loadData = async () => {
  loading.value = true
  try {
    await Promise.all([loadReviewData(), loadWrongData()])
  } catch {
    ElMessage.error('错题复习数据加载失败')
  } finally {
    loading.value = false
  }
}

const handleWrongPageChange = (page: number) => {
  wrongCurrentPage.value = page
  void loadWrongData()
}

const toggleExpand = (id: number) => {
  expandedId.value = expandedId.value === id ? null : id
}

const startReview = () => {
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
    await submitReviewRateApi(currentReviewItem.value.wrongQuestionId, { rating })
    showAnswer.value = false
    currentIndex.value++
    await Promise.all([loadReviewData(), loadWrongData()])
  } catch {
    ElMessage.error('提交评分失败')
  } finally {
    submitting.value = false
  }
}

const jumpToReviewItem = (item: WrongQuestionItem) => {
  const targetIndex = reviewItems.value.findIndex((review) => review.wrongQuestionId === item.id)
  switchTab('review')
  if (targetIndex >= 0) {
    started.value = true
    currentIndex.value = targetIndex
    showAnswer.value = false
  }
}

watch(
  () => route.query.tab,
  () => {
    syncTabFromRoute()
  }
)

onMounted(() => {
  syncTabFromRoute()
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

.repair-workbench__tabs,
.repair-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.repair-tab,
.repair-filter-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
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

.dark .repair-tab,
.dark .repair-filter-chip {
  background: rgba(255, 255, 255, 0.05);
}

.repair-tab-active,
.repair-filter-chip-active {
  border-color: rgba(var(--bc-accent-rgb), 0.28);
  color: var(--bc-ink);
  background: rgba(var(--bc-accent-rgb), 0.1);
}

.review-launch {
  border-radius: 24px;
  border: 1px solid var(--bc-line);
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.1), transparent 32%),
    rgba(255, 255, 255, 0.24);
  padding: 24px;
}

.dark .review-launch {
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.12), transparent 32%),
    rgba(255, 255, 255, 0.03);
}

.review-launch__head {
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
  border: 1px solid var(--bc-line);
  border-left-width: 3px;
  border-radius: 22px;
  background: var(--bc-surface-card);
  box-shadow: var(--bc-shadow-soft);
  transition:
    border-color var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard),
    transform var(--motion-base) var(--ease-hard);
}

.repair-card:hover,
.repair-card.is-expanded {
  box-shadow: var(--bc-shadow-hover);
  transform: translateY(-2px);
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

.repair-card-mastered {
  border-left-color: var(--bc-lime);
}

.repair-card__title {
  text-wrap: balance;
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
}

@media (max-width: 640px) {
  .module-topbar__heading {
    font-size: 24px;
  }

  .review-launch {
    padding: 18px;
    border-radius: 20px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .flashcard {
    transition-duration: 0.01ms;
  }
}
</style>
