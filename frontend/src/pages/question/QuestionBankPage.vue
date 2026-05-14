<template>
  <div class="space-y-6">
    <AppShellHeader>
      <template #actions>
        <RouterLink
          to="/interview"
          class="hard-button-primary"
        >
          去模拟面试
        </RouterLink>
        <RouterLink
          to="/chat"
          class="hard-button-secondary"
        >
          继续问答
        </RouterLink>
      </template>
    </AppShellHeader>

    <section class="space-y-4">
      <section class="shell-section-card p-5 sm:p-6">
        <div class="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
          <div class="min-w-0 max-w-3xl">
            <h2 class="text-2xl font-semibold tracking-[-0.03em] text-ink">先筛出你现在最该练的题</h2>
            <p class="mt-3 text-sm leading-7 text-secondary">
              按分类、难度、岗位方向和标签缩小范围，再决定是直接刷题、追问还是去做模拟面试。
            </p>
          </div>
          <div class="grid gap-3 sm:grid-cols-4">
            <article class="question-summary-card">
              <span>当前页题目</span>
              <strong>{{ questions.length }}</strong>
            </article>
            <article class="question-summary-card">
              <span>困难题</span>
              <strong>{{ hardQuestionCount }}</strong>
            </article>
            <article class="question-summary-card">
              <span>带标签题目</span>
              <strong>{{ taggedQuestionCount }}</strong>
            </article>
            <article class="question-summary-card">
              <span>岗位向题目</span>
              <strong>{{ directionalQuestionCount }}</strong>
            </article>
          </div>
        </div>

        <div class="mt-5 grid gap-3 xl:grid-cols-[1.1fr_180px_180px_220px_220px_220px_auto]">
            <el-input
              v-model="filters.keyword"
              clearable
              size="large"
              placeholder="搜索题目标题、标签、答案或错误示例"
              @keyup.enter="applyFilters"
            />
            <el-select
              v-model="filters.categoryId"
              clearable
              size="large"
              placeholder="技术分类"
            >
              <el-option
                v-for="item in categories"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
            <el-select
              v-model="filters.difficulty"
              clearable
              size="large"
              placeholder="难度"
            >
              <el-option
                label="简单"
                value="easy"
              />
              <el-option
                label="中等"
                value="medium"
              />
              <el-option
                label="困难"
                value="hard"
              />
            </el-select>
            <el-select
              v-model="filters.type"
              clearable
              size="large"
              placeholder="题目类型"
            >
              <el-option label="八股题" value="concept" />
              <el-option label="场景题" value="scenario" />
              <el-option label="项目题" value="project" />
              <el-option label="算法题" value="coding" />
            </el-select>
            <el-input
              v-model="filters.jobDirection"
              clearable
              size="large"
              placeholder="岗位方向，如 Java 后端 / 校招"
              @keyup.enter="applyFilters"
            />
            <el-input
              v-model="filters.tag"
              clearable
              size="large"
              placeholder="标签，如 Redis / 并发 / 微服务"
              @keyup.enter="applyFilters"
            />
            <div class="flex gap-3">
              <el-button
                type="primary"
                size="large"
                class="action-button flex-1"
                :loading="loading"
                @click="applyFilters"
              >
                刷新题库
              </el-button>
              <el-button
                size="large"
                class="hard-button-secondary !ml-0"
                @click="resetFilters"
              >
                重置
              </el-button>
            </div>
          </div>
      </section>

      <section
        v-if="loading"
        class="shell-section-card min-h-[260px] p-8 text-center"
      >
        <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent" />
        <p class="mt-4 text-sm text-secondary">
          加载题库中...
        </p>
      </section>

      <section
        v-else
        class="space-y-4"
      >
        <article
          v-for="item in questions"
          :key="item.id"
          class="shell-section-card question-card p-5 sm:p-6"
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
                class="hard-button-secondary text-sm"
                @click="openDetail(item)"
              >
                查看详情
              </button>
              <RouterLink
                to="/chat"
                class="hard-button-secondary text-sm"
              >
                继续追问
              </RouterLink>
              <RouterLink
                to="/interview"
                class="hard-button-primary text-sm"
              >
                去练表达
              </RouterLink>
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

        <section
          v-if="!questions.length"
          class="shell-section-card p-6"
        >
          <EmptyState
            icon="search"
            title="当前筛选下没有题目"
            description="换个分类、标签或关键词，再继续筛一轮。"
          />
        </section>

        <section
          v-if="totalPages > 1"
          class="shell-section-card px-5 py-4"
        >
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </section>
      </section>
    </section>

    <el-drawer
      v-model="detailVisible"
      title="题目详情"
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
            <span class="question-detail-title">先看标准答案</span>
            <p>{{ selectedQuestion.standardAnswer || '暂无标准答案。' }}</p>
          </section>

          <section
            v-if="selectedQuestion.interviewAnswer"
            class="question-detail-block"
          >
            <span class="question-detail-title">再看面试表达</span>
            <p>{{ selectedQuestion.interviewAnswer }}</p>
          </section>

          <section
            v-if="selectedQuestion.followUpSuggestions"
            class="question-detail-block"
          >
            <span class="question-detail-title">接着继续追问</span>
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
            <span class="question-detail-title">最后对照评分标准</span>
            <p>{{ selectedQuestion.scoreStandard }}</p>
          </section>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import AppShellHeader from '@/components/AppShellHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { fetchCategoriesApi } from '@/api/category'
import { fetchQuestionsApi } from '@/api/question'
import type { CategoryItem, QuestionItem } from '@/types/api'

const loading = ref(false)
const currentPage = ref(1)
const pageSize = 12
const total = ref(0)
const totalPages = ref(0)
const categories = ref<CategoryItem[]>([])
const questions = ref<QuestionItem[]>([])
const detailVisible = ref(false)
const selectedQuestion = ref<QuestionItem | null>(null)

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
const directionalQuestionCount = computed(() => questions.value.filter((item) => Boolean(item.jobDirection?.trim())).length)

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
    ElMessage.error('题库加载失败，请稍后重试')
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

onMounted(() => {
  void Promise.all([loadCategories(), loadQuestions()])
})
</script>

<style scoped>
.question-page-grid {
  display: grid;
  gap: 1rem;
}

.question-card {
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

.question-summary-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.08), transparent 60%), var(--bc-surface-muted);
  padding: 1rem;
}

.question-summary-card span {
  display: block;
  font-size: 0.8rem;
  color: var(--bc-ink-secondary);
}

.question-summary-card strong {
  display: block;
  margin-top: 0.55rem;
  font-size: 1.65rem;
  line-height: 1.1;
  color: var(--bc-ink);
}

.question-insight-grid {
  display: grid;
  gap: 0.9rem;
}

.question-insight-card,
.question-detail-block {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 1rem;
}

.question-insight-title,
.question-detail-title {
  display: inline-flex;
  margin-bottom: 0.55rem;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--bc-ink-secondary);
}

.question-insight-card p,
.question-detail-block p {
  white-space: pre-line;
  font-size: 0.95rem;
  line-height: 1.75;
  color: var(--bc-ink-secondary);
}

.question-insight-card-danger,
.question-detail-block-danger {
  border-color: color-mix(in srgb, var(--bc-coral) 24%, var(--bc-border-subtle));
  background: color-mix(in srgb, var(--bc-coral) 8%, var(--bc-surface-muted));
}

@media (min-width: 1200px) {
  .question-page-grid {
    grid-template-columns: minmax(0, 1fr) 320px;
    align-items: start;
  }
}

@media (min-width: 900px) {
  .question-insight-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
