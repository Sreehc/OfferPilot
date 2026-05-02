<template>
  <div class="flex min-h-screen items-center justify-center px-4 py-8">
    <section class="paper-panel w-full max-w-2xl p-8 md:p-10">
      <p class="section-kicker">Create Account</p>
      <h1 class="mt-4 text-4xl font-semibold tracking-[-0.03em] text-ink">创建账号后，直接进入你的学习首页</h1>
      <p class="mt-4 max-w-xl text-sm leading-7 text-slate-600 dark:text-slate-300">
        注册只保留最小字段：`username`、`password`、`nickname`。成功后会自动签发 token，并直接进入 Dashboard。
      </p>
      <div class="rule-divider mt-6"></div>

      <el-form ref="formRef" :model="form" :rules="rules" class="mt-8" label-position="top" @submit.prevent>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="ByteCoach" size="large" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="demo" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="123456" size="large" />
        </el-form-item>
        <div class="flex flex-col gap-3 pt-4 md:flex-row">
          <el-button :loading="loading" type="primary" size="large" class="action-button transition active:translate-y-px md:min-w-40" @click="handleRegister">
            注册
          </el-button>
          <RouterLink to="/login" class="accent-link inline-flex items-center text-sm">已有账号，返回登录</RouterLink>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  nickname: 'ByteCoach',
  username: 'demo',
  password: '123456'
})

const rules: FormRules<typeof form> = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleRegister = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.register(form)
    ElMessage.success('注册成功，已自动登录')
    await router.push('/dashboard')
  } catch {
    // Message is handled by the request interceptor.
  } finally {
    loading.value = false
  }
}
</script>
