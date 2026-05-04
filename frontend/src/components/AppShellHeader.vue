<template>
  <header class="paper-panel flex flex-col gap-4 p-5 lg:flex-row lg:items-center lg:justify-between">
    <div class="min-w-0">
      <p class="section-kicker">{{ kicker }}</p>
      <h2 class="page-title mt-3 text-xl sm:text-2xl lg:text-3xl">{{ title }}</h2>
      <p class="page-subtitle mt-3 hidden sm:block">{{ subtitle }}</p>
    </div>

    <div class="flex flex-col gap-3 self-start sm:flex-row sm:items-center">
      <slot name="actions" />

      <div class="surface-card hidden sm:flex items-center gap-3 px-3 py-2">
        <div
          class="group relative flex size-10 cursor-pointer items-center justify-center overflow-hidden rounded-full bg-accent text-sm font-semibold text-white"
          @click="triggerAvatarUpload"
        >
          <img v-if="avatarUrl" :src="avatarUrl" alt="avatar" class="h-full w-full object-cover" />
          <span v-else>{{ initials }}</span>
          <div class="absolute inset-0 flex items-center justify-center bg-black/40 opacity-0 transition group-hover:opacity-100">
            <svg class="h-4 w-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
          <input
            ref="fileInputRef"
            type="file"
            accept="image/*"
            class="hidden"
            @change="handleAvatarChange"
          />
        </div>
        <div>
          <div class="text-sm font-semibold">{{ name }}</div>
          <div class="text-xs uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">{{ role }}</div>
          <RouterLink
            to="/settings"
            class="mt-1 inline-block text-[11px] text-accent hover:underline"
          >
            账户设置
          </RouterLink>
        </div>
      </div>

      <button
        type="button"
        class="hard-button-secondary hidden sm:inline-flex"
        @click="$emit('logout')"
      >
        退出登录
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import { uploadAvatarApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

defineEmits<{
  logout: []
}>()

defineProps<{
  kicker: string
  title: string
  subtitle: string
  name: string
  role: string
  initials: string
}>()

const fileInputRef = ref<HTMLInputElement | null>(null)
const avatarUrl = ref<string | null>(authStore.user?.avatar || null)

const triggerAvatarUpload = () => {
  fileInputRef.value?.click()
}

const handleAvatarChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return
  }

  try {
    const response = await uploadAvatarApi(file)
    avatarUrl.value = response.data
    // Update auth store user info
    if (authStore.user) {
      authStore.user.avatar = response.data
      authStore.persistUser()
    }
    ElMessage.success('头像已更新')
  } catch {
    ElMessage.error('头像上传失败')
  }

  // Reset input
  input.value = ''
}
</script>
