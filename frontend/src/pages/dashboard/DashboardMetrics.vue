<template>
  <section class="grid gap-3 xl:grid-cols-4 md:grid-cols-2">
    <article v-for="metric in metrics" :key="metric.label" class="data-slab p-4">
      <p class="metric-label">{{ metric.label }}</p>
      <p class="metric-value !mt-2 !text-3xl">{{ metric.value }}</p>
      <p v-if="metric.desc" class="mt-2 text-xs leading-5 text-slate-400">{{ metric.desc }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
interface MetricItem {
  label: string
  value: string
  desc?: string
}

defineProps<{
  metrics: MetricItem[]
}>()
</script>
