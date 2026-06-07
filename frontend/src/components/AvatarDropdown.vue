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
            <UiIcon name="settings" class="h-4 w-4 text-tertiary" />
            账户设置
          </RouterLink>

          <button
            type="button"
            class="flex w-full items-center gap-3 px-3 py-2 text-sm text-primary transition hover:bg-[rgba(var(--bc-accent-rgb),0.08)]"
            @click="handleToggleTheme"
          >
            <UiIcon :name="theme === 'light' ? 'moon' : 'sunny'" class="h-4 w-4 text-tertiary" />
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
            <UiIcon name="switch" class="h-4 w-4" />
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
import { UiIcon } from '@/components/ui'

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
