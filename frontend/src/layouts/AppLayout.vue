<template>
  <div class="ambient-shell min-h-screen pb-24 md:pb-0">
    <header class="global-topbar">
      <div class="global-topbar__left">
        <button
          type="button"
          class="global-topbar__menu"
          aria-label="切换侧边栏"
          @click="sidebarVisible = !sidebarVisible"
        >
          <span></span>
          <span></span>
          <span></span>
        </button>

        <RouterLink to="/dashboard" class="global-topbar__brand">
          <span class="global-topbar__brand-mark">
            <span class="global-topbar__brand-core"></span>
            <span class="global-topbar__brand-dot"></span>
          </span>
          <span class="global-topbar__brand-name">ByteCoach</span>
        </RouterLink>
      </div>

      <div class="global-topbar__right">
        <template v-if="authStore.isLoggedIn">
          <NotificationDropdown />
          <AvatarDropdown
            :name="displayName"
            :role="authStore.user?.role ?? 'USER'"
            :initials="initials"
            @logout="handleLogout"
          />
        </template>
        <RouterLink v-else to="/login" class="global-topbar__login">登录</RouterLink>
      </div>
    </header>

    <div
      class="app-layout-shell pt-[64px] lg:mt-[64px] lg:h-[calc(100dvh-64px)] lg:pt-0 lg:overflow-hidden"
      :class="{ 'app-layout-shell-sidebar-hidden': !sidebarVisible }"
    >
      <div
        class="desktop-rail hidden lg:block"
        :class="{ 'desktop-rail-collapsed': !sidebarVisible }"
        :aria-hidden="!sidebarVisible"
        :inert="!sidebarVisible"
      >
        <NavRail
          class="min-h-[280px] border-r border-[var(--bc-border-subtle)] lg:h-full lg:min-h-0 lg:overflow-y-auto"
        />
      </div>

      <Transition name="mobile-rail-fade">
        <div v-if="sidebarVisible" class="mobile-rail-overlay lg:hidden" @click="sidebarVisible = false"></div>
      </Transition>

      <Transition name="mobile-rail-slide">
        <div
          v-if="sidebarVisible"
          class="mobile-rail-panel lg:hidden"
          role="dialog"
          aria-modal="true"
          aria-label="功能栏"
        >
          <NavRail class="mobile-rail-nav h-full min-h-0" />
        </div>
      </Transition>

      <main
        class="relative z-[1] flex min-w-0 flex-col px-4 py-5 md:px-6 md:py-7 lg:h-full lg:min-h-0 lg:overflow-hidden lg:px-5 lg:py-7 xl:px-6"
      >
        <section class="min-h-0 flex flex-1 lg:overflow-y-auto">
          <div class="app-canvas w-full max-w-[1720px]">
            <RouterView v-slot="{ Component, route: viewRoute }">
              <Transition name="page-slide" mode="out-in">
                <component :is="Component" :key="viewRoute.path" class="app-canvas-page" />
              </Transition>
            </RouterView>
          </div>
        </section>
      </main>
    </div>

    <!-- Mobile bottom tab bar -->
    <MobileNavBar />

    <!-- Offline overlay -->
    <OfflinePage />

    <!-- Global Search Modal (Cmd+K) -->
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
          placeholder="搜索页面或操作..."
          clearable
          @keydown.escape="searchVisible = false"
          @keydown.enter="handleSearchNavigate"
        >
          <template #prefix>
            <svg
              class="h-4 w-4 text-slate-400 dark:text-slate-500"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
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
            <span class="font-medium text-ink dark:text-slate-200">{{ item.label }}</span>
            <span class="ml-auto text-xs text-slate-400 dark:text-slate-500">{{ item.path }}</span>
          </button>
          <p
            v-if="searchQuery && !filteredSearchItems.length"
            class="py-4 text-center text-sm text-slate-400 dark:text-slate-500"
          >
            无匹配结果
          </p>
        </div>
      </div>
      <template #footer>
        <div class="flex items-center justify-between px-1 text-xs text-slate-400 dark:text-slate-500">
          <span
            ><kbd class="search-kbd">/</kbd> 或
            <kbd class="search-kbd">⌘K</kbd>
            搜索</span
          >
          <span
            ><kbd class="search-kbd">Esc</kbd> 关闭 ·
            <kbd class="search-kbd">回车</kbd>
            跳转</span
          >
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NavRail from '@/components/NavRail.vue'
import MobileNavBar from '@/components/MobileNavBar.vue'
import OfflinePage from '@/components/OfflinePage.vue'
import NotificationDropdown from '@/components/NotificationDropdown.vue'
import AvatarDropdown from '@/components/AvatarDropdown.vue'
import { useAuthStore } from '@/stores/auth'
import type { ComponentPublicInstance } from 'vue'

const authStore = useAuthStore()
const router = useRouter()

// Sidebar visibility (Cmd+B)
const sidebarVisible = ref(false)

// Global search (Cmd+K)
const searchVisible = ref(false)
const searchQuery = ref('')
const searchInputRef = ref<ComponentPublicInstance | null>(null)

const allSearchItems = [
  { label: '首页概览', path: '/dashboard' },
  { label: '今日卡片', path: '/cards' },
  { label: '知识库', path: '/knowledge' },
  { label: '复习', path: '/review' },
  { label: '问答', path: '/chat' },
  { label: '面试诊断', path: '/interview' },
  { label: '数据分析', path: '/analytics' },
  { label: '社区', path: '/community' },
  { label: '面试诊断历史', path: '/interview/history' },
  { label: '排行榜', path: '/community/leaderboard' },
  { label: '管理后台', path: '/admin', adminOnly: true },
  { label: '账户设置', path: '/settings' }
]

const searchItems = computed(() => allSearchItems.filter((item) => !item.adminOnly || authStore.user?.role === 'ADMIN'))

const filteredSearchItems = computed(() => {
  if (!searchQuery.value) return searchItems.value
  const q = searchQuery.value.toLowerCase()
  return searchItems.value.filter((item) => item.label.toLowerCase().includes(q) || item.path.toLowerCase().includes(q))
})

const navigateTo = (path: string) => {
  searchVisible.value = false
  searchQuery.value = ''
  sidebarVisible.value = false
  router.push(path)
}

const handleSearchNavigate = () => {
  if (filteredSearchItems.value.length > 0) {
    navigateTo(filteredSearchItems.value[0]?.path ?? '/dashboard')
  }
}

// Keyboard shortcuts
const handleKeydown = (e: KeyboardEvent) => {
  const isMod = e.metaKey || e.ctrlKey
  const isInput =
    (e.target as HTMLElement)?.tagName === 'INPUT' ||
    (e.target as HTMLElement)?.tagName === 'TEXTAREA' ||
    (e.target as HTMLElement)?.contentEditable === 'true'

  // Cmd/Ctrl+K or / to open search (when not in input)
  if ((isMod && e.key === 'k') || (e.key === '/' && !isInput && !searchVisible.value)) {
    e.preventDefault()
    searchVisible.value = true
    nextTick(() => {
      const input = searchInputRef.value?.$el?.querySelector('input') as HTMLInputElement | null
      input?.focus()
    })
  }

  // Esc to close search
  if (e.key === 'Escape' && searchVisible.value) {
    e.preventDefault()
    searchVisible.value = false
  }

  // Cmd/Ctrl+B to toggle sidebar
  if (isMod && e.key === 'b') {
    e.preventDefault()
    sidebarVisible.value = !sidebarVisible.value
  }
}

const openSidebar = () => {
  sidebarVisible.value = true
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('bytecoach:open-sidebar', openSidebar)
  if (window.innerWidth >= 1024) {
    sidebarVisible.value = true
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('bytecoach:open-sidebar', openSidebar)
})

const displayName = computed(() => authStore.user?.nickname || '访客')
const initials = computed(() => displayName.value.slice(0, 1).toUpperCase())

const handleLogout = async () => {
  await authStore.logout()
}
</script>

<style scoped>
.app-canvas {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 100%;
  margin-inline: auto;
  width: 100%;
  max-width: 1720px;
}

.app-canvas-page {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 100%;
}

.app-layout-shell {
  display: block;
}

.desktop-rail {
  min-width: 0;
}

.global-topbar {
  position: fixed;
  inset: 0 0 auto 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 64px;
  padding: 0 16px;
  border-bottom: 1px solid var(--bc-border-subtle);
  background:
    radial-gradient(circle at 12% 20%, rgba(var(--bc-accent-rgb), 0.08), transparent 18%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 244, 238, 0.88));
  box-shadow: 0 8px 24px rgba(32, 40, 53, 0.05);
  backdrop-filter: blur(18px);
}

.global-topbar__left,
.global-topbar__right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.global-topbar__menu {
  display: inline-flex;
  width: 40px;
  height: 40px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  border: 1px solid var(--bc-border-subtle);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.56);
  box-shadow: var(--bc-shadow-soft);
  transition:
    background-color var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard);
}

.global-topbar__menu:hover {
  background: rgba(var(--bc-accent-rgb), 0.08);
  box-shadow: var(--bc-shadow-hover);
}

.global-topbar__menu span {
  width: 18px;
  height: 2px;
  border-radius: 999px;
  background: var(--bc-ink);
}

.global-topbar__brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.global-topbar__brand-mark {
  position: relative;
  display: inline-flex;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.14);
  border-radius: 999px;
  background:
    radial-gradient(circle at 32% 32%, rgba(var(--bc-accent-rgb), 0.22), transparent 48%),
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.14), rgba(var(--bc-accent-rgb), 0.06));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.48),
    0 8px 16px rgba(var(--bc-accent-rgb), 0.12);
}

.global-topbar__brand-core {
  position: relative;
  width: 12px;
  height: 12px;
  border: 2px solid var(--bc-ink);
  border-radius: 4px;
  transform: rotate(45deg);
}

.global-topbar__brand-dot {
  position: absolute;
  right: 8px;
  top: 8px;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--bc-accent);
}

.global-topbar__brand-name {
  color: var(--bc-ink);
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  white-space: nowrap;
}

.global-topbar__login {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 12px;
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.72);
  color: var(--bc-ink);
  font-size: 14px;
  font-weight: 700;
  box-shadow: var(--bc-shadow-soft);
  transition:
    transform var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard);
}

.global-topbar__login:hover {
  transform: translateY(-1px);
  box-shadow: var(--bc-shadow-hover);
}

.mobile-rail-overlay {
  position: fixed;
  inset: 64px 0 0;
  z-index: 38;
  background: rgba(16, 35, 58, 0.14);
  backdrop-filter: blur(6px);
}

.mobile-rail-panel {
  position: fixed;
  top: 64px;
  left: 0;
  z-index: 39;
  width: min(84vw, 320px);
  height: calc(100dvh - 64px);
  border-right: 1px solid var(--bc-border-subtle);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 244, 238, 0.96)),
    var(--bc-panel);
  box-shadow: 24px 0 48px rgba(32, 40, 53, 0.12);
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
  background: rgba(255, 255, 255, 0.72);
  padding: 0.15rem 0.4rem;
  color: var(--bc-ink-secondary);
  box-shadow: var(--bc-shadow-soft);
}

@media (min-width: 1024px) {
  .app-layout-shell {
    display: grid;
    grid-template-columns: 252px minmax(0, 1fr);
    transition: grid-template-columns 240ms var(--ease-hard);
  }

  .app-layout-shell-sidebar-hidden {
    grid-template-columns: 0 minmax(0, 1fr);
  }

  .desktop-rail {
    overflow: hidden;
    opacity: 1;
    transform: translateX(0);
    transition:
      opacity 180ms var(--ease-hard),
      transform 220ms var(--ease-hard);
  }

  .desktop-rail-collapsed {
    pointer-events: none;
    opacity: 0;
    transform: translateX(-18px);
  }
}

@media (min-width: 1280px) {
  .app-layout-shell {
    grid-template-columns: 264px minmax(0, 1fr);
  }

  .app-layout-shell-sidebar-hidden {
    grid-template-columns: 0 minmax(0, 1fr);
  }
}

@media (min-width: 1024px) {
  .global-topbar {
    padding-inline: 22px;
  }
}

@media (max-width: 767px) {
  .global-topbar {
    padding-inline: 12px;
  }

  .global-topbar__left,
  .global-topbar__right {
    gap: 10px;
  }

  .global-topbar__brand-name {
    font-size: 1.02rem;
  }

  .global-topbar__brand-mark {
    width: 34px;
    height: 34px;
  }
}

@media (min-width: 1024px) {
  :global(html),
  :global(body),
  :global(#app) {
    height: 100%;
    overflow: hidden;
  }
}
</style>
