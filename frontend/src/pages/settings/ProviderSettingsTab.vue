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
            <el-input v-model="ensureDraft(item.scope).baseUrl" placeholder="https://api.example.com/v1" />
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
import { onMounted, reactive, ref } from 'vue'
import {
  checkProviderConfigsApi,
  fetchProviderConfigsApi,
  updateProviderConfigsApi,
  type ProviderConfigUpdateItemPayload
} from '@/api/settings'
import type { ProviderScope, UserProviderConfigItem } from '@/types/api'

type ProviderDraft = ProviderConfigUpdateItemPayload

const loading = ref(true)
const savingScope = ref<ProviderScope | ''>('')
const checking = ref(false)
const configs = ref<UserProviderConfigItem[]>([])
const draftByScope = reactive<Record<string, ProviderDraft>>({})

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
const needsBaseUrl = (scope: ProviderScope) => ['llm', 'embedding'].includes(scope)
const needsModel = (scope: ProviderScope) => ['llm', 'embedding'].includes(scope)
const needsApiKey = (scope: ProviderScope) => scope !== 'oss'

const statusLabel = (status: string) => {
  if (status === 'ready') return '已可用'
  if (status === 'saved') return '已保存'
  if (status === 'incomplete') return '待补齐'
  return '未配置'
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
  .provider-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
