<template>
  <section
    class="ui-capability-gate"
    :class="`ui-capability-gate--${tone}`"
  >
    <div class="ui-capability-gate__content">
      <StatusBadge
        :tone="badgeTone"
        :label="label"
      />
      <h3 class="ui-capability-gate__title">{{ title }}</h3>
      <p
        v-if="description"
        class="ui-capability-gate__description"
      >
        {{ description }}
      </p>
      <div
        v-if="$slots.default"
        class="ui-capability-gate__body"
      >
        <slot />
      </div>
    </div>
    <div
      v-if="$slots.action"
      class="ui-capability-gate__action"
    >
      <slot name="action" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StatusBadge from './StatusBadge.vue'

const props = withDefaults(defineProps<{
  tone?: 'warning' | 'danger' | 'accent' | 'success'
  label?: string
  title: string
  description?: string
}>(), {
  tone: 'warning',
  label: '需要处理',
  description: ''
})

const badgeTone = computed(() => props.tone === 'accent' ? 'accent' : props.tone)
</script>
