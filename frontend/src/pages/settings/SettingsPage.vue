<template>
  <div class="space-y-6">
    <section class="paper-panel p-6">
      <p class="section-kicker">账户安全</p>
      <h2 class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink">账户设置</h2>
      <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-600 dark:text-slate-300">
        管理已登录设备、查看登录历史、配置两步验证、导出学习数据，保障账户安全。
      </p>
    </section>

    <section class="paper-panel p-6">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="设备管理" name="devices">
          <DeviceManagePage />
        </el-tab-pane>
        <el-tab-pane label="登录历史" name="loginHistory">
          <LoginHistoryTab />
        </el-tab-pane>
        <el-tab-pane label="两步验证" name="twoFactor">
          <TwoFactorTab />
        </el-tab-pane>
        <el-tab-pane label="数据导出" name="dataExport">
          <DataExportTab />
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import DeviceManagePage from './DeviceManagePage.vue'
import LoginHistoryTab from './LoginHistoryTab.vue'
import TwoFactorTab from './TwoFactorTab.vue'
import DataExportTab from './DataExportTab.vue'

const activeTab = ref('devices')
</script>
