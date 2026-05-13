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

    <section class="question-page-grid">
      <section class="space-y-4">
        <section class="shell-section-card p-5 sm:p-6">
          <div class="grid gap-3 lg:grid-cols-[1.1fr_220px_220px_auto]">
            <el-input
              v-model="filters.keyword"
              clearable
              size="large"
              placeholder="搜索题目标题、标签或答案关键词"
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
                  <span v-if="item.categoryName">{{ item.categoryName }}</span>
                  <span v-if="item.frequency">高频度 {{ item.frequency }}</span>
                </div>
                <h3 class="mt-3 text-xl font-semibold leading-8 text-ink">
                  {{ item.title }}
                </h3>
              </div>

              <div class="flex shrink-0 gap-2">
                <RouterLink
                  to="/chat"
                  class="hard-button-secondary text-sm"
                >
                  追问答案
                </RouterLink>
                <RouterLink
                  to="/interview"
                  class="hard-button-primary text-sm"
                >
                  进入面试
                </RouterLink>
              </div>
            </div>

            <div
              v-if="tagList(item.tags).length"
              class="mt-4 flex flex-wrap gap-2"
            >
              <span
                v-for="tag in tagList(item.tags)"
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
              description="调整分类、难度或关键词后重新加载题库。"
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

      <aside class="space-y-4">
        <section class="shell-section-card p-5 sm:p-6">
          <p class="section-kicker">
            当前视图
          </p>
          <div class="mt-5 grid gap-3">
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
              <span>总分页</span>
              <strong>{{ totalPages || 1 }}</strong>
            </article>
          </div>
        </section>

        <section class="shell-section-card p-5 sm:p-6">
          <p class="section-kicker">
            训练建议
          </p>
          <div class="mt-4 space-y-3 text-sm leading-7 text-secondary">
            <p>先用题库做结构化训练，再去问答页扩展回答，再用模拟面试检验表达质量。</p>
            <p>分类筛选适合补弱项，难度筛选适合做冲刺，标签更适合定位岗位方向和专题训练。</p>
          </div>
        </section>
      </aside>
    </section>
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

const filters = reactive<{
  categoryId?: number
  difficulty?: QuestionItem['difficulty']
  keyword: string
}>({
  categoryId: undefined,
  difficulty: undefined,
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
      difficulty: filters.difficulty,
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
  filters.difficulty = undefined
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

function tagList(tags?: string) {
  return (tags ?? '')
    .split(/[,\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

const answerPreview = (answer?: string) => {
  if (!answer?.trim()) {
    return '当前题目还没有补充标准答案，可以先进入模拟面试或问答页继续扩展。'
  }
  return answer.length > 180 ? `${answer.slice(0, 180)}...` : answer
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

@media (min-width: 1200px) {
  .question-page-grid {
    grid-template-columns: minmax(0, 1fr) 320px;
    align-items: start;
  }
}
</style>
