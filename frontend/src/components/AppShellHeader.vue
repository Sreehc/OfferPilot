<template>
  <header class="app-shell-header shell-section-card p-5 sm:p-6" :class="compact ? 'app-shell-header-compact' : ''">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
      <div class="min-w-0">
        <p v-if="resolvedKicker" class="section-kicker">{{ resolvedKicker }}</p>
        <h1 :class="compact ? 'text-xl sm:text-2xl' : 'text-3xl sm:text-4xl'" class="page-title text-ink">
          {{ resolvedTitle }}
        </h1>
        <p v-if="showResolvedSubtitle" class="page-subtitle mt-2 max-w-3xl text-sm">
          {{ resolvedSubtitle }}
        </p>
      </div>

      <div v-if="$slots.actions" class="flex flex-wrap gap-3 lg:justify-end">
        <slot name="actions" />
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const props = withDefaults(defineProps<{
  title?: string
  subtitle?: string
  kicker?: string
  compact?: boolean
  showSubtitle?: boolean
}>(), {
  title: '',
  subtitle: '',
  kicker: '',
  compact: false,
  showSubtitle: true
})

const route = useRoute()

const routeMeta = computed(() => route.meta as { title?: string; subtitle?: string; kicker?: string })
const resolvedTitle = computed(() => props.title || routeMeta.value.title || '')
const resolvedSubtitle = computed(() => props.subtitle || routeMeta.value.subtitle || '')
const resolvedKicker = computed(() => props.kicker || routeMeta.value.kicker || '')
const showResolvedSubtitle = computed(() => props.showSubtitle && Boolean(resolvedSubtitle.value))
</script>

<style scoped>
.app-shell-header {
  overflow: visible;
  background:
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.05), transparent 26%),
    var(--bc-surface-card);
  box-shadow: var(--bc-shadow-soft);
}

.page-title {
  margin-top: 0;
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.05;
}

.app-shell-header-compact .page-title {
  letter-spacing: -0.03em;
}
</style>
