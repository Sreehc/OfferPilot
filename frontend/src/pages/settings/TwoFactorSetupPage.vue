<template>
  <div class="space-y-6">
    <section class="paper-panel p-4 sm:p-6">
      <p class="section-kicker">两步验证</p>
      <h3 class="mt-4 text-xl sm:text-3xl font-semibold tracking-[-0.03em] text-ink">设置两步验证</h3>
      <p class="mt-4 text-sm leading-7 text-slate-600 dark:text-slate-300">
        使用身份验证器应用（如 Google Authenticator、Microsoft Authenticator）扫描二维码，输入 6 位验证码完成设置。
      </p>
    </section>

    <!-- Step 1: Show QR code -->
    <section v-if="step === 1" class="paper-panel p-4 sm:p-6">
      <h4 class="text-lg font-semibold text-ink">第 1 步：扫描二维码</h4>
      <div class="mt-4 flex flex-col items-center gap-6">
        <div class="rounded-lg border border-slate-200 dark:border-slate-700 bg-white p-4">
          <img
            v-if="qrCodeUrl"
            :src="qrCodeUrl"
            alt="TOTP QR Code"
            class="h-48 w-48"
          />
          <div v-else class="flex h-48 w-48 items-center justify-center text-slate-400">
            <el-icon class="is-loading" :size="24"><i class="el-icon-loading" /></el-icon>
          </div>
        </div>

        <div class="text-center">
          <p class="text-xs text-slate-500 dark:text-slate-400">无法扫描？手动输入密钥：</p>
          <div class="mt-2 flex items-center gap-2">
            <code class="rounded bg-slate-100 dark:bg-slate-800 px-3 py-1.5 text-sm font-mono select-all">{{ setupData?.secret }}</code>
            <el-button size="small" @click="copySecret">复制</el-button>
          </div>
        </div>
      </div>

      <el-button type="primary" size="large" class="action-button mt-6 w-full" @click="step = 2">
        下一步
      </el-button>
    </section>

    <!-- Step 2: Verify code -->
    <section v-if="step === 2" class="paper-panel p-4 sm:p-6">
      <h4 class="text-lg font-semibold text-ink">第 2 步：输入验证码</h4>
      <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">
        打开身份验证器应用，输入显示的 6 位数字。
      </p>
      <div class="mt-4 max-w-xs">
        <el-input
          v-model="verifyCode"
          placeholder="6 位验证码"
          size="large"
          maxlength="6"
          @keyup.enter="handleEnable"
        />
      </div>
      <div class="mt-4 flex gap-3">
        <el-button size="large" @click="step = 1">上一步</el-button>
        <el-button :loading="enabling" type="primary" size="large" class="action-button" @click="handleEnable">
          启用两步验证
        </el-button>
      </div>
    </section>

    <!-- Step 3: Show recovery codes -->
    <section v-if="step === 3" class="paper-panel p-4 sm:p-6">
      <h4 class="text-lg font-semibold text-ink">恢复码</h4>
      <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">
        请妥善保存以下恢复码。当您无法使用身份验证器时，可以使用恢复码登录。每个恢复码只能使用一次。
      </p>
      <div class="mt-4 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/50 p-4">
        <div class="grid grid-cols-2 gap-2">
          <code
            v-for="code in recoveryCodes"
            :key="code"
            class="rounded bg-white dark:bg-slate-900 px-3 py-1.5 text-sm font-mono text-center border border-slate-200 dark:border-slate-700"
          >{{ code }}</code>
        </div>
      </div>
      <div class="mt-4 flex gap-3">
        <el-button size="large" @click="copyRecoveryCodes">复制全部</el-button>
        <el-button size="large" @click="downloadRecoveryCodes">下载</el-button>
        <el-button type="primary" size="large" class="action-button" @click="$emit('done')">
          我已保存，完成
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { enableTwoFactorApi, setupTwoFactorApi } from '@/api/auth'
import type { TwoFactorSetup } from '@/types/api'

const emit = defineEmits<{ done: [] }>()

const step = ref(1)
const setupData = ref<TwoFactorSetup | null>(null)
const verifyCode = ref('')
const recoveryCodes = ref<string[]>([])
const enabling = ref(false)

const qrCodeUrl = computed(() => {
  if (!setupData.value?.otpauthUri) return ''
  // Use a QR code API service to generate the image
  return `https://api.qrserver.com/v1/create-qr-code/?size=192x192&data=${encodeURIComponent(setupData.value.otpauthUri)}`
})

const loadSetup = async () => {
  try {
    const response = await setupTwoFactorApi()
    setupData.value = response.data
  } catch {
    ElMessage.error('初始化两步验证失败')
  }
}

const handleEnable = async () => {
  if (!verifyCode.value.trim() || verifyCode.value.length !== 6) {
    ElMessage.warning('请输入 6 位验证码')
    return
  }
  enabling.value = true
  try {
    const response = await enableTwoFactorApi(verifyCode.value)
    recoveryCodes.value = response.data.recoveryCodes
    step.value = 3
    ElMessage.success('两步验证已启用')
  } catch {
    ElMessage.error('验证码错误，请重试')
  } finally {
    enabling.value = false
  }
}

const copySecret = async () => {
  if (!setupData.value?.secret) return
  try {
    await navigator.clipboard.writeText(setupData.value.secret)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

const copyRecoveryCodes = async () => {
  try {
    await navigator.clipboard.writeText(recoveryCodes.value.join('\n'))
    ElMessage.success('恢复码已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

const downloadRecoveryCodes = () => {
  const content = `ByteCoach 两步验证恢复码\n生成时间：${new Date().toLocaleString()}\n\n${recoveryCodes.value.join('\n')}\n\n请妥善保管，每个恢复码只能使用一次。`
  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'bytecoach-recovery-codes.txt'
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(loadSetup)
</script>
