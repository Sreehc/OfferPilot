<template>
  <div v-if="error" class="flex min-h-screen items-center justify-center p-6" style="background: var(--page-bg);">
    <div class="shell-section-card max-w-lg p-8 text-center">
      <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-coral/10">
        <UiIcon name="warning" class="h-8 w-8 text-red-500" />
      </div>
      <h2 class="mt-5 text-2xl font-semibold tracking-[-0.03em] text-ink">页面出现了错误</h2>
      <p class="mt-3 text-sm leading-7 text-secondary">
        抱歉，页面出了点问题。你可以尝试刷新页面，或者返回首页。
      </p>
      <div class="mt-6 flex justify-center gap-3">
        <button type="button" class="hard-button-primary" @click="handleReload">刷新页面</button>
        <RouterLink to="/" class="hard-button-secondary" @click="handleReset">返回首页</RouterLink>
      </div>
      <details v-if="errorInfo" class="mt-6 text-left">
        <summary class="cursor-pointer text-xs uppercase tracking-[0.22em] text-tertiary">错误详情</summary>
        <pre class="mt-2 overflow-auto rounded-lg p-3 text-xs text-secondary" style="background: var(--panel-muted); border: 1px solid var(--border-subtle);">{{ errorInfo }}</pre>
      </details>
    </div>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
import { onErrorCaptured, ref } from 'vue'
import { UiIcon } from '@/components/ui'

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
