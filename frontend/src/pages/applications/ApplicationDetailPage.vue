<template>
  <div class="space-y-6">
    <div>
      <button type="button" class="flex items-center gap-1 text-sm text-secondary transition hover:text-accent" @click="router.back()">
        <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
        </svg>
        返回投递看板
      </button>
    </div>

    <section v-if="!detail" class="shell-section-card p-8 text-center">
      <p class="text-lg font-semibold text-ink">投递记录未找到</p>
      <p class="mt-3 text-sm leading-7 text-secondary">这条投递记录暂时无法查看。请返回投递管理重试。</p>
      <RouterLink :to="applicationBoardLink" class="hard-button-primary mt-4 inline-flex">返回看板</RouterLink>
    </section>

    <template v-else>
      <section class="shell-section-card application-detail-hero p-5 sm:p-6">
        <div class="grid gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(0,0.65fr)] xl:items-start">
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <span class="hard-chip">{{ statusLabel(detail.status) }}</span>
              <span class="detail-pill">{{ detail.company }}</span>
              <span class="detail-pill">{{ detail.city || '城市待补充' }}</span>
              <span v-if="seededFocusLabel" class="detail-pill">{{ seededFocusLabel }}</span>
              <span v-if="detail.hasStrategyDraft" class="detail-pill">Agent 策略草案待消费</span>
            </div>
            <h2 class="mt-5 text-3xl font-semibold tracking-[-0.04em] text-ink">{{ detail.jobTitle }}</h2>
            <p class="mt-4 max-w-3xl text-sm leading-7 text-secondary">
              {{ seededWorkspaceSummary || detail.nextStepSuggestion || detail.reviewSuggestion || detail.analysisSummary }}
            </p>

            <div class="mt-6 flex flex-wrap gap-3">
              <button type="button" class="hard-button-primary" @click="scrollToStatus">更新这条投递</button>
              <RouterLink :to="applicationJobPrepLink" class="hard-button-secondary">
                去 JD 备面
              </RouterLink>
              <RouterLink :to="applicationRecordingReviewLink" class="hard-button-secondary">
                去录音复盘
              </RouterLink>
              <RouterLink :to="applicationCopilotPrepLink" class="hard-button-secondary">
                去 Copilot Prep
              </RouterLink>
              <RouterLink :to="applicationAgentLink" class="hard-button-secondary">
                交给 Agent 推进
              </RouterLink>
              <button type="button" class="hard-button-secondary" @click="scrollToTimeline">查看时间线</button>
            </div>
          </div>

          <div class="grid gap-3 sm:grid-cols-3 xl:grid-cols-1">
            <article class="application-detail-metric">
              <span>匹配度</span>
              <strong>{{ Math.round(detail.matchScore || 0) }}</strong>
            </article>
            <article class="application-detail-metric">
              <span>绑定简历</span>
              <strong>{{ detail.resumeTitle || '未绑定' }}</strong>
            </article>
            <article class="application-detail-metric">
              <span>下一节点</span>
              <strong>{{ detail.nextStepDate || '待安排' }}</strong>
            </article>
          </div>
        </div>
      </section>

      <section id="application-next-step" class="shell-section-card application-primary-zone p-5 sm:p-6">
        <div class="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
          <div class="min-w-0 max-w-3xl">
            <div class="flex flex-wrap items-center gap-2">
              <span class="hard-chip">当前主任务</span>
              <span class="detail-pill">{{ statusLabel(detail.status) }}</span>
              <span class="detail-pill">下一节点 {{ detail.nextStepDate || '待安排' }}</span>
            </div>
            <h3 class="mt-5 text-2xl font-semibold tracking-[-0.03em] text-ink">处理这条岗位的下一步</h3>
            <p class="mt-3 text-sm leading-7 text-secondary">
              {{ detail.nextStepSuggestion || '更新当前阶段后，补充下一轮的准备重点。' }}
            </p>
          </div>

          <div class="flex shrink-0 flex-wrap gap-3">
            <RouterLink :to="applicationJobPrepLink" class="hard-button-secondary">
              结合当前岗位备面
            </RouterLink>
            <RouterLink :to="applicationRecordingReviewLink" class="hard-button-secondary">
              带去录音复盘
            </RouterLink>
            <RouterLink :to="applicationMockInterviewLink" class="hard-button-secondary">
              去模拟面试
            </RouterLink>
            <button type="button" class="hard-button-secondary" @click="scrollToTimeline">查看时间线</button>
          </div>
        </div>

        <article v-if="seededFocusCard" class="surface-card mt-5 p-4">
          <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">当前进入上下文</div>
          <h4 class="mt-2 text-lg font-semibold text-ink">{{ seededFocusCard.title }}</h4>
          <p class="mt-2 text-sm leading-7 text-secondary">{{ seededFocusCard.description }}</p>
        </article>

        <article v-if="detail.hasStrategyDraft" class="surface-card mt-5 p-4">
          <div class="flex flex-wrap items-center gap-2">
            <span class="hard-chip">Agent 投递策略草案</span>
            <span v-if="detail.strategyDraftUpdatedAt" class="detail-pill">{{ formatDateTime(detail.strategyDraftUpdatedAt) }}</span>
          </div>
          <p class="mt-3 text-sm leading-7 text-secondary">
            {{ detail.strategyDraftSummary || '最近一次 Agent 已经把这条岗位的推进策略写回。' }}
          </p>
          <div class="mt-4 flex flex-wrap gap-2">
            <RouterLink :to="applicationJobPrepLink" class="hard-button-secondary">带去 JD 备面</RouterLink>
            <RouterLink :to="applicationRecordingReviewLink" class="hard-button-secondary">带去录音复盘</RouterLink>
            <RouterLink :to="applicationCopilotPrepLink" class="hard-button-secondary">带去 Copilot Prep</RouterLink>
            <RouterLink :to="applicationAgentLink" class="hard-button-secondary">回到 Agent 继续收口</RouterLink>
          </div>
          <ul v-if="detail.strategyDraftActions?.length" class="mt-4 space-y-2 text-sm leading-6 text-primary">
            <li v-for="item in detail.strategyDraftActions" :key="item">• {{ item }}</li>
          </ul>
        </article>

        <div class="mt-5 grid gap-4 xl:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
          <div class="space-y-4">
            <div class="surface-card p-4">
              <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">当前建议</div>
              <p class="mt-2 text-sm leading-7 text-secondary">{{ detail.nextStepSuggestion || '更新当前阶段信息。' }}</p>
            </div>

            <div class="surface-card p-4">
              <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">复盘重点</div>
              <p class="mt-2 text-sm leading-7 text-secondary">{{ detail.reviewSuggestion || '记录这轮反馈，补充下一轮准备重点。' }}</p>
            </div>

            <div class="grid gap-4 md:grid-cols-2">
              <div class="surface-card p-4">
                <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">已命中关键词</div>
                <div class="mt-3 flex flex-wrap gap-2">
                  <span v-for="tag in detail.jdKeywords" :key="`match-${tag}`" class="rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700">
                    {{ tag }}
                  </span>
                  <span v-if="!detail.jdKeywords.length" class="text-sm text-secondary">
                    {{ EMPTY_STATE_COPY.applicationDetailKeywords.title }}，{{ EMPTY_STATE_COPY.applicationDetailKeywords.description }}
                  </span>
                </div>
              </div>
              <div class="surface-card p-4">
                <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">待补关键词</div>
                <div class="mt-3 flex flex-wrap gap-2">
                  <span v-for="tag in detail.missingKeywords" :key="`missing-${tag}`" class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral">
                    {{ tag }}
                  </span>
                  <span v-if="!detail.missingKeywords.length" class="text-sm text-secondary">当前没有明显缺口</span>
                </div>
              </div>
            </div>
          </div>

          <article id="application-status" class="application-primary-zone__form">
            <div class="flex items-center justify-between gap-3">
              <div>
                <h4 class="text-xl font-semibold tracking-[-0.03em] text-ink">更新推进阶段</h4>
                <p class="mt-2 text-sm leading-7 text-secondary">
                  补充阶段、下一节点和当前说明后，记录更细的反馈。
                </p>
              </div>
              <el-button :loading="refreshingAnalysis" size="large" class="hard-button-secondary" @click="handleRefreshAnalysis">
                刷新 JD 分析
              </el-button>
            </div>

            <div class="mt-5 grid gap-4">
              <div class="grid gap-4 md:grid-cols-2">
                <div class="data-slab p-4">
                  <div class="text-xs uppercase tracking-[0.22em] text-tertiary">当前阶段</div>
                  <el-select v-model="statusForm.status" class="mt-2 w-full" size="large">
                    <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </div>
                <div class="data-slab p-4">
                  <div class="text-xs uppercase tracking-[0.22em] text-tertiary">下一节点日期</div>
                  <el-date-picker v-model="statusForm.nextStepDate" class="mt-2 w-full" type="date" value-format="YYYY-MM-DD" placeholder="例如：下周三" />
                </div>
              </div>

              <div class="data-slab p-4">
                <div class="text-xs uppercase tracking-[0.22em] text-tertiary">推进说明</div>
                <el-input
                  v-model="statusForm.note"
                  class="mt-2"
                  type="textarea"
                  :rows="4"
                  placeholder="例如：约了下周三一面，需要补 Redis 和缓存一致性"
                />
              </div>
            </div>

            <el-button :loading="updatingStatus" size="large" class="action-button mt-4 w-full" @click="handleUpdateStatus">
              保存当前阶段
            </el-button>
          </article>
        </div>
      </section>

      <section class="grid gap-4 xl:grid-cols-[minmax(0,1.08fr)_minmax(0,0.92fr)]">
        <article id="application-timeline" class="shell-section-card p-5 sm:p-6">
          <div class="flex items-start justify-between gap-3">
            <div>
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">回看时间线</h3>
              <p class="mt-2 text-sm leading-7 text-secondary">
                确认这条岗位的历史进展后，补充新的反馈或阶段变化。
              </p>
            </div>
          </div>

          <div class="mt-5 space-y-4">
            <article
              v-for="event in detail.events"
              :key="event.id"
              class="timeline-card"
            >
              <div class="timeline-dot"></div>
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">
                      {{ eventTypeLabel(event.eventType) }}
                      <span v-if="event.interviewRound" class="ml-2 text-accent">第 {{ event.interviewRound }} 轮</span>
                    </div>
                    <h4 class="mt-2 text-lg font-semibold text-ink">{{ event.title }}</h4>
                  </div>
                  <span class="text-xs text-tertiary">{{ formatDateTime(event.eventTime) }}</span>
                </div>

                <p v-if="event.content" class="mt-3 text-sm leading-7 text-secondary">{{ event.content }}</p>

                <div class="mt-3 flex flex-wrap gap-2">
                  <span v-if="event.interviewer" class="detail-pill">面试官：{{ event.interviewer }}</span>
                  <span v-if="event.result" class="detail-pill">结果：{{ event.result }}</span>
                  <span
                    v-for="tag in event.feedbackTags || []"
                    :key="`${event.id}-${tag}`"
                    class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral"
                  >
                    {{ tag }}
                  </span>
                </div>
              </div>
            </article>

            <div v-if="!detail.events.length" class="rounded-2xl border border-dashed border-[var(--bc-line)] p-5 text-sm text-secondary">
              {{ EMPTY_STATE_COPY.applicationDetailTimeline.title }}，{{ EMPTY_STATE_COPY.applicationDetailTimeline.description }}
            </div>
          </div>
        </article>

        <div class="space-y-4">
          <article class="shell-section-card application-secondary-zone p-5 sm:p-6">
            <div class="flex flex-wrap items-center gap-2">
              <span class="detail-pill">次级动作</span>
            </div>
            <h3 class="mt-4 text-2xl font-semibold tracking-[-0.03em] text-ink">补一条反馈记录</h3>
            <p class="mt-2 text-sm leading-7 text-secondary">
              在当前阶段和下一步明确后，把面试、作业和复盘补进时间线。
            </p>

            <div class="mt-5 space-y-4">
              <div class="data-slab p-4">
                <div class="text-xs uppercase tracking-[0.22em] text-tertiary">记录类型</div>
                <el-select v-model="eventForm.eventType" class="mt-2 w-full" size="large">
                  <el-option label="面试反馈" value="interview" />
                  <el-option label="复盘记录" value="review" />
                  <el-option label="状态备注" value="note" />
                </el-select>
              </div>

              <div v-if="eventForm.eventType === 'interview'" class="grid gap-4 md:grid-cols-2">
                <div class="data-slab p-4">
                  <div class="text-xs uppercase tracking-[0.22em] text-tertiary">面试轮次</div>
                  <el-input-number v-model="eventForm.interviewRound" class="mt-2 !w-full" :min="1" :step="1" />
                </div>
                <div class="data-slab p-4">
                  <div class="text-xs uppercase tracking-[0.22em] text-tertiary">面试官</div>
                  <el-input v-model="eventForm.interviewer" class="mt-2" size="large" placeholder="例如：后端组负责人" />
                </div>
              </div>

              <div class="data-slab p-4">
                <div class="text-xs uppercase tracking-[0.22em] text-tertiary">标题</div>
                <el-input v-model="eventForm.title" class="mt-2" size="large" placeholder="例如：一面结束，需要补缓存一致性" />
              </div>

              <div class="data-slab p-4">
                <div class="text-xs uppercase tracking-[0.22em] text-tertiary">记录内容</div>
                <el-input
                  v-model="eventForm.content"
                  class="mt-2"
                  type="textarea"
                  :rows="5"
                  placeholder="记录真实问题、表现、反馈和下一轮准备重点"
                />
              </div>

              <div class="grid gap-4 md:grid-cols-2">
                <div class="data-slab p-4">
                  <div class="text-xs uppercase tracking-[0.22em] text-tertiary">结果</div>
                  <el-input v-model="eventForm.result" class="mt-2" size="large" placeholder="例如：通过 / 待反馈 / 挂在项目深挖" />
                </div>
                <div class="data-slab p-4">
                  <div class="text-xs uppercase tracking-[0.22em] text-tertiary">反馈标签</div>
                  <el-input
                    v-model="eventForm.feedbackTagsText"
                    class="mt-2"
                    size="large"
                    placeholder="用逗号分隔，例如：项目深挖, 并发, 表达不稳"
                  />
                </div>
              </div>

              <el-button :loading="addingEvent" size="large" class="action-button w-full" @click="handleAddEvent">
                记录这次反馈
              </el-button>
            </div>
          </article>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter, type LocationQueryRaw, type RouteLocationRaw } from 'vue-router'
import {
  addApplicationEventApi,
  fetchApplicationDetailApi,
  refreshApplicationAnalysisApi,
  updateApplicationStatusApi
} from '@/api/applications'
import { EMPTY_STATE_COPY, ERROR_COPY } from '@/constants/productCopy'
import type { JobApplicationDetail } from '@/types/api'
import { buildAgentWorkbenchLocation } from '@/utils/agent'

const route = useRoute()
const router = useRouter()

const detail = ref<JobApplicationDetail | null>(null)
const updatingStatus = ref(false)
const addingEvent = ref(false)
const refreshingAnalysis = ref(false)

const statusOptions = [
  { label: '待投递', value: 'saved' },
  { label: '已投递', value: 'applied' },
  { label: '笔试 / 作业', value: 'written' },
  { label: '面试中', value: 'interview' },
  { label: 'Offer', value: 'offer' },
  { label: '已淘汰', value: 'rejected' }
]

const statusForm = reactive({
  status: 'saved',
  note: '',
  nextStepDate: ''
})

const eventForm = reactive({
  eventType: 'review',
  title: '',
  content: '',
  result: '',
  interviewRound: 1,
  interviewer: '',
  feedbackTagsText: ''
})

const readSeedQueryValue = (key: 'seedTopic' | 'seedWorkflow' | 'seedNote') => {
  const value = route.query[key]
  return typeof value === 'string' ? value.trim() : ''
}

const seededTopic = computed(() => readSeedQueryValue('seedTopic'))
const seededWorkflow = computed(() => readSeedQueryValue('seedWorkflow'))
const seededNote = computed(() => readSeedQueryValue('seedNote'))

const seededFocusLabel = computed(() => {
  if (!seededTopic.value) return ''
  return seededWorkflow.value === 'applications' ? `${seededTopic.value} 投递验证` : `${seededTopic.value} 定向上下文`
})

const seededWorkspaceSummary = computed(() => {
  if (seededNote.value) return seededNote.value
  if (!seededTopic.value) return ''
  return `当前从其他工作区带入「${seededTopic.value}」上下文，优先把它压到真实岗位推进、备面和反馈验证里。`
})

const seededFocusCard = computed(() => {
  if (!seededTopic.value && !seededNote.value) return null
  return {
    title: seededTopic.value ? `围绕 ${seededTopic.value} 推进这条岗位` : '沿着当前上下文继续推进岗位动作',
    description: seededWorkspaceSummary.value || '优先把当前薄弱点落到这条岗位的备面、反馈和后续动作里。'
  }
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

const applicationBoardLink = computed(() => {
  const query = appendSeedQuery(new URLSearchParams())
  return query.toString() ? `/applications?${query.toString()}` : '/applications'
})

const statusLabel = (value: string) => statusOptions.find((item) => item.value === value)?.label || value

const eventTypeLabel = (value: string) => {
  switch (value) {
    case 'interview':
      return '面试反馈'
    case 'review':
      return '复盘记录'
    case 'status_change':
      return '阶段变更'
    case 'analysis':
      return 'JD 分析'
    case 'strategy':
      return 'Agent 策略草案'
    default:
      return '状态备注'
  }
}

const formatDateTime = (value?: string) => (value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '刚刚')
const applicationId = () => String(route.params.id || '')
const buildInterviewWorkspaceLink = (workspace: 'job-prep' | 'copilot-prep' | 'mock-interview' | 'recording-review') => {
  const query = appendSeedQuery(new URLSearchParams({ workspace }))
  const id = applicationId()
  if (id) {
    query.set('applicationId', id)
  }
  if (detail.value?.resumeFileId) {
    query.set('resumeId', String(detail.value.resumeFileId))
  }
  return `/interview?${query.toString()}`
}
const applicationAgentLink = computed(() => {
  const id = applicationId()
  const contextRefs = ['analytics:profile', 'resume:latest']
  if (id) {
    contextRefs.unshift(`application:${id}`)
  } else {
    contextRefs.unshift('application:board')
  }
  return buildSeededAgentWorkbenchLocation({
    agentType: 'application_strategist',
    triggerSource: 'applications',
    contextRefs,
    userPrompt: detail.value?.jobTitle
      ? seededTopic.value
        ? `围绕 ${detail.value.company} 的 ${detail.value.jobTitle} 岗位，重点验证「${seededTopic.value}」，整理下一步推进策略。`
        : `围绕 ${detail.value.company} 的 ${detail.value.jobTitle} 岗位，整理下一步推进策略。`
      : seededTopic.value
        ? `结合当前投递进展、JD 分析和历史反馈，重点围绕「${seededTopic.value}」整理下一步推进策略。`
        : '结合当前投递进展、JD 分析和历史反馈，整理下一步推进策略。'
  })
})
const applicationJobPrepLink = computed(() => buildInterviewWorkspaceLink('job-prep'))
const applicationRecordingReviewLink = computed(() => buildInterviewWorkspaceLink('recording-review'))
const applicationCopilotPrepLink = computed(() => buildInterviewWorkspaceLink('copilot-prep'))
const applicationMockInterviewLink = computed(() => buildInterviewWorkspaceLink('mock-interview'))

const loadData = async () => {
  const id = applicationId()
  if (!id) return
  const response = await fetchApplicationDetailApi(id)
  detail.value = response.data
  statusForm.status = response.data.status
  statusForm.nextStepDate = response.data.nextStepDate || ''
}

const handleUpdateStatus = async () => {
  const id = applicationId()
  if (!id) return
  updatingStatus.value = true
  try {
    const response = await updateApplicationStatusApi(id, {
      status: statusForm.status,
      note: statusForm.note || undefined,
      nextStepDate: statusForm.nextStepDate || undefined
    })
    detail.value = response.data
    statusForm.note = ''
    ElMessage.success('当前阶段已更新')
  } catch (error: any) {
    ElMessage.error(error?.message || ERROR_COPY.applicationStatusUpdateFailed)
  } finally {
    updatingStatus.value = false
  }
}

const parseFeedbackTags = () =>
  eventForm.feedbackTagsText
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)

const handleAddEvent = async () => {
  const id = applicationId()
  if (!id || !eventForm.title.trim()) {
    ElMessage.warning('请填写记录标题')
    return
  }
  addingEvent.value = true
  try {
    const response = await addApplicationEventApi(id, {
      eventType: eventForm.eventType,
      title: eventForm.title.trim(),
      content: eventForm.content.trim() || undefined,
      result: eventForm.result.trim() || undefined,
      interviewRound: eventForm.eventType === 'interview' ? eventForm.interviewRound : undefined,
      interviewer: eventForm.eventType === 'interview' ? eventForm.interviewer.trim() || undefined : undefined,
      feedbackTags: parseFeedbackTags()
    })
    detail.value = response.data
    eventForm.title = ''
    eventForm.content = ''
    eventForm.result = ''
    eventForm.interviewRound = 1
    eventForm.interviewer = ''
    eventForm.feedbackTagsText = ''
    ElMessage.success('反馈已记录')
  } catch (error: any) {
    ElMessage.error(error?.message || ERROR_COPY.applicationEventCreateFailed)
  } finally {
    addingEvent.value = false
  }
}

const handleRefreshAnalysis = async () => {
  const id = applicationId()
  if (!id) return
  refreshingAnalysis.value = true
  try {
    const response = await refreshApplicationAnalysisApi(id)
    detail.value = response.data
    ElMessage.success('JD 分析已刷新')
  } catch (error: any) {
    ElMessage.error(error?.message || ERROR_COPY.applicationAnalysisRefreshFailed)
  } finally {
    refreshingAnalysis.value = false
  }
}

const scrollToStatus = () => {
  document.getElementById('application-status')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const scrollToTimeline = () => {
  document.getElementById('application-timeline')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped>
.application-detail-hero {
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.11), transparent 28%),
    radial-gradient(circle at 88% 20%, rgba(var(--bc-cyan-rgb), 0.11), transparent 20%),
    var(--bc-surface-card);
}

.application-detail-metric {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.38);
  padding: 0.95rem 1rem;
  backdrop-filter: blur(10px);
}

.application-detail-metric span {
  display: block;
  font-size: 0.8rem;
  color: var(--bc-ink-secondary);
}

.application-detail-metric strong {
  display: block;
  margin-top: 0.45rem;
  font-size: 1.15rem;
  line-height: 1.25;
  color: var(--bc-ink);
}

.application-primary-zone {
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.08), transparent 30%),
    var(--bc-surface-card);
}

.application-primary-zone__form {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.42);
  padding: 1rem;
  backdrop-filter: blur(10px);
}

.application-secondary-zone {
  border: 1px solid var(--bc-border-subtle);
  background:
    linear-gradient(180deg, rgba(var(--bc-ink-rgb), 0.015), transparent 40%),
    var(--bc-surface-card);
}

.timeline-card {
  display: flex;
  gap: 1rem;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 1rem 1.05rem;
}

.timeline-dot {
  width: 0.8rem;
  height: 0.8rem;
  margin-top: 0.4rem;
  border-radius: 999px;
  background: var(--bc-accent);
  box-shadow: 0 0 0 6px rgba(var(--bc-accent-rgb), 0.12);
  flex-shrink: 0;
}
</style>
