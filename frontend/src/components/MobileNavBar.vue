<template>
  <nav class="safe-area-bottom fixed bottom-0 left-0 right-0 z-50 border-t border-[var(--bc-line)] bg-white/88 backdrop-blur-xl dark:bg-[#07111f]/86 md:hidden">
    <div class="flex items-center justify-around px-2 py-1.5">
      <component
        v-for="item in items"
        :key="item.path"
        :is="item.path ? 'RouterLink' : 'button'"
        v-bind="item.path ? { to: item.path } : { type: 'button' }"
        class="flex min-w-0 flex-1 flex-col items-center gap-1 rounded-2xl px-2 py-2 transition-all"
        :class="isActive(item.path) ? 'bg-accent/12 text-accent shadow-[0_0_22px_rgba(var(--bc-accent-rgb),0.16)]' : 'text-slate-500 dark:text-slate-400'"
        @click="item.action?.()"
      >
        <component :is="item.icon" class="h-5 w-5 shrink-0" />
        <span class="text-[10px] font-medium truncate">{{ item.label }}</span>
      </component>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { h } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// Simple SVG icon components
const IconHome = () => h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' })
])

const IconKnowledge = () => h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253' })
])

const IconCards = () => h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M9 6.75h10.5A1.75 1.75 0 0121.25 8.5v9A1.75 1.75 0 0119.5 19.25H9A1.75 1.75 0 017.25 17.5v-9A1.75 1.75 0 019 6.75z' }),
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M7.25 8.25H5.5A1.75 1.75 0 003.75 10v7.5c0 .966.784 1.75 1.75 1.75h10A1.75 1.75 0 0017.25 17.5v-1.25' })
])

const IconReview = () => h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M9 12h6m-6 4h6M7 4h10a2 2 0 012 2v12a2 2 0 01-2 2H7a2 2 0 01-2-2V6a2 2 0 012-2z' })
])

const IconMore = () => h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M12 6h.01M12 12h.01M12 18h.01' })
])

const openSidebar = () => {
  window.dispatchEvent(new CustomEvent('bytecoach:open-sidebar'))
}

const items = [
  { path: '/dashboard', label: '首页', icon: IconHome },
  { path: '/cards', label: '今日卡片', icon: IconCards },
  { path: '/review', label: '复习', icon: IconReview },
  { path: '/knowledge', label: '知识库', icon: IconKnowledge },
  { path: '', label: '更多', icon: IconMore, action: openSidebar },
]

const isActive = (path?: string) => Boolean(path) && (route.path === path || route.path.startsWith(path + '/'))
</script>

<style scoped>
.safe-area-bottom {
  padding-bottom: env(safe-area-inset-bottom, 0px);
}
</style>
