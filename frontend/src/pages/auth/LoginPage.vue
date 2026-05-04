<template>
  <div class="flex min-h-screen items-center justify-center px-4 py-8">
    <div class="grid w-full max-w-6xl gap-5 lg:grid-cols-[1.1fr_0.9fr]">
      <section class="paper-panel relative overflow-hidden p-8 md:p-12">
        <div
          class="absolute -left-8 top-10 h-36 w-44 -rotate-6 bg-[rgba(47,79,157,0.08)] blur-2xl"
          style="border-radius: var(--radius-lg);"
        ></div>
        <div
          class="absolute bottom-8 right-8 h-44 w-52 rotate-6 bg-[rgba(131,149,189,0.14)] blur-2xl"
          style="border-radius: var(--radius-lg);"
        ></div>
        <p class="section-kicker relative">ByteCoach</p>
        <h1 class="page-title relative mt-5 max-w-xl">
          进入你的 Java 面试准备驾驶舱，问答、面试、错题、计划一站搞定。
        </h1>
        <p class="page-subtitle relative mt-5">
          登录之后，首页会把学习概览、最近面试、错题数量、薄弱点和计划完成率放进同一个工作台，帮助你决定下一步该做什么。
        </p>

        <div class="relative mt-12 grid gap-4 md:grid-cols-3">
          <article class="metric-card">
            <p class="metric-label">智能问答</p>
            <p class="metric-value">AI</p>
            <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">支持普通问答和知识库问答两种模式</p>
          </article>
          <article class="metric-card">
            <p class="metric-label">模拟面试</p>
            <p class="metric-value">3-5</p>
            <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">每场 3-5 题，AI 实时评分和追问</p>
          </article>
          <article class="metric-card">
            <p class="metric-label">学习闭环</p>
            <p class="metric-value">4</p>
            <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">问答、面试、错题、计划四段闭环</p>
          </article>
        </div>
      </section>

      <section class="paper-panel p-8 md:p-10">
        <p class="section-kicker">Sign In</p>
        <h2 class="mt-4 text-3xl font-semibold tracking-[-0.03em] text-ink">进入 ByteCoach</h2>
        <div class="rule-divider mt-6"></div>
        <el-form ref="formRef" :model="form" :rules="rules" class="mt-8" label-position="top" @submit.prevent>
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
              <div
                class="cursor-pointer shrink-0 rounded border border-slate-200 dark:border-slate-700 overflow-hidden"
                @click="refreshCaptcha"
              >
                <img
                  v-if="captchaImage"
                  :src="captchaImage"
                  alt="验证码"
                  class="h-10 w-[120px] object-cover"
                />
                <div v-else class="flex h-10 w-[120px] items-center justify-center text-xs text-slate-400">
                  加载中...
                </div>
              </div>
            </div>
          </el-form-item>

          <el-button :loading="loading" type="primary" size="large" class="action-button mt-4 w-full transition active:translate-y-px" @click="handleLogin">
            登录
          </el-button>
        </el-form>
        <div class="mt-6 flex items-center justify-between text-sm text-slate-500 dark:text-slate-400">
          <span>登录后直接进入 Dashboard 总览</span>
          <RouterLink class="accent-link" to="/register">创建账号</RouterLink>
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
