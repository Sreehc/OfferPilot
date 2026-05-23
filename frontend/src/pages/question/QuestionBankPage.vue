<template>
  <div class="question-bank-page">
    <section class="shell-section-card workspace-shell overflow-hidden">
      <div class="workspace-head">
        <div class="workspace-head__top">
          <div class="workspace-head__main">
            <h1 class="workspace-title">题库训练</h1>
            <p class="workspace-summary">先筛到要练的题，先看清题目和答案，再决定继续追问还是开始表达练习。</p>
          </div>
          <div v-if="questions.length" class="question-toolbar__summary text-xs text-tertiary">
            <span>共 <strong class="font-semibold text-ink">{{ total }}</strong> 题</span>
            <span>当前页 {{ questions.length }} 题</span>
            <span v-if="hardQuestionCount">困难 {{ hardQuestionCount }}</span>
            <span v-if="taggedQuestionCount">带标签 {{ taggedQuestionCount }}</span>
          </div>
        </div>

        <div class="question-toolbar__filters mt-4">
          <el-input
            v-model="filters.keyword"
            clearable
            size="default"
            placeholder="搜索题目标题、标签、答案…"
            class="question-toolbar__search"
            @keyup.enter="applyFilters"
          />
          <el-select v-model="filters.categoryId" clearable size="default" placeholder="分类" class="w-28">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="filters.difficulty" clearable size="default" placeholder="难度" class="w-24">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
          <button type="button" class="filter-chip" @click="applyFilters">搜索</button>
          <button type="button" class="filter-chip filter-chip--muted" @click="resetFilters">重置</button>
          <button type="button" class="filter-chip filter-chip--muted" @click="showAdvancedFilters = !showAdvancedFilters">
            {{ showAdvancedFilters ? '收起筛选' : '更多筛选' }}
          </button>
        </div>

        <div v-if="showAdvancedFilters" class="question-toolbar__advanced mt-3">
          <el-select v-model="filters.type" clearable size="default" placeholder="题型" class="w-28">
            <el-option label="八股题" value="concept" />
            <el-option label="场景题" value="scenario" />
            <el-option label="项目题" value="project" />
            <el-option label="算法题" value="coding" />
          </el-select>
          <el-input
            v-model="filters.jobDirection"
            clearable
            size="default"
            placeholder="岗位方向"
            class="w-36"
            @keyup.enter="applyFilters"
          />
          <el-input
            v-model="filters.tag"
            clearable
            size="default"
            placeholder="标签，如 Redis / 并发"
            class="w-44"
            @keyup.enter="applyFilters"
          />
        </div>
      </div>

      <div class="workspace-separator"></div>

      <div v-if="loading" class="workspace-section min-h-[260px] text-center">
        <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent" />
        <p class="mt-4 text-sm text-secondary">加载题库中...</p>
      </div>

      <div v-else class="workspace-section question-list-section">
        <div v-if="questions.length" class="space-y-4">
          <article
            v-for="item in questions"
            :key="item.id"
            class="question-card p-5 sm:p-6"
          >
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-tertiary">
                <span
                  class="question-difficulty"
                  :class="difficultyToneClass(item.difficulty)"
                >
                  {{ difficultyLabel(item.difficulty) }}
                </span>
                <span v-if="item.type">{{ questionTypeLabel(item.type) }}</span>
                <span v-if="item.categoryName">{{ item.categoryName }}</span>
                <span v-if="item.jobDirection">{{ item.jobDirection }}</span>
              </div>
              <h3 class="mt-3 text-xl font-semibold leading-8 text-ink">
                {{ item.title }}
              </h3>
            </div>

            <div class="flex shrink-0 gap-2">
              <button
                type="button"
                class="favorites-toggle"
                :class="isFavorited(item.id) ? 'favorites-toggle-active' : ''"
                @click="toggleFavorite(item)"
              >
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                </svg>
              </button>
              <button
                type="button"
                class="hard-button-primary text-sm"
                @click="openDetail(item)"
              >
                先看这道题
              </button>
            </div>
          </div>

          <div
            v-if="tagList(item.tags).length"
            class="mt-4 flex flex-wrap gap-2"
          >
            <span
              v-for="tag in tagList(item.tags).slice(0, 5)"
              :key="tag"
              class="question-tag"
            >{{ tag }}</span>
          </div>

          <p class="mt-5 text-sm leading-7 text-secondary">
            {{ answerPreview(item.standardAnswer) }}
          </p>
          </article>
        </div>

        <section v-else class="py-2">
          <EmptyState
            icon="search"
            title="当前筛选下没有题目"
            description="换个分类、标签或关键词，再继续筛一轮。"
          />
        </section>

        <section v-if="totalPages > 1" class="workspace-separator mt-5 px-1 pt-4">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </section>
      </div>
    </section>

    <el-drawer
      v-model="detailVisible"
      title="题目准备"
      size="min(720px, 100%)"
    >
      <template v-if="selectedQuestion">
        <div class="space-y-5">
          <section>
            <div class="flex flex-wrap gap-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-tertiary">
              <span class="question-difficulty" :class="difficultyToneClass(selectedQuestion.difficulty)">
                {{ difficultyLabel(selectedQuestion.difficulty) }}
              </span>
              <span v-if="selectedQuestion.type">{{ questionTypeLabel(selectedQuestion.type) }}</span>
              <span v-if="selectedQuestion.categoryName">{{ selectedQuestion.categoryName }}</span>
              <span v-if="selectedQuestion.jobDirection">{{ selectedQuestion.jobDirection }}</span>
            </div>
            <h3 class="mt-3 text-2xl font-semibold leading-9 text-ink">
              {{ selectedQuestion.title }}
            </h3>
            <p
              v-if="selectedQuestion.applicableScope || selectedQuestion.source"
              class="mt-3 text-sm text-secondary"
            >
              {{ selectedQuestion.applicableScope || '先把核心思路讲清楚，再补来源和扩展资料。' }}
              <span v-if="selectedQuestion.source"> · 来源：{{ selectedQuestion.source }}</span>
            </p>

            <div class="mt-5 flex flex-wrap gap-3">
              <RouterLink
                :to="questionChatTarget(selectedQuestion)"
                class="hard-button-secondary text-sm"
              >
                去问答页继续追问
              </RouterLink>
              <RouterLink
                :to="questionInterviewTarget(selectedQuestion)"
                class="hard-button-primary text-sm"
              >
                开始表达练习
              </RouterLink>
            </div>
          </section>

          <section
            v-if="tagList(selectedQuestion.tags).length"
            class="flex flex-wrap gap-2"
          >
            <span
              v-for="tag in tagList(selectedQuestion.tags)"
              :key="tag"
              class="question-tag"
            >{{ tag }}</span>
          </section>

          <section class="question-detail-block">
            <span class="question-detail-title">1. 先看标准答案</span>
            <p>{{ selectedQuestion.standardAnswer || '暂无标准答案。' }}</p>
          </section>

          <section
            v-if="selectedQuestion.interviewAnswer"
            class="question-detail-block"
          >
            <span class="question-detail-title">2. 再看表达示例</span>
            <p>{{ selectedQuestion.interviewAnswer }}</p>
          </section>

          <section
            v-if="selectedQuestion.followUpSuggestions"
            class="question-detail-block"
          >
            <span class="question-detail-title">3. 再决定怎么继续追问</span>
            <p>{{ selectedQuestion.followUpSuggestions }}</p>
          </section>

          <section
            v-if="selectedQuestion.commonMistakes"
            class="question-detail-block question-detail-block-danger"
          >
            <span class="question-detail-title">避免这样回答</span>
            <p>{{ selectedQuestion.commonMistakes }}</p>
          </section>

          <section
            v-if="selectedQuestion.scoreStandard"
            class="question-detail-block"
          >
            <span class="question-detail-title">4. 最后对照评分标准</span>
            <p>{{ selectedQuestion.scoreStandard }}</p>
          </section>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'
import { fetchCategoriesApi } from '@/api/category'
import { fetchQuestionsApi } from '@/api/question'
import { addFavoriteApi, removeFavoriteApi, fetchFavoriteListApi } from '@/api/favorites'
import type { CategoryItem, QuestionItem } from '@/types/api'

const route = useRoute()
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 12
const total = ref(0)
const totalPages = ref(0)
const categories = ref<CategoryItem[]>([])
const questions = ref<QuestionItem[]>([])
const detailVisible = ref(false)
const selectedQuestion = ref<QuestionItem | null>(null)
const showAdvancedFilters = ref(false)

const filters = reactive<{
  categoryId?: number
  type?: string
  difficulty?: QuestionItem['difficulty']
  jobDirection: string
  tag: string
  keyword: string
}>({
  categoryId: undefined,
  type: undefined,
  difficulty: undefined,
  jobDirection: '',
  tag: '',
  keyword: ''
})

const hardQuestionCount = computed(() => questions.value.filter((item) => item.difficulty === 'hard').length)
const taggedQuestionCount = computed(() => questions.value.filter((item) => tagList(item.tags).length > 0).length)

const loadCategories = async () => {
  try {
    const { data } = await fetchCategoriesApi({ type: 'question' })
    categories.value = data
  } catch {
    categories.value = []
  }
}

const loadQuestions = async () => {
  loading.value = true
  try {
    const { data } = await fetchQuestionsApi({
      categoryId: filters.categoryId,
      type: filters.type || undefined,
      difficulty: filters.difficulty,
      jobDirection: filters.jobDirection.trim() || undefined,
      tag: filters.tag.trim() || undefined,
      keyword: filters.keyword.trim() || undefined,
      pageNum: currentPage.value,
      pageSize
    })
    questions.value = data.records
    total.value = data.total
    totalPages.value = data.totalPages
  } catch {
    questions.value = []
    total.value = 0
    totalPages.value = 0
    ElMessage.error('题库暂时没加载出来，请调整筛选后重试')
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  currentPage.value = 1
  void loadQuestions()
}

const resetFilters = () => {
  filters.categoryId = undefined
  filters.type = undefined
  filters.difficulty = undefined
  filters.jobDirection = ''
  filters.tag = ''
  filters.keyword = ''
  currentPage.value = 1
  void loadQuestions()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  void loadQuestions()
}

const difficultyLabel = (difficulty?: string) => {
  if (difficulty === 'easy') return '简单'
  if (difficulty === 'hard') return '困难'
  return '中等'
}

const difficultyToneClass = (difficulty?: string) => {
  if (difficulty === 'easy') return 'question-difficulty-easy'
  if (difficulty === 'hard') return 'question-difficulty-hard'
  return 'question-difficulty-medium'
}

const questionTypeLabel = (type?: string) => {
  if (type === 'concept') return '八股题'
  if (type === 'scenario') return '场景题'
  if (type === 'project') return '项目题'
  if (type === 'coding') return '算法题'
  return '综合题'
}

function tagList(tags?: string) {
  return (tags ?? '')
    .split(/[,\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

const answerPreview = (answer?: string, max = 180) => {
  if (!answer?.trim()) {
    return '当前题目还没有补充标准答案，可以先进入模拟面试或问答页继续扩展。'
  }
  return answer.length > max ? `${answer.slice(0, max)}...` : answer
}

const openDetail = (question: QuestionItem) => {
  selectedQuestion.value = question
  detailVisible.value = true
}

const questionChatTarget = (question: QuestionItem) => ({
  path: '/chat',
  query: {
    sourceQuestionId: String(question.id),
    sourceQuestionTitle: question.title,
    sourceQuestionCategory: question.categoryName || '',
    sourceQuestionTag: tagList(question.tags)[0] || '',
    sourceQuestionDirection: question.jobDirection || ''
  }
})

const questionInterviewTarget = (question: QuestionItem) => ({
  path: '/interview',
  query: {
    sourceQuestionId: String(question.id),
    sourceQuestionTitle: question.title,
    sourceQuestionCategory: question.categoryName || '',
    sourceQuestionTag: tagList(question.tags)[0] || '',
    sourceQuestionDirection: question.jobDirection || ''
  }
})

const applyRouteFilters = () => {
  const categoryId = Number(route.query.categoryId)
  filters.categoryId = Number.isFinite(categoryId) && categoryId > 0 ? categoryId : undefined

  const difficulty = String(route.query.difficulty || '').trim()
  filters.difficulty = difficulty === 'easy' || difficulty === 'medium' || difficulty === 'hard'
    ? difficulty
    : undefined

  filters.keyword = String(route.query.keyword || '').trim()
  filters.jobDirection = String(route.query.jobDirection || '').trim()
  filters.tag = String(route.query.tag || '').trim()
}

const favoriteIds = ref(new Set<number>())

const loadFavoriteIds = async () => {
  try {
    const { data } = await fetchFavoriteListApi({ targetType: 'question', pageSize: 500 })
    favoriteIds.value = new Set(data.records.map((f) => f.targetId))
  } catch {
    // silent
  }
}

const isFavorited = (questionId: number) => favoriteIds.value.has(questionId)

const toggleFavorite = async (item: QuestionItem) => {
  try {
    if (isFavorited(item.id)) {
      const { data } = await fetchFavoriteListApi({ targetType: 'question', pageSize: 500 })
      const fav = data.records.find((f) => f.targetId === item.id)
      if (fav) await removeFavoriteApi(fav.id)
      favoriteIds.value.delete(item.id)
      ElMessage.success('已取消收藏')
    } else {
      await addFavoriteApi({ targetType: 'question', targetId: item.id })
      favoriteIds.value.add(item.id)
      ElMessage.success('已收藏')
    }
  } catch {
    ElMessage.error('收藏状态没有更新成功，请稍后再试')
  }
}

onMounted(() => {
  applyRouteFilters()
  void Promise.all([loadCategories(), loadQuestions(), loadFavoriteIds()])
})

watch(
  () => route.fullPath,
  () => {
    applyRouteFilters()
    currentPage.value = 1
    void loadQuestions()
  }
)
</script>

<style scoped>
.question-toolbar__head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px 16px;
}

.question-toolbar__summary {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px 14px;
}

.question-toolbar__filters,
.question-toolbar__advanced {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.question-toolbar__search {
  width: min(100%, 320px);
}

.filter-chip {
  min-height: 2rem;
  border-radius: 0.625rem;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.22);
  background: rgba(var(--bc-accent-rgb), 0.06);
  color: var(--bc-ink);
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0 0.85rem;
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--ease-hard),
    border-color var(--motion-fast) var(--ease-hard),
    box-shadow var(--motion-fast) var(--ease-hard);
}

.filter-chip:hover {
  background: rgba(var(--bc-accent-rgb), 0.12);
  box-shadow: 0 2px 8px rgba(var(--bc-accent-rgb), 0.1);
}

.filter-chip--muted {
  border-color: var(--bc-line);
  background: transparent;
  color: var(--bc-ink-secondary);
}

.filter-chip--muted:hover {
  background: var(--interactive-hover);
  box-shadow: none;
}

.question-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  transition:
    transform var(--motion-fast) var(--ease-hard),
    box-shadow var(--motion-fast) var(--ease-hard);
}

.question-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--bc-shadow-hover);
}

.question-difficulty {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 0.38rem 0.7rem;
  border: 1px solid transparent;
}

.question-difficulty-easy {
  color: var(--bc-lime);
  background: color-mix(in srgb, var(--bc-lime) 12%, transparent);
  border-color: color-mix(in srgb, var(--bc-lime) 30%, transparent);
}

.question-difficulty-medium {
  color: var(--bc-accent);
  background: rgba(var(--bc-accent-rgb), 0.1);
  border-color: rgba(var(--bc-accent-rgb), 0.24);
}

@media (max-width: 767px) {
  .question-toolbar__summary {
    justify-content: flex-start;
  }
}

.question-difficulty-hard {
  color: var(--bc-coral);
  background: color-mix(in srgb, var(--bc-coral) 12%, transparent);
  border-color: color-mix(in srgb, var(--bc-coral) 28%, transparent);
}

.question-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 0.36rem 0.7rem;
  background: var(--bc-surface-muted);
  border: 1px solid var(--bc-border-subtle);
  font-size: 0.76rem;
  color: var(--bc-ink-secondary);
}

.question-detail-block {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 1rem;
}

.question-detail-title {
  display: inline-flex;
  margin-bottom: 0.55rem;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--bc-ink-secondary);
}

.question-detail-block p {
  white-space: pre-line;
  font-size: 0.95rem;
  line-height: 1.75;
  color: var(--bc-ink-secondary);
}

.question-detail-block-danger {
  border-color: color-mix(in srgb, var(--bc-coral) 24%, var(--bc-border-subtle));
  background: color-mix(in srgb, var(--bc-coral) 8%, var(--bc-surface-muted));
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
