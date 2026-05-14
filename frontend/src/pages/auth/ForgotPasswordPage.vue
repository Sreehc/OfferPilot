<template>
  <div class="auth-immersive-shell px-4 py-8 md:px-6 md:py-10">
    <div
      class="auth-viewport mx-auto grid min-h-[calc(100vh-4rem)] max-w-[1180px] items-stretch gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(380px,1.05fr)]"
    >
      <section class="shell-section-card auth-brand-panel p-6 sm:p-8">
        <div class="flex items-center gap-3">
          <span
            class="state-pulse"
            aria-hidden="true"
          />
          <p class="section-kicker">
            找回密码
          </p>
        </div>

        <div class="mt-8 max-w-2xl">
          <h1 class="auth-hero-title">
            用邮箱验证码恢复账号访问
          </h1>
          <p class="mt-5 text-sm leading-8 text-secondary sm:text-base">
            先拿到验证码，再设置新密码，很快就能回到账号里继续使用。
          </p>
        </div>

        <div class="mt-10 grid gap-4 sm:grid-cols-3">
          <div class="auth-feature-card">
            <div class="auth-feature-card__title">
              1. 发送验证码
            </div>
            <p class="auth-feature-card__desc">
              输入注册邮箱，把验证码发到你的邮箱里。
            </p>
          </div>
          <div class="auth-feature-card">
            <div class="auth-feature-card__title">
              2. 输入新密码
            </div>
            <p class="auth-feature-card__desc">
              验证码有效期内提交新密码即可完成重置。
            </p>
          </div>
          <div class="auth-feature-card">
            <div class="auth-feature-card__title">
              3. 返回登录
            </div>
            <p class="auth-feature-card__desc">
              重置成功后返回登录页继续使用原账号。
            </p>
          </div>
        </div>
      </section>

      <section class="shell-section-card auth-form-panel p-6 sm:p-8 md:p-10">
        <div>
          <p class="section-kicker">
            密码恢复
          </p>
          <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink">
            先拿到验证码，再重置密码
          </h2>
          <p class="mt-3 text-sm leading-7 text-secondary">
            如果你现在收不到邮件，可以先确认邮箱是否填写正确。
          </p>
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
            label="邮箱"
            prop="email"
          >
            <el-input
              v-model="form.email"
              placeholder="you@example.com"
              size="large"
              autocomplete="email"
            />
          </el-form-item>

          <el-form-item
            label="验证码"
            prop="code"
          >
            <div class="verification-inline">
              <el-input
                v-model="form.code"
                placeholder="输入 6 位验证码"
                size="large"
                maxlength="12"
              />
              <el-button
                :loading="sending"
                size="large"
                class="hard-button-secondary !ml-0"
                @click="handleSendCode"
              >
                {{ sending ? '发送中...' : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item
            label="新密码"
            prop="newPassword"
          >
            <el-input
              v-model="form.newPassword"
              type="password"
              show-password
              placeholder="至少 6 位"
              size="large"
              autocomplete="new-password"
            />
          </el-form-item>

          <div
            v-if="deliveryMessage"
            class="auth-inline-note"
          >
            <p class="font-semibold text-ink">
              {{ deliveryMessage }}
            </p>
            <p
              v-if="maskedEmail || expiresText"
              class="mt-1 text-xs text-secondary"
            >
              <span v-if="maskedEmail">目标邮箱：{{ maskedEmail }}</span>
              <span v-if="maskedEmail && expiresText"> · </span>
              <span v-if="expiresText">{{ expiresText }}</span>
            </p>
            <p
              v-if="debugCode"
              class="mt-2 text-xs text-accent"
            >
              开发验证码：{{ debugCode }}
            </p>
          </div>

          <div class="mt-6 grid gap-3">
            <el-button
              :loading="submitting"
              type="primary"
              size="large"
              class="action-button !min-h-12 w-full transition active:translate-y-px"
              @click="handleResetPassword"
            >
              {{ submitting ? '重置中...' : '重置密码' }}
            </el-button>
            <div class="auth-links">
              <RouterLink
                to="/login"
                class="accent-link text-sm font-semibold"
              >
                返回登录
              </RouterLink>
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
import { useRouter } from 'vue-router'
import { forgotPasswordApi, resetPasswordApi } from '@/api/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const sending = ref(false)
const submitting = ref(false)
const deliveryMessage = ref('')
const maskedEmail = ref('')
const expiresMinutes = ref<number | null>(null)
const debugCode = ref('')

const form = reactive({
  email: '',
  code: '',
  newPassword: ''
})

const rules: FormRules<typeof form> = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' }
  ]
}

const expiresText = computed(() => (expiresMinutes.value ? `${expiresMinutes.value} 分钟内有效` : ''))

const handleSendCode = async () => {
  const valid = await formRef.value?.validateField('email').catch(() => false)
  if (valid === false) return

  sending.value = true
  try {
    const { data } = await forgotPasswordApi(form.email.trim())
    deliveryMessage.value = data.message
    maskedEmail.value = data.maskedEmail || ''
    expiresMinutes.value = data.expiresInMinutes ?? null
    debugCode.value = data.debugCode || ''
    ElMessage.success('验证码发送结果已更新')
  } finally {
    sending.value = false
  }
}

const handleResetPassword = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await resetPasswordApi({
      email: form.email.trim(),
      code: form.code.trim(),
      newPassword: form.newPassword
    })
    ElMessage.success('密码已重置，请重新登录')
    await router.push('/login')
  } finally {
    submitting.value = false
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

.verification-inline {
  display: grid;
  gap: 0.75rem;
  width: 100%;
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

.auth-inline-note {
  border-radius: 18px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.16);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.06), transparent 60%), var(--bc-surface-muted);
  padding: 0.95rem 1rem;
}

.auth-links {
  display: flex;
  justify-content: center;
}

@media (min-width: 768px) {
  .verification-inline {
    grid-template-columns: minmax(0, 1fr) 168px;
    align-items: start;
  }
}
</style>
