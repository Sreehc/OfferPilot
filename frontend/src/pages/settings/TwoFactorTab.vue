<template>
  <div class="space-y-4">
    <div v-if="loading" class="shell-section-card p-4">
      <StateView variant="loading" :rows="2" compact />
    </div>

    <template v-else>
      <div v-if="status?.enabled" class="space-y-4">
        <div class="flex items-center gap-3 rounded-lg border border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-900/20 p-4">
          <div class="flex h-8 w-8 items-center justify-center rounded-full bg-green-100 dark:bg-green-800/40">
            <UiIcon name="circleCheck" class="h-4 w-4 text-green-600 dark:text-green-400" />
          </div>
          <div>
            <p class="font-semibold text-green-800 dark:text-green-300">两步验证已启用</p>
            <p class="text-sm text-green-600 dark:text-green-400">
              剩余恢复码：{{ status.recoveryCodesRemaining ?? '-' }} 个
            </p>
          </div>
        </div>

        <div class="shell-section-card p-4">
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
          <div class="shell-section-card flex flex-wrap items-center justify-between gap-3 p-4">
            <div>
              <div class="text-sm font-semibold text-ink">当前还没开启两步验证</div>
              <p class="mt-1 text-xs leading-5 text-secondary">为常用账号启用两步验证，可降低异常登录风险。</p>
            </div>
            <el-button type="primary" size="large" class="action-button" @click="showSetup = true">
              现在启用
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
import { StateView, UiIcon } from '@/components/ui'

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
    ElMessage.error('无法加载两步验证状态，请稍后刷新。')
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
    ElMessage.error('验证码校验失败，请检查后重试。')
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
