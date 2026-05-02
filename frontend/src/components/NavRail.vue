<template>
  <aside class="paper-panel flex h-full flex-col justify-between p-6">
    <div class="space-y-8">
      <div>
        <div class="section-kicker">ByteCoach</div>
        <h1 class="mt-3 text-3xl font-semibold leading-none tracking-[-0.03em] text-ink">Java 面试训练台</h1>
        <p class="mt-3 text-sm leading-6 text-slate-500 dark:text-slate-400">
          聚焦 Java 后端面试准备，问答、面试、错题、计划一站完成。
        </p>
      </div>

      <nav class="space-y-1 pt-2">
        <RouterLink
          v-for="item in items"
          :key="item.path"
          :to="item.path"
          class="group flex cursor-pointer items-center justify-between px-4 py-3 text-sm transition-all duration-150"
          style="border-radius: var(--radius-md);"
          :class="isActive(item.path) ? 'bg-accent/10 text-ink shadow-[inset_0_0_0_1px_rgba(47,79,157,0.08)]' : 'text-slate-600 dark:text-slate-300 hover:bg-white/70 dark:hover:bg-slate-800/70 hover:text-ink active:translate-y-px'"
        >
          <div class="flex items-center gap-3">
            <span
              class="h-2.5 w-2.5 rounded-full transition-colors duration-150"
              :class="isActive(item.path) ? 'bg-accent' : 'bg-slate-300 dark:bg-slate-600 group-hover:bg-slate-400 dark:group-hover:bg-slate-500'"
            ></span>
            <span class="font-semibold">{{ item.label }}</span>
          </div>
          <span class="text-[11px] font-semibold tracking-[0.24em] opacity-55">{{ item.index }}</span>
        </RouterLink>
      </nav>
    </div>

    <div class="surface-muted px-4 py-5">
      <p class="text-xs font-semibold uppercase tracking-[0.28em] text-accent">ByteCoach</p>
      <p class="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300">
        智能问答、模拟面试、错题复习、学习计划，四段闭环助你备战 Java 面试。
      </p>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const items = [
  { path: '/dashboard', label: '首页', index: '01' },
  { path: '/chat', label: '问答', index: '02' },
  { path: '/knowledge', label: '知识库', index: '03' },
  { path: '/interview', label: '面试', index: '04' },
  { path: '/wrong', label: '错题本', index: '05' },
  { path: '/plan', label: '学习计划', index: '06' },
  { path: '/admin', label: '管理后台', index: '07' }
]

const isActive = (path: string) => route.path === path
</script>
