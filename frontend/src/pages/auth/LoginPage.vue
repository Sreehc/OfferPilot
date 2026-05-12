<template>
  <div class="auth-immersive-shell px-4 py-8 md:px-6 md:py-10">
    <div class="auth-viewport mx-auto grid min-h-[calc(100vh-4rem)] max-w-[1180px] items-stretch gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(380px,1.05fr)]">
      <section class="shell-section-card auth-brand-panel p-6 sm:p-8">
        <div class="flex items-center gap-3">
          <span class="state-pulse" aria-hidden="true"></span>
          <p class="section-kicker">账号登录</p>
        </div>

        <div class="mt-8 max-w-2xl">
          <h1 class="auth-hero-title">快速进入今天的学习</h1>
          <p class="mt-5 text-sm leading-8 text-slate-600 dark:text-slate-300 sm:text-base">
            登录后继续学习。
          </p>
        </div>

      </section>

      <section class="shell-section-card auth-form-panel p-6 sm:p-8 md:p-10">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="section-kicker">登录</p>
            <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink">输入账号继续</h2>
            <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
              输入用户名和密码。
            </p>
          </div>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="mt-8 space-y-1"
          label-position="top"
          @submit.prevent
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              autocomplete="username"
            />
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
                class="flex-1"
                @keyup.enter="handleLogin"
              />
              <button
                type="button"
                class="captcha-panel"
                @click="refreshCaptcha"
              >
                <img
                  v-if="captchaImage"
                  :src="captchaImage"
                  alt="验证码"
                  class="h-14 w-[148px] object-cover"
                />
                <div v-else class="flex h-14 w-[148px] items-center justify-center text-xs text-slate-400">
                  加载中...
                </div>
                <span class="captcha-panel__hint">点击刷新</span>
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
              <span class="text-sm text-slate-500 dark:text-slate-400">
                还没有账号？
                <RouterLink class="accent-link font-semibold" to="/register">立即注册</RouterLink>
              </span>
            </div>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCaptchaApi, type LoginPayload } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const failCount = ref(0)
const captchaImage = ref('')
const captchaKey = ref('')

const showCaptcha = computed(() => failCount.value >= 3)
const redirectTarget = computed(() => (route.query.redirect as string) || '/dashboard')
const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
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
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (showCaptcha.value && !form.captchaCode.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }

  loading.value = true
  try {
    const payload: LoginPayload = {
      username: form.username.trim(),
      password: form.password,
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
          redirect: redirectTarget.value,
        }
      })
      return
    }

    ElMessage.success('登录成功')
    await router.push(redirectTarget.value)
  } catch (error: any) {
    failCount.value++
    if (failCount.value >= 3) {
      await refreshCaptcha()
    }
    ElMessage.error(error?.message || '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-immersive-shell {
  min-height: 100dvh;
}

.auth-viewport {
  align-items: stretch;
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
    radial-gradient(circle at 82% 16%, rgba(47, 127, 119, 0.08), transparent 24%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.18), transparent 42%),
    var(--bc-panel);
}

.auth-hero-title {
  font-family: theme('fontFamily.display');
  font-size: clamp(2.25rem, 3.4vw, 4rem);
  line-height: 0.98;
  letter-spacing: -0.04em;
  color: var(--bc-ink);
}

.captcha-panel {
  border-radius: 22px;
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.72);
}

.dark .captcha-panel {
  background: rgba(255, 255, 255, 0.05);
}

.captcha-console {
  display: grid;
  gap: 12px;
  width: 100%;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.captcha-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px;
  color: var(--bc-ink-secondary);
}

.captcha-panel__hint {
  font-size: 11px;
}

.auth-links {
  display: flex;
  justify-content: center;
}

@media (max-width: 640px) {
  .captcha-console {
    grid-template-columns: 1fr;
  }

  .captcha-panel {
    width: 100%;
  }
}
</style>
