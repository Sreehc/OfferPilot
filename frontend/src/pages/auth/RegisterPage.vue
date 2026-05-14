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
            创建账号
          </p>
        </div>

        <div class="mt-8 max-w-2xl">
          <h1 class="auth-hero-title">
            先建好账号，把后续资料与训练沉淀到同一处
          </h1>
          <p class="mt-5 text-sm leading-8 text-secondary sm:text-base">
            先把账号和邮箱准备好，后面整理简历、上传资料和恢复账号都会更顺手。
          </p>
        </div>

        <div class="mt-10 grid gap-4 sm:grid-cols-3">
          <div class="auth-feature-card">
            <div class="auth-feature-card__title">
              保存训练记录
            </div>
            <p class="auth-feature-card__desc">
              题库、问答、模拟面试和投递记录都会跟着同一个账号走。
            </p>
          </div>
          <div class="auth-feature-card">
            <div class="auth-feature-card__title">
              整理你的资料
            </div>
            <p class="auth-feature-card__desc">
              上传资料、整理简历和项目后，后续训练会更连贯。
            </p>
          </div>
          <div class="auth-feature-card">
            <div class="auth-feature-card__title">
              方便找回账号
            </div>
            <p class="auth-feature-card__desc">
              先填好邮箱，后面忘记密码时可以更快恢复登录。
            </p>
          </div>
        </div>
      </section>

      <section class="shell-section-card auth-form-panel p-6 sm:p-8 md:p-10">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="section-kicker">
              注册
            </p>
            <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink">
              填好信息后开始使用
            </h2>
            <p class="mt-3 text-sm leading-7 text-secondary">
              用户名、邮箱和密码准备好后，就能直接进入工作台。
            </p>
          </div>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="mt-8"
          label-position="top"
          @submit.prevent
        >
          <el-form-item
            label="昵称"
            prop="nickname"
          >
            <el-input
              v-model="form.nickname"
              placeholder="例如：Spring猎人"
              size="large"
              maxlength="32"
            />
          </el-form-item>
          <el-form-item
            label="用户名"
            prop="username"
          >
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              maxlength="32"
            />
          </el-form-item>
          <el-form-item
            label="邮箱"
            prop="email"
          >
            <el-input
              v-model="form.email"
              placeholder="you@example.com"
              size="large"
              maxlength="128"
              autocomplete="email"
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
              {{ loading ? '创建中...' : '创建账号' }}
            </el-button>
            <div class="auth-links">
              <span class="text-sm text-secondary">
                已有账号？
                <RouterLink
                  to="/login"
                  class="accent-link font-semibold"
                >返回登录</RouterLink>
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
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { RegisterPayload } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<RegisterPayload>({
  nickname: '',
  username: '',
  email: '',
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
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' },
    { max: 128, message: '邮箱不能超过 128 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' },
    { max: 64, message: '密码不能超过 64 个字符', trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.register({
      nickname: form.nickname.trim(),
      username: form.username.trim(),
      email: form.email.trim(),
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
  justify-content: flex-start;
  gap: 3rem;
  background:
    radial-gradient(circle at 16% 18%, rgba(var(--bc-accent-rgb), 0.12), transparent 30%),
    radial-gradient(circle at 82% 14%, rgba(var(--bc-cyan-rgb), 0.08), transparent 24%),
    linear-gradient(145deg, rgba(var(--bc-ink-rgb), 0.04), transparent 42%),
    var(--panel-bg);
}

.auth-hero-title {
  font-family: theme('fontFamily.display');
  font-size: clamp(2.25rem, 3.4vw, 3.9rem);
  line-height: 0.98;
  letter-spacing: -0.04em;
  color: var(--bc-ink);
}

.auth-feature-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 1rem;
}

.auth-feature-card__title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.auth-feature-card__desc {
  margin-top: 0.6rem;
  font-size: 0.88rem;
  line-height: 1.75;
  color: var(--bc-ink-secondary);
}

.auth-links {
  display: flex;
  justify-content: center;
}
</style>
