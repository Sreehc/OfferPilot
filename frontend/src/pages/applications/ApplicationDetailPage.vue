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
      <section class="shell-section-card p-6">
        <div class="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <span class="hard-chip">{{ statusLabel(detail.status) }}</span>
              <span class="detail-pill">{{ detail.company }}</span>
              <span class="detail-pill">{{ detail.city || '城市未填写' }}</span>
            </div>
            <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink">{{ detail.jobTitle }}</h2>
            <p class="mt-3 max-w-3xl text-sm leading-7 text-secondary">{{ detail.analysisSummary }}</p>
            <div class="mt-4 flex flex-wrap gap-2">
              <span v-for="tag in detail.jdKeywords" :key="tag" class="rounded-full bg-accent/10 px-3 py-1 text-xs font-semibold text-accent">
                {{ tag }}
              </span>
            </div>
          </div>

          <div class="application-score-card p-6 text-white">
            <div class="text-xs uppercase tracking-[0.24em] text-white/60">匹配度</div>
            <div class="mt-2 text-5xl font-semibold tracking-[-0.03em]">{{ Math.round(detail.matchScore || 0) }}</div>
            <p class="mt-3 text-sm text-white/80">{{ detail.resumeTitle || '当前未绑定简历' }}</p>
          </div>
        </div>
      </section>

      <section class="grid gap-4 xl:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)]">
        <article class="shell-section-card p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="section-kicker">状态流转</p>
              <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">更新当前进度</h3>
            </div>
            <el-button :loading="refreshingAnalysis" size="large" class="hard-button-secondary" @click="handleRefreshAnalysis">
              重做 JD 分析
            </el-button>
          </div>

          <div class="mt-5 grid gap-4 md:grid-cols-2">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">当前状态</div>
              <el-select v-model="statusForm.status" class="mt-2 w-full" size="large">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">下一节点日期</div>
              <el-date-picker v-model="statusForm.nextStepDate" class="mt-2 w-full" type="date" value-format="YYYY-MM-DD" placeholder="可选" />
            </div>
          </div>
          <div class="data-slab mt-4 p-4">
            <div class="text-xs uppercase tracking-[0.22em] text-tertiary">备注</div>
            <el-input v-model="statusForm.note" class="mt-2" type="textarea" :rows="4" placeholder="例如：约了下周三一面，需要补 Redis 和消息队列项目案例" />
          </div>
          <el-button :loading="updatingStatus" size="large" class="action-button mt-4 w-full" @click="handleUpdateStatus">
            更新投递状态
          </el-button>
        </article>

        <article class="shell-section-card p-5 sm:p-6">
          <p class="section-kicker">JD 分析</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">差距与准备重点</h3>
          <div class="mt-5 grid gap-4 md:grid-cols-2">
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
                <span v-if="!detail.missingKeywords.length" class="text-sm text-secondary">当前未识别到明显缺口</span>
              </div>
            </div>
          </div>

          <div class="surface-card mt-4 p-4">
            <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">JD 原文</div>
            <p class="mt-2 whitespace-pre-wrap text-sm leading-7 text-primary">{{ detail.jdText }}</p>
          </div>
        </article>
      </section>

      <section class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_340px]">
        <article class="shell-section-card p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="section-kicker">真实事件</p>
              <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">面试记录与复盘</h3>
            </div>
          </div>

          <div class="mt-5 space-y-3">
            <article v-for="event in detail.events" :key="event.id" class="surface-card p-4">
              <div class="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">{{ event.eventType }}</div>
                  <h4 class="mt-2 text-lg font-semibold text-ink">{{ event.title }}</h4>
                </div>
                <span class="text-xs text-tertiary">{{ formatDateTime(event.eventTime) }}</span>
              </div>
              <p v-if="event.content" class="mt-2 text-sm leading-7 text-secondary">{{ event.content }}</p>
              <p v-if="event.result" class="mt-2 text-sm font-semibold text-accent">结果：{{ event.result }}</p>
            </article>
            <div v-if="!detail.events.length" class="rounded-2xl border border-dashed border-[var(--bc-line)] p-5 text-sm text-secondary">
              还没有事件记录，可以先补一条面试安排或复盘笔记。
            </div>
          </div>
        </article>

        <article class="shell-section-card p-5 sm:p-6">
          <p class="section-kicker">新增事件</p>
          <div class="mt-5 space-y-4">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">事件类型</div>
              <el-select v-model="eventForm.eventType" class="mt-2 w-full" size="large">
                <el-option label="面试安排" value="interview" />
                <el-option label="复盘笔记" value="review" />
                <el-option label="状态备注" value="note" />
              </el-select>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">标题</div>
              <el-input v-model="eventForm.title" class="mt-2" size="large" placeholder="例如：一面结束，需要补并发和缓存一致性" />
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">内容</div>
              <el-input v-model="eventForm.content" class="mt-2" type="textarea" :rows="5" placeholder="记录真实问题、表现、反馈和下轮准备重点" />
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">结果</div>
              <el-input v-model="eventForm.result" class="mt-2" size="large" placeholder="例如：通过 / 待反馈 / 挂在项目深挖" />
            </div>
            <el-button :loading="addingEvent" size="large" class="action-button w-full" @click="handleAddEvent">
              添加事件
            </el-button>
          </div>
        </article>
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
  result: ''
})

const statusLabel = (value: string) => statusOptions.find((item) => item.value === value)?.label || value
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
    ElMessage.success('状态已更新')
  } catch (error: any) {
    ElMessage.error(error?.message || '状态更新失败')
  } finally {
    updatingStatus.value = false
  }
}

const handleAddEvent = async () => {
  const id = applicationId()
  if (!id || !eventForm.title.trim()) {
    ElMessage.warning('请填写事件标题')
    return
  }
  addingEvent.value = true
  try {
    const response = await addApplicationEventApi(id, {
      eventType: eventForm.eventType,
      title: eventForm.title.trim(),
      content: eventForm.content.trim() || undefined,
      result: eventForm.result.trim() || undefined
    })
    detail.value = response.data
    eventForm.title = ''
    eventForm.content = ''
    eventForm.result = ''
    ElMessage.success('事件已添加')
  } catch (error: any) {
    ElMessage.error(error?.message || '添加事件失败')
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

onMounted(() => {
  void loadData()
})
</script>

<style scoped>
.application-score-card {
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 86% 18%, rgba(var(--bc-cyan-rgb), 0.28), transparent 34%),
    linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.94), rgba(75, 64, 49, 0.96));
}
</style>
