<template>
  <header class="cockpit-panel app-shell-header flex flex-col gap-5 p-4 sm:p-5 lg:flex-row lg:items-center lg:justify-between">
    <div class="min-w-0">
      <div class="flex items-center gap-3">
        <span class="state-pulse" aria-hidden="true"></span>
        <p class="section-kicker">{{ kicker }}</p>
      </div>
      <h2 class="page-title mt-2 text-2xl sm:text-3xl">{{ title }}</h2>
      <p class="page-subtitle mt-2 max-w-3xl text-sm">{{ subtitle }}</p>
    </div>

    <div class="flex flex-col gap-3 self-start sm:flex-row sm:items-center sm:self-auto">
      <slot name="actions" />

      <div class="hidden rounded-full border border-[var(--bc-line)] bg-white/45 px-3 py-2 text-xs text-slate-500 dark:bg-white/5 dark:text-slate-400 lg:inline-flex">
        <span>/ 或 ⌘K 搜索</span>
      </div>

      <!-- Notification + Avatar dropdown -->
      <div class="hidden sm:flex items-center gap-2">
        <NotificationDropdown />
        <AvatarDropdown
          :name="name"
          :role="role"
          :initials="initials"
          @logout="$emit('logout')"
        />
      </div>

      <!-- Mobile: simplified actions -->
      <div class="flex sm:hidden items-center gap-2">
        <NotificationDropdown />
        <AvatarDropdown
          :name="name"
          :role="role"
          :initials="initials"
          @logout="$emit('logout')"
        />
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import NotificationDropdown from './NotificationDropdown.vue'
import AvatarDropdown from './AvatarDropdown.vue'

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
</script>

<style scoped>
.app-shell-header {
  overflow: visible;
  z-index: 20;
}
</style>
