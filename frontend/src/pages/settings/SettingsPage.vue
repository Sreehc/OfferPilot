<template>
  <div class="settings-cockpit space-y-6">
    <section class="grid gap-4 xl:grid-cols-[minmax(0,1.08fr)_minmax(320px,0.92fr)]">
      <article class="cockpit-panel p-5 sm:p-6">
        <div class="flex items-center gap-3">
          <span class="state-pulse" aria-hidden="true"></span>
          <p class="section-kicker">Security Console</p>
        </div>
        <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">账户安全控制台</h2>
        <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-300">
          设备、登录历史、两步验证和数据导出都收束到同一个安全工作台。目标不是让你浏览一组 tab，而是快速知道当前风险和下一步动作。
        </p>

        <div class="mt-6 grid gap-3 md:grid-cols-3">
          <article v-for="signal in securitySignals" :key="signal.label" class="data-slab p-4" :class="signal.toneClass">
            <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ signal.label }}</p>
            <p class="mt-3 text-xl font-semibold text-ink">{{ signal.title }}</p>
            <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ signal.detail }}</p>
          </article>
        </div>
      </article>

      <article class="cockpit-panel p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="section-kicker">Control Deck</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">当前工作台</h3>
          </div>
          <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ currentDeck.badge }}</span>
        </div>

        <div class="mt-5 rounded-[24px] border border-[var(--bc-line)] bg-white/38 p-5 dark:bg-white/5">
          <p class="text-lg font-semibold text-ink">{{ currentDeck.title }}</p>
          <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">{{ currentDeck.description }}</p>
        </div>

        <div class="mt-5 space-y-3">
          <article v-for="lane in deckLanes" :key="lane.label" class="deck-lane">
            <div class="flex items-center justify-between gap-3">
              <div class="flex items-center gap-3">
                <span class="inline-flex h-2.5 w-2.5 rounded-full" :class="lane.dotClass"></span>
                <div>
                  <p class="text-sm font-semibold text-ink">{{ lane.label }}</p>
                  <p class="text-xs text-slate-500 dark:text-slate-400">{{ lane.detail }}</p>
                </div>
              </div>
              <span class="font-mono text-sm font-semibold text-ink">{{ lane.value }}</span>
            </div>
          </article>
        </div>
      </article>
    </section>

    <section class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="section-kicker">Workspace Tabs</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">安全工作区</h3>
        </div>
        <div class="settings-tab-hint">
          当前选项：<span class="font-semibold text-ink">{{ currentDeck.title }}</span>
        </div>
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
    label: 'Devices',
    title: '设备可追踪',
    detail: '随时撤销可疑设备，保持登录面最小化。',
    toneClass: 'settings-slab-cyan',
  },
  {
    label: '2FA',
    title: '二次校验',
    detail: '关键账号建议启用两步验证，降低密码泄露风险。',
    toneClass: 'settings-slab-amber',
  },
  {
    label: 'Export',
    title: '数据可带走',
    detail: '学习数据始终可以导出，避免锁在产品里。',
    toneClass: 'settings-slab-lime',
  },
]

const deckMap = {
  devices: {
    title: '设备管理',
    description: '查看当前登录设备，识别异常终端，并快速撤销不再可信的访问。',
    badge: 'Devices',
  },
  loginHistory: {
    title: '登录历史',
    description: '检查最近登录轨迹，判断是否存在异常地点、时间或设备。',
    badge: 'Logs',
  },
  twoFactor: {
    title: '两步验证',
    description: '开启或关闭 2FA，让登录流程在账号密码之外再加一道安全校验。',
    badge: '2FA',
  },
  dataExport: {
    title: '数据导出',
    description: '导出你的学习数据，保持长期可迁移和可归档。',
    badge: 'Export',
  },
} as const

const currentDeck = computed(() => deckMap[activeTab.value as keyof typeof deckMap] ?? deckMap.devices)

const deckLanes = computed(() => [
  {
    label: '当前模块',
    value: currentDeck.value.title,
    detail: '你正在查看的安全工作台。',
    dotClass: 'bg-[var(--bc-accent)]',
  },
  {
    label: '风险类型',
    value: activeTab.value === 'devices' || activeTab.value === 'loginHistory' ? 'Access' : activeTab.value === 'twoFactor' ? 'Identity' : 'Control',
    detail: '当前模块主要应对的安全维度。',
    dotClass: activeTab.value === 'twoFactor' ? 'bg-[var(--bc-cyan)]' : 'bg-[var(--bc-amber)]',
  },
  {
    label: '动作导向',
    value: activeTab.value === 'dataExport' ? 'Archive' : 'Review',
    detail: '是检查风险还是执行导出。',
    dotClass: activeTab.value === 'dataExport' ? 'bg-[var(--bc-lime)]' : 'bg-[var(--bc-coral)]',
  },
])
</script>

<style scoped>
.deck-lane {
  border-radius: 20px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.34);
  padding: 14px 16px;
}

.dark .deck-lane {
  background: rgba(255, 255, 255, 0.05);
}

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
