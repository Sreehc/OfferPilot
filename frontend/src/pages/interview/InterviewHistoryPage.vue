<template>
  <div class="space-y-6">
    <!-- Header -->
    <section class="paper-panel p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="section-kicker">面试历史</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
            最近完成的模拟面试
          </h3>
          <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
            按方向快速筛选，先看题量、得分和时间，再进入详情复盘。
          </p>
        </div>
        <div class="flex gap-3">
          <el-select v-model="filterDirection" clearable placeholder="按方向筛选" size="large" class="w-[160px]">
            <el-option v-for="d in directions" :key="d" :label="d" :value="d" />
          </el-select>
          <el-button size="large" class="hard-button-secondary" @click="loadData">刷新</el-button>
          <RouterLink to="/interview">
            <el-button size="large" class="hard-button-primary">开始面试</el-button>
          </RouterLink>
        </div>
      </div>
    </section>

    <!-- Loading -->
    <section v-if="loading" class="space-y-3">
      <article v-for="n in 3" :key="n" class="paper-panel p-5">
        <div class="h-4 w-32 animate-pulse rounded bg-slate-200 dark:bg-slate-700"></div>
        <div class="mt-3 h-3 w-full animate-pulse rounded bg-slate-100 dark:bg-slate-800"></div>
      </article>
    </section>

    <!-- Empty State -->
    <section v-else-if="!items.length" class="paper-panel p-6">
      <EmptyState
        icon="chart"
        title="暂无面试记录"
        description="完成一场模拟面试后，记录会自动出现在这里。"
      >
        <template #action>
          <RouterLink to="/interview" class="hard-button-primary inline-flex">
            开始面试
          </RouterLink>
        </template>
      </EmptyState>
    </section>

    <!-- History List -->
    <section v-else>
      <div class="space-y-3">
        <div
          v-for="item in items"
          :key="item.sessionId"
          class="paper-panel cursor-pointer p-5 transition hover:ring-2 hover:ring-accent/20"
          @click="goToDetail(item.sessionId)"
        >
          <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <span class="hard-chip !bg-blue-100 !text-blue-700 text-xs">{{ item.direction }}</span>
                <span class="text-xs text-slate-400 dark:text-slate-500">{{ item.questionCount }} 题</span>
              </div>
              <div class="mt-3 text-lg font-semibold text-ink">完成一次 {{ item.direction }} 方向练习</div>
              <div class="mt-2 flex flex-wrap items-center gap-4 text-xs text-slate-400 dark:text-slate-500">
                <span v-if="item.startTime">{{ formatTime(item.startTime) }}</span>
                <span v-if="item.endTime">结束于 {{ formatTime(item.endTime) }}</span>
              </div>
            </div>
            <div class="flex shrink-0 items-center gap-6">
              <div class="text-right">
                <div class="text-xs text-slate-400 dark:text-slate-500">得分</div>
                <span
                  class="text-2xl font-semibold tracking-[-0.03em]"
                  :class="item.totalScore >= 60 ? 'text-accent' : 'text-red-500'"
                >
                  {{ formatScore(item.totalScore) }}
                </span>
              </div>
              <div class="text-sm font-semibold text-accent">查看详情</div>
            </div>
          </div>
        </div>
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
import { onMounted, ref, watch } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { useRouter } from 'vue-router'
import { fetchInterviewHistoryApi } from '@/api/interview'
import type { InterviewHistoryItem } from '@/types/api'

const router = useRouter()
const directions = ['Spring', 'JVM', 'MySQL', 'Redis', '并发', '微服务']

const items = ref<InterviewHistoryItem[]>([])
const loading = ref(true)
const filterDirection = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = ref(0)

const formatScore = (score: number): string => {
  return Number.isInteger(score) ? String(score) : score.toFixed(2)
}

const formatTime = (time: string): string => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const loadData = async () => {
  loading.value = true
  try {
    const response = await fetchInterviewHistoryApi(
      filterDirection.value || undefined,
      currentPage.value,
      pageSize.value
    )
    items.value = response.data.records
    total.value = response.data.total
    totalPages.value = response.data.totalPages
  } catch {
    ElMessage.error('加载面试历史失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  void loadData()
}

const goToDetail = (sessionId: number) => {
  router.push(`/interview/detail/${sessionId}`)
}

watch(filterDirection, () => {
  currentPage.value = 1
  void loadData()
})

onMounted(() => {
  void loadData()
})
</script>
