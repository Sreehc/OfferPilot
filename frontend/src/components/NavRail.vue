<template>
  <aside class="relative z-[1] h-full min-h-0 overflow-y-auto bg-transparent px-3 py-4">
    <div class="space-y-6 pb-5">
      <section
        v-for="group in groups"
        :key="group.label"
        class="space-y-2"
      >
        <p class="px-3 text-[10px] font-semibold uppercase tracking-[0.28em] text-tertiary">
          {{ group.label }}
        </p>
        <nav class="space-y-1">
          <RouterLink
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="group flex cursor-pointer items-center gap-3 px-3 py-3 text-sm transition-all duration-150"
            style="border-radius: var(--radius-md)"
            :class="
              isActive(item.path)
                ? 'bg-[rgba(var(--bc-accent-rgb),0.12)] text-ink shadow-[inset_3px_0_0_rgba(var(--bc-accent-rgb),0.9)]'
                : 'text-secondary hover:text-ink active:translate-y-px rail-link-idle'
            "
          >
            <div class="flex min-w-0 items-center gap-3">
              <span
                class="h-2 w-2 rounded-full transition-colors duration-150"
                :class="
                  isActive(item.path)
                    ? 'bg-accent'
                    : 'rail-link-dot group-hover:bg-accent/60'
                "
              />
              <div class="min-w-0">
                <div class="font-semibold text-ink">
                  {{ item.label }}
                </div>
                <div class="truncate text-[11px] text-tertiary">
                  {{ item.hint }}
                </div>
              </div>
            </div>
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
  adminOnly?: boolean
  group: '主线训练' | '辅助强化' | '外部' | '管理'
}

const allItems: NavItem[] = [
  { path: '/dashboard', label: '首页', hint: '求职训练总览与下一步动作', group: '主线训练' },
  { path: '/question', label: '题库', hint: '按分类、难度和岗位方向训练', group: '主线训练' },
  { path: '/knowledge', label: '知识库', hint: '管理资料、语料和学习来源', group: '主线训练' },
  { path: '/chat', label: '问答', hint: '基于资料做带引用的知识问答', group: '主线训练' },
  { path: '/interview', label: '模拟面试', hint: '启动一场文字或语音模拟面试', group: '主线训练' },
  { path: '/study-plan', label: '学习计划', hint: '规划 7/14/30 天训练路线', group: '主线训练' },
  { path: '/resume', label: '简历助手', hint: '承接简历解析与项目问答', group: '主线训练' },
  { path: '/applications', label: '投递管理', hint: '跟踪投递、JD 和真实面试进度', group: '主线训练' },
  { path: '/cards', label: '卡片强化', hint: '作为专项巩固的辅助入口保留', group: '辅助强化' },
  { path: '/review', label: '复习巩固', hint: '清理到期复习、错题与弱项', group: '辅助强化' },
  { path: '/analytics', label: '数据分析', hint: '观察训练结果和趋势变化', group: '辅助强化' },
  { path: '/community', label: '社区', hint: '浏览问题和发布回答', group: '外部' },
  { path: '/admin', label: '管理后台', hint: '内容与数据管理', adminOnly: true, group: '管理' }
]

const items = computed(() => allItems.filter((item) => !item.adminOnly || authStore.user?.role === 'ADMIN'))
const groups = computed(() =>
  ['主线训练', '辅助强化', '外部', '管理']
    .map((label) => ({ label, items: items.value.filter((item) => item.group === label) }))
    .filter((group) => group.items.length > 0)
)

const isActive = (path: string) => route.path === path || route.path.startsWith(path + '/')
</script>

<style scoped>
.rail-link-idle:hover {
  background: var(--interactive-hover);
}

.rail-link-dot {
  background: color-mix(in srgb, var(--text-tertiary) 72%, transparent);
}
</style>
