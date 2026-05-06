<template>
  <div class="auth-immersive-shell px-4 py-8 md:px-6 md:py-10">
    <div class="auth-viewport mx-auto grid min-h-[calc(100vh-4rem)] max-w-[1180px] items-stretch gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(380px,1.05fr)]">
      <section class="cockpit-panel auth-brand-panel p-6 sm:p-8">
        <div class="flex items-center gap-3">
          <span class="state-pulse" aria-hidden="true"></span>
          <p class="section-kicker">创建账号</p>
        </div>

        <div class="mt-8 max-w-2xl">
          <h1 class="auth-hero-title">用一个账号开始学习</h1>
          <p class="mt-5 text-sm leading-8 text-slate-600 dark:text-slate-300 sm:text-base">
            注册后会自动登录，并直接进入首页。之后你的问答、面试、错题和复习都会记录在这个账号下，方便持续学习。
          </p>
        </div>

        <div class="auth-orbit-grid mt-8">
          <article v-for="signal in setupHighlights" :key="signal.label" class="data-slab p-4" :class="signal.toneClass">
            <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ signal.label }}</p>
            <p class="mt-3 text-xl font-semibold text-ink">{{ signal.title }}</p>
            <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ signal.detail }}</p>
          </article>
        </div>

        <div class="mission-orbit mt-8">
          <p class="text-sm font-semibold text-ink">创建后会发生什么</p>
          <div class="mission-orbit__track">
            <div
              v-for="step in registerSteps"
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
            <p class="section-kicker">注册</p>
            <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink">填写三个字段即可</h2>
            <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
              只需昵称、用户名和密码。完成后会自动登录，不需要额外配置。
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
              {{ loading ? '创建中...' : '创建账号并开始学习' }}
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
            注册成功后会自动记录当前设备，后续可在设置里管理。
          </div>
          <div class="auth-footnote__item">
            <span class="inline-flex h-2.5 w-2.5 rounded-full bg-[var(--bc-cyan)]"></span>
            后续可按需要开启两步验证，不影响现在先开始学习。
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

const setupHighlights = [
  {
    label: '学习记录',
    title: '所有训练集中保留',
    detail: '问答、面试、错题和复习都会归到同一个账号下。',
    toneClass: 'auth-slab-cyan',
  },
  {
    label: '快速开始',
    title: '注册后自动登录',
    detail: '创建完成后直接进入首页，不需要再回登录页。',
    toneClass: 'auth-slab-amber',
  },
  {
    label: '账号安全',
    title: '后续再补充安全设置',
    detail: '设备管理和两步验证都可以在进入系统后再慢慢完成。',
    toneClass: 'auth-slab-lime',
  },
]

const registerSteps = [
  { index: '01', title: '填写昵称和账号', detail: '昵称用于展示，用户名用于后续登录。' },
  { index: '02', title: '设置密码', detail: '密码至少 6 位，之后可在设置里进一步增强安全性。' },
  { index: '03', title: '自动进入首页', detail: '创建成功后直接开始学习，不需要再次登录。' },
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
  if (readinessScore.value >= 100) return '可创建'
  if (readinessScore.value >= 65) return '快完成'
  return '填写中'
})

const readinessSignals = computed(() => [
  {
    label: '昵称',
    value: form.nickname.trim().length || 0,
    detail: '建议使用后续愿意公开展示的名字。',
    toneClass: 'auth-slab-cyan',
  },
  {
    label: '用户名',
    value: form.username.trim().length || 0,
    detail: '保持简洁，后续用于登录。',
    toneClass: 'auth-slab-amber',
  },
  {
    label: '完成度',
    value: `${readinessScore.value}%`,
    detail: '三个字段填写完成后即可直接进入首页。',
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
