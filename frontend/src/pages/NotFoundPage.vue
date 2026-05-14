<template>
  <div class="flex min-h-screen items-center justify-center px-4">
    <section class="shell-section-card max-w-lg p-10 text-center">
      <div class="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-[var(--panel-muted)]">
        <span class="text-3xl font-bold text-tertiary">?</span>
      </div>
      <p class="mt-6 section-kicker">404</p>
      <h1 class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink">页面不存在</h1>
      <p class="mt-3 text-sm leading-6 text-secondary">
        你访问的页面可能已被移除，或当前地址不正确。
      </p>
      <div class="mt-8 flex items-center justify-center gap-3">
        <button type="button" class="hard-button-secondary" @click="goBack">
          返回上一页
        </button>
        <RouterLink :to="homePath" class="hard-button-primary">
          {{ homeLabel }}
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const homePath = computed(() => (authStore.isLoggedIn ? '/dashboard' : '/login'))
const homeLabel = computed(() => (authStore.isLoggedIn ? '回到工作台' : '去登录'))

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push(homePath.value)
  }
}
</script>
