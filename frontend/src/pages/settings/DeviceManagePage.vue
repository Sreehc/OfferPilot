<template>
  <div class="space-y-6">
    <!-- Header -->
    <section class="paper-panel p-4 sm:p-6">
      <p class="section-kicker">账户安全</p>
      <h3 class="mt-4 text-xl sm:text-3xl font-semibold tracking-[-0.03em] text-ink">已登录设备</h3>
      <p class="mt-4 text-sm leading-7 text-slate-600 dark:text-slate-300">
        查看当前已登录的所有设备。如发现可疑设备，可立即撤销其登录状态。
      </p>
    </section>

    <!-- Actions -->
    <section class="paper-panel p-4 sm:p-6">
      <div class="flex items-center justify-between">
        <div class="text-sm text-slate-500 dark:text-slate-400">
          共 <span class="font-semibold text-ink">{{ devices.length }}</span> 台设备登录中
        </div>
        <div class="flex gap-3">
          <el-button :loading="loading" type="primary" size="large" class="action-button" @click="loadDevices">
            刷新
          </el-button>
          <el-popconfirm
            v-if="devices.length > 1"
            title="确认撤销除当前设备外的所有设备？被撤销的设备将需要重新登录。"
            confirm-button-text="确认撤销"
            cancel-button-text="取消"
            @confirm="handleRevokeAll"
          >
            <template #reference>
              <el-button :loading="revokingAll" size="large" type="danger" plain>
                撤销其他设备
              </el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </section>

    <!-- Device list -->
    <section v-if="loading && devices.length === 0" class="paper-panel p-6">
      <div class="flex items-center justify-center py-8">
        <el-icon class="is-loading text-slate-400" :size="24"><i class="el-icon-loading" /></el-icon>
        <span class="ml-3 text-sm text-slate-400">加载中...</span>
      </div>
    </section>

    <section v-else-if="devices.length === 0" class="paper-panel p-6">
      <EmptyState
        icon="shield"
        title="暂无已登录设备"
        description="登录后设备会自动出现在此列表中。"
      />
    </section>

    <div v-else class="grid gap-4 md:grid-cols-2">
      <article
        v-for="device in devices"
        :key="device.id"
        class="metric-card"
        :class="device.current ? 'ring-2 ring-accent/30' : ''"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="flex items-center gap-3">
            <div
              class="flex h-10 w-10 items-center justify-center rounded-lg"
              :class="device.current ? 'bg-accent/10 text-accent' : 'bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400'"
            >
              <!-- Phone icon -->
              <svg v-if="deviceIcon(device.deviceName) === 'phone'" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 1.5H8.25A2.25 2.25 0 006 3.75v16.5a2.25 2.25 0 002.25 2.25h7.5A2.25 2.25 0 0018 20.25V3.75a2.25 2.25 0 00-2.25-2.25H13.5m-3 0V3h3V1.5m-3 0h3m-3 18.75h3" />
              </svg>
              <!-- Tablet icon -->
              <svg v-else-if="deviceIcon(device.deviceName) === 'tablet'" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5h3m-6.75 2.25h10.5a2.25 2.25 0 002.25-2.25v-15a2.25 2.25 0 00-2.25-2.25H6.75A2.25 2.25 0 004.5 4.5v15a2.25 2.25 0 002.25 2.25z" />
              </svg>
              <!-- Computer icon (default) -->
              <svg v-else class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 17.25v1.007a3 3 0 01-.879 2.122L7.5 21h9l-.621-.621A3 3 0 0115 18.257V17.25m6-12V15a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 15V5.25A2.25 2.25 0 015.25 3h13.5A2.25 2.25 0 0121 5.25z" />
              </svg>
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h4 class="font-semibold text-ink text-sm">{{ device.deviceName || '未知设备' }}</h4>
                <span
                  v-if="device.current"
                  class="rounded-full bg-accent/10 px-2 py-0.5 text-[10px] font-semibold text-accent"
                >
                  当前设备
                </span>
              </div>
              <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
                {{ device.ip || '未知 IP' }}
                <span v-if="device.city"> · {{ device.city }}</span>
              </p>
            </div>
          </div>

          <el-button
            v-if="!device.current"
            size="small"
            type="danger"
            plain
            :loading="revokingId === device.id"
            @click="confirmRevoke(device.id)"
          >
            撤销
          </el-button>
        </div>

        <div class="mt-4 flex items-center justify-between text-xs uppercase tracking-[0.22em] text-slate-400 dark:text-slate-500">
          <span>最后活跃 {{ formatTime(device.lastActiveTime) }}</span>
          <span>登录于 {{ formatTime(device.createTime) }}</span>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { fetchDevicesApi, revokeDeviceApi, revokeAllDevicesApi } from '@/api/auth'
import type { LoginDeviceItem } from '@/types/api'

const devices = ref<LoginDeviceItem[]>([])
const loading = ref(false)
const revokingId = ref<number | null>(null)
const revokingAll = ref(false)

const loadDevices = async () => {
  loading.value = true
  try {
    const response = await fetchDevicesApi()
    devices.value = response.data
  } catch {
    ElMessage.error('设备列表加载失败')
  } finally {
    loading.value = false
  }
}

const confirmRevoke = async (deviceId: number) => {
  try {
    await ElMessageBox.confirm('确认撤销此设备？该设备将需要重新登录。', '撤销设备', {
      confirmButtonText: '撤销',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await handleRevoke(deviceId)
  } catch {
    // User cancelled
  }
}

const handleRevoke = async (deviceId: number) => {
  revokingId.value = deviceId
  try {
    await revokeDeviceApi(deviceId)
    ElMessage.success('设备已撤销')
    await loadDevices()
  } catch {
    ElMessage.error('撤销失败')
  } finally {
    revokingId.value = null
  }
}

const handleRevokeAll = async () => {
  revokingAll.value = true
  try {
    await revokeAllDevicesApi()
    ElMessage.success('其他设备已全部撤销')
    await loadDevices()
  } catch {
    ElMessage.error('撤销失败')
  } finally {
    revokingAll.value = false
  }
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

const deviceIcon = (name?: string): 'phone' | 'tablet' | 'computer' => {
  if (!name) return 'computer'
  const lower = name.toLowerCase()
  if (lower.includes('iphone') || lower.includes('android') || lower.includes('mobile') || lower.includes('phone')) return 'phone'
  if (lower.includes('ipad') || lower.includes('tablet') || lower.includes('pad')) return 'tablet'
  return 'computer'
}

onMounted(loadDevices)
</script>
