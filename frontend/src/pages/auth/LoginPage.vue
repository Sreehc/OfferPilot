<template>
  <div class="auth-immersive-shell px-4 py-8 md:px-6 md:py-10">
    <div class="auth-viewport mx-auto grid min-h-[calc(100vh-4rem)] max-w-[1180px] items-stretch gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(380px,1.05fr)]">
      <section class="cockpit-panel auth-brand-panel p-6 sm:p-8">
        <div class="flex items-center gap-3">
          <span class="state-pulse" aria-hidden="true"></span>
          <p class="section-kicker">账号登录</p>
        </div>

        <div class="mt-8 max-w-2xl">
          <h1 class="auth-hero-title">快速进入今天的学习</h1>
          <p class="mt-5 text-sm leading-8 text-slate-600 dark:text-slate-300 sm:text-base">
            登录后可继续问答、面试、复习或查看计划。
          </p>
        </div>

        <div class="auth-orbit-grid mt-8">
          <article v-for="item in accessHighlights" :key="item.label" class="data-slab p-4" :class="item.toneClass">
            <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ item.label }}</p>
            <p class="mt-3 text-xl font-semibold text-ink">{{ item.title }}</p>
            <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ item.detail }}</p>
          </article>
        </div>

        <div class="mission-orbit mt-8">
          <p class="text-sm font-semibold text-ink">登录后可继续</p>
          <div class="mission-orbit__track">
            <div
              v-for="(step, index) in nextSteps"
              :key="step.title"
              class="mission-orbit__node"
              :style="{ '--mission-delay': `${index * 90}ms` }"
            >
              <span class="mission-orbit__index">{{ step.index }}</span>
              <div>
                <p class="text-sm font-semibold text-ink">{{ step.title }}</p>
                <p class="mt-1 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ step.detail }}</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="cockpit-panel auth-form-panel p-6 sm:p-8 md:p-10">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="section-kicker">登录</p>
            <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink">输入账号继续</h2>
            <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
              输入用户名和密码即可继续。连续失败 3 次后才需要验证码。
            </p>
          </div>
          <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ showCaptcha ? '需要验证码' : '快速进入' }}</span>
        </div>

        <div class="mt-6 grid gap-3 sm:grid-cols-2">
          <article class="auth-stat-card">
            <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">登录状态</p>
            <p class="mt-2 text-lg font-semibold text-ink">{{ showCaptcha ? '加强校验中' : '常规登录' }}</p>
            <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">{{ showCaptcha ? '请输入验证码后再继续。' : '当前无需额外验证。' }}</p>
          </article>
          <article class="auth-stat-card">
            <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">登录后去向</p>
            <p class="mt-2 text-lg font-semibold text-ink">{{ redirectLabel }}</p>
            <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">登录后会自动跳转。</p>
          </article>
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
              {{ loading ? '登录中...' : '登录并继续' }}
            </el-button>
            <div class="auth-links">
              <span class="text-sm text-slate-500 dark:text-slate-400">
                还没有账号？
                <RouterLink class="accent-link font-semibold" to="/register">立即注册</RouterLink>
              </span>
            </div>
          </div>
        </el-form>

        <div class="auth-footnote mt-8">
          <div class="auth-footnote__item">
            <span class="inline-flex h-2.5 w-2.5 rounded-full bg-[var(--bc-cyan)]"></span>
            登录后可在设置里开启两步验证和设备管理。
          </div>
          <div class="auth-footnote__item">
            <span class="inline-flex h-2.5 w-2.5 rounded-full bg-[var(--bc-amber)]"></span>
            如果你是从其他页面跳转过来，成功后会自动返回。
          </div>
        </div>
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

const accessHighlights = [
  {
    label: '继续学习',
    title: '不中断当前任务',
    detail: '登录成功后会回到你原本想访问的页面，不需要重新找入口。',
    toneClass: 'auth-slab-cyan',
  },
  {
    label: '统一记录',
    title: '所有训练放在同一账号下',
    detail: '问答、面试、错题、复习和计划都会累计到同一个学习记录里。',
    toneClass: 'auth-slab-amber',
  },
  {
    label: '账号安全',
    title: '必要时再增加验证',
    detail: '只有连续失败后才触发验证码，平时不会额外增加登录负担。',
    toneClass: 'auth-slab-lime',
  },
]

const nextSteps = [
  { index: '01', title: '查看今天要做什么', detail: '先看首页中的主任务和待处理项。' },
  { index: '02', title: '继续刚才的训练', detail: '如果你是从问答、面试或复习跳转过来，会直接回到原页面。' },
  { index: '03', title: '把结果沉淀下来', detail: '继续训练后，系统会自动更新错题、复习和计划。' },
]

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
const redirectLabel = computed(() => {
  if (redirectTarget.value.includes('/interview')) return '模拟面试'
  if (redirectTarget.value.includes('/review') || redirectTarget.value.includes('/wrong')) return '错题复习'
  if (redirectTarget.value.includes('/chat')) return '智能问答'
  return '首页概览'
})

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
  justify-content: space-between;
  background:
    radial-gradient(circle at 18% 18%, rgba(var(--bc-accent-rgb), 0.16), transparent 34%),
    radial-gradient(circle at 82% 16%, rgba(85, 214, 190, 0.14), transparent 30%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.06), transparent 42%),
    var(--bc-panel);
}

.auth-hero-title {
  font-family: theme('fontFamily.display');
  font-size: clamp(2.6rem, 4vw, 4.8rem);
  line-height: 0.94;
  letter-spacing: -0.05em;
  color: var(--bc-ink);
}

.auth-orbit-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.auth-slab-cyan {
  border-left-color: var(--bc-cyan);
}

.auth-slab-amber {
  border-left-color: var(--bc-amber);
}

.auth-slab-coral {
  border-left-color: var(--bc-coral);
}

.auth-slab-lime {
  border-left-color: var(--bc-lime);
}

.mission-orbit {
  border-radius: 28px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.32);
  padding: 22px;
}

.dark .mission-orbit {
  background: rgba(255, 255, 255, 0.04);
}

.mission-orbit__track {
  display: grid;
  gap: 14px;
}

.mission-orbit__node {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.mission-orbit__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 16px;
  background: rgba(var(--bc-accent-rgb), 0.14);
  color: var(--bc-accent);
  font-family: theme('fontFamily.mono');
  font-size: 12px;
  font-weight: 700;
}

.auth-stat-card,
.captcha-panel {
  border-radius: 22px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.34);
}

.dark .auth-stat-card,
.dark .captcha-panel {
  background: rgba(255, 255, 255, 0.05);
}

.auth-stat-card {
  padding: 14px;
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

.auth-footnote {
  display: grid;
  gap: 10px;
}

.auth-footnote__item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--bc-ink-secondary);
}

@media (max-width: 1024px) {
  .auth-orbit-grid {
    grid-template-columns: 1fr;
  }
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
