<template>
  <div class="space-y-4">
    <section class="shell-section-card p-5">
      <div class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
        <div>
          <p class="section-kicker">AI 调用治理</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">模型调用、延迟与失败排查</h3>
          <p class="mt-3 max-w-3xl text-sm leading-7 text-secondary">
            统一查看问答、面试评分、卡片生成和向量化调用的场景、耗时、估算 token 与异常信息。
          </p>
        </div>
        <div class="grid gap-3 sm:grid-cols-2">
          <article v-for="card in spotlightCards" :key="card.label" class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">{{ card.label }}</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ card.value }}</div>
          </article>
        </div>
      </div>
    </section>

    <section class="shell-section-card p-5">
      <div class="grid gap-3 xl:grid-cols-4">
        <el-select v-model="scene" clearable size="large" placeholder="场景">
          <el-option label="聊天回答" value="chat.answer.chat" />
          <el-option label="RAG 回答" value="chat.answer.rag" />
          <el-option label="流式 RAG" value="chat.stream.rag" />
          <el-option label="面试评分" value="interview.score" />
          <el-option label="知识检索" value="knowledge.search" />
          <el-option label="知识索引" value="knowledge.index" />
          <el-option label="卡片生成" value="cards.generate" />
        </el-select>
        <el-select v-model="callType" clearable size="large" placeholder="调用类型">
          <el-option label="聊天" value="chat" />
          <el-option label="流式" value="stream" />
          <el-option label="向量" value="embedding" />
        </el-select>
        <el-select v-model="success" clearable size="large" placeholder="执行状态">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-input
          v-model="keyword"
          clearable
          size="large"
          placeholder="搜索模型、场景或错误"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
      </div>
      <div class="mt-4 flex flex-wrap gap-3">
        <el-button :loading="loading" size="large" class="action-button" @click="handleSearch">刷新日志</el-button>
        <el-button size="large" class="hard-button-secondary" @click="resetFilters">重置筛选</el-button>
      </div>
    </section>

    <section class="shell-section-card p-5">
      <el-table
        v-loading="loading"
        :data="logs"
        stripe
        class="w-full"
        :header-cell-style="{ background: 'var(--el-bg-color-page)', color: 'var(--el-text-color-primary)' }"
      >
        <el-table-column label="时间" min-width="168">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="场景" min-width="180">
          <template #default="{ row }">{{ row.scene || '-' }}</template>
        </el-table-column>
        <el-table-column label="类型" min-width="92">
          <template #default="{ row }">
            <el-tag size="small" :type="row.callType === 'embedding' ? 'warning' : 'primary'">
              {{ row.callType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模型" min-width="160">
          <template #default="{ row }">{{ row.model || '-' }}</template>
        </el-table-column>
        <el-table-column label="Token" min-width="120">
          <template #default="{ row }">{{ row.inputTokens ?? 0 }} / {{ row.outputTokens ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="耗时" min-width="96">
          <template #default="{ row }">{{ row.latencyMs ?? 0 }} ms</template>
        </el-table-column>
        <el-table-column label="状态" min-width="92">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="错误信息" min-width="240">
          <template #default="{ row }"><span class="text-sm text-secondary">{{ row.errorMessage || '-' }}</span></template>
        </el-table-column>
      </el-table>
    </section>

    <div v-if="totalPages > 1" class="flex justify-center pt-2">
      <el-pagination
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  fetchAdminAiLogsApi,
  fetchAdminAiLogSummaryApi,
  type AdminAiLogItem,
  type AdminAiLogSummary
} from '@/api/admin'

const logs = ref<AdminAiLogItem[]>([])
const summary = ref<AdminAiLogSummary | null>(null)
const loading = ref(false)
const scene = ref('')
const callType = ref('')
const success = ref<number | undefined>()
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)

const spotlightCards = computed(() => [
  { label: '总调用数', value: summary.value?.totalCalls ?? 0 },
  { label: '成功率', value: summary.value ? `${rate(summary.value.successCalls, summary.value.totalCalls)}%` : '0%' },
  { label: '平均耗时', value: `${summary.value?.avgLatencyMs ?? 0} ms` },
  { label: '向量调用', value: summary.value?.embeddingCalls ?? 0 }
])

const rate = (part: number, whole: number) => {
  if (!whole) return 0
  return Math.round((part / whole) * 100)
}

const loadData = async () => {
  loading.value = true
  try {
    const [summaryRes, logsRes] = await Promise.all([
      fetchAdminAiLogSummaryApi(),
      fetchAdminAiLogsApi({
        scene: scene.value || undefined,
        callType: callType.value || undefined,
        success: success.value,
        keyword: keyword.value || undefined,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      })
    ])
    summary.value = summaryRes.data
    logs.value = logsRes.data.records
    total.value = logsRes.data.total
    totalPages.value = logsRes.data.totalPages
  } catch {
    ElMessage.error('AI 调用日志加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  void loadData()
}

const resetFilters = () => {
  scene.value = ''
  callType.value = ''
  success.value = undefined
  keyword.value = ''
  handleSearch()
}

const handlePageChange = (page: number) => {
  pageNum.value = page
  void loadData()
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(new Date(value))
}

onMounted(() => {
  void loadData()
})
</script>
