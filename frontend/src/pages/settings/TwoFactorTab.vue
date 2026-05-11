<template>
  <div class="space-y-4">
    <div v-if="loading" class="flex items-center justify-center py-8">
      <el-icon class="is-loading text-slate-400" :size="24"><i class="el-icon-loading" /></el-icon>
      <span class="ml-3 text-sm text-slate-400">正在加载...</span>
    </div>

    <template v-else>
      <div v-if="status?.enabled" class="space-y-4">
        <div class="flex items-center gap-3 rounded-lg border border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-900/20 p-4">
          <div class="flex h-8 w-8 items-center justify-center rounded-full bg-green-100 dark:bg-green-800/40">
            <svg class="h-4 w-4 text-green-600 dark:text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <div>
            <p class="font-semibold text-green-800 dark:text-green-300">两步验证已启用</p>
            <p class="text-sm text-green-600 dark:text-green-400">
              剩余恢复码：{{ status.recoveryCodesRemaining ?? '-' }} 个
            </p>
          </div>
        </div>

        <div class="paper-panel p-4">
          <h4 class="text-sm font-semibold text-ink">关闭两步验证</h4>
          <div class="mt-3 flex items-center gap-3 max-w-xs">
            <el-input
              v-model="disableCode"
              placeholder="输入当前验证码"
              size="large"
              maxlength="6"
            />
            <el-button
              :loading="disabling"
              type="danger"
              size="large"
              plain
              @click="handleDisable"
            >
              关闭
            </el-button>
          </div>
        </div>
      </div>

      <div v-else>
        <template v-if="!showSetup">
          <div class="paper-panel flex flex-wrap items-center justify-between gap-3 p-4">
            <div class="text-sm font-semibold text-ink">未启用</div>
            <el-button type="primary" size="large" class="action-button" @click="showSetup = true">
              启用两步验证
            </el-button>
          </div>
        </template>

        <template v-else>
          <TwoFactorSetupPage @done="handleSetupDone" />
        </template>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { disableTwoFactorApi, fetchTwoFactorStatusApi } from '@/api/auth'
import type { TwoFactorStatus } from '@/types/api'
import TwoFactorSetupPage from './TwoFactorSetupPage.vue'

const status = ref<TwoFactorStatus | null>(null)
const loading = ref(false)
const showSetup = ref(false)
const disableCode = ref('')
const disabling = ref(false)

const loadStatus = async () => {
  loading.value = true
  try {
    const response = await fetchTwoFactorStatusApi()
    status.value = response.data
  } catch {
    ElMessage.error('加载两步验证状态失败')
  } finally {
    loading.value = false
  }
}

const handleDisable = async () => {
  if (!disableCode.value.trim() || disableCode.value.length !== 6) {
    ElMessage.warning('请输入 6 位验证码')
    return
  }
  disabling.value = true
  try {
    await disableTwoFactorApi(disableCode.value)
    ElMessage.success('两步验证已关闭')
    disableCode.value = ''
    await loadStatus()
  } catch {
    ElMessage.error('验证码错误')
  } finally {
    disabling.value = false
  }
}

const handleSetupDone = async () => {
  showSetup.value = false
  await loadStatus()
}

onMounted(loadStatus)
</script>
