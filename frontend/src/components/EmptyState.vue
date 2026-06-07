<template>
  <StateView
    :icon="icon"
    :title="title"
    :description="description"
    :compact="compact"
  >
    <template
      v-if="$slots.action"
      #action
    >
      <slot name="action" />
    </template>
  </StateView>
</template>

<script setup lang="ts">
import StateView from '@/components/ui/StateView.vue'

withDefaults(defineProps<{
  icon?: string
  title: string
  description: string
  compact?: boolean
}>(), {
  icon: 'inbox',
  compact: false
})
</script>
