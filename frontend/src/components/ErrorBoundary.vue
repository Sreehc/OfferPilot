<template>
  <div v-if="error" class="flex min-h-screen items-center justify-center bg-slate-50 p-6">
    <div class="paper-panel max-w-lg p-8 text-center">
      <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-red-50">
        <svg class="h-8 w-8 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <h2 class="mt-5 text-2xl font-semibold tracking-[-0.03em] text-ink">页面出现了错误</h2>
      <p class="mt-3 text-sm leading-7 text-slate-600">
        抱歉，渲染过程中发生了异常。你可以尝试刷新页面，或者返回首页。
      </p>
      <div class="mt-6 flex justify-center gap-3">
        <button type="button" class="hard-button-primary" @click="handleReload">刷新页面</button>
        <RouterLink to="/" class="hard-button-secondary" @click="handleReset">返回首页</RouterLink>
      </div>
      <details v-if="errorInfo" class="mt-6 text-left">
        <summary class="cursor-pointer text-xs uppercase tracking-[0.22em] text-slate-400">错误详情</summary>
        <pre class="mt-2 overflow-auto rounded-lg bg-slate-100 p-3 text-xs text-slate-600">{{ errorInfo }}</pre>
      </details>
    </div>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
import { onErrorCaptured, ref } from 'vue'

const error = ref<Error | null>(null)
const errorInfo = ref('')

onErrorCaptured((err: Error, _instance, info: string) => {
  error.value = err
  errorInfo.value = `${err.message}\n\nComponent lifecycle: ${info}\n\n${err.stack ?? ''}`
  return false
})

const handleReload = () => {
  window.location.reload()
}

const handleReset = () => {
  error.value = null
  errorInfo.value = ''
}
</script>
