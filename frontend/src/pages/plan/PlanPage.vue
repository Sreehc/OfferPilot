<template>
  <div class="plan-orbit space-y-6">
    <section class="cockpit-panel p-4 sm:p-6">
      <div class="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
        <div class="max-w-3xl">
          <div class="flex items-center gap-3">
            <span class="state-pulse" aria-hidden="true"></span>
            <p class="section-kicker">学习计划</p>
          </div>
          <h3 class="mt-4 font-display text-4xl font-semibold leading-none tracking-[-0.04em] text-ink sm:text-5xl">
            {{ currentPlan ? currentPlan.title : '生成学习计划' }}
          </h3>
          <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
            {{ currentPlan ? currentPlan.goal : '根据你的薄弱点生成一份可执行的每日学习安排。' }}
          </p>
          <div v-if="currentPlan" class="mt-3 flex items-center gap-3">
            <span class="hard-chip !bg-slate-100 dark:!bg-slate-700 !text-slate-600 dark:!text-slate-300 text-xs">
              v{{ currentPlan.version || 1 }}
            </span>
            <span v-if="currentPlan.startDate" class="text-xs text-slate-400 dark:text-slate-500">
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
            {{ adjusting ? '调整中...' : '调整计划' }}
          </el-button>
          <el-button size="large" class="hard-button-primary" @click="showGenerate = true">
            {{ currentPlan ? '重新生成' : '生成计划' }}
          </el-button>
        </div>
      </div>
    </section>

    <section v-if="showGenerate" class="cockpit-panel p-6">
      <p class="section-kicker">生成计划</p>
      <h4 class="mt-4 text-lg font-semibold text-ink">设置计划方向和时长</h4>

      <div v-if="abilityProfile?.weakCategories && abilityProfile.weakCategories.length > 0" class="mt-4 surface-muted p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-red-600 dark:text-red-400 mb-2">检测到薄弱分类</p>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="cat in abilityProfile.weakCategories"
            :key="cat"
            class="px-2.5 py-1 text-xs font-medium rounded-full bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400"
          >
            {{ cat }}
          </span>
        </div>
        <p class="mt-2 text-xs text-slate-500 dark:text-slate-400">
          已根据当前薄弱分类提供推荐方向。
        </p>
      </div>

      <div class="mt-4 grid gap-4 sm:grid-cols-2">
        <div>
          <label class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">复习方向</label>
          <el-select v-model="form.direction" size="large" class="mt-2 w-full" placeholder="选择方向">
            <el-option label="Java 后端" value="Java 后端" />
            <el-option label="Spring 全家桶" value="Spring 全家桶" />
            <el-option label="数据库与缓存" value="数据库与缓存" />
            <el-option label="系统设计" value="系统设计" />
            <el-option label="综合复习" value="综合复习" />
          </el-select>
        </div>
        <div>
          <label class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">计划天数</label>
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
          {{ generating ? '生成中...' : '确认生成' }}
        </el-button>
        <el-button size="large" class="hard-button-secondary" @click="showGenerate = false">取消</el-button>
      </div>
    </section>

    <section v-if="loading" class="cockpit-panel p-8 text-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      <p class="mt-4 text-sm text-slate-500 dark:text-slate-400">加载计划中...</p>
    </section>

    <section v-else-if="!currentPlan && !showGenerate" class="cockpit-panel p-6">
      <EmptyState
        icon="document"
        title="暂无学习计划"
        description="生成计划后，这里会显示今天的学习任务。"
      >
        <template #action>
          <el-button size="large" class="hard-button-primary" @click="showGenerate = true">
            生成计划
          </el-button>
        </template>
      </EmptyState>
    </section>

    <template v-if="currentPlan && !loading">
      <section class="grid gap-4 lg:grid-cols-[minmax(0,1.18fr)_minmax(300px,0.82fr)]">
        <article class="cockpit-panel p-6">
          <div class="flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p class="section-kicker">当前计划</p>
              <h4 class="mt-3 font-display text-4xl font-semibold leading-none text-ink">{{ progressPercent }}% 完成</h4>
              <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
                已完成 {{ completedCount }} / {{ currentPlan.tasks.length }} 个任务。
              </p>
            </div>
            <svg class="h-28 w-28 shrink-0 -rotate-90" viewBox="0 0 36 36" aria-label="计划完成进度">
              <circle cx="18" cy="18" r="15.5" fill="none" class="stroke-slate-200 dark:stroke-white/10" stroke-width="3" />
              <circle
                cx="18" cy="18" r="15.5" fill="none"
                class="stroke-accent transition-all duration-700"
                stroke-width="3"
                stroke-linecap="round"
                :stroke-dasharray="`${progressPercent * 0.974} 97.4`"
              />
              <text x="18" y="20" class="fill-ink text-[7px] font-semibold" text-anchor="middle" transform="rotate(90 18 18)">{{ progressPercent }}%</text>
            </svg>
          </div>
          <div class="mt-6 h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-white/10">
            <div
              class="h-full rounded-full bg-accent transition-all duration-500"
              :style="{ width: progressPercent + '%' }"
            ></div>
          </div>
        </article>

        <aside class="cockpit-panel p-6">
          <p class="section-kicker">今日任务</p>
          <div v-if="todayTasks.length" class="mt-4 space-y-3">
            <div
              v-for="task in todayTasks"
              :key="task.id"
              class="rounded-2xl border border-[var(--bc-line)] bg-white/35 p-3 dark:bg-white/5"
            >
              <div class="flex items-start gap-3">
                <button
                  type="button"
                  class="orbit-check mt-0.5"
                  :class="task.status === 'done' ? 'is-done' : ''"
                  :aria-label="task.status === 'done' ? '标记为未完成' : '标记为已完成'"
                  @click="toggleTaskStatus(task)"
                >
                  <svg v-if="task.status === 'done'" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                  </svg>
                </button>
                <div class="min-w-0">
                  <span class="hard-chip text-[10px]" :class="task.taskType === 'interview' ? '!bg-accent/10 !text-accent' : '!bg-cyan/10 !text-cyan'">
                    {{ taskTypeLabel(task.taskType) }}
                  </span>
                  <p class="mt-2 text-sm font-medium leading-6 text-ink" :class="task.status === 'done' ? 'line-through opacity-60' : ''">
                    {{ task.content }}
                  </p>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="mt-4 rounded-2xl border border-[var(--bc-line)] bg-white/35 p-5 text-sm text-slate-500 dark:bg-white/5 dark:text-slate-400">
            今天没有待处理任务。
          </div>
          <div class="mt-5 grid grid-cols-2 gap-3">
            <div class="data-slab p-3">
              <p class="metric-label">健康分</p>
              <p class="metric-value !mt-2 !text-2xl" :class="healthTextColor">{{ currentPlan.healthScore ?? '—' }}</p>
            </div>
            <div class="data-slab p-3">
              <p class="metric-label">状态</p>
              <p class="metric-value !mt-2 !text-2xl">{{ currentPlan.status === 'active' ? '进行中' : '已完成' }}</p>
            </div>
          </div>
        </aside>
      </section>

      <section class="cockpit-panel p-6">
        <p class="section-kicker">完整时间线</p>
        <h4 class="mt-3 text-lg font-semibold text-ink">按日期查看全部任务</h4>
        <div class="mt-5 space-y-6">
          <template v-for="(group, dateKey) in groupedTasks" :key="dateKey">
            <div class="orbit-day">
              <div class="flex flex-wrap items-center gap-2">
                <span class="font-mono text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">{{ dateKey }}</span>
                <span class="text-xs text-slate-400 dark:text-slate-500">{{ group.filter(t => t.status === 'done').length }}/{{ group.length }} 已完成</span>
              </div>
              <div class="mt-3 space-y-3">
                <div
                  v-for="task in group"
                  :key="task.id"
                  class="orbit-task"
                  :class="task.status === 'done' ? 'is-done' : ''"
                >
                  <button
                    type="button"
                    class="orbit-check"
                    :class="task.status === 'done' ? 'is-done' : ''"
                    :aria-label="task.status === 'done' ? '标记为未完成' : '标记为已完成'"
                    @click="toggleTaskStatus(task)"
                  >
                    <svg v-if="task.status === 'done'" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  </button>

                  <div class="flex-1 min-w-0">
                    <span
                      class="text-sm font-medium leading-6"
                      :class="task.status === 'done' ? 'text-slate-400 dark:text-slate-500 line-through' : 'text-ink'"
                    >
                      {{ task.content }}
                    </span>
                  </div>

                  <span
                    class="hard-chip shrink-0 text-xs"
                    :class="task.taskType === 'interview'
                      ? '!bg-accent/10 !text-accent'
                      : '!bg-cyan/10 !text-cyan'"
                  >
                    {{ taskTypeLabel(task.taskType) }}
                  </span>
                </div>
              </div>
            </div>
          </template>
        </div>
      </section>

      <section v-if="planHistory.length > 1" class="cockpit-panel p-6">
        <details>
          <summary class="cursor-pointer list-none">
            <p class="section-kicker">版本记录</p>
            <h4 class="mt-3 text-lg font-semibold text-ink">查看历史版本</h4>
          </summary>
          <div class="mt-4 space-y-2">
            <div
              v-for="version in planHistory"
              :key="version.id"
              class="flex items-center gap-4 rounded-2xl border border-[var(--bc-line)] bg-white/35 px-4 py-3 transition hover:bg-white/50 dark:bg-white/5 dark:hover:bg-white/10"
              :class="version.id === currentPlan?.id ? 'border-[var(--bc-line-hot)]' : ''"
            >
              <span
                class="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold text-white"
                :class="version.id === currentPlan?.id ? 'bg-accent' : 'bg-slate-400 dark:bg-slate-600'"
              >
                v{{ version.version || 1 }}
              </span>
              <div class="flex-1 min-w-0">
                <span class="text-sm font-medium text-ink">{{ version.title }}</span>
                <span class="ml-2 text-xs text-slate-400 dark:text-slate-500">{{ version.startDate }} ~ {{ version.endDate }}</span>
              </div>
              <span class="text-xs text-slate-400 dark:text-slate-500">
                {{ version.completedTasks || 0 }}/{{ version.totalTasks || 0 }} 完成
              </span>
              <span
                class="hard-chip text-xs"
                :class="version.status === 'active'
                  ? '!bg-accent !text-white'
                  : '!bg-slate-200 dark:!bg-slate-700 !text-slate-600 dark:!text-slate-300'"
              >
                {{ version.status === 'active' ? '当前' : '已归档' }}
              </span>
            </div>
          </div>
        </details>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  fetchCurrentPlanApi,
  generatePlanApi,
  updateTaskStatusApi,
  adjustPlanApi,
  fetchPlanHistoryApi
} from '@/api/plan'
import { fetchAbilityProfileApi } from '@/api/adaptive'
import type { StudyPlanItem, StudyPlanTaskItem, AbilityProfile } from '@/types/api'

const currentPlan = ref<StudyPlanItem | null>(null)
const planHistory = ref<StudyPlanItem[]>([])
const loading = ref(true)
const generating = ref(false)
const adjusting = ref(false)
const showGenerate = ref(false)
const abilityProfile = ref<AbilityProfile | null>(null)

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

const todayTasks = computed(() => {
  if (!currentPlan.value) return []
  const today = new Date().toISOString().slice(0, 10)
  return currentPlan.value.tasks.filter((task) => task.taskDate === today)
})

const taskTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    interview: '面试',
    review: '复习',
    wrong: '错题',
    knowledge: '知识'
  }
  return map[type] || type || '任务'
}

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

const loadAbilityProfile = async () => {
  try {
    const response = await fetchAbilityProfileApi()
    abilityProfile.value = response.data
    if (response.data.suggestedFocus) {
      form.value.direction = response.data.suggestedFocus
    }
  } catch {
    // Keep defaults when profile is unavailable.
  }
}

onMounted(() => {
  void loadCurrentPlan()
  void loadAbilityProfile()
})
</script>

<style scoped>
.plan-orbit :deep(.el-input-number),
.plan-orbit :deep(.el-select) {
  width: 100%;
}

.orbit-day {
  position: relative;
  padding-left: 22px;
}

.orbit-day::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 28px;
  bottom: -20px;
  width: 1px;
  background: linear-gradient(180deg, var(--bc-accent), transparent);
  opacity: 0.42;
}

.orbit-task {
  position: relative;
  display: flex;
  min-height: 64px;
  align-items: flex-start;
  gap: 14px;
  border: 1px solid var(--bc-line);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.35);
  padding: 14px;
  transition:
    border-color var(--motion-base) var(--ease-hard),
    background-color var(--motion-base) var(--ease-hard),
    transform var(--motion-base) var(--ease-hard);
}

.dark .orbit-task {
  background: rgba(255, 255, 255, 0.05);
}

.orbit-task:hover {
  border-color: var(--bc-line-hot);
  transform: translateY(-1px);
}

.orbit-task.is-done {
  opacity: 0.76;
}

.orbit-check {
  display: inline-flex;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--bc-line);
  border-radius: var(--radius-pill);
  color: #101826;
  transition:
    background-color var(--motion-fast) var(--ease-hard),
    border-color var(--motion-fast) var(--ease-hard),
    transform var(--motion-fast) var(--ease-hard);
}

.orbit-check:hover {
  border-color: var(--bc-line-hot);
  transform: scale(1.04);
}

.orbit-check.is-done {
  border-color: var(--bc-accent);
  background: var(--bc-accent);
}

@media (prefers-reduced-motion: reduce) {
  .orbit-task,
  .orbit-check {
    transition-duration: 0.01ms;
  }

  .orbit-task:hover,
  .orbit-check:hover {
    transform: none;
  }
}
</style>
