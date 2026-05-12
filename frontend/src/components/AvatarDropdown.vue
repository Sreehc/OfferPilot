<template>
  <div class="relative" ref="containerRef">
    <!-- Avatar trigger -->
    <button
      type="button"
      class="shell-avatar-trigger text-sm font-semibold"
      @click="open = !open"
    >
      <img v-if="avatarUrl" :src="avatarUrl" alt="用户头像" class="h-full w-full rounded-full object-cover" />
      <span v-else>{{ initials }}</span>
    </button>

    <!-- Dropdown -->
    <Transition name="dropdown">
      <div
        v-if="open"
        class="shell-popover absolute right-0 top-12 z-50 w-48 overflow-hidden"
      >
        <!-- User info -->
        <div class="border-b border-[var(--bc-border-subtle)] px-3 py-2">
          <div class="text-sm font-semibold text-ink">{{ name }}</div>
          <div class="text-xs text-tertiary">{{ roleLabel }}</div>
        </div>

        <!-- Menu items -->
        <div class="py-1">
          <RouterLink
            to="/settings"
            class="flex items-center gap-3 px-3 py-2 text-sm text-primary transition hover:bg-[rgba(var(--bc-accent-rgb),0.08)]"
            @click="open = false"
          >
            <svg class="h-4 w-4 text-tertiary" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            账户设置
          </RouterLink>

          <button
            type="button"
            class="flex w-full items-center gap-3 px-3 py-2 text-sm text-primary transition hover:bg-[rgba(var(--bc-accent-rgb),0.08)]"
            @click="handleToggleTheme"
          >
            <svg v-if="theme === 'light'" class="h-4 w-4 text-tertiary" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21.752 15.002A9.718 9.718 0 0118 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 003 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 009.002-5.998z" />
            </svg>
            <svg v-else class="h-4 w-4 text-tertiary" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 3v2.25m6.364.386l-1.591 1.591M21 12h-2.25m-.386 6.364l-1.591-1.591M12 18.75V21m-4.773-4.227l-1.591 1.591M5.25 12H3m4.227-4.773L5.636 5.636M15.75 12a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0z" />
            </svg>
            {{ theme === 'light' ? '深色模式' : '浅色模式' }}
          </button>
        </div>

        <!-- Logout -->
        <div class="border-t border-[var(--bc-border-subtle)] py-1">
          <button
            type="button"
            class="flex w-full items-center gap-3 px-3 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition"
            @click="handleLogout"
          >
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9" />
            </svg>
            退出登录
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'

const emit = defineEmits<{
  logout: []
}>()

defineProps<{
  name: string
  role: string
  initials: string
}>()

const authStore = useAuthStore()
const { theme, toggleTheme } = useTheme()

const open = ref(false)
const containerRef = ref<HTMLElement | null>(null)

const avatarUrl = computed(() => authStore.user?.avatar || null)
const roleLabel = computed(() => authStore.user?.role === 'ADMIN' ? '管理员' : '用户')

const handleToggleTheme = () => {
  toggleTheme()
}

const handleLogout = () => {
  open.value = false
  emit('logout')
}

const handleClickOutside = (e: MouseEvent) => {
  if (containerRef.value && !containerRef.value.contains(e.target as Node)) {
    open.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', handleClickOutside))
</script>

<style scoped>
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
