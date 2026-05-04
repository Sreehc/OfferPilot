<template>
  <Transition name="fade">
    <div
      v-if="visible"
      class="fixed inset-0 z-[9999] flex items-center justify-center bg-white/95 dark:bg-slate-900/95 backdrop-blur-sm"
    >
      <div class="text-center px-6">
        <!-- Wifi off icon -->
        <div class="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-red-50 dark:bg-red-900/20">
          <svg class="h-10 w-10 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M18.364 5.636a9 9 0 010 12.728m0 0l-2.829-2.829m2.829 2.829L21 21M15.536 8.464a5 5 0 010 7.072m0 0l-2.829-2.829m-4.243 2.829a4.978 4.978 0 01-1.414-2.83m-1.414 5.658a9 9 0 01-2.167-9.238m7.824 2.167a1 1 0 111.414 1.414m-1.414-1.414L3 3" />
          </svg>
        </div>

        <h2 class="mt-6 text-xl font-semibold text-ink">网络已断开</h2>
        <p class="mt-2 max-w-sm text-sm text-slate-500 dark:text-slate-400">
          无法连接到服务器，请检查网络连接后重试。
        </p>

        <button
          type="button"
          class="hard-button-primary mt-6"
          @click="handleRetry"
        >
          <svg v-if="retrying" class="mr-2 h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
          {{ retrying ? '重试中...' : '重新连接' }}
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const visible = ref(false)
const retrying = ref(false)

const checkOnline = () => {
  visible.value = !navigator.onLine
}

const handleRetry = async () => {
  retrying.value = true
  try {
    // Try a lightweight fetch to check connectivity
    await fetch('/api/health', { method: 'HEAD', cache: 'no-store' })
    visible.value = false
  } catch {
    // Still offline
  } finally {
    retrying.value = false
  }
}

onMounted(() => {
  window.addEventListener('online', checkOnline)
  window.addEventListener('offline', checkOnline)
  checkOnline()
})

onUnmounted(() => {
  window.removeEventListener('online', checkOnline)
  window.removeEventListener('offline', checkOnline)
})
</script>
