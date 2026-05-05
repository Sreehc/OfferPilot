<template>
  <aside class="relative z-[1] flex h-full flex-col justify-between bg-white/30 p-6 dark:bg-transparent">
    <div class="space-y-8">
      <div>
        <div class="flex items-center gap-3">
          <span class="state-pulse" aria-hidden="true"></span>
          <div class="section-kicker">ByteCoach</div>
        </div>
        <h1 class="mt-4 font-display text-4xl font-semibold leading-[0.9] tracking-[-0.03em] text-ink">Java 面试训练舱</h1>
        <p class="mt-3 text-sm leading-6 text-slate-500 dark:text-slate-400">
          聚焦 Java 后端面试准备，串联问答、面试、错题、复习与计划。
        </p>
      </div>

      <nav class="space-y-1 pt-2">
        <RouterLink
          v-for="item in items"
          :key="item.path"
          :to="item.path"
          class="group flex cursor-pointer items-center justify-between px-4 py-3 text-sm transition-all duration-150"
          style="border-radius: var(--radius-md);"
          :class="isActive(item.path) ? 'border border-[var(--bc-line-hot)] bg-accent/10 text-ink shadow-[0_0_28px_rgba(var(--bc-accent-rgb),0.12)]' : 'border border-transparent text-slate-600 hover:border-[var(--bc-line)] hover:bg-white/50 hover:text-ink active:translate-y-px dark:text-slate-300 dark:hover:bg-white/5'"
        >
          <div class="flex items-center gap-3">
            <span
              class="h-2.5 w-2.5 rounded-full transition-colors duration-150"
              :class="isActive(item.path) ? 'bg-accent shadow-[0_0_16px_rgba(var(--bc-accent-rgb),0.58)]' : 'bg-slate-300 group-hover:bg-accent/60 dark:bg-slate-600'"
            ></span>
            <span class="font-semibold">{{ item.label }}</span>
          </div>
          <span class="font-mono text-[11px] font-semibold tracking-[0.24em] opacity-55">{{ item.index }}</span>
        </RouterLink>
      </nav>
    </div>

    <div class="cockpit-panel px-4 py-5">
      <p class="text-xs font-semibold uppercase tracking-[0.28em] text-accent">ByteCoach</p>
      <p class="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300">
        当前任务优先于功能浏览。先完成今日训练，再查看趋势。
      </p>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const allItems = [
  { path: '/dashboard', label: '首页', index: '01' },
  { path: '/chat', label: '问答', index: '02' },
  { path: '/knowledge', label: '知识库', index: '03' },
  { path: '/interview', label: '面试', index: '04' },
  { path: '/wrong', label: '错题本', index: '05' },
  { path: '/review', label: '复习', index: '06' },
  { path: '/community', label: '社区', index: '07' },
  { path: '/plan', label: '学习计划', index: '08' },
  { path: '/admin', label: '管理后台', index: '09', adminOnly: true }
]

const items = computed(() =>
  allItems.filter((item) => !item.adminOnly || authStore.user?.role === 'ADMIN')
)

const isActive = (path: string) => route.path === path
</script>
