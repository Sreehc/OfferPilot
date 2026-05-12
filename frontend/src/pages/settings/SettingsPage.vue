<template>
  <div class="settings-cockpit space-y-6">
    <AppShellHeader />

    <section class="shell-section-card p-5 sm:p-6">
      <section class="settings-tab-shell">
        <el-tabs v-model="activeTab" class="settings-tabs">
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
        </el-tabs>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppShellHeader from '@/components/AppShellHeader.vue'
import DeviceManagePage from './DeviceManagePage.vue'
import LoginHistoryTab from './LoginHistoryTab.vue'
import TwoFactorTab from './TwoFactorTab.vue'
import DataExportTab from './DataExportTab.vue'

const activeTab = ref('twoFactor')
</script>

<style scoped>
.settings-tab-shell {
  border-radius: 24px;
  background: transparent;
  padding: 4px 0 0;
}

.dark .settings-tab-shell {
  background: transparent;
}

:deep(.settings-tabs > .el-tabs__header) {
  margin-bottom: 0;
  padding: 0 0 16px;
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
  padding: 12px 0 0;
}
</style>
