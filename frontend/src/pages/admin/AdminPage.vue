<template>
  <div class="space-y-6">
    <section class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div class="max-w-3xl">
          <div class="flex items-center gap-3">
            <span class="state-pulse" aria-hidden="true"></span>
            <p class="section-kicker">管理后台</p>
          </div>
          <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">先切到对应模块，再直接执行管理动作</h2>
          <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
            这里集中处理概览、用户、审核、分类、题库、文档和登录日志。优先使用下方标签进入具体管理区。
          </p>
        </div>
        <div class="flex flex-wrap gap-2">
          <el-button :loading="exportingQuestions" size="large" class="hard-button-secondary" @click="handleExportQuestions">导出题库</el-button>
          <el-button :loading="exportingUsers" size="large" class="hard-button-primary" @click="handleExportUsers">导出用户</el-button>
        </div>
      </div>

      <div class="mt-6 grid gap-3 md:grid-cols-3">
        <article v-for="signal in adminSignals" :key="signal.label" class="data-slab p-4" :class="signal.toneClass">
          <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ signal.label }}</p>
          <p class="mt-3 text-xl font-semibold text-ink">{{ signal.title }}</p>
          <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ signal.detail }}</p>
        </article>
      </div>
    </section>

    <section class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="section-kicker">管理模块</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">{{ currentTabMeta.title }}</h3>
          <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">{{ currentTabMeta.description }}</p>
        </div>
      </div>

      <section class="admin-tab-shell mt-6">
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
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
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

const adminSignals = [
  {
    label: '用户',
    title: '用户与权限',
    detail: '查找用户、调整角色、处理封禁与详情查看。',
    toneClass: '',
  },
  {
    label: '内容',
    title: '审核与题库',
    detail: '快速处理待审核内容，并维护分类、题目和知识文档。',
    toneClass: 'admin-slab-cyan',
  },
  {
    label: '导出',
    title: '数据可留档',
    detail: '题库和用户数据都可以直接导出，用于留档或排查。',
    toneClass: 'admin-slab-lime',
  },
]

const tabMeta = {
  overview: { title: '系统概览', description: '先看核心指标和近 30 天趋势。' },
  users: { title: '用户管理', description: '优先搜索用户，再处理角色、状态和详情。' },
  contentReview: { title: '内容审核', description: '按提交时间扫描待审核内容，快速通过或拒绝。' },
  category: { title: '分类管理', description: '左侧编辑，右侧查看现有分类列表。' },
  question: { title: '题库管理', description: '先筛选题目，再决定编辑、删除或批量导入。' },
  knowledge: { title: '文档管理', description: '先筛选文档，再执行导入、重切分和重建索引。' },
  loginLogs: { title: '登录日志', description: '按用户名、时间和状态排查后台登录情况。' },
} as const

const currentTabMeta = computed(() => tabMeta[activeTab.value as keyof typeof tabMeta] ?? tabMeta.overview)

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
.admin-slab-cyan {
  border-left-color: var(--bc-cyan);
}

.admin-slab-lime {
  border-left-color: var(--bc-lime);
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

:deep(.admin-tabs > .el-tabs__header) {
  margin-bottom: 0;
  padding: 0 8px;
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
</style>
