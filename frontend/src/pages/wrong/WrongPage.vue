<template>
  <div class="space-y-6">
    <!-- Header -->
    <section class="paper-panel p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="section-kicker">Wrong Book</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
            {{ total }} 道错题待复习
          </h3>
          <p class="mt-2 text-sm leading-6 text-slate-500">
            低分面试题自动沉淀到此处，标记掌握状态后可在计划中安排复习。
          </p>
        </div>
        <div class="flex gap-3">
          <el-select v-model="filterMastery" clearable placeholder="掌握状态" size="large" class="w-[160px]">
            <el-option label="未开始" value="not_started" />
            <el-option label="复习中" value="reviewing" />
            <el-option label="已掌握" value="mastered" />
          </el-select>
          <el-button size="large" class="hard-button-secondary" @click="loadData">刷新</el-button>
        </div>
      </div>
    </section>

    <!-- Loading -->
    <section v-if="loading" class="grid gap-4 xl:grid-cols-3 md:grid-cols-2">
      <article v-for="n in 3" :key="n" class="metric-card">
        <div class="h-4 w-24 animate-pulse rounded bg-slate-200"></div>
        <div class="mt-4 h-3 w-full animate-pulse rounded bg-slate-100"></div>
        <div class="mt-2 h-3 w-3/4 animate-pulse rounded bg-slate-100"></div>
      </article>
    </section>

    <!-- Empty State -->
    <section v-else-if="!filteredItems.length" class="paper-panel p-8 text-center">
      <div class="text-5xl font-semibold tracking-[-0.03em] text-slate-300">0</div>
      <p class="mt-4 text-lg font-semibold text-ink">错题本为空</p>
      <p class="mt-2 text-sm leading-6 text-slate-500">
        完成一场模拟面试后，低分会自动沉淀到这里。也可以从 Dashboard 快捷进入面试。
      </p>
      <RouterLink to="/interview" class="hard-button-primary mt-6 inline-flex">
        开始面试
      </RouterLink>
    </section>

    <!-- Wrong Items Grid -->
    <section v-else>
      <div class="grid gap-4 xl:grid-cols-3 md:grid-cols-2">
        <article
          v-for="item in filteredItems"
          :key="item.id"
          class="metric-card cursor-pointer"
          :class="expandedId === item.id ? 'ring-2 ring-accent/20' : ''"
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

          <!-- Error Reason (collapsed preview) -->
          <p class="mt-3 text-sm leading-6 text-slate-600">
            {{ item.errorReason || '暂无错误原因记录' }}
          </p>

          <!-- Expanded Detail -->
          <div v-if="expandedId === item.id" class="mt-4 space-y-3 border-t border-slate-200/60 pt-4">
            <!-- User Answer -->
            <div v-if="item.standardAnswer">
              <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">标准答案</div>
              <p class="mt-1 text-sm leading-6 text-slate-700">{{ item.standardAnswer }}</p>
            </div>

            <!-- Mastery Toggle -->
            <div>
              <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">掌握状态</div>
              <div class="mt-2 flex flex-wrap gap-2">
                <button
                  v-for="level in masteryLevels"
                  :key="level.value"
                  type="button"
                  class="hard-chip cursor-pointer transition-all"
                  :class="item.masteryLevel === level.value
                    ? '!bg-accent !text-white'
                    : '!bg-white/80 !text-slate-600 hover:!bg-slate-100'"
                  @click.stop="handleMasteryChange(item.id, level.value)"
                >
                  {{ level.label }}
                </button>
              </div>
            </div>

            <!-- Actions -->
            <div class="flex gap-2 pt-1">
              <RouterLink
                :to="`/interview?reanswer=${item.questionId}`"
                class="hard-button-secondary !min-h-9 !px-3 !py-1 text-xs"
                @click.stop
              >
                重新作答
              </RouterLink>
              <button
                type="button"
                class="text-xs text-slate-400 transition hover:text-red-500"
                @click.stop="handleDelete(item.id)"
              >
                删除
              </button>
            </div>
          </div>

          <!-- Collapsed hint -->
          <div v-else class="mt-3 text-xs tracking-[0.2em] text-slate-400">
            点击展开详情
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
import { deleteWrongApi, fetchWrongListApi, updateMasteryApi } from '@/api/wrong'
import type { WrongQuestionItem } from '@/types/api'

const items = ref<WrongQuestionItem[]>([])
const loading = ref(true)
const expandedId = ref<number | null>(null)
const filterMastery = ref<string>('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)

const masteryLevels = [
  { value: 'not_started' as const, label: '未开始' },
  { value: 'reviewing' as const, label: '复习中' },
  { value: 'mastered' as const, label: '已掌握' }
]

const filteredItems = computed(() => {
  if (!filterMastery.value) return items.value
  return items.value.filter((item) => item.masteryLevel === filterMastery.value)
})

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

const handleMasteryChange = async (id: number, level: WrongQuestionItem['masteryLevel']) => {
  try {
    await updateMasteryApi(id, { masteryLevel: level })
    const item = items.value.find((i) => i.id === id)
    if (item) item.masteryLevel = level
    ElMessage.success(`已标记为"${masteryLabel(level)}"`)
  } catch {
    ElMessage.error('更新掌握状态失败')
  }
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
  return '!bg-white/80 !text-slate-600'
}

onMounted(() => {
  void loadData()
})
</script>
