<template>
  <div class="space-y-4">
    <section class="shell-section-card p-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p class="section-kicker">登录日志</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">共 {{ total }} 条记录</h3>
        </div>
        <div class="flex flex-wrap items-center gap-3">
          <el-input
            v-model="keyword"
            placeholder="搜索用户"
            clearable
            size="large"
            class="max-w-xs"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button :loading="loading" type="primary" size="large" class="action-button" @click="handleSearch">
            搜索
          </el-button>
        </div>
      </div>
    </section>

    <section class="shell-section-card p-5 mobile-cards">
    <el-table
      v-loading="loading"
      :data="logs"
      stripe
      class="w-full"
      :header-cell-style="{ background: 'var(--el-bg-color-page)', color: 'var(--el-text-color-primary)' }"
    >
      <el-table-column label="用户名" min-width="100">
        <template #default="{ row }">
          {{ row.username || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="时间" min-width="160">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="IP" prop="ip" min-width="120" />
      <el-table-column label="地点" min-width="100">
        <template #default="{ row }">
          {{ row.city || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="设备" min-width="140">
        <template #default="{ row }">
          {{ row.device || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="140">
        <template #default="{ row }">
          <span v-if="row.status !== 1 && row.failReason" class="text-xs text-red-500">{{ row.failReason }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="mobile-card-list">
      <div v-for="row in logs" :key="row.id" class="mobile-card-item">
        <div class="flex items-center gap-2 mb-2">
          <span class="text-sm font-semibold text-ink">{{ row.username || '-' }}</span>
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
        </div>
        <div class="mobile-card-field"><span class="mobile-card-label">时间</span><span class="mobile-card-value">{{ formatTime(row.createTime) }}</span></div>
        <div class="mobile-card-field"><span class="mobile-card-label">IP</span><span class="mobile-card-value">{{ row.ip || '-' }}</span></div>
        <div class="mobile-card-field"><span class="mobile-card-label">地点</span><span class="mobile-card-value">{{ row.city || '-' }}</span></div>
        <div class="mobile-card-field"><span class="mobile-card-label">设备</span><span class="mobile-card-value">{{ row.device || '-' }}</span></div>
        <div v-if="row.failReason" class="mobile-card-field"><span class="mobile-card-label">原因</span><span class="mobile-card-value text-red-500">{{ row.failReason }}</span></div>
      </div>
    </div>
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
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { fetchAdminLoginLogsApi } from '@/api/auth'
import type { LoginLogItem } from '@/types/api'

const logs = ref<LoginLogItem[]>([])
const loading = ref(false)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)

const loadLogs = async () => {
  loading.value = true
  try {
    const response = await fetchAdminLoginLogsApi({
      keyword: keyword.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    logs.value = response.data.records
    total.value = response.data.total
    totalPages.value = response.data.totalPages
  } catch {
    ElMessage.error('登录日志加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  void loadLogs()
}

const handlePageChange = (page: number) => {
  pageNum.value = page
  void loadLogs()
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

onMounted(loadLogs)
</script>
