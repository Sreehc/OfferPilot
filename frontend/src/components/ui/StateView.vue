<template>
  <div
    class="ui-state-view"
    :class="[`ui-state-view--${variant}`, compact ? 'ui-state-view--compact' : '']"
  >
    <div
      v-if="variant === 'loading'"
      class="ui-state-view__skeleton"
    >
      <SkeletonBlock
        :rows="rows"
        :avatar="avatar"
        :actions="actions"
      />
    </div>
    <template v-else>
      <div class="ui-state-view__icon">
        <UiIcon :name="iconName" />
      </div>
      <h3 class="ui-state-view__title">{{ title }}</h3>
      <p
        v-if="description"
        class="ui-state-view__description"
      >
        {{ description }}
      </p>
      <div
        v-if="$slots.action"
        class="ui-state-view__action"
      >
        <slot name="action" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SkeletonBlock from '@/components/SkeletonBlock.vue'
import UiIcon from './UiIcon.vue'

const props = withDefaults(defineProps<{
  variant?: 'loading' | 'empty' | 'error' | 'permission'
  title?: string
  description?: string
  icon?: string
  compact?: boolean
  rows?: number
  avatar?: boolean
  actions?: boolean
}>(), {
  variant: 'empty',
  title: '',
  description: '',
  icon: '',
  compact: false,
  rows: 4,
  avatar: false,
  actions: true
})

const iconName = computed(() => {
  if (props.icon) return props.icon
  if (props.variant === 'error') return 'refresh'
  if (props.variant === 'permission') return 'lock'
  return 'operation'
})
</script>
