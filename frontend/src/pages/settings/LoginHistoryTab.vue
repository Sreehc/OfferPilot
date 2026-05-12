<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between gap-3">
      <div class="text-sm text-secondary">共 {{ total }} 条</div>
      <el-button :loading="loading" type="primary" size="large" class="action-button" @click="loadLogs">
        刷新
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="logs"
      stripe
      class="w-full"
      :header-cell-style="{ background: 'var(--el-bg-color-page)', color: 'var(--el-text-color-primary)' }"
    >
      <el-table-column label="时间" min-width="160">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="网络地址" prop="ip" min-width="120" />
      <el-table-column label="城市" min-width="100">
        <template #default="{ row }">
          {{ row.city || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="设备" min-width="140">
        <template #default="{ row }">
          {{ formatDeviceName(row.device) }}
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
          <span v-else class="text-xs text-slate-400">-</span>
        </template>
      </el-table-column>
    </el-table>

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
import { fetchLoginLogsApi } from '@/api/auth'
import { localizeDeviceName } from '@/utils/device'
import type { LoginLogItem } from '@/types/api'

const logs = ref<LoginLogItem[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)

const loadLogs = async () => {
  loading.value = true
  try {
    const response = await fetchLoginLogsApi({
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

const formatDeviceName = (name?: string) => localizeDeviceName(name)

onMounted(loadLogs)
</script>
