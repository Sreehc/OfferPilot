<template>
  <aside
    class="relative z-[1] flex h-full min-h-0 flex-col justify-between overflow-y-auto bg-white/30 p-6 dark:bg-transparent"
  >
    <div class="space-y-8 pb-6">
      <nav class="space-y-1">
        <RouterLink
          v-for="item in items"
          :key="item.path"
          :to="item.path"
          class="group flex cursor-pointer items-center justify-between gap-3 px-4 py-3 text-sm transition-all duration-150"
          style="border-radius: var(--radius-md)"
          :class="
            isActive(item.path)
              ? 'border border-[var(--bc-line-hot)] bg-accent/10 text-ink shadow-[0_0_28px_rgba(var(--bc-accent-rgb),0.12)]'
              : 'border border-transparent text-slate-600 hover:border-[var(--bc-line)] hover:bg-white/50 hover:text-ink active:translate-y-px dark:text-slate-300 dark:hover:bg-white/5'
          "
        >
          <div class="flex min-w-0 items-center gap-3">
            <span
              class="h-2.5 w-2.5 rounded-full transition-colors duration-150"
              :class="
                isActive(item.path)
                  ? 'bg-accent shadow-[0_0_16px_rgba(var(--bc-accent-rgb),0.58)]'
                  : 'bg-slate-300 group-hover:bg-accent/60 dark:bg-slate-600'
              "
            ></span>
            <div class="min-w-0">
              <div class="font-semibold text-ink">{{ item.label }}</div>
              <div class="truncate text-xs text-slate-400 dark:text-slate-500">{{ item.hint }}</div>
            </div>
          </div>
          <span class="font-mono text-[11px] font-semibold tracking-[0.24em] opacity-55">{{ item.index }}</span>
        </RouterLink>
      </nav>
    </div>

    <div class="cockpit-panel px-4 py-5">
      <p class="text-xs font-semibold uppercase tracking-[0.28em] text-accent">快捷键</p>
      <div class="mt-3 space-y-2 text-sm text-slate-600 dark:text-slate-300">
        <div class="flex items-center justify-between gap-3">
          <span>打开搜索</span>
          <span class="font-mono text-xs text-slate-400">/ 或 ⌘K</span>
        </div>
        <div class="flex items-center justify-between gap-3">
          <span>收起侧栏</span>
          <span class="font-mono text-xs text-slate-400">⌘B</span>
        </div>
      </div>
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
  { path: '/dashboard', label: '首页', hint: '今日任务与进展', index: '01' },
  { path: '/chat', label: '问答', hint: '提问并查看资料引用', index: '02' },
  { path: '/knowledge', label: '知识库', hint: '上传、筛选和管理资料', index: '03' },
  { path: '/interview', label: '面试', hint: '开始练习或查看结果', index: '04' },
  { path: '/review', label: '错题复习', hint: '处理今日复习并查看全部错题', index: '05' },
  { path: '/community', label: '社区', hint: '浏览问题和发布回答', index: '06' },
  { path: '/admin', label: '管理后台', hint: '内容与数据管理', index: '07', adminOnly: true }
]

const items = computed(() => allItems.filter((item) => !item.adminOnly || authStore.user?.role === 'ADMIN'))

const isActive = (path: string) => route.path === path || route.path.startsWith(path + '/')
</script>
