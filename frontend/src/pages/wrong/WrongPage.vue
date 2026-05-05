<template>
  <div class="space-y-6">
    <!-- Header -->
    <section class="cockpit-panel p-4 sm:p-6">
      <div class="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
        <div class="max-w-3xl">
          <div class="flex items-center gap-3">
            <span class="state-pulse" aria-hidden="true"></span>
            <p class="section-kicker">Repair Vault</p>
          </div>
          <h3 class="mt-4 font-display text-4xl font-semibold leading-none tracking-[-0.04em] text-ink sm:text-5xl">
            {{ total }} 个知识断点
          </h3>
          <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
            低分面试题自动沉淀到修复库。优先处理逾期、未开始和复习中的题目，再回到面试舱验证。
          </p>
        </div>
        <div class="flex flex-wrap gap-2 sm:gap-3">
          <RouterLink to="/review" class="hard-button-primary" :class="{ 'animate-pulse': todayDue > 0 }">
            开始复习{{ todayDue > 0 ? ` (${todayDue})` : '' }}
          </RouterLink>
          <el-select v-model="filterReview" clearable placeholder="复习状态" size="large" class="w-[130px] sm:w-[160px]">
            <el-option label="今日待复习" value="due_today" />
            <el-option label="已排期" value="scheduled" />
            <el-option label="已掌握" value="mastered" />
          </el-select>
          <el-select v-model="filterMastery" clearable placeholder="掌握状态" size="large" class="w-[130px] sm:w-[160px]">
            <el-option label="未开始" value="not_started" />
            <el-option label="复习中" value="reviewing" />
            <el-option label="已掌握" value="mastered" />
          </el-select>
          <el-button size="large" class="hard-button-secondary" :loading="exporting" @click="handleExport">
            导出 MD
          </el-button>
          <el-button size="large" class="hard-button-secondary" @click="loadData">刷新</el-button>
        </div>
      </div>
    </section>

    <section class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      <article
        v-for="signal in repairSignals"
        :key="signal.label"
        class="data-slab p-4"
      >
        <div class="flex items-center justify-between gap-3">
          <p class="metric-label">{{ signal.label }}</p>
          <span class="h-2.5 w-2.5 rounded-full" :class="signal.dotClass"></span>
        </div>
        <p class="metric-value !mt-3 !text-3xl">{{ signal.value }}</p>
        <p class="mt-1 text-xs text-slate-400 dark:text-slate-500">{{ signal.desc }}</p>
      </article>
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
      <EmptyState
        icon="review"
        title="错题本为空"
        description="完成一场模拟面试后，低分会自动沉淀到这里。也可以从 Dashboard 快捷进入面试。"
      >
        <template #action>
          <RouterLink to="/interview" class="hard-button-primary inline-flex">
            开始面试
          </RouterLink>
        </template>
      </EmptyState>
    </section>

    <!-- Wrong Items Grid -->
    <section v-else>
      <div class="grid gap-4 xl:grid-cols-3 md:grid-cols-2">
        <article
          v-for="item in prioritizedItems"
          :key="item.id"
          class="repair-card cursor-pointer p-4"
          :class="[repairCardClass(item), expandedId === item.id ? 'is-expanded' : '']"
          @click="toggleExpand(item.id)"
        >
          <!-- Header -->
          <div class="flex items-start justify-between gap-3">
            <span class="font-semibold text-ink leading-snug">{{ item.title }}</span>
            <span
              class="hard-chip shrink-0"
              :class="masteryChipClass(item.masteryLevel)"
            >
              {{ masteryLabel(item.masteryLevel) }}
            </span>
          </div>

          <!-- Review Info -->
          <div class="mt-3 flex flex-wrap items-center gap-3 text-xs text-slate-500 dark:text-slate-400">
            <span v-if="item.nextReviewDate">
              下次复习: <span :class="isDueToday(item.nextReviewDate) ? 'text-coral font-semibold' : ''">
                {{ formatDate(item.nextReviewDate) }}
              </span>
            </span>
            <span v-if="item.reviewCount">已复习 {{ item.reviewCount }} 次</span>
            <span v-if="item.streak && item.streak > 0" class="text-lime">连续 {{ item.streak }}</span>
          </div>

          <!-- Error Reason (collapsed preview) -->
          <p class="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300">
            {{ item.errorReason || '暂无错误原因记录' }}
          </p>

          <!-- Expanded Detail -->
          <div v-if="expandedId === item.id" class="mt-4 space-y-4 border-t border-[var(--bc-line)] pt-4">
            <div class="grid gap-3 sm:grid-cols-3">
              <div class="rounded-2xl border border-[var(--bc-line)] bg-white/35 p-3 dark:bg-white/5">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">状态</div>
                <p class="mt-2 text-sm font-semibold text-ink">{{ masteryLabel(item.masteryLevel) }}</p>
              </div>
              <div class="rounded-2xl border border-[var(--bc-line)] bg-white/35 p-3 dark:bg-white/5">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">间隔</div>
                <p class="mt-2 text-sm font-semibold text-ink">{{ item.intervalDays ?? 1 }} 天</p>
              </div>
              <div class="rounded-2xl border border-[var(--bc-line)] bg-white/35 p-3 dark:bg-white/5">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">EF</div>
                <p class="mt-2 text-sm font-semibold text-ink">{{ item.easeFactor?.toFixed(2) ?? '2.50' }}</p>
              </div>
            </div>

            <!-- Standard Answer -->
            <div v-if="item.standardAnswer">
              <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">答案修复参考</div>
              <p class="mt-2 text-sm leading-7 text-slate-700 dark:text-slate-200">{{ item.standardAnswer }}</p>
            </div>

            <!-- Actions -->
            <div class="flex flex-wrap gap-2 pt-1">
              <RouterLink
                :to="`/interview?reanswer=${item.questionId}`"
                class="hard-button-primary !min-h-10 !px-3 !py-1 text-xs"
                @click.stop
              >
                重新作答
              </RouterLink>
              <RouterLink
                to="/review"
                class="hard-button-secondary !min-h-10 !px-3 !py-1 text-xs"
                @click.stop
              >
                进入复习
              </RouterLink>
              <button
                type="button"
                class="hard-button-secondary !min-h-10 !px-3 !py-1 text-xs"
                @click.stop="toggleExpand(item.id)"
              >
                收起答案
              </button>
              <button
                type="button"
                class="min-h-10 px-3 py-1 text-xs text-slate-400 transition hover:text-coral dark:text-slate-500"
                @click.stop="handleDelete(item.id)"
              >
                删除
              </button>
            </div>
          </div>

          <!-- Collapsed hint -->
          <div v-else class="mt-3 text-xs tracking-[0.2em] text-slate-400 dark:text-slate-500">
            点击展开修复动作
          </div>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { deleteWrongApi, exportWrongMarkdownApi, fetchWrongListApi } from '@/api/wrong'
import { fetchReviewTodayApi } from '@/api/review'
import type { WrongQuestionItem } from '@/types/api'

const items = ref<WrongQuestionItem[]>([])
const loading = ref(true)
const exporting = ref(false)
const expandedId = ref<number | null>(null)
const filterMastery = ref<string>('')
const filterReview = ref<string>('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)
const todayDue = ref(0)

const filteredItems = computed(() => {
  let result = items.value

  if (filterMastery.value) {
    result = result.filter((item) => item.masteryLevel === filterMastery.value)
  }

  if (filterReview.value) {
    const today = new Date().toISOString().slice(0, 10)
    if (filterReview.value === 'due_today') {
      result = result.filter((item) => item.nextReviewDate && item.nextReviewDate <= today)
    } else if (filterReview.value === 'scheduled') {
      result = result.filter((item) => item.nextReviewDate && item.nextReviewDate > today)
    } else if (filterReview.value === 'mastered') {
      result = result.filter((item) => item.masteryLevel === 'mastered')
    }
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

const masteredCount = computed(() => items.value.filter((item) => item.masteryLevel === 'mastered').length)
const masteryRate = computed(() => {
  if (!items.value.length) return 0
  return Math.round((masteredCount.value / items.value.length) * 100)
})

const longestOverdue = computed(() => {
  const today = new Date(todayIso()).getTime()
  return items.value.reduce((max, item) => {
    if (!item.nextReviewDate || item.nextReviewDate > todayIso()) return max
    const days = Math.max(0, Math.floor((today - new Date(item.nextReviewDate).getTime()) / 86400000))
    return Math.max(max, days)
  }, 0)
})

const repairSignals = computed(() => [
  { label: '待修复', value: String(total.value), desc: '当前错题库总量', dotClass: 'bg-accent' },
  { label: '今日到期', value: String(todayDue.value), desc: '建议优先进入复习', dotClass: todayDue.value > 0 ? 'bg-coral' : 'bg-lime' },
  { label: '掌握率', value: `${masteryRate.value}%`, desc: `${masteredCount.value} 道已掌握`, dotClass: masteryRate.value >= 70 ? 'bg-lime' : 'bg-amber' },
  { label: '最长逾期', value: `${longestOverdue.value} 天`, desc: '越久越应先处理', dotClass: longestOverdue.value > 0 ? 'bg-coral' : 'bg-cyan' }
])

const loadData = async () => {
  loading.value = true
  try {
    const response = await fetchWrongListApi(currentPage.value, pageSize.value)
    items.value = response.data.records
    total.value = response.data.total
    totalPages.value = response.data.totalPages

    // Load today's due count
    try {
      const reviewRes = await fetchReviewTodayApi()
      todayDue.value = reviewRes.data.length
    } catch {
      // Silently fail
    }
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

const handleExport = async () => {
  exporting.value = true
  try {
    const response = await exportWrongMarkdownApi()
    const blob = new Blob([response.data], { type: 'text/markdown' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'wrong-questions.md'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('错题已导出为 Markdown')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

const toggleExpand = (id: number) => {
  expandedId.value = expandedId.value === id ? null : id
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要从错题本中移除这道题吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteWrongApi(id)
    items.value = items.value.filter((i) => i.id !== id)
    if (expandedId.value === id) expandedId.value = null
    ElMessage.success('已从错题本移除')
  } catch {
    // User cancelled or delete failed
  }
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

const isDueToday = (dateStr: string) => {
  if (!dateStr) return false
  return dateStr <= todayIso()
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
  border-radius: var(--radius-md);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.08), transparent 34%),
    var(--bc-surface-card);
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
