<template>
  <AuthLayout
    skip-label="跳到密码重置表单"
    brand-kicker="找回密码"
    brand-description="输入邮箱、验证码和新密码后即可重置密码。"
    side-note="验证码会发到你注册时填写的邮箱里，收到后尽快完成重置。"
  >
    <div>
      <p class="section-kicker">密码恢复</p>
      <h1 class="auth-panel-title mt-4 text-3xl font-semibold text-ink">获取验证码，重置密码</h1>
      <p class="mt-3 text-sm leading-7 text-secondary">如果暂时没有收到邮件，请确认邮箱地址是否正确。</p>
    </div>

    <div class="sr-only" aria-live="assertive">
      {{ liveMessage }}
    </div>

    <div
      v-if="formAnnouncement"
      ref="formErrorSummaryRef"
      class="auth-feedback-banner mt-6"
      tabindex="-1"
      role="alert"
      aria-live="assertive"
    >
      {{ formAnnouncement }}
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" class="mt-8" label-position="top" @submit.prevent>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="you@example.com" size="large" autocomplete="email" />
      </el-form-item>

      <el-form-item label="验证码" prop="code">
        <div class="verification-inline">
          <el-input ref="codeInputRef" v-model="form.code" placeholder="输入 6 位验证码" size="large" maxlength="12" />
          <el-button :loading="sending" size="large" class="hard-button-secondary !ml-0" @click="handleSendCode">
            {{ sending ? '发送中...' : '发送验证码' }}
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          placeholder="至少 6 位"
          size="large"
          autocomplete="new-password"
        />
      </el-form-item>

      <div v-if="deliveryMessage" class="auth-inline-note" aria-live="polite">
        <p class="font-semibold text-ink">
          {{ deliveryMessage }}
        </p>
        <p v-if="maskedEmail || expiresText" class="mt-1 text-xs text-secondary">
          <span v-if="maskedEmail">目标邮箱：{{ maskedEmail }}</span>
          <span v-if="maskedEmail && expiresText"> · </span>
          <span v-if="expiresText">{{ expiresText }}</span>
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
          <RouterLink to="/login" class="accent-link touch-link text-sm font-semibold"> 返回登录 </RouterLink>
        </div>
      </div>
    </el-form>
  </AuthLayout>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { computed, nextTick, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { forgotPasswordApi, resetPasswordApi } from '@/api/auth'
import AuthLayout from './AuthLayout.vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const formErrorSummaryRef = ref<HTMLElement | null>(null)
const codeInputRef = ref<{ focus?: () => void } | null>(null)
const sending = ref(false)
const submitting = ref(false)
const deliveryMessage = ref('')
const maskedEmail = ref('')
const expiresMinutes = ref<number | null>(null)
const formAnnouncement = ref('')
const liveMessage = ref('')

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

const clearAnnouncement = () => {
  formAnnouncement.value = ''
  liveMessage.value = ''
}

const handleSendCode = async () => {
  clearAnnouncement()
  const valid = await formRef.value?.validateField('email').catch(() => false)
  if (valid === false) {
    await announce('请输入可接收邮件的邮箱地址。')
    await focusFirstInvalidField()
    return
  }

  sending.value = true
  try {
    const { data } = await forgotPasswordApi(form.email.trim())
    deliveryMessage.value = data.message || '验证码已发送，请去邮箱查看。'
    maskedEmail.value = data.maskedEmail || ''
    expiresMinutes.value = data.expiresInMinutes ?? null
    clearAnnouncement()
    ElMessage.success('验证码发送结果已更新')
  } catch (error: any) {
    await announce(error?.message || '验证码发送失败，请检查邮箱地址后重试。')
  } finally {
    sending.value = false
  }
}

const handleResetPassword = async () => {
  clearAnnouncement()
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    await announce('请补全邮箱、验证码和新密码。')
    await focusFirstInvalidField()
    return
  }

  submitting.value = true
  try {
    await resetPasswordApi({
      email: form.email.trim(),
      code: form.code.trim(),
      newPassword: form.newPassword
    })
    clearAnnouncement()
    ElMessage.success('密码已重置，请重新登录')
    await router.push('/login')
  } catch (error: any) {
    await announce(error?.message || '密码重置失败，请核对验证码和新密码后重试。')
    await nextTick()
    codeInputRef.value?.focus?.()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.verification-inline {
  display: grid;
  gap: 0.75rem;
  width: 100%;
}

.auth-inline-note {
  border-radius: 18px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.16);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.06), transparent 60%), var(--bc-surface-muted);
  padding: 0.95rem 1rem;
}

.auth-feedback-banner {
  border-radius: 18px;
  border: 1px solid rgba(195, 71, 71, 0.18);
  background: linear-gradient(180deg, rgba(195, 71, 71, 0.08), transparent 72%), var(--bc-surface-muted);
  padding: 0.95rem 1rem;
  color: var(--bc-ink);
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
