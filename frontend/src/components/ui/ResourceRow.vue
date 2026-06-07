<template>
  <article
    class="ui-resource-row"
    :class="{ 'ui-resource-row--clickable': clickable }"
  >
    <div
      v-if="icon"
      class="ui-resource-row__icon"
    >
      <UiIcon :name="icon" />
    </div>
    <div class="ui-resource-row__content">
      <div
        v-if="$slots.meta"
        class="ui-resource-row__meta"
      >
        <slot name="meta" />
      </div>
      <h3 class="ui-resource-row__title">
        <slot name="title">{{ title }}</slot>
      </h3>
      <p
        v-if="$slots.summary || summary"
        class="ui-resource-row__summary"
      >
        <slot name="summary">{{ summary }}</slot>
      </p>
      <div
        v-if="$slots.tags"
        class="ui-resource-row__tags"
      >
        <slot name="tags" />
      </div>
    </div>
    <div
      v-if="$slots.actions"
      class="ui-resource-row__actions"
    >
      <slot name="actions" />
    </div>
  </article>
</template>

<script setup lang="ts">
import UiIcon from './UiIcon.vue'

withDefaults(defineProps<{
  title?: string
  summary?: string
  icon?: string
  clickable?: boolean
}>(), {
  title: '',
  summary: '',
  icon: '',
  clickable: false
})
</script>
