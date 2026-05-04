<template>
  <header class="paper-panel flex flex-col gap-4 p-5 lg:flex-row lg:items-center lg:justify-between">
    <div class="min-w-0">
      <p class="section-kicker">{{ kicker }}</p>
      <h2 class="page-title mt-3 text-xl sm:text-2xl lg:text-3xl">{{ title }}</h2>
      <p class="page-subtitle mt-3 hidden sm:block">{{ subtitle }}</p>
    </div>

    <div class="flex flex-col gap-3 self-start sm:flex-row sm:items-center">
      <slot name="actions" />

      <RouterLink
        to="/settings"
        class="hidden sm:flex h-10 w-10 items-center justify-center rounded-full transition hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300"
        title="账户设置"
      >
        <img v-if="avatarUrl" :src="avatarUrl" alt="avatar" class="h-6 w-6 rounded-full object-cover" />
        <svg v-else class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M17.982 18.725A7.488 7.488 0 0012 15.75a7.488 7.488 0 00-5.982 2.975m11.963 0a9 9 0 10-11.963 0m11.963 0A8.966 8.966 0 0112 21a8.966 8.966 0 01-5.982-2.275M15 9.75a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      </RouterLink>

      <!-- Mobile: icon-only logout button -->
      <button
        type="button"
        class="flex sm:hidden items-center justify-center h-10 w-10 rounded-full text-slate-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition"
        title="退出登录"
        @click="$emit('logout')"
      >
        <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9" />
        </svg>
      </button>
      <!-- Desktop: icon logout button -->
      <button
        type="button"
        class="hidden sm:flex h-10 w-10 items-center justify-center rounded-full text-slate-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition"
        title="退出登录"
        @click="$emit('logout')"
      >
        <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9" />
        </svg>
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

defineEmits<{
  logout: []
}>()

defineProps<{
  kicker: string
  title: string
  subtitle: string
  name: string
  role: string
  initials: string
}>()

const avatarUrl = authStore.user?.avatar || null
</script>
