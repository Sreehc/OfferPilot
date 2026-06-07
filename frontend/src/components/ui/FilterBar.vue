<template>
  <section class="ui-filter-bar">
    <div class="ui-filter-bar__main">
      <slot />
    </div>
    <div
      v-if="$slots.actions"
      class="ui-filter-bar__actions"
    >
      <slot name="actions" />
    </div>
    <div
      v-if="$slots.advanced"
      class="ui-filter-bar__advanced"
    >
      <slot name="advanced" />
    </div>
  </section>
</template>
