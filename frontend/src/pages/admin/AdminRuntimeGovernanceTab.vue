<template>
  <div v-loading="loading" class="space-y-4">
    <section class="shell-section-card p-5">
      <div class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div>
          <p class="section-kicker">运行时治理</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">Agent、Copilot、转写与 Provider 观测</h3>
          <p class="mt-3 max-w-3xl text-sm leading-7 text-secondary">
            这里集中查看运行时链路的审批堆积、实时会话状态、转写处理情况、Provider 可用性，以及当前 AI 成本与失败请求。
          </p>
        </div>
        <div class="grid gap-3 sm:grid-cols-2">
          <article v-for="card in spotlightCards" :key="card.label" class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">{{ card.label }}</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ card.value }}</div>
            <p class="mt-2 text-sm text-secondary">{{ card.description }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="grid gap-4 xl:grid-cols-2">
      <article class="shell-section-card p-5">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="section-kicker">Agent Runtime</p>
            <h4 class="mt-2 text-xl font-semibold text-ink">审批与阻断</h4>
          </div>
          <span class="detail-pill">{{ summary?.totalAgentRuns ?? 0 }} runs</span>
        </div>
        <div class="mt-5 grid gap-3 sm:grid-cols-2">
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">待审批</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.pendingApprovalRuns ?? 0 }}</div>
          </article>
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">Provider 阻断</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.providerBlockedRuns ?? 0 }}</div>
          </article>
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">拒绝数</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.rejectedAgentRuns ?? 0 }}</div>
          </article>
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">AI 失败请求</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.failedAiCalls ?? 0 }}</div>
          </article>
        </div>
      </article>

      <article class="shell-section-card p-5">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="section-kicker">Copilot Runtime</p>
            <h4 class="mt-2 text-xl font-semibold text-ink">实时阶段状态</h4>
          </div>
          <span class="detail-pill">{{ summary?.totalCopilotRealtimeSessions ?? 0 }} sessions</span>
        </div>
        <div class="mt-5 grid gap-3 sm:grid-cols-2">
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">Live 中</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.liveCopilotRealtimeSessions ?? 0 }}</div>
          </article>
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">断连</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.disconnectedCopilotRealtimeSessions ?? 0 }}</div>
          </article>
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">关键阻断</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.blockedCopilotRealtimeSessions ?? 0 }}</div>
          </article>
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">降级运行</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.degradedCopilotRealtimeSessions ?? 0 }}</div>
          </article>
        </div>
        <p class="mt-4 text-sm leading-6 text-secondary">
          Copilot Prep 累计 {{ summary?.totalCopilotPrepSessions ?? 0 }} 次，实时链路的断连、阻断和降级状态都会优先汇总到这里。
        </p>
      </article>
    </section>

    <section class="grid gap-4 xl:grid-cols-2">
      <article class="shell-section-card p-5">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="section-kicker">Transcription</p>
            <h4 class="mt-2 text-xl font-semibold text-ink">录音复盘与转写队列</h4>
          </div>
          <span class="detail-pill">{{ summary?.totalRecordingReviews ?? 0 }} reviews</span>
        </div>
        <div class="mt-5 grid gap-3 sm:grid-cols-2">
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">处理中</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.processingRecordingReviews ?? 0 }}</div>
          </article>
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">已就绪</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.readyRecordingReviews ?? 0 }}</div>
          </article>
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">失败</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.failedRecordingReviews ?? 0 }}</div>
          </article>
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">平均转写耗时</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ formatDuration(summary?.avgTranscriptTimeMs) }}</div>
          </article>
        </div>
      </article>

      <article class="shell-section-card p-5">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="section-kicker">Provider Config</p>
            <h4 class="mt-2 text-xl font-semibold text-ink">配置可用性</h4>
          </div>
          <span class="detail-pill">{{ summary?.configuredProviderUsers ?? 0 }} users</span>
        </div>
        <div class="mt-5 grid gap-3 sm:grid-cols-2">
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">总配置数</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.totalProviderConfigs ?? 0 }}</div>
          </article>
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">已启用</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.enabledProviderConfigs ?? 0 }}</div>
          </article>
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">探测成功</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ summary?.readyProviderConfigs ?? 0 }}</div>
          </article>
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">失败 / 未检查</div>
            <div class="mt-3 text-3xl font-semibold text-ink">
              {{ summary?.failedProviderConfigs ?? 0 }} / {{ summary?.uncheckedProviderConfigs ?? 0 }}
            </div>
          </article>
        </div>
      </article>
    </section>

    <section class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
      <article class="shell-section-card p-5">
        <p class="section-kicker">Cost & Latency</p>
        <h4 class="mt-2 text-xl font-semibold text-ink">AI 成本与耗时</h4>
        <div class="mt-5 grid gap-3 sm:grid-cols-2">
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">估算成本</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ formatCost(summary?.totalEstimatedAiCost) }}</div>
          </article>
          <article class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">平均耗时</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ formatDuration(summary?.avgAiLatencyMs) }}</div>
          </article>
        </div>
      </article>

      <article class="shell-section-card p-5">
        <p class="section-kicker">Risk Review</p>
        <h4 class="mt-2 text-xl font-semibold text-ink">运行时风险与建议</h4>
        <div class="mt-5 grid gap-3 xl:grid-cols-2">
          <div class="runtime-note-card">
            <p class="runtime-note-card__title">风险信号</p>
            <ul class="runtime-note-list mt-3">
              <li v-for="item in riskSignals" :key="item">{{ item }}</li>
            </ul>
          </div>
          <div class="runtime-note-card">
            <p class="runtime-note-card__title">建议动作</p>
            <ul class="runtime-note-list mt-3">
              <li v-for="item in recommendations" :key="item">{{ item }}</li>
            </ul>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchAdminRuntimeGovernanceSummaryApi, type AdminRuntimeGovernanceSummary } from '@/api/admin'
import { ERROR_COPY } from '@/constants/productCopy'

const loading = ref(false)
const summary = ref<AdminRuntimeGovernanceSummary | null>(null)

const spotlightCards = computed(() => [
  {
    label: '运行总量',
    value: summary.value?.totalAgentRuns ?? 0,
    description: '当前累计 agent run 数量。'
  },
  {
    label: '实时会话',
    value: summary.value?.totalCopilotRealtimeSessions ?? 0,
    description: '当前累计创建的 Realtime Copilot 会话。'
  },
  {
    label: '转写处理中',
    value: summary.value?.processingRecordingReviews ?? 0,
    description: '当前还在排队或处理中录音复盘。'
  },
  {
    label: 'Provider 用户',
    value: summary.value?.configuredProviderUsers ?? 0,
    description: '至少保存过一类 provider 配置的用户数。'
  }
])

const riskSignals = computed(() => {
  if (!summary.value?.riskSignals?.length) {
    return ['当前没有明显风险信号，运行时链路整体稳定。']
  }
  return summary.value.riskSignals
})

const recommendations = computed(() => {
  if (!summary.value?.recommendations?.length) {
    return ['当前没有额外治理动作需要优先处理。']
  }
  return summary.value.recommendations
})

const loadData = async () => {
  loading.value = true
  try {
    const response = await fetchAdminRuntimeGovernanceSummaryApi()
    summary.value = response.data
  } catch {
    ElMessage.error(ERROR_COPY.adminRuntimeGovernanceLoadFailed)
  } finally {
    loading.value = false
  }
}

const formatDuration = (value?: number) => {
  if (value == null || value <= 0) return '-'
  if (value >= 1000) return `${(value / 1000).toFixed(1)}s`
  return `${Math.round(value)}ms`
}

const formatCost = (value?: number) => {
  if (value == null) return '-'
  return `¥${Number(value).toFixed(4)}`
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped>
.runtime-note-card {
  border-radius: 20px;
  background: rgba(var(--bc-accent-rgb), 0.08);
  padding: 18px;
}

.runtime-note-card__title {
  color: var(--bc-ink);
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.runtime-note-list {
  margin: 0;
  padding-left: 18px;
  color: var(--bc-ink-secondary);
  display: grid;
  gap: 10px;
  font-size: 0.92rem;
  line-height: 1.65;
}
</style>
