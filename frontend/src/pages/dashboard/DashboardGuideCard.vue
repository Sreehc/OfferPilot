<template>
  <section
    class="dashboard-guide-card shell-section-card relative overflow-hidden p-5 sm:p-6"
    style="border-radius: var(--radius-lg)"
  >
    <div
      class="dashboard-guide-card__glow absolute -right-8 top-0 h-28 w-40 rotate-[8deg] blur-2xl"
      style="border-radius: var(--radius-lg)"
    ></div>
    <div class="relative flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
      <div class="max-w-3xl">
        <p class="section-kicker">{{ eyebrow }}</p>
        <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink sm:text-3xl">{{ title }}</h3>
        <p class="mt-3 text-sm leading-7 text-secondary">{{ description }}</p>
        <p v-if="hint" class="mt-2 text-xs font-semibold uppercase tracking-[0.16em] text-tertiary">{{ hint }}</p>
      </div>
      <RouterLink :to="actionTo" class="hard-button-primary inline-flex min-w-[180px] justify-center">
        {{ actionLabel }}
      </RouterLink>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow?: string
  title: string
  description: string
  actionLabel: string
  actionTo: string
  hint?: string
}>()
</script>

<style scoped>
.dashboard-guide-card {
  background:
    linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.12), transparent 42%),
    linear-gradient(180deg, rgba(var(--bc-cyan-rgb), 0.08), transparent 58%), var(--bc-surface-card);
}

.dashboard-guide-card__glow {
  background: linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.12), rgba(var(--bc-cyan-rgb), 0.12));
}
</style>
