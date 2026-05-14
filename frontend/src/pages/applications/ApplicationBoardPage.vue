<template>
  <div class="space-y-6">
    <AppShellHeader>
      <template #actions>
        <RouterLink to="/resume" class="hard-button-secondary">
          检查简历版本
        </RouterLink>
        <RouterLink v-if="currentFocus" :to="`/applications/${currentFocus.id}`" class="hard-button-primary">
          继续推进当前岗位
        </RouterLink>
      </template>
    </AppShellHeader>

    <section class="shell-section-card application-state-card p-5 sm:p-6">
      <div class="grid gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(0,0.65fr)] xl:items-start">
        <div class="min-w-0">
          <div class="flex flex-wrap gap-2">
            <span class="hard-chip">{{ applications.length ? '继续推进投递' : '先记录第一条岗位' }}</span>
            <span class="detail-pill">{{ applications.length }} 条岗位</span>
            <span class="detail-pill">{{ activeCount }} 条正在推进</span>
            <span v-if="currentFocus" class="detail-pill">{{ statusLabel(currentFocus.status) }}</span>
          </div>

          <p class="mt-5 font-display text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">
            {{ currentFocus ? `先推进 ${currentFocus.company} · ${currentFocus.jobTitle}` : '先把岗位记录下来' }}
          </p>
          <p class="mt-4 max-w-3xl text-sm leading-7 text-secondary">
            {{
              currentFocus
                ? currentFocus.nextStepSuggestion || currentFocus.reviewSuggestion || '先把这条岗位推进到下一阶段，再回来看板。'
                : '先录入岗位和 JD，系统会帮你整理匹配度、下一步提醒和后续复盘线索。'
            }}
          </p>

          <div class="mt-6 flex flex-wrap gap-3">
            <a href="#application-create" class="hard-button-primary">
              {{ applications.length ? '再记录一条岗位' : '开始记录岗位' }}
            </a>
            <RouterLink v-if="currentFocus" :to="`/applications/${currentFocus.id}`" class="hard-button-secondary">
              查看当前时间线
            </RouterLink>
          </div>
        </div>

        <div class="grid gap-3 sm:grid-cols-3 xl:grid-cols-1">
          <article class="application-metric-card">
            <span>当前主动作</span>
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
      </div>
    </section>

    <section class="grid gap-4 xl:grid-cols-[minmax(0,0.92fr)_minmax(0,1.08fr)]">
      <article id="application-create" class="shell-section-card p-5 sm:p-6">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">1. 记录岗位</h3>
            <p class="mt-2 text-sm leading-7 text-secondary">
              先录公司、岗位和 JD，再决定用哪份简历去推进。
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
            <el-select v-model="form.resumeFileId" class="mt-2 w-full" size="large" clearable placeholder="默认使用最新简历">
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
              placeholder="粘贴岗位 JD，系统会先帮你算匹配度，再给后续推进建议"
            />
          </div>

          <el-button :loading="creating" size="large" class="action-button w-full" @click="handleCreate">
            记录岗位并生成建议
          </el-button>
        </div>
      </article>

      <div class="space-y-4">
        <article class="shell-section-card p-5 sm:p-6">
          <div class="flex items-start justify-between gap-3">
            <div>
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">2. 推进当前岗位</h3>
              <p class="mt-2 text-sm leading-7 text-secondary">
                先看你现在最该推进哪一条，再回到对应状态列处理。
              </p>
            </div>
          </div>

          <div v-if="currentFocus" class="mt-5 focus-card">
            <div class="flex flex-wrap items-center gap-2">
              <span class="hard-chip">{{ currentFocus.company }}</span>
              <span class="detail-pill">{{ currentFocus.jobTitle }}</span>
              <span class="detail-pill">{{ statusLabel(currentFocus.status) }}</span>
            </div>
            <p class="mt-4 text-lg font-semibold text-ink">
              {{ currentFocus.nextStepSuggestion || '先把这条岗位推进到下一阶段' }}
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
          </div>
          <div v-else class="mt-5 rounded-2xl border border-dashed border-[var(--bc-line)] p-5 text-sm text-secondary">
            还没有可推进的岗位，先记录第一条。
          </div>
        </article>

        <article class="shell-section-card p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <div>
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">3. 按阶段浏览</h3>
              <p class="mt-2 text-sm leading-7 text-secondary">
                看板只负责帮你找到当前阶段的岗位，不再和录入区抢注意力。
              </p>
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
                      <div class="text-xs font-semibold uppercase tracking-[0.18em] text-tertiary">{{ item.company }}</div>
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

                <div v-if="!column.items.length" class="rounded-2xl border border-dashed border-[var(--bc-line)] p-4 text-sm text-secondary">
                  当前阶段还没有岗位。
                </div>
              </div>
            </article>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import AppShellHeader from '@/components/AppShellHeader.vue'
import { createJobApplicationApi, fetchApplicationBoardApi } from '@/api/applications'
import { fetchResumeListApi } from '@/api/resume'
import type { JobApplicationItem, ResumeSummaryItem } from '@/types/api'

const creating = ref(false)
const applications = ref<JobApplicationItem[]>([])
const resumes = ref<ResumeSummaryItem[]>([])

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
  (Object.entries(statuses.value) as Array<[string, { label: string; count: number; items: JobApplicationItem[] }]>).map(([key, value]) => ({
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
    ElMessage.success('岗位已记录，接下来可以继续推进这条投递')
  } catch (error: any) {
    ElMessage.error(error?.message || '创建岗位失败')
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
    radial-gradient(circle at 88% 20%, rgba(var(--bc-cyan-rgb), 0.11), transparent 20%),
    var(--bc-surface-card);
}

.application-metric-card {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.38);
  padding: 0.95rem 1rem;
  backdrop-filter: blur(10px);
}

.application-metric-card span {
  display: block;
  font-size: 0.8rem;
  color: var(--bc-ink-secondary);
}

.application-metric-card strong {
  display: block;
  margin-top: 0.45rem;
  font-size: 1.15rem;
  line-height: 1.25;
  color: var(--bc-ink);
}

.focus-card {
  border-radius: calc(var(--radius-lg) - 2px);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.16);
  background:
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.05), transparent 52%),
    var(--bc-surface-muted);
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

@media (min-width: 1280px) {
  .application-board {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
