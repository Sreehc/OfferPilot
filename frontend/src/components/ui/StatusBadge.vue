<template>
  <span
    class="ui-status-badge"
    :class="`ui-status-badge--${tone}`"
  >
    <span
      v-if="dot"
      class="ui-status-badge__dot"
      aria-hidden="true"
    />
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  label?: string
  tone?: 'neutral' | 'accent' | 'success' | 'warning' | 'danger' | 'muted'
  dot?: boolean
}>(), {
  label: '',
  tone: 'neutral',
  dot: false
})
</script>
