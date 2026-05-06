<template>
  <div class="knowledge-cockpit space-y-6">
    <section class="grid gap-4 xl:grid-cols-[minmax(0,1.08fr)_minmax(320px,0.92fr)]">
      <article class="cockpit-panel p-5 sm:p-6">
        <div class="flex flex-wrap items-start justify-between gap-4">
          <div class="min-w-0">
            <div class="flex items-center gap-3">
              <span class="state-pulse" aria-hidden="true"></span>
              <p class="section-kicker">资料概览</p>
            </div>
            <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">管理你的学习资料</h2>
            <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-300">
              上传、查看和筛选资料，确认哪些文档已可用于问答。
            </p>
          </div>
          <div class="grid min-w-[220px] gap-3 sm:grid-cols-2">
            <div class="data-slab p-3">
              <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">当前文档</p>
              <p class="mt-2 font-mono text-2xl font-semibold text-ink">{{ docs.length }}</p>
              <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">{{ activeTabLabel }} 当前页</p>
            </div>
            <div class="data-slab border-l-[var(--bc-cyan)] p-3">
              <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">可检索</p>
              <p class="mt-2 font-mono text-2xl font-semibold text-[var(--bc-cyan)]">{{ statusSummary.indexed }}</p>
              <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">已经可以参与知识库问答</p>
            </div>
          </div>
        </div>

        <div class="mt-6 grid gap-3 md:grid-cols-3">
          <article
            v-for="signal in knowledgeSignals"
            :key="signal.label"
            class="rounded-[20px] border border-[var(--bc-line)] bg-white/40 p-4 dark:bg-white/5"
          >
            <div class="flex items-center gap-2">
              <span class="inline-flex h-2.5 w-2.5 rounded-full" :class="signal.dotClass"></span>
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">
                {{ signal.label }}
              </p>
            </div>
            <p class="mt-3 font-mono text-2xl font-semibold text-ink">{{ signal.value }}</p>
            <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ signal.detail }}</p>
          </article>
        </div>
      </article>

      <article class="cockpit-panel p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="section-kicker">上传资料</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">上传资料</h3>
          </div>
          <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ activeTab === 'my' ? '我的文档' : '系统资料' }}</span>
        </div>

        <div
          class="upload-dock mt-5"
          @dragover.prevent
          @drop.prevent="handleDrop"
        >
          <div class="upload-dock__icon" aria-hidden="true">
            <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 16V4m0 0l-4 4m4-4l4 4M4 15v2a3 3 0 003 3h10a3 3 0 003-3v-2" />
            </svg>
          </div>
          <div class="text-center">
            <p class="text-sm font-semibold text-ink">拖拽文档到这里，或直接选择上传</p>
            <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">
              支持 `md / txt / pdf`，单文件不超过 20MB。上传后会自动处理，完成后可用于检索。
            </p>
          </div>
          <el-upload
            :show-file-list="false"
            :before-upload="handleBeforeUpload"
            :http-request="handleUpload"
            accept=".md,.markdown,.txt,.text,.pdf"
          >
            <el-button :loading="uploading" type="primary" size="large" class="action-button !min-h-12 !px-6">
              {{ uploading ? '上传中...' : '选择文件上传' }}
            </el-button>
          </el-upload>
        </div>

        <div class="mt-5 grid gap-3 sm:grid-cols-3">
          <article
            v-for="item in statusCards"
            :key="item.status"
            class="status-orbit"
            :class="item.toneClass"
          >
            <div class="flex items-center justify-between gap-3">
              <span class="text-[11px] font-semibold uppercase tracking-[0.22em]">{{ item.label }}</span>
              <span class="font-mono text-lg font-semibold">{{ item.count }}</span>
            </div>
            <div class="status-orbit__track mt-3">
              <span class="status-orbit__fill" :style="{ width: `${item.percent}%` }"></span>
            </div>
            <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ item.description }}</p>
          </article>
        </div>
      </article>
    </section>

    <section class="space-y-4">
      <article class="cockpit-panel p-5 sm:p-6">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div class="min-w-0">
            <p class="section-kicker">文档列表</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">查看资料列表</h3>
            <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-300">
              按分类、状态和关键词筛选资料，并查看当前处理状态。
            </p>
          </div>
          <div class="mode-switch grid grid-cols-2 gap-2">
            <button
              type="button"
              class="mode-switch__item"
              :class="{ 'mode-switch__item-active': activeTab === 'system' }"
              @click="switchTab('system')"
            >
              系统资料
            </button>
            <button
              type="button"
              class="mode-switch__item"
              :class="{ 'mode-switch__item-active': activeTab === 'my' }"
              @click="switchTab('my')"
            >
              我的文档
            </button>
          </div>
        </div>

        <div class="mt-5 grid gap-3 lg:grid-cols-[minmax(0,1fr)_auto]">
          <div class="grid gap-3 md:grid-cols-3">
            <el-select v-model="filters.categoryId" clearable placeholder="知识分类" size="large">
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
            <el-select v-model="filters.status" clearable placeholder="文档状态" size="large">
              <el-option label="草稿" value="draft" />
              <el-option label="已解析" value="parsed" />
              <el-option label="已索引" value="indexed" />
            </el-select>
            <el-input v-model="filters.keyword" clearable placeholder="标题 / 摘要关键字" size="large" />
          </div>
          <div class="flex flex-wrap gap-3">
            <el-button :loading="loadingDocs" type="primary" size="large" class="action-button !px-5" @click="loadDocs">
              刷新列表
            </el-button>
            <el-button size="large" class="hard-button-secondary !px-5" @click="resetFilters">重置筛选</el-button>
          </div>
        </div>

        <div v-if="docs.length === 0 && !loadingDocs" class="mt-5">
          <EmptyState
            class="empty-state-card"
            icon="document"
            :title="activeTab === 'my' ? '你还没有上传文档' : '系统资料暂时为空'"
            :description="activeTab === 'my'
              ? '上传文档后，可在这里查看处理进度。'
              : '请联系管理员导入知识资料，或先切换到“我的文档”上传学习材料。'"
          />
        </div>

        <div v-else class="mt-5 grid gap-4 lg:grid-cols-2 2xl:grid-cols-3">
          <article
            v-for="doc in docs"
            :key="doc.id"
            class="mission-card doc-card p-4 sm:p-5"
            :class="statusToneClass(doc.status)"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="flex min-w-0 items-start gap-3">
                <div class="doc-card__glyph" :class="docType(doc.fileUrl) === 'pdf' ? 'doc-card__glyph-pdf' : 'doc-card__glyph-text'">
                  <svg v-if="docType(doc.fileUrl) === 'pdf'" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.6">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
                  </svg>
                  <svg v-else class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.6">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
                  </svg>
                </div>
                <div class="min-w-0">
                  <div class="flex flex-wrap items-center gap-2">
                    <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ docType(doc.fileUrl).toUpperCase() }}</span>
                    <span class="inline-flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.18em]" :class="statusTextClass(doc.status)">
                      <span class="h-2 w-2 rounded-full" :class="statusDotClass(doc.status)"></span>
                      {{ statusLabel(doc.status) }}
                    </span>
                  </div>
                  <h4 class="mt-3 line-clamp-2 text-lg font-semibold text-ink">{{ doc.title }}</h4>
                </div>
              </div>
              <el-popconfirm
                v-if="activeTab === 'my'"
                title="确认删除此文档？删除后关联的 chunk 和向量数据将一并清除。"
                confirm-button-text="删除"
                cancel-button-text="取消"
                @confirm="handleDelete(doc.id)"
              >
                <template #reference>
                  <button type="button" class="doc-card__danger">删除</button>
                </template>
              </el-popconfirm>
            </div>

            <div class="mt-4 flex flex-wrap gap-2 text-[11px]">
              <span class="rounded-full border border-[var(--bc-line)] px-2.5 py-1 text-slate-500 dark:text-slate-300">
                {{ doc.categoryName || '未分类' }}
              </span>
              <span class="rounded-full border border-[var(--bc-line)] px-2.5 py-1 text-slate-500 dark:text-slate-300">
                {{ doc.chunkCount ?? 0 }} 个片段
              </span>
              <span class="rounded-full border border-[var(--bc-line)] px-2.5 py-1 text-slate-500 dark:text-slate-300">
                {{ formatDate(doc.updateTime) }}
              </span>
            </div>

            <p class="mt-4 line-clamp-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
              {{ doc.summary || '暂无摘要，等待系统完成解析后会在这里显示资料概览。' }}
            </p>

            <div class="mt-4 rounded-[18px] border border-[var(--bc-line)] bg-white/45 p-3 dark:bg-white/5">
              <div class="flex items-center justify-between gap-3 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">
                <span>参与问答路径</span>
                <span>{{ statusPathLabel(doc.status) }}</span>
              </div>
              <div class="mt-3 flex flex-wrap items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                <span class="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800/80">问题</span>
                <span>→</span>
                <span class="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800/80">{{ doc.categoryName || '分类' }}</span>
                <span>→</span>
                <span class="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800/80 truncate max-w-[130px]">{{ doc.title }}</span>
                <span>→</span>
                <span class="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800/80">片段 {{ doc.chunkCount ? Math.min(doc.chunkCount, 1) : 0 }}</span>
              </div>
            </div>
          </article>
        </div>

        <div v-if="totalPages > 1" class="mt-6 flex justify-center">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </article>

      <article class="cockpit-panel p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="section-kicker">分类状态</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">按分类查看文档分布</h3>
          </div>
          <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ categories.length }} 个分类</span>
        </div>

        <div v-if="categoryMatrix.length" class="mt-5 space-y-3">
          <article
            v-for="item in categoryMatrix"
            :key="item.id"
            class="matrix-node"
          >
            <div class="flex items-center justify-between gap-3">
              <div class="min-w-0">
                <p class="truncate text-sm font-semibold text-ink">{{ item.name }}</p>
                <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">{{ item.summary }}</p>
              </div>
              <div class="text-right">
                <p class="font-mono text-xl font-semibold text-ink">{{ item.count }}</p>
                <p class="text-[11px] uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">{{ item.dominantLabel }}</p>
              </div>
            </div>
            <div class="mt-4 grid grid-cols-3 gap-2 text-[11px]">
              <div v-for="segment in item.segments" :key="segment.label" class="matrix-node__segment">
                <div class="flex items-center justify-between gap-2">
                  <span class="inline-flex items-center gap-2">
                    <span class="h-2 w-2 rounded-full" :class="segment.dotClass"></span>
                    {{ segment.label }}
                  </span>
                  <span class="font-mono">{{ segment.value }}</span>
                </div>
              </div>
            </div>
          </article>
        </div>
        <EmptyState
          v-else
          class="empty-state-card mt-5"
          icon="document"
          title="还没有知识分类矩阵"
          description="加载到知识分类后，这里会按分类显示文档数量和主要状态。"
          compact
        />
      </article>
    </section>

    <section class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div class="min-w-0">
          <p class="section-kicker">检索验证</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">确认资料是否真的能被问答命中</h3>
          <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-300">
            提一个问题后，检查命中的文档、片段和相关度。这样能判断知识库回答是否值得参考。
          </p>
        </div>
        <div class="grid min-w-[220px] gap-3 sm:grid-cols-2">
          <div class="data-slab border-l-[var(--bc-cyan)] p-3">
            <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">命中引用</p>
            <p class="mt-2 font-mono text-2xl font-semibold text-[var(--bc-cyan)]">{{ searchResult?.references.length ?? 0 }}</p>
            <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">当前检索命中的资料片段</p>
          </div>
          <div class="data-slab border-l-[var(--bc-line-hot)] p-3">
            <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">验证路径</p>
            <p class="mt-2 text-sm font-semibold text-ink">问题 → 分类 → 文档 → 片段</p>
            <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">逐步确认资料来源是否合理</p>
          </div>
        </div>
      </div>

      <div class="mt-5 grid gap-3 lg:grid-cols-[minmax(0,1fr)_150px]">
        <el-input v-model="searchQuery" placeholder="例如：JVM 垃圾回收器分类，以及 CMS 和 G1 的差异" size="large" />
        <el-button :loading="searching" type="primary" size="large" class="action-button !min-h-12" @click="runSearch">
          {{ searching ? '检索中...' : '开始检索' }}
        </el-button>
      </div>

      <div v-if="searchResult?.references.length" class="mt-6 grid gap-4 xl:grid-cols-2">
        <article
          v-for="reference in decoratedReferences"
          :key="reference.chunkId"
          class="reference-card p-4 sm:p-5"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <span class="hard-chip !px-2 !py-0.5 !text-[9px]">命中 {{ reference.rank }}</span>
                <span class="text-[11px] font-semibold uppercase tracking-[0.2em]" :class="reference.confidenceClass">
                  {{ reference.confidenceLabel }}
                </span>
              </div>
              <h4 class="mt-3 text-lg font-semibold text-ink">{{ reference.docTitle }}</h4>
            </div>
            <span class="reference-score">{{ reference.scoreText }}</span>
          </div>

          <div class="mt-4 flex flex-wrap items-center gap-2 text-[11px] text-slate-500 dark:text-slate-400">
            <span class="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800/80">{{ searchResult.query }}</span>
            <span>→</span>
            <span class="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800/80">{{ resolveReferenceCategory(reference.docTitle) }}</span>
            <span>→</span>
            <span class="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800/80 truncate max-w-[150px]">{{ reference.docTitle }}</span>
            <span>→</span>
            <span class="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800/80">片段 {{ reference.chunkId }}</span>
          </div>

          <p class="mt-4 text-sm leading-7 text-slate-700 dark:text-slate-200" v-html="reference.snippetHtml"></p>
        </article>
      </div>

      <EmptyState
        v-else-if="searchResult"
        class="empty-state-card mt-6"
        icon="search"
        title="没有找到相关结果"
        description="尝试换一个更具体的问题，或者先检查相关资料是否已经成功索引。"
        compact
      >
        <template #action>
          <button type="button" class="hard-button-secondary text-sm" @click="clearSearch">
            清空检索
          </button>
        </template>
      </EmptyState>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { fetchCategoriesApi } from '@/api/category'
import {
  deleteKnowledgeDocApi,
  fetchKnowledgeDocsApi,
  fetchMyKnowledgeDocsApi,
  searchKnowledgeApi,
  uploadKnowledgeDocApi,
} from '@/api/knowledge'
import type { CategoryItem, KnowledgeDocItem, KnowledgeSearchResult } from '@/types/api'

const categories = ref<CategoryItem[]>([])
const docs = ref<KnowledgeDocItem[]>([])
const searchResult = ref<KnowledgeSearchResult | null>(null)
const loadingDocs = ref(false)
const searching = ref(false)
const uploading = ref(false)
const searchQuery = ref('')
const activeTab = ref<'system' | 'my'>('system')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)
const filters = reactive<{
  categoryId?: number
  keyword: string
  status?: KnowledgeDocItem['status']
}>({
  categoryId: undefined,
  keyword: '',
  status: undefined,
})

const activeTabLabel = computed(() => (activeTab.value === 'my' ? '我的文档' : '系统资料'))

const statusSummary = computed(() => {
  const summary = { draft: 0, parsed: 0, indexed: 0 }
  docs.value.forEach((doc) => {
    summary[doc.status] += 1
  })
  return summary
})

const totalChunks = computed(() =>
  docs.value.reduce((sum, doc) => sum + (doc.chunkCount ?? 0), 0)
)

const indexedRatio = computed(() => {
  if (!docs.value.length) return 0
  return Math.round((statusSummary.value.indexed / docs.value.length) * 100)
})

const freshestUpdate = computed(() => {
  const timestamps = docs.value
    .map((doc) => doc.updateTime ? new Date(doc.updateTime).getTime() : 0)
    .filter((value) => value > 0)
  if (!timestamps.length) return '暂无更新'
  const latest = new Date(Math.max(...timestamps))
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(latest)
})

const knowledgeSignals = computed(() => [
  {
    label: '可检索占比',
    value: `${indexedRatio.value}%`,
    detail: '当前页里有多少资料已经可以参与知识库问答。',
    dotClass: 'bg-[var(--bc-cyan)]',
  },
  {
    label: '总片段数',
    value: totalChunks.value,
    detail: '片段越多，覆盖越细，也更需要检查命中质量。',
    dotClass: 'bg-[var(--bc-amber)]',
  },
  {
    label: '最近更新',
    value: freshestUpdate.value,
    detail: '方便判断是否需要重新验证检索结果。',
    dotClass: 'bg-[var(--bc-lime)]',
  },
])

const statusCards = computed(() => {
  const totalDocs = docs.value.length || 1
  return [
    {
      status: 'draft',
      label: '待处理',
      count: statusSummary.value.draft,
      percent: Math.round((statusSummary.value.draft / totalDocs) * 100),
      description: '刚上传，还没有完成解析。',
      toneClass: 'status-orbit-draft',
    },
    {
      status: 'parsed',
      label: '已解析',
      count: statusSummary.value.parsed,
      percent: Math.round((statusSummary.value.parsed / totalDocs) * 100),
      description: '已经切分完成，等待进入检索。',
      toneClass: 'status-orbit-parsed',
    },
    {
      status: 'indexed',
      label: '可检索',
      count: statusSummary.value.indexed,
      percent: Math.round((statusSummary.value.indexed / totalDocs) * 100),
      description: '已经可以参与知识库问答。',
      toneClass: 'status-orbit-indexed',
    },
  ]
})

const categoryMatrix = computed(() =>
  categories.value.map((category) => {
    const scopedDocs = docs.value.filter((doc) => doc.categoryId === category.id)
    const counts = {
      draft: scopedDocs.filter((doc) => doc.status === 'draft').length,
      parsed: scopedDocs.filter((doc) => doc.status === 'parsed').length,
      indexed: scopedDocs.filter((doc) => doc.status === 'indexed').length,
    }
    const dominantStatus = (['indexed', 'parsed', 'draft'] as const).find((status) => counts[status] > 0) ?? 'draft'
    return {
      id: category.id,
      name: category.name,
      count: scopedDocs.length,
      dominantLabel: statusLabel(dominantStatus),
      summary: scopedDocs.length
        ? `当前页已命中 ${scopedDocs.length} 份文档，${counts.indexed} 份已可检索。`
        : '当前筛选条件下暂无文档落入该分类。',
      segments: [
        { label: '草稿', value: counts.draft, dotClass: statusDotClass('draft') },
        { label: '解析', value: counts.parsed, dotClass: statusDotClass('parsed') },
        { label: '索引', value: counts.indexed, dotClass: statusDotClass('indexed') },
      ],
    }
  })
)

const decoratedReferences = computed(() =>
  (searchResult.value?.references ?? []).map((reference, index) => {
    const query = searchResult.value?.query ?? ''
    const keywords = query.split(/[\s,，。？！?!.、/]+/).filter((item) => item.length >= 2)
    return {
      ...reference,
      rank: index + 1,
      scoreText: scorePercent(reference.score),
      confidenceLabel: confidenceLabel(reference.score),
      confidenceClass: confidenceClass(reference.score),
      snippetHtml: highlightSnippet(reference.snippet, keywords),
    }
  })
)

const clearSearch = () => {
  searchQuery.value = ''
  searchResult.value = null
}

const loadCategories = async () => {
  try {
    const response = await fetchCategoriesApi({ type: 'knowledge' })
    categories.value = response.data
  } catch {
    console.warn('Failed to load categories')
    categories.value = []
  }
}

const loadDocs = async () => {
  loadingDocs.value = true
  try {
    const params = {
      categoryId: filters.categoryId,
      keyword: filters.keyword || undefined,
      status: filters.status,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    }
    const response = activeTab.value === 'my'
      ? await fetchMyKnowledgeDocsApi(params)
      : await fetchKnowledgeDocsApi(params)
    docs.value = response.data.records
    total.value = response.data.total
    totalPages.value = response.data.totalPages
  } catch {
    ElMessage.error('文档加载失败')
  } finally {
    loadingDocs.value = false
  }
}

const switchTab = (tab: 'system' | 'my') => {
  activeTab.value = tab
  currentPage.value = 1
  void loadDocs()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  void loadDocs()
}

const handleBeforeUpload = (file: File) => {
  const maxSize = 20 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 20MB')
    return false
  }
  return true
}

const handleUpload = async (options: { file: File }) => {
  uploading.value = true
  try {
    await uploadKnowledgeDocApi(options.file, filters.categoryId)
    ElMessage.success('文档上传成功，正在后台处理')
    activeTab.value = 'my'
    currentPage.value = 1
    await loadDocs()
  } catch {
    ElMessage.error('文档上传失败')
  } finally {
    uploading.value = false
  }
}

const handleDrop = (event: DragEvent) => {
  const files = event.dataTransfer?.files
  if (files && files.length > 0) {
    const file = files[0]
    if (file && handleBeforeUpload(file)) {
      void handleUpload({ file })
    }
  }
}

const handleDelete = async (docId: number) => {
  try {
    await deleteKnowledgeDocApi(docId)
    ElMessage.success('文档已删除')
    await loadDocs()
  } catch {
    ElMessage.error('删除失败')
  }
}

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

const resetFilters = () => {
  filters.categoryId = undefined
  filters.keyword = ''
  filters.status = undefined
  currentPage.value = 1
  void loadDocs()
}

const statusLabel = (status: KnowledgeDocItem['status']) => {
  const map: Record<KnowledgeDocItem['status'], string> = { draft: '草稿', parsed: '已解析', indexed: '已索引' }
  return map[status]
}

const statusPathLabel = (status: KnowledgeDocItem['status']) => {
  const map: Record<KnowledgeDocItem['status'], string> = {
    draft: '上传完成',
    parsed: '已切分',
    indexed: '可参与问答',
  }
  return map[status]
}

const statusDotClass = (status: KnowledgeDocItem['status']) => {
  const map: Record<KnowledgeDocItem['status'], string> = {
    draft: 'bg-[var(--bc-coral)]',
    parsed: 'bg-[var(--bc-amber)]',
    indexed: 'bg-[var(--bc-cyan)]',
  }
  return map[status]
}

const statusTextClass = (status: KnowledgeDocItem['status']) => {
  const map: Record<KnowledgeDocItem['status'], string> = {
    draft: 'text-[var(--bc-coral)]',
    parsed: 'text-[var(--bc-amber)]',
    indexed: 'text-[var(--bc-cyan)]',
  }
  return map[status]
}

const statusToneClass = (status: KnowledgeDocItem['status']) => {
  const map: Record<KnowledgeDocItem['status'], string> = {
    draft: 'doc-card-danger',
    parsed: 'doc-card-warm',
    indexed: 'doc-card-cyan',
  }
  return map[status]
}

const docType = (fileUrl?: string): 'pdf' | 'text' => {
  if (!fileUrl) return 'text'
  return fileUrl.toLowerCase().endsWith('.pdf') ? 'pdf' : 'text'
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
  if (score == null) return 'text-slate-500 dark:text-slate-400'
  if (score >= 0.82) return 'text-[var(--bc-cyan)]'
  if (score >= 0.66) return 'text-[var(--bc-amber)]'
  return 'text-[var(--bc-coral)]'
}

const highlightSnippet = (snippet: string, keywords: string[]) => {
  if (!keywords.length) return snippet
  const escaped = keywords.map((keyword) => keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
  const pattern = new RegExp(`(${escaped.join('|')})`, 'gi')
  return snippet.replace(pattern, '<mark class="knowledge-highlight">$1</mark>')
}

const resolveReferenceCategory = (docTitle: string) => {
  const matched = categories.value.find((category) => docTitle.toLowerCase().includes(category.name.toLowerCase()))
  return matched?.name ?? '知识分类'
}

const formatDate = (value?: string) => {
  if (!value) return 'recent'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

onMounted(async () => {
  await loadCategories()
  await loadDocs()
})
</script>

<style scoped>
.knowledge-cockpit .mode-switch {
  min-width: min(100%, 260px);
}

.mode-switch {
  border: 1px solid var(--bc-line);
  border-radius: calc(var(--radius-md) + 4px);
  background: rgba(255, 255, 255, 0.32);
  padding: 4px;
}

.dark .mode-switch {
  background: rgba(255, 255, 255, 0.04);
}

.mode-switch__item {
  min-height: 44px;
  border: 0;
  border-radius: calc(var(--radius-sm) + 2px);
  background: transparent;
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  transition:
    background-color var(--motion-base) var(--ease-hard),
    color var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard);
}

.mode-switch__item-active {
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.18), rgba(var(--bc-accent-rgb), 0.08));
  color: var(--bc-ink);
  box-shadow: inset 0 0 0 1px rgba(var(--bc-accent-rgb), 0.2);
}

.upload-dock {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  min-height: 240px;
  border: 1px dashed rgba(var(--bc-accent-rgb), 0.34);
  border-radius: 28px;
  padding: 28px 22px;
  background:
    radial-gradient(circle at top, rgba(var(--bc-accent-rgb), 0.12), transparent 48%),
    rgba(255, 255, 255, 0.3);
  transition:
    border-color var(--motion-base) var(--ease-hard),
    transform var(--motion-base) var(--ease-hard),
    background-color var(--motion-base) var(--ease-hard);
}

.dark .upload-dock {
  background:
    radial-gradient(circle at top, rgba(var(--bc-accent-rgb), 0.16), transparent 48%),
    rgba(255, 255, 255, 0.04);
}

.upload-dock:hover {
  border-color: var(--bc-line-hot);
  transform: translateY(-1px);
}

.upload-dock__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 999px;
  color: #101826;
  background: linear-gradient(180deg, #ffe0ad 0%, var(--bc-amber) 100%);
  box-shadow: 0 16px 28px rgba(var(--bc-accent-rgb), 0.22);
}

.status-orbit {
  border-radius: 20px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.36);
  padding: 16px;
}

.dark .status-orbit {
  background: rgba(255, 255, 255, 0.04);
}

.status-orbit__track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(140, 166, 191, 0.18);
}

.status-orbit__fill {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.status-orbit-draft .status-orbit__fill {
  background: linear-gradient(90deg, rgba(255, 107, 107, 0.5), var(--bc-coral));
}

.status-orbit-parsed .status-orbit__fill {
  background: linear-gradient(90deg, rgba(255, 183, 77, 0.48), var(--bc-amber));
}

.status-orbit-indexed .status-orbit__fill {
  background: linear-gradient(90deg, rgba(85, 214, 190, 0.45), var(--bc-cyan));
}

.doc-card {
  min-height: 100%;
}

.doc-card-danger {
  border-color: rgba(255, 107, 107, 0.28);
}

.doc-card-warm {
  border-color: rgba(255, 183, 77, 0.3);
}

.doc-card-cyan {
  border-color: rgba(85, 214, 190, 0.32);
}

.doc-card__glyph {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 16px;
  flex-shrink: 0;
}

.doc-card__glyph-text {
  background: rgba(255, 183, 77, 0.16);
  color: var(--bc-amber);
}

.doc-card__glyph-pdf {
  background: rgba(255, 107, 107, 0.16);
  color: var(--bc-coral);
}

.doc-card__danger {
  border: 0;
  background: transparent;
  color: var(--bc-coral);
  font-size: 12px;
  font-weight: 700;
}

.matrix-node {
  border: 1px solid var(--bc-line);
  border-radius: 22px;
  padding: 16px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.08), transparent 42%),
    rgba(255, 255, 255, 0.34);
}

.dark .matrix-node {
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.05), transparent 42%),
    rgba(255, 255, 255, 0.04);
}

.matrix-node__segment {
  border-radius: 16px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.42);
}

.dark .matrix-node__segment {
  background: rgba(255, 255, 255, 0.06);
}

.reference-card {
  border: 1px solid rgba(85, 214, 190, 0.2);
  border-radius: var(--radius-md);
  background:
    linear-gradient(145deg, rgba(85, 214, 190, 0.08), transparent 36%),
    var(--bc-surface-card);
  box-shadow: var(--bc-shadow-soft);
}

.reference-score {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  border-radius: 999px;
  background: rgba(85, 214, 190, 0.12);
  color: var(--bc-cyan);
  font-family: theme('fontFamily.mono');
  font-size: 12px;
  font-weight: 700;
  padding: 6px 10px;
}

.knowledge-highlight {
  border-radius: 6px;
  background: rgba(255, 183, 77, 0.24);
  color: inherit;
  padding: 0 2px;
}

@media (max-width: 768px) {
  .upload-dock {
    min-height: 220px;
    padding-inline: 18px;
  }
}
</style>
