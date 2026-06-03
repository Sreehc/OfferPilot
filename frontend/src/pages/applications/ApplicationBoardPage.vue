<template>
  <div class="space-y-5">
    <section class="shell-section-card application-state-card p-4 sm:p-5">
      <div class="workspace-head__top">
        <div class="workspace-head__main">
          <div class="flex flex-wrap gap-2">
            <span class="hard-chip">{{ applications.length ? '投递进行中' : '记录第一条岗位' }}</span>
            <span class="detail-pill">{{ applications.length }} 条岗位</span>
            <span class="detail-pill">{{ activeCount }} 条正在推进</span>
            <span v-if="currentFocus" class="detail-pill">{{ statusLabel(currentFocus.status) }}</span>
          </div>

          <h1 class="mt-4 workspace-title">投递管理</h1>
          <p class="workspace-summary">
            {{
              currentFocus
                ? `${currentFocus.company} · ${currentFocus.jobTitle}，处理这条岗位的下一步动作。`
                : '录入岗位和 JD 后，这里会显示匹配度和下一步建议。'
            }}
          </p>
        </div>

        <div class="workspace-actions">
          <a href="#application-create" class="hard-button-primary">
            {{ applications.length ? '新增岗位' : '记录岗位' }}
          </a>
          <RouterLink :to="applicationBoardAgentLink" class="hard-button-secondary">
            交给 Agent 推进
          </RouterLink>
          <RouterLink to="/resume" class="hard-button-secondary">检查简历版本</RouterLink>
          <RouterLink v-if="currentFocus" :to="`/applications/${currentFocus.id}`" class="hard-button-secondary">
            查看当前时间线
          </RouterLink>
        </div>
      </div>

      <div class="application-state-metrics mt-4">
        <article class="application-metric-card">
          <span>当前重点</span>
          <strong>{{ currentFocus ? '推进现有岗位' : '录入岗位信息' }}</strong>
        </article>
        <article class="application-metric-card">
          <span>最高匹配度</span>
          <strong>{{ topMatchScore }}</strong>
        </article>
        <article class="application-metric-card">
          <span>Offer 数</span>
          <strong>{{ statuses.offer.count }}</strong>
        </article>
      </div>
    </section>

    <section class="grid gap-4 xl:grid-cols-[minmax(0,0.92fr)_minmax(0,1.08fr)]">
      <article v-if="!currentFocus" id="application-create" class="shell-section-card p-5 sm:p-6">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">记录岗位</h3>
            <p class="mt-2 text-sm leading-7 text-secondary">填写公司、岗位和 JD，把这条机会加入当前工作区。</p>
          </div>
        </div>

        <div class="mt-5 grid gap-4">
          <div class="grid gap-4 md:grid-cols-2">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">公司</div>
              <el-input v-model="form.company" class="mt-2" size="large" placeholder="例如：美团" />
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">岗位</div>
              <el-input v-model="form.jobTitle" class="mt-2" size="large" placeholder="例如：Java 后端开发" />
            </div>
          </div>

          <div class="grid gap-4 md:grid-cols-2">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">城市</div>
              <el-input v-model="form.city" class="mt-2" size="large" placeholder="例如：上海" />
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">渠道</div>
              <el-input v-model="form.source" class="mt-2" size="large" placeholder="例如：Boss / 官网 / 内推" />
            </div>
          </div>

          <div class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.22em] text-tertiary">绑定简历</div>
            <el-select
              v-model="form.resumeFileId"
              class="mt-2 w-full"
              size="large"
              clearable
              placeholder="默认使用最新简历"
            >
              <el-option v-for="item in resumes" :key="item.id" :label="item.title" :value="item.id" />
            </el-select>
          </div>

          <div class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.22em] text-tertiary">JD 原文</div>
            <el-input
              v-model="form.jdText"
              class="mt-2"
              type="textarea"
              :rows="11"
              placeholder="粘贴岗位 JD，获取匹配度和推进建议"
            />
          </div>

          <el-button :loading="creating" size="large" class="action-button w-full" @click="handleCreate">
            记录岗位并生成建议
          </el-button>
        </div>
      </article>

      <div class="space-y-4">
        <article class="shell-section-card p-5 sm:p-6 application-board-panel">
          <section>
            <div class="flex items-start justify-between gap-3">
              <div>
                <h3 class="text-xl font-semibold tracking-[-0.03em] text-ink">当前重点岗位</h3>
                <p class="mt-2 text-sm text-secondary">查看当前最值得推进的岗位。</p>
              </div>
            </div>

            <div v-if="currentFocus" class="mt-5 focus-card">
              <div class="flex flex-wrap items-center gap-2">
                <span class="hard-chip">{{ currentFocus.company }}</span>
                <span class="detail-pill">{{ currentFocus.jobTitle }}</span>
                <span class="detail-pill">{{ statusLabel(currentFocus.status) }}</span>
              </div>
              <p class="mt-4 text-lg font-semibold text-ink">
                {{ currentFocus.nextStepSuggestion || '把这条岗位推进到下一阶段' }}
              </p>
              <p class="mt-3 text-sm leading-7 text-secondary">
                {{ currentFocus.reviewSuggestion || currentFocus.analysisSummary }}
              </p>
              <div class="mt-4 flex flex-wrap gap-2">
                <span
                  v-for="tag in currentFocus.missingKeywords.slice(0, 4)"
                  :key="`${currentFocus.id}-${tag}`"
                  class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral"
                >
                  {{ tag }}
                </span>
              </div>
              <RouterLink :to="`/applications/${currentFocus.id}`" class="hard-button-primary mt-5 inline-flex">
                打开这条岗位
              </RouterLink>
              <RouterLink :to="applicationBoardAgentLink" class="hard-button-secondary mt-3 inline-flex">
                生成推进策略
              </RouterLink>
            </div>
            <div v-else class="mt-5 rounded-2xl border border-dashed border-[var(--bc-line)] p-5 text-sm text-secondary">
              {{ EMPTY_STATE_COPY.applicationBoardFocus.title }}，{{ EMPTY_STATE_COPY.applicationBoardFocus.description }}
            </div>
          </section>

          <section class="application-board-panel__section">
            <div class="flex items-center justify-between gap-3">
              <div>
                <h3 class="text-xl font-semibold tracking-[-0.03em] text-ink">按阶段浏览</h3>
                <p class="mt-2 text-sm text-secondary">按阶段快速定位每条岗位的当前位置。</p>
              </div>
            </div>

            <div class="application-board mt-6">
              <article v-for="column in orderedColumns" :key="column.key" class="application-column">
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">{{ column.label }}</div>
                    <h4 class="mt-2 text-lg font-semibold text-ink">{{ column.count }} 条</h4>
                  </div>
                  <span class="application-column__count">{{ column.avgScore }}</span>
                </div>

                <div class="mt-4 space-y-3">
                  <RouterLink
                    v-for="item in column.items"
                    :key="item.id"
                    :to="`/applications/${item.id}`"
                    class="application-card block"
                  >
                    <div class="flex items-center justify-between gap-3">
                      <div class="min-w-0">
                        <div class="text-xs font-semibold uppercase tracking-[0.18em] text-tertiary">
                          {{ item.company }}
                        </div>
                        <h5 class="mt-2 text-base font-semibold text-ink">{{ item.jobTitle }}</h5>
                      </div>
                      <span class="text-xl font-semibold tracking-[-0.03em]" :class="scoreClass(item.matchScore)">
                        {{ Math.round(item.matchScore || 0) }}
                      </span>
                    </div>
                    <p class="mt-2 text-sm leading-7 text-secondary">
                      {{ item.nextStepSuggestion || item.reviewSuggestion || item.analysisSummary }}
                    </p>
                  </RouterLink>

                  <div
                    v-if="!column.items.length"
                    class="rounded-2xl border border-dashed border-[var(--bc-line)] p-4 text-sm text-secondary"
                  >
                    当前阶段还没有岗位。你可以新增岗位，或补充上一阶段的进展。
                  </div>
                </div>
              </article>
            </div>
          </section>
        </article>

        <article v-if="currentFocus" id="application-create" class="shell-section-card p-5 sm:p-6">
          <div class="flex items-start justify-between gap-3">
            <div>
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">补记下一条岗位</h3>
              <p class="mt-2 text-sm leading-7 text-secondary">
                当前重点岗位已经明确。需要扩充机会时，可以补充新的岗位。
              </p>
            </div>
          </div>

          <div class="mt-5 grid gap-4">
            <div class="grid gap-4 md:grid-cols-2">
              <div class="data-slab p-4">
                <div class="text-xs uppercase tracking-[0.22em] text-tertiary">公司</div>
                <el-input v-model="form.company" class="mt-2" size="large" placeholder="例如：美团" />
              </div>
              <div class="data-slab p-4">
                <div class="text-xs uppercase tracking-[0.22em] text-tertiary">岗位</div>
                <el-input v-model="form.jobTitle" class="mt-2" size="large" placeholder="例如：Java 后端开发" />
              </div>
            </div>

            <div class="grid gap-4 md:grid-cols-2">
              <div class="data-slab p-4">
                <div class="text-xs uppercase tracking-[0.22em] text-tertiary">城市</div>
                <el-input v-model="form.city" class="mt-2" size="large" placeholder="例如：上海" />
              </div>
              <div class="data-slab p-4">
                <div class="text-xs uppercase tracking-[0.22em] text-tertiary">渠道</div>
                <el-input v-model="form.source" class="mt-2" size="large" placeholder="例如：Boss / 官网 / 内推" />
              </div>
            </div>

            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">绑定简历</div>
              <el-select
                v-model="form.resumeFileId"
                class="mt-2 w-full"
                size="large"
                clearable
                placeholder="默认使用最新简历"
              >
                <el-option v-for="item in resumes" :key="item.id" :label="item.title" :value="item.id" />
              </el-select>
            </div>

            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">JD 原文</div>
              <el-input
                v-model="form.jdText"
                class="mt-2"
                type="textarea"
                :rows="11"
                placeholder="粘贴岗位 JD，获取匹配度和下一步建议"
              />
            </div>

            <el-button :loading="creating" size="large" class="action-button w-full" @click="handleCreate">
              记录这条岗位
            </el-button>
          </div>
        </article>

      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { createJobApplicationApi, fetchApplicationBoardApi } from '@/api/applications'
import { EMPTY_STATE_COPY } from '@/constants/productCopy'
import { fetchResumeListApi } from '@/api/resume'
import type { JobApplicationItem, ResumeSummaryItem } from '@/types/api'
import { buildAgentWorkbenchLocation } from '@/utils/agent'
import { markGuideSeenForCriticalAction } from '@/utils/guide'
import { storage } from '@/utils/storage'

const creating = ref(false)
const applications = ref<JobApplicationItem[]>([])
const resumes = ref<ResumeSummaryItem[]>([])
const currentUser = storage.getUser()

const form = reactive({
  company: '',
  jobTitle: '',
  city: '',
  source: '',
  jdText: '',
  resumeFileId: ''
})

const statuses = computed(() => {
  const map = {
    saved: { label: '待投递', count: 0, items: [] as JobApplicationItem[] },
    applied: { label: '已投递', count: 0, items: [] as JobApplicationItem[] },
    written: { label: '笔试 / 作业', count: 0, items: [] as JobApplicationItem[] },
    interview: { label: '面试中', count: 0, items: [] as JobApplicationItem[] },
    offer: { label: 'Offer', count: 0, items: [] as JobApplicationItem[] },
    rejected: { label: '已淘汰', count: 0, items: [] as JobApplicationItem[] }
  }
  for (const item of applications.value) {
    const key = (item.status || 'saved') as keyof typeof map
    if (map[key]) {
      map[key].items.push(item)
      map[key].count++
    }
  }
  return map
})

const orderedColumns = computed(() =>
  (
    Object.entries(statuses.value) as Array<[string, { label: string; count: number; items: JobApplicationItem[] }]>
  ).map(([key, value]) => ({
    key,
    label: value.label,
    count: value.count,
    items: value.items,
    avgScore: value.items.length
      ? `${Math.round(value.items.reduce((sum, item) => sum + (item.matchScore || 0), 0) / value.items.length)} 分`
      : '--'
  }))
)

const statusPriority: Record<string, number> = {
  interview: 0,
  written: 1,
  applied: 2,
  saved: 3,
  offer: 4,
  rejected: 5
}

const currentFocus = computed(() => {
  if (!applications.value.length) return null
  return [...applications.value].sort((left, right) => {
    const leftPriority = statusPriority[left.status] ?? 99
    const rightPriority = statusPriority[right.status] ?? 99
    if (leftPriority !== rightPriority) return leftPriority - rightPriority
    return (right.matchScore || 0) - (left.matchScore || 0)
  })[0]
})

const activeCount = computed(
  () => statuses.value.applied.count + statuses.value.written.count + statuses.value.interview.count
)

const topMatchScore = computed(() => {
  if (!applications.value.length) return '--'
  return `${Math.round(Math.max(...applications.value.map((item) => item.matchScore || 0)))}`
})
const applicationBoardAgentLink = computed(() => {
  const contextRefs = ['application:board', 'analytics:profile', 'resume:latest']
  if (currentFocus.value?.id) {
    contextRefs.unshift(`application:${currentFocus.value.id}`)
  }
  return buildAgentWorkbenchLocation({
    agentType: 'application_strategist',
    triggerSource: 'applications',
    contextRefs,
    userPrompt: currentFocus.value?.jobTitle
      ? `围绕 ${currentFocus.value.company} 的 ${currentFocus.value.jobTitle} 岗位，整理下一步推进策略和备面动作。`
      : '结合当前投递看板、JD 分析和简历上下文，整理下一步推进策略。'
  })
})

const statusLabel = (value: string) => {
  switch (value) {
    case 'saved':
      return '待投递'
    case 'applied':
      return '已投递'
    case 'written':
      return '笔试 / 作业'
    case 'interview':
      return '面试中'
    case 'offer':
      return 'Offer'
    case 'rejected':
      return '已淘汰'
    default:
      return value
  }
}

const scoreClass = (score: number) => (score >= 75 ? 'text-accent' : score >= 60 ? 'text-amber-500' : 'text-coral')

const loadData = async () => {
  const [applicationsResponse, resumeResponse] = await Promise.all([fetchApplicationBoardApi(), fetchResumeListApi()])
  applications.value = applicationsResponse.data
  resumes.value = resumeResponse.data
}

const resetForm = () => {
  form.company = ''
  form.jobTitle = ''
  form.city = ''
  form.source = ''
  form.jdText = ''
  form.resumeFileId = ''
}

const handleCreate = async () => {
  if (!form.company.trim() || !form.jobTitle.trim() || !form.jdText.trim()) {
    ElMessage.warning('请补全公司、岗位和 JD 原文')
    return
  }
  const isFirstApplication = applications.value.length === 0
  creating.value = true
  try {
    await createJobApplicationApi({
      company: form.company.trim(),
      jobTitle: form.jobTitle.trim(),
      city: form.city.trim() || undefined,
      source: form.source.trim() || undefined,
      jdText: form.jdText.trim(),
      resumeFileId: form.resumeFileId || undefined
    })
    resetForm()
    await loadData()
    if (isFirstApplication) {
      markGuideSeenForCriticalAction(currentUser?.id)
    }
    ElMessage.success('岗位已记录')
  } catch (error: any) {
    ElMessage.error(error?.message || '岗位记录失败，请检查岗位信息和 JD 后重试。')
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped>
.application-state-card {
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.11), transparent 28%),
    radial-gradient(circle at 88% 20%, rgba(var(--bc-cyan-rgb), 0.11), transparent 20%), var(--bc-surface-card);
}

.application-state-hero {
  min-width: 0;
}

.application-board-panel {
  display: grid;
  gap: 20px;
}

.application-board-panel__section {
  padding-top: 20px;
  border-top: 1px solid var(--bc-border-subtle);
}

.application-state-metrics {
  display: grid;
  gap: 0.65rem;
  margin-top: 1rem;
}

.application-metric-card {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.38);
  padding: 0.8rem 0.9rem;
  backdrop-filter: blur(10px);
}

.application-metric-card span {
  display: block;
  font-size: 0.8rem;
  color: var(--bc-ink-secondary);
}

.application-metric-card strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.05rem;
  line-height: 1.25;
  color: var(--bc-ink);
}

.focus-card {
  border-radius: calc(var(--radius-lg) - 2px);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.16);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.05), transparent 52%), var(--bc-surface-muted);
  padding: 1.1rem 1.15rem;
}

.application-board {
  display: grid;
  gap: 1rem;
}

.application-column {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.05), transparent 60%), var(--bc-surface-muted);
  padding: 1rem;
}

.application-column__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 3rem;
  border-radius: 999px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.2);
  background: rgba(var(--bc-accent-rgb), 0.1);
  padding: 0.35rem 0.7rem;
  font-size: 0.76rem;
  font-weight: 700;
  color: var(--bc-accent);
}

.application-card {
  border-radius: 16px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-card);
  padding: 0.95rem;
  text-decoration: none;
}

@media (min-width: 768px) {
  .application-state-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (min-width: 1280px) {
  .application-board {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
