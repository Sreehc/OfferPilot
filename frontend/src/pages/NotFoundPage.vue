<template>
  <div class="flex min-h-screen items-center justify-center px-4">
    <section class="paper-panel max-w-lg p-10 text-center">
      <!-- 404 icon -->
      <div class="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-slate-100 dark:bg-slate-800">
        <span class="text-3xl font-bold text-slate-300 dark:text-slate-600">?</span>
      </div>
      <p class="mt-6 section-kicker">404</p>
      <h1 class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink">页面不存在</h1>
      <p class="mt-3 text-sm leading-6 text-slate-500 dark:text-slate-400">
        你访问的页面不存在或已被移除，请检查地址是否正确。
      </p>
      <div class="mt-8 flex items-center justify-center gap-3">
        <button type="button" class="hard-button-secondary" @click="goBack">
          返回上页
        </button>
        <RouterLink to="/dashboard" class="hard-button-primary">
          回到首页
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/dashboard')
  }
}
</script>
