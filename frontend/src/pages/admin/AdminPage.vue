<template>
  <div class="space-y-6">
    <AppShellHeader>
      <template #actions>
          <el-button
            :loading="exportingQuestions"
            size="large"
            class="hard-button-secondary !min-h-11 !px-5"
            @click="handleExportQuestions"
          >
            导出题库
          </el-button>
          <el-button
            :loading="exportingUsers"
            size="large"
            class="hard-button-primary !min-h-11 !px-5"
            @click="handleExportUsers"
          >
            导出用户
          </el-button>
      </template>
    </AppShellHeader>

    <section class="cockpit-panel p-5 sm:p-6">
      <div class="admin-tab-shell">
      <el-tabs v-model="activeTab" class="admin-tabs">
        <el-tab-pane label="系统概览" name="overview">
          <AdminOverviewTab />
        </el-tab-pane>

        <el-tab-pane label="用户管理" name="users">
          <AdminUserTab />
        </el-tab-pane>

        <el-tab-pane label="内容审核" name="contentReview">
          <AdminContentReviewTab />
        </el-tab-pane>

        <el-tab-pane label="分类管理" name="category">
          <AdminCategoryTab
            :categories="categories"
            :form="categoryForm"
            :saving="categorySaving"
            @save="saveCategory"
            @edit="editCategory"
            @remove="removeCategory"
            @reset="resetCategoryForm"
          />
        </el-tab-pane>

        <el-tab-pane label="题库管理" name="question">
          <AdminQuestionTab
            v-model:current-page="questionPage"
            v-model:importing="questionImporting"
            :questions="questions"
            :categories="questionCategories"
            :form="questionForm"
            :filter="questionFilter"
            :saving="questionSaving"
            :loading="questionLoading"
            :page-size="questionPageSize"
            :total="questionTotal"
            :total-pages="questionTotalPages"
            @save="saveQuestion"
            @edit="editQuestion"
            @remove="removeQuestion"
            @reset="resetQuestionForm"
            @filter-reset="resetQuestionFilter"
            @load="loadQuestions"
            @page-change="handleQuestionPageChange"
          />
        </el-tab-pane>

        <el-tab-pane label="文档管理" name="knowledge">
          <AdminKnowledgeTab
            v-model:current-page="knowledgePage"
            :docs="knowledgeDocs"
            :categories="knowledgeCategories"
            :filter="knowledgeFilter"
            :seeds="builtInSeeds"
            :loading="knowledgeLoading"
            :importing="knowledgeImporting"
            :action-id="knowledgeActionId"
            :page-size="knowledgePageSize"
            :total="knowledgeTotal"
            :total-pages="knowledgeTotalPages"
            @import="importSeed"
            @rechunk="rechunkDoc"
            @reindex="reindexDoc"
            @filter-reset="resetKnowledgeFilter"
            @load="loadKnowledgeDocs"
            @page-change="handleKnowledgePageChange"
          />
        </el-tab-pane>

        <el-tab-pane label="登录日志" name="loginLogs">
          <AdminLoginLogTab />
        </el-tab-pane>
      </el-tabs>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import AppShellHeader from '@/components/AppShellHeader.vue'
import AdminOverviewTab from './AdminOverviewTab.vue'
import AdminUserTab from './AdminUserTab.vue'
import AdminContentReviewTab from './AdminContentReviewTab.vue'
import AdminCategoryTab from './AdminCategoryTab.vue'
import AdminQuestionTab from './AdminQuestionTab.vue'
import AdminKnowledgeTab from './AdminKnowledgeTab.vue'
import AdminLoginLogTab from './AdminLoginLogTab.vue'
import { exportQuestionsApi, exportUsersApi } from '@/api/admin'
import { addCategoryApi, deleteCategoryApi, fetchCategoriesApi, updateCategoryApi } from '@/api/category'
import { fetchKnowledgeDocsApi, importKnowledgeSeedApi, rechunkKnowledgeDocApi, reindexKnowledgeDocApi } from '@/api/knowledge'
import { addQuestionApi, deleteQuestionApi, fetchQuestionsApi, updateQuestionApi } from '@/api/question'
import type { CategoryItem, KnowledgeDocItem, QuestionItem } from '@/types/api'

const activeTab = ref('overview')
const categories = ref<CategoryItem[]>([])
const questions = ref<QuestionItem[]>([])
const knowledgeDocs = ref<KnowledgeDocItem[]>([])

const categorySaving = ref(false)
const questionSaving = ref(false)
const questionLoading = ref(false)
const questionImporting = ref(false)
const knowledgeLoading = ref(false)
const exportingQuestions = ref(false)
const exportingUsers = ref(false)
const knowledgeImporting = ref<string | null>(null)
const knowledgeActionId = ref<string | null>(null)

const questionPage = ref(1)
const questionPageSize = ref(20)
const questionTotal = ref(0)
const questionTotalPages = ref(0)

const knowledgePage = ref(1)
const knowledgePageSize = ref(20)
const knowledgeTotal = ref(0)
const knowledgeTotalPages = ref(0)

const categoryForm = reactive<{ id?: number; name: string; type: CategoryItem['type']; sortOrder: number }>({ id: undefined, name: '', type: 'question', sortOrder: 0 })
const questionForm = reactive<{ id?: number; title: string; categoryId?: number; difficulty: QuestionItem['difficulty']; tags: string; standardAnswer: string }>({ id: undefined, title: '', categoryId: undefined, difficulty: 'medium', tags: '', standardAnswer: '' })
const questionFilter = reactive<{ categoryId?: number; difficulty?: QuestionItem['difficulty']; keyword: string }>({ categoryId: undefined, difficulty: undefined, keyword: '' })
const knowledgeFilter = reactive<{ categoryId?: number; status?: KnowledgeDocItem['status']; keyword: string }>({ categoryId: undefined, status: undefined, keyword: '' })

const questionCategories = computed(() => categories.value.filter((item) => item.type === 'question'))
const knowledgeCategories = computed(() => categories.value.filter((item) => item.type === 'knowledge'))

const builtInSeeds = [
  { seedKey: 'spring-core-notes', title: 'Spring 核心笔记', summary: 'IOC、AOP、事务与典型追问。' },
  { seedKey: 'jvm-interview-handbook', title: 'JVM 面试手册', summary: '内存区域、GC、类加载。' },
  { seedKey: 'mysql-high-frequency', title: 'MySQL 高频题', summary: '索引、事务、锁与执行计划。' }
]

const loadCategories = async () => { const r = await fetchCategoriesApi(); categories.value = r.data }
const loadQuestions = async () => { questionLoading.value = true; try { const r = await fetchQuestionsApi({ categoryId: questionFilter.categoryId, difficulty: questionFilter.difficulty, keyword: questionFilter.keyword || undefined, pageNum: questionPage.value, pageSize: questionPageSize.value }); questions.value = r.data.records; questionTotal.value = r.data.total; questionTotalPages.value = r.data.totalPages } catch { ElMessage.error('题库加载失败') } finally { questionLoading.value = false } }
const handleQuestionPageChange = (p: number) => { questionPage.value = p; void loadQuestions() }
const loadKnowledgeDocs = async () => { knowledgeLoading.value = true; try { const r = await fetchKnowledgeDocsApi({ categoryId: knowledgeFilter.categoryId, status: knowledgeFilter.status, keyword: knowledgeFilter.keyword || undefined, pageNum: knowledgePage.value, pageSize: knowledgePageSize.value }); knowledgeDocs.value = r.data.records; knowledgeTotal.value = r.data.total; knowledgeTotalPages.value = r.data.totalPages } catch { ElMessage.error('知识文档加载失败') } finally { knowledgeLoading.value = false } }
const handleKnowledgePageChange = (p: number) => { knowledgePage.value = p; void loadKnowledgeDocs() }

const saveCategory = async () => { if (!categoryForm.name.trim()) { ElMessage.warning('请输入分类名称'); return } categorySaving.value = true; try { if (categoryForm.id) { await updateCategoryApi(categoryForm); ElMessage.success('分类已更新') } else { await addCategoryApi(categoryForm); ElMessage.success('分类已新增') } resetCategoryForm(); await loadCategories() } catch { ElMessage.error('分类保存失败') } finally { categorySaving.value = false } }
const editCategory = (item: CategoryItem) => { categoryForm.id = item.id; categoryForm.name = item.name; categoryForm.type = item.type; categoryForm.sortOrder = item.sortOrder ?? 0; activeTab.value = 'category' }
const removeCategory = async (id: number) => { try { await deleteCategoryApi(id); ElMessage.success('分类已删除'); await loadCategories() } catch { ElMessage.error('删除分类失败') } }
const resetCategoryForm = () => { categoryForm.id = undefined; categoryForm.name = ''; categoryForm.type = 'question'; categoryForm.sortOrder = 0 }

const saveQuestion = async () => { if (!questionForm.title.trim() || !questionForm.categoryId) { ElMessage.warning('请补全题目标题和分类'); return } questionSaving.value = true; try { const p = { id: questionForm.id, title: questionForm.title, categoryId: questionForm.categoryId, difficulty: questionForm.difficulty, tags: questionForm.tags, standardAnswer: questionForm.standardAnswer }; if (questionForm.id) { await updateQuestionApi(p); ElMessage.success('题目已更新') } else { await addQuestionApi(p); ElMessage.success('题目已新增') } resetQuestionForm(); await loadQuestions() } catch { ElMessage.error('题目保存失败') } finally { questionSaving.value = false } }
const editQuestion = (item: QuestionItem) => { questionForm.id = item.id; questionForm.title = item.title; questionForm.categoryId = item.categoryId; questionForm.difficulty = item.difficulty; questionForm.tags = item.tags || ''; questionForm.standardAnswer = item.standardAnswer || ''; activeTab.value = 'question' }
const removeQuestion = async (id: number) => { try { await deleteQuestionApi(id); ElMessage.success('题目已删除'); await loadQuestions() } catch { ElMessage.error('删除题目失败') } }
const resetQuestionForm = () => { questionForm.id = undefined; questionForm.title = ''; questionForm.categoryId = undefined; questionForm.difficulty = 'medium'; questionForm.tags = ''; questionForm.standardAnswer = '' }
const resetQuestionFilter = () => { questionFilter.categoryId = undefined; questionFilter.difficulty = undefined; questionFilter.keyword = ''; void loadQuestions() }

const importSeed = async (seedKey: string) => { knowledgeImporting.value = seedKey; try { await importKnowledgeSeedApi({ seedKey }); ElMessage.success('知识资料已导入'); await Promise.all([loadCategories(), loadKnowledgeDocs()]) } catch { ElMessage.error('知识资料导入失败') } finally { knowledgeImporting.value = null } }
const rechunkDoc = async (id: number) => { knowledgeActionId.value = `rechunk-${id}`; try { await rechunkKnowledgeDocApi(id); ElMessage.success('文档已重新切分'); await loadKnowledgeDocs() } catch { ElMessage.error('重新切分失败') } finally { knowledgeActionId.value = null } }
const reindexDoc = async (id: number) => { knowledgeActionId.value = `reindex-${id}`; try { await reindexKnowledgeDocApi(id); ElMessage.success('索引已重建'); await loadKnowledgeDocs() } catch { ElMessage.error('重建索引失败') } finally { knowledgeActionId.value = null } }
const resetKnowledgeFilter = () => { knowledgeFilter.categoryId = undefined; knowledgeFilter.status = undefined; knowledgeFilter.keyword = ''; void loadKnowledgeDocs() }

const downloadBlob = (blob: BlobPart, filename: string) => {
  const url = URL.createObjectURL(blob instanceof Blob ? blob : new Blob([blob]))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

const handleExportQuestions = async () => {
  exportingQuestions.value = true
  try {
    const response = await exportQuestionsApi()
    downloadBlob(response.data as unknown as BlobPart, `bytecoach-questions-${Date.now()}.xlsx`)
    ElMessage.success('题库导出成功')
  } catch {
    ElMessage.error('题库导出失败')
  } finally {
    exportingQuestions.value = false
  }
}

const handleExportUsers = async () => {
  exportingUsers.value = true
  try {
    const response = await exportUsersApi()
    downloadBlob(response.data as unknown as BlobPart, `bytecoach-users-${Date.now()}.xlsx`)
    ElMessage.success('用户导出成功')
  } catch {
    ElMessage.error('用户导出失败')
  } finally {
    exportingUsers.value = false
  }
}

onMounted(async () => { await loadCategories(); await Promise.all([loadQuestions(), loadKnowledgeDocs()]) })
</script>

<style scoped>
.module-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.module-topbar__title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}

.module-topbar__heading {
  color: var(--bc-ink);
  font-size: 1.2rem;
  font-weight: 700;
  line-height: 1.2;
}

.module-topbar__action {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.admin-panel-heading__title {
  color: var(--bc-ink);
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.03em;
}

.admin-tab-shell {
  border-radius: 28px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.26);
  padding: 12px;
}

.dark .admin-tab-shell {
  background: rgba(255, 255, 255, 0.04);
}

:deep(.admin-tabs .el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.admin-tabs .el-tabs__item) {
  min-height: 44px;
  color: var(--bc-ink-secondary);
  font-weight: 600;
}

:deep(.admin-tabs .el-tabs__item.is-active) {
  color: var(--bc-ink);
}

:deep(.admin-tabs .el-tabs__active-bar) {
  background: var(--bc-accent);
}

@media (max-width: 1023px) {
  .module-topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .module-topbar__action {
    justify-content: flex-start;
  }
}
</style>
