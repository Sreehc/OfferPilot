<template>
  <div class="flex min-h-screen items-center justify-center px-4 py-8">
    <section class="paper-panel w-full max-w-md p-8 md:p-10">
      <h1 class="text-2xl font-bold tracking-[-0.03em] text-ink text-center">登录 ByteCoach</h1>
      <p class="mt-2 text-sm text-slate-400 text-center">Java 面试准备，一站搞定</p>

      <!-- Feature chips -->
      <div class="mt-5 flex flex-wrap justify-center gap-2">
        <span v-for="f in featureLabels" :key="f"
          class="rounded-full bg-slate-100 dark:bg-slate-800 px-2.5 py-1 text-[11px] font-medium text-slate-500 dark:text-slate-400"
        >{{ f }}</span>
      </div>

      <div class="rule-divider mt-6"></div>

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
    </section>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCaptchaApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const featureLabels = ['AI 问答', '模拟面试', '错题复习', '学习计划']

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
    // Silently fail
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
