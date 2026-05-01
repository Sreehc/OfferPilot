<template>
  <div class="space-y-6">
    <section class="paper-panel p-6">
      <p class="section-kicker">Admin Workspace</p>
      <h2 class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink">题库、文档和分类已经进入最小运营态</h2>
      <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-600">
        当前后台不拆独立应用，先在同一个工作台里完成分类管理、题库 CRUD、内置知识导入、切分和重建索引。
      </p>
    </section>

    <section class="paper-panel p-6">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="分类管理" name="category">
          <div class="grid gap-4 xl:grid-cols-[320px_minmax(0,1fr)]">
            <div class="surface-muted p-4">
              <div class="text-sm font-semibold text-ink">{{ categoryForm.id ? '编辑分类' : '新增分类' }}</div>
              <div class="mt-4 space-y-3">
                <el-input v-model="categoryForm.name" placeholder="分类名称" size="large" />
                <el-select v-model="categoryForm.type" placeholder="分类类型" size="large" class="w-full">
                  <el-option label="question" value="question" />
                  <el-option label="knowledge" value="knowledge" />
                  <el-option label="interview" value="interview" />
                </el-select>
                <el-input-number v-model="categoryForm.sortOrder" :min="0" class="w-full" />
              </div>
              <div class="mt-4 flex gap-3">
                <el-button :loading="categorySaving" type="primary" class="action-button" @click="saveCategory">
                  {{ categoryForm.id ? '保存修改' : '新增分类' }}
                </el-button>
                <el-button class="hard-button-secondary" @click="resetCategoryForm">重置</el-button>
              </div>
            </div>

            <div class="space-y-3">
              <article v-for="item in categories" :key="item.id" class="surface-card p-4">
                <div class="flex items-start justify-between gap-3">
                  <div>
                    <div class="font-semibold text-ink">{{ item.name }}</div>
                    <div class="mt-1 text-xs uppercase tracking-[0.22em] text-slate-500">{{ item.type }}</div>
                  </div>
                  <div class="flex gap-2">
                    <button type="button" class="accent-link text-sm font-semibold" @click="editCategory(item)">编辑</button>
                    <button type="button" class="text-sm text-slate-500 transition hover:text-red-500" @click="removeCategory(item.id)">删除</button>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="题库管理" name="question">
          <div class="grid gap-4 xl:grid-cols-[380px_minmax(0,1fr)]">
            <div class="surface-muted p-4">
              <div class="text-sm font-semibold text-ink">{{ questionForm.id ? '编辑题目' : '新增题目' }}</div>
              <div class="mt-4 space-y-3">
                <el-input v-model="questionForm.title" placeholder="题目标题" size="large" />
                <el-select v-model="questionForm.categoryId" placeholder="题目分类" size="large" class="w-full">
                  <el-option v-for="item in questionCategories" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
                <el-select v-model="questionForm.difficulty" placeholder="难度" size="large" class="w-full">
                  <el-option label="easy" value="easy" />
                  <el-option label="medium" value="medium" />
                  <el-option label="hard" value="hard" />
                </el-select>
                <el-input v-model="questionForm.tags" placeholder="标签，例如：Spring,AOP" size="large" />
                <el-input v-model="questionForm.standardAnswer" type="textarea" :rows="5" placeholder="标准答案" />
              </div>
              <div class="mt-4 flex gap-3">
                <el-button :loading="questionSaving" type="primary" class="action-button" @click="saveQuestion">
                  {{ questionForm.id ? '保存修改' : '新增题目' }}
                </el-button>
                <el-button class="hard-button-secondary" @click="resetQuestionForm">重置</el-button>
              </div>
            </div>

            <div class="space-y-4">
              <div class="grid gap-3 md:grid-cols-3">
                <el-select v-model="questionFilter.categoryId" clearable placeholder="分类筛选" size="large">
                  <el-option v-for="item in questionCategories" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
                <el-select v-model="questionFilter.difficulty" clearable placeholder="难度筛选" size="large">
                  <el-option label="easy" value="easy" />
                  <el-option label="medium" value="medium" />
                  <el-option label="hard" value="hard" />
                </el-select>
                <el-input v-model="questionFilter.keyword" clearable placeholder="关键字" size="large" />
              </div>

              <div class="flex gap-3">
                <el-button :loading="questionLoading" type="primary" class="action-button" @click="loadQuestions">刷新题库</el-button>
                <el-button class="hard-button-secondary" @click="resetQuestionFilter">重置筛选</el-button>
              </div>

              <article v-for="item in questions" :key="item.id" class="surface-card p-4">
                <div class="flex items-start justify-between gap-4">
                  <div class="min-w-0">
                    <div class="font-semibold text-ink">{{ item.title }}</div>
                    <div class="mt-2 flex flex-wrap gap-2 text-xs uppercase tracking-[0.2em] text-slate-500">
                      <span>{{ item.categoryName }}</span>
                      <span>{{ item.difficulty }}</span>
                    </div>
                    <p class="mt-3 text-sm leading-6 text-slate-600">{{ item.standardAnswer || '暂无标准答案' }}</p>
                  </div>
                  <div class="flex shrink-0 gap-2">
                    <button type="button" class="accent-link text-sm font-semibold" @click="editQuestion(item)">编辑</button>
                    <button type="button" class="text-sm text-slate-500 transition hover:text-red-500" @click="removeQuestion(item.id)">删除</button>
                  </div>
                </div>
              </article>

              <div v-if="questionTotalPages > 1" class="mt-4 flex justify-center">
                <el-pagination
                  v-model:current-page="questionPage"
                  :page-size="questionPageSize"
                  :total="questionTotal"
                  layout="prev, pager, next"
                  @current-change="handleQuestionPageChange"
                />
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="文档管理" name="knowledge">
          <div class="grid gap-4 xl:grid-cols-[360px_minmax(0,1fr)]">
            <div class="surface-muted p-4">
              <div class="text-sm font-semibold text-ink">导入内置知识资料</div>
              <div class="mt-4 space-y-3">
                <div v-for="seed in builtInSeeds" :key="seed.seedKey" class="surface-card p-4">
                  <div class="font-semibold text-ink">{{ seed.title }}</div>
                  <p class="mt-2 text-sm leading-6 text-slate-600">{{ seed.summary }}</p>
                  <div class="mt-3 flex items-center justify-between">
                    <span class="text-xs uppercase tracking-[0.2em] text-slate-400">{{ seed.seedKey }}</span>
                    <el-button :loading="knowledgeImporting === seed.seedKey" type="primary" class="action-button !min-h-9 !px-3" @click="importSeed(seed.seedKey)">
                      导入
                    </el-button>
                  </div>
                </div>
              </div>
            </div>

            <div class="space-y-4">
              <div class="grid gap-3 md:grid-cols-3">
                <el-select v-model="knowledgeFilter.categoryId" clearable placeholder="知识分类" size="large">
                  <el-option v-for="item in knowledgeCategories" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
                <el-select v-model="knowledgeFilter.status" clearable placeholder="状态筛选" size="large">
                  <el-option label="draft" value="draft" />
                  <el-option label="parsed" value="parsed" />
                  <el-option label="indexed" value="indexed" />
                </el-select>
                <el-input v-model="knowledgeFilter.keyword" clearable placeholder="标题/摘要关键字" size="large" />
              </div>

              <div class="flex gap-3">
                <el-button :loading="knowledgeLoading" type="primary" class="action-button" @click="loadKnowledgeDocs">刷新文档</el-button>
                <el-button class="hard-button-secondary" @click="resetKnowledgeFilter">重置筛选</el-button>
              </div>

              <article v-for="doc in knowledgeDocs" :key="doc.id" class="surface-card p-4">
                <div class="flex items-start justify-between gap-4">
                  <div>
                    <div class="font-semibold text-ink">{{ doc.title }}</div>
                    <div class="mt-2 flex flex-wrap gap-2 text-xs uppercase tracking-[0.2em] text-slate-500">
                      <span>{{ doc.categoryName || '未分配分类' }}</span>
                      <span>{{ doc.status }}</span>
                      <span>chunks {{ doc.chunkCount ?? 0 }}</span>
                    </div>
                    <p class="mt-3 text-sm leading-6 text-slate-600">{{ doc.summary || '暂无摘要' }}</p>
                  </div>
                  <div class="flex shrink-0 gap-2">
                    <el-button :loading="knowledgeActionId === `rechunk-${doc.id}`" class="hard-button-secondary !min-h-9 !px-3" @click="rechunkDoc(doc.id)">
                      重切分
                    </el-button>
                    <el-button :loading="knowledgeActionId === `reindex-${doc.id}`" type="primary" class="action-button !min-h-9 !px-3" @click="reindexDoc(doc.id)">
                      重建索引
                    </el-button>
                  </div>
                </div>
              </article>

              <div v-if="knowledgeTotalPages > 1" class="mt-4 flex justify-center">
                <el-pagination
                  v-model:current-page="knowledgePage"
                  :page-size="knowledgePageSize"
                  :total="knowledgeTotal"
                  layout="prev, pager, next"
                  @current-change="handleKnowledgePageChange"
                />
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
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
