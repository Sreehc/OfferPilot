<template>
  <div class="space-y-4">
    <section class="surface-muted p-4">
      <div class="grid gap-3 md:grid-cols-3">
        <el-select v-model="filter.categoryId" clearable placeholder="按分类" size="large">
          <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="filter.status" clearable placeholder="按状态" size="large">
          <el-option label="草稿" value="draft" />
          <el-option label="已解析" value="parsed" />
          <el-option label="已索引" value="indexed" />
        </el-select>
        <el-input v-model="filter.keyword" clearable placeholder="搜索文档" size="large" />
      </div>

      <div class="mt-4 flex flex-wrap gap-3">
        <el-button :loading="loading" type="primary" class="action-button" @click="emit('load')">刷新</el-button>
        <el-button class="hard-button-secondary" @click="emit('filterReset')">重置</el-button>
        <el-button class="hard-button-secondary" @click="seedPanelOpen = !seedPanelOpen">
          {{ seedPanelOpen ? '收起导入区' : '导入内置资料' }}
        </el-button>
      </div>
    </section>

    <section v-if="seedPanelOpen" class="surface-muted p-4">
      <div class="text-sm font-semibold text-ink">导入内置资料</div>
      <div class="mt-4 grid gap-3 xl:grid-cols-3">
        <div v-for="seed in seeds" :key="seed.seedKey" class="surface-card p-4">
          <div class="font-semibold text-ink">{{ seed.title }}</div>
          <p class="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">{{ seed.summary }}</p>
          <div class="mt-3 flex items-center justify-between gap-3">
            <span class="truncate text-xs uppercase tracking-[0.2em] text-slate-400 dark:text-slate-500">{{ seed.seedKey }}</span>
            <el-button :loading="importing === seed.seedKey" type="primary" class="action-button !min-h-9 !px-3" @click="emit('import', seed.seedKey)">
              导入
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="space-y-4">
      <div class="text-sm text-slate-500 dark:text-slate-400">共 {{ total }} 份文档</div>

      <article v-for="doc in docs" :key="doc.id" class="surface-card p-4">
        <div class="flex items-start justify-between gap-4">
          <div>
            <div class="font-semibold text-ink">{{ doc.title }}</div>
            <div class="mt-2 flex flex-wrap gap-2 text-xs uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
              <span>{{ doc.status === 'draft' ? '草稿' : doc.status === 'parsed' ? '已解析' : '已索引' }}</span>
              <span>{{ doc.chunkCount ?? 0 }} 个分块</span>
            </div>
            <p class="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300">{{ doc.summary || '暂无摘要' }}</p>
          </div>
          <div class="flex shrink-0 gap-2">
            <el-button :loading="actionId === `rechunk-${doc.id}`" class="hard-button-secondary !min-h-9 !px-3" @click="emit('rechunk', doc.id)">
              重切分
            </el-button>
            <el-button :loading="actionId === `reindex-${doc.id}`" type="primary" class="action-button !min-h-9 !px-3" @click="emit('reindex', doc.id)">
              重建索引
            </el-button>
          </div>
        </div>
      </article>

      <div v-if="totalPages > 1" class="mt-4 flex justify-center">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="(page: number) => emit('pageChange', page)"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { CategoryItem, KnowledgeDocItem } from '@/types/api'

interface KnowledgeFilter {
  categoryId?: number
  status?: KnowledgeDocItem['status']
  keyword: string
}

interface SeedItem {
  seedKey: string
  title: string
  summary: string
}

defineProps<{
  docs: KnowledgeDocItem[]
  categories: CategoryItem[]
  filter: KnowledgeFilter
  seeds: SeedItem[]
  loading: boolean
  importing: string | null
  actionId: string | null
  currentPage: number
  pageSize: number
  total: number
  totalPages: number
}>()

const currentPage = defineModel<number>('currentPage', { default: 1 })
const seedPanelOpen = ref(false)

const emit = defineEmits<{
  import: [seedKey: string]
  rechunk: [id: number]
  reindex: [id: number]
  filterReset: []
  load: []
  pageChange: [page: number]
}>()
</script>
