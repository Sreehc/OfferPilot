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
            <span class="global-topbar__brand-angle global-topbar__brand-angle-left">&lt;</span>
            <span class="global-topbar__brand-cup">
              <span class="global-topbar__brand-steam global-topbar__brand-steam-one"></span>
              <span class="global-topbar__brand-steam global-topbar__brand-steam-two"></span>
              <span class="global-topbar__brand-cup-body"></span>
              <span class="global-topbar__brand-cup-handle"></span>
            </span>
            <span class="global-topbar__brand-angle global-topbar__brand-angle-right">/&gt;</span>
          </span>
          <span class="global-topbar__brand-name">BYTE COACH</span>
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
      class="pt-[73px] lg:mt-[73px] lg:grid lg:h-[calc(100dvh-73px)] lg:pt-0 lg:overflow-hidden"
      :class="sidebarVisible ? 'lg:grid-cols-[292px_minmax(0,1fr)]' : 'lg:grid-cols-[minmax(0,1fr)]'"
    >
      <NavRail
        v-show="sidebarVisible"
        class="hidden min-h-[280px] border-r border-[var(--bc-line)] lg:block lg:h-full lg:min-h-0 lg:overflow-y-auto"
      />

      <main
        class="relative z-[1] flex min-w-0 flex-col px-4 py-4 md:px-6 md:py-6 lg:h-full lg:min-h-0 lg:overflow-hidden lg:px-8 lg:py-6"
      >
        <section class="min-h-0 flex-1 lg:overflow-y-auto">
          <RouterView v-slot="{ Component, route: viewRoute }">
            <Transition name="page-slide" mode="out-in">
              <component :is="Component" :key="viewRoute.path" />
            </Transition>
          </RouterView>
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
            class="flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-left text-sm transition hover:bg-slate-100 dark:hover:bg-slate-800"
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
            ><kbd class="rounded border border-slate-300 px-1 py-0.5 text-[10px] dark:border-slate-600">/</kbd> 或
            <kbd class="rounded border border-slate-300 px-1 py-0.5 text-[10px] dark:border-slate-600">⌘K</kbd>
            搜索</span
          >
          <span
            ><kbd class="rounded border border-slate-300 px-1 py-0.5 text-[10px] dark:border-slate-600">Esc</kbd> 关闭 ·
            <kbd class="rounded border border-slate-300 px-1 py-0.5 text-[10px] dark:border-slate-600">回车</kbd>
            跳转</span
          >
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavRail from '@/components/NavRail.vue'
import MobileNavBar from '@/components/MobileNavBar.vue'
import OfflinePage from '@/components/OfflinePage.vue'
import NotificationDropdown from '@/components/NotificationDropdown.vue'
import AvatarDropdown from '@/components/AvatarDropdown.vue'
import { useAuthStore } from '@/stores/auth'
import type { ComponentPublicInstance } from 'vue'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

// Sidebar visibility (Cmd+B)
const sidebarVisible = ref(true)

// Global search (Cmd+K)
const searchVisible = ref(false)
const searchQuery = ref('')
const searchInputRef = ref<ComponentPublicInstance | null>(null)

const allSearchItems = [
  { label: '首页概览', path: '/dashboard' },
  { label: '智能问答', path: '/chat' },
  { label: '知识库', path: '/knowledge' },
  { label: '模拟面试', path: '/interview' },
  { label: '面试历史', path: '/interview/history' },
  { label: '错题复习', path: '/review' },
  { label: '学习社区', path: '/community' },
  { label: '排行榜', path: '/community/leaderboard' },
  { label: '数据分析', path: '/analytics' },
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

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

const displayName = computed(() => authStore.user?.nickname || '访客')
const initials = computed(() => displayName.value.slice(0, 1).toUpperCase())

const handleLogout = async () => {
  await authStore.logout()
}
</script>

<style scoped>
.global-topbar {
  position: fixed;
  inset: 0 0 auto 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 73px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(120, 223, 214, 0.16);
  background:
    radial-gradient(circle at top left, rgba(76, 201, 240, 0.18), transparent 28%),
    linear-gradient(90deg, #0f172a 0%, #10243a 52%, #0d1f33 100%);
  box-shadow: 0 10px 32px rgba(5, 12, 24, 0.24);
}

.global-topbar__left,
.global-topbar__right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.global-topbar__menu {
  display: inline-flex;
  width: 40px;
  height: 40px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  transition: background-color var(--motion-base) var(--ease-hard);
}

.global-topbar__menu:hover {
  background: rgba(255, 255, 255, 0.08);
}

.global-topbar__menu span {
  width: 24px;
  height: 2.5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
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
  width: 58px;
  height: 44px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  gap: 3px;
  overflow: hidden;
  border: 1px solid rgba(120, 223, 214, 0.28);
  border-radius: 16px;
  background:
    radial-gradient(circle at top, rgba(76, 201, 240, 0.22), transparent 52%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.03));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 12px 24px rgba(6, 18, 35, 0.28);
}

.global-topbar__brand-angle {
  position: relative;
  z-index: 1;
  font-family: theme('fontFamily.mono');
  font-size: 10px;
  font-weight: 700;
  letter-spacing: -0.08em;
  color: rgba(120, 223, 214, 0.9);
}

.global-topbar__brand-cup {
  position: relative;
  display: inline-flex;
  width: 20px;
  height: 24px;
  align-items: flex-end;
  justify-content: center;
}

.global-topbar__brand-steam {
  position: absolute;
  top: 1px;
  width: 5px;
  height: 9px;
  border-top: 1.5px solid rgba(255, 205, 86, 0.92);
  border-left: 1.5px solid transparent;
  border-right: 1.5px solid transparent;
  border-radius: 999px;
  opacity: 0.95;
}

.global-topbar__brand-steam-one {
  left: 4px;
  transform: rotate(-10deg);
}

.global-topbar__brand-steam-two {
  right: 4px;
  transform: rotate(10deg);
}

.global-topbar__brand-cup-body {
  position: absolute;
  bottom: 3px;
  width: 13px;
  height: 8px;
  border-radius: 0 0 5px 5px;
  background: linear-gradient(180deg, #ffd166 0%, #f59e0b 100%);
}

.global-topbar__brand-cup-handle {
  position: absolute;
  right: 0;
  bottom: 5px;
  width: 5px;
  height: 5px;
  border: 1.5px solid rgba(255, 205, 86, 0.92);
  border-left: 0;
  border-radius: 0 999px 999px 0;
}

.global-topbar__brand-name {
  color: rgba(241, 245, 249, 0.96);
  font-size: 1.85rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  white-space: nowrap;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.04);
}

.global-topbar__login {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  border-radius: 10px;
  padding: 0 18px;
  background: linear-gradient(135deg, #4cc9f0 0%, #2a9dce 100%);
  color: #062131;
  font-size: 14px;
  font-weight: 700;
  transition:
    transform var(--motion-base) var(--ease-hard),
    opacity var(--motion-base) var(--ease-hard);
}

.global-topbar__login:hover {
  transform: translateY(-1px);
  opacity: 0.94;
}

:deep(.global-topbar__right .relative > button) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(120, 223, 214, 0.12);
}

:deep(.global-topbar__right .relative > button:hover) {
  background: rgba(255, 255, 255, 0.14);
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
    font-size: 1.15rem;
  }

  .global-topbar__brand-mark {
    width: 50px;
    height: 38px;
    border-radius: 14px;
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
