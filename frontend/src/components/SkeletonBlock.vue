<template>
  <div class="skeleton-block" :class="`skeleton-block--${variant}`">
    <template v-if="variant === 'page-header'">
      <div class="flex items-start gap-3">
        <div v-if="avatar" class="skeleton-line h-12 w-12 shrink-0 rounded-2xl" />
        <div class="min-w-0 flex-1 space-y-3">
          <div class="skeleton-line h-3 w-28 rounded-full" />
          <div class="skeleton-line h-8 w-3/5 rounded-full" />
          <div class="skeleton-line h-4 w-4/5 rounded-full" />
        </div>
      </div>
      <div v-if="actions" class="flex flex-wrap gap-3 pt-2">
        <div class="skeleton-line h-10 w-28 rounded-xl" />
        <div class="skeleton-line h-10 w-24 rounded-xl" />
      </div>
    </template>

    <template v-else-if="variant === 'list'">
      <div v-for="i in rows" :key="i" class="skeleton-block__row">
        <div v-if="avatar" class="skeleton-line h-10 w-10 shrink-0 rounded-xl" />
        <div class="min-w-0 flex-1 space-y-2">
          <div class="skeleton-line h-4 w-3/5 rounded-full" />
          <div class="skeleton-line h-3 w-full rounded-full" />
          <div class="skeleton-line h-3 w-2/5 rounded-full" />
        </div>
      </div>
    </template>

    <template v-else-if="variant === 'detail'">
      <div class="skeleton-line h-7 w-2/5 rounded-full" />
      <div class="grid gap-3 sm:grid-cols-3">
        <div v-for="i in 3" :key="i" class="skeleton-line h-20 rounded-2xl" />
      </div>
      <div class="skeleton-line h-40 rounded-2xl" />
      <div v-for="i in Math.max(rows - 2, 1)" :key="i" class="skeleton-line h-4 rounded-full" />
    </template>

    <template v-else-if="variant === 'metrics'">
      <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div v-for="i in Math.max(rows, 4)" :key="i" class="skeleton-block__metric">
          <div class="skeleton-line h-3 w-16 rounded-full" />
          <div class="skeleton-line h-7 w-20 rounded-full" />
        </div>
      </div>
    </template>

    <template v-else>
      <div v-if="avatar" class="flex items-center gap-3">
        <div class="skeleton-line h-10 w-10 rounded-full" />
        <div class="flex-1 space-y-2">
          <div class="skeleton-line h-4 w-1/3 rounded-full" />
          <div class="skeleton-line h-3 w-1/2 rounded-full" />
        </div>
      </div>
      <template v-for="i in rows" :key="i">
        <div class="skeleton-line h-4 rounded-full" :style="{ width: i === rows ? '60%' : '100%' }" />
      </template>
      <div v-if="actions" class="flex gap-3 pt-2">
        <div class="skeleton-line h-9 w-24 rounded-xl" />
        <div class="skeleton-line h-9 w-20 rounded-xl" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    rows?: number
    avatar?: boolean
    actions?: boolean
    variant?: 'text' | 'page-header' | 'list' | 'detail' | 'metrics'
  }>(),
  {
    rows: 3,
    avatar: false,
    actions: false,
    variant: 'text'
  }
)
</script>

<style scoped>
.skeleton-block {
  display: grid;
  gap: 0.75rem;
}

.skeleton-block__row {
  display: flex;
  gap: 0.85rem;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 0.95rem;
}

.skeleton-block__metric {
  display: grid;
  gap: 0.75rem;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 1rem;
}
</style>
