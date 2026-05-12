<template>
  <div class="space-y-4">
    <div class="flex flex-wrap gap-3">
      <el-button :loading="exporting" type="primary" size="large" class="action-button" @click="handleExport">
        <svg class="mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
        </svg>
        导出个人数据
      </el-button>
    </div>

    <div class="rounded-lg border border-[var(--border-subtle)] bg-[var(--panel-muted)] p-4">
      <h4 class="mb-2 text-sm font-semibold text-ink">包含内容</h4>
      <p class="text-xs text-secondary">
        面试记录、错题本和复习记录。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import { exportMyDataApi } from '@/api/auth'

const exporting = ref(false)

const handleExport = async () => {
  exporting.value = true
  try {
    const response = await exportMyDataApi()
    downloadBlob(response.data as any, '个人数据导出.xlsx')
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const downloadBlob = (data: Blob, filename: string) => {
  const url = URL.createObjectURL(new Blob([data]))
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
</script>
