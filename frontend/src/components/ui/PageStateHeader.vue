<template>
  <header
    class="ui-page-state"
    :class="{ 'ui-page-state--compact': compact }"
  >
    <div class="ui-page-state__main">
      <div
        v-if="$slots.meta || chips.length"
        class="ui-page-state__meta"
      >
        <slot name="meta">
          <StatusBadge
            v-for="chip in chips.slice(0, 4)"
            :key="chip.label"
            :tone="chip.tone || 'neutral'"
            :label="chip.label"
          />
        </slot>
      </div>

      <h1 class="ui-page-state__title">{{ title }}</h1>
      <p
        v-if="description"
        class="ui-page-state__description"
      >
        {{ description }}
      </p>

      <slot name="below" />
    </div>

    <div
      v-if="$slots.actions"
      class="ui-page-state__actions"
    >
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'

type PageStateChip = {
  label: string
  tone?: 'neutral' | 'accent' | 'success' | 'warning' | 'danger' | 'muted'
}

withDefaults(defineProps<{
  title: string
  description?: string
  chips?: PageStateChip[]
  compact?: boolean
}>(), {
  description: '',
  chips: () => [],
  compact: false
})
</script>
