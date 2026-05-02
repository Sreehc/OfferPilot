<template>
  <div class="space-y-6">
    <!-- Header -->
    <section class="paper-panel p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="section-kicker">面试历史</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
            面试历史记录
          </h3>
          <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
            查看所有已完成的面试记录，点击可查看详情和复盘。
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
    <section v-else-if="!items.length" class="paper-panel p-8 text-center">
      <div class="text-5xl font-semibold tracking-[-0.03em] text-slate-300 dark:text-slate-600">0</div>
      <p class="mt-4 text-lg font-semibold text-ink">暂无面试记录</p>
      <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
        完成一场面试后，记录会自动出现在这里。
      </p>
      <RouterLink to="/interview" class="hard-button-primary mt-6 inline-flex">
        开始面试
      </RouterLink>
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
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <span class="hard-chip !bg-blue-100 !text-blue-700 text-xs">{{ item.direction }}</span>
              <span class="text-sm text-slate-400 dark:text-slate-500">{{ item.questionCount }} 题</span>
            </div>
            <div class="text-right">
              <span
                class="text-2xl font-semibold tracking-[-0.03em]"
                :class="item.totalScore >= 60 ? 'text-accent' : 'text-red-500'"
              >
                {{ formatScore(item.totalScore) }}
              </span>
            </div>
          </div>
          <div class="mt-2 flex items-center gap-4 text-xs text-slate-400 dark:text-slate-500">
            <span v-if="item.startTime">{{ formatTime(item.startTime) }}</span>
            <span v-if="item.endTime">~ {{ formatTime(item.endTime) }}</span>
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
