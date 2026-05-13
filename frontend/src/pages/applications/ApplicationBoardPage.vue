<template>
  <div class="space-y-6">
    <AppShellHeader>
      <template #actions>
        <RouterLink to="/resume" class="hard-button-secondary">
          绑定简历
        </RouterLink>
        <RouterLink to="/analytics" class="hard-button-primary">
          查看趋势
        </RouterLink>
      </template>
    </AppShellHeader>

    <section class="shell-section-card application-hero p-5 sm:p-6">
      <div class="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
        <div class="max-w-3xl">
          <div class="flex flex-wrap gap-2">
            <span class="hard-chip">{{ applications.length ? '投递看板已上线' : '先录入第一条岗位信息' }}</span>
            <span class="detail-pill">{{ applications.length }} 条岗位</span>
            <span class="detail-pill">{{ statuses.interview.count }} 条进行中</span>
          </div>
          <h2 class="mt-5 font-display text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">
            把岗位、简历、真实面试和复盘建议收进同一个投递看板
          </h2>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-secondary">
            新建岗位后，系统会提取 JD 关键词、计算和当前简历的匹配度，并把状态流转、真实面试记录和复盘事件沉淀到 application 域。
          </p>
        </div>

        <div class="application-hero__signals">
          <article class="application-hero__signal">
            <span>最高匹配度</span>
            <strong>{{ topMatchScore }}</strong>
          </article>
          <article class="application-hero__signal">
            <span>已投递</span>
            <strong>{{ statuses.applied.count }}</strong>
          </article>
          <article class="application-hero__signal">
            <span>Offer</span>
            <strong>{{ statuses.offer.count }}</strong>
          </article>
        </div>
      </div>
    </section>

    <section class="shell-section-card p-5 sm:p-6">
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="section-kicker">新增岗位</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">先录入 JD 再分析</h3>
        </div>
      </div>

      <div class="mt-5 grid gap-4 xl:grid-cols-2">
        <div class="space-y-4">
          <div class="grid gap-4 md:grid-cols-2">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">公司</div>
              <el-input v-model="form.company" class="mt-2" size="large" placeholder="例如：字节 / 腾讯 / 美团" />
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">岗位</div>
              <el-input v-model="form.jobTitle" class="mt-2" size="large" placeholder="Java 后端开发" />
            </div>
          </div>
          <div class="grid gap-4 md:grid-cols-2">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">城市</div>
              <el-input v-model="form.city" class="mt-2" size="large" placeholder="上海 / 北京 / 杭州" />
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">渠道</div>
              <el-input v-model="form.source" class="mt-2" size="large" placeholder="Boss / 官网 / 内推" />
            </div>
          </div>
          <div class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.22em] text-tertiary">绑定简历</div>
            <el-select v-model="form.resumeFileId" class="mt-2 w-full" size="large" clearable placeholder="默认使用最新简历">
              <el-option v-for="item in resumes" :key="item.id" :label="item.title" :value="item.id" />
            </el-select>
          </div>
        </div>

        <div class="data-slab p-4">
          <div class="text-xs uppercase tracking-[0.22em] text-tertiary">JD 原文</div>
          <el-input
            v-model="form.jdText"
            class="mt-2"
            type="textarea"
            :rows="12"
            placeholder="粘贴岗位 JD，系统会抽取技术关键词、匹配点和风险项"
          />
          <el-button :loading="creating" size="large" class="action-button mt-4 w-full" @click="handleCreate">
            创建并分析岗位
          </el-button>
        </div>
      </div>
    </section>

    <section class="shell-section-card p-5 sm:p-6">
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="section-kicker">投递看板</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">按状态管理真实求职流程</h3>
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
              <p class="mt-2 text-sm leading-7 text-secondary">{{ item.analysisSummary }}</p>
              <div class="mt-3 flex flex-wrap gap-2">
                <span v-for="tag in item.jdKeywords.slice(0, 4)" :key="`${item.id}-${tag}`" class="detail-pill">
                  {{ tag }}
                </span>
              </div>
            </RouterLink>

            <div v-if="!column.items.length" class="rounded-2xl border border-dashed border-[var(--bc-line)] p-4 text-sm text-secondary">
              当前列还没有岗位。
            </div>
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

const topMatchScore = computed(() => {
  if (!applications.value.length) return '--'
  return `${Math.round(Math.max(...applications.value.map((item) => item.matchScore || 0)))}`
})

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
    ElMessage.success('岗位已创建并完成初步分析')
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
.application-hero {
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.11), transparent 28%),
    radial-gradient(circle at 88% 20%, rgba(var(--bc-cyan-rgb), 0.11), transparent 20%),
    var(--bc-surface-card);
}

.application-hero__signals {
  display: grid;
  gap: 0.75rem;
}

.application-hero__signal {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.38);
  padding: 0.95rem 1rem;
  backdrop-filter: blur(10px);
}

.application-hero__signal span {
  display: block;
  font-size: 0.8rem;
  color: var(--bc-ink-secondary);
}

.application-hero__signal strong {
  display: block;
  margin-top: 0.45rem;
  font-size: 1.4rem;
  line-height: 1.15;
  color: var(--bc-ink);
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
  .application-hero__signals {
    grid-template-columns: repeat(3, minmax(0, 150px));
  }

  .application-board {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }
}
</style>
