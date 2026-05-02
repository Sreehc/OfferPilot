<template>
  <div class="space-y-6">
    <!-- Header -->
    <section class="paper-panel p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="section-kicker">Study Plan</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
            {{ currentPlan ? currentPlan.title : '生成学习计划' }}
          </h3>
          <p class="mt-2 text-sm leading-6 text-slate-500">
            {{ currentPlan ? currentPlan.goal : '根据错题薄弱点，AI 自动生成每日复习任务。' }}
          </p>
          <div v-if="currentPlan" class="mt-3 flex items-center gap-3">
            <span class="hard-chip !bg-slate-100 !text-slate-600 text-xs">
              v{{ currentPlan.version || 1 }}
            </span>
            <span v-if="currentPlan.startDate" class="text-xs text-slate-400">
              {{ currentPlan.startDate }} ~ {{ currentPlan.endDate }}
            </span>
          </div>
        </div>
        <div class="flex gap-3">
          <el-button size="large" class="hard-button-secondary" @click="loadCurrentPlan">刷新</el-button>
          <el-button
            v-if="currentPlan"
            size="large"
            class="hard-button-secondary"
            :loading="adjusting"
            @click="handleAdjust"
          >
            {{ adjusting ? 'AI 调整中...' : 'AI 调整' }}
          </el-button>
          <el-button size="large" class="hard-button-primary" @click="showGenerate = true">
            {{ currentPlan ? '重新生成' : '生成计划' }}
          </el-button>
        </div>
      </div>
    </section>

    <!-- Generate Form -->
    <section v-if="showGenerate" class="paper-panel p-6">
      <p class="section-kicker">Plan Generator</p>
      <h4 class="mt-4 text-lg font-semibold text-ink">设置计划参数</h4>
      <div class="mt-4 grid gap-4 sm:grid-cols-2">
        <div>
          <label class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">复习方向</label>
          <el-select v-model="form.direction" size="large" class="mt-2 w-full" placeholder="选择方向">
            <el-option label="Java 后端" value="Java 后端" />
            <el-option label="Spring 全家桶" value="Spring 全家桶" />
            <el-option label="数据库与缓存" value="数据库与缓存" />
            <el-option label="系统设计" value="系统设计" />
            <el-option label="综合复习" value="综合复习" />
          </el-select>
        </div>
        <div>
          <label class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">计划天数</label>
          <el-input-number
            v-model="form.days"
            :min="1"
            :max="30"
            size="large"
            class="mt-2 w-full"
          />
        </div>
      </div>
      <div class="mt-4 flex gap-3">
        <el-button size="large" class="hard-button-primary" :loading="generating" @click="handleGenerate">
          {{ generating ? 'AI 生成中...' : '确认生成' }}
        </el-button>
        <el-button size="large" class="hard-button-secondary" @click="showGenerate = false">取消</el-button>
      </div>
    </section>

    <!-- Loading -->
    <section v-if="loading" class="paper-panel p-8 text-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      <p class="mt-4 text-sm text-slate-500">加载计划中...</p>
    </section>

    <!-- Empty State -->
    <section v-else-if="!currentPlan && !showGenerate" class="paper-panel p-8 text-center">
      <div class="text-5xl font-semibold tracking-[-0.03em] text-slate-300">0</div>
      <p class="mt-4 text-lg font-semibold text-ink">暂无学习计划</p>
      <p class="mt-2 text-sm leading-6 text-slate-500">
        点击「生成计划」，AI 将根据你的错题薄弱点自动生成每日任务。
      </p>
      <el-button size="large" class="hard-button-primary mt-6" @click="showGenerate = true">
        生成计划
      </el-button>
    </section>

    <!-- Plan Content -->
    <template v-if="currentPlan && !loading">
      <!-- Progress Bar + Health Score -->
      <section class="paper-panel p-6">
        <div class="flex items-center justify-between">
          <div>
            <span class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">进度</span>
            <span class="ml-3 text-2xl font-semibold text-ink">{{ completedCount }}/{{ currentPlan.tasks.length }}</span>
          </div>
          <div class="flex items-center gap-3">
            <!-- Health Score Badge -->
            <div class="flex items-center gap-2">
              <span class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">健康分</span>
              <span
                class="inline-flex h-8 w-8 items-center justify-center rounded-full text-xs font-bold text-white"
                :class="healthColorClass"
              >
                {{ currentPlan.healthScore ?? '—' }}
              </span>
            </div>
            <span class="hard-chip" :class="currentPlan.status === 'active' ? '!bg-accent !text-white' : '!bg-slate-200 !text-slate-600'">
              {{ currentPlan.status === 'active' ? '进行中' : '已完成' }}
            </span>
          </div>
        </div>
        <div class="mt-4 h-2 overflow-hidden rounded-full bg-slate-100">
          <div
            class="h-full rounded-full bg-accent transition-all duration-500"
            :style="{ width: progressPercent + '%' }"
          ></div>
        </div>
        <!-- Health Score Bar -->
        <div v-if="currentPlan.healthScore != null" class="mt-3">
          <div class="flex items-center justify-between mb-1">
            <span class="text-xs text-slate-400">计划健康度</span>
            <span class="text-xs font-medium" :class="healthTextColor">{{ currentPlan.healthScore }}/100</span>
          </div>
          <div class="h-1.5 overflow-hidden rounded-full bg-slate-100">
            <div
              class="h-full rounded-full transition-all duration-500"
              :class="healthBarColor"
              :style="{ width: currentPlan.healthScore + '%' }"
            ></div>
          </div>
        </div>
      </section>

      <!-- Task Timeline -->
      <section class="paper-panel p-6">
        <p class="section-kicker">Task Timeline</p>
        <div class="mt-4 space-y-2">
          <template v-for="(group, dateKey) in groupedTasks" :key="dateKey">
            <div class="mb-4">
              <div class="flex items-center gap-2">
                <span class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">{{ dateKey }}</span>
                <span class="text-xs text-slate-400">({{ group.filter(t => t.status === 'done').length }}/{{ group.length }})</span>
              </div>
              <div class="mt-2 divide-y divide-slate-200/60 overflow-hidden rounded-xl border border-slate-200/60">
                <div
                  v-for="task in group"
                  :key="task.id"
                  class="flex items-center gap-4 px-4 py-3 transition hover:bg-slate-50/50"
                >
                  <!-- Checkbox -->
                  <button
                    type="button"
                    class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-2 transition-all"
                    :class="task.status === 'done'
                      ? 'border-accent bg-accent text-white'
                      : 'border-slate-300 hover:border-accent'"
                    @click="toggleTaskStatus(task)"
                  >
                    <svg v-if="task.status === 'done'" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  </button>

                  <!-- Content -->
                  <div class="flex-1 min-w-0">
                    <span
                      class="text-sm font-medium"
                      :class="task.status === 'done' ? 'text-slate-400 line-through' : 'text-ink'"
                    >
                      {{ task.content }}
                    </span>
                  </div>

                  <!-- Type Badge -->
                  <span
                    class="hard-chip shrink-0 text-xs"
                    :class="task.taskType === 'interview'
                      ? '!bg-purple-100 !text-purple-700'
                      : '!bg-blue-100 !text-blue-700'"
                  >
                    {{ task.taskType === 'interview' ? '面试' : '复习' }}
                  </span>
                </div>
              </div>
            </div>
          </template>
        </div>
      </section>

      <!-- Plan History -->
      <section v-if="planHistory.length > 1" class="paper-panel p-6">
        <p class="section-kicker">Version History</p>
        <h4 class="mt-3 text-lg font-semibold text-ink">计划版本记录</h4>
        <div class="mt-4 divide-y divide-slate-200/60 overflow-hidden rounded-xl border border-slate-200/60">
          <div
            v-for="version in planHistory"
            :key="version.id"
            class="flex items-center gap-4 px-4 py-3 transition hover:bg-slate-50/50"
            :class="version.id === currentPlan?.id ? 'bg-accent/5' : ''"
          >
            <span
              class="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold text-white"
              :class="version.id === currentPlan?.id ? 'bg-accent' : 'bg-slate-400'"
            >
              v{{ version.version || 1 }}
            </span>
            <div class="flex-1 min-w-0">
              <span class="text-sm font-medium text-ink">{{ version.title }}</span>
              <span class="ml-2 text-xs text-slate-400">{{ version.startDate }} ~ {{ version.endDate }}</span>
            </div>
            <span class="text-xs text-slate-400">
              {{ version.completedTasks || 0 }}/{{ version.totalTasks || 0 }} 完成
            </span>
            <span
              class="hard-chip text-xs"
              :class="version.status === 'active'
                ? '!bg-accent !text-white'
                : '!bg-slate-200 !text-slate-600'"
            >
              {{ version.status === 'active' ? '当前' : '已归档' }}
            </span>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import {
  fetchCurrentPlanApi,
  generatePlanApi,
  updateTaskStatusApi,
  adjustPlanApi,
  fetchPlanHistoryApi
} from '@/api/plan'
import type { StudyPlanItem, StudyPlanTaskItem } from '@/types/api'

const currentPlan = ref<StudyPlanItem | null>(null)
const planHistory = ref<StudyPlanItem[]>([])
const loading = ref(true)
const generating = ref(false)
const adjusting = ref(false)
const showGenerate = ref(false)

const form = ref({
  direction: 'Java 后端',
  days: 7
})

const completedCount = computed(() => {
  if (!currentPlan.value) return 0
  return currentPlan.value.tasks.filter((t) => t.status === 'done').length
})

const progressPercent = computed(() => {
  if (!currentPlan.value || currentPlan.value.tasks.length === 0) return 0
  return Math.round((completedCount.value / currentPlan.value.tasks.length) * 100)
})

const healthColorClass = computed(() => {
  const score = currentPlan.value?.healthScore ?? 100
  if (score >= 70) return 'bg-green-500'
  if (score >= 40) return 'bg-amber-500'
  return 'bg-red-500'
})

const healthBarColor = computed(() => {
  const score = currentPlan.value?.healthScore ?? 100
  if (score >= 70) return 'bg-green-500'
  if (score >= 40) return 'bg-amber-500'
  return 'bg-red-500'
})

const healthTextColor = computed(() => {
  const score = currentPlan.value?.healthScore ?? 100
  if (score >= 70) return 'text-green-600'
  if (score >= 40) return 'text-amber-600'
  return 'text-red-600'
})

const groupedTasks = computed(() => {
  if (!currentPlan.value) return {}
  const groups: Record<string, StudyPlanTaskItem[]> = {}
  for (const task of currentPlan.value.tasks) {
    const key = task.taskDate || '未安排'
    if (!groups[key]) groups[key] = []
    groups[key].push(task)
  }
  return groups
})

const loadCurrentPlan = async () => {
  loading.value = true
  try {
    const response = await fetchCurrentPlanApi()
    currentPlan.value = response.data
    if (currentPlan.value) {
      void loadHistory()
    }
  } catch {
    ElMessage.error('加载学习计划失败')
  } finally {
    loading.value = false
  }
}

const loadHistory = async () => {
  try {
    const response = await fetchPlanHistoryApi()
    planHistory.value = response.data || []
  } catch {
    // Silently fail — history is supplementary
  }
}

const handleGenerate = async () => {
  generating.value = true
  try {
    const response = await generatePlanApi({
      direction: form.value.direction,
      days: form.value.days
    })
    currentPlan.value = response.data
    showGenerate.value = false
    ElMessage.success('学习计划已生成')
    void loadHistory()
  } catch {
    ElMessage.error('计划生成失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

const handleAdjust = async () => {
  if (!currentPlan.value) return
  adjusting.value = true
  try {
    const response = await adjustPlanApi(currentPlan.value.id)
    currentPlan.value = response.data
    ElMessage.success(`计划已调整为 v${response.data.version || 2}`)
    void loadHistory()
  } catch {
    ElMessage.error('计划调整失败，请稍后重试')
  } finally {
    adjusting.value = false
  }
}

const toggleTaskStatus = async (task: StudyPlanTaskItem) => {
  const newStatus = task.status === 'done' ? 'todo' : 'done'
  try {
    await updateTaskStatusApi(task.id, { status: newStatus })
    task.status = newStatus
  } catch {
    ElMessage.error('更新任务状态失败')
  }
}

onMounted(() => {
  void loadCurrentPlan()
})
</script>
