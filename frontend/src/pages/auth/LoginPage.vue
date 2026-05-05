<template>
  <div class="flex min-h-screen items-center justify-center px-4 py-8">
    <div class="w-full max-w-5xl lg:grid lg:gap-8 lg:grid-cols-[1fr_1fr]">
      <!-- Left: Feature intro -->
      <section class="paper-panel relative overflow-hidden p-8 md:p-12 hidden lg:flex items-center">
        <div
          class="absolute -left-12 top-0 h-48 w-48 -rotate-6 bg-[rgba(47,79,157,0.06)] blur-3xl"
          style="border-radius: var(--radius-lg);"
        ></div>
        <div
          class="absolute bottom-0 right-0 h-40 w-40 rotate-12 bg-[rgba(131,149,189,0.1)] blur-3xl"
          style="border-radius: var(--radius-lg);"
        ></div>

        <div class="relative w-full">
          <h1 class="text-3xl font-bold tracking-[-0.03em] text-ink leading-tight">
            Java 面试准备，<br>一站搞定。
          </h1>
          <p class="mt-4 text-base leading-7 text-slate-500 dark:text-slate-400">
            AI 驱动的问答、模拟面试、错题复习、学习计划四段闭环。
          </p>

          <div class="mt-10 space-y-4">
            <div v-for="feature in features" :key="feature.title" class="flex items-start gap-3">
              <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-accent/10 text-accent">
                <svg class="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" :d="feature.icon" />
                </svg>
              </div>
              <div>
                <div class="text-sm font-semibold text-ink">{{ feature.title }}</div>
                <div class="mt-0.5 text-xs text-slate-400">{{ feature.desc }}</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Right: Login form -->
      <section class="paper-panel p-8 md:p-10 max-w-md mx-auto lg:max-w-none lg:flex lg:items-center">
        <div class="w-full">
          <h2 class="text-2xl font-semibold tracking-[-0.03em] text-ink">登录 ByteCoach</h2>
          <p class="mt-2 text-sm text-slate-400">输入你的账号密码开始学习</p>

          <el-form ref="formRef" :model="form" :rules="rules" class="mt-6 space-y-1" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large" />
          </el-form-item>

          <!-- Captcha: shown after 3 consecutive failures -->
          <el-form-item v-if="showCaptcha" label="验证码" prop="captchaCode">
            <div class="flex items-center gap-3 w-full">
              <el-input
                v-model="form.captchaCode"
                placeholder="请输入验证码"
                size="large"
                class="flex-1"
                @keyup.enter="handleLogin"
              />
              <div class="shrink-0 text-center">
                <div
                  class="cursor-pointer rounded border border-slate-200 dark:border-slate-700 overflow-hidden"
                  @click="refreshCaptcha"
                >
                  <img
                    v-if="captchaImage"
                    :src="captchaImage"
                    alt="验证码"
                    class="h-12 w-[140px] object-cover"
                  />
                  <div v-else class="flex h-12 w-[140px] items-center justify-center text-xs text-slate-400">
                    加载中...
                  </div>
                </div>
                <p class="mt-1 text-[10px] text-slate-400">点击刷新</p>
              </div>
            </div>
          </el-form-item>

          <el-button :loading="loading" type="primary" size="large" class="action-button mt-6 w-full transition active:translate-y-px" @click="handleLogin">
            登录
          </el-button>
        </el-form>

          <div class="mt-6 text-center text-sm text-slate-400">
            还没有账号？
            <RouterLink class="font-semibold text-accent hover:underline" to="/register">立即注册</RouterLink>
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
import { useRoute, useRouter } from 'vue-router'
import { fetchCaptchaApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const features = [
  { title: '智能问答', desc: '普通问答 + 知识库 RAG 两种模式', icon: 'M8.625 12a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H8.25m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H12m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 01-2.555-.337A5.972 5.972 0 015.41 20.97a5.969 5.969 0 01-.474-.065 4.48 4.48 0 00.978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25z' },
  { title: '模拟面试', desc: '3-5 题/场，AI 实时评分和追问', icon: 'M12 18v-5.25m0 0a6.01 6.01 0 001.5-.189m-1.5.189a6.01 6.01 0 01-1.5-.189m3.75 7.478a12.06 12.06 0 01-4.5 0m3.75 2.383a14.406 14.406 0 01-3 0M14.25 18v-.192c0-.983.658-1.823 1.508-2.316a7.5 7.5 0 10-7.517 0c.85.493 1.509 1.333 1.509 2.316V18' },
  { title: '错题复习', desc: 'SM-2 遗忘曲线自动调度复习', icon: 'M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25' },
  { title: '学习计划', desc: 'AI 根据薄弱点自动生成每日任务', icon: 'M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25zM6.75 12h.008v.008H6.75V12zm0 3h.008v.008H6.75V15zm0 3h.008v.008H6.75V18z' }
]

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const failCount = ref(0)
const captchaImage = ref('')
const captchaKey = ref('')

const showCaptcha = computed(() => failCount.value >= 3)

const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const refreshCaptcha = async () => {
  try {
    const response = await fetchCaptchaApi()
    captchaKey.value = response.data.key
    captchaImage.value = response.data.image
  } catch {
    // Silently fail - captcha is optional enhancement
  }
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (showCaptcha.value && !form.captchaCode.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }

  loading.value = true
  try {
    const payload: Record<string, string> = {
      username: form.username,
      password: form.password
    }
    if (showCaptcha.value && captchaKey.value) {
      payload.captchaKey = captchaKey.value
      payload.captchaCode = form.captchaCode
    }
    const data = await authStore.login(payload)

    // Check if 2FA is required
    if (data.requires2fa && data.tempToken) {
      await router.push({
        path: '/verify-2fa',
        query: {
          tempToken: data.tempToken,
          redirect: route.query.redirect as string || '/dashboard'
        }
      })
      return
    }

    ElMessage.success('登录成功')
    await router.push((route.query.redirect as string) || '/dashboard')
  } catch {
    failCount.value++
    if (failCount.value >= 3) {
      await refreshCaptcha()
    }
  } finally {
    loading.value = false
  }
}
</script>
