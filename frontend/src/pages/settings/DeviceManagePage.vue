<template>
  <div class="space-y-6">
    <section class="shell-section-card p-4 sm:p-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="text-sm text-secondary">当前 {{ devices.length }} 台</div>
        <div class="flex flex-wrap items-center gap-3">
          <el-button :loading="loading" type="primary" size="large" class="action-button" @click="loadDevices">
            刷新
          </el-button>
          <el-popconfirm
            v-if="devices.length > 1"
            title="确认撤销其他设备？"
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

    <section v-if="loading && devices.length === 0" class="shell-section-card p-6">
      <StateView variant="loading" :rows="4" />
    </section>

    <section v-else-if="devices.length === 0" class="shell-section-card p-6">
      <StateView
        icon="bell"
        :title="EMPTY_STATE_COPY.deviceManage.title"
        :description="EMPTY_STATE_COPY.deviceManage.description"
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
              :class="device.current ? 'bg-accent/10 text-accent' : 'bg-[var(--panel-muted)] text-secondary'"
            >
              <UiIcon :name="deviceIcon(device.deviceName)" />
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h4 class="font-semibold text-ink text-sm">{{ formatDeviceName(device.deviceName) }}</h4>
                <span
                  v-if="device.current"
                  class="rounded-full bg-accent/10 px-2 py-0.5 text-[10px] font-semibold text-accent"
                >
                  当前设备
                </span>
              </div>
              <p class="mt-1 text-xs text-secondary">
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

        <div class="mt-4 flex items-center justify-between text-xs uppercase tracking-[0.22em] text-tertiary">
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
import { EMPTY_STATE_COPY, ERROR_COPY } from '@/constants/productCopy'
import { fetchDevicesApi, revokeDeviceApi, revokeAllDevicesApi } from '@/api/auth'
import { localizeDeviceName } from '@/utils/device'
import type { LoginDeviceItem } from '@/types/api'
import { StateView, UiIcon } from '@/components/ui'

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
    ElMessage.error(ERROR_COPY.deviceLoadFailed)
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
    ElMessage.error(ERROR_COPY.deviceRevokeFailed)
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
    ElMessage.error(ERROR_COPY.deviceRevokeAllFailed)
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

const formatDeviceName = (name?: string) => localizeDeviceName(name)

const deviceIcon = (name?: string): 'phone' | 'tablet' | 'computer' => {
  if (!name) return 'computer'
  const lower = name.toLowerCase()
  if (lower.includes('iphone') || lower.includes('android') || lower.includes('mobile') || lower.includes('phone')) return 'phone'
  if (lower.includes('ipad') || lower.includes('tablet') || lower.includes('pad')) return 'tablet'
  return 'computer'
}

onMounted(loadDevices)
</script>
