<template>
  <div class="space-y-5">
    <section class="account-overview-grid">
      <article class="shell-section-card p-5 sm:p-6">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="section-kicker">
              邮箱状态
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
            {{ sendingVerificationCode ? '发送中...' : authStore.user?.emailVerified ? '已完成验证' : '发送验证验证码' }}
          </el-button>
        </div>

        <div
          v-if="verificationMessage"
          class="account-inline-note mt-4"
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
          <p
            v-if="debugCode"
            class="mt-2 text-xs text-accent"
          >
            开发验证码：{{ debugCode }}
          </p>
        </div>

        <div
          v-if="authStore.user?.email && !authStore.user?.emailVerified"
          class="mt-5"
        >
          <label class="mb-2 block text-sm font-semibold text-ink">输入邮箱验证码</label>
          <div class="account-verify-row">
            <el-input
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
              {{ verifyingEmail ? '验证中...' : '确认验证' }}
            </el-button>
          </div>
        </div>
      </article>

      <article class="shell-section-card p-5 sm:p-6">
        <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
          其他登录方式
        </h3>
        <p class="mt-3 text-sm leading-7 text-secondary">
          GitHub 登录开放后，你可以直接在这里查看状态并切换使用。
        </p>

        <div class="mt-5 space-y-3">
          <article class="account-provider-card">
            <div class="flex items-center justify-between gap-3">
              <div>
                <div class="text-sm font-semibold text-ink">
                  GitHub
                </div>
                <div class="mt-1 text-xs text-secondary">
                  {{ githubProvider?.configured ? '登录配置已经准备好。' : '当前还不能使用 GitHub 登录。' }}
                </div>
              </div>
              <span class="detail-pill">{{ githubProvider?.enabled ? '可使用' : '暂未开放' }}</span>
            </div>
          </article>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { fetchOAuthProvidersApi, sendEmailVerificationCodeApi, verifyEmailCodeApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import type { OAuthProviderInfo } from '@/types/api'

const authStore = useAuthStore()
const verificationCode = ref('')
const sendingVerificationCode = ref(false)
const verifyingEmail = ref(false)
const verificationMessage = ref('')
const verificationHint = ref('')
const debugCode = ref('')
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
    verificationMessage.value = data.message
    verificationHint.value = [data.maskedEmail, data.expiresInMinutes ? `${data.expiresInMinutes} 分钟内有效` : '']
      .filter(Boolean)
      .join(' · ')
    debugCode.value = data.debugCode || ''
    ElMessage.success('验证码发送成功')
  } finally {
    sendingVerificationCode.value = false
  }
}

const handleVerifyEmail = async () => {
  if (!verificationCode.value.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  verifyingEmail.value = true
  try {
    const { data } = await verifyEmailCodeApi(verificationCode.value.trim())
    authStore.user = data
    authStore.persistUser()
    verificationMessage.value = '邮箱验证已完成'
    verificationHint.value = data.email || ''
    debugCode.value = ''
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
