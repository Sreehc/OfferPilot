<template>
  <div class="space-y-4">
    <section class="shell-section-card p-5">
      <div class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
        <div>
          <p class="section-kicker">面试治理</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">会话模式、完结率与低分风险</h3>
          <p class="mt-3 max-w-3xl text-sm leading-7 text-secondary">
            从后台统一查看面试会话的模式分布、是否关联简历项目、低分题数量和当前完成状态。
          </p>
        </div>
        <div class="grid gap-3 sm:grid-cols-2">
          <article v-for="card in summaryCards" :key="card.label" class="data-slab p-4">
            <div class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">{{ card.label }}</div>
            <div class="mt-3 text-3xl font-semibold text-ink">{{ card.value }}</div>
          </article>
        </div>
      </div>
    </section>

    <section class="shell-section-card p-5">
      <div class="grid gap-3 xl:grid-cols-4">
        <el-select v-model="status" clearable size="large" placeholder="会话状态">
          <el-option label="进行中" value="in_progress" />
          <el-option label="已完成" value="finished" />
        </el-select>
        <el-select v-model="mode" clearable size="large" placeholder="面试模式">
          <el-option label="文本" value="text" />
          <el-option label="语音" value="voice" />
        </el-select>
        <el-input
          v-model="keyword"
          clearable
          size="large"
          placeholder="搜索方向、岗位或技术栈"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <div class="flex gap-3">
          <el-button :loading="loading" size="large" class="action-button flex-1" @click="handleSearch">刷新列表</el-button>
          <el-button size="large" class="hard-button-secondary" @click="resetFilters">重置</el-button>
        </div>
      </div>
    </section>

    <section class="shell-section-card p-5">
      <el-table
        v-loading="loading"
        :data="records"
        stripe
        class="w-full"
        :header-cell-style="{ background: 'var(--el-bg-color-page)', color: 'var(--el-text-color-primary)' }"
      >
        <el-table-column label="方向 / 岗位" min-width="200">
          <template #default="{ row }">
            <div class="font-semibold text-ink">{{ row.direction }}</div>
            <div class="mt-1 text-sm text-secondary">{{ row.jobRole || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="模式" min-width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.mode === 'voice' ? 'warning' : 'primary'">
              {{ row.mode === 'voice' ? '语音' : '文本' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'finished' ? 'success' : 'info'">
              {{ row.status === 'finished' ? '已完成' : '进行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总分" min-width="90">
          <template #default="{ row }">{{ row.totalScore ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="低分题" min-width="90">
          <template #default="{ row }">{{ row.lowScoreCount }}</template>
        </el-table-column>
        <el-table-column label="简历项目" min-width="110">
          <template #default="{ row }">{{ row.includeResumeProject ? '已关联' : '未关联' }}</template>
        </el-table-column>
        <el-table-column label="技术栈" min-width="180">
          <template #default="{ row }">{{ row.techStack || '-' }}</template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
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
  fetchAdminInterviewGovernanceApi,
  fetchAdminInterviewGovernanceSummaryApi,
  type AdminInterviewGovernanceItem,
  type AdminInterviewGovernanceSummary
} from '@/api/admin'

const records = ref<AdminInterviewGovernanceItem[]>([])
const summary = ref<AdminInterviewGovernanceSummary | null>(null)
const loading = ref(false)
const status = ref('')
const mode = ref('')
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)

const summaryCards = computed(() => [
  { label: '总会话', value: summary.value?.totalSessions ?? 0 },
  { label: '已完成', value: summary.value?.finishedSessions ?? 0 },
  { label: '语音场次', value: summary.value?.voiceSessions ?? 0 },
  { label: '平均分', value: summary.value?.averageScore ?? 0 }
])

const loadData = async () => {
  loading.value = true
  try {
    const [summaryRes, listRes] = await Promise.all([
      fetchAdminInterviewGovernanceSummaryApi(),
      fetchAdminInterviewGovernanceApi({
        status: status.value || undefined,
        mode: mode.value || undefined,
        keyword: keyword.value || undefined,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      })
    ])
    summary.value = summaryRes.data
    records.value = listRes.data.records
    total.value = listRes.data.total
    totalPages.value = listRes.data.totalPages
  } catch {
    ElMessage.error('面试治理数据加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  void loadData()
}

const resetFilters = () => {
  status.value = ''
  mode.value = ''
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
    minute: '2-digit'
  }).format(new Date(value))
}

onMounted(() => {
  void loadData()
})
</script>
