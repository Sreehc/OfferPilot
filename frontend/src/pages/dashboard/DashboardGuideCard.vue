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
        <p class="section-kicker">首次使用</p>
        <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink sm:text-3xl">先生成第一组卡片</h3>
        <div class="mt-5 flex flex-wrap gap-3">
          <RouterLink v-for="action in actions" :key="action.to" :to="action.to" class="hard-button-primary">
            {{ action.label }}
          </RouterLink>
        </div>
      </div>
      <button type="button" class="hard-button-secondary" @click="emit('dismiss')">知道了</button>
    </div>
  </section>
</template>

<script setup lang="ts">
interface QuickAction {
  to: string
  label: string
}

defineProps<{
  actions: QuickAction[]
}>()

const emit = defineEmits<{
  dismiss: []
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
