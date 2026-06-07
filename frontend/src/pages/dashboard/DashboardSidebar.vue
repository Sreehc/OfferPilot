<template>
  <aside class="dashboard-sidebar">
    <RouterLink
      to="/dashboard"
      class="dashboard-sidebar__brand"
    >
      <AppBrandGlyph :size="40" />
      <div class="min-w-0">
        <div class="dashboard-sidebar__brand-name">OfferPilot</div>
        <div class="dashboard-sidebar__brand-subtitle">AI 求职训练平台</div>
      </div>
    </RouterLink>

    <nav class="dashboard-sidebar__nav">
      <RouterLink
        to="/dashboard"
        class="dashboard-sidebar__item dashboard-sidebar__item--root"
        :class="{ 'dashboard-sidebar__item--active': isRouteMatch({ path: '/dashboard' }) }"
      >
        <span class="dashboard-sidebar__item-icon">
          <UiIcon name="dashboard" />
        </span>
        <span class="truncate">首页</span>
      </RouterLink>

      <section
        v-for="group in primaryGroups"
        :key="group.label"
        class="dashboard-sidebar__group"
        :class="{ 'dashboard-sidebar__group--active': isGroupActive(group) }"
      >
        <div
          class="dashboard-sidebar__group-head"
          :class="{ 'dashboard-sidebar__group-head--active': isGroupActive(group) }"
        >
          <span class="dashboard-sidebar__group-label">
            <span class="dashboard-sidebar__group-icon">
              <UiIcon :name="group.icon" />
            </span>
            <span>{{ group.label }}</span>
          </span>
          <UiIcon name="arrowRight" class="dashboard-sidebar__group-chevron" />
        </div>

        <div class="dashboard-sidebar__group-items">
          <RouterLink
            v-for="(item, index) in group.items"
            :key="item.label + item.path + JSON.stringify(item.query || {}) + (item.hash || '')"
            :to="routeTarget(item)"
            class="dashboard-sidebar__item"
            :class="{ 'dashboard-sidebar__item--active': isItemActive(item, group.items, index) }"
          >
            <span class="dashboard-sidebar__item-dot" />
            <span class="truncate">{{ item.label }}</span>
          </RouterLink>
        </div>
      </section>
    </nav>

    <RouterLink
      to="/settings"
      class="dashboard-sidebar__item dashboard-sidebar__item--root dashboard-sidebar__item--footer"
      :class="{ 'dashboard-sidebar__item--active': isRouteMatch({ path: '/settings' }) }"
    >
      <span class="dashboard-sidebar__item-icon">
        <UiIcon name="settings" />
      </span>
      <span class="truncate">{{ PRODUCT_PAGE_NAMES.settings }}</span>
    </RouterLink>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { PRODUCT_PAGE_NAMES } from '@/constants/productCopy'
import AppBrandGlyph from '@/components/AppBrandGlyph.vue'
import UiIcon from '@/components/ui/UiIcon.vue'

const route = useRoute()

type SidebarItem = {
  label: string
  path: string
  query?: Record<string, string>
  hash?: string
}

type SidebarGroup = {
  label: string
  icon: 'interview' | 'knowledge' | 'applications' | 'resume'
  items: SidebarItem[]
}

const primaryGroups: SidebarGroup[] = [
  {
    label: '面试训练',
    icon: 'interview',
    items: [
      { label: '模拟面试', path: '/interview' },
      { label: '题库训练', path: '/question' },
      { label: '错题本', path: '/wrong' },
      { label: '复习巩固', path: '/review' }
    ]
  },
  {
    label: '知识准备',
    icon: 'knowledge',
    items: [
      { label: '知识库', path: '/knowledge' },
      { label: '问答', path: '/chat' },
      { label: '我的收藏', path: '/favorites' }
    ]
  },
  {
    label: '求职进展',
    icon: 'applications',
    items: [
      { label: '学习计划', path: '/study-plan' },
      { label: '投递管理', path: '/applications' }
    ]
  },
  {
    label: '简历优化',
    icon: 'resume',
    items: [
      { label: '简历助手', path: '/resume' }
    ]
  }
]

const seededQuery = computed<Record<string, string>>(() => {
  const query: Record<string, string> = {}
  const seedTopic = typeof route.query.seedTopic === 'string' ? route.query.seedTopic.trim() : ''
  const seedWorkflow = typeof route.query.seedWorkflow === 'string' ? route.query.seedWorkflow.trim() : ''
  const seedNote = typeof route.query.seedNote === 'string' ? route.query.seedNote.trim() : ''
  if (seedTopic) {
    query.seedTopic = seedTopic
  }
  if (seedWorkflow) {
    query.seedWorkflow = seedWorkflow
  }
  if (seedNote) {
    query.seedNote = seedNote
  }
  return query
})

const shouldCarrySeed = (path: string) =>
  path === '/interview' ||
  path === '/resume' ||
  path === '/applications' ||
  path === '/review' ||
  path === '/question' ||
  path === '/wrong' ||
  path === '/knowledge' ||
  path === '/study-plan' ||
  path === '/chat'

const routeTarget = (item: SidebarItem) => {
  if (!item.query && !item.hash && (!shouldCarrySeed(item.path) || !Object.keys(seededQuery.value).length)) {
    return item.path
  }
  const query = {
    ...(shouldCarrySeed(item.path) ? seededQuery.value : {}),
    ...(item.query || {})
  }
  return {
    path: item.path,
    query: Object.keys(query).length ? query : undefined,
    hash: item.hash
  }
}

const isRouteMatch = (item: SidebarItem | { path: string }) => {
  const pathMatches = route.path === item.path || route.path.startsWith(item.path + '/')
  if (!pathMatches) return false

  if ('query' in item && item.query) {
    const queryMatches = Object.entries(item.query).every(([key, value]) => route.query[key] === value)
    if (!queryMatches) return false
  }

  if ('hash' in item && item.hash) {
    return route.hash === item.hash
  }

  return true
}

const itemSpecificity = (item: SidebarItem) =>
  item.path.length + Object.keys(item.query || {}).length * 1000 + (item.hash ? 100 : 0)

const isItemActive = (item: SidebarItem, siblings: SidebarItem[], index: number) => {
  if (!isRouteMatch(item)) return false

  const matchedSiblings = siblings
    .map((sibling, siblingIndex) => ({ item: sibling, index: siblingIndex }))
    .filter(({ item: sibling }) => isRouteMatch(sibling))

  if (!matchedSiblings.length) return false

  const strongestMatch = Math.max(...matchedSiblings.map(({ item: sibling }) => itemSpecificity(sibling)))
  const activeSibling = matchedSiblings.find(({ item: sibling }) => itemSpecificity(sibling) === strongestMatch)

  return activeSibling?.index === index
}

const isGroupActive = (group: SidebarGroup) => group.items.some((item) => isRouteMatch(item))
</script>

<style scoped>
.dashboard-sidebar {
  display: flex;
  height: 100%;
  flex-direction: column;
  gap: 1rem;
  overflow: hidden;
  border-right: 1px solid var(--bc-border-subtle);
  background: transparent;
  padding: 1rem 1rem 0.95rem 0.12rem;
  box-shadow: none;
}

.dashboard-sidebar__brand {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 0.15rem 0.3rem 1rem;
}

.dashboard-sidebar__brand-name {
  color: var(--bc-ink);
  font-size: 19px;
  font-weight: 800;
  letter-spacing: 0;
}

.dashboard-sidebar__brand-subtitle {
  margin-top: 0.08rem;
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.35;
}

.dashboard-sidebar__nav {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
  padding-right: 0.1rem;
}

.dashboard-sidebar__item:hover {
  background: rgba(var(--bc-accent-rgb), 0.065);
  color: var(--bc-ink);
}

.dashboard-sidebar__group-icon {
  display: inline-flex;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  color: currentColor;
  opacity: 0.82;
}

.dashboard-sidebar__group-icon :deep(.ui-icon) {
  width: 17px;
  height: 17px;
}

.dashboard-sidebar__group {
  display: flex;
  flex-direction: column;
  gap: 0.32rem;
}

.dashboard-sidebar__item--footer {
  margin-top: auto;
}

.dashboard-sidebar__item--root {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 16px;
  color: var(--bc-ink-secondary);
  font-size: 15px;
  font-weight: 650;
  line-height: 1.35;
  padding: 11px 14px 11px 16px;
  transition:
    background-color var(--motion-base) var(--ease-hard),
    color var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard),
    transform var(--motion-base) var(--ease-hard);
}

.dashboard-sidebar__item--root:hover {
  background: rgba(var(--bc-accent-rgb), 0.065);
  color: var(--bc-ink);
}

.dashboard-sidebar__item-icon {
  display: inline-flex;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  color: currentColor;
  opacity: 0.9;
}

.dashboard-sidebar__item-icon :deep(.ui-icon) {
  width: 18px;
  height: 18px;
}

.dashboard-sidebar__group + .dashboard-sidebar__group {
  border-top: 1px solid var(--bc-border-subtle);
  padding-top: 14px;
}

.dashboard-sidebar__group-items {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dashboard-sidebar__group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--bc-ink-tertiary);
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 0.032em;
  padding: 3px 14px 3px 16px;
  text-transform: uppercase;
  transition:
    color var(--motion-base) var(--ease-hard),
    transform var(--motion-base) var(--ease-hard);
}

.dashboard-sidebar__group-head--active {
  color: var(--bc-ink);
}

.dashboard-sidebar__group-label {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.dashboard-sidebar__group-head--active .dashboard-sidebar__group-icon {
  opacity: 1;
}

.dashboard-sidebar__group-chevron {
  width: 11px;
  height: 11px;
  color: var(--bc-ink-tertiary);
  opacity: 0.32;
  transform: scale(0.88);
}

.dashboard-sidebar__item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 16px;
  color: var(--bc-ink-secondary);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.35;
  padding: 11px 14px 11px 40px;
  transition:
    background-color var(--motion-base) var(--ease-hard),
    color var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard),
    transform var(--motion-base) var(--ease-hard);
}

.dashboard-sidebar__item--active {
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.11), rgba(var(--bc-accent-rgb), 0.045));
  box-shadow:
    inset 2px 0 0 rgba(var(--bc-accent-rgb), 0.82),
    0 10px 24px rgba(var(--bc-accent-rgb), 0.06);
  color: var(--bc-accent);
  font-weight: 750;
}

.dashboard-sidebar__item--active::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 50%;
  height: 18px;
  width: 2px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.92), rgba(var(--bc-accent-rgb), 0.56));
  transform: translateY(-50%);
}

.dashboard-sidebar__item-dot {
  width: 6px;
  height: 6px;
  flex-shrink: 0;
  border-radius: 999px;
  background: currentColor;
  opacity: 0.34;
  transition:
    transform var(--motion-base) var(--ease-hard),
    opacity var(--motion-base) var(--ease-hard);
}

.dashboard-sidebar__item--active .dashboard-sidebar__item-dot {
  opacity: 0.95;
  transform: scale(1.15);
}

@media (max-width: 1279px) {
  .dashboard-sidebar {
    height: auto;
  }
}

@media (max-width: 767px) {
  .dashboard-sidebar {
    border-radius: 24px;
    padding: 1rem 0.9rem;
  }

  .dashboard-sidebar__brand {
    padding-bottom: 0.9rem;
  }
}
</style>
