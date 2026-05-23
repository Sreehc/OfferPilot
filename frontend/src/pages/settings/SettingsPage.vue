<template>
  <div class="settings-cockpit space-y-6">
    <section class="settings-hero shell-section-card">
      <div class="settings-hero__copy">
        <p class="section-kicker">
          账户设置
        </p>
        <h1 class="settings-hero__title">
          管理你的账号、安全和数据
        </h1>
        <p class="settings-hero__description">
          先看当前账号状态，再进入对应任务。常用的安全操作和登录记录都集中在这里。
        </p>
      </div>

      <div class="settings-hero__status">
        <article class="settings-status-card">
          <span class="settings-status-card__label">邮箱状态</span>
          <strong class="settings-status-card__value">{{ emailStatusLabel }}</strong>
          <p class="settings-status-card__hint">{{ emailStatusHint }}</p>
        </article>
        <article class="settings-status-card">
          <span class="settings-status-card__label">两步验证</span>
          <strong class="settings-status-card__value">{{ twoFactorSummary.label }}</strong>
          <p class="settings-status-card__hint">{{ twoFactorSummary.hint }}</p>
        </article>
        <article class="settings-status-card">
          <span class="settings-status-card__label">当前登录</span>
          <strong class="settings-status-card__value">{{ loginSummaryLabel }}</strong>
          <p class="settings-status-card__hint">{{ loginSummaryHint }}</p>
        </article>
      </div>
    </section>

    <section class="settings-task-grid">
      <button
        v-for="item in taskItems"
        :key="item.name"
        type="button"
        class="settings-task-card"
        :class="{ 'settings-task-card--active': activeTab === item.name }"
        @click="activeTab = item.name"
      >
        <div class="settings-task-card__head">
          <span class="settings-task-card__icon" aria-hidden="true">{{ item.icon }}</span>
          <span class="settings-task-card__title">{{ item.label }}</span>
        </div>
        <p class="settings-task-card__description">{{ item.description }}</p>
        <span class="settings-task-card__meta">{{ item.meta }}</span>
      </button>
    </section>

    <section class="shell-section-card p-5 sm:p-6 settings-workspace">
      <div class="settings-workspace__head">
        <div>
          <p class="section-kicker">当前任务</p>
          <h2 class="settings-workspace__title">{{ activeTask.label }}</h2>
          <p class="settings-workspace__description">{{ activeTask.description }}</p>
        </div>
        <span class="detail-pill">{{ activeTask.meta }}</span>
      </div>

      <div class="settings-workspace__body">
        <div v-if="activeTab === 'account'" class="settings-tab-content">
          <AccountProfileTab />
        </div>
        <div v-else-if="activeTab === 'twoFactor'" class="settings-tab-content">
          <TwoFactorTab />
        </div>
        <div v-else-if="activeTab === 'dataExport'" class="settings-tab-content">
          <DataExportTab />
        </div>
        <div v-else-if="activeTab === 'devices'" class="settings-tab-content">
          <DeviceManagePage />
        </div>
        <div v-else class="settings-tab-content">
          <LoginHistoryTab />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchTwoFactorStatusApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import type { TwoFactorStatus } from '@/types/api'
import AccountProfileTab from './AccountProfileTab.vue'
import DataExportTab from './DataExportTab.vue'
import DeviceManagePage from './DeviceManagePage.vue'
import LoginHistoryTab from './LoginHistoryTab.vue'
import TwoFactorTab from './TwoFactorTab.vue'

const authStore = useAuthStore()
const activeTab = ref('account')
const twoFactorStatus = ref<TwoFactorStatus | null>(null)

const emailStatusLabel = computed(() => {
  if (!authStore.user?.email) return '未填写邮箱'
  return authStore.user.emailVerified ? '已验证' : '待验证'
})

const emailStatusHint = computed(() => {
  if (!authStore.user?.email) return '补一个常用邮箱，方便找回密码和接收验证码。'
  return authStore.user.emailVerified
    ? '这个邮箱已经可以用于恢复账号和接收安全提醒。'
    : '建议先完成邮箱验证，后续找回账号会更顺。'
})

const twoFactorSummary = computed(() => {
  if (twoFactorStatus.value?.enabled) {
    return {
      label: '已启用',
      hint: `剩余恢复码 ${twoFactorStatus.value.recoveryCodesRemaining ?? '-'} 个`
    }
  }

  return {
    label: '未启用',
    hint: '建议为常用账号补上两步验证。'
  }
})

const loginSummaryLabel = computed(() => authStore.user?.lastLoginTime ? '最近有登录' : '仅当前设备')
const loginSummaryHint = computed(() => authStore.user?.lastLoginTime
  ? `上次登录 ${formatDateTime(authStore.user.lastLoginTime)}`
  : '你可以在这里查看登录设备和历史记录。')

const taskItems = computed(() => [
  {
    name: 'account',
    label: '更新账号资料',
    description: '头像、邮箱验证和基础资料都在这里维护。',
    meta: authStore.user?.emailVerified ? '邮箱已验证' : '建议先完成邮箱验证',
    icon: '01'
  },
  {
    name: 'twoFactor',
    label: '保护登录安全',
    description: '启用或关闭两步验证，减少账号被盗风险。',
    meta: twoFactorStatus.value?.enabled ? '已启用两步验证' : '现在可启用两步验证',
    icon: '02'
  },
  {
    name: 'devices',
    label: '管理登录设备',
    description: '检查当前登录设备，必要时撤销其他设备。',
    meta: '快速处理陌生设备',
    icon: '03'
  },
  {
    name: 'loginHistory',
    label: '查看登录记录',
    description: '回看登录时间、地点和失败原因，排查异常登录。',
    meta: '适合排查安全问题',
    icon: '04'
  },
  {
    name: 'dataExport',
    label: '导出个人数据',
    description: '下载面试记录、错题和复习数据，留存到本地。',
    meta: '按需导出训练数据',
    icon: '05'
  }
])

const activeTask = computed(() => taskItems.value.find((item) => item.name === activeTab.value) ?? taskItems.value[0]!)

const loadTwoFactorStatus = async () => {
  try {
    const response = await fetchTwoFactorStatusApi()
    twoFactorStatus.value = response.data
  } catch {
    twoFactorStatus.value = null
  }
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

onMounted(loadTwoFactorStatus)
</script>

<style scoped>
.settings-hero {
  display: grid;
  gap: 1.25rem;
  padding: 1.5rem;
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.14), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(244, 247, 255, 0.9));
}

.settings-hero__copy {
  max-width: 40rem;
}

.settings-hero__title {
  margin-top: 0.75rem;
  font-size: clamp(1.9rem, 4vw, 2.65rem);
  line-height: 1.05;
  font-weight: 700;
  letter-spacing: -0.04em;
  color: var(--bc-ink);
}

.settings-hero__description {
  margin-top: 0.9rem;
  max-width: 34rem;
  font-size: 0.95rem;
  line-height: 1.8;
  color: var(--bc-ink-secondary);
}

.settings-hero__status {
  display: grid;
  gap: 0.85rem;
}

.settings-status-card {
  border-radius: 1.35rem;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.12);
  background: rgba(255, 255, 255, 0.8);
  padding: 1rem 1.05rem;
  box-shadow: 0 14px 36px rgba(var(--bc-ink-rgb), 0.05);
}

.settings-status-card__label {
  display: inline-flex;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--bc-ink-tertiary);
}

.settings-status-card__value {
  display: block;
  margin-top: 0.55rem;
  font-size: 1.1rem;
  color: var(--bc-ink);
}

.settings-status-card__hint {
  margin-top: 0.35rem;
  font-size: 0.82rem;
  line-height: 1.65;
  color: var(--bc-ink-secondary);
}

.settings-task-grid {
  display: grid;
  gap: 0.9rem;
}

.settings-task-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 0.75rem;
  border-radius: 1.4rem;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.1);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 249, 255, 0.94));
  padding: 1rem 1.05rem;
  text-align: left;
  transition:
    transform var(--motion-base) var(--ease-hard),
    border-color var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard);
}

.settings-task-card:hover,
.settings-task-card:focus-visible {
  transform: translateY(-2px);
  border-color: rgba(var(--bc-accent-rgb), 0.24);
  box-shadow: 0 14px 30px rgba(var(--bc-accent-rgb), 0.12);
}

.settings-task-card--active {
  border-color: rgba(var(--bc-accent-rgb), 0.28);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.12), rgba(255, 255, 255, 0.96));
  box-shadow: 0 18px 40px rgba(var(--bc-accent-rgb), 0.14);
}

.settings-task-card__head {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.settings-task-card__icon {
  display: inline-flex;
  min-width: 2.3rem;
  height: 2.3rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(var(--bc-accent-rgb), 0.12);
  color: var(--bc-accent);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.settings-task-card__title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.settings-task-card__description {
  font-size: 0.88rem;
  line-height: 1.7;
  color: var(--bc-ink-secondary);
}

.settings-task-card__meta {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--bc-accent);
}

.settings-workspace {
  display: grid;
  gap: 1rem;
}

.settings-workspace__head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem 1rem;
}

.settings-workspace__title {
  margin-top: 0.7rem;
  font-size: 1.55rem;
  line-height: 1.1;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--bc-ink);
}

.settings-workspace__description {
  margin-top: 0.7rem;
  max-width: 40rem;
  font-size: 0.92rem;
  line-height: 1.8;
  color: var(--bc-ink-secondary);
}

.settings-workspace__body {
  border-top: 1px solid var(--bc-border-subtle);
  padding-top: 0.2rem;
}

.settings-tab-content {
  min-height: 420px;
  padding: 0.75rem 0 0;
}

@media (min-width: 768px) {
  .settings-hero {
    grid-template-columns: minmax(0, 1.5fr) minmax(18rem, 1fr);
    align-items: stretch;
    padding: 1.75rem;
  }

  .settings-hero__status {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .settings-task-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1200px) {
  .settings-task-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}
</style>
