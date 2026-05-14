<template>
  <div class="space-y-6">
    <AppShellHeader>
      <template #actions>
        <RouterLink to="/interview" class="hard-button-secondary">
          准备模拟面试
        </RouterLink>
        <RouterLink to="/applications" class="hard-button-primary">
          绑定投递岗位
        </RouterLink>
      </template>
    </AppShellHeader>

    <section class="shell-section-card resume-hero p-5 sm:p-6">
      <div class="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
        <div class="max-w-3xl">
          <div class="flex flex-wrap gap-2">
            <span class="hard-chip">{{ currentResume ? '继续整理这份简历' : '先上传一份简历' }}</span>
            <span class="detail-pill">{{ resumeList.length }} 份简历</span>
            <span class="detail-pill">{{ currentResume?.projects.length || 0 }} 个项目</span>
          </div>
          <p class="mt-5 font-display text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">
            {{ currentResume ? currentResume.title : '从简历开始整理项目表达' }}
          </p>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-secondary">
            {{
              currentResume
                ? currentResume.summary
                : '上传 PDF 或 Word 简历后，你可以继续整理项目经历、项目追问、自我介绍和面试版提纲。'
            }}
          </p>
        </div>

        <div class="resume-hero__signals">
          <article class="resume-hero__signal">
            <span>技能标签</span>
            <strong>{{ currentResume?.skills.length || 0 }}</strong>
          </article>
          <article class="resume-hero__signal">
            <span>追问条数</span>
            <strong>{{ totalQuestions }}</strong>
          </article>
          <article class="resume-hero__signal">
            <span>风险提示</span>
            <strong>{{ totalRisks }}</strong>
          </article>
        </div>
      </div>
    </section>

    <section class="grid gap-4 xl:grid-cols-[360px_minmax(0,1fr)]">
      <aside class="space-y-4">
        <article class="shell-section-card p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <div>
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">先上传简历</h3>
            </div>
          </div>

          <el-upload
            class="mt-5 w-full"
            drag
            :show-file-list="false"
            accept=".pdf,.doc,.docx"
            :before-upload="handleUpload"
          >
            <div class="py-5">
              <div class="text-base font-semibold text-ink">拖拽简历到这里，或点击上传</div>
              <p class="mt-2 text-sm text-secondary">支持 PDF / DOC / DOCX，单个文件不超过 10MB</p>
            </div>
          </el-upload>

          <p v-if="uploading" class="mt-3 text-sm text-secondary">正在整理项目、技能和追问，请稍候...</p>
        </article>

        <article class="shell-section-card p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <div>
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">切换已上传的简历</h3>
            </div>
          </div>

          <div v-if="resumeList.length" class="mt-5 space-y-3">
            <button
              v-for="item in resumeList"
              :key="item.id"
              type="button"
              class="resume-list-card w-full text-left"
              :class="selectedResumeId === item.id ? 'resume-list-card-active' : ''"
              @click="handleSelectResume(item.id)"
            >
              <div class="flex items-center justify-between gap-3">
                <div class="min-w-0">
                  <div class="text-sm font-semibold text-ink">{{ item.title }}</div>
                  <div class="mt-1 text-xs text-tertiary">{{ item.fileType.toUpperCase() }} · {{ formatDateTime(item.updateTime) }}</div>
                </div>
                <span class="detail-pill">{{ item.parseStatus }}</span>
              </div>
            </button>
          </div>
          <div v-else class="mt-5 rounded-2xl border border-dashed border-[var(--bc-line)] p-5 text-sm text-secondary">
            还没有上传简历，先上传一份 PDF / Word 简历开始生成结构化结果。
          </div>
        </article>
      </aside>

      <section class="space-y-4">
        <article v-if="currentResume" class="shell-section-card p-5 sm:p-6">
          <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">教育信息</div>
              <p class="mt-2 text-sm leading-6 text-primary">{{ currentResume.education || '未识别' }}</p>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">项目数</div>
              <div class="mt-2 text-3xl font-semibold tracking-[-0.03em] text-ink">{{ currentResume.projects.length }}</div>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">技能标签</div>
              <div class="mt-3 flex flex-wrap gap-2">
                <span
                  v-for="skill in currentResume.skills.slice(0, 6)"
                  :key="skill"
                  class="rounded-full bg-accent/10 px-3 py-1 text-xs font-semibold text-accent"
                >
                  {{ skill }}
                </span>
              </div>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">最后解析</div>
              <p class="mt-2 text-sm leading-6 text-primary">{{ formatDateTime(currentResume.lastParsedAt || currentResume.updateTime) }}</p>
            </div>
          </div>
        </article>

        <article v-if="currentResume" class="shell-section-card p-5 sm:p-6">
          <el-tabs v-model="activeTab" class="resume-tabs">
            <el-tab-pane label="解析结果" name="overview">
              <div class="grid gap-4 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
                <div class="space-y-4">
                  <article v-for="project in currentResume.projects" :key="project.id" class="resume-project-card">
                    <div class="flex flex-wrap items-start justify-between gap-3">
                      <div class="min-w-0">
                        <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">{{ project.roleName }}</div>
                        <h4 class="mt-2 text-lg font-semibold text-ink">{{ project.projectName }}</h4>
                      </div>
                      <span class="detail-pill">{{ project.techStack || '技术栈待补充' }}</span>
                    </div>
                    <p class="mt-3 text-sm leading-7 text-secondary">{{ project.projectSummary }}</p>
                    <div class="mt-4 grid gap-3 md:grid-cols-2">
                      <div class="surface-card p-4">
                        <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">职责</div>
                        <p class="mt-2 text-sm leading-6 text-primary">{{ project.responsibility }}</p>
                      </div>
                      <div class="surface-card p-4">
                        <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">成果</div>
                        <p class="mt-2 text-sm leading-6 text-primary">{{ project.achievement }}</p>
                      </div>
                    </div>
                  </article>
                </div>

                <article class="shell-section-card !shadow-none !border p-5 sm:p-6">
                  <p class="section-kicker">摘要</p>
                  <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">当前简历风险</h3>
                  <div class="mt-5 space-y-3">
                    <div
                      v-for="risk in flattenedRisks"
                      :key="risk"
                      class="rounded-2xl border border-coral/20 bg-coral/5 px-4 py-3 text-sm text-secondary"
                    >
                      {{ risk }}
                    </div>
                    <div v-if="!flattenedRisks.length" class="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-secondary">
                      当前项目描述相对完整，下一步建议直接进入项目追问训练。
                    </div>
                  </div>
                </article>
              </div>
            </el-tab-pane>

            <el-tab-pane label="项目追问" name="questions">
              <div class="space-y-4">
                <article v-for="project in projectQuestions" :key="project.id" class="resume-project-card">
                  <h4 class="text-lg font-semibold text-ink">{{ project.projectName }}</h4>
                  <p class="mt-2 text-sm leading-6 text-secondary">{{ project.projectSummary }}</p>
                  <div class="mt-4 space-y-3">
                    <div v-for="question in project.followUpQuestions" :key="question.question" class="surface-card p-4">
                      <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">{{ question.intent }}</div>
                      <p class="mt-2 text-sm leading-6 text-primary">{{ question.question }}</p>
                    </div>
                  </div>
                </article>
              </div>
            </el-tab-pane>

            <el-tab-pane label="自我介绍" name="intro">
              <div class="surface-card p-5">
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <p class="section-kicker">面试开场</p>
                    <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">推荐自我介绍</h3>
                  </div>
                </div>
                <p class="mt-5 whitespace-pre-wrap text-sm leading-8 text-primary">{{ introContent }}</p>
              </div>
            </el-tab-pane>

            <el-tab-pane label="面试简历" name="interviewResume">
              <div class="surface-card p-5">
                <p class="section-kicker">面试版提纲</p>
                <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">用于项目深挖的简历口径</h3>
                <pre class="resume-preview mt-5 whitespace-pre-wrap text-sm leading-8 text-primary">{{ interviewResumeContent }}</pre>
              </div>
            </el-tab-pane>
          </el-tabs>
        </article>

        <article v-else class="shell-section-card p-8 text-center">
          <p class="text-lg font-semibold text-ink">还没有可展示的简历结果</p>
          <p class="mt-3 text-sm leading-7 text-secondary">
            上传后，这里会继续帮你整理项目拆解、项目追问、自我介绍和面试版提纲。
          </p>
        </article>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import AppShellHeader from '@/components/AppShellHeader.vue'
import {
  fetchInterviewResumeApi,
  fetchLatestResumeApi,
  fetchResumeDetailApi,
  fetchResumeIntroApi,
  fetchResumeListApi,
  fetchResumeProjectQuestionsApi,
  uploadResumeApi
} from '@/api/resume'
import type { ResumeFileDetail, ResumeProjectItem, ResumeSummaryItem } from '@/types/api'

const uploading = ref(false)
const activeTab = ref('overview')
const resumeList = ref<ResumeSummaryItem[]>([])
const currentResume = ref<ResumeFileDetail | null>(null)
const projectQuestions = ref<ResumeProjectItem[]>([])
const introContent = ref('')
const interviewResumeContent = ref('')
const selectedResumeId = ref('')

const totalQuestions = computed(
  () => currentResume.value?.projects.reduce((sum, item) => sum + (item.followUpQuestions?.length || 0), 0) || 0
)
const flattenedRisks = computed(() => {
  if (!currentResume.value) return []
  return [...new Set(currentResume.value.projects.flatMap((item) => item.riskHints || []))]
})
const totalRisks = computed(() => flattenedRisks.value.length)

const formatDateTime = (value?: string) => {
  if (!value) return '刚刚'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

const syncDerivedPanels = async (resumeId: string) => {
  const [questionsResponse, introResponse, previewResponse] = await Promise.all([
    fetchResumeProjectQuestionsApi(resumeId),
    fetchResumeIntroApi(resumeId),
    fetchInterviewResumeApi(resumeId)
  ])
  projectQuestions.value = questionsResponse.data
  introContent.value = introResponse.data.content
  interviewResumeContent.value = previewResponse.data.content
}

const handleSelectResume = async (resumeId: string) => {
  selectedResumeId.value = resumeId
  const detailResponse = await fetchResumeDetailApi(resumeId)
  currentResume.value = detailResponse.data
  await syncDerivedPanels(resumeId)
}

const loadData = async () => {
  const [listResponse, latestResponse] = await Promise.all([fetchResumeListApi(), fetchLatestResumeApi()])
  resumeList.value = listResponse.data
  currentResume.value = latestResponse.data
  if (currentResume.value) {
    selectedResumeId.value = currentResume.value.id
    await syncDerivedPanels(currentResume.value.id)
  }
}

const handleUpload = async (file: File) => {
  uploading.value = true
  try {
    const response = await uploadResumeApi(file)
    currentResume.value = response.data
    selectedResumeId.value = response.data.id
    await syncDerivedPanels(response.data.id)
    const listResponse = await fetchResumeListApi()
    resumeList.value = listResponse.data
    activeTab.value = 'overview'
    ElMessage.success('简历解析完成')
  } catch (error: any) {
    ElMessage.error(error?.message || '简历上传失败')
  } finally {
    uploading.value = false
  }
  return false
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped>
.resume-hero {
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.11), transparent 28%),
    radial-gradient(circle at 88% 20%, rgba(var(--bc-cyan-rgb), 0.11), transparent 18%),
    var(--bc-surface-card);
}

.resume-hero__signals {
  display: grid;
  gap: 0.75rem;
}

.resume-hero__signal {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.38);
  padding: 0.95rem 1rem;
  backdrop-filter: blur(10px);
}

.resume-hero__signal span {
  display: block;
  font-size: 0.8rem;
  color: var(--bc-ink-secondary);
}

.resume-hero__signal strong {
  display: block;
  margin-top: 0.45rem;
  font-size: 1.4rem;
  line-height: 1.15;
  color: var(--bc-ink);
}

.resume-list-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 0.95rem 1rem;
  transition: all 0.2s ease;
}

.resume-list-card:hover,
.resume-list-card-active {
  border-color: rgba(var(--bc-accent-rgb), 0.32);
  background: rgba(var(--bc-accent-rgb), 0.08);
}

.resume-project-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.05), transparent 58%), var(--bc-surface-card);
  padding: 1rem 1.05rem;
}

.resume-preview {
  font-family: inherit;
}

@media (min-width: 1024px) {
  .resume-hero__signals {
    grid-template-columns: repeat(3, minmax(0, 150px));
  }
}
</style>
