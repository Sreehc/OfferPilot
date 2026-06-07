<template>
  <section class="ui-workspace-page">
    <PageStateHeader
      :title="title"
      :description="description"
      :chips="chips"
      :compact="compact"
    >
      <template
        v-if="$slots.meta"
        #meta
      >
        <slot name="meta" />
      </template>
      <template
        v-if="$slots.actions"
        #actions
      >
        <slot name="actions" />
      </template>
      <template
        v-if="$slots.below"
        #below
      >
        <slot name="below" />
      </template>
    </PageStateHeader>

    <MetricStrip
      v-if="metrics.length"
      :items="metrics"
    />

    <div
      class="ui-workspace-page__body"
      :class="{ 'ui-workspace-page__body--split': Boolean($slots.side) }"
    >
      <main class="ui-workspace-page__main">
        <slot />
      </main>
      <aside
        v-if="$slots.side"
        class="ui-workspace-page__side"
      >
        <slot name="side" />
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import MetricStrip from './MetricStrip.vue'
import PageStateHeader from './PageStateHeader.vue'

type MetricStripItem = {
  label: string
  value: string | number
  hint?: string
  tone?: 'accent' | 'success' | 'warning' | 'danger'
}

type PageStateChip = {
  label: string
  tone?: 'neutral' | 'accent' | 'success' | 'warning' | 'danger' | 'muted'
}

withDefaults(defineProps<{
  title: string
  description?: string
  chips?: PageStateChip[]
  metrics?: MetricStripItem[]
  compact?: boolean
}>(), {
  description: '',
  chips: () => [],
  metrics: () => [],
  compact: false
})
</script>
