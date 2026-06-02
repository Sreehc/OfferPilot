<template>
  <div class="agent-workbench space-y-5">
    <section class="shell-section-card p-5 sm:p-6">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p class="section-kicker">Agent</p>
          <h1 class="text-3xl font-semibold tracking-[-0.03em] text-ink">统一任务工作台</h1>
          <p class="mt-2 max-w-3xl text-sm leading-6 text-secondary">
            这里负责统一发起任务、查看 run 结果、确认待审批动作，并把结果送回训练、简历、投递和面试链路。
          </p>
        </div>
        <div class="agent-workbench__legend">
          <span class="detail-pill">统一入口</span>
          <span class="detail-pill">run 管理</span>
          <span class="detail-pill">审批占位</span>
        </div>
      </div>
    </section>

    <section class="shell-section-card p-5 sm:p-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 class="text-2xl font-semibold tracking-[-0.03em] text-ink">快速发起</h2>
          <p class="mt-1 text-sm text-secondary">先选 agent 类型和触发来源，再决定要把哪个模块的上下文带进来。</p>
        </div>
      </div>

      <div class="mt-5 grid gap-3 lg:grid-cols-3">
        <button
          v-for="item in quickStarts"
          :key="item.agentType"
          type="button"
          class="agent-launch-card"
          @click="applyQuickStart(item)"
        >
          <p class="text-sm font-semibold text-ink">{{ item.label }}</p>
          <p class="mt-2 text-sm leading-6 text-secondary">{{ item.description }}</p>
          <p class="mt-3 text-xs uppercase tracking-[0.18em] text-tertiary">{{ item.agentType }}</p>
        </button>
      </div>

      <div class="mt-6 grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
        <div class="space-y-4">
          <div class="grid gap-4 md:grid-cols-2">
            <div>
              <label class="flat-field-label">Agent 类型</label>
              <el-select v-model="form.agentType" class="w-full">
                <el-option v-for="item in agentOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
            <div>
              <label class="flat-field-label">触发来源</label>
              <el-select v-model="form.triggerSource" class="w-full">
                <el-option v-for="item in triggerOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
          </div>

          <div>
            <label class="flat-field-label">上下文引用</label>
            <el-select
              v-model="form.contextRefs"
              class="w-full"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="例如 interview:latest, analytics:profile, application:board"
            >
              <el-option v-for="item in contextRefOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </div>

          <div class="grid gap-4 md:grid-cols-2">
            <div>
              <label class="flat-field-label">Stream 模式</label>
              <el-select v-model="form.streamMode" class="w-full">
                <el-option label="sync" value="sync" />
                <el-option label="event-stream (预留)" value="event-stream" />
              </el-select>
            </div>
            <div class="agent-workbench__status-card">
              <p class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">当前契约</p>
              <p class="mt-2 text-sm leading-6 text-secondary">
                请求体会带上 `agentType / triggerSource / contextRefs / streamMode`，后续可直接衔接更完整的编排层。
              </p>
            </div>
          </div>

          <div>
            <label class="flat-field-label">补充目标</label>
            <el-input
              v-model="form.userPrompt"
              type="textarea"
              :rows="4"
              placeholder="例如：把这次录音复盘结果转成下一轮训练动作；或者优先帮我推进下周的一面准备。"
            />
          </div>

          <div class="flex justify-end">
            <el-button :loading="creating" type="primary" size="large" class="action-button !min-h-11" @click="handleCreate">
              发起 Agent Run
            </el-button>
          </div>
        </div>

        <aside class="agent-contract-card">
          <p class="section-kicker">运行边界</p>
          <h3 class="mt-2 text-xl font-semibold text-ink">当前这版先做什么</h3>
          <ul class="agent-contract-list mt-4">
            <li>统一发起任务并生成 run 结果。</li>
            <li>区分 agent 类型、触发来源和下一步动作。</li>
            <li>为后续审批、实时 Copilot 和结果写回保留位置。</li>
            <li>不在这一刀里假装已经有完整多 agent 编排。</li>
          </ul>
        </aside>
      </div>
    </section>

    <section class="grid gap-4 xl:grid-cols-[360px_minmax(0,1fr)]">
      <article class="shell-section-card p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <div>
            <h2 class="text-2xl font-semibold tracking-[-0.03em] text-ink">最近 Run</h2>
            <p class="mt-1 text-sm text-secondary">按更新时间倒序显示，可回看各模块发起过的结果。</p>
          </div>
        </div>

        <div v-if="loading" class="mt-6 flex h-[280px] items-center justify-center">
          <div class="text-center">
            <div class="mx-auto h-7 w-7 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
            <p class="mt-3 text-sm text-secondary">正在加载 run 列表...</p>
          </div>
        </div>
        <div v-else-if="!runs.length" class="mt-6">
          <EmptyState
            icon="clipboard"
            title="还没有 Agent Run"
            description="先从上面的统一入口发起一个任务，这里会开始沉淀最近执行记录。"
            compact
          />
        </div>
        <div v-else class="mt-4 space-y-2">
          <button
            v-for="run in runs"
            :key="run.id"
            type="button"
            class="agent-run-card"
            :class="{ 'agent-run-card--active': selectedRun?.id === run.id }"
            @click="selectRun(run)"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <p class="text-sm font-semibold text-ink">{{ run.title }}</p>
                <p class="mt-1 text-xs uppercase tracking-[0.18em] text-tertiary">
                  {{ run.agentType }} · {{ run.triggerSource }}
                </p>
              </div>
              <span class="agent-run-status" :class="run.requiresApproval ? 'agent-run-status--warning' : 'agent-run-status--ready'">
                {{ run.requiresApproval ? '待审批' : '已完成' }}
              </span>
            </div>
            <p class="mt-3 text-sm leading-6 text-secondary">{{ run.summary }}</p>
          </button>
        </div>
      </article>

      <article class="shell-section-card p-5 sm:p-6">
        <div v-if="selectedRun" class="space-y-5">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p class="section-kicker">Run Detail</p>
              <h2 class="text-2xl font-semibold tracking-[-0.03em] text-ink">{{ selectedRun.title }}</h2>
              <p class="mt-2 text-sm leading-6 text-secondary">{{ selectedRun.summary }}</p>
            </div>
            <div class="agent-detail-meta">
              <span class="detail-pill">{{ selectedRun.agentType }}</span>
              <span class="detail-pill">{{ selectedRun.triggerSource }}</span>
              <span class="detail-pill">{{ selectedRun.streamMode || 'sync' }}</span>
            </div>
          </div>

          <div v-if="selectedRun.userPrompt" class="agent-detail-block">
            <p class="agent-detail-block__title">补充目标</p>
            <p class="mt-2 text-sm leading-6 text-primary">{{ selectedRun.userPrompt }}</p>
          </div>

          <div v-if="selectedRun.contextRefs.length" class="agent-detail-block">
            <p class="agent-detail-block__title">上下文引用</p>
            <div class="mt-3 flex flex-wrap gap-2">
              <span v-for="item in selectedRun.contextRefs" :key="item" class="agent-context-pill">{{ item }}</span>
            </div>
          </div>

          <div class="grid gap-4 xl:grid-cols-2">
            <div class="agent-detail-block">
              <p class="agent-detail-block__title">建议动作</p>
              <ul class="agent-detail-list mt-3">
                <li v-for="item in selectedRun.recommendations" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div class="agent-detail-block">
              <p class="agent-detail-block__title">执行检查点</p>
              <ul class="agent-detail-list mt-3">
                <li v-for="item in selectedRun.checkpoints" :key="item">{{ item }}</li>
              </ul>
            </div>
          </div>

          <div class="agent-detail-footer">
            <div class="agent-approval-callout" :class="selectedRun.requiresApproval ? 'agent-approval-callout--warning' : 'agent-approval-callout--ready'">
              <p class="text-sm font-semibold text-ink">{{ selectedRun.requiresApproval ? '后续需要审批' : '当前无需审批' }}</p>
              <p class="mt-1 text-sm leading-6 text-secondary">
                {{ selectedRun.requiresApproval ? '下一步可以把这个 run 的结果接到审批写操作。' : '下一步可以直接跳转到结果消费页继续执行。' }}
              </p>
            </div>
            <RouterLink v-if="selectedRun.nextActionPath" :to="selectedRun.nextActionPath" class="hard-button-primary">
              前往下一步
            </RouterLink>
          </div>
        </div>
        <div v-else class="flex min-h-[360px] items-center justify-center">
          <EmptyState
            icon="chart"
            title="选择一个 Run 查看详情"
            description="这里会显示结果摘要、建议动作、待审批状态和下一步跳转。"
            compact
          />
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { createAgentRunApi, fetchAgentRunDetailApi, fetchAgentRunsApi } from '@/api/agent'
import EmptyState from '@/components/EmptyState.vue'
import { PRODUCT_PAGE_NAMES } from '@/constants/productCopy'
import type { AgentRun } from '@/types/api'

type QuickStart = {
  label: string
  description: string
  agentType: string
  triggerSource: string
  contextRefs: string[]
  userPrompt: string
}

const agentOptions = [
  { label: '协调代理', value: 'coordinator' },
  { label: '学习计划代理', value: 'study_planner' },
  { label: 'JD 备面代理', value: 'job_prep' },
  { label: '录音复盘代理', value: 'recording_review' },
  { label: '面试复盘代理', value: 'interview_review' },
  { label: '简历教练代理', value: 'resume_coach' },
  { label: '投递策略代理', value: 'application_strategist' },
  { label: '实时 Copilot 代理', value: 'realtime_copilot' }
] as const

const triggerOptions = [
  { label: '手动发起', value: 'manual' },
  { label: PRODUCT_PAGE_NAMES.analytics, value: 'analytics' },
  { label: '录音复盘', value: 'recording_review' },
  { label: '实时面试', value: 'interview_live' },
  { label: PRODUCT_PAGE_NAMES.interview, value: 'interview' },
  { label: PRODUCT_PAGE_NAMES.resume, value: 'resume' },
  { label: PRODUCT_PAGE_NAMES.applications, value: 'applications' }
] as const

const contextRefOptions = [
  'analytics:profile',
  'analytics:weak-topics',
  'interview:latest',
  'interview:recording-review',
  'resume:latest',
  'application:board',
  'study-plan:active',
  'settings:providers'
]

const quickStarts: QuickStart[] = [
  {
    label: '从画像刷新训练',
    description: '把长期画像和最近复盘结果转成下一轮训练动作。',
    agentType: 'study_planner',
    triggerSource: 'analytics',
    contextRefs: ['analytics:profile', 'analytics:weak-topics'],
    userPrompt: '根据当前薄弱领域刷新下一轮训练动作。'
  },
  {
    label: '把录音复盘转成任务',
    description: '围绕真实录音的薄弱点整理后续模拟和复习动作。',
    agentType: 'recording_review',
    triggerSource: 'recording_review',
    contextRefs: ['interview:recording-review', 'study-plan:active'],
    userPrompt: '把这次录音复盘的结果转成下一轮训练重点。'
  },
  {
    label: '准备下一场一面',
    description: '把 JD、简历和当前反馈统一成会前准备清单。',
    agentType: 'job_prep',
    triggerSource: 'manual',
    contextRefs: ['resume:latest', 'application:board'],
    userPrompt: '优先准备下周最可能进入一面的岗位。'
  }
]

const form = reactive({
  agentType: 'coordinator',
  triggerSource: 'manual',
  contextRefs: [] as string[],
  streamMode: 'sync',
  userPrompt: ''
})

const runs = ref<AgentRun[]>([])
const selectedRun = ref<AgentRun | null>(null)
const loading = ref(false)
const creating = ref(false)

const applyQuickStart = (item: QuickStart) => {
  form.agentType = item.agentType
  form.triggerSource = item.triggerSource
  form.contextRefs = [...item.contextRefs]
  form.userPrompt = item.userPrompt
}

const loadRuns = async (selectedId?: string) => {
  loading.value = true
  try {
    const response = await fetchAgentRunsApi()
    runs.value = response.data
    const preferred = selectedId
      ? runs.value.find((item) => item.id === selectedId)
      : selectedRun.value
        ? runs.value.find((item) => item.id === selectedRun.value?.id)
        : runs.value[0]
    if (preferred) {
      await selectRun(preferred)
    } else {
      selectedRun.value = null
    }
  } catch {
    runs.value = []
    selectedRun.value = null
    ElMessage.error('无法加载 Agent Run 列表，请稍后重试。')
  } finally {
    loading.value = false
  }
}

const selectRun = async (run: AgentRun) => {
  try {
    const response = await fetchAgentRunDetailApi(run.id)
    selectedRun.value = response.data
  } catch {
    ElMessage.error('无法加载这个 Agent Run 的详情。')
  }
}

const handleCreate = async () => {
  creating.value = true
  try {
    const response = await createAgentRunApi({
      agentType: form.agentType,
      triggerSource: form.triggerSource,
      contextRefs: form.contextRefs,
      streamMode: form.streamMode,
      userPrompt: form.userPrompt.trim() || undefined
    })
    ElMessage.success('Agent Run 已创建。')
    await loadRuns(response.data.id)
  } catch (error: any) {
    ElMessage.error(error?.message || 'Agent Run 创建失败，请稍后重试。')
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  void loadRuns()
})
</script>

<style scoped>
.agent-workbench__legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.agent-launch-card {
  text-align: left;
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background:
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.05), transparent 60%),
    var(--panel-bg);
  padding: 16px;
  transition:
    transform 180ms ease,
    border-color 180ms ease,
    box-shadow 180ms ease;
}

.agent-launch-card:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--bc-accent-rgb), 0.22);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}

.agent-workbench__status-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 14px 16px;
}

.agent-contract-card {
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background:
    radial-gradient(circle at top right, rgba(var(--bc-cyan-rgb), 0.08), transparent 38%),
    var(--panel-bg);
  padding: 18px;
}

.agent-contract-list {
  display: grid;
  gap: 10px;
  padding-left: 18px;
  color: var(--bc-ink-secondary);
}

.agent-contract-list li {
  line-height: 1.7;
}

.agent-run-card {
  width: 100%;
  text-align: left;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-bg);
  padding: 14px;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.agent-run-card:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--bc-accent-rgb), 0.2);
}

.agent-run-card--active {
  border-color: rgba(var(--bc-accent-rgb), 0.24);
  box-shadow: 0 12px 28px rgba(var(--bc-accent-rgb), 0.08);
}

.agent-run-status {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  border-radius: 999px;
  padding: 0 10px;
  font-size: 0.75rem;
  font-weight: 700;
}

.agent-run-status--ready {
  background: rgba(var(--bc-cyan-rgb), 0.12);
  color: var(--bc-ink);
}

.agent-run-status--warning {
  background: rgba(var(--bc-amber-rgb), 0.14);
  color: var(--bc-ink);
}

.agent-detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.agent-detail-block {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-bg);
  padding: 16px;
}

.agent-detail-block__title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.agent-context-pill {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  border-radius: 999px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.18);
  background: rgba(var(--bc-accent-rgb), 0.08);
  padding: 0 11px;
  font-size: 0.74rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.agent-detail-list {
  display: grid;
  gap: 10px;
  padding-left: 18px;
  color: var(--bc-ink-secondary);
}

.agent-detail-list li {
  line-height: 1.7;
}

.agent-detail-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
  justify-content: space-between;
  gap: 12px;
}

.agent-approval-callout {
  flex: 1;
  min-width: 240px;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  padding: 16px;
}

.agent-approval-callout--ready {
  background: rgba(var(--bc-cyan-rgb), 0.08);
}

.agent-approval-callout--warning {
  background: rgba(var(--bc-amber-rgb), 0.1);
}
</style>
