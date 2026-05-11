<template>
  <aside class="relative z-[1] h-full min-h-0 overflow-y-auto bg-white/30 p-6 dark:bg-transparent">
    <div class="space-y-8 pb-6">
      <section v-for="group in groups" :key="group.label" class="space-y-2">
        <p class="px-4 text-[10px] font-semibold uppercase tracking-[0.28em] text-slate-400 dark:text-slate-500">
          {{ group.label }}
        </p>
        <nav class="space-y-1">
          <RouterLink
            v-for="item in group.items"
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
      </section>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

type NavItem = {
  path: string
  label: string
  hint: string
  index: string
  adminOnly?: boolean
  group: '主任务' | '辅助' | '外部' | '管理'
}

const allItems: NavItem[] = [
  { path: '/dashboard', label: '首页', hint: '今日任务与进展', index: '01', group: '主任务' },
  { path: '/cards', label: '今日卡片', hint: '今天先完成这里', index: '02', group: '主任务' },
  { path: '/review', label: '复习', hint: '处理到期和积压项', index: '03', group: '主任务' },
  { path: '/knowledge', label: '知识库', hint: '上传、筛选和管理资料', index: '04', group: '主任务' },
  { path: '/chat', label: '问答', hint: '提问并查看资料引用', index: '05', group: '辅助' },
  { path: '/interview', label: '面试诊断', hint: '做一次进阶练习', index: '06', group: '辅助' },
  { path: '/analytics', label: '数据分析', hint: '查看节奏、掌握度和趋势', index: '07', group: '辅助' },
  { path: '/community', label: '社区', hint: '浏览问题和发布回答', index: '08', group: '外部' },
  { path: '/admin', label: '管理后台', hint: '内容与数据管理', index: '09', adminOnly: true, group: '管理' }
]

const items = computed(() => allItems.filter((item) => !item.adminOnly || authStore.user?.role === 'ADMIN'))
const groups = computed(() =>
  ['主任务', '辅助', '外部', '管理']
    .map((label) => ({ label, items: items.value.filter((item) => item.group === label) }))
    .filter((group) => group.items.length > 0)
)

const isActive = (path: string) => route.path === path || route.path.startsWith(path + '/')
</script>
