<template>
  <nav class="fixed bottom-0 left-0 right-0 z-50 border-t border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 safe-area-bottom md:hidden">
    <div class="flex items-center justify-around px-1 py-1">
      <RouterLink
        v-for="item in items"
        :key="item.path"
        :to="item.path"
        class="flex flex-col items-center gap-0.5 px-2 py-2 min-w-0 rounded-lg transition-colors"
        :class="isActive(item.path) ? 'text-accent' : 'text-slate-400 dark:text-slate-500'"
      >
        <component :is="item.icon" class="h-5 w-5 shrink-0" />
        <span class="text-[10px] font-medium truncate">{{ item.label }}</span>
      </RouterLink>
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

const IconChat = () => h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z' })
])

const IconInterview = () => h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z' })
])

const IconReview = () => h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253' })
])

const IconCommunity = () => h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z' })
])

const items = [
  { path: '/dashboard', label: '首页', icon: IconHome },
  { path: '/interview', label: '面试', icon: IconInterview },
  { path: '/review', label: '复习', icon: IconReview },
  { path: '/community', label: '社区', icon: IconCommunity },
  { path: '/chat', label: '问答', icon: IconChat },
]

const isActive = (path: string) => route.path === path || route.path.startsWith(path + '/')
</script>

<style scoped>
.safe-area-bottom {
  padding-bottom: env(safe-area-inset-bottom, 0px);
}
</style>
