<template>
  <div class="auth-immersive-shell px-4 py-8 md:px-6 md:py-10">
    <div class="auth-viewport mx-auto grid min-h-[calc(100vh-4rem)] max-w-[1280px] items-stretch gap-4 xl:grid-cols-[minmax(0,1.05fr)_minmax(380px,0.95fr)]">
      <section class="cockpit-panel auth-brand-panel p-6 sm:p-8">
        <div class="flex items-center gap-3">
          <span class="state-pulse" aria-hidden="true"></span>
          <p class="section-kicker">ByteCoach Signup</p>
        </div>

        <div class="mt-8 max-w-2xl">
          <h1 class="auth-hero-title">创建你的训练身份</h1>
          <p class="mt-5 text-sm leading-8 text-slate-600 dark:text-slate-300 sm:text-base">
            一个账号会把你的问答、面试、错题和复习轨迹串到同一条训练链路里。注册完成后直接进入首页，不需要再做额外设置。
          </p>
        </div>

        <div class="auth-orbit-grid mt-8">
          <article v-for="signal in setupSignals" :key="signal.label" class="data-slab p-4" :class="signal.toneClass">
            <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ signal.label }}</p>
            <p class="mt-3 text-xl font-semibold text-ink">{{ signal.title }}</p>
            <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ signal.detail }}</p>
          </article>
        </div>

        <div class="mission-orbit mt-8">
          <div class="mission-orbit__track">
            <div
              v-for="step in setupSteps"
              :key="step.index"
              class="mission-orbit__node"
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
            <p class="section-kicker">Register</p>
            <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink">创建账号</h2>
            <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
              只需昵称、用户名和密码。完成后系统会自动登录，并把你送到训练主看板。
            </p>
          </div>
          <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ readinessLabel }}</span>
        </div>

        <div class="mt-6 grid gap-3 sm:grid-cols-3">
          <article v-for="item in readinessSignals" :key="item.label" class="auth-stat-card" :class="item.toneClass">
            <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ item.label }}</p>
            <p class="mt-2 text-lg font-semibold text-ink">{{ item.value }}</p>
            <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">{{ item.detail }}</p>
          </article>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="mt-8"
          label-position="top"
          @submit.prevent
        >
          <el-form-item label="昵称" prop="nickname">
            <el-input
              v-model="form.nickname"
              placeholder="例如：Spring猎人"
              size="large"
              maxlength="32"
            />
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              maxlength="32"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="至少 6 位"
              size="large"
              maxlength="64"
              @keyup.enter="handleRegister"
            />
          </el-form-item>

          <div class="mt-6 grid gap-3">
            <el-button
              :loading="loading"
              type="primary"
              size="large"
              class="action-button !min-h-12 w-full transition active:translate-y-px"
              @click="handleRegister"
            >
              {{ loading ? '创建中...' : '创建账号并进入训练舱' }}
            </el-button>
            <div class="auth-links">
              <span class="text-sm text-slate-500 dark:text-slate-400">
                已有账号？
                <RouterLink to="/login" class="accent-link font-semibold">返回登录</RouterLink>
              </span>
            </div>
          </div>
        </el-form>

        <div class="auth-footnote mt-8">
          <div class="auth-footnote__item">
            <span class="inline-flex h-2.5 w-2.5 rounded-full bg-[var(--bc-lime)]"></span>
            注册成功后自动持久化当前设备。
          </div>
          <div class="auth-footnote__item">
            <span class="inline-flex h-2.5 w-2.5 rounded-full bg-[var(--bc-cyan)]"></span>
            后续可在设置页继续开启 2FA 与设备管理。
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
import { useRouter } from 'vue-router'
import type { RegisterPayload } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const setupSignals = [
  {
    label: 'Identity',
    title: '训练身份',
    detail: '昵称会作为社区和学习 cockpit 里的主显示名。',
    toneClass: 'auth-slab-cyan',
  },
  {
    label: 'Device',
    title: '设备绑定',
    detail: '注册时会自动记录当前设备，方便后续安全管理。',
    toneClass: 'auth-slab-amber',
  },
  {
    label: 'Launch',
    title: '直接进入',
    detail: '成功后不再停留在认证流，会直接进入首页开始训练。',
    toneClass: 'auth-slab-lime',
  },
  {
    label: 'Growth',
    title: '长期轨迹',
    detail: '你的问答、面试和复习都会累计到同一个学习档案里。',
    toneClass: 'auth-slab-coral',
  },
]

const setupSteps = [
  { index: '01', title: '创建身份', detail: '先留下你在社区和训练 cockpit 中使用的昵称。' },
  { index: '02', title: '绑定设备', detail: '系统会自动把当前设备接入安全链路。' },
  { index: '03', title: '进入看板', detail: '注册成功后直接进入首页开始第一轮训练。' },
]

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<RegisterPayload>({
  nickname: '',
  username: '',
  password: '',
})

const rules: FormRules<typeof form> = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 32, message: '昵称不能超过 32 个字符', trigger: 'blur' },
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, message: '用户名至少 2 个字符', trigger: 'blur' },
    { max: 32, message: '用户名不能超过 32 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' },
    { max: 64, message: '密码不能超过 64 个字符', trigger: 'blur' },
  ],
}

const readinessScore = computed(() => {
  let score = 0
  if (form.nickname.trim().length >= 2) score += 35
  if (form.username.trim().length >= 2) score += 30
  if (form.password.length >= 6) score += 35
  return score
})

const readinessLabel = computed(() => {
  if (readinessScore.value >= 100) return 'Ready'
  if (readinessScore.value >= 65) return 'Almost'
  return 'Draft'
})

const readinessSignals = computed(() => [
  {
    label: 'Nickname',
    value: form.nickname.trim().length || 0,
    detail: '建议使用后续愿意公开展示的学习身份。',
    toneClass: 'auth-slab-cyan',
  },
  {
    label: 'Username',
    value: form.username.trim().length || 0,
    detail: '保持简洁，后续用于登录。',
    toneClass: 'auth-slab-amber',
  },
  {
    label: 'Readiness',
    value: `${readinessScore.value}%`,
    detail: '三个字段填满后即可直接进入训练舱。',
    toneClass: readinessScore.value >= 100 ? 'auth-slab-lime' : 'auth-slab-coral',
  },
])

const handleRegister = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.register({
      nickname: form.nickname.trim(),
      username: form.username.trim(),
      password: form.password,
    })
    ElMessage.success('注册成功，已自动登录')
    await router.push('/dashboard')
  } catch {
    // Message is handled by the request interceptor.
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
    radial-gradient(circle at 16% 18%, rgba(var(--bc-accent-rgb), 0.14), transparent 34%),
    radial-gradient(circle at 82% 14%, rgba(85, 214, 190, 0.14), transparent 30%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.06), transparent 42%),
    var(--bc-panel);
}

.auth-hero-title {
  font-family: theme('fontFamily.display');
  font-size: clamp(2.6rem, 4vw, 4.6rem);
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
</style>
