<template>
  <div class="space-y-4">
    <AppShellHeader compact title="数据导出" subtitle="导出你的学习记录。" />

    <div class="flex flex-wrap gap-3">
      <el-button :loading="exporting" type="primary" size="large" class="action-button" @click="handleExport">
        <svg class="mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
        </svg>
        导出个人数据
      </el-button>
    </div>

    <div class="rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/50 p-4">
      <h4 class="text-sm font-semibold text-ink mb-2">导出内容</h4>
      <ul class="text-xs text-slate-500 dark:text-slate-400 space-y-1">
        <li>• 面试记录：面试方向、时间、总分、得分、回答内容、点评</li>
        <li>• 错题本：题目来源、掌握程度、复习次数、下次复习日期</li>
        <li>• 复习记录：评分、记忆系数变化、间隔变化</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import { exportMyDataApi } from '@/api/auth'
import AppShellHeader from '@/components/AppShellHeader.vue'

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
