<template>
  <section
    class="paper-panel relative overflow-hidden bg-[linear-gradient(135deg,rgba(47,79,157,0.08),rgba(255,255,255,0.94))] p-5 sm:p-6"
    style="border-radius: var(--radius-lg);"
  >
    <div
      class="absolute -right-8 top-0 h-28 w-40 rotate-[8deg] bg-[rgba(47,79,157,0.08)] blur-2xl"
      style="border-radius: var(--radius-lg);"
    ></div>
    <div class="relative flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
      <div class="max-w-3xl">
        <p class="section-kicker">首次使用</p>
        <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink sm:text-3xl">先开始今日卡片，首页才会越来越有用</h3>
        <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
          先从资料生成一轮卡片任务，再按天推进记忆。系统会随着你的复习、问答和诊断记录，逐步补齐薄弱点与建议。
        </p>
        <div class="mt-5 flex flex-wrap gap-3">
          <RouterLink
            v-for="action in actions"
            :key="action.to"
            :to="action.to"
            class="hard-button-primary"
          >
            {{ action.label }}
          </RouterLink>
        </div>
      </div>
      <button
        type="button"
        class="hard-button-secondary"
        @click="emit('dismiss')"
      >
        知道了
      </button>
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
