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
      <RouterLink to="/applications" class="hard-button-primary mt-4 inline-flex">返回看板</RouterLink>
    </section>

    <template v-else>
      <section class="shell-section-card application-detail-hero p-5 sm:p-6">
        <div class="grid gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(0,0.65fr)] xl:items-start">
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <span class="hard-chip">{{ statusLabel(detail.status) }}</span>
              <span class="detail-pill">{{ detail.company }}</span>
              <span class="detail-pill">{{ detail.city || '城市待补充' }}</span>
            </div>
            <h2 class="mt-5 text-3xl font-semibold tracking-[-0.04em] text-ink">{{ detail.jobTitle }}</h2>
            <p class="mt-4 max-w-3xl text-sm leading-7 text-secondary">
              {{ detail.nextStepSuggestion || detail.reviewSuggestion || detail.analysisSummary }}
            </p>

            <div class="mt-6 flex flex-wrap gap-3">
              <button type="button" class="hard-button-primary" @click="scrollToStatus">继续推进这条投递</button>
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

      <section class="grid gap-4 xl:grid-cols-[minmax(0,1.08fr)_minmax(0,0.92fr)]">
        <article id="application-timeline" class="shell-section-card p-5 sm:p-6">
          <div class="flex items-start justify-between gap-3">
            <div>
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">1. 看时间线</h3>
              <p class="mt-2 text-sm leading-7 text-secondary">
                先看这条投递已经走到哪里，再决定下一步推进什么。
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
              还没有过程记录，先补一条当前状态或面试反馈。
            </div>
          </div>
        </article>

        <div class="space-y-4">
          <article class="shell-section-card p-5 sm:p-6">
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">2. 看下一步</h3>
            <div class="mt-5 space-y-4">
              <div class="surface-card p-4">
                <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">当前建议</div>
                <p class="mt-2 text-sm leading-7 text-secondary">{{ detail.nextStepSuggestion || '先推进当前阶段' }}</p>
              </div>
              <div class="surface-card p-4">
                <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">复盘重点</div>
                <p class="mt-2 text-sm leading-7 text-secondary">{{ detail.reviewSuggestion || '先记录这轮反馈，再补下一轮准备重点。' }}</p>
              </div>
            </div>
          </article>

          <article id="application-status" class="shell-section-card p-5 sm:p-6">
            <div class="flex items-center justify-between gap-3">
              <div>
                <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">3. 推进阶段</h3>
                <p class="mt-2 text-sm leading-7 text-secondary">
                  先更新当前阶段，再决定要不要补新的反馈记录。
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

          <article class="shell-section-card p-5 sm:p-6">
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">4. 记录反馈</h3>
            <p class="mt-2 text-sm leading-7 text-secondary">
              面试、作业和复盘都记在这里，让时间线真正可回看。
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

          <article class="shell-section-card p-5 sm:p-6">
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">5. 补准备重点</h3>
            <div class="mt-5 grid gap-4">
              <div class="surface-card p-4">
                <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">已命中关键词</div>
                <div class="mt-3 flex flex-wrap gap-2">
                  <span v-for="tag in detail.jdKeywords" :key="`match-${tag}`" class="rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700">
                    {{ tag }}
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
          </article>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addApplicationEventApi,
  fetchApplicationDetailApi,
  refreshApplicationAnalysisApi,
  updateApplicationStatusApi
} from '@/api/applications'
import type { JobApplicationDetail } from '@/types/api'

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
    default:
      return '状态备注'
  }
}

const formatDateTime = (value?: string) => (value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '刚刚')
const applicationId = () => String(route.params.id || '')

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
    ElMessage.error(error?.message || '状态更新失败')
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
    ElMessage.error(error?.message || '记录反馈失败')
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
    ElMessage.error(error?.message || '刷新分析失败')
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
