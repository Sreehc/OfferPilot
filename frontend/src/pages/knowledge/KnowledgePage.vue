<template>
  <div class="space-y-6">
    <!-- Header + Upload -->
    <section class="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
      <div class="paper-panel p-6">
        <p class="section-kicker">Knowledge Library</p>
        <h3 class="mt-4 text-3xl font-semibold tracking-[-0.03em] text-ink">知识库管理</h3>
        <p class="mt-4 text-sm leading-7 text-slate-600 dark:text-slate-300">
          系统内置资料与你上传的学习材料统一管理，支持分类查看、关键字检索和向量语义检索。
        </p>
      </div>
      <div class="paper-panel p-6">
        <p class="section-kicker">Upload Document</p>
        <div
          class="mt-4 flex flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed border-slate-300 dark:border-slate-600 bg-slate-50/50 dark:bg-slate-800/50 p-6 text-center transition-colors hover:border-accent hover:bg-accent/5"
          @dragover.prevent
          @drop.prevent="handleDrop"
        >
          <div class="text-sm text-slate-500 dark:text-slate-400">拖拽文件到此处，或点击选择</div>
          <div class="text-xs text-slate-400 dark:text-slate-500">支持 .md / .txt / .pdf，单文件最大 20MB</div>
          <el-upload
            :show-file-list="false"
            :before-upload="handleBeforeUpload"
            :http-request="handleUpload"
            accept=".md,.markdown,.txt,.text,.pdf"
          >
            <el-button :loading="uploading" type="primary" size="large" class="action-button">
              选择文件上传
            </el-button>
          </el-upload>
        </div>
      </div>
    </section>

    <!-- Tabs: System / My Docs -->
    <section class="paper-panel p-6">
      <div class="flex items-center gap-4 border-b border-slate-200 dark:border-slate-700 pb-4">
        <button
          class="pb-1 text-sm font-semibold transition-colors"
          :class="activeTab === 'system' ? 'border-b-2 border-accent text-accent' : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'"
          @click="switchTab('system')"
        >
          系统资料
        </button>
        <button
          class="pb-1 text-sm font-semibold transition-colors"
          :class="activeTab === 'my' ? 'border-b-2 border-accent text-accent' : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'"
          @click="switchTab('my')"
        >
          我的文档
        </button>
      </div>

      <!-- Filters -->
      <div class="mt-4 flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div class="grid gap-3 md:grid-cols-3">
          <el-select v-model="filters.categoryId" clearable placeholder="知识分类" size="large">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="filters.status" clearable placeholder="文档状态" size="large">
            <el-option label="draft" value="draft" />
            <el-option label="parsed" value="parsed" />
            <el-option label="indexed" value="indexed" />
          </el-select>
          <el-input v-model="filters.keyword" clearable placeholder="标题 / 摘要关键字" size="large" />
        </div>
        <div class="flex gap-3">
          <el-button :loading="loadingDocs" type="primary" size="large" class="action-button" @click="loadDocs">
            刷新列表
          </el-button>
          <el-button size="large" class="hard-button-secondary" @click="resetFilters">重置筛选</el-button>
        </div>
      </div>
    </section>

    <!-- Doc List -->
    <section>
      <div v-if="docs.length === 0 && !loadingDocs" class="empty-state-card">
        <div class="font-semibold text-ink">
          {{ activeTab === 'my' ? '你还没有上传过文档' : '暂无系统资料' }}
        </div>
        <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
          {{ activeTab === 'my' ? '上传 Markdown 或 TXT 文件，系统会自动切分并建立向量索引。' : '请联系管理员导入知识资料。' }}
        </p>
      </div>

      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <article v-for="doc in docs" :key="doc.id" class="metric-card">
          <div class="flex items-center justify-between gap-3">
            <h4 class="font-semibold">{{ doc.title }}</h4>
            <div class="flex items-center gap-2">
              <span
                class="hard-chip"
                :class="doc.status === 'indexed' ? '!bg-accent !text-white' : '!bg-white/80 dark:!bg-slate-700/80 !text-slate-600 dark:!text-slate-300'"
              >
                {{ doc.status }}
              </span>
              <el-popconfirm
                v-if="activeTab === 'my'"
                title="确认删除此文档？删除后关联的 chunk 和向量数据将一并清除。"
                confirm-button-text="删除"
                cancel-button-text="取消"
                @confirm="handleDelete(doc.id)"
              >
                <template #reference>
                  <el-button size="small" type="danger" plain>删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
          <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">{{ doc.categoryName || '未分配分类' }}</p>
          <p class="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300">{{ doc.summary || '暂无摘要' }}</p>
          <div class="mt-4 flex items-center justify-between text-xs uppercase tracking-[0.22em] text-slate-400 dark:text-slate-500">
            <span>chunks {{ doc.chunkCount ?? 0 }}</span>
            <span>{{ formatDate(doc.updateTime) }}</span>
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
    </section>

    <!-- Search Console -->
    <section class="paper-panel p-6">
      <div class="flex items-center justify-between">
        <div>
          <p class="section-kicker">Search Console</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">知识检索测试</h3>
        </div>
      </div>
      <div class="mt-4 flex flex-col gap-3 lg:flex-row">
        <el-input v-model="searchQuery" placeholder="例如：JVM 垃圾回收器分类" size="large" class="flex-1" />
        <el-button :loading="searching" type="primary" size="large" class="action-button" @click="runSearch">
          检索测试
        </el-button>
      </div>

      <div v-if="searchResult?.references.length" class="mt-6 space-y-3">
        <article v-for="reference in searchResult.references" :key="reference.chunkId" class="surface-card p-4">
          <div class="flex items-center justify-between">
            <div class="text-xs uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">{{ reference.docTitle }}</div>
            <span v-if="reference.score != null" class="rounded-full bg-accent/10 px-2 py-0.5 text-[10px] font-medium text-accent">
              {{ Math.round(reference.score * 100) }}%
            </span>
          </div>
          <p class="mt-3 text-sm leading-7 text-slate-700 dark:text-slate-200">{{ reference.snippet }}</p>
        </article>
      </div>
      <div v-else-if="searchResult" class="empty-state-card mt-6">
        <div class="font-semibold text-ink">没有找到相关结果</div>
        <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">尝试换一个关键词，或先上传相关文档。</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { fetchCategoriesApi } from '@/api/category'
import {
  fetchKnowledgeDocsApi,
  fetchMyKnowledgeDocsApi,
  searchKnowledgeApi,
  uploadKnowledgeDocApi,
  deleteKnowledgeDocApi,
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

const loadCategories = async () => {
  const response = await fetchCategoriesApi({ type: 'knowledge' })
  categories.value = response.data
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
    ElMessage.error('文件大小不能超过 5MB')
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
