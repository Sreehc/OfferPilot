<template>
  <div class="space-y-6">
    <AppShellHeader compact>
      <template #actions>
        <el-select v-model="filterDirection" clearable placeholder="按方向筛选" size="large" class="w-[160px]">
          <el-option v-for="d in directions" :key="d" :label="d" :value="d" />
        </el-select>
        <el-button size="large" class="hard-button-secondary" @click="loadData">刷新</el-button>
        <RouterLink to="/interview">
          <el-button size="large" class="hard-button-primary">做一次诊断</el-button>
        </RouterLink>
      </template>
    </AppShellHeader>

    <section v-if="loading" class="space-y-3">
      <article v-for="n in 3" :key="n" class="shell-section-card p-5">
        <div class="h-4 w-32 animate-pulse rounded bg-slate-200 dark:bg-slate-700"></div>
        <div class="mt-3 h-3 w-full animate-pulse rounded bg-slate-100 dark:bg-slate-800"></div>
      </article>
    </section>

    <section v-else-if="!items.length" class="shell-section-card p-6">
      <EmptyState
        icon="chart"
        title="暂无面试记录"
        description="做完诊断后会显示在这里。"
      >
        <template #action>
          <RouterLink to="/interview" class="hard-button-primary inline-flex">
            开始诊断
          </RouterLink>
        </template>
      </EmptyState>
    </section>

    <section v-else>
      <div class="space-y-3">
        <div
          v-for="item in items"
          :key="item.sessionId"
          class="shell-section-card p-5 transition hover:ring-2 hover:ring-accent/20"
        >
          <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div class="min-w-0 cursor-pointer" @click="goToDetail(item.sessionId)">
              <div class="flex flex-wrap items-center gap-2">
                <span class="hard-chip !bg-blue-100 !text-blue-700 text-xs">{{ item.direction }}</span>
                <span class="text-xs text-slate-400 dark:text-slate-500">{{ item.questionCount }} 题</span>
                <span
                  class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold"
                  :class="item.cardsGenerated ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'"
                >
                  {{ item.cardsGenerated ? `已生成 ${item.generatedCardCount || 0} 张` : '未生成卡片' }}
                </span>
              </div>
              <div class="mt-3 text-lg font-semibold text-ink">{{ item.direction }} 方向诊断</div>
              <div class="mt-2 flex flex-wrap items-center gap-4 text-xs text-slate-400 dark:text-slate-500">
                <span v-if="item.startTime">{{ formatTime(item.startTime) }}</span>
                <span v-if="item.endTime">结束于 {{ formatTime(item.endTime) }}</span>
              </div>
            </div>

            <div class="flex shrink-0 flex-col items-end gap-3">
              <div class="flex items-center gap-6">
                <div class="text-right">
                  <div class="text-xs text-slate-400 dark:text-slate-500">得分</div>
                  <span
                    class="text-2xl font-semibold tracking-[-0.03em]"
                    :class="item.totalScore >= 60 ? 'text-accent' : 'text-red-500'"
                  >
                    {{ formatScore(item.totalScore) }}
                  </span>
                </div>
              </div>
              <div class="flex flex-wrap justify-end gap-2">
                <el-button
                  v-if="!item.cardsGenerated"
                  :loading="sessionActionLoading[item.sessionId] === 'generate'"
                  size="small"
                  class="hard-button-secondary"
                  @click="handleGenerate(item.sessionId)"
                >
                  生成卡片
                </el-button>
                <el-button
                  v-else
                  :loading="sessionActionLoading[item.sessionId] === 'activate'"
                  size="small"
                  class="hard-button-primary"
                  @click="handleActivate(item.sessionId)"
                >
                  加入今日卡片
                </el-button>
                <el-button size="small" text type="primary" @click="goToDetail(item.sessionId)">
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

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
import { onMounted, reactive, ref, watch } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import AppShellHeader from '@/components/AppShellHeader.vue'
import { useRouter } from 'vue-router'
import { activateInterviewCardsApi, fetchInterviewHistoryApi, generateInterviewCardsApi } from '@/api/interview'
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
const sessionActionLoading = reactive<Record<string, '' | 'generate' | 'activate'>>({})

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

const goToDetail = (sessionId: string) => {
  router.push(`/interview/detail/${sessionId}`)
}

const handleGenerate = async (sessionId: string) => {
  sessionActionLoading[sessionId] = 'generate'
  try {
    await generateInterviewCardsApi(sessionId)
    ElMessage.success('已生成面试复习卡片')
    await loadData()
  } catch {
    ElMessage.error('生成卡片失败')
  } finally {
    sessionActionLoading[sessionId] = ''
  }
}

const handleActivate = async (sessionId: string) => {
  sessionActionLoading[sessionId] = 'activate'
  try {
    await activateInterviewCardsApi(sessionId)
    ElMessage.success('已加入今日卡片')
    await router.push('/cards')
  } catch {
    ElMessage.error('加入今日卡片失败')
  } finally {
    sessionActionLoading[sessionId] = ''
  }
}

watch(filterDirection, () => {
  currentPage.value = 1
  void loadData()
})

onMounted(() => {
  void loadData()
})
</script>
