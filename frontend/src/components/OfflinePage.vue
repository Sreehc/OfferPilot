<template>
  <Transition name="fade">
    <div
      v-if="visible"
      class="fixed inset-0 z-[9999] flex items-center justify-center backdrop-blur-md"
      style="background: color-mix(in srgb, var(--page-bg) 84%, transparent);"
    >
      <div class="shell-section-card max-w-md px-8 py-10 text-center">
        <!-- Wifi off icon -->
        <div class="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-coral/10">
          <UiIcon name="connection" class="h-10 w-10 text-red-400" />
        </div>

        <h2 class="mt-6 text-xl font-semibold text-ink">网络已断开</h2>
        <p class="mt-2 max-w-sm text-sm text-secondary">
          无法连接到服务器，请检查网络连接后重试。
        </p>

        <button type="button" class="hard-button-primary mt-6" @click="handleRetry">
          <UiIcon v-if="retrying" name="loading" class="mr-2 h-4 w-4 animate-spin" />
          {{ retrying ? '重试中...' : '重新连接' }}
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { UiIcon } from '@/components/ui'

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
