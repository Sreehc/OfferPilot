<template>
  <div class="relative" ref="containerRef">
    <!-- Bell trigger -->
    <button
      type="button"
      class="shell-icon-button relative"
      title="通知"
      @click="toggleDropdown"
    >
      <svg class="h-5 w-5 text-slate-600 dark:text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
      </svg>
      <span
        v-if="unreadCount > 0"
        class="absolute -right-0.5 -top-0.5 flex h-4.5 min-w-[18px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white"
      >
        {{ unreadCount > 99 ? '99+' : unreadCount }}
      </span>
    </button>

    <!-- Dropdown panel -->
    <Transition name="dropdown">
      <div
        v-if="open"
        class="shell-popover absolute right-0 top-full z-50 mt-2 w-80 overflow-hidden sm:w-96"
      >
        <!-- Header -->
        <div class="flex items-center justify-between border-b border-[var(--bc-border-subtle)] px-4 py-3">
          <h3 class="text-sm font-semibold text-ink">通知</h3>
          <button
            v-if="unreadCount > 0"
            type="button"
            class="text-xs font-medium text-accent hover:underline"
            @click.stop="handleMarkAllRead"
          >
            全部已读
          </button>
        </div>

        <!-- Notification list -->
        <div class="max-h-80 overflow-y-auto">
          <div v-if="loading" class="flex items-center justify-center py-8">
            <div class="h-5 w-5 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
          </div>

          <EmptyState
            v-else-if="notifications.length === 0"
            icon="bell"
            title="暂无通知"
            description="新的通知会出现在这里"
            compact
          />

          <div v-else>
            <button
              v-for="n in notifications"
              :key="n.id"
              type="button"
              class="flex w-full gap-3 px-4 py-3 text-left transition hover:bg-slate-50 dark:hover:bg-slate-700/50"
              :class="{ 'bg-blue-50/50 dark:bg-blue-900/10': !n.isRead }"
              @click="handleClickNotification(n)"
            >
              <!-- Type icon -->
              <div
                class="mt-0.5 flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full text-xs font-semibold"
                :class="typeIconClass(n.type)"
              >
                {{ typeGlyph(n.type) }}
              </div>

              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2">
                  <span class="truncate text-sm font-medium text-ink">{{ n.title }}</span>
                  <span
                    v-if="!n.isRead"
                    class="h-1.5 w-1.5 flex-shrink-0 rounded-full bg-[var(--bc-accent)]"
                  ></span>
                </div>
                <p class="mt-0.5 line-clamp-2 text-xs text-slate-500 dark:text-slate-400">
                  {{ n.content }}
                </p>
                <span class="mt-1 block text-[11px] text-slate-400">{{ formatTime(n.createTime) }}</span>
              </div>
            </button>
          </div>
        </div>

        <!-- Footer -->
        <div v-if="notifications.length > 0" class="border-t border-[var(--bc-border-subtle)] px-4 py-2.5 text-center">
          <button
            type="button"
            class="text-xs font-medium text-accent hover:underline"
            @click="handleViewAll"
          >
            查看全部通知
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'
import {
  fetchNotificationsApi,
  fetchUnreadCountApi,
  markNotificationsReadApi,
  markAllNotificationsReadApi,
} from '@/api/notification'
import type { NotificationItem } from '@/types/api'

const router = useRouter()

const open = ref(false)
const loading = ref(false)
const notifications = ref<NotificationItem[]>([])
const unreadCount = ref(0)
const containerRef = ref<HTMLElement | null>(null)

let pollTimer: ReturnType<typeof setInterval> | null = null

const toggleDropdown = () => {
  open.value = !open.value
  if (open.value) {
    loadNotifications()
  }
}

const loadNotifications = async () => {
  loading.value = true
  try {
    const res = await fetchNotificationsApi(1, 20)
    notifications.value = res.data.records
  } catch {
    // silently ignore
  } finally {
    loading.value = false
  }
}

const loadUnreadCount = async () => {
  try {
    const res = await fetchUnreadCountApi()
    unreadCount.value = res.data.count
  } catch {
    // silently ignore
  }
}

const handleClickNotification = async (n: NotificationItem) => {
  // Mark as read
  if (!n.isRead) {
    try {
      await markNotificationsReadApi([n.id])
      n.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {
      // silently ignore
    }
  }

  // Navigate to link
  if (n.link) {
    open.value = false
    router.push(n.link)
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllNotificationsReadApi()
    notifications.value.forEach((n) => (n.isRead = true))
    unreadCount.value = 0
  } catch {
    // silently ignore
  }
}

const handleViewAll = () => {
  open.value = false
  // For now, just close. A full notification page can be added in V2.
}

const typeGlyph = (type: string) => {
  const map: Record<string, string> = {
    interview: '面',
    review: '复',
    community: '社',
    rank: '榜',
  }
  return map[type] || '通'
}

const typeIconClass = (type: string) => {
  const map: Record<string, string> = {
    interview: 'bg-[rgba(47,127,119,0.12)] text-[var(--bc-cyan)]',
    review: 'bg-[rgba(var(--bc-accent-rgb),0.12)] text-[var(--bc-accent)]',
    community: 'bg-[rgba(16,35,58,0.08)] text-[var(--bc-ink)]',
    rank: 'bg-[rgba(74,122,73,0.12)] text-[var(--bc-lime)]',
  }
  return map[type] || 'bg-[rgba(16,35,58,0.08)] text-[var(--bc-ink)]'
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return minutes + ' 分钟前'
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return hours + ' 小时前'
  const days = Math.floor(hours / 24)
  if (days < 7) return days + ' 天前'
  return date.toLocaleDateString('zh-CN')
}

// Close on outside click
const handleClickOutside = (e: MouseEvent) => {
  if (containerRef.value && !containerRef.value.contains(e.target as Node)) {
    open.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  loadUnreadCount()
  // Poll unread count every 60 seconds
  pollTimer = setInterval(loadUnreadCount, 60000)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
