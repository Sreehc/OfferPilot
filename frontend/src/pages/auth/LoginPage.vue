<template>
  <AuthLayout
    skip-label="跳到登录表单"
    brand-kicker="回到你的求职训练"
    brand-description="题库、问答、模拟面试和投递进展都会跟着当前账号保留。"
    side-note="训练记录会跟随当前账号保存，忘记密码时可通过邮箱重置。"
  >
    <div class="flex items-start justify-between gap-4">
      <div>
        <p class="section-kicker">登录</p>
        <h1 class="auth-panel-title mt-4 text-3xl font-semibold text-ink">登录后进入今天的训练</h1>
        <p class="mt-3 text-sm leading-7 text-secondary">输入用户名和密码，若忘记密码可走邮箱验证码重置。</p>
      </div>
    </div>

    <div class="sr-only" aria-live="assertive">
      {{ liveMessage }}
    </div>

    <div
      v-if="formAnnouncement"
      id="login-form-summary"
      ref="formErrorSummaryRef"
      class="auth-feedback-banner mt-6"
      tabindex="-1"
      role="alert"
      aria-live="assertive"
    >
      {{ formAnnouncement }}
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" class="mt-8 space-y-1" label-position="top" @submit.prevent>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" size="large" autocomplete="username" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          placeholder="请输入密码"
          size="large"
          autocomplete="current-password"
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <el-form-item v-if="showCaptcha" label="验证码" prop="captchaCode">
        <div class="captcha-console">
          <el-input
            v-model="form.captchaCode"
            placeholder="请输入验证码"
            size="large"
            class="captcha-console__input"
            @keyup.enter="handleLogin"
          />
          <button type="button" class="captcha-panel" aria-label="刷新验证码" @click="refreshCaptcha">
            <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-panel__image" />
            <div v-else class="captcha-panel__placeholder">正在加载验证码...</div>
          </button>
        </div>
      </el-form-item>

      <div class="mt-6 grid gap-3">
        <el-button
          :loading="loading"
          type="primary"
          size="large"
          class="action-button !min-h-12 w-full transition active:translate-y-px"
          @click="handleLogin"
        >
          {{ loading ? '登录中...' : '登录' }}
        </el-button>
        <div class="auth-links">
          <span class="text-sm text-secondary">
            还没有账号？
            <RouterLink class="accent-link touch-link font-semibold" to="/register">立即注册</RouterLink>
          </span>
          <RouterLink to="/forgot-password" class="accent-link touch-link text-sm font-semibold"> 忘记密码 </RouterLink>
        </div>
        <div class="auth-provider-card">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-sm font-semibold text-ink">当前登录方式</p>
              <p class="mt-1 text-xs leading-6 text-secondary">
                现在使用用户名和密码登录，忘记密码时可通过邮箱验证码重置。
              </p>
            </div>
            <span class="detail-pill">账号密码登录</span>
          </div>
        </div>
      </div>
    </el-form>
  </AuthLayout>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCaptchaApi, type LoginPayload } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from './AuthLayout.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const formErrorSummaryRef = ref<HTMLElement | null>(null)
const loading = ref(false)
const failCount = ref(0)
const captchaImage = ref('')
const captchaKey = ref('')
const formAnnouncement = ref('')
const liveMessage = ref('')

const showCaptcha = computed(() => failCount.value >= 3)
const redirectTarget = computed(() => (route.query.redirect as string) || '/dashboard')
const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const announce = async (message: string) => {
  formAnnouncement.value = message
  liveMessage.value = message
  await nextTick()
  formErrorSummaryRef.value?.focus()
}

const focusFirstInvalidField = async () => {
  await nextTick()
  const formEl = formRef.value?.$el as HTMLElement | undefined
  const invalidInput = formEl?.querySelector('.is-error input, .is-error textarea') as HTMLElement | null
  invalidInput?.focus()
}

const focusCaptchaField = async () => {
  await nextTick()
  const formEl = formRef.value?.$el as HTMLElement | undefined
  const captchaInput = formEl?.querySelector('input[placeholder="请输入验证码"]') as HTMLElement | null
  captchaInput?.focus()
}

const clearAnnouncement = () => {
  formAnnouncement.value = ''
  liveMessage.value = ''
}

const refreshCaptcha = async () => {
  try {
    const response = await fetchCaptchaApi()
    captchaKey.value = response.data.key
    captchaImage.value = response.data.image
  } catch {
    // Silently fail
  }
}

const handleLogin = async () => {
  clearAnnouncement()
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    await announce('请输入用户名和密码。')
    await focusFirstInvalidField()
    return
  }

  if (showCaptcha.value && !form.captchaCode.trim()) {
    await announce('请输入验证码。')
    ElMessage.warning('请输入验证码')
    await focusCaptchaField()
    return
  }

  loading.value = true
  try {
    const payload: LoginPayload = {
      username: form.username.trim(),
      password: form.password
    }
    if (showCaptcha.value && captchaKey.value) {
      payload.captchaKey = captchaKey.value
      payload.captchaCode = form.captchaCode.trim()
    }
    const data = await authStore.login(payload)

    if (data.requires2fa && data.tempToken) {
      await router.push({
        path: '/verify-2fa',
        query: {
          tempToken: data.tempToken,
          redirect: redirectTarget.value
        }
      })
      return
    }

    clearAnnouncement()
    ElMessage.success('登录成功')
    await router.push(redirectTarget.value)
  } catch (error: any) {
    failCount.value++
    if (failCount.value >= 3) {
      await refreshCaptcha()
    }
    const message = error?.message || '登录失败，请检查账号和密码后重试。'
    await announce(message)
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void refreshCaptcha()
})
</script>

<style scoped>
.auth-feedback-banner {
  border-radius: 18px;
  border: 1px solid rgba(195, 71, 71, 0.18);
  background: linear-gradient(180deg, rgba(195, 71, 71, 0.08), transparent 72%), var(--bc-surface-muted);
  padding: 0.95rem 1rem;
  color: var(--bc-ink);
}

.captcha-panel {
  border-radius: 22px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--interactive-bg);
}

.dark .captcha-panel {
  background: var(--interactive-bg);
}

.captcha-console {
  display: grid;
  gap: 12px;
  width: 100%;
  grid-template-columns: minmax(0, 1fr) 188px;
  align-items: stretch;
}

.captcha-console__input {
  min-width: 0;
}

.captcha-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 56px;
  padding: 10px 14px;
  color: var(--bc-ink-secondary);
  transition:
    border-color 160ms ease,
    transform 160ms ease,
    box-shadow 160ms ease;
}

.captcha-panel:hover {
  border-color: rgba(var(--bc-accent-rgb), 0.28);
  box-shadow: 0 10px 24px rgba(var(--bc-ink-rgb), 0.08);
}

.captcha-panel:active {
  transform: translateY(1px);
}

.captcha-panel__image {
  display: block;
  width: 148px;
  height: 40px;
  object-fit: contain;
}

.captcha-panel__placeholder {
  display: flex;
  width: 148px;
  height: 40px;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--bc-ink-tertiary);
}

.auth-links {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.75rem;
}

.auth-provider-card {
  border-radius: 18px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 0.95rem 1rem;
}

.auth-provider-card :deep(button[disabled]) {
  cursor: not-allowed;
  opacity: 0.72;
}

@media (max-width: 640px) {
  .captcha-console {
    grid-template-columns: 1fr;
  }

  .auth-links {
    justify-content: center;
  }

  .captcha-panel {
    width: 100%;
  }

  .auth-provider-card > div {
    flex-direction: column;
    align-items: flex-start;
  }

  .auth-provider-card :deep(button) {
    width: 100%;
  }
}
</style>
