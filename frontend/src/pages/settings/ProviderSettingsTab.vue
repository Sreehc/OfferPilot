<template>
  <div class="space-y-5">
    <section class="provider-intro shell-section-card p-5 sm:p-6">
      <p class="section-kicker">能力依赖</p>
      <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
        管理模型、语音和外部服务配置
      </h3>
      <p class="mt-3 max-w-3xl text-sm leading-7 text-secondary">
        这些配置按当前账号独立保存。后续的知识库、JD 备面、录音复盘和实时 Copilot
        都会先检查这里的配置完整性，再决定是否允许继续。
      </p>
      <div class="mt-4 flex justify-end">
        <el-button
          size="large"
          class="action-button"
          :loading="checking"
          @click="handleCheck"
        >
          {{ checking ? '检测中...' : '重新检测状态' }}
        </el-button>
      </div>
    </section>

    <section v-if="providerReturnPath" class="shell-section-card p-5 sm:p-6 provider-return-card">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p class="section-kicker">恢复链路</p>
          <h3 class="mt-2 text-2xl font-semibold tracking-[-0.03em] text-ink">
            补齐依赖后继续原来的工作区
          </h3>
          <p class="mt-2 max-w-3xl text-sm leading-7 text-secondary">
            你是从受 provider 影响的链路进入这里的。保存或重新检测完成后，可以直接回到原页面继续 JD 备面、录音复盘、实时 Copilot 或 Agent Run。
          </p>
        </div>
        <RouterLink :to="providerReturnPath" class="hard-button-primary">
          {{ providerReturnLabel }}
        </RouterLink>
      </div>
    </section>

    <section v-if="!loading" class="shell-section-card p-5 sm:p-6">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p class="section-kicker">能力影响</p>
          <h3 class="mt-2 text-2xl font-semibold tracking-[-0.03em] text-ink">
            当前配置会影响哪些能力
          </h3>
          <p class="mt-2 max-w-3xl text-sm leading-7 text-secondary">
            这里直接映射训练闭环、JD 备面、录音复盘和实时 Copilot 的可用性，避免只看到 provider 状态却不知道会影响哪条链路。
          </p>
        </div>
        <div class="provider-capability-summary">
          <span class="detail-pill">可用 {{ capabilityStatusSummary.ready }}</span>
          <span class="detail-pill">降级 {{ capabilityStatusSummary.degraded }}</span>
          <span class="detail-pill">待补齐 {{ capabilityStatusSummary.blocked }}</span>
        </div>
      </div>

      <div class="provider-capability-grid mt-5">
        <article
          v-for="capability in capabilityCards"
          :key="capability.key"
          class="provider-capability-card"
          :class="`provider-capability-card--${capability.status}`"
        >
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p class="text-sm font-semibold text-ink">{{ capability.label }}</p>
              <p class="mt-2 text-sm leading-6 text-secondary">{{ capability.description }}</p>
            </div>
            <span class="provider-status-badge" :class="`provider-status-badge--${capability.status}`">
              {{ capabilityCardStatusLabel(capability.status) }}
            </span>
          </div>

          <div class="mt-4 flex flex-wrap gap-2">
            <span
              v-for="scope in capability.providerScopes"
              :key="`${capability.key}-${scope}`"
              class="provider-impact-pill"
              :class="providerScopePillClass(scope, capability.requiredScopes.includes(scope))"
            >
              {{ providerScopeLabel(scope) }}{{ capability.requiredScopes.includes(scope) ? ' / 关键' : ' / 增强' }}
            </span>
          </div>

          <p class="mt-4 text-sm leading-6 text-primary">{{ capability.summary }}</p>

          <ul v-if="capability.gaps.length" class="provider-impact-list mt-3">
            <li v-for="gap in capability.gaps" :key="gap">{{ gap }}</li>
          </ul>

          <div class="provider-capability-card__actions">
            <RouterLink :to="capability.agentLaunchPath" class="hard-button-primary">
              {{ capability.agentLaunchLabel }}
            </RouterLink>
            <RouterLink :to="capability.reviewRunsPath" class="hard-button-secondary">
              {{ capability.reviewRunsLabel }}
            </RouterLink>
          </div>
        </article>
      </div>
    </section>

    <div v-if="loading" class="provider-grid">
      <article
        v-for="item in 3"
        :key="item"
        class="shell-section-card p-5 sm:p-6"
      >
        <div class="space-y-3">
          <div class="h-4 w-24 rounded-full bg-[rgba(var(--bc-ink-rgb),0.08)]" />
          <div class="h-8 w-48 rounded-full bg-[rgba(var(--bc-ink-rgb),0.08)]" />
          <div class="h-20 rounded-3xl bg-[rgba(var(--bc-ink-rgb),0.05)]" />
        </div>
      </article>
    </div>

    <div v-else class="provider-grid">
      <article
        v-for="item in configs"
        :key="item.scope"
        class="shell-section-card p-5 sm:p-6 provider-card"
      >
        <div class="provider-card__head">
          <div>
            <p class="section-kicker">Provider</p>
            <h3 class="provider-card__title">{{ item.label }}</h3>
            <p class="provider-card__description">{{ item.description }}</p>
          </div>
          <div class="provider-card__status">
            <span class="provider-status-badge" :class="`provider-status-badge--${item.status}`">
              {{ statusLabel(item.status) }}
            </span>
            <p class="provider-card__status-text">{{ item.statusMessage }}</p>
          </div>
        </div>

        <div class="provider-card__form">
          <el-switch
            v-model="ensureDraft(item.scope).enabled"
            inline-prompt
            active-text="启用"
            inactive-text="停用"
          />

          <div v-if="needsProviderName(item.scope)" class="provider-field">
            <label>服务商</label>
            <el-input v-model="ensureDraft(item.scope).providerName" placeholder="例如 DashScope、Tavily、Tencent VPR" />
          </div>

          <div v-if="needsBaseUrl(item.scope)" class="provider-field">
            <label>Base URL</label>
            <el-input v-model="ensureDraft(item.scope).baseUrl" :placeholder="baseUrlPlaceholder(item.scope)" />
          </div>

          <div v-if="needsModel(item.scope)" class="provider-field">
            <label>模型</label>
            <el-input v-model="ensureDraft(item.scope).model" placeholder="例如 gpt-4.1-mini 或 text-embedding-3-small" />
          </div>

          <div v-if="item.scope === 'embedding'" class="provider-field">
            <label>向量维度</label>
            <el-input-number v-model="ensureDraft(item.scope).dimensions" :min="1" :max="16384" :step="1" />
          </div>

          <div v-if="needsApiKey(item.scope)" class="provider-field">
            <label>API Key</label>
            <el-input
              v-model="ensureDraft(item.scope).apiKey"
              type="password"
              show-password
              placeholder="留空则保留现有密钥"
            />
            <div class="provider-secret-row">
              <span class="text-xs text-secondary">
                当前：{{ item.apiKeyMasked || '未保存' }}
              </span>
              <el-checkbox v-model="ensureDraft(item.scope).clearApiKey">清除现有 API Key</el-checkbox>
            </div>
          </div>

          <template v-if="item.scope === 'oss'">
            <div class="provider-field">
              <label>Endpoint</label>
              <el-input v-model="ensureDraft(item.scope).endpoint" placeholder="https://oss-cn-hangzhou.aliyuncs.com" />
            </div>
            <div class="provider-field">
              <label>Bucket</label>
              <el-input v-model="ensureDraft(item.scope).bucket" placeholder="offerpilot-audio" />
            </div>
            <div class="provider-field">
              <label>区域</label>
              <el-input v-model="ensureDraft(item.scope).regionName" placeholder="例如 cn-hangzhou" />
            </div>
            <div class="provider-field">
              <label>Access Key</label>
              <el-input
                v-model="ensureDraft(item.scope).accessKey"
                type="password"
                show-password
                placeholder="留空则保留现有 Access Key"
              />
              <div class="provider-secret-row">
                <span class="text-xs text-secondary">
                  当前：{{ item.accessKeyMasked || '未保存' }}
                </span>
                <el-checkbox v-model="ensureDraft(item.scope).clearAccessKey">清除现有 Access Key</el-checkbox>
              </div>
            </div>
            <div class="provider-field">
              <label>Secret Key</label>
              <el-input
                v-model="ensureDraft(item.scope).secretKey"
                type="password"
                show-password
                placeholder="留空则保留现有 Secret Key"
              />
              <div class="provider-secret-row">
                <span class="text-xs text-secondary">
                  当前：{{ item.secretKeyMasked || '未保存' }}
                </span>
                <el-checkbox v-model="ensureDraft(item.scope).clearSecretKey">清除现有 Secret Key</el-checkbox>
              </div>
            </div>
          </template>
        </div>

        <div class="provider-card__foot">
          <div class="text-xs leading-6 text-secondary">
            <p>最近检查：{{ formatDateTime(item.lastCheckedAt) }}</p>
            <p v-if="item.lastCheckMessage">{{ item.lastCheckMessage }}</p>
          </div>

          <el-button
            type="primary"
            size="large"
            class="action-button"
            :loading="savingScope === item.scope"
            @click="handleSave(item.scope)"
          >
            {{ savingScope === item.scope ? '保存中...' : '保存配置' }}
          </el-button>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, type RouteLocationRaw } from 'vue-router'
import {
  checkProviderConfigsApi,
  fetchProviderConfigsApi,
  updateProviderConfigsApi,
  type ProviderConfigUpdateItemPayload
} from '@/api/settings'
import type { ProviderScope, UserProviderConfigItem } from '@/types/api'

type ProviderDraft = ProviderConfigUpdateItemPayload
type CapabilityCardStatus = 'ready' | 'degraded' | 'missing'
type CapabilityCard = {
  key: string
  label: string
  description: string
  agentType: string
  providerScopes: ProviderScope[]
  requiredScopes: ProviderScope[]
  status: CapabilityCardStatus
  summary: string
  gaps: string[]
  agentLaunchLabel: string
  agentLaunchPath: RouteLocationRaw
  reviewRunsLabel: string
  reviewRunsPath: RouteLocationRaw
}

const route = useRoute()
const loading = ref(true)
const savingScope = ref<ProviderScope | ''>('')
const checking = ref(false)
const configs = ref<UserProviderConfigItem[]>([])
const draftByScope = reactive<Record<string, ProviderDraft>>({})

const providerReturnPath = computed<string | null>(() => {
  const value = route.query.returnTo
  if (typeof value !== 'string') return null
  const normalized = value.trim()
  if (!normalized.startsWith('/')) return null
  return normalized
})

const providerReturnLabel = computed(() => {
  const value = route.query.returnLabel
  if (typeof value === 'string' && value.trim()) {
    return value.trim()
  }
  return '返回上一条链路'
})

const defaultDraft = (scope: ProviderScope): ProviderDraft => ({
  scope,
  enabled: false,
  providerName: '',
  baseUrl: '',
  model: '',
  apiKey: '',
  clearApiKey: false,
  accessKey: '',
  clearAccessKey: false,
  secretKey: '',
  clearSecretKey: false,
  endpoint: '',
  bucket: '',
  regionName: '',
  dimensions: null
})

const fillDraft = (item: UserProviderConfigItem) => {
  draftByScope[item.scope] = {
    ...defaultDraft(item.scope),
    enabled: item.enabled,
    providerName: item.providerName || '',
    baseUrl: item.baseUrl || '',
    model: item.model || '',
    endpoint: item.endpoint || '',
    bucket: item.bucket || '',
    regionName: item.regionName || '',
    dimensions: item.dimensions ?? null
  }
}

const ensureDraft = (scope: ProviderScope) => {
  if (!draftByScope[scope]) {
    draftByScope[scope] = defaultDraft(scope)
  }
  return draftByScope[scope]
}

const providerConfigMap = computed<Record<ProviderScope, UserProviderConfigItem | undefined>>(() => {
  const entries = configs.value.map((item) => [item.scope, item] as const)
  return Object.fromEntries(entries) as Record<ProviderScope, UserProviderConfigItem | undefined>
})

const isProviderReadyForCapability = (scope: ProviderScope) => {
  const item = providerConfigMap.value[scope]
  return item?.status === 'ready' || item?.status === 'saved'
}

const providerScopeLabel = (scope: ProviderScope) => {
  switch (scope) {
    case 'llm':
      return '主模型'
    case 'embedding':
      return '向量检索'
    case 'asr':
      return '语音识别'
    case 'search':
      return '联网搜索'
    case 'oss':
      return '对象存储'
    case 'voiceprint':
      return '声纹识别'
    default:
      return scope
  }
}

const buildInterviewWorkspaceLink = (
  workspace: 'job-prep' | 'recording-review' | 'copilot-prep' | 'copilot-live',
  note: string
): RouteLocationRaw => ({
  path: '/interview',
  query: {
    workspace,
    seedWorkflow: workspace,
    seedNote: note
  }
})

const buildAgentLaunchPath = (
  agentType: string,
  triggerSource: string,
  contextRefs: string[],
  userPrompt: string
): RouteLocationRaw => ({
  path: '/agent',
  query: {
    agentType,
    triggerSource,
    contextRefs,
    userPrompt
  }
})

const buildCapabilityCard = (
  key: string,
  label: string,
  description: string,
  agentType: string,
  providerScopes: ProviderScope[],
  requiredScopes: ProviderScope[],
  fallbackReadySummary: string,
  degradedSummary: string,
  blockedSummary: string,
  agentLaunchLabel: string,
  agentLaunchPath: RouteLocationRaw
): CapabilityCard => {
  const missingRequired = requiredScopes.filter((scope) => !isProviderReadyForCapability(scope))
  const optionalScopes = providerScopes.filter((scope) => !requiredScopes.includes(scope))
  const missingOptional = optionalScopes.filter((scope) => !isProviderReadyForCapability(scope))
  const reviewRunsPath = buildAgentRunReviewPath(agentType, missingRequired.length ? 'blocked' : missingOptional.length ? 'degraded' : '')
  const reviewRunsLabel = missingRequired.length || missingOptional.length ? '查看受影响 Run' : '查看相关 Run'
  const gaps = [
    ...missingRequired.map((scope) => `${providerScopeLabel(scope)} 还没就绪，会直接阻断这条能力链路。`),
    ...missingOptional.map((scope) => `${providerScopeLabel(scope)} 未配置，能力可继续但会降级。`)
  ]

  if (missingRequired.length) {
    return {
      key,
      label,
      description,
      agentType,
      providerScopes,
      requiredScopes,
      status: 'missing',
      summary: blockedSummary,
      gaps,
      agentLaunchLabel,
      agentLaunchPath,
      reviewRunsLabel,
      reviewRunsPath
    }
  }
  if (missingOptional.length) {
    return {
      key,
      label,
      description,
      agentType,
      providerScopes,
      requiredScopes,
      status: 'degraded',
      summary: degradedSummary,
      gaps,
      agentLaunchLabel,
      agentLaunchPath,
      reviewRunsLabel,
      reviewRunsPath
    }
  }
  return {
    key,
    label,
    description,
    agentType,
    providerScopes,
    requiredScopes,
    status: 'ready',
    summary: fallbackReadySummary,
    gaps,
    agentLaunchLabel,
    agentLaunchPath,
    reviewRunsLabel,
    reviewRunsPath
  }
}

const buildRecordingReviewCapabilityCard = (): CapabilityCard => {
  const llmReady = isProviderReadyForCapability('llm')
  const asrReady = isProviderReadyForCapability('asr')
  const ossReady = isProviderReadyForCapability('oss')
  const status: CapabilityCardStatus = !llmReady ? 'missing' : asrReady && ossReady ? 'ready' : 'degraded'
  const gaps = [
    ...(!llmReady ? ['主模型还没就绪，会直接阻断录音复盘和 transcript 复盘生成。'] : []),
    ...(!asrReady ? ['语音识别未配置时，音频上传会被禁用，但仍可改用文字 transcript 模式继续复盘。'] : []),
    ...(!ossReady ? ['对象存储未配置，长音频上传、回放和存储承载能力会降级。'] : [])
  ]

  let summary = '当前录音复盘链路可完整使用，包括音频转写、复盘和训练建议。'
  if (!llmReady) {
    summary = '主模型未就绪，录音复盘当前无法稳定生成音频或 transcript 复盘结果。'
  } else if (!asrReady && !ossReady) {
    summary = '录音复盘仍可继续，但会退化为文字 transcript 模式，且长音频承载能力也会受限。'
  } else if (!asrReady) {
    summary = '录音复盘仍可继续，但音频上传会被禁用，建议先改用文字 transcript 模式。'
  } else if (!ossReady) {
    summary = '录音复盘可继续，但长音频存储和上传承载能力会降级。'
  }

  return {
    key: 'recording_review',
    label: '录音复盘',
    description: '把真实面试录音或手动 transcript 转成转写、弱点提炼、训练动作和画像回写。',
    agentType: 'recording_review',
    providerScopes: ['llm', 'asr', 'oss'],
    requiredScopes: ['llm'],
    status,
    summary,
    gaps,
    agentLaunchLabel: '进入录音复盘工作区',
    agentLaunchPath: buildInterviewWorkspaceLink(
      'recording-review',
      '当前从 Provider 设置进入，先确认是否走音频上传还是文字 transcript 模式，再继续录音复盘。'
    ),
    reviewRunsLabel: status === 'ready' ? '查看相关 Run' : '查看受影响 Run',
    reviewRunsPath: buildAgentRunReviewPath('recording_review', status === 'missing' ? 'blocked' : status === 'degraded' ? 'degraded' : '')
  }
}

const buildAgentRunReviewPath = (agentType: string, providerGateStatus: string): RouteLocationRaw => {
  const params = new URLSearchParams()
  params.set('listAgentType', agentType)
  if (providerGateStatus) {
    params.set('listProviderGateStatus', providerGateStatus)
  }
  return { path: '/agent', query: Object.fromEntries(params) }
}

const capabilityCards = computed<CapabilityCard[]>(() => [
  buildCapabilityCard(
    'job_prep',
    'JD 备面',
    '围绕岗位 JD、简历和投递上下文生成针对性的备面重点与会前清单。',
    'job_prep',
    ['llm', 'search'],
    ['llm'],
    '当前 JD 备面链路可完整使用，包括岗位研究和会前重点整理。',
    'JD 备面仍可运行，但公司背景研究和岗位情报会降级。',
    '主模型未就绪，JD 备面当前无法生成有效结果。',
    '进入 JD 备面工作区',
    buildInterviewWorkspaceLink('job-prep', '当前从 Provider 设置进入，先检查依赖恢复后再继续 JD 备面。')
  ),
  buildRecordingReviewCapabilityCard(),
  buildCapabilityCard(
    'realtime_copilot',
    '实时 Copilot',
    '承接 Copilot Prep、实时追问辅助和面后复盘入口，是最依赖 provider 完整性的链路。',
    'realtime_copilot',
    ['llm', 'asr', 'search', 'voiceprint'],
    ['llm', 'asr', 'search'],
    '当前实时 Copilot 可完整使用，包括现场转写、背景检索和追问辅助。',
    '实时 Copilot 仍可进入，但说话人区分或部分增强能力会降级。',
    '实时 Copilot 缺少关键依赖，当前不建议进入实时阶段。',
    '进入 Copilot 工作区',
    buildInterviewWorkspaceLink('copilot-live', '当前从 Provider 设置进入，先确认实时依赖恢复后再继续实时 Copilot。')
  ),
  buildCapabilityCard(
    'profile_loop',
    '训练画像与长期闭环',
    '支撑训练结果回写、知识检索、画像分析和下一轮训练刷新。',
    'study_planner',
    ['llm', 'embedding'],
    ['llm'],
    '当前训练画像与长期闭环已具备基础能力，可继续承接训练结果和画像分析。',
    '训练画像可继续，但知识检索和向量召回能力会降级。',
    '主模型未就绪，画像分析和下一轮训练刷新无法稳定生成。',
    '发起训练计划代理',
    buildAgentLaunchPath(
      'study_planner',
      'analytics',
      ['analytics:profile', 'analytics:weak-topics', 'settings:providers'],
      '当前从 Provider 设置进入，优先检查依赖恢复后再刷新训练画像和下一轮计划。'
    )
  )
])

const capabilityStatusSummary = computed(() => capabilityCards.value.reduce((summary, item) => {
  if (item.status === 'ready') summary.ready += 1
  else if (item.status === 'degraded') summary.degraded += 1
  else summary.blocked += 1
  return summary
}, { ready: 0, degraded: 0, blocked: 0 }))

const loadConfigs = async () => {
  loading.value = true
  try {
    const { data } = await fetchProviderConfigsApi()
    configs.value = data
    data.forEach(fillDraft)
  } catch {
    ElMessage.error('无法加载 provider 配置，请刷新后重试。')
  } finally {
    loading.value = false
  }
}

const handleSave = async (scope: ProviderScope) => {
  const draft = ensureDraft(scope)
  savingScope.value = scope
  try {
    const { data } = await updateProviderConfigsApi([draft])
    configs.value = data
    data.forEach(fillDraft)
    ElMessage.success('配置已保存')
  } catch {
    ElMessage.error('配置保存失败，请检查字段后重试。')
  } finally {
    savingScope.value = ''
  }
}

const handleCheck = async () => {
  checking.value = true
  try {
    const { data } = await checkProviderConfigsApi()
    configs.value = data
    data.forEach(fillDraft)
    ElMessage.success('已刷新 provider 状态')
  } catch {
    ElMessage.error('状态检测失败，请稍后重试。')
  } finally {
    checking.value = false
  }
}

const needsProviderName = (scope: ProviderScope) => ['asr', 'search', 'voiceprint'].includes(scope)
const needsBaseUrl = (scope: ProviderScope) => ['llm', 'embedding', 'asr', 'search', 'voiceprint'].includes(scope)
const needsModel = (scope: ProviderScope) => ['llm', 'embedding'].includes(scope)
const needsApiKey = (scope: ProviderScope) => scope !== 'oss'

const baseUrlPlaceholder = (scope: ProviderScope) => {
  switch (scope) {
    case 'llm':
    case 'embedding':
      return 'https://api.example.com/v1'
    case 'asr':
      return 'https://api.example.com/v1'
    case 'search':
      return 'https://api.search.example.com'
    case 'voiceprint':
      return 'https://api.voiceprint.example.com'
    default:
      return 'https://api.example.com'
  }
}

const statusLabel = (status: string) => {
  if (status === 'ready') return '已可用'
  if (status === 'saved') return '已保存'
  if (status === 'failed') return '检测失败'
  if (status === 'incomplete') return '待补齐'
  return '未配置'
}

const capabilityCardStatusLabel = (status: CapabilityCardStatus) => {
  if (status === 'ready') return '可完整使用'
  if (status === 'degraded') return '可降级运行'
  return '待补齐'
}

const providerScopePillClass = (scope: ProviderScope, required: boolean) => {
  const status = isProviderReadyForCapability(scope) ? 'ready' : required ? 'missing' : 'degraded'
  return `provider-impact-pill--${status}`
}

const formatDateTime = (value?: string) => {
  if (!value) return '尚未检查'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

onMounted(loadConfigs)
</script>

<style scoped>
.provider-intro {
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.14), transparent 36%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(246, 248, 255, 0.92));
}

.provider-grid {
  display: grid;
  gap: 1rem;
}

.provider-return-card {
  background:
    linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.1), rgba(255, 255, 255, 0.92)),
    var(--panel-bg);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.16);
}

.provider-capability-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.provider-capability-grid {
  display: grid;
  gap: 1rem;
}

.provider-capability-card {
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-bg);
  padding: 1rem;
}

.provider-capability-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1rem;
}

.provider-capability-card--ready {
  background:
    linear-gradient(180deg, rgba(41, 163, 110, 0.06), transparent 52%),
    var(--panel-bg);
}

.provider-capability-card--degraded {
  background:
    linear-gradient(180deg, rgba(203, 143, 33, 0.08), transparent 52%),
    var(--panel-bg);
}

.provider-capability-card--missing {
  background:
    linear-gradient(180deg, rgba(148, 163, 184, 0.08), transparent 52%),
    var(--panel-bg);
}

.provider-impact-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  border-radius: 999px;
  padding: 0 0.75rem;
  font-size: 0.76rem;
  font-weight: 700;
}

.provider-impact-pill--ready {
  background: rgba(41, 163, 110, 0.12);
  color: #1d7b52;
}

.provider-impact-pill--degraded {
  background: rgba(203, 143, 33, 0.14);
  color: #8c6110;
}

.provider-impact-pill--missing {
  background: rgba(var(--bc-ink-rgb), 0.08);
  color: var(--bc-ink-secondary);
}

.provider-impact-list {
  display: grid;
  gap: 0.55rem;
  padding-left: 1rem;
  color: var(--bc-ink-secondary);
}

.provider-card {
  display: grid;
  gap: 1.25rem;
}

.provider-card__head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 1rem;
}

.provider-card__title {
  margin-top: 0.7rem;
  font-size: 1.4rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--bc-ink);
}

.provider-card__description {
  margin-top: 0.55rem;
  max-width: 40rem;
  font-size: 0.92rem;
  line-height: 1.75;
  color: var(--bc-ink-secondary);
}

.provider-card__status {
  min-width: 14rem;
  display: grid;
  gap: 0.55rem;
  align-content: start;
}

.provider-status-badge {
  display: inline-flex;
  width: fit-content;
  border-radius: 999px;
  padding: 0.3rem 0.8rem;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.provider-status-badge--ready {
  background: rgba(41, 163, 110, 0.12);
  color: #1d7b52;
}

.provider-status-badge--saved {
  background: rgba(var(--bc-accent-rgb), 0.12);
  color: var(--bc-accent);
}

.provider-status-badge--incomplete {
  background: rgba(203, 143, 33, 0.14);
  color: #8c6110;
}

.provider-status-badge--failed {
  background: rgba(210, 77, 87, 0.14);
  color: #a33139;
}

.provider-status-badge--missing {
  background: rgba(var(--bc-ink-rgb), 0.08);
  color: var(--bc-ink-secondary);
}

.provider-card__status-text {
  font-size: 0.84rem;
  line-height: 1.65;
  color: var(--bc-ink-secondary);
}

.provider-card__form {
  display: grid;
  gap: 1rem;
}

.provider-field {
  display: grid;
  gap: 0.55rem;
}

.provider-field label {
  font-size: 0.84rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.provider-secret-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
}

.provider-card__foot {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

@media (min-width: 1100px) {
  .provider-capability-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .provider-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
