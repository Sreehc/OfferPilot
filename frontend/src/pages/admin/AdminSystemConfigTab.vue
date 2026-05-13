<template>
  <div class="space-y-4">
    <section class="shell-section-card p-5">
      <p class="section-kicker">系统配置</p>
      <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">模型参数与提示词模板</h3>
      <p class="mt-3 max-w-3xl text-sm leading-7 text-secondary">
        这里可以覆盖默认的模型名称、网关地址、超时秒数以及核心提示词模板。首期重点是可查看、可调整、可回退。
      </p>
    </section>

    <section v-for="group in groupedConfigs" :key="group.key" class="shell-section-card p-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p class="section-kicker">{{ group.label }}</p>
          <h4 class="mt-3 text-xl font-semibold tracking-[-0.03em] text-ink">{{ group.title }}</h4>
        </div>
        <el-button :loading="loading" size="large" class="hard-button-secondary" @click="loadConfigs">刷新配置</el-button>
      </div>

      <div class="mt-5 grid gap-4">
        <article v-for="item in group.items" :key="item.configKey" class="surface-card p-4">
          <div class="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
            <div class="min-w-0 xl:max-w-[340px]">
              <div class="flex flex-wrap items-center gap-2">
                <h5 class="text-lg font-semibold text-ink">{{ item.displayName }}</h5>
                <el-tag size="small" effect="plain">{{ item.configKey }}</el-tag>
              </div>
              <p class="mt-2 text-sm leading-6 text-secondary">{{ item.description }}</p>
              <p class="mt-3 text-xs leading-6 text-tertiary">默认值：{{ item.runtimeDefault || '空' }}</p>
            </div>

            <div class="flex-1 space-y-3">
              <div class="flex justify-end">
                <el-switch v-model="item.enabled" active-text="已启用" inactive-text="已停用" />
              </div>
              <el-input
                v-if="item.valueType === 'textarea'"
                v-model="item.configValue"
                type="textarea"
                :rows="8"
                resize="vertical"
              />
              <el-input v-else v-model="item.configValue" size="large" />
              <div class="flex justify-end gap-3">
                <el-button size="large" class="hard-button-secondary" @click="resetItem(item)">回到默认</el-button>
                <el-button
                  :loading="savingKey === item.configKey"
                  size="large"
                  class="action-button"
                  @click="saveItem(item)"
                >
                  保存配置
                </el-button>
              </div>
            </div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  fetchAdminSystemConfigsApi,
  updateAdminSystemConfigApi,
  type AdminSystemConfigItem
} from '@/api/admin'

const loading = ref(false)
const savingKey = ref('')
const configs = ref<AdminSystemConfigItem[]>([])

const groupMeta: Record<string, { label: string; title: string }> = {
  llm: { label: 'LLM', title: '问答与评分模型配置' },
  embedding: { label: 'Embedding', title: '检索与向量化配置' },
  prompt: { label: 'Prompt', title: '核心提示词模板' }
}

const groupedConfigs = computed(() => {
  return Object.entries(groupMeta)
    .map(([key, meta]) => ({
      key,
      label: meta.label,
      title: meta.title,
      items: configs.value.filter((item) => item.configGroup === key)
    }))
    .filter((group) => group.items.length)
})

const loadConfigs = async () => {
  loading.value = true
  try {
    const response = await fetchAdminSystemConfigsApi()
    configs.value = response.data.map((item) => ({ ...item }))
  } catch {
    ElMessage.error('系统配置加载失败')
  } finally {
    loading.value = false
  }
}

const resetItem = (item: AdminSystemConfigItem) => {
  item.configValue = item.runtimeDefault || ''
  item.enabled = true
}

const saveItem = async (item: AdminSystemConfigItem) => {
  savingKey.value = item.configKey
  try {
    const response = await updateAdminSystemConfigApi(item.configKey, {
      configValue: item.configValue,
      enabled: item.enabled
    })
    const index = configs.value.findIndex((entry) => entry.configKey === item.configKey)
    if (index >= 0) {
      configs.value[index] = { ...response.data }
    }
    ElMessage.success('配置已保存')
  } catch {
    ElMessage.error('配置保存失败')
  } finally {
    savingKey.value = ''
  }
}

onMounted(() => {
  void loadConfigs()
})
</script>
