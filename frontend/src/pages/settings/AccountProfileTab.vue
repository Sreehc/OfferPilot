<template>
  <div class="space-y-5">
    <section class="account-overview-grid">
      <article class="shell-section-card p-5 sm:p-6">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="section-kicker">
              头像与展示
            </p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
              更新你的头像
            </h3>
            <p class="mt-3 text-sm leading-7 text-secondary">
              上传一张清晰头像，社区、评论和导航里的个人入口都会同步显示。
            </p>
          </div>

          <div class="account-avatar-shell">
            <img
              v-if="avatarPreview"
              :src="avatarPreview"
              alt="用户头像"
              class="account-avatar-image"
            />
            <span v-else class="account-avatar-fallback">
              {{ avatarInitial }}
            </span>
          </div>
        </div>

        <div class="mt-5 flex flex-wrap gap-3">
          <input
            ref="avatarInputRef"
            type="file"
            accept="image/png,image/jpeg,image/webp,image/gif"
            class="sr-only"
            @change="handleAvatarChange"
          >
          <el-button
            :loading="uploadingAvatar"
            type="primary"
            size="large"
            class="action-button"
            @click="avatarInputRef?.click()"
          >
            {{ uploadingAvatar ? '上传中...' : '上传头像' }}
          </el-button>
          <span class="text-xs text-secondary">支持 PNG、JPG、WebP、GIF，建议 2MB 以内。</span>
        </div>
      </article>

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

    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, nextTick, ref } from 'vue'
import { sendEmailVerificationCodeApi, uploadAvatarApi, verifyEmailCodeApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const avatarInputRef = ref<HTMLInputElement | null>(null)
const verificationCode = ref('')
const verificationInputRef = ref<{ focus?: () => void } | null>(null)
const sendingVerificationCode = ref(false)
const verifyingEmail = ref(false)
const uploadingAvatar = ref(false)
const verificationMessage = ref('')
const verificationHint = ref('')
const avatarPreview = computed(() => authStore.user?.avatar || '')
const avatarInitial = computed(() => (authStore.user?.nickname || authStore.user?.username || 'U').slice(0, 1).toUpperCase())
const emailStatusTitle = computed(() => (authStore.user?.emailVerified ? '邮箱已验证' : authStore.user?.email ? '邮箱待验证' : '当前未填写邮箱'))
const emailStatusDescription = computed(() => {
  if (!authStore.user?.email) {
    return '建议填写常用邮箱，方便找回密码和接收验证码。'
  }
  if (authStore.user.emailVerified) {
    return '这个邮箱已经可以用来接收验证码和恢复账号。'
  }
  return '建议完成邮箱验证，方便后续找回密码。'
})

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
    verificationMessage.value = '请输入邮箱验证码。'
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

const resetAvatarInput = () => {
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
}

const handleAvatarChange = async (event: Event) => {
  const input = event.target as HTMLInputElement | null
  const file = input?.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    resetAvatarInput()
    return
  }

  uploadingAvatar.value = true
  try {
    const { data } = await uploadAvatarApi(file)
    if (authStore.user) {
      authStore.user = {
        ...authStore.user,
        avatar: data
      }
      authStore.persistUser()
    }
    ElMessage.success('头像已更新')
  } finally {
    uploadingAvatar.value = false
    resetAvatarInput()
  }
}
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

.account-avatar-shell {
  display: inline-flex;
  height: 5rem;
  width: 5rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 1.5rem;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.18);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.12), transparent 72%), var(--bc-surface-muted);
}

.account-avatar-image {
  height: 100%;
  width: 100%;
  object-fit: cover;
}

.account-avatar-fallback {
  color: var(--bc-ink);
  font-size: 1.6rem;
  font-weight: 700;
}

@media (min-width: 1080px) {
  .account-overview-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (min-width: 768px) {
  .account-verify-row {
    grid-template-columns: minmax(0, 1fr) 148px;
  }
}
</style>
