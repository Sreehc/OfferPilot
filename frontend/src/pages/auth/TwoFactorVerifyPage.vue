<template>
  <div class="auth-immersive-shell px-4 py-8 md:px-6 md:py-10">
    <div class="auth-viewport mx-auto grid min-h-[calc(100vh-4rem)] max-w-[1180px] items-stretch gap-4 xl:grid-cols-[minmax(0,1.02fr)_minmax(360px,0.98fr)]">
      <section class="cockpit-panel auth-brand-panel p-6 sm:p-8">
        <div class="flex items-center gap-3">
          <span class="state-pulse" aria-hidden="true"></span>
          <p class="section-kicker">Security Checkpoint</p>
        </div>

        <div class="mt-8 max-w-2xl">
          <h1 class="auth-hero-title">完成最后一道安全校验</h1>
          <p class="mt-5 text-sm leading-8 text-slate-600 dark:text-slate-300 sm:text-base">
            你已经通过账号密码校验，接下来只需要输入验证码或恢复码。验证通过后会立刻回到训练现场，不会打断原本的跳转目标。
          </p>
        </div>

        <div class="auth-orbit-grid mt-8">
          <article v-for="signal in verifySignals" :key="signal.label" class="data-slab p-4" :class="signal.toneClass">
            <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ signal.label }}</p>
            <p class="mt-3 text-xl font-semibold text-ink">{{ signal.title }}</p>
            <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ signal.detail }}</p>
          </article>
        </div>

        <div class="mission-orbit mt-8">
          <div class="mission-orbit__track">
            <div v-for="step in verifySteps" :key="step.index" class="mission-orbit__node">
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
            <p class="section-kicker">Two-Factor Verify</p>
            <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink">两步验证</h2>
            <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
              {{ useRecovery ? '请输入恢复码完成登录。' : '请输入身份验证器中的 6 位验证码。' }}
            </p>
          </div>
          <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ useRecovery ? 'Recovery' : 'Authenticator' }}</span>
        </div>

        <div class="mt-6 grid gap-3 sm:grid-cols-2">
          <article class="auth-stat-card" :class="useRecovery ? 'auth-slab-coral' : 'auth-slab-cyan'">
            <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">Mode</p>
            <p class="mt-2 text-lg font-semibold text-ink">{{ useRecovery ? '恢复码' : '验证码' }}</p>
            <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
              {{ useRecovery ? '适用于无法访问身份验证器的情况。' : '来自你的身份验证器应用。' }}
            </p>
          </article>
          <article class="auth-stat-card auth-slab-amber">
            <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">Redirect</p>
            <p class="mt-2 text-lg font-semibold text-ink">{{ redirectLabel }}</p>
            <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">验证成功后的落点。</p>
          </article>
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
              {{ loading ? '验证中...' : '完成验证并进入训练舱' }}
            </el-button>
          </div>
        </el-form>

        <div class="mt-6 flex flex-wrap items-center justify-between gap-3 text-sm">
          <button class="accent-link font-semibold" @click="toggleMode">
            {{ useRecovery ? '改用验证码' : '改用恢复码' }}
          </button>
          <RouterLink class="accent-link font-semibold" to="/login">返回登录</RouterLink>
        </div>

        <div class="auth-footnote mt-8">
          <div class="auth-footnote__item">
            <span class="inline-flex h-2.5 w-2.5 rounded-full bg-[var(--bc-cyan)]"></span>
            恢复码通常只在身份验证器不可用时使用。
          </div>
          <div class="auth-footnote__item">
            <span class="inline-flex h-2.5 w-2.5 rounded-full bg-[var(--bc-coral)]"></span>
            如果验证已过期，返回登录页重新发起即可。
          </div>
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

const verifySignals = [
  {
    label: 'Checkpoint',
    title: '最后校验',
    detail: '这一步完成后就会恢复你的完整登录态。',
    toneClass: 'auth-slab-cyan',
  },
  {
    label: 'Fallback',
    title: '恢复码',
    detail: '当身份验证器不可用时，可以切换到恢复码模式。',
    toneClass: 'auth-slab-coral',
  },
  {
    label: 'Return',
    title: '原路返回',
    detail: '验证成功后会跳回你最初要访问的页面。',
    toneClass: 'auth-slab-amber',
  },
  {
    label: 'State',
    title: '不中断训练',
    detail: '这一步只做安全确认，不会重置训练上下文。',
    toneClass: 'auth-slab-lime',
  },
]

const verifySteps = [
  { index: '01', title: '读取验证码', detail: '从身份验证器中读取当前 6 位验证码。' },
  { index: '02', title: '提交校验', detail: '系统会用临时令牌完成最后一步验证。' },
  { index: '03', title: '恢复上下文', detail: '验证完成后直接回到原始训练页面。' },
]

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const code = ref('')
const loading = ref(false)
const useRecovery = ref(false)

const tempToken = route.query.tempToken as string
const redirectTarget = computed(() => (route.query.redirect as string) || '/dashboard')
const redirectLabel = computed(() => {
  if (redirectTarget.value.includes('/interview')) return '面试舱'
  if (redirectTarget.value.includes('/review')) return '记忆回放'
  if (redirectTarget.value.includes('/chat')) return '知识潜航'
  return '任务看板'
})

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
  justify-content: space-between;
  background:
    radial-gradient(circle at 18% 18%, rgba(var(--bc-accent-rgb), 0.14), transparent 34%),
    radial-gradient(circle at 82% 14%, rgba(85, 214, 190, 0.14), transparent 30%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.06), transparent 42%),
    var(--bc-panel);
}

.auth-hero-title {
  font-family: theme('fontFamily.display');
  font-size: clamp(2.5rem, 4vw, 4.4rem);
  line-height: 0.94;
  letter-spacing: -0.05em;
  color: var(--bc-ink);
}

.auth-orbit-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
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

.auth-stat-card {
  border-radius: 22px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.34);
  padding: 14px;
}

.dark .auth-stat-card {
  background: rgba(255, 255, 255, 0.05);
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
</style>
