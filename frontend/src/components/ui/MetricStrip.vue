<template>
  <div class="ui-metric-strip">
    <article
      v-for="item in items"
      :key="item.label"
      class="ui-metric-tile"
      :class="item.tone ? `ui-metric-tile--${item.tone}` : ''"
    >
      <span class="ui-metric-tile__label">{{ item.label }}</span>
      <strong class="ui-metric-tile__value">{{ item.value }}</strong>
      <span
        v-if="item.hint"
        class="ui-metric-tile__hint"
      >
        {{ item.hint }}
      </span>
    </article>
  </div>
</template>

<script setup lang="ts">
type MetricStripItem = {
  label: string
  value: string | number
  hint?: string
  tone?: 'accent' | 'success' | 'warning' | 'danger'
}

defineProps<{
  items: MetricStripItem[]
}>()
</script>
