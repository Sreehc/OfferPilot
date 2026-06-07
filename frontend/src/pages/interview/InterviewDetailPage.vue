<template>
  <div class="space-y-6">
    <div>
      <button
        type="button"
        class="flex items-center gap-1 text-sm text-secondary transition hover:text-accent"
        @click="router.back()"
      >
        <UiIcon name="back" />
        返回模拟面试
      </button>
    </div>

    <section v-if="loading" class="shell-section-card p-6">
      <StateView variant="loading" :rows="6" />
    </section>

    <section v-else-if="!detail" class="shell-section-card p-6">
      <StateView
        icon="interview"
        title="面试记录未找到"
        description="这次面试记录暂时无法查看。请返回模拟面试重试。"
      >
        <template #action>
          <RouterLink :to="mockInterviewWorkspaceLink" class="hard-button-primary">返回模拟面试</RouterLink>
        </template>
      </StateView>
    </section>

    <template v-else>
      <section class="shell-section-card p-6">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div class="mt-3 flex flex-wrap items-center gap-3">
              <h3 class="text-2xl font-semibold text-ink" style="text-wrap: balance">
                {{ detail.direction }} 方向面试诊断
              </h3>
              <span v-if="seededFocusLabel" class="detail-pill">{{ seededFocusLabel }}</span>
            </div>
            <div class="mt-2 flex flex-wrap items-center gap-4 text-sm text-secondary">
              <span>{{ detail.mode === 'voice' ? '语音面试' : '文本面试' }}</span>
              <span>{{ detail.questionCount }} 题</span>
              <span>{{ detail.durationMinutes || 20 }} 分钟</span>
              <span>{{ interviewContextLabel(detail.contextSource, detail.includeResumeProject) }}</span>
              <span v-if="detail.startTime">{{ formatTime(detail.startTime) }}</span>
              <span v-if="detail.endTime">~ {{ formatTime(detail.endTime) }}</span>
            </div>
            <p v-if="seededWorkspaceSummary" class="mt-3 text-sm leading-7 text-secondary">
              {{ seededWorkspaceSummary }}
            </p>
          </div>
          <div class="flex flex-col gap-3 lg:items-end">
            <div class="interview-score-card p-6 text-white">
              <div class="text-xs uppercase tracking-[0.24em] text-white/60">总分</div>
              <div class="mt-2 text-5xl font-semibold" style="font-variant-numeric: tabular-nums">
                {{ formatScore(detail.totalScore) }}
              </div>
            </div>
            <div class="flex flex-wrap gap-3">
              <RouterLink :to="interviewReviewAgentLink" class="hard-button-secondary inline-flex items-center justify-center px-4">
                交给 Agent 复盘
              </RouterLink>
              <RouterLink :to="interviewPlanRefreshLink" class="hard-button-secondary inline-flex items-center justify-center px-4">
                按这场面试刷新计划
              </RouterLink>
              <RouterLink :to="interviewRecordingReviewLink" class="hard-button-secondary inline-flex items-center justify-center px-4">
                去录音复盘
              </RouterLink>
              <RouterLink :to="interviewWrongFocusLink" class="hard-button-secondary inline-flex items-center justify-center px-4">
                {{ hasInterviewWrongFocusLink ? '查看低分题' : '查看错题本' }}
              </RouterLink>
            </div>
          </div>
        </div>
      </section>

      <MetricStrip :items="interviewDetailMetrics" />

      <section
        v-for="(record, index) in sortedRecords"
        :key="record.questionId"
        class="shell-section-card overflow-hidden"
      >
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200/60 px-6 py-4 dark:border-slate-700/60">
          <div class="flex min-w-0 items-center gap-3">
            <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-accent text-sm font-bold text-white">
              {{ index + 1 }}
            </span>
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <span class="font-semibold text-ink">{{ record.questionTitle }}</span>
                <span
                  v-if="record.isLowScore"
                  class="inline-flex items-center rounded-full bg-coral/10 px-2 py-0.5 text-xs font-semibold text-coral"
                >
                  低分题
                </span>
              </div>
            </div>
          </div>
          <span
            class="text-2xl font-semibold"
            style="font-variant-numeric: tabular-nums"
            :class="record.score >= 60 ? 'text-accent' : 'text-red-500'"
          >
            {{ formatScore(record.score) }}
          </span>
        </div>

        <div class="surface-muted mx-6 mt-4 rounded-lg p-4">
          <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">本题点评</div>
          <p class="mt-1 text-sm leading-6 text-primary">{{ record.comment || '这道题暂时没有点评。可以对照分数和标准答案复盘。' }}</p>
        </div>

        <div v-if="record.scoreBreakdown?.length" class="mx-6 mt-4 grid gap-3 md:grid-cols-3">
          <div
            v-for="item in record.scoreBreakdown"
            :key="`${record.questionId}-${item.dimension}`"
            class="surface-card p-4"
          >
            <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">{{ item.dimension }}</div>
            <div class="mt-2 text-3xl font-semibold text-ink" style="font-variant-numeric: tabular-nums">{{ item.score }}</div>
            <p class="mt-2 text-sm leading-6 text-secondary">{{ item.summary }}</p>
          </div>
        </div>

        <div v-if="record.weakPointTags?.length || record.reviewSummary" class="mx-6 mt-4 grid gap-4 md:grid-cols-2">
          <div v-if="record.weakPointTags?.length" class="surface-card p-4">
            <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">薄弱点标签</div>
            <div class="mt-3 flex flex-wrap gap-2">
              <span
                v-for="tag in record.weakPointTags"
                :key="`${record.questionId}-${tag}`"
                class="inline-flex items-center rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral"
              >
                {{ tag }}
              </span>
            </div>
          </div>
          <div v-if="record.reviewSummary" class="surface-card p-4">
            <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">复盘建议</div>
            <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-primary">{{ record.reviewSummary }}</p>
          </div>
        </div>

        <div v-if="record.voiceTranscript" class="mx-6 mt-4 surface-card p-4">
          <div class="flex items-center gap-2">
            <div class="h-2 w-2 rounded-full bg-purple-400"></div>
            <span class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">语音转录</span>
            <span v-if="record.voiceConfidence" class="ml-auto text-xs text-tertiary">
              置信度 {{ Math.round(record.voiceConfidence * 100) }}%
            </span>
          </div>
          <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-primary">
            {{ record.voiceTranscript }}
          </p>
        </div>

        <div class="mt-4 grid gap-4 px-6 pb-5 md:grid-cols-2">
          <div class="surface-card p-4">
            <div class="flex items-center gap-2">
              <div class="h-2 w-2 rounded-full bg-amber-400"></div>
              <span class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">
                {{ record.voiceTranscript ? '转录文本' : '我的回答' }}
              </span>
            </div>
            <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-primary">
              {{ record.userAnswer || '未作答' }}
            </p>
          </div>

          <div class="surface-card p-4">
            <div class="flex items-center gap-2">
              <div class="h-2 w-2 rounded-full bg-green-400"></div>
              <span class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">标准答案</span>
            </div>
            <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-primary">
              {{ record.standardAnswer || '这道题暂时没有标准答案。可以回看你的回答，并补充更完整的版本。' }}
            </p>
          </div>
        </div>

        <div v-if="record.followUp" class="border-t border-slate-200/60 px-6 py-4 dark:border-slate-700/60">
          <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">追问</div>
          <p class="mt-1 text-sm leading-6 text-secondary">{{ record.followUp }}</p>
        </div>
      </section>

      <section class="flex gap-3">
        <RouterLink :to="mockInterviewWorkspaceLink" class="hard-button-secondary flex-1 text-center">
          返回模拟面试
        </RouterLink>
        <RouterLink :to="restartInterviewLink" class="hard-button-primary flex-1 text-center">
          再做一场同方向模拟
        </RouterLink>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter, type LocationQueryRaw, type RouteLocationRaw } from 'vue-router'
import { interviewDetailApi } from '@/api/interview'
import { ERROR_COPY } from '@/constants/productCopy'
import type { InterviewDetail } from '@/types/api'
import { MetricStrip, StateView, UiIcon } from '@/components/ui'
import { buildAgentWorkbenchLocation } from '@/utils/agent'

const route = useRoute()
const router = useRouter()

const detail = ref<InterviewDetail | null>(null)
const loading = ref(true)
const sortedRecords = computed(() => {
  if (!detail.value?.records) return []
  return [...detail.value.records].sort((a, b) => Number(b.isLowScore) - Number(a.isLowScore))
})
const firstWrongRecord = computed(() => {
  return sortedRecords.value.find((item) => item.wrongQuestionId) || null
})
const interviewDetailMetrics = computed(() => {
  if (!detail.value) return []
  return [
    {
      label: '目标岗位',
      value: detail.value.jobRole || '未设置',
      hint: experienceLabel(detail.value.experienceLevel)
    },
    {
      label: '技术范围',
      value: detail.value.techStack || '未限定',
      hint: detail.value.direction,
      tone: 'accent' as const
    },
    {
      label: '面试策略',
      value: detail.value.includeResumeProject ? '结合项目' : '基础表达',
      hint: detail.value.includeResumeProject ? '项目表达与工程权衡' : '基础原理与结构化表达'
    },
    {
      label: '低分题',
      value: sortedRecords.value.filter((item) => item.isLowScore).length,
      hint: '优先复盘',
      tone: sortedRecords.value.some((item) => item.isLowScore) ? 'warning' as const : 'success' as const
    }
  ]
})

const formatScore = (score: number | undefined | null): string => {
  if (score == null) return '-'
  return Number.isInteger(score) ? String(score) : Number(score).toFixed(2)
}

const formatTime = (time: string): string => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const experienceLabel = (value?: string) => {
  switch (value) {
    case 'intern':
      return '在校 / 实习准备'
    case 'junior':
      return '0-1 年'
    case 'mid':
      return '1-3 年'
    case 'senior':
      return '3 年以上'
    default:
      return '未设置'
  }
}

const interviewContextLabel = (context?: InterviewDetail['contextSource'] | null, includeResumeProject?: boolean) => {
  if (context?.type === 'project') return '结合项目'
  if (context?.type === 'resume') return '结合简历'
  if (includeResumeProject) return '结合项目'
  return '不带简历'
}

const readSeedQueryValue = (key: 'seedTopic' | 'seedWorkflow' | 'seedNote') => {
  const value = route.query[key]
  return typeof value === 'string' ? value.trim() : ''
}

const seededTopic = computed(() => readSeedQueryValue('seedTopic'))
const seededWorkflow = computed(() => readSeedQueryValue('seedWorkflow'))
const seededNote = computed(() => readSeedQueryValue('seedNote'))
const seededFocusLabel = computed(() => {
  if (!seededTopic.value) return ''
  return seededWorkflow.value === 'mock-interview' ? `${seededTopic.value} 定向模拟` : `${seededTopic.value} 定向上下文`
})
const seededWorkspaceSummary = computed(() => {
  if (seededNote.value) return seededNote.value
  if (!seededTopic.value) return ''
  return `当前沿着「${seededTopic.value}」上下文查看这场面试，后续复盘、重练和录音复盘会继续保留这个主题。`
})

const appendSeedQuery = (query: URLSearchParams) => {
  if (seededTopic.value) {
    query.set('seedTopic', seededTopic.value)
  }
  if (seededWorkflow.value) {
    query.set('seedWorkflow', seededWorkflow.value)
  }
  if (seededNote.value) {
    query.set('seedNote', seededNote.value)
  }
  return query
}

const buildSeededAgentWorkbenchLocation = (
  prefill: Parameters<typeof buildAgentWorkbenchLocation>[0]
): RouteLocationRaw => {
  const location = buildAgentWorkbenchLocation(prefill) as {
    path: string
    query?: LocationQueryRaw
  }
  return {
    path: location.path,
    query: {
      ...(location.query || {}),
      ...(seededTopic.value ? { seedTopic: seededTopic.value } : {}),
      ...(seededWorkflow.value ? { seedWorkflow: seededWorkflow.value } : {}),
      ...(seededNote.value ? { seedNote: seededNote.value } : {})
    }
  }
}

const sessionId = () => String(route.params.id || '')
const buildInterviewWorkspaceLink = (workspace: 'mock-interview' | 'recording-review') => {
  const query = appendSeedQuery(new URLSearchParams({ workspace }))
  return `/interview?${query.toString()}`
}
const buildSeededPath = (path: string, query: Record<string, string> = {}) => {
  const nextQuery = appendSeedQuery(new URLSearchParams(query))
  return nextQuery.toString() ? `${path}?${nextQuery.toString()}` : path
}
const mockInterviewWorkspaceLink = computed(() => buildInterviewWorkspaceLink('mock-interview'))
const interviewResumeId = computed(() => detail.value?.contextSource?.resumeId || '')
const restartInterviewLink = computed(() => {
  const query = appendSeedQuery(new URLSearchParams({ workspace: 'mock-interview' }))
  if (interviewResumeId.value) {
    query.set('resumeId', interviewResumeId.value)
  }
  return `/interview?${query.toString()}`
})
const interviewRecordingReviewLink = computed(() => buildInterviewWorkspaceLink('recording-review'))
const interviewWrongFocusId = computed(() => firstWrongRecord.value?.wrongQuestionId || '')
const hasInterviewWrongFocusLink = computed(() => Boolean(interviewWrongFocusId.value))
const interviewWrongFocusLink = computed(() => {
  return interviewWrongFocusId.value
    ? buildSeededPath('/wrong', { wrongId: interviewWrongFocusId.value })
    : buildSeededPath('/wrong')
})
const interviewReviewAgentLink = computed(() => {
  const id = sessionId()
  const contextRefs = ['analytics:profile']
  if (id) {
    contextRefs.unshift(`interview:session:${id}`)
  } else {
    contextRefs.unshift('interview:latest')
  }
  if (detail.value?.contextSource?.type === 'resume' || detail.value?.contextSource?.type === 'project') {
    contextRefs.push('resume:latest')
  }
  return buildSeededAgentWorkbenchLocation({
    agentType: 'interview_review',
    triggerSource: 'interview',
    contextRefs,
    userPrompt: detail.value?.direction
      ? `基于这次 ${detail.value.direction} 面试记录，总结薄弱点并安排下一轮训练。`
      : '基于这次模拟面试记录，总结薄弱点并安排下一轮训练。'
  })
})
const interviewPlanRefreshLink = computed(() => {
  const id = sessionId()
  const contextRefs = ['analytics:profile', 'study-plan:active']
  if (id) {
    contextRefs.unshift(`interview:session:${id}`)
  } else {
    contextRefs.unshift('interview:latest')
  }
  return buildSeededAgentWorkbenchLocation({
    agentType: 'study_planner',
    triggerSource: 'interview',
    contextRefs,
    userPrompt: detail.value?.direction
      ? `把这次 ${detail.value.direction} 面试暴露的问题刷新进下一轮训练计划。`
      : '把这次模拟面试暴露的问题刷新进下一轮训练计划。'
  })
})

const loadData = async () => {
  const id = sessionId()
  if (!id) {
    loading.value = false
    return
  }
  try {
    const response = await interviewDetailApi(id)
    detail.value = response.data
  } catch {
    ElMessage.error(ERROR_COPY.interviewDetailLoadFailed)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped>
.interview-score-card {
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 86% 18%, rgba(var(--bc-cyan-rgb), 0.28), transparent 34%),
    linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.94), rgba(75, 64, 49, 0.96));
  box-shadow: 0 16px 36px rgba(var(--bc-accent-rgb), 0.16);
}
</style>
