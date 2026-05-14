<template>
  <div class="admin-tool-grid">
    <aside class="space-y-4">
      <section class="surface-muted p-4">
        <div class="text-sm font-semibold text-ink">筛选文档</div>
        <div class="mt-4 grid gap-3">
          <el-select v-model="filter.categoryId" clearable placeholder="按分类" size="large">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="filter.businessType" clearable placeholder="按业务归属" size="large">
            <el-option label="系统知识" value="system_knowledge" />
            <el-option label="个人笔记" value="user_note" />
            <el-option label="简历资料" value="resume" />
            <el-option label="JD 资料" value="jd" />
            <el-option label="项目资料" value="project_doc" />
          </el-select>
          <el-select v-model="filter.fileType" clearable placeholder="按文件类型" size="large">
            <el-option label="Markdown" value="md" />
            <el-option label="TXT" value="txt" />
            <el-option label="PDF" value="pdf" />
            <el-option label="DOC" value="doc" />
            <el-option label="DOCX" value="docx" />
          </el-select>
          <el-select v-model="filter.parseStatus" clearable placeholder="解析状态" size="large">
            <el-option label="处理中" value="pending" />
            <el-option label="已完成" value="parsed" />
            <el-option label="失败" value="failed" />
          </el-select>
          <el-select v-model="filter.indexStatus" clearable placeholder="索引状态" size="large">
            <el-option label="处理中" value="pending" />
            <el-option label="已完成" value="indexed" />
            <el-option label="失败" value="failed" />
          </el-select>
          <el-input v-model="filter.keyword" clearable placeholder="搜索文档" size="large" />
        </div>

        <div class="mt-4 grid gap-3">
          <el-button :loading="loading" type="primary" class="action-button" @click="emit('load')">刷新列表</el-button>
          <el-button class="hard-button-secondary" @click="emit('filterReset')">重置筛选</el-button>
          <el-button class="hard-button-secondary" @click="seedPanelOpen = !seedPanelOpen">
            {{ seedPanelOpen ? '收起导入区' : '导入内置资料' }}
          </el-button>
          <el-button class="hard-button-secondary" @click="searchPanelOpen = !searchPanelOpen">
            {{ searchPanelOpen ? '收起检索区' : '检索验证' }}
          </el-button>
        </div>
      </section>

      <section class="surface-card p-4">
        <div class="text-sm font-semibold text-ink">补救失败文档</div>
        <div class="mt-4 grid gap-3">
          <article class="surface-muted p-4">
            <div class="text-xs uppercase tracking-[0.18em] text-tertiary">解析失败</div>
            <div class="mt-2 text-2xl font-semibold text-ink">{{ failedParseDocs.length }}</div>
            <p class="mt-2 text-sm text-secondary">优先重试当前列表里的解析失败文档。</p>
          </article>
          <article class="surface-muted p-4">
            <div class="text-xs uppercase tracking-[0.18em] text-tertiary">索引失败</div>
            <div class="mt-2 text-2xl font-semibold text-ink">{{ failedIndexDocs.length }}</div>
            <p class="mt-2 text-sm text-secondary">索引失败时直接重建当前列表里的索引。</p>
          </article>
        </div>
        <div class="mt-4 grid gap-3">
          <el-button
            :disabled="!failedParseDocs.length"
            :loading="batchActionId === 'rechunk-batch'"
            class="hard-button-secondary"
            @click="emit('batchRechunk', failedParseDocs)"
          >
            重试当前列表的解析失败文档
          </el-button>
          <el-button
            :disabled="!failedIndexDocs.length"
            :loading="batchActionId === 'reindex-batch'"
            type="primary"
            class="action-button"
            @click="emit('batchReindex', failedIndexDocs)"
          >
            重建当前列表的失败索引
          </el-button>
        </div>
      </section>

      <section v-if="seedPanelOpen" class="surface-card p-4">
        <div class="text-sm font-semibold text-ink">导入资料</div>
        <div class="mt-4 space-y-3">
          <article v-for="seed in seeds" :key="seed.seedKey" class="surface-muted p-4">
            <div class="font-semibold text-ink">{{ seed.title }}</div>
            <p class="mt-2 text-sm leading-6 text-secondary">{{ seed.summary }}</p>
            <div class="mt-3 flex items-center justify-between gap-3">
              <span class="truncate text-xs uppercase tracking-[0.2em] text-tertiary">{{ seed.seedKey }}</span>
              <el-button :loading="importing === seed.seedKey" type="primary" class="action-button !min-h-9 !px-3" @click="emit('import', seed.seedKey)">
                导入
              </el-button>
            </div>
          </article>
        </div>
      </section>

      <section v-if="searchPanelOpen" class="surface-card p-4">
        <div class="text-sm font-semibold text-ink">检索验证</div>
        <div class="mt-4 grid gap-3">
          <el-input v-model="searchQuery" placeholder="输入一个问题，检查检索结果是否可用" size="large" />
          <el-button :loading="searching" type="primary" class="action-button" @click="runSearch">
            {{ searching ? '检索中...' : '开始检索' }}
          </el-button>
        </div>
      </section>
    </aside>

    <section class="space-y-4">
      <header class="shell-section-card p-5">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h3 class="admin-section-title">查看文档并执行治理</h3>
            <p class="mt-2 text-sm text-secondary">优先看失败状态，再决定重试解析还是重建索引。</p>
          </div>
          <div class="text-sm text-slate-500">共 {{ total }} 份文档</div>
        </div>
      </header>

      <section v-if="searchPanelOpen && searchResult" class="shell-section-card p-5">
        <div class="flex items-center justify-between gap-3">
          <div class="text-sm font-semibold text-ink">检索结果</div>
          <div class="text-xs uppercase tracking-[0.16em] text-tertiary">{{ searchResult.references.length }} 条命中</div>
        </div>

        <div v-if="searchResult.references.length" class="mt-4 grid gap-3 xl:grid-cols-2">
          <article v-for="(reference, index) in searchResult.references" :key="reference.chunkId" class="surface-muted p-4">
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="text-[11px] font-semibold uppercase tracking-[0.18em] text-tertiary">命中 {{ index + 1 }}</span>
                  <span class="text-[11px] font-semibold" :class="confidenceClass(reference.score)">
                    {{ confidenceLabel(reference.score) }}
                  </span>
                </div>
                <div class="mt-2 font-semibold text-ink">{{ reference.docTitle }}</div>
              </div>
              <span class="text-sm font-semibold text-ink">{{ scorePercent(reference.score) }}</span>
            </div>
            <p class="mt-3 text-sm leading-6 text-secondary">{{ reference.snippet }}</p>
          </article>
        </div>

        <div v-else class="mt-4 rounded-2xl bg-[var(--panel-muted)] px-4 py-5 text-sm text-secondary">
          没有找到相关结果。
        </div>
      </section>

      <section class="surface-card overflow-hidden">
        <div v-if="docs.length" class="divide-y divide-slate-200/70 dark:divide-slate-700/70">
          <article v-for="doc in docs" :key="doc.id" class="admin-record">
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <div class="font-semibold text-ink">{{ doc.title }}</div>
                <el-tag size="small" effect="plain">{{ recoveryLabel(doc) }}</el-tag>
              </div>
              <div class="mt-2 flex flex-wrap gap-2 text-xs uppercase tracking-[0.2em] text-secondary">
                <span>{{ doc.libraryScope === 'personal' ? '个人库' : '系统库' }}</span>
                <span>{{ businessTypeLabel(doc.businessType) }}</span>
                <span v-if="doc.fileType">{{ doc.fileType.toUpperCase() }}</span>
                <span>{{ doc.chunkCount ?? 0 }} 个分块</span>
              </div>
              <div class="mt-2 flex flex-wrap gap-2 text-xs text-secondary">
                <span>解析 {{ processLabel(doc.parseStatus) }}</span>
                <span>索引 {{ indexLabel(doc.indexStatus) }}</span>
                <span>状态 {{ statusLabel(doc.status) }}</span>
              </div>
              <p class="mt-3 text-sm leading-6 text-secondary">{{ doc.summary || '暂无摘要' }}</p>
            </div>
            <div class="admin-record__actions">
              <el-button :loading="actionId === `rechunk-${doc.id}`" class="hard-button-secondary !min-h-9 !px-3" @click="emit('rechunk', doc.id)">
                重新切分
              </el-button>
              <el-button :loading="actionId === `reindex-${doc.id}`" type="primary" class="action-button !min-h-9 !px-3" @click="emit('reindex', doc.id)">
                重建索引
              </el-button>
            </div>
          </article>
        </div>
        <div v-else class="px-5 py-12 text-center text-sm text-slate-500">
          还没有文档，先导入一份资料。
        </div>
      </section>

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
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { searchKnowledgeApi } from '@/api/knowledge'
import type { CategoryItem, KnowledgeDocItem, KnowledgeSearchResult } from '@/types/api'

interface KnowledgeFilter {
  categoryId?: number
  businessType?: string
  fileType?: string
  parseStatus?: KnowledgeDocItem['parseStatus']
  indexStatus?: KnowledgeDocItem['indexStatus']
  status?: KnowledgeDocItem['status']
  keyword: string
}

interface SeedItem {
  seedKey: string
  title: string
  summary: string
}

const props = defineProps<{
  docs: KnowledgeDocItem[]
  categories: CategoryItem[]
  filter: KnowledgeFilter
  seeds: SeedItem[]
  loading: boolean
  importing: string | null
  actionId: string | null
  batchActionId: string | null
  currentPage: number
  pageSize: number
  total: number
  totalPages: number
}>()

const currentPage = defineModel<number>('currentPage', { default: 1 })
const seedPanelOpen = ref(false)
const searchPanelOpen = ref(false)
const searching = ref(false)
const searchQuery = ref('')
const searchResult = ref<KnowledgeSearchResult | null>(null)

const emit = defineEmits<{
  import: [seedKey: string]
  rechunk: [id: number]
  reindex: [id: number]
  batchRechunk: [ids: number[]]
  batchReindex: [ids: number[]]
  filterReset: []
  load: []
  pageChange: [page: number]
}>()

const failedParseDocs = computed(() => props.docs.filter((doc) => doc.parseStatus === 'failed').map((doc) => doc.id))
const failedIndexDocs = computed(() => props.docs.filter((doc) => doc.indexStatus === 'failed').map((doc) => doc.id))

const runSearch = async () => {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('请输入检索问题')
    return
  }
  searching.value = true
  searchResult.value = null
  try {
    const response = await searchKnowledgeApi(searchQuery.value.trim())
    searchResult.value = response.data
  } catch {
    ElMessage.error('知识检索失败')
  } finally {
    searching.value = false
  }
}

const scorePercent = (score?: number) => {
  if (score == null) return 'N/A'
  return `${Math.round(score * 100)}%`
}

const confidenceLabel = (score?: number) => {
  if (score == null) return '待核验'
  if (score >= 0.82) return '高可信'
  if (score >= 0.66) return '可参考'
  return '弱相关'
}

const confidenceClass = (score?: number) => {
  if (score == null) return 'text-secondary'
  if (score >= 0.82) return 'text-[var(--bc-cyan)]'
  if (score >= 0.66) return 'text-[var(--bc-amber)]'
  return 'text-[var(--bc-coral)]'
}

const businessTypeLabel = (type?: string) => {
  if (type === 'user_note') return '个人笔记'
  if (type === 'resume') return '简历资料'
  if (type === 'jd') return 'JD 资料'
  if (type === 'project_doc') return '项目资料'
  return '系统知识'
}

const processLabel = (status?: string) => {
  if (status === 'failed') return '失败'
  if (status === 'parsed') return '已完成'
  return '处理中'
}

const indexLabel = (status?: string) => {
  if (status === 'failed') return '失败'
  if (status === 'indexed') return '已完成'
  return '处理中'
}

const statusLabel = (status?: string) => {
  if (status === 'indexed') return '可使用'
  if (status === 'parsed') return '待索引'
  return '待处理'
}

const recoveryLabel = (doc: KnowledgeDocItem) => {
  if (doc.parseStatus === 'failed') return '先重试解析'
  if (doc.indexStatus === 'failed') return '先重建索引'
  if (doc.indexStatus === 'indexed') return '可直接使用'
  return '继续处理'
}
</script>

<style scoped>
.admin-tool-grid {
  display: grid;
  gap: 20px;
}

.admin-section-title {
  color: var(--bc-ink);
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.admin-record {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
}

.admin-record__actions {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
}

@media (min-width: 1280px) {
  .admin-tool-grid {
    grid-template-columns: 288px minmax(0, 1fr);
    align-items: start;
  }
}

@media (max-width: 767px) {
  .admin-record {
    flex-direction: column;
  }

  .admin-record__actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
