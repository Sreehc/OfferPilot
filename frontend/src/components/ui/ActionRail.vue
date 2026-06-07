<template>
  <section class="ui-action-rail">
    <div class="ui-action-rail__head">
      <div>
        <h3 class="ui-action-rail__title">{{ title }}</h3>
        <p
          v-if="description"
          class="ui-action-rail__description"
        >
          {{ description }}
        </p>
      </div>
    </div>
    <div class="ui-action-rail__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title?: string
  description?: string
}>(), {
  title: '下一步动作',
  description: ''
})
</script>
