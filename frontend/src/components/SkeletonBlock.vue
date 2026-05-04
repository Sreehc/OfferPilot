<template>
  <div class="animate-pulse space-y-3">
    <div v-if="avatar" class="flex items-center gap-3">
      <div class="h-10 w-10 rounded-full bg-slate-200 dark:bg-slate-700"></div>
      <div class="flex-1 space-y-2">
        <div class="h-4 w-1/3 rounded bg-slate-200 dark:bg-slate-700"></div>
        <div class="h-3 w-1/2 rounded bg-slate-100 dark:bg-slate-800"></div>
      </div>
    </div>
    <template v-for="i in rows" :key="i">
      <div
        class="h-4 rounded bg-slate-200 dark:bg-slate-700"
        :style="{ width: i === rows ? '60%' : '100%' }"
      ></div>
    </template>
    <div v-if="actions" class="flex gap-3 pt-2">
      <div class="h-9 w-24 rounded bg-slate-200 dark:bg-slate-700"></div>
      <div class="h-9 w-20 rounded bg-slate-100 dark:bg-slate-800"></div>
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
