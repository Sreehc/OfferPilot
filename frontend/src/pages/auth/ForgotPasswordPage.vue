<template>
  <div class="auth-immersive-shell px-4 py-8 md:px-6 md:py-10">
    <a
      href="#auth-main"
      class="skip-link"
    >
      跳到密码重置表单
    </a>

    <div
      id="auth-main"
      class="auth-viewport mx-auto grid min-h-[calc(100vh-4rem)] max-w-[1080px] items-stretch gap-4 xl:grid-cols-[minmax(248px,0.52fr)_minmax(440px,1.48fr)]"
    >
      <section class="shell-section-card auth-brand-panel order-2 hidden p-5 sm:p-6 xl:order-1 xl:flex">
        <RouterLink
          to="/login"
          class="auth-brand-mark"
        >
          <AppBrandGlyph :size="38" />
          <div>
            <div class="auth-brand-mark__name">OfferPilot</div>
            <div class="auth-brand-mark__meta">AI 求职训练平台</div>
          </div>
        </RouterLink>

        <div class="flex items-center gap-3">
          <span
            class="state-pulse"
            aria-hidden="true"
          />
          <p class="section-kicker">
            找回密码
          </p>
        </div>

        <div class="mt-4 max-w-md">
          <p class="mt-3 text-sm leading-7 text-secondary">
            输入邮箱、验证码和新密码后即可重置密码。
          </p>
        </div>

        <div class="auth-side-note">验证码会发到你注册时填写的邮箱里，收到后尽快完成重置。</div>
      </section>

      <section class="shell-section-card auth-form-panel order-1 p-6 sm:p-8 md:p-10 xl:order-2">
        <div>
          <p class="section-kicker">
            密码恢复
          </p>
          <h1 class="auth-panel-title mt-4 text-3xl font-semibold text-ink">
            获取验证码，重置密码
          </h1>
          <p class="mt-3 text-sm leading-7 text-secondary">
            如果暂时没有收到邮件，请确认邮箱地址是否正确。
          </p>
        </div>

        <div
          class="sr-only"
          aria-live="assertive"
        >
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
                ref="codeInputRef"
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
            aria-live="polite"
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
                class="accent-link touch-link text-sm font-semibold"
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
import { computed, nextTick, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { forgotPasswordApi, resetPasswordApi } from '@/api/auth'
import AppBrandGlyph from '@/components/AppBrandGlyph.vue'

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
.auth-immersive-shell {
  min-height: 100dvh;
}

.skip-link {
  position: absolute;
  left: 1.25rem;
  top: 0.75rem;
  z-index: 20;
  transform: translateY(-180%);
  border-radius: 999px;
  background: var(--bc-ink);
  color: var(--bc-shell);
  padding: 0.55rem 0.9rem;
  font-size: 0.85rem;
  font-weight: 700;
  transition: transform 160ms ease;
}

.skip-link:focus {
  transform: translateY(0);
}

.auth-brand-panel,
.auth-form-panel {
  min-height: 100%;
}

.auth-brand-panel {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  gap: 0.9rem;
  background:
    radial-gradient(circle at 16% 18%, rgba(var(--bc-accent-rgb), 0.12), transparent 30%),
    radial-gradient(circle at 82% 14%, rgba(var(--bc-cyan-rgb), 0.08), transparent 24%),
    linear-gradient(145deg, rgba(var(--bc-ink-rgb), 0.04), transparent 42%),
    var(--panel-bg);
}

.verification-inline {
  display: grid;
  gap: 0.75rem;
  width: 100%;
}

.auth-side-note {
  max-width: 21rem;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 0.9rem 1rem;
  font-size: 0.88rem;
  line-height: 1.7;
  color: var(--bc-ink-secondary);
}

@media (min-width: 1280px) {
  .auth-form-panel {
    padding-inline: 2.75rem;
  }
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
