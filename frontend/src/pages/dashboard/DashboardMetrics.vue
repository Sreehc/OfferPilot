<template>
  <section class="grid gap-4 xl:grid-cols-4 md:grid-cols-2">
    <article v-for="metric in metrics" :key="metric.label" class="metric-card">
      <p class="metric-label">{{ metric.label }}</p>
      <p class="metric-value">{{ metric.value }}</p>
      <p class="mt-2 text-sm leading-6 text-slate-500">{{ metric.desc }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
interface MetricItem {
  label: string
  value: string
  desc: string
}

defineProps<{
  metrics: MetricItem[]
}>()
</script>
