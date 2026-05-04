<template>
  <div class="space-y-6">
    <section class="paper-panel p-6">
      <p class="section-kicker">管理后台</p>
      <h2 class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink">题库、文档和分类已经进入最小运营态</h2>
      <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-600 dark:text-slate-300">
        当前后台不拆独立应用，先在同一个工作台里完成分类管理、题库 CRUD、内置知识导入、切分和重建索引。
      </p>
    </section>

    <section class="paper-panel p-6">
      <el-tabs v-model="activeTab">
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
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import AdminCategoryTab from './AdminCategoryTab.vue'
import AdminQuestionTab from './AdminQuestionTab.vue'
import AdminKnowledgeTab from './AdminKnowledgeTab.vue'
import AdminLoginLogTab from './AdminLoginLogTab.vue'
import { addCategoryApi, deleteCategoryApi, fetchCategoriesApi, updateCategoryApi } from '@/api/category'
import { fetchKnowledgeDocsApi, importKnowledgeSeedApi, rechunkKnowledgeDocApi, reindexKnowledgeDocApi } from '@/api/knowledge'
import { addQuestionApi, deleteQuestionApi, fetchQuestionsApi, updateQuestionApi } from '@/api/question'
import type { CategoryItem, KnowledgeDocItem, QuestionItem } from '@/types/api'

const activeTab = ref('category')
const categories = ref<CategoryItem[]>([])
const questions = ref<QuestionItem[]>([])
const knowledgeDocs = ref<KnowledgeDocItem[]>([])

const categorySaving = ref(false)
const questionSaving = ref(false)
const questionLoading = ref(false)
const knowledgeLoading = ref(false)
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

const categoryForm = reactive<{
  id?: number
  name: string
  type: CategoryItem['type']
  sortOrder: number
}>({
  id: undefined,
  name: '',
  type: 'question',
  sortOrder: 0
})

const questionForm = reactive<{
  id?: number
  title: string
  categoryId?: number
  difficulty: QuestionItem['difficulty']
  tags: string
  standardAnswer: string
}>({
  id: undefined,
  title: '',
  categoryId: undefined,
  difficulty: 'medium',
  tags: '',
  standardAnswer: ''
})

const questionFilter = reactive<{
  categoryId?: number
  difficulty?: QuestionItem['difficulty']
  keyword: string
}>({
  categoryId: undefined,
  difficulty: undefined,
  keyword: ''
})

const knowledgeFilter = reactive<{
  categoryId?: number
  status?: KnowledgeDocItem['status']
  keyword: string
}>({
  categoryId: undefined,
  status: undefined,
  keyword: ''
})

const questionCategories = computed(() => categories.value.filter((item) => item.type === 'question'))
const knowledgeCategories = computed(() => categories.value.filter((item) => item.type === 'knowledge'))

const builtInSeeds = [
  { seedKey: 'spring-core-notes', title: 'Spring 核心笔记', summary: 'IOC、AOP、事务与典型追问。' },
  { seedKey: 'jvm-interview-handbook', title: 'JVM 面试手册', summary: '内存区域、GC、类加载。' },
  { seedKey: 'mysql-high-frequency', title: 'MySQL 高频题', summary: '索引、事务、锁与执行计划。' }
]

const loadCategories = async () => {
  const response = await fetchCategoriesApi()
  categories.value = response.data
}

const loadQuestions = async () => {
  questionLoading.value = true
  try {
    const response = await fetchQuestionsApi({
      categoryId: questionFilter.categoryId,
      difficulty: questionFilter.difficulty,
      keyword: questionFilter.keyword || undefined,
      pageNum: questionPage.value,
      pageSize: questionPageSize.value
    })
    questions.value = response.data.records
    questionTotal.value = response.data.total
    questionTotalPages.value = response.data.totalPages
  } catch {
    ElMessage.error('题库加载失败')
  } finally {
    questionLoading.value = false
  }
}

const handleQuestionPageChange = (page: number) => {
  questionPage.value = page
  void loadQuestions()
}

const loadKnowledgeDocs = async () => {
  knowledgeLoading.value = true
  try {
    const response = await fetchKnowledgeDocsApi({
      categoryId: knowledgeFilter.categoryId,
      status: knowledgeFilter.status,
      keyword: knowledgeFilter.keyword || undefined,
      pageNum: knowledgePage.value,
      pageSize: knowledgePageSize.value
    })
    knowledgeDocs.value = response.data.records
    knowledgeTotal.value = response.data.total
    knowledgeTotalPages.value = response.data.totalPages
  } catch {
    ElMessage.error('知识文档加载失败')
  } finally {
    knowledgeLoading.value = false
  }
}

const handleKnowledgePageChange = (page: number) => {
  knowledgePage.value = page
  void loadKnowledgeDocs()
}

const saveCategory = async () => {
  if (!categoryForm.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  categorySaving.value = true
  try {
    if (categoryForm.id) {
      await updateCategoryApi(categoryForm)
      ElMessage.success('分类已更新')
    } else {
      await addCategoryApi(categoryForm)
      ElMessage.success('分类已新增')
    }
    resetCategoryForm()
    await loadCategories()
  } catch {
    ElMessage.error('分类保存失败')
  } finally {
    categorySaving.value = false
  }
}

const editCategory = (item: CategoryItem) => {
  categoryForm.id = item.id
  categoryForm.name = item.name
  categoryForm.type = item.type
  categoryForm.sortOrder = item.sortOrder ?? 0
  activeTab.value = 'category'
}

const removeCategory = async (id: number) => {
  try {
    await deleteCategoryApi(id)
    ElMessage.success('分类已删除')
    await loadCategories()
  } catch {
    ElMessage.error('删除分类失败')
  }
}

const resetCategoryForm = () => {
  categoryForm.id = undefined
  categoryForm.name = ''
  categoryForm.type = 'question'
  categoryForm.sortOrder = 0
}

const saveQuestion = async () => {
  if (!questionForm.title.trim() || !questionForm.categoryId) {
    ElMessage.warning('请补全题目标题和分类')
    return
  }
  questionSaving.value = true
  try {
    const payload = {
      id: questionForm.id,
      title: questionForm.title,
      categoryId: questionForm.categoryId,
      difficulty: questionForm.difficulty,
      tags: questionForm.tags,
      standardAnswer: questionForm.standardAnswer
    }
    if (questionForm.id) {
      await updateQuestionApi(payload)
      ElMessage.success('题目已更新')
    } else {
      await addQuestionApi(payload)
      ElMessage.success('题目已新增')
    }
    resetQuestionForm()
    await loadQuestions()
  } catch {
    ElMessage.error('题目保存失败')
  } finally {
    questionSaving.value = false
  }
}

const editQuestion = (item: QuestionItem) => {
  questionForm.id = item.id
  questionForm.title = item.title
  questionForm.categoryId = item.categoryId
  questionForm.difficulty = item.difficulty
  questionForm.tags = item.tags || ''
  questionForm.standardAnswer = item.standardAnswer || ''
  activeTab.value = 'question'
}

const removeQuestion = async (id: number) => {
  try {
    await deleteQuestionApi(id)
    ElMessage.success('题目已删除')
    await loadQuestions()
  } catch {
    ElMessage.error('删除题目失败')
  }
}

const resetQuestionForm = () => {
  questionForm.id = undefined
  questionForm.title = ''
  questionForm.categoryId = undefined
  questionForm.difficulty = 'medium'
  questionForm.tags = ''
  questionForm.standardAnswer = ''
}

const resetQuestionFilter = () => {
  questionFilter.categoryId = undefined
  questionFilter.difficulty = undefined
  questionFilter.keyword = ''
  void loadQuestions()
}

const importSeed = async (seedKey: string) => {
  knowledgeImporting.value = seedKey
  try {
    await importKnowledgeSeedApi({ seedKey })
    ElMessage.success('知识资料已导入')
    await Promise.all([loadCategories(), loadKnowledgeDocs()])
  } catch {
    ElMessage.error('知识资料导入失败')
  } finally {
    knowledgeImporting.value = null
  }
}

const rechunkDoc = async (id: number) => {
  knowledgeActionId.value = `rechunk-${id}`
  try {
    await rechunkKnowledgeDocApi(id)
    ElMessage.success('文档已重新切分')
    await loadKnowledgeDocs()
  } catch {
    ElMessage.error('重新切分失败')
  } finally {
    knowledgeActionId.value = null
  }
}

const reindexDoc = async (id: number) => {
  knowledgeActionId.value = `reindex-${id}`
  try {
    await reindexKnowledgeDocApi(id)
    ElMessage.success('索引已重建')
    await loadKnowledgeDocs()
  } catch {
    ElMessage.error('重建索引失败')
  } finally {
    knowledgeActionId.value = null
  }
}

const resetKnowledgeFilter = () => {
  knowledgeFilter.categoryId = undefined
  knowledgeFilter.status = undefined
  knowledgeFilter.keyword = ''
  void loadKnowledgeDocs()
}

onMounted(async () => {
  await loadCategories()
  await Promise.all([loadQuestions(), loadKnowledgeDocs()])
})
</script>
