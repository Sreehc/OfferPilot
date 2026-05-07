<template>
  <div class="space-y-6">
    <section class="cockpit-panel p-4 sm:p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div class="flex items-center gap-3">
            <span class="state-pulse" aria-hidden="true"></span>
            <p class="section-kicker">错题本</p>
          </div>
          <h3 class="mt-4 font-display text-4xl font-semibold leading-none tracking-[-0.04em] text-ink sm:text-5xl">
            错题清单
          </h3>
        </div>

        <el-select v-model="statusFilter" placeholder="状态筛选" size="large" class="w-full lg:w-[180px]">
          <el-option label="全部" value="all" />
          <el-option label="今日待复习" value="due_today" />
          <el-option label="未开始" value="not_started" />
          <el-option label="复习中" value="reviewing" />
          <el-option label="已掌握" value="mastered" />
        </el-select>
      </div>
    </section>

    <!-- Loading -->
    <section v-if="loading" class="grid gap-4 grid-cols-1 sm:grid-cols-2 xl:grid-cols-3">
      <article v-for="n in 3" :key="n" class="metric-card">
        <div class="h-4 w-24 animate-pulse rounded bg-slate-200 dark:bg-slate-700"></div>
        <div class="mt-4 h-3 w-full animate-pulse rounded bg-slate-100 dark:bg-slate-800"></div>
        <div class="mt-2 h-3 w-3/4 animate-pulse rounded bg-slate-100 dark:bg-slate-800"></div>
      </article>
    </section>

    <!-- Empty State -->
    <section v-else-if="!prioritizedItems.length" class="cockpit-panel p-6">
      <EmptyState icon="review" title="错题本为空" description="完成练习后，需要继续复习的题目会显示在这里。">
        <template #action>
          <RouterLink to="/interview" class="hard-button-primary inline-flex"> 开始面试 </RouterLink>
        </template>
      </EmptyState>
    </section>

    <!-- Wrong Items Grid -->
    <section v-else>
      <div class="space-y-3">
        <article
          v-for="item in prioritizedItems"
          :key="item.id"
          class="repair-card cursor-pointer p-4 sm:p-5"
          :class="[repairCardClass(item), expandedId === item.id ? 'is-expanded' : '']"
          @click="toggleExpand(item.id)"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="min-w-0 flex-1">
              <h4 class="repair-card__title text-lg font-semibold leading-snug text-ink">{{ item.title }}</h4>
              <p class="repair-card__summary mt-2 text-sm text-slate-500 dark:text-slate-400">
                {{ itemSummary(item) }}
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
          </div>

          <div v-else class="mt-3 text-xs tracking-[0.16em] text-slate-400 dark:text-slate-500">点击查看详情</div>
        </article>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="mt-6 flex justify-center">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { fetchWrongListApi } from '@/api/wrong'
import type { WrongQuestionItem } from '@/types/api'

const items = ref<WrongQuestionItem[]>([])
const loading = ref(true)
const expandedId = ref<number | null>(null)
const statusFilter = ref<'all' | 'due_today' | 'not_started' | 'reviewing' | 'mastered'>('all')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)

const filteredItems = computed(() => {
  let result = items.value

  if (statusFilter.value === 'due_today') {
    result = result.filter((item) => isDue(item))
  } else if (statusFilter.value !== 'all') {
    result = result.filter((item) => item.masteryLevel === statusFilter.value)
  }

  return result
})

const todayIso = () => new Date().toISOString().slice(0, 10)

const isDue = (item: WrongQuestionItem) => Boolean(item.nextReviewDate && item.nextReviewDate <= todayIso())

const repairPriority = (item: WrongQuestionItem) => {
  if (isDue(item)) return 0
  if (item.masteryLevel === 'not_started') return 1
  if (item.masteryLevel === 'reviewing') return 2
  return 3
}

const prioritizedItems = computed(() =>
  [...filteredItems.value].sort((a, b) => {
    const priorityDiff = repairPriority(a) - repairPriority(b)
    if (priorityDiff !== 0) return priorityDiff
    return (b.reviewCount ?? 0) - (a.reviewCount ?? 0)
  })
)

const loadData = async () => {
  loading.value = true
  try {
    const response = await fetchWrongListApi(currentPage.value, pageSize.value)
    items.value = response.data.records
    total.value = response.data.total
    totalPages.value = response.data.totalPages
  } catch {
    ElMessage.error('错题列表加载失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  void loadData()
}

const toggleExpand = (id: number) => {
  expandedId.value = expandedId.value === id ? null : id
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

const itemSummary = (item: WrongQuestionItem) => {
  if (isDue(item)) return `今天复习 · 已复习 ${item.reviewCount ?? 0} 次`
  if (item.nextReviewDate) return `下次 ${formatDate(item.nextReviewDate)} · 已复习 ${item.reviewCount ?? 0} 次`
  return `待安排复习 · 已复习 ${item.reviewCount ?? 0} 次`
}

const repairCardClass = (item: WrongQuestionItem) => {
  if (isDue(item)) return 'repair-card-due'
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

onMounted(() => {
  void loadData()
})
</script>

<style scoped>
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
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.08), transparent 36%), rgba(255, 255, 255, 0.42);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.12);
}

.dark .repair-card__answer {
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.12), transparent 36%), rgba(255, 255, 255, 0.03);
}

.repair-card__answer-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--bc-accent);
}

.repair-card__reason {
  border-radius: 18px;
  padding: 14px 15px;
  background: rgba(15, 23, 42, 0.03);
}

.dark .repair-card__reason {
  background: rgba(255, 255, 255, 0.03);
}

.repair-card__reason-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--bc-ink-tertiary);
}

@media (prefers-reduced-motion: reduce) {
  .repair-card {
    transition-duration: 0.01ms;
  }

  .repair-card:hover,
  .repair-card.is-expanded {
    transform: none;
  }
}
</style>
