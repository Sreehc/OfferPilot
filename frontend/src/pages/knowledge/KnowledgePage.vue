<template>
  <div class="space-y-6">
    <section class="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
      <div class="paper-panel p-6">
        <p class="section-kicker">Knowledge Library</p>
        <h3 class="mt-4 text-3xl font-semibold tracking-[-0.03em] text-ink">内置资料已接入真实列表与检索</h3>
        <p class="mt-4 text-sm leading-7 text-slate-600">
          当前阶段仍然只使用仓库内置资料，但已经支持分类查看、关键字检索和引用片段测试，为后续 RAG 问答提供最小数据底座。
        </p>
      </div>
      <div class="paper-panel p-6">
        <p class="section-kicker">Search Console</p>
        <div class="mt-4 flex flex-col gap-3">
          <el-input v-model="searchQuery" placeholder="例如：JVM 垃圾回收器分类" size="large" />
          <el-button :loading="searching" type="primary" size="large" class="action-button" @click="runSearch">
            检索测试
          </el-button>
        </div>
      </div>
    </section>

    <section class="paper-panel p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="section-kicker">Document Filters</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">按分类和状态查看知识文档</h3>
        </div>
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
      </div>
      <div class="mt-4 flex gap-3">
        <el-button :loading="loadingDocs" type="primary" size="large" class="action-button" @click="loadDocs">
          刷新列表
        </el-button>
        <el-button size="large" class="hard-button-secondary" @click="resetFilters">重置筛选</el-button>
      </div>
    </section>

    <section>
      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <article v-for="doc in docs" :key="doc.id" class="metric-card">
          <div class="flex items-center justify-between gap-3">
            <h4 class="font-semibold">{{ doc.title }}</h4>
            <span class="hard-chip" :class="doc.status === 'indexed' ? '!bg-accent !text-white' : '!bg-white/80 !text-slate-600'">
              {{ doc.status }}
            </span>
          </div>
          <p class="mt-2 text-sm text-slate-500">{{ doc.categoryName || '未分配分类' }}</p>
          <p class="mt-3 text-sm leading-6 text-slate-600">{{ doc.summary || '暂无摘要' }}</p>
          <div class="mt-4 flex items-center justify-between text-xs uppercase tracking-[0.22em] text-slate-400">
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

    <section class="paper-panel p-6">
      <div class="flex items-center justify-between">
        <div>
          <p class="section-kicker">Search Hits</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">检索返回的引用片段</h3>
        </div>
        <div class="text-sm text-slate-500">{{ searchResult?.references.length ?? 0 }} hits</div>
      </div>

      <div v-if="searchResult?.references.length" class="mt-6 space-y-3">
        <article v-for="reference in searchResult.references" :key="reference.chunkId" class="surface-card p-4">
          <div class="text-xs uppercase tracking-[0.24em] text-slate-500">{{ reference.docTitle }}</div>
          <p class="mt-3 text-sm leading-7 text-slate-700">{{ reference.snippet }}</p>
          <div class="mt-3 text-xs text-slate-400">doc {{ reference.docId }} / chunk {{ reference.chunkId }}</div>
        </article>
      </div>
      <div v-else class="empty-state-card mt-6">
        <div class="font-semibold text-ink">还没有检索结果</div>
        <p class="mt-2 text-sm leading-6 text-slate-500">先输入一个具体的 Java 后端主题，例如 “Spring AOP” 或 “MySQL 索引失效”。</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { fetchCategoriesApi } from '@/api/category'
import { fetchKnowledgeDocsApi, searchKnowledgeApi } from '@/api/knowledge'
import type { CategoryItem, KnowledgeDocItem, KnowledgeSearchResult } from '@/types/api'

const categories = ref<CategoryItem[]>([])
const docs = ref<KnowledgeDocItem[]>([])
const searchResult = ref<KnowledgeSearchResult | null>(null)
const loadingDocs = ref(false)
const searching = ref(false)
const searchQuery = ref('JVM 垃圾回收器分类')
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
  status: undefined
})

const loadCategories = async () => {
  const response = await fetchCategoriesApi({ type: 'knowledge' })
  categories.value = response.data
}

const loadDocs = async () => {
  loadingDocs.value = true
  try {
    const response = await fetchKnowledgeDocsApi({
      categoryId: filters.categoryId,
      keyword: filters.keyword || undefined,
      status: filters.status,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    docs.value = response.data.records
    total.value = response.data.total
    totalPages.value = response.data.totalPages
  } catch {
    ElMessage.error('知识文档加载失败')
  } finally {
    loadingDocs.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  void loadDocs()
}

const runSearch = async () => {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('请输入检索问题')
    return
  }
  searching.value = true
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
    minute: '2-digit'
  }).format(new Date(value))
}

onMounted(async () => {
  await loadCategories()
  await loadDocs()
})
</script>
