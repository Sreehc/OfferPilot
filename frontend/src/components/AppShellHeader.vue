<template>
  <header class="cockpit-panel flex flex-col gap-4 p-4 sm:p-5 lg:flex-row lg:items-center lg:justify-between">
    <div class="min-w-0">
      <div class="flex items-center gap-3">
        <span class="state-pulse" aria-hidden="true"></span>
        <p class="section-kicker">{{ kicker }}</p>
      </div>
      <h2 class="page-title mt-3 text-2xl sm:text-3xl lg:text-4xl">{{ title }}</h2>
      <p class="page-subtitle mt-3 hidden sm:block">{{ subtitle }}</p>
    </div>

    <div class="flex flex-col gap-3 self-start sm:flex-row sm:items-center">
      <slot name="actions" />

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
