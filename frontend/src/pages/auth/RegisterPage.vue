<template>
  <AuthLayout
    skip-label="跳到注册表单"
    brand-kicker="创建账号"
    brand-description="注册后可以保存训练记录，并用于找回账号。"
    side-note="一个账号就能串起题库、问答、模拟面试和投递记录，邮箱也能帮你恢复登录。"
  >
    <div class="flex items-start justify-between gap-4">
      <div>
        <p class="section-kicker">注册</p>
        <h1 class="auth-panel-title mt-4 text-3xl font-semibold text-ink">填好信息后开始使用</h1>
        <p class="mt-3 text-sm leading-7 text-secondary">用户名、邮箱和密码准备好后，就能直接进入工作台。</p>
      </div>
    </div>

    <div class="sr-only" aria-live="assertive">
      {{ liveMessage }}
    </div>

    <div
      v-if="formAnnouncement"
      id="register-form-summary"
      ref="formErrorSummaryRef"
      class="auth-feedback-banner mt-6"
      tabindex="-1"
      role="alert"
      aria-live="assertive"
    >
      {{ formAnnouncement }}
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" class="mt-8" label-position="top" @submit.prevent>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="例如：Spring猎人" size="large" maxlength="32" />
      </el-form-item>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" size="large" maxlength="32" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input
          v-model="form.email"
          placeholder="you@example.com"
          size="large"
          maxlength="128"
          autocomplete="email"
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
          {{ loading ? '创建中...' : '创建账号' }}
        </el-button>
        <div class="auth-links">
          <span class="text-sm text-secondary">
            已有账号？
            <RouterLink to="/login" class="accent-link touch-link font-semibold">返回登录</RouterLink>
          </span>
        </div>
      </div>
    </el-form>
  </AuthLayout>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { nextTick, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { RegisterPayload } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from './AuthLayout.vue'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const formErrorSummaryRef = ref<HTMLElement | null>(null)
const loading = ref(false)
const formAnnouncement = ref('')
const liveMessage = ref('')

const form = reactive<RegisterPayload>({
  nickname: '',
  username: '',
  email: '',
  password: ''
})

const rules: FormRules<typeof form> = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 32, message: '昵称不能超过 32 个字符', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, message: '用户名至少 2 个字符', trigger: 'blur' },
    { max: 32, message: '用户名不能超过 32 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' },
    { max: 128, message: '邮箱不能超过 128 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' },
    { max: 64, message: '密码不能超过 64 个字符', trigger: 'blur' }
  ]
}

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

const handleRegister = async () => {
  clearAnnouncement()
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    await announce('请补全注册信息。')
    await focusFirstInvalidField()
    return
  }
  loading.value = true
  try {
    await authStore.register({
      nickname: form.nickname.trim(),
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password
    })
    clearAnnouncement()
    ElMessage.success('注册成功，已自动登录')
    await router.push('/dashboard')
  } catch (error: any) {
    await announce(error?.message || '注册失败，请检查信息后重试。')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
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
</style>
