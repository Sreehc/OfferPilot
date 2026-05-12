<template>
  <div class="auth-immersive-shell px-4 py-8 md:px-6 md:py-10">
    <div class="auth-viewport mx-auto grid min-h-[calc(100vh-4rem)] max-w-[1120px] items-stretch gap-4 xl:grid-cols-[minmax(0,0.92fr)_minmax(360px,1.08fr)]">
      <section class="shell-section-card auth-brand-panel p-6 sm:p-8">
        <div class="flex items-center gap-3">
          <span class="state-pulse" aria-hidden="true"></span>
          <p class="section-kicker">两步验证</p>
        </div>

        <div class="mt-8 max-w-2xl">
          <h1 class="auth-hero-title">完成最后一步验证</h1>
          <p class="mt-5 text-sm leading-8 text-secondary sm:text-base">
            输入验证码或恢复码以完成登录。
          </p>
        </div>

      </section>

      <section class="shell-section-card auth-form-panel p-6 sm:p-8 md:p-10">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="section-kicker">验证</p>
            <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink">输入验证码继续</h2>
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
              {{ loading ? '验证中...' : '验证并继续' }}
            </el-button>
          </div>
        </el-form>

        <div class="mt-6 flex flex-wrap items-center justify-between gap-3 text-sm">
          <button class="accent-link font-semibold" @click="toggleMode">
            {{ useRecovery ? '改用验证码' : '改用恢复码' }}
          </button>
          <RouterLink class="accent-link font-semibold" to="/login">返回登录</RouterLink>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, ref } from 'vue'
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

<style scoped>
.auth-immersive-shell {
  min-height: 100dvh;
}

.auth-brand-panel,
.auth-form-panel {
  min-height: 100%;
}

.auth-brand-panel {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  gap: 3rem;
  background:
    radial-gradient(circle at 18% 18%, rgba(var(--bc-accent-rgb), 0.12), transparent 30%),
    radial-gradient(circle at 82% 14%, rgba(var(--bc-cyan-rgb), 0.08), transparent 24%),
    linear-gradient(145deg, rgba(var(--bc-ink-rgb), 0.04), transparent 42%),
    var(--panel-bg);
}

.auth-hero-title {
  font-family: theme('fontFamily.display');
  font-size: clamp(2.2rem, 3.3vw, 3.7rem);
  line-height: 0.98;
  letter-spacing: -0.04em;
  color: var(--bc-ink);
}

</style>
