<template>
  <div class="auth-immersive-shell px-4 py-8 md:px-6 md:py-10">
    <div class="auth-viewport mx-auto grid min-h-[calc(100vh-4rem)] max-w-[1180px] items-stretch gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(380px,1.05fr)]">
      <section class="shell-section-card auth-brand-panel p-6 sm:p-8">
        <div class="flex items-center gap-3">
          <span
            class="state-pulse"
            aria-hidden="true"
          />
          <p class="section-kicker">
            账号登录
          </p>
        </div>

        <div class="mt-8 max-w-2xl">
          <h1 class="auth-hero-title">
            登录后继续今天的训练
          </h1>
          <p class="mt-5 text-sm leading-8 text-secondary sm:text-base">
            回到题库、问答、模拟面试、简历和投递记录，继续推进你当前的求职准备。
          </p>
        </div>

        <div class="mt-10 grid gap-4 sm:grid-cols-3">
          <div class="auth-feature-card">
            <div class="auth-feature-card__icon">
              🧠
            </div>
            <p class="auth-feature-card__title">
              AI 间隔记忆
            </p>
            <p class="auth-feature-card__desc">
              根据遗忘曲线自动安排复习，科学高效。
            </p>
          </div>
          <div class="auth-feature-card">
            <div class="auth-feature-card__icon">
              🎯
            </div>
            <p class="auth-feature-card__title">
              模拟面试
            </p>
            <p class="auth-feature-card__desc">
              AI 评分 + 标准答案 + 追问，查漏补缺。
            </p>
          </div>
          <div class="auth-feature-card">
            <div class="auth-feature-card__icon">
              📚
            </div>
            <p class="auth-feature-card__title">
              知识卡片
            </p>
            <p class="auth-feature-card__desc">
              上传资料，自动生成结构化学习卡片。
            </p>
          </div>
        </div>
      </section>

      <section class="shell-section-card auth-form-panel p-6 sm:p-8 md:p-10">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="section-kicker">
              登录
            </p>
            <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink">
              输入账号继续
            </h2>
            <p class="mt-3 text-sm leading-7 text-secondary">
              输入用户名和密码，若忘记密码可走邮箱验证码重置。
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
          <el-form-item
            label="用户名"
            prop="username"
          >
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item
            label="密码"
            prop="password"
          >
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

          <el-form-item
            v-if="showCaptcha"
            label="验证码"
            prop="captchaCode"
          >
            <div class="captcha-console">
              <el-input
                v-model="form.captchaCode"
                placeholder="请输入验证码"
                size="large"
                class="captcha-console__input"
                @keyup.enter="handleLogin"
              />
              <button
                type="button"
                class="captcha-panel"
                aria-label="刷新验证码"
                @click="refreshCaptcha"
              >
                <img
                  v-if="captchaImage"
                  :src="captchaImage"
                  alt="验证码"
                  class="captcha-panel__image"
                >
                <div
                  v-else
                  class="captcha-panel__placeholder"
                >
                  加载中...
                </div>
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
                <RouterLink
                  class="accent-link font-semibold"
                  to="/register"
                >立即注册</RouterLink>
              </span>
              <RouterLink
                to="/forgot-password"
                class="accent-link text-sm font-semibold"
              >
                忘记密码
              </RouterLink>
            </div>
            <div class="auth-provider-card">
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-sm font-semibold text-ink">
                    其他登录方式
                  </p>
                  <p class="mt-1 text-xs leading-6 text-secondary">
                    GitHub 登录准备好后，会直接出现在这里。
                  </p>
                </div>
                <button
                  type="button"
                  class="hard-button-secondary text-sm"
                  :disabled="!githubProvider?.enabled"
                >
                  {{ githubProvider?.enabled ? '使用 GitHub 登录' : 'GitHub 登录暂未开放' }}
                </button>
              </div>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCaptchaApi, fetchOAuthProvidersApi, type LoginPayload } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import type { OAuthProviderInfo } from '@/types/api'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const failCount = ref(0)
const captchaImage = ref('')
const captchaKey = ref('')
const oauthProviders = ref<OAuthProviderInfo[]>([])

const showCaptcha = computed(() => failCount.value >= 3)
const redirectTarget = computed(() => (route.query.redirect as string) || '/dashboard')
const githubProvider = computed(() => oauthProviders.value.find((item) => item.provider === 'github') ?? null)
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

const loadOAuthProviders = async () => {
  try {
    const response = await fetchOAuthProvidersApi()
    oauthProviders.value = response.data
  } catch {
    oauthProviders.value = []
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

onMounted(() => {
  void loadOAuthProviders()
})
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
    radial-gradient(circle at 82% 16%, rgba(var(--bc-cyan-rgb), 0.08), transparent 24%),
    linear-gradient(145deg, rgba(var(--bc-ink-rgb), 0.04), transparent 42%),
    var(--panel-bg);
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

.auth-feature-card {
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.12);
  background: var(--panel-muted);
  padding: 16px;
}

.auth-feature-card__icon {
  font-size: 1.5rem;
  line-height: 1;
}

.auth-feature-card__title {
  margin-top: 10px;
  font-size: 14px;
  font-weight: 700;
  color: var(--bc-ink);
}

.auth-feature-card__desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--bc-ink-secondary);
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
