<template>
  <div class="flex min-h-screen items-center justify-center px-4 py-8">
    <div class="w-full max-w-md">
      <section class="paper-panel p-8 md:p-10">
        <p class="section-kicker">Two-Factor Authentication</p>
        <h2 class="mt-4 text-3xl font-semibold tracking-[-0.03em] text-ink">两步验证</h2>
        <p class="mt-3 text-sm text-slate-500 dark:text-slate-400">
          请输入身份验证器应用中的 6 位验证码，或使用恢复码登录。
        </p>
        <div class="rule-divider mt-6"></div>

        <el-form class="mt-8" label-position="top" @submit.prevent>
          <el-form-item label="验证码">
            <el-input
              v-model="code"
              :placeholder="useRecovery ? '请输入恢复码' : '6 位验证码'"
              size="large"
              :maxlength="useRecovery ? 10 : 6"
              @keyup.enter="handleVerify"
            />
          </el-form-item>

          <el-button :loading="loading" type="primary" size="large" class="action-button w-full" @click="handleVerify">
            验证
          </el-button>
        </el-form>

        <div class="mt-6 flex items-center justify-between text-sm text-slate-500 dark:text-slate-400">
          <button class="accent-link" @click="toggleMode">
            {{ useRecovery ? '使用验证码' : '使用恢复码' }}
          </button>
          <RouterLink class="accent-link" to="/login">返回登录</RouterLink>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { verifyTwoFactorApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const code = ref('')
const loading = ref(false)
const useRecovery = ref(false)

const tempToken = route.query.tempToken as string

const toggleMode = () => {
  useRecovery.value = !useRecovery.value
  code.value = ''
}

const handleVerify = async () => {
  if (!code.value.trim()) {
    ElMessage.warning(useRecovery.value ? '请输入恢复码' : '请输入验证码')
    return
  }
  if (!tempToken) {
    ElMessage.error('验证已过期，请重新登录')
    await router.push('/login')
    return
  }

  loading.value = true
  try {
    const response = await verifyTwoFactorApi(tempToken, code.value)
    authStore.persistFromResponse(response.data)
    ElMessage.success('登录成功')
    await router.push((route.query.redirect as string) || '/dashboard')
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>
