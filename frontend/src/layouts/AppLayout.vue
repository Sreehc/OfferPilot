<template>
  <div class="min-h-screen px-4 py-4 pb-20 md:pb-4 md:px-6 md:py-6">
    <div class="mx-auto grid max-w-[1480px] gap-4 lg:grid-cols-[300px_minmax(0,1fr)]">
      <!-- Desktop sidebar: hidden below lg -->
      <NavRail v-show="sidebarVisible" class="hidden lg:block min-h-[280px]" />

      <main class="space-y-4">
        <AppShellHeader
          :kicker="headerMeta.kicker"
          :title="headerMeta.title"
          :subtitle="headerMeta.subtitle"
          :name="displayName"
          :role="authStore.user?.role ?? 'USER'"
          :initials="initials"
          @logout="handleLogout"
        >
          <template #actions>
            <NotificationDropdown />
            <button
              type="button"
              class="flex h-9 w-9 items-center justify-center rounded-full transition hover:bg-slate-200 dark:hover:bg-slate-700"
              title="切换主题 (无快捷键)"
              @click="toggleTheme"
            >
              <svg v-if="theme === 'light'" class="h-4.5 w-4.5 text-slate-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M20.354 15.354A9 9 0 018.646 3.646 9.005 9.005 0 0012 21a9.005 9.005 0 008.354-5.646z" />
              </svg>
              <svg v-else class="h-4.5 w-4.5 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
            </button>
          </template>
        </AppShellHeader>

        <section class="overflow-hidden p-1 md:p-2">
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
          placeholder="搜索页面、功能..."
          clearable
          @keydown.escape="searchVisible = false"
          @keydown.enter="handleSearchNavigate"
        >
          <template #prefix>
            <svg class="h-4 w-4 text-slate-400 dark:text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
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
          <p v-if="searchQuery && !filteredSearchItems.length" class="py-4 text-center text-sm text-slate-400 dark:text-slate-500">
            无匹配结果
          </p>
        </div>
      </div>
      <template #footer>
        <div class="flex items-center justify-between px-1 text-xs text-slate-400 dark:text-slate-500">
          <span><kbd class="rounded border border-slate-300 px-1 py-0.5 text-[10px] dark:border-slate-600">/</kbd> 或 <kbd class="rounded border border-slate-300 px-1 py-0.5 text-[10px] dark:border-slate-600">⌘K</kbd> 搜索</span>
          <span><kbd class="rounded border border-slate-300 px-1 py-0.5 text-[10px] dark:border-slate-600">Esc</kbd> 关闭 · <kbd class="rounded border border-slate-300 px-1 py-0.5 text-[10px] dark:border-slate-600">Enter</kbd> 跳转</span>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShellHeader from '@/components/AppShellHeader.vue'
import NavRail from '@/components/NavRail.vue'
import MobileNavBar from '@/components/MobileNavBar.vue'
import NotificationDropdown from '@/components/NotificationDropdown.vue'
import OfflinePage from '@/components/OfflinePage.vue'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'
import type { ComponentPublicInstance } from 'vue'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const { theme, toggleTheme } = useTheme()

// Sidebar visibility (Cmd+B)
const sidebarVisible = ref(true)

// Global search (Cmd+K)
const searchVisible = ref(false)
const searchQuery = ref('')
const searchInputRef = ref<ComponentPublicInstance | null>(null)

const allSearchItems = [
  { label: '首页看板', path: '/dashboard' },
  { label: '知识问答', path: '/chat' },
  { label: '知识库', path: '/knowledge' },
  { label: '模拟面试', path: '/interview' },
  { label: '面试历史', path: '/interview/history' },
  { label: '错题本', path: '/wrong' },
  { label: '间隔复习', path: '/review' },
  { label: '学习社区', path: '/community' },
  { label: '排行榜', path: '/community/leaderboard' },
  { label: '学习计划', path: '/plan' },
  { label: '数据分析', path: '/analytics' },
  { label: '后台管理', path: '/admin', adminOnly: true },
  { label: '账户设置', path: '/settings' }
]

const searchItems = computed(() =>
  allSearchItems.filter((item) => !item.adminOnly || authStore.user?.role === 'ADMIN')
)

const filteredSearchItems = computed(() => {
  if (!searchQuery.value) return searchItems.value
  const q = searchQuery.value.toLowerCase()
  return searchItems.value.filter(
    (item) => item.label.toLowerCase().includes(q) || item.path.toLowerCase().includes(q)
  )
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
  const isInput = (e.target as HTMLElement)?.tagName === 'INPUT'
    || (e.target as HTMLElement)?.tagName === 'TEXTAREA'
    || (e.target as HTMLElement)?.contentEditable === 'true'

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

const displayName = computed(() => authStore.user?.nickname || 'Visitor')
const initials = computed(() => displayName.value.slice(0, 1).toUpperCase())
const headerMeta = computed(() => {
  const meta = route.meta as { kicker?: string; title?: string; subtitle?: string }
  return {
    kicker: meta.kicker ?? 'ByteCoach',
    title: meta.title ?? 'Interview Studio',
    subtitle: meta.subtitle ?? '围绕学习闭环组织功能、状态和下一步动作。'
  }
})

const handleLogout = async () => {
  await authStore.logout()
}
</script>
