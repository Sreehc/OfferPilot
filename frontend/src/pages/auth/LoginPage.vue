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
          进入一个围绕 Java 面试准备设计的学习驾驶舱，而不是普通聊天页。
        </h1>
        <p class="page-subtitle relative mt-5">
          登录之后，首页会把学习概览、最近面试、错题数量、薄弱点和计划完成率放进同一个工作台，帮助你决定下一步该做什么。
        </p>

        <div class="relative mt-12 grid gap-4 md:grid-cols-3">
          <article class="metric-card">
            <p class="metric-label">Core Loop</p>
            <p class="metric-value">6</p>
            <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">登录、问答、面试、错题、计划、Dashboard</p>
          </article>
          <article class="metric-card">
            <p class="metric-label">Dashboard</p>
            <p class="metric-value">Live</p>
            <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">学习概览、面试结果、薄弱点与快捷入口统一呈现</p>
          </article>
          <article class="metric-card">
            <p class="metric-label">Auth</p>
            <p class="metric-value">JWT</p>
            <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">注册、登录、恢复登录态、退出登录形成完整闭环</p>
          </article>
        </div>
      </section>

      <section class="paper-panel p-8 md:p-10">
        <p class="section-kicker">Sign In</p>
        <h2 class="mt-4 text-3xl font-semibold tracking-[-0.03em] text-ink">进入 ByteCoach</h2>
        <div class="rule-divider mt-6"></div>
        <el-form ref="formRef" :model="form" :rules="rules" class="mt-8" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="demo" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="123456" size="large" />
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
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: 'demo',
  password: '123456'
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    await router.push((route.query.redirect as string) || '/dashboard')
  } catch {
    // Message is handled by the request interceptor.
  } finally {
    loading.value = false
  }
}
</script>
