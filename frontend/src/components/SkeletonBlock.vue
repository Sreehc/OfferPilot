<template>
  <div class="space-y-3">
    <div v-if="avatar" class="flex items-center gap-3">
      <div class="skeleton-line h-10 w-10 rounded-full"></div>
      <div class="flex-1 space-y-2">
        <div class="skeleton-line h-4 w-1/3"></div>
        <div class="skeleton-line h-3 w-1/2"></div>
      </div>
    </div>
    <template v-for="i in rows" :key="i">
      <div class="skeleton-line h-4" :style="{ width: i === rows ? '60%' : '100%' }"></div>
    </template>
    <div v-if="actions" class="flex gap-3 pt-2">
      <div class="skeleton-line h-9 w-24"></div>
      <div class="skeleton-line h-9 w-20"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  rows?: number
  avatar?: boolean
  actions?: boolean
}>(), {
  rows: 3,
  avatar: false,
  actions: false,
})
</script>
