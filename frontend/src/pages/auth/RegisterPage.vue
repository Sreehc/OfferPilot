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
            创建账号后开始学习。
          </p>
        </div>

      </section>

      <section class="cockpit-panel auth-form-panel p-6 sm:p-8 md:p-10">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="section-kicker">注册</p>
            <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink">填写三个字段即可</h2>
            <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
              填写昵称、用户名和密码。
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
              {{ loading ? '创建中...' : '创建账号' }}
            </el-button>
            <div class="auth-links">
              <span class="text-sm text-slate-500 dark:text-slate-400">
                已有账号？
                <RouterLink to="/login" class="accent-link font-semibold">返回登录</RouterLink>
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

.auth-links {
  display: flex;
  justify-content: center;
}
</style>
