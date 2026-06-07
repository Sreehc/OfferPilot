<template>
  <section class="ui-form-section">
    <div
      v-if="title || description || $slots.head"
      class="ui-form-section__head"
    >
      <slot name="head">
        <h3 class="ui-form-section__title">{{ title }}</h3>
        <p
          v-if="description"
          class="ui-form-section__description"
        >
          {{ description }}
        </p>
      </slot>
    </div>
    <div class="ui-form-section__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title?: string
  description?: string
}>(), {
  title: '',
  description: ''
})
</script>
