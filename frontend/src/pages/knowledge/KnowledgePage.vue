<template>
  <div class="knowledge-cockpit space-y-4">
    <section class="shell-section-card workspace-shell knowledge-shell">
        <div class="workspace-head">
          <div class="workspace-head__top">
            <div class="workspace-head__main">
              <div class="flex flex-wrap items-center gap-2">
                <h1 class="workspace-title">知识库</h1>
                <span class="detail-pill">{{ activeTabLabel }}</span>
              </div>
              <p class="workspace-summary">先看哪类资料，再上传新文档；优先处理可提问和处理中这两类内容。</p>
            </div>
            <div class="knowledge-table-head__tools">
              <div class="mode-switch mode-switch-compact">
                <button
                  type="button"
                  class="mode-switch__chip"
                  :class="{ 'mode-switch__chip-active': activeTab === 'system' }"
                  @click="switchTab('system')"
                >
                  推荐资料
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
              <el-upload
                :show-file-list="false"
                :before-upload="handleBeforeUpload"
                :http-request="handleUpload"
                accept=".md,.markdown,.txt,.text,.pdf,.doc,.docx"
              >
                <el-button :loading="uploading" type="primary" size="default" class="action-button !px-4">
                  {{ uploading ? '上传中...' : '添加文档' }}
                </el-button>
              </el-upload>
            </div>
          </div>
          <div class="knowledge-table-head__stats">
            <div class="knowledge-table-stat">
              <span>当前文档数</span>
              <strong>{{ total }}</strong>
            </div>
            <div class="knowledge-table-stat">
              <span>可以提问</span>
              <strong>{{ statusSummary.indexed }}</strong>
            </div>
            <div class="knowledge-table-stat">
              <span>处理中</span>
              <strong>{{ statusSummary.pending }}</strong>
            </div>
          </div>
        </div>

        <div class="workspace-separator"></div>

        <div class="knowledge-workspace">
          <section class="knowledge-table-shell">

          <div v-if="docs.length === 0 && !loadingDocs" class="p-5">
            <EmptyState
              class="empty-state-card"
              icon="document"
              :title="activeTab === 'my' ? EMPTY_STATE_COPY.knowledgePersonal.title : EMPTY_STATE_COPY.knowledgeRecommended.title"
              :description="
                activeTab === 'my'
                  ? EMPTY_STATE_COPY.knowledgePersonal.description
                  : EMPTY_STATE_COPY.knowledgeRecommended.description
              "
            />
          </div>

          <div v-else class="knowledge-list">
            <article v-for="doc in docs" :key="doc.id" class="knowledge-row" :class="statusToneClass(doc.status)">
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
                    <svg
                      v-else
                      class="h-4 w-4"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="1.6"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"
                      />
                    </svg>
                  </div>
                  <div class="min-w-0">
                    <div class="flex flex-wrap items-center gap-2">
                      <span
                        class="inline-flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.18em]"
                        :class="statusTextClass(doc.status)"
                      >
                        <span class="h-2 w-2 rounded-full" :class="statusDotClass(doc.status)"></span>
                        {{ statusLabel(doc.status) }}
                      </span>
                      <span class="detail-pill">{{ libraryScopeLabel(doc.libraryScope) }}</span>
                      <span v-if="doc.businessType" class="detail-pill">{{ businessTypeLabel(doc.businessType) }}</span>
                      <span v-if="doc.fileType" class="detail-pill">{{ doc.fileType.toUpperCase() }}</span>
                    </div>
                    <h4 class="mt-3 text-lg font-semibold leading-7 text-ink">{{ doc.title }}</h4>
                  </div>
                </div>

                <div class="flex shrink-0 items-center gap-2">
                  <span v-if="doc.status === 'indexed' || doc.status === 'parsed'" class="detail-pill">可以提问</span>
                  <button
                    type="button"
                    class="favorites-toggle"
                    :class="isFavorited(doc.id) ? 'favorites-toggle-active' : ''"
                    @click="toggleFavorite(doc)"
                  >
                    <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"
                      />
                    </svg>
                  </button>
                  <el-popconfirm
                    v-if="activeTab === 'my'"
                    title="确认删除此文档？删除后相关的提问记录也会一并清除。"
                    confirm-button-text="删除"
                    cancel-button-text="取消"
                    @confirm="handleDelete(doc.id)"
                  >
                    <template #reference>
                      <button type="button" class="doc-card__danger">删除</button>
                    </template>
                  </el-popconfirm>
                </div>
              </div>
              <div class="knowledge-row__body">
                <div class="knowledge-row__summary">
                  <p class="text-sm leading-7 text-secondary">
                    {{ doc.summary || '这份资料的摘要还在整理中，先看标题和标签判断是否要继续处理。' }}
                  </p>
                  <div class="mt-3 flex flex-wrap gap-2 text-xs text-secondary">
                    <span class="detail-pill">{{ availabilityLabel(doc) }}</span>
                    <span class="detail-pill">文件解析 {{ parseStatusLabel(doc.parseStatus) }}</span>
                    <span class="detail-pill">问答就绪 {{ indexStatusLabel(doc.indexStatus) }}</span>
                    <span v-if="doc.categoryName" class="detail-pill">{{ doc.categoryName }}</span>
                  </div>
                </div>

                <div class="doc-card__memory">
                  <div class="doc-card__memory-meta">
                    <strong>{{ doc.title }}</strong>
                    <span>{{ availabilityHint(doc) }}</span>
                  </div>

                  <div class="doc-card__memory-actions">
                    <RouterLink v-if="canAskWithDocument(doc)" :to="knowledgeChatTarget(doc)" class="hard-button-primary text-sm">
                      去提问
                    </RouterLink>
                    <span v-else class="detail-pill">{{ unavailableActionHint(doc) }}</span>
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
            <section class="knowledge-side-panel">
              <div class="upload-dropzone" @dragover.prevent @drop.prevent="handleDrop">
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
                  <p class="text-sm font-semibold text-ink">把资料拖到这里，或点击右上角上传</p>
                  <p class="mt-1 text-sm text-secondary">支持 md / txt / pdf / doc / docx，单文件不超过 20MB。</p>
                </div>
              </div>

              <div class="upload-dropzone__aside">
                <div class="flex flex-wrap gap-2 text-[11px] text-secondary">
                  <span class="hard-chip">当前 {{ docs.length }} 份</span>
                  <span class="rounded-full border border-[var(--bc-line)] px-2.5 py-1"
                    >可以提问 {{ statusSummary.indexed }}</span
                  >
                </div>
              </div>
              </div>
            </section>

            <section class="knowledge-side-panel knowledge-side-panel--divider">
              <h3 class="workspace-section-title">筛选资料</h3>
              <p class="workspace-section-summary">直接筛出现在能用的资料。</p>
              <div class="mt-4 grid gap-3">
              <el-select v-model="filters.categoryId" clearable placeholder="知识分类" size="large">
                <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
              <el-select v-model="filters.libraryScope" clearable placeholder="资料范围" size="large">
                <el-option label="推荐资料" value="system" />
                <el-option label="我的文档" value="personal" />
              </el-select>
              <el-select v-model="filters.businessType" clearable placeholder="业务归属" size="large">
                <el-option label="平台资料" value="system_knowledge" />
                <el-option label="个人笔记" value="user_note" />
                <el-option label="简历资料" value="resume" />
                <el-option label="JD 资料" value="jd" />
                <el-option label="项目资料" value="project_doc" />
              </el-select>
              <el-select v-model="filters.fileType" clearable placeholder="文件类型" size="large">
                <el-option label="Markdown" value="md" />
                <el-option label="TXT" value="txt" />
                <el-option label="PDF" value="pdf" />
                <el-option label="DOC" value="doc" />
                <el-option label="DOCX" value="docx" />
              </el-select>
              <el-select v-model="filters.parseStatus" clearable placeholder="文件解析状态" size="large">
                <el-option label="处理中" value="pending" />
                <el-option label="已就绪" value="parsed" />
                <el-option label="处理失败" value="failed" />
              </el-select>
              <el-select v-model="filters.indexStatus" clearable placeholder="问答就绪状态" size="large">
                <el-option label="处理中" value="pending" />
                <el-option label="已就绪" value="indexed" />
                <el-option label="处理失败" value="failed" />
              </el-select>
              <el-select v-model="filters.status" clearable placeholder="资料状态" size="large">
                <el-option label="处理中" value="draft" />
                <el-option label="待提问" value="parsed" />
                <el-option label="可以提问" value="indexed" />
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
                应用筛选
              </el-button>
              <el-button size="large" class="hard-button-secondary knowledge-tool-button !ml-0" @click="resetFilters">
                重置
              </el-button>
              </div>
            </section>
          </aside>
        </div>
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
  uploadKnowledgeDocApi
} from '@/api/knowledge'
import { addFavoriteApi, removeFavoriteApi, fetchFavoriteListApi } from '@/api/favorites'
import { EMPTY_STATE_COPY, ERROR_COPY, KNOWLEDGE_STATUS_NAMES } from '@/constants/productCopy'
import type { CategoryItem, KnowledgeDocItem } from '@/types/api'
import { buildKnowledgeChatTarget, canAskWithKnowledgeDocument } from './knowledgeTargets'

const categories = ref<CategoryItem[]>([])
const docs = ref<KnowledgeDocItem[]>([])
const loadingDocs = ref(false)
const uploading = ref(false)
const activeTab = ref<'system' | 'my'>('system')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)
const filters = reactive<{
  categoryId?: number
  libraryScope?: KnowledgeDocItem['libraryScope']
  businessType?: string
  fileType?: string
  parseStatus?: KnowledgeDocItem['parseStatus']
  indexStatus?: KnowledgeDocItem['indexStatus']
  keyword: string
  status?: KnowledgeDocItem['status']
}>({
  categoryId: undefined,
  libraryScope: undefined,
  businessType: undefined,
  fileType: undefined,
  parseStatus: undefined,
  indexStatus: undefined,
  keyword: '',
  status: undefined
})

const statusSummary = computed(() => {
  const summary = { draft: 0, parsed: 0, indexed: 0, pending: 0 }
  docs.value.forEach((doc) => {
    summary[doc.status] += 1
    if (doc.status !== 'indexed') {
      summary.pending += 1
    }
  })
  return summary
})

const activeTabLabel = computed(() => (activeTab.value === 'my' ? '我的文档' : '推荐资料'))

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
      libraryScope: filters.libraryScope || (activeTab.value === 'my' ? 'personal' : 'system'),
      businessType: filters.businessType,
      fileType: filters.fileType,
      parseStatus: filters.parseStatus,
      indexStatus: filters.indexStatus,
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
    ElMessage.error(ERROR_COPY.knowledgeListLoadFailed)
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
    ElMessage.success('文档已上传，处理完成后就可以继续提问了')
    activeTab.value = 'my'
    currentPage.value = 1
    await loadDocs()
  } catch {
    ElMessage.error(ERROR_COPY.knowledgeUploadFailed)
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
    ElMessage.error(ERROR_COPY.knowledgeDeleteFailed)
  }
}

const resetFilters = () => {
  filters.categoryId = undefined
  filters.libraryScope = undefined
  filters.businessType = undefined
  filters.fileType = undefined
  filters.parseStatus = undefined
  filters.indexStatus = undefined
  filters.keyword = ''
  filters.status = undefined
  currentPage.value = 1
  void loadDocs()
}

const statusLabel = (status: KnowledgeDocItem['status']) => {
  const map: Record<KnowledgeDocItem['status'], string> = KNOWLEDGE_STATUS_NAMES
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

const libraryScopeLabel = (scope?: string) => {
  return scope === 'personal' ? '个人库' : '推荐库'
}

const businessTypeLabel = (type?: string) => {
  if (type === 'user_note') return '个人笔记'
  if (type === 'resume') return '简历资料'
  if (type === 'jd') return 'JD 资料'
  if (type === 'project_doc') return '项目资料'
  return '平台资料'
}

const parseStatusLabel = (status?: string) => {
  if (status === 'failed') return '处理失败'
  if (status === 'parsed') return '已就绪'
  return '处理中'
}

const indexStatusLabel = (status?: string) => {
  if (status === 'failed') return '处理失败'
  if (status === 'indexed') return '已就绪'
  return '处理中'
}

const availabilityLabel = (doc: KnowledgeDocItem) => {
  if (doc.status === 'indexed') return '可以提问'
  if (doc.parseStatus === 'failed' || doc.indexStatus === 'failed') return '这份资料处理失败'
  if (doc.parseStatus !== 'parsed') return '这份资料还在处理中'
  return '这份资料待提问'
}

const availabilityHint = (doc: KnowledgeDocItem) => {
  if (doc.status === 'indexed') return '可以直接进入问答，继续围绕这份资料追问。'
  if (doc.parseStatus === 'failed') return '这份资料处理失败，修复后才能继续提问。'
  if (doc.indexStatus === 'failed') return '这份资料索引失败，修复后才能继续提问。'
  if (doc.parseStatus !== 'parsed') return '资料处理完成后，就可以继续提问。'
  return '索引完成后，这份资料就能继续提问。'
}

const canAskWithDocument = (doc: KnowledgeDocItem) => canAskWithKnowledgeDocument(doc)

const unavailableActionHint = (doc: KnowledgeDocItem) => {
  if (doc.parseStatus === 'failed' || doc.indexStatus === 'failed') return '修复后再提问'
  if (doc.parseStatus !== 'parsed') return '处理完成后可提问'
  return '索引完成后可提问'
}

const knowledgeChatTarget = (doc: KnowledgeDocItem) => buildKnowledgeChatTarget(doc)

const docType = (fileUrl?: string): 'pdf' | 'text' => {
  if (!fileUrl) return 'text'
  return fileUrl.toLowerCase().endsWith('.pdf') ? 'pdf' : 'text'
}

const favoriteIds = ref(new Set<number>())

const loadFavoriteIds = async () => {
  try {
    const { data } = await fetchFavoriteListApi({ targetType: 'knowledge', pageSize: 500 })
    favoriteIds.value = new Set(data.records.map((f) => f.targetId))
  } catch {
    // silent
  }
}

const isFavorited = (docId: number) => favoriteIds.value.has(docId)

const toggleFavorite = async (doc: KnowledgeDocItem) => {
  try {
    if (isFavorited(doc.id)) {
      const { data } = await fetchFavoriteListApi({ targetType: 'knowledge', pageSize: 500 })
      const fav = data.records.find((f) => f.targetId === doc.id)
      if (fav) await removeFavoriteApi(fav.id)
      favoriteIds.value.delete(doc.id)
      ElMessage.success('已取消收藏')
    } else {
      await addFavoriteApi({ targetType: 'knowledge', targetId: doc.id })
      favoriteIds.value.add(doc.id)
      ElMessage.success('已收藏')
    }
  } catch {
    ElMessage.error('收藏状态还没更新成功，请稍后再试')
  }
}

onMounted(async () => {
  await loadCategories()
  await Promise.all([loadDocs(), loadFavoriteIds()])
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

.knowledge-shell {
  padding: 0;
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

.knowledge-table-head__top {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px 16px;
}

.knowledge-table-head__tools {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.knowledge-table-head__stats {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.knowledge-table-stat {
  display: grid;
  gap: 4px;
  min-width: 108px;
  padding: 10px 14px;
  border-radius: 16px;
  background: var(--panel-muted);
}

.dark .knowledge-table-stat {
  background: var(--panel-muted);
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
  min-width: 0;
}

.knowledge-workspace {
  display: grid;
  min-width: 0;
}

.knowledge-side {
  display: grid;
  gap: 18px;
  min-width: 0;
  padding: 1rem 1.25rem;
}

.knowledge-side-panel {
  min-width: 0;
}

.knowledge-side-panel--divider {
  padding-top: 18px;
  border-top: 1px solid var(--bc-border-subtle);
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
  background: var(--interactive-bg);
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
  background: var(--interactive-bg);
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
  background: var(--interactive-bg);
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
  background: var(--interactive-bg);
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
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.18), transparent 48%), var(--interactive-hover);
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
    radial-gradient(circle at left top, rgba(var(--bc-accent-rgb), 0.11), transparent 44%), var(--panel-muted);
  transition:
    border-color var(--motion-base) var(--ease-hard),
    transform var(--motion-base) var(--ease-hard),
    background-color var(--motion-base) var(--ease-hard);
}

.dark .upload-dropzone {
  background:
    radial-gradient(circle at left top, rgba(var(--bc-accent-rgb), 0.16), transparent 44%), var(--panel-muted);
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
  background: linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.1), transparent 62%), var(--panel-muted);
  padding: 14px;
}

.dark .doc-card__memory {
  background: linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.11), transparent 62%), var(--panel-muted);
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

  .knowledge-table-head__tools {
    width: 100%;
    align-items: flex-start;
  }

  .knowledge-side {
    padding-top: 0;
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

@media (min-width: 768px) {
  .knowledge-table-head__stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
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
    padding: 1rem 1.25rem;
    border-left: 1px solid var(--bc-border-subtle);
  }

  .knowledge-row__body {
    grid-template-columns: minmax(0, 1fr) 300px;
    align-items: start;
  }

  .knowledge-table-head__stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

.favorites-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--bc-ink-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
}

.favorites-toggle:hover {
  background: rgba(var(--bc-accent-rgb), 0.1);
  color: var(--bc-accent);
}

.favorites-toggle-active {
  color: var(--bc-accent);
}

.favorites-toggle-active svg {
  fill: var(--bc-accent);
}
</style>
