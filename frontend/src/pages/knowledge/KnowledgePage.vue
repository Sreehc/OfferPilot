<template>
  <div class="knowledge-cockpit space-y-5">
    <AppShellHeader>
      <template #actions>
        <el-upload
          :show-file-list="false"
          :before-upload="handleBeforeUpload"
          :http-request="handleUpload"
          accept=".md,.markdown,.txt,.text,.pdf"
        >
          <el-button :loading="uploading" type="primary" size="large" class="action-button !min-h-11 !px-5">
            {{ uploading ? '上传中...' : '添加文档' }}
          </el-button>
        </el-upload>
      </template>
    </AppShellHeader>

    <section class="knowledge-workspace">
      <section class="shell-section-card overflow-hidden knowledge-table-shell">
        <div class="knowledge-table-head border-b border-slate-200/70 px-5 py-4 dark:border-slate-700/70">
          <div class="knowledge-table-head__main">
            <p class="section-kicker">文档</p>
            <div class="mode-switch mode-switch-compact mt-3">
              <button
                type="button"
                class="mode-switch__chip"
                :class="{ 'mode-switch__chip-active': activeTab === 'system' }"
                @click="switchTab('system')"
              >
                系统资料
              </button>
              <button
                type="button"
                class="mode-switch__chip"
                :class="{ 'mode-switch__chip-active': activeTab === 'my' }"
                @click="switchTab('my')"
              >
                我的文档
              </button>
            </div>
            <h3 class="mt-4 text-2xl font-semibold tracking-[-0.03em] text-ink">共 {{ total }} 份文档</h3>
          </div>
          <div class="knowledge-table-head__aside">
            <div class="knowledge-table-stat">
              <span>当前列表</span>
              <strong>{{ docs.length }}</strong>
            </div>
            <div class="knowledge-table-stat">
              <span>可检索</span>
              <strong>{{ statusSummary.indexed }}</strong>
            </div>
          </div>
        </div>

        <div v-if="docs.length === 0 && !loadingDocs" class="p-5">
          <EmptyState
            class="empty-state-card"
            icon="document"
            :title="activeTab === 'my' ? '你还没有上传文档' : '系统资料暂时为空'"
            :description="activeTab === 'my' ? '上传文档。' : '等待管理员导入资料。'"
          />
        </div>

        <div v-else class="knowledge-list">
          <article
            v-for="doc in docs"
            :key="doc.id"
            class="knowledge-row"
            :class="statusToneClass(doc.status)"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="flex min-w-0 items-start gap-3">
                <div
                  class="doc-card__glyph"
                  :class="docType(doc.fileUrl) === 'pdf' ? 'doc-card__glyph-pdf' : 'doc-card__glyph-text'"
                >
                  <svg
                    v-if="docType(doc.fileUrl) === 'pdf'"
                    class="h-4 w-4"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    stroke-width="1.6"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"
                    />
                  </svg>
                  <svg v-else class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.6">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"
                    />
                  </svg>
                </div>
                <div class="min-w-0">
                  <span
                    class="inline-flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.18em]"
                    :class="statusTextClass(doc.status)"
                  >
                    <span class="h-2 w-2 rounded-full" :class="statusDotClass(doc.status)"></span>
                    {{ statusLabel(doc.status) }}
                  </span>
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
            <div class="knowledge-row__body">
              <div class="knowledge-row__summary">
                <p class="line-clamp-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
                  {{ doc.summary || '暂无摘要。' }}
                </p>
              </div>

              <div class="doc-card__memory">
                <div class="doc-card__memory-meta">
                  <template v-if="doc.cardDeckId">
                    <strong>{{ doc.cardDeckTitle || doc.title }}</strong>
                    <span>已生成 {{ doc.cardCount ?? 0 }} 张卡片。</span>
                  </template>
                  <template v-else>
                    <strong>未生成卡片</strong>
                    <span>从这份资料生成第一组卡片。</span>
                  </template>
                </div>

                <div class="doc-card__memory-actions">
                  <RouterLink
                    v-if="doc.cardDeckId"
                    to="/cards"
                    class="hard-button-primary text-sm"
                  >
                    去今日卡片
                  </RouterLink>
                  <button
                    v-else
                    type="button"
                    class="hard-button-primary text-sm"
                    @click="openGeneratePanel(doc)"
                  >
                    生成卡片
                  </button>
                </div>
              </div>
            </div>
          </article>
        </div>
        <div v-if="totalPages > 1" class="border-t border-slate-200/70 px-5 py-5 dark:border-slate-700/70">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </section>

      <aside class="knowledge-side">
        <section class="shell-section-card p-5 sm:p-6">
          <p class="section-kicker">上传</p>
          <div class="upload-dropzone mt-5" @dragover.prevent @drop.prevent="handleDrop">
            <div class="upload-dropzone__copy">
              <div class="upload-dropzone__icon" aria-hidden="true">
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M12 16V4m0 0l-4 4m4-4l4 4M4 15v2a3 3 0 003 3h10a3 3 0 003-3v-2"
                  />
                </svg>
              </div>
              <div class="min-w-0">
                <p class="text-sm font-semibold text-ink">拖拽文档到这里上传</p>
                <p class="mt-1 text-sm text-slate-600 dark:text-slate-300">
                  支持 <span class="font-semibold text-ink">md / txt / pdf</span>，单文件不超过 20MB。
                </p>
              </div>
            </div>

            <div class="upload-dropzone__aside">
              <div class="flex flex-wrap gap-2 text-[11px] text-slate-500 dark:text-slate-400">
                <span class="hard-chip">当前 {{ docs.length }} 份</span>
                <span class="rounded-full border border-[var(--bc-line)] px-2.5 py-1">可检索 {{ statusSummary.indexed }}</span>
              </div>
            </div>
          </div>
        </section>

        <section class="shell-section-card p-5 sm:p-6">
          <p class="section-kicker">筛选与状态</p>
          <div class="mt-5 grid gap-3">
            <el-select v-model="filters.categoryId" clearable placeholder="知识分类" size="large">
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
            <el-select v-model="filters.status" clearable placeholder="文档状态" size="large">
              <el-option label="草稿" value="draft" />
              <el-option label="已解析" value="parsed" />
              <el-option label="已索引" value="indexed" />
            </el-select>
            <el-input v-model="filters.keyword" clearable placeholder="搜索标题或摘要" size="large" />
          </div>
          <div class="knowledge-tool-actions mt-4">
            <el-button
              :loading="loadingDocs"
              type="primary"
              size="large"
              class="action-button knowledge-tool-button"
              @click="loadDocs"
            >
              刷新
            </el-button>
            <el-button size="large" class="hard-button-secondary knowledge-tool-button !ml-0" @click="resetFilters">
              重置
            </el-button>
          </div>
          <div class="mt-5 space-y-3">
            <div class="knowledge-status-row">
              <span>草稿</span>
              <strong>{{ statusSummary.draft }}</strong>
            </div>
            <div class="knowledge-status-row">
              <span>已解析</span>
              <strong>{{ statusSummary.parsed }}</strong>
            </div>
            <div class="knowledge-status-row">
              <span>已索引</span>
              <strong>{{ statusSummary.indexed }}</strong>
            </div>
          </div>
        </section>
      </aside>
    </section>

    <CardGeneratePanel
      :visible="showGeneratePanel"
      :doc-id="generateDocId"
      :doc-title="generateDocTitle"
      :decks="[]"
      @close="showGeneratePanel = false"
      @generated="handleGenerated"
      @activated="handleGenerated"
    />
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import AppShellHeader from '@/components/AppShellHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import CardGeneratePanel from '@/pages/cards/CardGeneratePanel.vue'
import { fetchCategoriesApi } from '@/api/category'
import { deleteKnowledgeDocApi, fetchKnowledgeDocsApi, fetchMyKnowledgeDocsApi, uploadKnowledgeDocApi } from '@/api/knowledge'
import type { CategoryItem, KnowledgeDocItem } from '@/types/api'
const categories = ref<CategoryItem[]>([])
const docs = ref<KnowledgeDocItem[]>([])
const loadingDocs = ref(false)
const uploading = ref(false)
const showGeneratePanel = ref(false)
const generateDocId = ref(0)
const generateDocTitle = ref('')
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
  status: undefined
})

const statusSummary = computed(() => {
  const summary = { draft: 0, parsed: 0, indexed: 0 }
  docs.value.forEach((doc) => {
    summary[doc.status] += 1
  })
  return summary
})

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
      pageSize: pageSize.value
    }
    const response =
      activeTab.value === 'my' ? await fetchMyKnowledgeDocsApi(params) : await fetchKnowledgeDocsApi(params)
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

const statusDotClass = (status: KnowledgeDocItem['status']) => {
  const map: Record<KnowledgeDocItem['status'], string> = {
    draft: 'bg-[var(--bc-coral)]',
    parsed: 'bg-[var(--bc-amber)]',
    indexed: 'bg-[var(--bc-cyan)]'
  }
  return map[status]
}

const statusTextClass = (status: KnowledgeDocItem['status']) => {
  const map: Record<KnowledgeDocItem['status'], string> = {
    draft: 'text-[var(--bc-coral)]',
    parsed: 'text-[var(--bc-amber)]',
    indexed: 'text-[var(--bc-cyan)]'
  }
  return map[status]
}

const statusToneClass = (status: KnowledgeDocItem['status']) => {
  const map: Record<KnowledgeDocItem['status'], string> = {
    draft: 'doc-card-danger',
    parsed: 'doc-card-warm',
    indexed: 'doc-card-cyan'
  }
  return map[status]
}

const docType = (fileUrl?: string): 'pdf' | 'text' => {
  if (!fileUrl) return 'text'
  return fileUrl.toLowerCase().endsWith('.pdf') ? 'pdf' : 'text'
}

const openGeneratePanel = (doc: KnowledgeDocItem) => {
  generateDocId.value = doc.id
  generateDocTitle.value = doc.title
  showGeneratePanel.value = true
}

const handleGenerated = () => {
  void loadDocs()
}

onMounted(async () => {
  await loadCategories()
  await loadDocs()
})
</script>

<style scoped>
.module-topbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px 18px;
}

.module-topbar__title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: max-content;
}

.module-topbar__heading {
  color: var(--bc-ink);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.1;
}

.module-topbar__center {
  display: flex;
  flex: 1;
  min-width: min(100%, 320px);
}

.module-topbar__action {
  min-width: max-content;
}

.knowledge-workspace {
  display: grid;
  gap: 18px;
}

.knowledge-table-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px 20px;
}

.knowledge-table-head__main {
  min-width: 0;
}

.knowledge-table-head__aside {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.knowledge-table-stat {
  display: grid;
  gap: 4px;
  min-width: 108px;
  padding: 10px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.38);
}

.dark .knowledge-table-stat {
  background: rgba(255, 255, 255, 0.05);
}

.knowledge-table-stat span {
  color: var(--bc-ink-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.knowledge-table-stat strong {
  color: var(--bc-ink);
  font-size: 1.125rem;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.knowledge-table-shell {
  border-top-left-radius: 24px;
  border-top-right-radius: 24px;
  min-width: 0;
}

.knowledge-side {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.knowledge-tool-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.knowledge-tool-button {
  width: 100%;
  min-height: 44px;
  padding-inline: 20px;
}

.knowledge-cockpit .mode-switch {
  min-width: min(100%, 260px);
}

.docs-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.mode-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  width: min(100%, 440px);
}

.mode-switch-compact {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 10px;
  width: auto;
}

.mode-switch__chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  border: 1px solid var(--bc-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.42);
  padding: 0 16px;
  color: var(--bc-ink-secondary);
  font-size: 13px;
  font-weight: 600;
  transition:
    border-color var(--motion-base) var(--ease-hard),
    color var(--motion-base) var(--ease-hard),
    background-color var(--motion-base) var(--ease-hard);
}

.dark .mode-switch__chip {
  background: rgba(255, 255, 255, 0.05);
}

.mode-switch__chip-active {
  border-color: rgba(var(--bc-accent-rgb), 0.28);
  color: var(--bc-ink);
  background: rgba(var(--bc-accent-rgb), 0.1);
}

.mode-switch__item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
  gap: 4px;
  min-height: 92px;
  padding: 14px 16px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.12);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(255, 255, 255, 0.56));
  color: var(--bc-ink-secondary);
  text-align: left;
  transition:
    background-color var(--motion-base) var(--ease-hard),
    border-color var(--motion-base) var(--ease-hard),
    transform var(--motion-base) var(--ease-hard),
    color var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard);
}

.dark .mode-switch__item {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.03));
}

.mode-switch__item:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--bc-accent-rgb), 0.2);
  color: var(--bc-ink);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
}

.mode-switch__item-active {
  border-color: rgba(var(--bc-accent-rgb), 0.3);
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.18), transparent 48%),
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.16), rgba(255, 255, 255, 0.92));
  color: var(--bc-ink);
  box-shadow:
    inset 0 0 0 1px rgba(var(--bc-accent-rgb), 0.14),
    0 16px 28px rgba(var(--bc-accent-rgb), 0.12);
}

.mode-switch__eyebrow {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--bc-ink-tertiary);
}

.mode-switch__title {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--bc-ink);
}

.mode-switch__hint {
  font-size: 12px;
  line-height: 1.5;
  color: var(--bc-ink-secondary);
}

.upload-dropzone {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 18px 20px;
  border: 1px dashed rgba(var(--bc-accent-rgb), 0.34);
  border-radius: 26px;
  padding: 20px 22px;
  background:
    radial-gradient(circle at left top, rgba(var(--bc-accent-rgb), 0.11), transparent 44%), rgba(255, 255, 255, 0.28);
  transition:
    border-color var(--motion-base) var(--ease-hard),
    transform var(--motion-base) var(--ease-hard),
    background-color var(--motion-base) var(--ease-hard);
}

.dark .upload-dropzone {
  background:
    radial-gradient(circle at left top, rgba(var(--bc-accent-rgb), 0.16), transparent 44%), rgba(255, 255, 255, 0.04);
}

.upload-dropzone:hover {
  border-color: var(--bc-line-hot);
  transform: translateY(-1px);
}

.upload-dropzone__copy {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.upload-dropzone__aside {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  width: 100%;
}

.upload-dropzone__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  flex-shrink: 0;
  color: #101826;
  background: linear-gradient(180deg, #ffe0ad 0%, var(--bc-amber) 100%);
  box-shadow: 0 14px 24px rgba(var(--bc-accent-rgb), 0.18);
}

.doc-card {
  min-height: 100%;
}

.knowledge-list {
  display: flex;
  flex-direction: column;
}

.knowledge-row {
  padding: 20px;
}

.knowledge-row + .knowledge-row {
  border-top: 1px solid rgba(148, 163, 184, 0.16);
}

.knowledge-row__body {
  display: grid;
  gap: 16px;
  margin-top: 16px;
}

.knowledge-row__summary {
  min-width: 0;
}

.knowledge-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.knowledge-status-row span {
  color: var(--bc-ink-secondary);
  font-size: 13px;
}

.knowledge-status-row strong {
  color: var(--bc-ink);
  font-size: 1rem;
  font-weight: 700;
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

.doc-card__memory {
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.1), transparent 62%),
    rgba(255, 255, 255, 0.24);
  padding: 14px;
}

.dark .doc-card__memory {
  background:
    linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.11), transparent 62%),
    rgba(255, 255, 255, 0.04);
}

.doc-card__memory-meta {
  display: grid;
  gap: 6px;
}

.doc-card__memory-kicker {
  color: var(--bc-ink-secondary);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.doc-card__memory-meta strong {
  color: var(--bc-ink);
  font-size: 1rem;
  font-weight: 780;
  letter-spacing: -0.03em;
}

.doc-card__memory-meta span {
  color: rgb(100 116 139);
  font-size: 13px;
  line-height: 1.7;
}

.doc-card__memory-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.reference-card {
  border: 1px solid rgba(85, 214, 190, 0.2);
  border-radius: var(--radius-md);
  background: linear-gradient(145deg, rgba(85, 214, 190, 0.08), transparent 36%), var(--bc-surface-card);
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
  .module-topbar__center {
    order: 3;
    flex-basis: 100%;
    min-width: 0;
  }

  .module-topbar__heading {
    font-size: 24px;
  }

  .docs-toolbar {
    width: 100%;
    justify-content: flex-start;
  }

  .docs-toolbar > * {
    width: 100%;
  }

  .mode-switch {
    grid-template-columns: minmax(0, 1fr);
  }

  .upload-dropzone {
    align-items: flex-start;
    padding-inline: 16px;
  }

  .upload-dropzone__copy {
    align-items: flex-start;
  }

  .upload-dropzone__aside {
    width: 100%;
    align-items: flex-start;
  }

  .knowledge-tool-actions {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (min-width: 1200px) {
  .knowledge-workspace {
    grid-template-columns: minmax(0, 1.45fr) 320px;
    align-items: start;
  }

  .knowledge-side {
    position: sticky;
    top: 88px;
  }

  .knowledge-row__body {
    grid-template-columns: minmax(0, 1fr) 300px;
    align-items: start;
  }
}
</style>
