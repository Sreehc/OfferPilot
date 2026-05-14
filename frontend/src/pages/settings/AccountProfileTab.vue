<template>
  <div class="space-y-5">
    <section class="account-overview-grid">
      <article class="shell-section-card p-5 sm:p-6">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="section-kicker">
              账号与邮箱
            </p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
              {{ emailStatusTitle }}
            </h3>
            <p class="mt-3 text-sm leading-7 text-secondary">
              {{ emailStatusDescription }}
            </p>
          </div>
          <span class="detail-pill">{{ authStore.user?.email || '未填写邮箱' }}</span>
        </div>

        <div class="mt-5 flex flex-wrap gap-3">
          <el-button
            :disabled="!authStore.user?.email || authStore.user?.emailVerified"
            :loading="sendingVerificationCode"
            type="primary"
            size="large"
            class="action-button"
            @click="handleSendVerificationCode"
          >
            {{ sendingVerificationCode ? '发送中...' : authStore.user?.emailVerified ? '邮箱已验证' : '发送邮箱验证码' }}
          </el-button>
        </div>

        <div
          v-if="verificationMessage"
          class="account-inline-note mt-4"
          aria-live="polite"
        >
          <p class="font-semibold text-ink">
            {{ verificationMessage }}
          </p>
          <p
            v-if="verificationHint"
            class="mt-1 text-xs text-secondary"
          >
            {{ verificationHint }}
          </p>
        </div>

        <div
          v-if="authStore.user?.email && !authStore.user?.emailVerified"
          class="mt-5"
        >
          <label class="mb-2 block text-sm font-semibold text-ink">输入邮箱验证码</label>
          <div class="account-verify-row">
            <el-input
              ref="verificationInputRef"
              v-model="verificationCode"
              placeholder="6 位验证码"
              size="large"
              maxlength="12"
            />
            <el-button
              :loading="verifyingEmail"
              size="large"
              class="hard-button-secondary !ml-0"
              @click="handleVerifyEmail"
            >
              {{ verifyingEmail ? '验证中...' : '完成验证' }}
            </el-button>
          </div>
        </div>
      </article>

      <article class="shell-section-card p-5 sm:p-6">
        <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
          备用登录方式
        </h3>
        <p class="mt-3 text-sm leading-7 text-secondary">
          这里会显示后续可直接启用的登录方式。当前先使用账号密码登录即可。
        </p>

        <div class="mt-5 space-y-3">
          <article class="account-provider-card">
            <div class="flex items-center justify-between gap-3">
              <div>
                <div class="text-sm font-semibold text-ink">
                  GitHub
                </div>
                <div class="mt-1 text-xs text-secondary">
                  {{ githubProvider?.enabled ? '登录页已可直接使用 GitHub 登录。' : '当前还不能直接使用，开放后会在登录页显示入口。' }}
                </div>
              </div>
              <span class="detail-pill">{{ githubProvider?.enabled ? '已可使用' : '尚不可用' }}</span>
            </div>
          </article>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, nextTick, onMounted, ref } from 'vue'
import { fetchOAuthProvidersApi, sendEmailVerificationCodeApi, verifyEmailCodeApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import type { OAuthProviderInfo } from '@/types/api'

const authStore = useAuthStore()
const verificationCode = ref('')
const verificationInputRef = ref<{ focus?: () => void } | null>(null)
const sendingVerificationCode = ref(false)
const verifyingEmail = ref(false)
const verificationMessage = ref('')
const verificationHint = ref('')
const oauthProviders = ref<OAuthProviderInfo[]>([])

const githubProvider = computed(() => oauthProviders.value.find((item) => item.provider === 'github') ?? null)
const emailStatusTitle = computed(() => (authStore.user?.emailVerified ? '邮箱已验证' : authStore.user?.email ? '邮箱待验证' : '尚未填写邮箱'))
const emailStatusDescription = computed(() => {
  if (!authStore.user?.email) {
    return '先补一个常用邮箱，后面找回密码和接收验证码都会更方便。'
  }
  if (authStore.user.emailVerified) {
    return '这个邮箱已经可以用来接收验证码和恢复账号。'
  }
  return '建议先完成验证，后面找回密码时会更省事。'
})

const loadProviders = async () => {
  try {
    const response = await fetchOAuthProvidersApi()
    oauthProviders.value = response.data
  } catch {
    oauthProviders.value = []
  }
}

const handleSendVerificationCode = async () => {
  sendingVerificationCode.value = true
  try {
    const { data } = await sendEmailVerificationCodeApi()
    verificationMessage.value = data.message || '验证码已发送，请去邮箱查看。'
    verificationHint.value = [data.maskedEmail, data.expiresInMinutes ? `${data.expiresInMinutes} 分钟内有效` : '']
      .filter(Boolean)
      .join(' · ')
    ElMessage.success('验证码已发送')
  } finally {
    sendingVerificationCode.value = false
  }
}

const handleVerifyEmail = async () => {
  if (!verificationCode.value.trim()) {
    verificationMessage.value = '请先输入邮箱验证码。'
    verificationHint.value = '验证码通常会发到你已填写的邮箱里。'
    ElMessage.warning('请输入验证码')
    await nextTick()
    verificationInputRef.value?.focus?.()
    return
  }
  verifyingEmail.value = true
  try {
    const { data } = await verifyEmailCodeApi(verificationCode.value.trim())
    authStore.user = data
    authStore.persistUser()
    verificationMessage.value = '邮箱验证已完成'
    verificationHint.value = data.email || ''
    verificationCode.value = ''
    ElMessage.success('邮箱已验证')
  } finally {
    verifyingEmail.value = false
  }
}

onMounted(() => {
  void loadProviders()
})
</script>

<style scoped>
.account-overview-grid {
  display: grid;
  gap: 1rem;
}

.account-inline-note {
  border-radius: 18px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.16);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.06), transparent 60%), var(--bc-surface-muted);
  padding: 0.95rem 1rem;
}

.account-verify-row {
  display: grid;
  gap: 0.75rem;
}

.account-provider-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 1rem;
}

@media (min-width: 1080px) {
  .account-overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 768px) {
  .account-verify-row {
    grid-template-columns: minmax(0, 1fr) 148px;
  }
}
</style>
