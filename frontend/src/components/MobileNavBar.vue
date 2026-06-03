<template>
  <nav
    class="mobile-nav-shell safe-area-bottom fixed bottom-0 left-0 right-0 z-50 border-t border-[var(--bc-border-subtle)] backdrop-blur-xl md:hidden"
  >
    <div class="mobile-nav-shell__grid">
      <component
        :is="item.path ? 'RouterLink' : 'button'"
        v-for="item in items"
        :key="item.path"
        v-bind="item.path ? { to: item.path } : { type: 'button' }"
        class="mobile-nav-shell__item"
        :class="
          isActive(item.path) ? 'mobile-nav-shell__item-active' : 'text-secondary'
        "
        @click="item.action?.()"
      >
        <component
          :is="item.icon"
          class="mobile-nav-shell__icon"
        />
        <span class="mobile-nav-shell__label">{{ item.label }}</span>
      </component>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { h } from 'vue'
import { useRoute } from 'vue-router'
import { PRODUCT_PAGE_NAMES } from '@/constants/productCopy'

const route = useRoute()

// Simple SVG icon components
const IconHome = () =>
  h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      d: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6'
    })
  ])

const IconQuestion = () =>
  h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      d: 'M8.25 8.25a3.75 3.75 0 117.5 0c0 1.345-.58 2.195-1.56 2.992-.74.602-1.44 1.25-1.44 2.258v.25'
    }),
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      d: 'M12 18h.01'
    })
  ])

const IconChat = () =>
  h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      d: 'M8 10h8M8 14h4m-7 5l1.76-3.52A8 8 0 114 15.5V19z'
    })
  ])

const IconInterview = () =>
  h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      d: 'M7 8h10M7 12h6m-6 4h10M5 5h14a2 2 0 012 2v10a2 2 0 01-2 2H9l-4 3v-3H5a2 2 0 01-2-2V7a2 2 0 012-2z'
    })
  ])

const IconMore = () =>
  h('svg', { class: 'h-5 w-5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': '2' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M12 6h.01M12 12h.01M12 18h.01' })
  ])

const openSidebar = () => {
  window.dispatchEvent(new CustomEvent('offerpilot:open-sidebar'))
}

const primaryPaths = ['/dashboard', '/question', '/chat', '/interview']

const items = [
  { path: '/dashboard', label: '首页', icon: IconHome },
  { path: '/question', label: PRODUCT_PAGE_NAMES.question, icon: IconQuestion },
  { path: '/chat', label: PRODUCT_PAGE_NAMES.chat, icon: IconChat },
  { path: '/interview', label: PRODUCT_PAGE_NAMES.interview, icon: IconInterview },
  { path: '', label: '更多', icon: IconMore, action: openSidebar }
]

const isRouteMatch = (path?: string) => Boolean(path) && (route.path === path || route.path.startsWith(`${path}/`))

const isActive = (path?: string) => {
  if (!path) {
    return !primaryPaths.some((itemPath) => isRouteMatch(itemPath))
  }

  if (!isRouteMatch(path)) return false

  const matchedItems = items.filter((item) => isRouteMatch(item.path))
  if (!matchedItems.length) return false

  const activeItem = matchedItems.reduce((current, item) =>
    (item.path?.length || 0) > (current.path?.length || 0) ? item : current
  )

  return activeItem.path === path
}
</script>

<style scoped>
.safe-area-bottom {
  padding-bottom: env(safe-area-inset-bottom, 0px);
}

.mobile-nav-shell {
  background: var(--bc-surface-card);
}

.mobile-nav-shell__grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  align-items: stretch;
  gap: 0.35rem;
  padding: 0.45rem 0.55rem 0.55rem;
}

.mobile-nav-shell__item {
  display: flex;
  min-width: 0;
  min-height: 3.65rem;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.22rem;
  border-radius: 1rem;
  border: 1px solid transparent;
  padding: 0.38rem 0.18rem;
  transition:
    color var(--motion-fast) var(--ease-hard),
    background-color var(--motion-fast) var(--ease-hard),
    border-color var(--motion-fast) var(--ease-hard),
    box-shadow var(--motion-fast) var(--ease-hard),
    transform var(--motion-fast) var(--ease-hard);
}

.mobile-nav-shell__item-active {
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.16), rgba(var(--bc-accent-rgb), 0.08));
  border-color: rgba(var(--bc-accent-rgb), 0.16);
  color: var(--bc-ink);
  box-shadow: 0 10px 22px rgba(var(--bc-accent-rgb), 0.12);
}

.mobile-nav-shell__item-active .mobile-nav-shell__icon {
  transform: translateY(-1px);
}

.mobile-nav-shell__icon {
  height: 1.2rem;
  width: 1.2rem;
  flex-shrink: 0;
}

.mobile-nav-shell__label {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.68rem;
  font-weight: 600;
  line-height: 1.2;
}
</style>
