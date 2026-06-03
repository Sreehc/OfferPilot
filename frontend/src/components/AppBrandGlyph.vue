<template>
  <span
    class="app-brand-glyph"
    :style="glyphStyle"
    aria-hidden="true"
  >
    <span class="app-brand-glyph__core" />
    <span class="app-brand-glyph__dot" />
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  size?: number
}>()

const glyphStyle = computed(() => ({
  width: `${props.size ?? 34}px`,
  height: `${props.size ?? 34}px`
}))
</script>

<style scoped>
.app-brand-glyph {
  position: relative;
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.14);
  border-radius: 999px;
  background:
    radial-gradient(circle at 32% 32%, rgba(var(--bc-accent-rgb), 0.22), transparent 48%),
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.14), rgba(var(--bc-accent-rgb), 0.06));
  box-shadow:
    inset 0 1px 0 rgba(var(--bc-ink-rgb), 0.05),
    0 8px 16px rgba(var(--bc-accent-rgb), 0.12);
}

.app-brand-glyph__core {
  position: relative;
  width: 12px;
  height: 12px;
  border: 2px solid var(--bc-ink);
  border-radius: 4px;
  transform: rotate(45deg);
}

.app-brand-glyph__dot {
  position: absolute;
  right: 7px;
  top: 7px;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--bc-accent);
}
</style>
