<template>
  <AuthLayout
    brand-kicker="两步验证"
    brand-title="完成最后一步验证"
    brand-description="输入验证码或恢复码以完成登录。"
    wide
  >
    <div class="flex items-start justify-between gap-4">
      <div>
        <p class="section-kicker">验证</p>
        <h2 class="auth-panel-title mt-4 text-3xl font-semibold text-ink">输入验证码完成登录</h2>
        <p class="mt-3 text-sm leading-7 text-secondary">
          {{ useRecovery ? '请输入恢复码完成登录。' : '请输入身份验证器里的 6 位验证码。' }}
        </p>
      </div>
    </div>

    <el-form class="mt-8" label-position="top" @submit.prevent>
      <el-form-item :label="useRecovery ? '恢复码' : '验证码'">
        <el-input
          v-model="code"
          :placeholder="useRecovery ? '请输入恢复码' : '6 位验证码'"
          size="large"
          :maxlength="useRecovery ? 10 : 6"
          @keyup.enter="handleVerify"
        />
      </el-form-item>

      <div class="mt-6 grid gap-3">
        <el-button
          :loading="loading"
          type="primary"
          size="large"
          class="action-button !min-h-12 w-full"
          @click="handleVerify"
        >
          {{ loading ? '验证中...' : '完成验证' }}
        </el-button>
      </div>
    </el-form>

    <div class="mt-6 flex flex-wrap items-center justify-between gap-3 text-sm">
      <button class="accent-link touch-link font-semibold" @click="toggleMode">
        {{ useRecovery ? '改用验证码' : '改用恢复码' }}
      </button>
      <RouterLink class="accent-link touch-link font-semibold" to="/login">返回登录</RouterLink>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { verifyTwoFactorApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from './AuthLayout.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const code = ref('')
const loading = ref(false)
const useRecovery = ref(false)

const tempToken = route.query.tempToken as string
const redirectTarget = computed(() => (route.query.redirect as string) || '/dashboard')
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
    const response = await verifyTwoFactorApi(tempToken, code.value.trim())
    authStore.persistFromResponse(response.data)
    ElMessage.success('登录成功')
    await router.push(redirectTarget.value)
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>
