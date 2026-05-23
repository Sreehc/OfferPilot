<template>
  <div class="wrong-book-shell">
    <section class="shell-section-card workspace-shell wrong-book-workspace">
      <div class="workspace-head">
        <div class="workspace-head__top">
          <div class="workspace-head__main">
            <h1 class="workspace-title">错题本</h1>
          </div>
          <div class="workspace-actions">
            <button
              type="button"
              class="hard-button-secondary"
              :disabled="exporting || wrongItems.length === 0"
              @click="handleExport"
            >
              {{ exporting ? '导出中...' : '导出 Markdown' }}
            </button>
          </div>
        </div>

        <div class="wrong-book-metrics mt-4">
          <article class="wrong-book-metric">
            <span>总错题</span>
            <strong>{{ total }}</strong>
          </article>
          <article class="wrong-book-metric">
            <span>待复习</span>
            <strong>{{ pendingCount }}</strong>
          </article>
          <article class="wrong-book-metric">
            <span>已掌握</span>
            <strong>{{ masteredCount }}</strong>
          </article>
          <article class="wrong-book-metric">
            <span>当前页</span>
            <strong>{{ wrongItems.length }}</strong>
          </article>
        </div>
      </div>

      <div class="workspace-separator"></div>

      <section class="wrong-book-layout workspace-section">
      <article class="wrong-book-panel">
        <div class="wrong-book-section-head">
          <div>
            <h3 class="workspace-section-title">错题列表</h3>
            <p class="workspace-section-summary">默认按复习优先级排序。</p>
          </div>
          <span class="detail-pill">{{ total }} 题</span>
        </div>

        <div v-if="loading" class="py-16 text-center">
          <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
          <p class="mt-4 text-sm text-secondary">正在读取错题列表和复习优先级...</p>
        </div>

        <div v-else-if="wrongItems.length === 0" class="mt-6">
          <EmptyState
            class="empty-state-card"
            icon="clipboard"
            title="当前还没有错题记录"
            description="先做一轮题库训练或模拟面试，低分题和易错题会自动进入这里，方便后续复习。"
            compact
          />
        </div>

        <div v-else class="mt-6 space-y-3">
          <button
            v-for="item in wrongItems"
            :key="item.id"
            type="button"
            class="wrong-book-item"
            :class="{ 'wrong-book-item-active': selectedWrongId === item.id }"
            @click="handleSelect(item.id)"
          >
            <div class="wrong-book-item__head">
              <div class="flex min-w-0 flex-wrap items-center gap-2">
                <span class="hard-chip" :class="masteryChipClass(item.masteryLevel)">
                  {{ masteryLabel(item.masteryLevel) }}
                </span>
                <span v-if="item.nextReviewDate" class="detail-pill">下次 {{ formatDate(item.nextReviewDate) }}</span>
              </div>
              <span class="text-xs text-tertiary">复习 {{ item.reviewCount ?? 0 }} 次</span>
            </div>
            <h4 class="wrong-book-item__title">{{ item.title }}</h4>
            <p class="wrong-book-item__meta">连续 {{ item.streak ?? 0 }} 次 · 间隔 {{ item.intervalDays ?? 0 }} 天</p>
          </button>
        </div>

        <div v-if="totalPages > 1" class="mt-6 flex justify-center">
          <el-pagination
            layout="prev, pager, next"
            :current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            @current-change="handlePageChange"
          />
        </div>
      </article>

      <aside class="wrong-book-detail">
        <div v-if="detailLoading" class="py-16 text-center">
          <div class="mx-auto h-7 w-7 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
          <p class="mt-4 text-sm text-secondary">正在读取这道错题的答案、错误原因和复习记录...</p>
        </div>

        <template v-else-if="selectedWrong">
          <div class="wrong-book-section-head">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.18em] text-tertiary">题目详情</p>
              <h3 class="mt-2 text-xl font-semibold tracking-[-0.03em] text-ink">{{ selectedWrong.title }}</h3>
            </div>
            <span class="hard-chip" :class="masteryChipClass(selectedWrong.masteryLevel)">
              {{ masteryLabel(selectedWrong.masteryLevel) }}
            </span>
          </div>

          <div class="wrong-book-metrics mt-6">
            <article class="wrong-book-metric">
              <span>复盘系数</span>
              <strong>{{ formatEaseFactor(selectedWrong.easeFactor) }}</strong>
            </article>
            <article class="wrong-book-metric">
              <span>间隔天数</span>
              <strong>{{ selectedWrong.intervalDays ?? 0 }}</strong>
            </article>
            <article class="wrong-book-metric">
              <span>连续次数</span>
              <strong>{{ selectedWrong.streak ?? 0 }}</strong>
            </article>
            <article class="wrong-book-metric">
              <span>复习次数</span>
              <strong>{{ selectedWrong.reviewCount ?? 0 }}</strong>
            </article>
          </div>

          <div class="mt-6 flex flex-wrap gap-3">
            <button
              v-for="option in masteryOptions"
              :key="option.value"
              type="button"
              class="wrong-book-mastery-button"
              :class="{
                'wrong-book-mastery-button-active': selectedWrong.masteryLevel === option.value
              }"
              :disabled="savingMastery === option.value"
              @click="handleUpdateMastery(option.value)"
            >
              {{ savingMastery === option.value ? '更新中...' : option.label }}
            </button>
            <button
              type="button"
              class="hard-button-secondary !border-red-300 !text-[var(--bc-coral)]"
              :disabled="deleting"
              @click="handleDelete"
            >
              {{ deleting ? '删除中...' : '删除错题' }}
            </button>
          </div>

          <div class="wrong-book-detail__section">
            <div class="wrong-book-detail__label">下次复习时间</div>
            <p class="text-sm leading-7 text-primary">
              {{ selectedWrong.nextReviewDate ? formatDate(selectedWrong.nextReviewDate) : '这道题还没排到下一次复习，先完成一轮复盘记录。' }}
            </p>
          </div>

          <div class="wrong-book-detail__section">
            <div class="wrong-book-detail__label">标准答案</div>
            <p class="whitespace-pre-wrap text-sm leading-7 text-primary">
              {{ selectedWrong.standardAnswer || '这道题的标准答案还没整理出来，先结合题目和错误原因补一版自己的回答。' }}
            </p>
          </div>

          <div class="wrong-book-detail__section">
            <div class="wrong-book-detail__label">错误原因</div>
            <p class="whitespace-pre-wrap text-sm leading-7 text-primary">
              {{ selectedWrong.errorReason || '这道题还没补错误原因，先回忆卡住的位置，再补一条复盘记录。' }}
            </p>
          </div>
        </template>

        <div v-else class="py-16">
          <EmptyState
            class="empty-state-card"
            icon="clipboard"
            title="先选一道错题开始复盘"
            description="点击左侧任意题目后，这里会显示标准答案、错误原因和下一次复习安排。"
            compact
          />
        </div>
      </aside>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref, watch } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  deleteWrongApi,
  exportWrongMarkdownApi,
  fetchWrongDetailApi,
  fetchWrongListApi,
  updateMasteryApi
} from '@/api/wrong'
import type { WrongQuestionItem } from '@/types/api'

const loading = ref(true)
const detailLoading = ref(false)
const deleting = ref(false)
const exporting = ref(false)
const pageNum = ref(1)
const pageSize = 12
const total = ref(0)
const totalPages = ref(0)
const wrongItems = ref<WrongQuestionItem[]>([])
const selectedWrong = ref<WrongQuestionItem | null>(null)
const selectedWrongId = ref<number | null>(null)
const savingMastery = ref<WrongQuestionItem['masteryLevel'] | null>(null)

const masteryOptions: Array<{ value: WrongQuestionItem['masteryLevel']; label: string }> = [
  { value: 'not_started', label: '设为未开始' },
  { value: 'reviewing', label: '设为复习中' },
  { value: 'mastered', label: '设为已掌握' }
]

const pendingCount = computed(() => wrongItems.value.filter((item) => item.masteryLevel !== 'mastered').length)
const masteredCount = computed(() => wrongItems.value.filter((item) => item.masteryLevel === 'mastered').length)

const masteryLabel = (level: string) => {
  if (level === 'mastered') return '已掌握'
  if (level === 'reviewing') return '复习中'
  return '未开始'
}

const masteryChipClass = (level: string) => {
  if (level === 'mastered') return '!bg-accent !text-white'
  if (level === 'reviewing') return '!bg-amber-100 !text-amber-700'
  return '!bg-[var(--interactive-bg)] !text-secondary'
}

const formatDate = (value?: string) => {
  if (!value) return '待安排'
  return new Date(value).toLocaleDateString('zh-CN')
}

const formatEaseFactor = (value?: number) => {
  return typeof value === 'number' ? value.toFixed(2) : '2.50'
}

const loadList = async () => {
  loading.value = true
  try {
    const { data } = await fetchWrongListApi(pageNum.value, pageSize)
    wrongItems.value = data.records
    total.value = data.total
    totalPages.value = data.totalPages

    if (wrongItems.value.length === 0) {
      selectedWrongId.value = null
      selectedWrong.value = null
      return
    }

    if (!selectedWrongId.value || !wrongItems.value.some((item) => item.id === selectedWrongId.value)) {
      selectedWrongId.value = wrongItems.value[0]!.id
    }
  } catch {
    wrongItems.value = []
    total.value = 0
    totalPages.value = 0
    selectedWrongId.value = null
    selectedWrong.value = null
    ElMessage.error('错题列表还没加载出来，请刷新页面或稍后再试。')
  } finally {
    loading.value = false
  }
}

const loadDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const { data } = await fetchWrongDetailApi(id)
    selectedWrong.value = data
  } catch {
    selectedWrong.value = null
    ElMessage.error('这道错题的详情还没加载出来，请换一题，或稍后再试。')
  } finally {
    detailLoading.value = false
  }
}

const handleSelect = (id: number) => {
  if (selectedWrongId.value === id) return
  selectedWrongId.value = id
}

const handlePageChange = (page: number) => {
  pageNum.value = page
}

const handleUpdateMastery = async (masteryLevel: WrongQuestionItem['masteryLevel']) => {
  if (!selectedWrong.value || selectedWrong.value.masteryLevel === masteryLevel) return
  savingMastery.value = masteryLevel
  try {
    await updateMasteryApi(selectedWrong.value.id, { masteryLevel })
    ElMessage.success('掌握状态已更新')
    await Promise.all([loadList(), loadDetail(selectedWrong.value.id)])
  } catch {
    ElMessage.error('掌握状态还没更新成功，请重新点一次当前目标状态。')
  } finally {
    savingMastery.value = null
  }
}

const handleDelete = async () => {
  if (!selectedWrong.value || !confirm('确定删除这道错题吗？')) return
  deleting.value = true
  try {
    const id = selectedWrong.value.id
    await deleteWrongApi(id)
    ElMessage.success('错题已删除')
    selectedWrongId.value = null
    selectedWrong.value = null
    await loadList()
  } catch {
    ElMessage.error('这道错题还没删除成功，请稍后再试。')
  } finally {
    deleting.value = false
  }
}

const handleExport = async () => {
  exporting.value = true
  try {
    const response = await exportWrongMarkdownApi()
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = 'wrong-questions.md'
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('错题本已导出')
  } catch {
    ElMessage.error('错题本还没导出成功，请稍后再试。')
  } finally {
    exporting.value = false
  }
}

watch(pageNum, () => {
  void loadList()
})

watch(selectedWrongId, (id) => {
  if (!id) return
  void loadDetail(id)
})

onMounted(() => {
  void loadList()
})
</script>

<style scoped>
.wrong-book-layout {
  display: grid;
  gap: 1rem;
}

.wrong-book-workspace {
  padding: 0;
}

.wrong-book-panel,
.wrong-book-detail {
  min-width: 0;
}

.wrong-book-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.9rem 1rem;
}

.wrong-book-hero__copy {
  min-width: 0;
  flex: 1;
}

.wrong-book-hero__actions {
  display: flex;
  align-items: flex-start;
}

.wrong-book-metrics {
  display: grid;
  gap: 0.65rem;
}

.wrong-book-metric {
  border-radius: 14px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 0.7rem 0.85rem;
}

.wrong-book-metric span {
  display: block;
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 700;
}

.wrong-book-metric strong {
  display: block;
  margin-top: 0.35rem;
  color: var(--bc-ink);
  font-size: 1.25rem;
  font-weight: 780;
}

.wrong-book-section-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.wrong-book-item {
  width: 100%;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-card);
  padding: 1rem;
  text-align: left;
  transition:
    transform var(--motion-fast) var(--ease-hard),
    box-shadow var(--motion-fast) var(--ease-hard),
    border-color var(--motion-fast) var(--ease-hard);
}

.wrong-book-item:hover {
  transform: translateY(-1px);
  box-shadow: var(--bc-shadow-hover);
}

.wrong-book-item-active {
  border-color: rgba(var(--bc-accent-rgb), 0.35);
  box-shadow: inset 3px 0 0 rgba(var(--bc-accent-rgb), 0.9);
}

.wrong-book-item__head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.75rem;
}

.wrong-book-item__title {
  margin-top: 0.9rem;
  color: var(--bc-ink);
  font-size: 1rem;
  font-weight: 700;
  line-height: 1.6;
}

.wrong-book-item__meta {
  margin-top: 0.5rem;
  color: var(--bc-ink-secondary);
  font-size: 0.8rem;
}

.wrong-book-detail {
  min-height: 420px;
  padding-left: 0;
}

.wrong-book-mastery-button {
  min-height: 42px;
  border-radius: 999px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-card);
  padding: 0.6rem 1rem;
  color: var(--bc-ink-secondary);
  font-size: 0.9rem;
  font-weight: 700;
  transition:
    background var(--motion-fast) var(--ease-hard),
    color var(--motion-fast) var(--ease-hard),
    border-color var(--motion-fast) var(--ease-hard);
}

.wrong-book-mastery-button-active {
  border-color: rgba(var(--bc-accent-rgb), 0.4);
  background: rgba(var(--bc-accent-rgb), 0.08);
  color: var(--bc-ink);
}

.wrong-book-detail__section {
  margin-top: 1.5rem;
  border-top: 1px solid var(--bc-border-subtle);
  padding-top: 1.5rem;
}

.wrong-book-detail__label {
  margin-bottom: 0.65rem;
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

@media (min-width: 900px) {
  .wrong-book-layout {
    grid-template-columns: minmax(0, 1.15fr) minmax(340px, 0.85fr);
  }

  .wrong-book-detail {
    padding-left: 1.25rem;
    border-left: 1px solid var(--bc-border-subtle);
  }
}

@media (min-width: 700px) {
  .wrong-book-metrics {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
