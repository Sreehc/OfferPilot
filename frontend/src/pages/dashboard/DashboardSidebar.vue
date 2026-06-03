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
          <svg
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="1.7"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M3.75 10.5 12 3.75l8.25 6.75V19.5A1.5 1.5 0 0 1 18.75 21h-13.5a1.5 1.5 0 0 1-1.5-1.5V10.5Z"
            />
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M9 21v-6.75h6V21"
            />
          </svg>
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
              <svg
                v-if="group.icon === 'interview'"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="1.7"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M7.5 8.25h9m-9 3h5.25m-6.75 7.5 1.462-2.435a1.5 1.5 0 0 1 1.286-.73H18A2.25 2.25 0 0 0 20.25 13.5v-6A2.25 2.25 0 0 0 18 5.25H6A2.25 2.25 0 0 0 3.75 7.5v6A2.25 2.25 0 0 0 6 15.75h.75v3Z"
                />
              </svg>
              <svg
                v-else-if="group.icon === 'knowledge'"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="1.7"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M12 6.253c-1.168-.776-2.754-1.253-4.5-1.253S4.168 5.477 3 6.253v13c1.168-.776 2.754-1.253 4.5-1.253s3.332.477 4.5 1.253m0-13c1.168-.776 2.754-1.253 4.5-1.253 1.747 0 3.332.477 4.5 1.253v13c-1.168-.776-2.753-1.253-4.5-1.253-1.746 0-3.332.477-4.5 1.253"
                />
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M12 6.25V19.25"
                />
              </svg>
              <svg
                v-else-if="group.icon === 'tools'"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="1.7"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M9 6.75V6a1.5 1.5 0 0 1 1.5-1.5h3A1.5 1.5 0 0 1 15 6v.75m-10.5 3h15m-13.5 0v7.5A1.5 1.5 0 0 0 7.5 18.75h9a1.5 1.5 0 0 0 1.5-1.5v-7.5"
                />
              </svg>
              <svg
                v-else-if="group.icon === 'resume'"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="1.7"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M15.75 3.75H7.5A2.25 2.25 0 0 0 5.25 6v12A2.25 2.25 0 0 0 7.5 20.25h9A2.25 2.25 0 0 0 18.75 18V6.75l-3-3Z"
                />
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M15.75 3.75V7.5h3M8.25 12h7.5M8.25 15.75h4.5"
                />
              </svg>
            </span>
            <span>{{ group.label }}</span>
          </span>
          <svg
            class="dashboard-sidebar__group-chevron"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="1.45"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="m9 6 6 6-6 6"
            />
          </svg>
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
        <svg
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="1.7"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M15.75 6.75a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.5 19.125a7.5 7.5 0 0 1 15 0"
          />
        </svg>
      </span>
      <span class="truncate">{{ PRODUCT_PAGE_NAMES.settings }}</span>
    </RouterLink>
  </aside>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { PRODUCT_PAGE_NAMES } from '@/constants/productCopy'
import AppBrandGlyph from '@/components/AppBrandGlyph.vue'

const route = useRoute()

type SidebarItem = {
  label: string
  path: string
  query?: Record<string, string>
  hash?: string
}

type SidebarGroup = {
  label: string
  icon: 'interview' | 'knowledge' | 'tools' | 'resume'
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
    icon: 'tools',
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

const routeTarget = (item: SidebarItem) => {
  if (!item.query && !item.hash) return item.path
  return {
    path: item.path,
    query: item.query,
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
  border-right: 1px solid rgba(18, 41, 76, 0.08);
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
  color: #142746;
  font-size: 19px;
  font-weight: 800;
  letter-spacing: 0;
}

.dashboard-sidebar__brand-subtitle {
  margin-top: 0.08rem;
  color: #8a96ab;
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
  background: rgba(83, 109, 169, 0.052);
  color: #16325d;
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

.dashboard-sidebar__group-icon svg {
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
  color: #6e7d94;
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
  background: rgba(83, 109, 169, 0.052);
  color: #16325d;
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

.dashboard-sidebar__item-icon svg {
  width: 18px;
  height: 18px;
}

.dashboard-sidebar__group + .dashboard-sidebar__group {
  border-top: 1px solid rgba(18, 41, 76, 0.06);
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
  color: #8694aa;
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
  color: #40526f;
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
  color: #c2cada;
  opacity: 0.32;
  transform: scale(0.88);
}

.dashboard-sidebar__item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 16px;
  color: #6e7d94;
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
  background: linear-gradient(180deg, rgba(103, 134, 214, 0.082), rgba(103, 134, 214, 0.036));
  box-shadow:
    inset 2px 0 0 rgba(90, 122, 206, 0.8),
    0 10px 24px rgba(71, 102, 176, 0.034);
  color: #2d4f98;
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
  background: linear-gradient(180deg, rgba(90, 122, 206, 0.92), rgba(136, 160, 224, 0.82));
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
