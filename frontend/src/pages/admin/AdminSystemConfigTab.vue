<template>
  <div class="space-y-4">
    <section class="shell-section-card p-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">查看并更新系统配置</h3>
          <p class="mt-2 text-sm text-secondary">直接核对当前值、填写变更原因，并回看最近修改记录。</p>
        </div>
        <el-button :loading="loading" size="large" class="hard-button-secondary" @click="loadConfigs">刷新配置</el-button>
      </div>
    </section>

    <section v-for="group in groupedConfigs" :key="group.key" class="shell-section-card p-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h4 class="text-xl font-semibold tracking-[-0.03em] text-ink">{{ group.title }}</h4>
          <p class="mt-2 text-sm text-secondary">按组查看并更新当前配置。</p>
        </div>
        <div class="text-sm text-secondary">{{ group.items.length }} 项配置</div>
      </div>

      <div class="mt-5 grid gap-4">
        <article v-for="item in group.items" :key="item.configKey" class="surface-card p-4">
          <div class="config-row">
            <div class="config-meta">
              <div class="flex flex-wrap items-center gap-2">
                <h5 class="text-lg font-semibold text-ink">{{ item.displayName }}</h5>
                <el-tag size="small" effect="plain">{{ item.configKey }}</el-tag>
              </div>
              <p class="mt-2 text-sm leading-6 text-secondary">{{ item.description }}</p>
              <p class="mt-3 text-xs leading-6 text-tertiary">默认值：{{ item.runtimeDefault || '空' }}</p>
              <p v-if="item.configHistory?.[0]" class="mt-2 text-xs leading-6 text-tertiary">
                最近修改：{{ formatTime(item.configHistory[0].createTime) }} · {{ item.configHistory[0].changeReason || '后台更新' }}
              </p>
            </div>

            <div class="config-editor">
              <div class="flex flex-wrap items-center justify-between gap-3">
                <span class="text-sm font-medium text-ink">当前状态</span>
                <el-switch v-model="item.enabled" active-text="启用" inactive-text="停用" />
              </div>
              <el-input
                v-if="item.valueType === 'textarea'"
                v-model="item.configValue"
                type="textarea"
                :rows="7"
                resize="vertical"
                placeholder="输入新的配置值"
              />
              <el-input v-else v-model="item.configValue" size="large" placeholder="输入新的配置值" />
              <el-input
                v-model="changeReasons[item.configKey]"
                size="large"
                maxlength="120"
                show-word-limit
                placeholder="填写本次更新原因，方便后续回看"
              />
              <div class="flex flex-wrap justify-end gap-3">
                <el-button size="large" class="hard-button-secondary" @click="resetItem(item)">回到默认</el-button>
                <el-button size="large" class="hard-button-secondary" @click="toggleHistory(item.configKey)">
                  {{ expandedHistoryKey === item.configKey ? '收起记录' : '查看记录' }}
                </el-button>
                <el-button
                  :loading="savingKey === item.configKey"
                  size="large"
                  class="action-button"
                  @click="saveItem(item)"
                >
                  更新配置
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="expandedHistoryKey === item.configKey" class="mt-4 rounded-2xl bg-[var(--panel-muted)] p-4">
            <div class="text-sm font-semibold text-ink">最近变更</div>
            <div v-if="item.configHistory?.length" class="mt-3 space-y-3">
              <article v-for="history in item.configHistory" :key="history.id" class="rounded-2xl bg-white/70 px-4 py-3 dark:bg-slate-900/40">
                <div class="flex flex-wrap items-center justify-between gap-3">
                  <div class="text-sm font-medium text-ink">{{ history.changeReason || '后台更新' }}</div>
                  <div class="text-xs text-secondary">{{ formatTime(history.createTime) }}</div>
                </div>
                <div class="mt-2 text-xs leading-6 text-secondary">
                  状态：{{ history.oldEnabled ? '启用' : '停用' }} -> {{ history.newEnabled ? '启用' : '停用' }}
                </div>
                <div class="mt-1 text-xs leading-6 text-secondary">
                  值：{{ shortValue(history.oldValue) }} -> {{ shortValue(history.newValue) }}
                </div>
              </article>
            </div>
            <div v-else class="mt-3 text-sm text-secondary">还没有变更记录。</div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  fetchAdminSystemConfigsApi,
  updateAdminSystemConfigApi,
  type AdminSystemConfigItem
} from '@/api/admin'

const loading = ref(false)
const savingKey = ref('')
const expandedHistoryKey = ref('')
const configs = ref<AdminSystemConfigItem[]>([])
const changeReasons = reactive<Record<string, string>>({})

const groupMeta: Record<string, { title: string }> = {
  llm: { title: '问答与评分模型' },
  embedding: { title: '检索与向量配置' },
  prompt: { title: '提示词模板' }
}

const groupedConfigs = computed(() => {
  return Object.entries(groupMeta)
    .map(([key, meta]) => ({
      key,
      title: meta.title,
      items: configs.value.filter((item) => item.configGroup === key)
    }))
    .filter((group) => group.items.length)
})

const loadConfigs = async () => {
  loading.value = true
  try {
    const response = await fetchAdminSystemConfigsApi()
    configs.value = response.data.map((item) => ({ ...item, configHistory: item.configHistory || [] }))
    for (const item of configs.value) {
      changeReasons[item.configKey] ||= ''
    }
  } catch {
    ElMessage.error('系统配置加载失败')
  } finally {
    loading.value = false
  }
}

const resetItem = (item: AdminSystemConfigItem) => {
  item.configValue = item.runtimeDefault || ''
  item.enabled = true
  changeReasons[item.configKey] = '回到默认值'
}

const toggleHistory = (configKey: string) => {
  expandedHistoryKey.value = expandedHistoryKey.value === configKey ? '' : configKey
}

const saveItem = async (item: AdminSystemConfigItem) => {
  savingKey.value = item.configKey
  try {
    const response = await updateAdminSystemConfigApi(item.configKey, {
      configValue: item.configValue,
      enabled: item.enabled,
      changeReason: changeReasons[item.configKey] || '后台更新'
    })
    const index = configs.value.findIndex((entry) => entry.configKey === item.configKey)
    if (index >= 0) {
      configs.value[index] = { ...response.data }
    }
    changeReasons[item.configKey] = ''
    expandedHistoryKey.value = item.configKey
    ElMessage.success('配置已更新')
  } catch {
    ElMessage.error('配置保存失败')
  } finally {
    savingKey.value = ''
  }
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

const shortValue = (value?: string) => {
  if (!value) return '空'
  return value.length > 48 ? `${value.slice(0, 48)}...` : value
}

onMounted(() => {
  void loadConfigs()
})
</script>

<style scoped>
.config-row {
  display: grid;
  gap: 20px;
}

.config-meta {
  min-width: 0;
}

.config-editor {
  display: grid;
  gap: 12px;
}

@media (min-width: 1200px) {
  .config-row {
    grid-template-columns: minmax(0, 340px) minmax(0, 1fr);
    align-items: start;
  }
}
</style>
