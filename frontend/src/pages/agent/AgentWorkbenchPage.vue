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
          <span class="detail-pill">审批闭环</span>
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
          :key="item.id"
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

        <div class="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-5">
          <div>
            <label class="flat-field-label">筛选 Agent</label>
            <el-select v-model="filters.agentType" class="w-full" clearable placeholder="全部 agent" @change="handleFilterChange">
              <el-option v-for="item in agentOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
          <div>
            <label class="flat-field-label">筛选状态</label>
            <el-select v-model="filters.status" class="w-full" clearable placeholder="全部状态" @change="handleFilterChange">
              <el-option v-for="item in runStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
          <div>
            <label class="flat-field-label">筛选来源</label>
            <el-select v-model="filters.triggerSource" class="w-full" clearable placeholder="全部来源" @change="handleFilterChange">
              <el-option v-for="item in triggerOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
          <div>
            <label class="flat-field-label">审批阶段</label>
            <el-select v-model="filters.approvalStage" class="w-full" clearable placeholder="全部阶段" @change="handleFilterChange">
              <el-option v-for="item in approvalStageOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
          <div>
            <label class="flat-field-label">Provider 状态</label>
            <el-select v-model="filters.providerGateStatus" class="w-full" clearable placeholder="全部依赖状态" @change="handleFilterChange">
              <el-option v-for="item in providerGateFilterOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
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
            :title="hasActiveFilters ? '当前筛选下没有 Run' : '还没有 Agent Run'"
            :description="hasActiveFilters ? '换一个筛选条件，或者先清空筛选再查看全部 run。' : '先从上面的统一入口发起一个任务，这里会开始沉淀最近执行记录。'"
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
                  {{ resolveAgentLabel(run.agentType) }} · {{ resolveTriggerLabel(run.triggerSource) }}
                </p>
              </div>
              <span class="agent-run-status" :class="resolveRunStatusClass(run.status)">
                {{ resolveRunStatusLabel(run.status) }}
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
              <span class="detail-pill">{{ resolveAgentLabel(selectedRun.agentType) }}</span>
              <span class="detail-pill">{{ resolveTriggerLabel(selectedRun.triggerSource) }}</span>
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
              <span
                v-for="item in selectedRun.contextRefs"
                :key="item"
                class="agent-context-pill"
                :title="item"
              >
                {{ resolveContextRefLabel(item) }}
              </span>
            </div>
          </div>

          <div v-if="selectedRun.approvalSummary" class="agent-detail-block">
            <p class="agent-detail-block__title">审批动作</p>
            <p class="mt-2 text-sm leading-6 text-primary">{{ selectedRun.approvalSummary }}</p>
          </div>

          <div v-if="selectedRun.providerGateSummary || selectedRun.providerGates.length" class="agent-detail-block">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p class="agent-detail-block__title">Provider Gating</p>
                <p v-if="selectedRun.providerGateSummary" class="mt-2 text-sm leading-6 text-primary">
                  {{ selectedRun.providerGateSummary }}
                </p>
              </div>
              <span class="agent-run-status" :class="resolveProviderGateClass(selectedRun.providerGateStatus)">
                {{ resolveProviderGateLabel(selectedRun.providerGateStatus) }}
              </span>
            </div>
            <div v-if="selectedRun.providerGates.length" class="mt-4 grid gap-3 md:grid-cols-2">
              <div v-for="item in selectedRun.providerGates" :key="item.scope" class="agent-provider-card">
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <p class="text-sm font-semibold text-ink">{{ item.label }}</p>
                    <p class="mt-1 text-xs uppercase tracking-[0.18em] text-tertiary">{{ item.scope }}</p>
                  </div>
                  <span class="agent-run-status" :class="resolveProviderItemClass(item.status)">
                    {{ item.required ? '关键依赖' : '可选依赖' }}
                  </span>
                </div>
                <p class="mt-3 text-sm leading-6 text-secondary">{{ item.statusMessage }}</p>
              </div>
            </div>
            <div
              v-if="selectedRun.providerGateStatus === 'blocked' || selectedRun.providerGateStatus === 'degraded'"
              class="agent-provider-recovery"
            >
              <div>
                <p class="text-sm font-semibold text-ink">
                  {{ selectedRun.providerGateStatus === 'blocked' ? '建议先补齐关键依赖' : '可以先降级运行，也可以先补齐依赖' }}
                </p>
                <p class="mt-1 text-sm leading-6 text-secondary">
                  {{ resolveProviderRecoveryText(selectedRun.providerGateStatus) }}
                </p>
              </div>
              <div class="flex flex-wrap gap-3">
                <RouterLink to="/settings?tab=providers" class="hard-button-secondary">
                  前往 Provider 设置
                </RouterLink>
                <RouterLink
                  v-if="selectedRun.nextActionPath && selectedRun.nextActionPath !== '/settings?tab=providers'"
                  :to="selectedRun.nextActionPath"
                  class="hard-button-primary"
                >
                  {{ resolveNextActionLabel(selectedRun) }}
                </RouterLink>
              </div>
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

          <div v-if="selectedRun.timeline.length" class="agent-detail-block">
            <p class="agent-detail-block__title">Run Timeline</p>
            <div class="mt-4 space-y-3">
              <div v-for="item in selectedRun.timeline" :key="item.key" class="agent-timeline-item">
                <div class="agent-timeline-item__rail">
                  <span class="agent-timeline-dot" :class="resolveTimelineDotClass(item.status)"></span>
                </div>
                <div class="min-w-0 flex-1">
                  <div class="flex flex-wrap items-center justify-between gap-3">
                    <div class="min-w-0">
                      <div class="flex flex-wrap items-center gap-2">
                        <p class="text-sm font-semibold text-ink">{{ item.title }}</p>
                        <span v-if="item.stepType" class="agent-step-type-pill">
                          {{ resolveTimelineStepTypeLabel(item.stepType) }}
                        </span>
                      </div>
                    </div>
                    <span class="agent-run-status" :class="resolveTimelineStatusClass(item.status)">
                      {{ resolveTimelineStatusLabel(item.status) }}
                    </span>
                  </div>
                  <p class="mt-2 text-sm leading-6 text-secondary">{{ item.description }}</p>
                  <p v-if="item.timestamp" class="mt-2 text-xs text-tertiary">{{ formatDateTime(item.timestamp) }}</p>
                </div>
              </div>
            </div>
          </div>

          <div v-if="selectedRun.executionSummary" class="agent-detail-block">
            <p class="agent-detail-block__title">执行结果</p>
            <p class="mt-2 text-sm leading-6 text-primary">{{ selectedRun.executionSummary }}</p>
          </div>

          <div v-if="selectedRun.decisionNote" class="agent-detail-block">
            <p class="agent-detail-block__title">处理备注</p>
            <p class="mt-2 text-sm leading-6 text-primary">{{ selectedRun.decisionNote }}</p>
          </div>

          <div class="agent-detail-footer">
            <div class="agent-approval-callout" :class="resolveApprovalCalloutClass(selectedRun.status)">
              <p class="text-sm font-semibold text-ink">{{ resolveApprovalTitle(selectedRun) }}</p>
              <p class="mt-1 text-sm leading-6 text-secondary">
                {{ resolveApprovalDescription(selectedRun) }}
              </p>
            </div>
            <div class="agent-detail-actions">
              <el-button
                v-if="selectedRun.status === 'pending_approval'"
                :loading="actionLoading === 'approve'"
                type="primary"
                class="action-button"
                @click="handleDecision('approve')"
              >
                审批通过
              </el-button>
              <el-button
                v-if="selectedRun.status === 'pending_approval'"
                :loading="actionLoading === 'reject'"
                class="action-button"
                @click="handleDecision('reject')"
              >
                拒绝
              </el-button>
              <el-button
                v-if="selectedRun.status !== 'approved' && selectedRun.status !== 'rejected' && selectedRun.status !== 'canceled'"
                :loading="actionLoading === 'cancel'"
                class="action-button"
                @click="handleDecision('cancel')"
              >
                取消 Run
              </el-button>
              <RouterLink v-if="selectedRun.nextActionPath" :to="selectedRun.nextActionPath" class="hard-button-primary">
                {{ resolveNextActionLabel(selectedRun) }}
              </RouterLink>
            </div>
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
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  approveAgentRunApi,
  cancelAgentRunApi,
  createAgentRunApi,
  fetchAgentRunDetailApi,
  fetchAgentRunsApi,
  rejectAgentRunApi,
  type AgentRunListQuery
} from '@/api/agent'
import EmptyState from '@/components/EmptyState.vue'
import { PRODUCT_PAGE_NAMES } from '@/constants/productCopy'
import type { AgentRun } from '@/types/api'

const route = useRoute()

type QuickStart = {
  id: string
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
  { label: '工作台', value: 'dashboard' },
  { label: PRODUCT_PAGE_NAMES.analytics, value: 'analytics' },
  { label: '录音复盘', value: 'recording_review' },
  { label: '实时面试', value: 'interview_live' },
  { label: PRODUCT_PAGE_NAMES.interview, value: 'interview' },
  { label: PRODUCT_PAGE_NAMES.resume, value: 'resume' },
  { label: PRODUCT_PAGE_NAMES.applications, value: 'applications' },
  { label: 'Provider 设置', value: 'settings' }
] as const

const runStatusOptions = [
  { label: '待审批', value: 'pending_approval' },
  { label: '已审批', value: 'approved' },
  { label: '已拒绝', value: 'rejected' },
  { label: '已取消', value: 'canceled' },
  { label: '已完成', value: 'completed' },
  { label: '执行失败', value: 'failed' }
] as const

const approvalStageOptions = [
  { label: '等待审批', value: 'waiting' },
  { label: '已审批', value: 'approved' },
  { label: '已拒绝', value: 'rejected' },
  { label: '已取消', value: 'canceled' },
  { label: '已完成', value: 'completed' },
  { label: '无需审批', value: 'not_required' }
] as const

const providerGateFilterOptions = [
  { label: '关键依赖缺失', value: 'blocked' },
  { label: '可降级运行', value: 'degraded' },
  { label: '依赖已就绪', value: 'ready' },
  { label: '无额外依赖', value: 'not_applicable' }
] as const

const contextRefOptions = [
  'dashboard:overview',
  'analytics:profile',
  'analytics:weak-topics',
  'analytics:topic:{id}',
  'analytics:retrospective:topic:{id}',
  'study-plan:active',
  'interview:latest',
  'interview:session:{id}',
  'interview:recording-review',
  'interview:recording-review:{id}',
  'interview:job-prep',
  'interview:job-prep:{id}',
  'interview:copilot-prep',
  'interview:copilot-prep:{id}',
  'interview:copilot-realtime',
  'interview:copilot-realtime:{id}',
  'resume:latest',
  'resume:{id}',
  'application:board',
  'application:{id}',
  'settings:providers'
]

const quickStarts: QuickStart[] = [
  {
    id: 'profile-refresh',
    label: '从画像刷新训练',
    description: '把长期画像和最近复盘结果转成下一轮训练动作。',
    agentType: 'study_planner',
    triggerSource: 'analytics',
    contextRefs: ['analytics:profile', 'analytics:weak-topics'],
    userPrompt: '根据当前薄弱领域刷新下一轮训练动作。'
  },
  {
    id: 'topic-retrospective',
    label: '把领域回顾落成训练',
    description: '围绕某个 topic 的回顾结果，把风险信号和下一步动作写进训练闭环。',
    agentType: 'study_planner',
    triggerSource: 'analytics',
    contextRefs: ['analytics:profile', 'analytics:topic:{id}', 'analytics:retrospective:topic:{id}', 'study-plan:active'],
    userPrompt: '把这个领域回顾的结论转成正式训练动作和下一轮刷新重点。'
  },
  {
    id: 'recording-review',
    label: '把录音复盘转成任务',
    description: '围绕真实录音的薄弱点整理后续模拟和复习动作。',
    agentType: 'recording_review',
    triggerSource: 'recording_review',
    contextRefs: ['interview:recording-review', 'study-plan:active'],
    userPrompt: '把这次录音复盘的结果转成下一轮训练重点。'
  },
  {
    id: 'job-prep',
    label: '准备下一场一面',
    description: '把 JD、简历和当前反馈统一成会前准备清单。',
    agentType: 'job_prep',
    triggerSource: 'interview',
    contextRefs: ['resume:latest', 'application:board', 'settings:providers'],
    userPrompt: '优先准备下周最可能进入一面的岗位。'
  },
  {
    id: 'copilot-prep',
    label: '把备面转成 Copilot Prep',
    description: '承接 JD 备面结果，快速整理成进入实时阶段前的会前清单。',
    agentType: 'realtime_copilot',
    triggerSource: 'interview',
    contextRefs: ['interview:job-prep', 'settings:providers'],
    userPrompt: '把当前 JD 备面结果转成 Copilot Prep，并明确进入实时阶段前的检查项。'
  },
  {
    id: 'live-post-review',
    label: '收束实时阶段',
    description: '把实时 Copilot 阶段的追问、卡壳点和复盘建议转成面后训练动作。',
    agentType: 'interview_review',
    triggerSource: 'interview_live',
    contextRefs: ['interview:copilot-realtime', 'analytics:profile', 'study-plan:active'],
    userPrompt: '把最近一次实时阶段的现场追问和卡壳点转成面后复盘与训练动作。'
  }
]

const defaultForm: {
  agentType: string
  triggerSource: string
  streamMode: string
  userPrompt: string
} = {
  agentType: 'coordinator',
  triggerSource: 'manual',
  streamMode: 'sync',
  userPrompt: ''
}

const form = reactive({
  ...defaultForm,
  contextRefs: [] as string[]
})

const filters = reactive({
  agentType: '',
  status: '',
  triggerSource: '',
  approvalStage: '',
  providerGateStatus: ''
})

const runs = ref<AgentRun[]>([])
const selectedRun = ref<AgentRun | null>(null)
const loading = ref(false)
const creating = ref(false)
const actionLoading = ref('')

const hasActiveFilters = ref(false)

const parseQueryList = (value: unknown) => {
  if (Array.isArray(value)) {
    return value.flatMap((item) => String(item || '').split(',')).map((item) => item.trim()).filter(Boolean)
  }
  if (typeof value === 'string') {
    return value.split(',').map((item) => item.trim()).filter(Boolean)
  }
  return []
}

const resetForm = () => {
  form.agentType = defaultForm.agentType
  form.triggerSource = defaultForm.triggerSource
  form.contextRefs = []
  form.streamMode = defaultForm.streamMode
  form.userPrompt = defaultForm.userPrompt
}

const syncFiltersFromRoute = () => {
  filters.agentType = typeof route.query.listAgentType === 'string' ? route.query.listAgentType.trim() : ''
  filters.status = typeof route.query.listStatus === 'string' ? route.query.listStatus.trim() : ''
  filters.triggerSource = typeof route.query.listTriggerSource === 'string' ? route.query.listTriggerSource.trim() : ''
  filters.approvalStage = typeof route.query.listApprovalStage === 'string' ? route.query.listApprovalStage.trim() : ''
  filters.providerGateStatus = typeof route.query.listProviderGateStatus === 'string' ? route.query.listProviderGateStatus.trim() : ''
  hasActiveFilters.value = Boolean(
    filters.agentType || filters.status || filters.triggerSource || filters.approvalStage || filters.providerGateStatus
  )
}

const applyRoutePrefill = () => {
  resetForm()
  syncFiltersFromRoute()
  if (typeof route.query.agentType === 'string' && route.query.agentType.trim()) {
    form.agentType = route.query.agentType.trim()
  }
  if (typeof route.query.triggerSource === 'string' && route.query.triggerSource.trim()) {
    form.triggerSource = route.query.triggerSource.trim()
  }
  if (typeof route.query.streamMode === 'string' && route.query.streamMode.trim()) {
    form.streamMode = route.query.streamMode.trim()
  }
  if (typeof route.query.userPrompt === 'string') {
    form.userPrompt = route.query.userPrompt
  }
  const contextRefs = parseQueryList(route.query.contextRefs)
  if (contextRefs.length) {
    form.contextRefs = contextRefs
  }
}

const applyQuickStart = (item: QuickStart) => {
  form.agentType = item.agentType
  form.triggerSource = item.triggerSource
  form.contextRefs = [...item.contextRefs]
  form.userPrompt = item.userPrompt
}

const buildRunListQuery = (): AgentRunListQuery | undefined => {
  const query: AgentRunListQuery = {}
  if (filters.agentType) query.agentType = filters.agentType
  if (filters.status) query.status = filters.status
  if (filters.triggerSource) query.triggerSource = filters.triggerSource
  if (filters.approvalStage) query.approvalStage = filters.approvalStage
  if (filters.providerGateStatus) query.providerGateStatus = filters.providerGateStatus
  return Object.keys(query).length ? query : undefined
}

const loadRuns = async (selectedId?: string) => {
  loading.value = true
  try {
    const response = await fetchAgentRunsApi(buildRunListQuery())
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

const handleFilterChange = async () => {
  hasActiveFilters.value = Boolean(
    filters.agentType || filters.status || filters.triggerSource || filters.approvalStage || filters.providerGateStatus
  )
  await loadRuns()
}

const selectRun = async (run: AgentRun) => {
  try {
    const response = await fetchAgentRunDetailApi(run.id)
    selectedRun.value = response.data
  } catch {
    ElMessage.error('无法加载这个 Agent Run 的详情。')
  }
}

const resolveRunStatusLabel = (status: string) => {
  switch (status) {
    case 'pending_approval':
      return '待审批'
    case 'approved':
      return '已审批'
    case 'rejected':
      return '已拒绝'
    case 'canceled':
      return '已取消'
    case 'failed':
      return '执行失败'
    default:
      return '已完成'
  }
}

const resolveRunStatusClass = (status: string) => {
  switch (status) {
    case 'pending_approval':
      return 'agent-run-status--warning'
    case 'rejected':
    case 'failed':
      return 'agent-run-status--danger'
    case 'canceled':
      return 'agent-run-status--neutral'
    default:
      return 'agent-run-status--ready'
  }
}

const resolveAgentLabel = (agentType: string) => {
  return agentOptions.find((item) => item.value === agentType)?.label || agentType
}

const resolveTriggerLabel = (triggerSource: string) => {
  return triggerOptions.find((item) => item.value === triggerSource)?.label || triggerSource
}

const resolveContextRefLabel = (contextRef: string) => {
  if (contextRef === 'dashboard:overview') return '工作台总览'
  if (contextRef === 'analytics:profile') return '能力画像'
  if (contextRef === 'analytics:weak-topics') return '薄弱主题'
  if (contextRef === 'study-plan:active') return '当前学习计划'
  if (contextRef === 'interview:latest') return '最近一次模拟面试'
  if (contextRef === 'interview:recording-review') return '最近一次录音复盘'
  if (contextRef === 'interview:job-prep') return '最近一次 JD 备面'
  if (contextRef === 'interview:copilot-prep') return '最近一次 Copilot Prep'
  if (contextRef === 'interview:copilot-realtime') return '最近一次实时 Copilot'
  if (contextRef === 'resume:latest') return '最新简历'
  if (contextRef === 'application:board') return '投递看板'
  if (contextRef === 'settings:providers') return 'Provider 配置'
  if (contextRef.startsWith('analytics:topic:')) return `主题详情 #${contextRef.slice('analytics:topic:'.length)}`
  if (contextRef.startsWith('analytics:retrospective:topic:')) {
    return `领域回顾 #${contextRef.slice('analytics:retrospective:topic:'.length)}`
  }
  if (contextRef.startsWith('interview:session:')) return `模拟面试 #${contextRef.slice('interview:session:'.length)}`
  if (contextRef.startsWith('interview:recording-review:')) {
    return `录音复盘 #${contextRef.slice('interview:recording-review:'.length)}`
  }
  if (contextRef.startsWith('interview:job-prep:')) return `JD 备面 #${contextRef.slice('interview:job-prep:'.length)}`
  if (contextRef.startsWith('interview:copilot-prep:')) {
    return `Copilot Prep #${contextRef.slice('interview:copilot-prep:'.length)}`
  }
  if (contextRef.startsWith('interview:copilot-realtime:')) {
    return `实时 Copilot #${contextRef.slice('interview:copilot-realtime:'.length)}`
  }
  if (contextRef.startsWith('resume:')) return `简历 #${contextRef.slice('resume:'.length)}`
  if (contextRef.startsWith('application:')) return `岗位投递 #${contextRef.slice('application:'.length)}`
  return contextRef
}

const resolveApprovalTitle = (run: AgentRun) => {
  switch (run.status) {
    case 'pending_approval':
      return '当前等待审批'
    case 'approved':
      return '审批已经通过'
    case 'rejected':
      return '审批已被拒绝'
    case 'canceled':
      return '当前 run 已取消'
    default:
      return run.requiresApproval ? '当前无需再审批' : '当前无需审批'
  }
}

const resolveApprovalDescription = (run: AgentRun) => {
  switch (run.status) {
    case 'pending_approval':
      return '你可以在这里决定是否把这次 run 的结果转成正式写操作。'
    case 'approved':
      return run.executionSummary || '审批动作已经执行完成，可以继续去目标页面消费结果。'
    case 'rejected':
      return '这次写操作没有执行，run 结果仍可继续参考。'
    case 'canceled':
      return '当前 run 已停止推进，不会继续执行后续动作。'
    default:
      return run.requiresApproval ? '这次写操作已经结束，可以直接继续下一步。' : '下一步可以直接跳转到结果消费页继续执行。'
  }
}

const resolveApprovalCalloutClass = (status: string) => {
  switch (status) {
    case 'pending_approval':
      return 'agent-approval-callout--warning'
    case 'rejected':
    case 'failed':
      return 'agent-approval-callout--danger'
    case 'canceled':
      return 'agent-approval-callout--neutral'
    default:
      return 'agent-approval-callout--ready'
  }
}

const resolveProviderGateLabel = (status?: string) => {
  switch (status) {
    case 'blocked':
      return '关键依赖缺失'
    case 'degraded':
      return '可降级运行'
    case 'ready':
      return '依赖已就绪'
    default:
      return '无额外依赖'
  }
}

const resolveProviderGateClass = (status?: string) => {
  switch (status) {
    case 'blocked':
      return 'agent-run-status--danger'
    case 'degraded':
      return 'agent-run-status--warning'
    case 'ready':
      return 'agent-run-status--ready'
    default:
      return 'agent-run-status--neutral'
  }
}

const resolveProviderItemClass = (status: string) => {
  switch (status) {
    case 'missing':
    case 'incomplete':
      return 'agent-run-status--danger'
    case 'saved':
      return 'agent-run-status--warning'
    default:
      return 'agent-run-status--ready'
  }
}

const resolveProviderRecoveryText = (status?: string) => {
  if (status === 'blocked') {
    return '当前 run 依赖的关键服务尚未配置完整。先补齐这里的模型、转写或外部服务配置，再重新发起会更稳妥。'
  }
  return '当前 run 可以继续，但部分增强能力会缺失。若要拿到更完整的备面、复盘或实时建议，建议先补齐依赖。'
}

const resolveTimelineStatusLabel = (status: string) => {
  switch (status) {
    case 'waiting':
      return '等待中'
    case 'ready':
      return '可执行'
    case 'rejected':
      return '已拒绝'
    case 'canceled':
      return '已取消'
    default:
      return '已完成'
  }
}

const resolveTimelineStatusClass = (status: string) => {
  switch (status) {
    case 'waiting':
      return 'agent-run-status--warning'
    case 'rejected':
      return 'agent-run-status--danger'
    case 'canceled':
      return 'agent-run-status--neutral'
    case 'ready':
      return 'agent-run-status--ready'
    default:
      return 'agent-run-status--ready'
  }
}

const resolveTimelineStepTypeLabel = (stepType?: string) => {
  switch (stepType) {
    case 'retrieve':
      return '取上下文'
    case 'score':
      return '评分整理'
    case 'update_profile':
      return '写回画像'
    case 'schedule_review':
      return '训练调度'
    case 'prepare_realtime':
      return '实时准备'
    case 'wait_transcription':
      return '等待转写'
    case 'wait_approval':
      return '等待审批'
    default:
      return '分析'
  }
}

const resolveTimelineDotClass = (status: string) => {
  switch (status) {
    case 'waiting':
      return 'agent-timeline-dot--warning'
    case 'rejected':
      return 'agent-timeline-dot--danger'
    case 'canceled':
      return 'agent-timeline-dot--neutral'
    case 'ready':
      return 'agent-timeline-dot--ready'
    default:
      return 'agent-timeline-dot--done'
  }
}

const formatDateTime = (value?: string) => (value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '刚刚')

const resolveNextActionLabel = (run: AgentRun) => {
  if (run.nextActionLabel?.trim()) {
    return run.nextActionLabel.trim()
  }
  if (run.providerGateStatus === 'blocked') {
    return '前往 Provider 设置'
  }
  switch (run.agentType) {
    case 'study_planner':
      return '前往训练计划'
    case 'job_prep':
      return '前往 JD 备面'
    case 'recording_review':
      return '前往录音复盘'
    case 'interview_review':
      return '前往面试复盘'
    case 'resume_coach':
      return '前往简历页'
    case 'application_strategist':
      return '前往投递页'
    case 'realtime_copilot':
      return '前往 Copilot'
    default:
      return '前往下一步'
  }
}

const handleDecision = async (decision: 'approve' | 'reject' | 'cancel') => {
  if (!selectedRun.value) return
  actionLoading.value = decision
  try {
    const api =
      decision === 'approve'
        ? approveAgentRunApi
        : decision === 'reject'
          ? rejectAgentRunApi
          : cancelAgentRunApi
    const response = await api(selectedRun.value.id)
    selectedRun.value = response.data
    ElMessage.success(
      decision === 'approve' ? '审批已通过。' : decision === 'reject' ? '已拒绝这次写操作。' : '当前 run 已取消。'
    )
    await loadRuns(response.data.id)
  } catch (error: any) {
    ElMessage.error(error?.message || '处理当前 run 失败，请稍后重试。')
  } finally {
    actionLoading.value = ''
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
  applyRoutePrefill()
  void loadRuns()
})

watch(() => route.fullPath, () => {
  applyRoutePrefill()
  void loadRuns(selectedRun.value?.id)
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

.agent-run-status--danger {
  background: rgba(224, 73, 73, 0.12);
  color: #842029;
}

.agent-run-status--neutral {
  background: rgba(148, 163, 184, 0.16);
  color: #475569;
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

.agent-provider-card {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 14px;
}

.agent-provider-recovery {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 16px;
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px dashed rgba(var(--bc-accent-rgb), 0.28);
  background: rgba(var(--bc-accent-rgb), 0.05);
  padding: 14px;
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

.agent-timeline-item {
  display: flex;
  gap: 12px;
}

.agent-timeline-item__rail {
  display: flex;
  justify-content: center;
  padding-top: 4px;
}

.agent-timeline-dot {
  display: inline-flex;
  width: 11px;
  height: 11px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.35);
}

.agent-timeline-dot--done {
  background: rgba(var(--bc-cyan-rgb), 0.72);
}

.agent-timeline-dot--ready {
  background: rgba(var(--bc-cyan-rgb), 0.9);
}

.agent-timeline-dot--warning {
  background: rgba(var(--bc-amber-rgb), 0.88);
}

.agent-timeline-dot--danger {
  background: rgba(224, 73, 73, 0.88);
}

.agent-timeline-dot--neutral {
  background: rgba(148, 163, 184, 0.72);
}

.agent-step-type-pill {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: rgba(var(--bc-accent-rgb), 0.1);
  color: var(--text-secondary);
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
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

.agent-approval-callout--danger {
  background: rgba(224, 73, 73, 0.08);
}

.agent-approval-callout--neutral {
  background: rgba(148, 163, 184, 0.1);
}

.agent-detail-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}
</style>
