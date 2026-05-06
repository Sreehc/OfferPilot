<template>
  <div class="settings-cockpit space-y-6">
    <section class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div class="max-w-3xl">
          <div class="flex items-center gap-3">
            <span class="state-pulse" aria-hidden="true"></span>
            <p class="section-kicker">账号与安全</p>
          </div>
          <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">管理账号与安全</h2>
          <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
            查看设备、登录历史、两步验证和数据导出。
          </p>
        </div>
        <div class="settings-tab-hint rounded-full border border-[var(--bc-line)] px-3 py-2">
          当前标签：<span class="font-semibold text-ink">{{ currentDeck.title }}</span>
        </div>
      </div>

      <div class="mt-6 grid gap-3 md:grid-cols-3">
        <article v-for="signal in securitySignals" :key="signal.label" class="data-slab p-4" :class="signal.toneClass">
          <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ signal.label }}</p>
          <p class="mt-3 text-xl font-semibold text-ink">{{ signal.title }}</p>
          <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ signal.detail }}</p>
        </article>
      </div>

      <section class="settings-tab-shell mt-6">
        <el-tabs v-model="activeTab" class="settings-tabs">
          <el-tab-pane label="设备管理" name="devices">
            <div class="settings-tab-content">
              <DeviceManagePage />
            </div>
          </el-tab-pane>
          <el-tab-pane label="登录历史" name="loginHistory">
            <div class="settings-tab-content">
              <LoginHistoryTab />
            </div>
          </el-tab-pane>
          <el-tab-pane label="两步验证" name="twoFactor">
            <div class="settings-tab-content">
              <TwoFactorTab />
            </div>
          </el-tab-pane>
          <el-tab-pane label="数据导出" name="dataExport">
            <div class="settings-tab-content">
              <DataExportTab />
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import DeviceManagePage from './DeviceManagePage.vue'
import LoginHistoryTab from './LoginHistoryTab.vue'
import TwoFactorTab from './TwoFactorTab.vue'
import DataExportTab from './DataExportTab.vue'

const activeTab = ref('devices')

const securitySignals = [
  {
    label: '设备',
    title: '设备可追踪',
    detail: '随时撤销可疑设备，保持登录面最小化。',
    toneClass: 'settings-slab-cyan',
  },
  {
    label: '验证',
    title: '二次校验',
    detail: '关键账号建议启用两步验证，降低密码泄露风险。',
    toneClass: 'settings-slab-amber',
  },
  {
    label: '导出',
    title: '数据可带走',
    detail: '学习数据始终可以导出，避免锁在产品里。',
    toneClass: 'settings-slab-lime',
  },
]

const deckMap = {
  devices: {
    title: '设备管理',
  },
  loginHistory: {
    title: '登录历史',
  },
  twoFactor: {
    title: '两步验证',
  },
  dataExport: {
    title: '数据导出',
  },
} as const

const currentDeck = computed(() => deckMap[activeTab.value as keyof typeof deckMap] ?? deckMap.devices)
</script>

<style scoped>
.settings-slab-cyan {
  border-left-color: var(--bc-cyan);
}

.settings-slab-amber {
  border-left-color: var(--bc-amber);
}

.settings-slab-lime {
  border-left-color: var(--bc-lime);
}

.settings-tab-hint {
  font-size: 12px;
  color: var(--bc-ink-secondary);
}

.settings-tab-shell {
  border-radius: 28px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.26);
  padding: 12px;
}

.dark .settings-tab-shell {
  background: rgba(255, 255, 255, 0.04);
}

:deep(.settings-tabs > .el-tabs__header) {
  margin-bottom: 0;
  padding: 0 8px;
}

:deep(.settings-tabs .el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.settings-tabs .el-tabs__item) {
  min-height: 44px;
  color: var(--bc-ink-secondary);
  font-weight: 600;
}

:deep(.settings-tabs .el-tabs__item.is-active) {
  color: var(--bc-ink);
}

:deep(.settings-tabs .el-tabs__active-bar) {
  background: var(--bc-accent);
}

.settings-tab-content {
  min-height: 420px;
  padding: 20px 8px 8px;
}
</style>
