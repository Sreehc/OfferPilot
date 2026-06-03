<template>
  <div class="ambient-shell app-layout-root min-h-screen pb-24 md:pb-0">
    <a
      href="#main-content"
      class="skip-link"
    >
      跳到主要内容
    </a>

    <div
      class="app-shell"
      :class="{ 'app-shell--sidebar-hidden': !sidebarVisible }"
    >
      <aside
        class="app-shell__sidebar hidden lg:block"
        :aria-hidden="!sidebarVisible"
        :inert="!sidebarVisible"
      >
        <DashboardSidebar />
      </aside>

      <Transition name="mobile-rail-fade">
        <div
          v-if="sidebarVisible"
          class="mobile-rail-overlay lg:hidden"
          @click="sidebarVisible = false"
        />
      </Transition>

      <Transition name="mobile-rail-slide">
        <div
          v-if="sidebarVisible"
          class="mobile-rail-panel lg:hidden"
          role="dialog"
          aria-modal="true"
          aria-label="功能栏"
        >
          <DashboardSidebar class="mobile-rail-nav h-full min-h-0" />
        </div>
      </Transition>

      <div class="app-shell__stage">
        <header class="app-topbar">
          <div class="app-topbar__left">
            <button
              type="button"
              class="app-topbar__menu"
              aria-label="切换侧边栏"
              @click="sidebarVisible = !sidebarVisible"
            >
              <span />
              <span />
              <span />
            </button>

            <button
              type="button"
              class="app-topbar__search"
              @click="openSearch"
            >
              <span class="app-topbar__search-icon">
                <svg
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  stroke-width="1.8"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M21 21l-4.35-4.35m1.85-5.15a7 7 0 11-14 0 7 7 0 0114 0z"
                  />
                </svg>
              </span>
              <span class="app-topbar__search-text">搜索页面或操作</span>
              <span class="app-topbar__search-kbd hidden sm:inline-flex">/</span>
            </button>
          </div>

          <div class="app-topbar__right">
            <NotificationDropdown />
            <AvatarDropdown
              :name="displayName"
              :role="authStore.user?.role ?? 'USER'"
              :initials="initials"
              @logout="handleLogout"
            />
          </div>
        </header>

        <main
          id="main-content"
          tabindex="-1"
          class="app-shell__content"
        >
          <div class="app-canvas">
            <RouterView v-slot="{ Component, route: viewRoute }">
              <Transition
                name="page-slide"
                mode="out-in"
              >
                <component
                  :is="Component"
                  :key="viewRoute.path"
                  class="app-canvas-page"
                />
              </Transition>
            </RouterView>
          </div>
        </main>
      </div>
    </div>

    <MobileNavBar />
    <OfflinePage />

    <el-dialog
      v-model="searchVisible"
      width="520px"
      :show-close="false"
      class="global-search-dialog"
      @close="searchVisible = false"
    >
      <div class="p-1">
        <el-input
          ref="searchInputRef"
          v-model="searchQuery"
          size="large"
          placeholder="搜索训练模块、页面或操作..."
          clearable
          @keydown.escape="searchVisible = false"
          @keydown.enter="handleSearchNavigate"
        >
          <template #prefix>
            <svg
              class="h-4 w-4 text-tertiary"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
          </template>
        </el-input>
        <div class="mt-3 space-y-1">
          <button
            v-for="item in filteredSearchItems"
            :key="item.path"
            type="button"
            class="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm transition hover:bg-[rgba(var(--bc-accent-rgb),0.08)]"
            @click="navigateTo(item.path)"
          >
            <span class="font-medium text-ink">{{ item.label }}</span>
            <span class="ml-auto text-xs text-tertiary">{{ item.path }}</span>
          </button>
          <p
            v-if="searchQuery && !filteredSearchItems.length"
            class="py-4 text-center text-sm text-tertiary"
          >
            无匹配结果
          </p>
        </div>
      </div>
      <template #footer>
        <div class="flex items-center justify-between px-1 text-xs text-tertiary">
          <span><kbd class="search-kbd">/</kbd> 或
            <kbd class="search-kbd">⌘K</kbd>
            搜索</span>
          <span><kbd class="search-kbd">Esc</kbd> 关闭 ·
            <kbd class="search-kbd">回车</kbd>
            跳转</span>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MobileNavBar from '@/components/MobileNavBar.vue'
import OfflinePage from '@/components/OfflinePage.vue'
import NotificationDropdown from '@/components/NotificationDropdown.vue'
import AvatarDropdown from '@/components/AvatarDropdown.vue'
import { PRODUCT_PAGE_NAMES } from '@/constants/productCopy'
import DashboardSidebar from '@/pages/dashboard/DashboardSidebar.vue'
import { useAuthStore } from '@/stores/auth'
import type { ComponentPublicInstance } from 'vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const sidebarVisible = ref(false)
const searchVisible = ref(false)
const searchQuery = ref('')
const searchInputRef = ref<ComponentPublicInstance | null>(null)

const allSearchItems = [
  { label: '首页', path: '/dashboard' },
  { label: PRODUCT_PAGE_NAMES.question, path: '/question' },
  { label: PRODUCT_PAGE_NAMES.knowledge, path: '/knowledge' },
  { label: PRODUCT_PAGE_NAMES.chat, path: '/chat' },
  { label: PRODUCT_PAGE_NAMES.favorites, path: '/favorites' },
  { label: PRODUCT_PAGE_NAMES.interview, path: '/interview' },
  { label: PRODUCT_PAGE_NAMES.wrong, path: '/wrong' },
  { label: PRODUCT_PAGE_NAMES.review, path: '/review' },
  { label: PRODUCT_PAGE_NAMES.studyPlan, path: '/study-plan' },
  { label: PRODUCT_PAGE_NAMES.applications, path: '/applications' },
  { label: PRODUCT_PAGE_NAMES.resume, path: '/resume' },
  { label: PRODUCT_PAGE_NAMES.analytics, path: '/analytics' },
  { label: '社区', path: '/community' },
  { label: '排行榜', path: '/community/leaderboard' },
  { label: '管理后台', path: '/admin', adminOnly: true },
  { label: PRODUCT_PAGE_NAMES.settings, path: '/settings' }
]

const searchItems = computed(() => allSearchItems.filter((item) => !item.adminOnly || authStore.user?.role === 'ADMIN'))
const filteredSearchItems = computed(() => {
  if (!searchQuery.value) return searchItems.value
  const q = searchQuery.value.toLowerCase()
  return searchItems.value.filter((item) => item.label.toLowerCase().includes(q) || item.path.toLowerCase().includes(q))
})

const displayName = computed(() => authStore.user?.nickname || '访客')
const initials = computed(() => displayName.value.slice(0, 1).toUpperCase())

const readSeedQueryValue = (key: 'seedTopic' | 'seedWorkflow' | 'seedNote') => {
  const value = route.query[key]
  return typeof value === 'string' ? value.trim() : ''
}

const seededTopic = computed(() => readSeedQueryValue('seedTopic'))
const seededWorkflow = computed(() => readSeedQueryValue('seedWorkflow'))
const seededNote = computed(() => readSeedQueryValue('seedNote'))

const carrySeedToPath = (path: string) => {
  if (!seededTopic.value && !seededWorkflow.value && !seededNote.value) return path
  if (
    !path.startsWith('/interview') &&
    !path.startsWith('/resume') &&
    !path.startsWith('/applications') &&
    !path.startsWith('/analytics') &&
    !path.startsWith('/study-plan') &&
    !path.startsWith('/review') &&
    !path.startsWith('/wrong') &&
    !path.startsWith('/question') &&
    !path.startsWith('/chat') &&
    !path.startsWith('/knowledge')
  ) {
    return path
  }
  const [rawPathWithoutHash, rawHash] = path.split('#')
  const pathWithoutHash = rawPathWithoutHash || ''
  const hash = rawHash || ''
  const [rawPathname, rawQuery] = pathWithoutHash.split('?')
  const pathname = rawPathname || ''
  const query = new URLSearchParams(rawQuery)
  if (seededTopic.value && !query.get('seedTopic') && pathname !== '/analytics') {
    query.set('seedTopic', seededTopic.value)
  }
  if (seededWorkflow.value && !query.get('seedWorkflow') && pathname !== '/analytics') {
    query.set('seedWorkflow', seededWorkflow.value)
  }
  if (seededNote.value && !query.get('seedNote') && pathname !== '/analytics') {
    query.set('seedNote', seededNote.value)
  }
  const nextPath = query.toString() ? `${pathname}?${query.toString()}` : pathname
  return hash ? `${nextPath}#${hash}` : nextPath
}

const navigateTo = (path: string) => {
  searchVisible.value = false
  searchQuery.value = ''
  if (window.innerWidth < 1024) {
    sidebarVisible.value = false
  }
  router.push(carrySeedToPath(path))
}

const handleSearchNavigate = () => {
  if (filteredSearchItems.value.length > 0) {
    navigateTo(filteredSearchItems.value[0]?.path ?? '/dashboard')
  }
}

const handleKeydown = (e: KeyboardEvent) => {
  const isMod = e.metaKey || e.ctrlKey
  const target = e.target as HTMLElement | null
  const isInput =
    target?.tagName === 'INPUT' ||
    target?.tagName === 'TEXTAREA' ||
    target?.contentEditable === 'true'

  if ((isMod && e.key === 'k') || (e.key === '/' && !isInput && !searchVisible.value)) {
    e.preventDefault()
    openSearch()
  }

  if (e.key === 'Escape' && searchVisible.value) {
    e.preventDefault()
    searchVisible.value = false
  }

  if (isMod && e.key === 'b') {
    e.preventDefault()
    sidebarVisible.value = !sidebarVisible.value
  }
}

const openSidebar = () => {
  sidebarVisible.value = true
}

const openSearch = () => {
  searchVisible.value = true
  nextTick(() => {
    const input = searchInputRef.value?.$el?.querySelector('input') as HTMLInputElement | null
    input?.focus()
  })
}

const handleResize = () => {
  sidebarVisible.value = window.innerWidth >= 1024
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('offerpilot:open-sidebar', openSidebar)
  window.addEventListener('offerpilot:open-search', openSearch)
  window.addEventListener('resize', handleResize)
  handleResize()
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('offerpilot:open-sidebar', openSidebar)
  window.removeEventListener('offerpilot:open-search', openSearch)
  window.removeEventListener('resize', handleResize)
})

watch(
  () => route.fullPath,
  () => {
    searchVisible.value = false
    searchQuery.value = ''
    if (window.innerWidth < 1024) {
      sidebarVisible.value = false
    }
  }
)

const handleLogout = async () => {
  await authStore.logout()
}
</script>

<style scoped>
.app-layout-root {
  background:
    radial-gradient(circle at 0 0, rgba(var(--bc-accent-rgb), 0.08), transparent 24%),
    linear-gradient(180deg, #f5f8ff 0%, #eff3fb 100%);
}

.app-shell {
  display: block;
  --app-shell-sidebar-width: 0px;
}

.app-shell__stage {
  min-width: 0;
}

.app-shell__content {
  padding: 14px;
}

.app-shell__sidebar {
  overflow: hidden;
  padding: 12px 0 12px 14px;
}

.app-canvas {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  gap: 16px;
  margin-inline: 0;
  width: 100%;
  max-width: none;
}

.app-canvas-page {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.skip-link {
  position: fixed;
  left: 1rem;
  top: 0.75rem;
  z-index: 120;
  transform: translateY(-180%);
  border-radius: 999px;
  background: var(--bc-ink);
  color: var(--bc-shell);
  padding: 0.55rem 0.9rem;
  font-size: 0.85rem;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(var(--bc-ink-rgb), 0.18);
  transition: transform 160ms ease;
}

.skip-link:focus {
  transform: translateY(0);
}

.app-topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px 6px;
  background:
    linear-gradient(180deg, rgba(245, 248, 255, 0.92), rgba(245, 248, 255, 0.72) 72%, transparent);
  backdrop-filter: blur(16px);
}

.app-topbar__left,
.app-topbar__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-topbar__left {
  min-width: 0;
  flex: 1;
}

.app-topbar__menu {
  display: inline-flex;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  border: 1px solid var(--bc-border-subtle);
  border-radius: 12px;
  background: var(--interactive-bg);
  box-shadow: var(--bc-shadow-soft);
  transition:
    background-color var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard);
}

.app-topbar__menu:hover {
  background: rgba(var(--bc-accent-rgb), 0.08);
  box-shadow: var(--bc-shadow-hover);
}

.app-topbar__menu span {
  width: 18px;
  height: 2px;
  border-radius: 999px;
  background: var(--bc-ink);
}

.app-topbar__search {
  display: inline-flex;
  min-width: 0;
  flex: 1 1 auto;
  width: 100%;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  border-radius: 18px;
  border: 1px solid rgba(84, 116, 198, 0.14);
  background: rgba(255, 255, 255, 0.92);
  color: #6f7d95;
  padding: 0.45rem 0.82rem;
  box-shadow: 0 8px 18px rgba(41, 62, 109, 0.05);
  transition:
    border-color var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard),
    transform var(--motion-base) var(--ease-hard);
}

.app-topbar__search:hover {
  border-color: rgba(84, 116, 198, 0.22);
  box-shadow: 0 10px 20px rgba(41, 62, 109, 0.08);
  transform: translateY(-1px);
}

.app-topbar__search-icon {
  display: inline-flex;
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.app-topbar__search-text {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: left;
  font-size: 0.92rem;
  font-weight: 500;
}

.app-topbar__search-kbd {
  border-radius: 999px;
  border: 1px solid rgba(84, 116, 198, 0.14);
  background: rgba(246, 249, 255, 0.92);
  color: #6c7a93;
  padding: 0.18rem 0.52rem;
  font-size: 0.72rem;
  font-weight: 700;
}

.mobile-rail-overlay {
  position: fixed;
  inset: 0;
  z-index: 38;
  background: rgba(16, 12, 8, 0.32);
  backdrop-filter: blur(6px);
}

.mobile-rail-panel {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 39;
  width: min(84vw, 320px);
  height: 100dvh;
  padding: 12px 0 12px 14px;
  background:
    linear-gradient(180deg, rgba(244, 247, 254, 0.98), rgba(240, 244, 252, 0.95)),
    var(--bc-panel);
  box-shadow: 24px 0 48px rgba(32, 40, 53, 0.16);
}

.mobile-rail-nav {
  background: transparent;
}

.mobile-rail-fade-enter-active,
.mobile-rail-fade-leave-active {
  transition: opacity 180ms ease;
}

.mobile-rail-fade-enter-from,
.mobile-rail-fade-leave-to {
  opacity: 0;
}

.mobile-rail-slide-enter-active,
.mobile-rail-slide-leave-active {
  transition:
    opacity 200ms ease,
    transform 200ms ease;
}

.mobile-rail-slide-enter-from,
.mobile-rail-slide-leave-to {
  opacity: 0;
  transform: translateX(-18px);
}

.search-kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  border-radius: 8px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--interactive-bg);
  padding: 0.15rem 0.4rem;
  color: var(--bc-ink-secondary);
  box-shadow: var(--bc-shadow-soft);
}

@media (min-width: 1024px) {
  .app-shell {
    --app-shell-sidebar-width: 252px;
    display: grid;
    grid-template-columns: var(--app-shell-sidebar-width) minmax(0, 1fr);
    align-items: start;
    transition: grid-template-columns 240ms var(--ease-hard);
  }

  .app-shell--sidebar-hidden {
    --app-shell-sidebar-width: 0px;
  }

  .app-shell__sidebar {
    position: sticky;
    top: 0;
    align-self: start;
    height: 100dvh;
    width: var(--app-shell-sidebar-width);
    min-width: 0;
    opacity: 1;
    transform: translateX(0);
    transition:
      width 220ms var(--ease-hard),
      opacity 180ms var(--ease-hard),
      transform 220ms var(--ease-hard),
      padding 220ms var(--ease-hard);
  }

  .app-shell--sidebar-hidden .app-shell__sidebar {
    pointer-events: none;
    opacity: 0;
    padding-left: 0;
    padding-right: 0;
    transform: translateX(-18px);
  }

  .app-shell__stage {
    min-height: 100dvh;
  }

  .app-shell__content {
    padding: 10px 18px 18px 8px;
  }

  .app-shell--sidebar-hidden .app-shell__content {
    padding-left: 18px;
  }
}

@media (max-width: 767px) {
  .app-topbar {
    gap: 10px;
    padding: 8px 12px 4px;
  }

  .app-topbar__left,
  .app-topbar__right {
    gap: 10px;
  }

  .app-topbar__menu {
    width: 44px;
    height: 44px;
    flex-basis: 44px;
    border-radius: 13px;
  }

  .app-topbar__search {
    min-height: 44px;
    border-radius: 16px;
    padding: 0.38rem 0.72rem;
    box-shadow: 0 6px 14px rgba(41, 62, 109, 0.05);
  }

  .app-topbar__search-text {
    font-size: 0.84rem;
  }

  .app-topbar__search-kbd {
    display: none;
  }
}

@media (min-width: 1280px) {
  .app-shell {
    --app-shell-sidebar-width: 264px;
  }

  .app-shell--sidebar-hidden {
    --app-shell-sidebar-width: 0px;
  }
}

@media (min-width: 1440px) {
  .app-shell__content {
    padding-right: 22px;
  }
}

@media (max-width: 767px) {
  .app-topbar {
    align-items: flex-start;
    flex-direction: column;
    padding: 12px 12px 0;
  }

  .app-topbar__left,
  .app-topbar__right {
    gap: 10px;
  }

  .app-topbar__left,
  .app-topbar__search {
    width: 100%;
  }

  .app-topbar__right {
    width: 100%;
    justify-content: flex-end;
  }

  .app-shell__content {
    padding: 12px;
  }
}
</style>
