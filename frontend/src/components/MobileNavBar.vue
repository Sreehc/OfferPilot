<template>
  <nav
    class="mobile-nav-shell safe-area-bottom fixed bottom-0 left-0 right-0 z-50 border-t border-[var(--bc-border-subtle)] backdrop-blur-xl md:hidden"
  >
    <div class="mobile-nav-shell__grid">
      <component
        :is="item.path ? 'RouterLink' : 'button'"
        v-for="item in items"
        :key="item.path"
        v-bind="item.path ? { to: itemTarget(item.path) } : { type: 'button' }"
        class="mobile-nav-shell__item"
        :class="
          isActive(item.path) ? 'mobile-nav-shell__item-active' : 'text-secondary'
        "
        @click="item.action?.()"
      >
        <UiIcon
          :name="item.icon"
          class="mobile-nav-shell__icon"
        />
        <span class="mobile-nav-shell__label">{{ item.label }}</span>
      </component>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { PRODUCT_PAGE_NAMES } from '@/constants/productCopy'
import UiIcon from '@/components/ui/UiIcon.vue'

const route = useRoute()

const openSidebar = () => {
  window.dispatchEvent(new CustomEvent('offerpilot:open-sidebar'))
}

const readSeedQueryValue = (key: 'seedTopic' | 'seedWorkflow' | 'seedNote') => {
  const value = route.query[key]
  return typeof value === 'string' ? value.trim() : ''
}

const seededTopic = computed(() => readSeedQueryValue('seedTopic'))
const seededWorkflow = computed(() => readSeedQueryValue('seedWorkflow'))
const seededNote = computed(() => readSeedQueryValue('seedNote'))

const itemTarget = (path?: string) => {
  if (!path) return path
  if (!seededTopic.value && !seededWorkflow.value && !seededNote.value) return path
  if (!path.startsWith('/interview')) return path
  const query = new URLSearchParams()
  if (seededTopic.value) {
    query.set('seedTopic', seededTopic.value)
  }
  if (seededWorkflow.value) {
    query.set('seedWorkflow', seededWorkflow.value)
  }
  if (seededNote.value) {
    query.set('seedNote', seededNote.value)
  }
  return query.toString() ? `${path}?${query.toString()}` : path
}

const primaryPaths = ['/dashboard', '/question', '/chat', '/interview']

const items = [
  { path: '/dashboard', label: '首页', icon: 'dashboard' },
  { path: '/question', label: PRODUCT_PAGE_NAMES.question, icon: 'question' },
  { path: '/chat', label: PRODUCT_PAGE_NAMES.chat, icon: 'chat' },
  { path: '/interview', label: PRODUCT_PAGE_NAMES.interview, icon: 'interview' },
  { path: '', label: '更多', icon: 'more', action: openSidebar }
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
  background: color-mix(in srgb, var(--bc-surface-card) 94%, transparent);
  box-shadow: 0 -10px 30px rgba(31, 43, 72, 0.08);
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
  border-radius: var(--radius-sm);
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
  background: rgba(var(--bc-accent-rgb), 0.11);
  border-color: rgba(var(--bc-accent-rgb), 0.16);
  color: var(--bc-ink);
  box-shadow: 0 10px 22px rgba(var(--bc-accent-rgb), 0.12);
}

.mobile-nav-shell__item-active .mobile-nav-shell__icon {
  transform: translateY(-1px);
}

.mobile-nav-shell__icon {
  height: 1.15rem;
  width: 1.15rem;
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
