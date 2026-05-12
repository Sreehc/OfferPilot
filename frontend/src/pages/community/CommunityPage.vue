<template>
  <div class="community-forum space-y-6">
    <AppShellHeader>
      <template #actions>
        <button class="hard-button-primary forum-header-bar__action text-sm" @click="$router.push('/community/submit')">
          发起提问
        </button>
      </template>
    </AppShellHeader>

    <section class="forum-hero shell-section-card p-4 sm:p-5">
      <div class="forum-header-bar">
        <div class="forum-header-bar__tabs">
          <button
            v-for="board in boardOptions"
            :key="board.id"
            type="button"
            class="forum-board-chip"
            :class="{ 'forum-board-chip-active': selectedCategoryId === board.id }"
            @click="selectBoard(board.id)"
          >
            {{ board.name }}
          </button>
        </div>
      </div>
    </section>

    <section class="forum-feed shell-section-card p-4 sm:p-5">
      <div class="forum-toolbar">
        <div class="mode-switch">
          <button
            v-for="s in sorts"
            :key="s.value"
            type="button"
            class="mode-switch__item"
            :class="{ 'mode-switch__item-active': sort === s.value }"
            @click="sort = s.value"
          >
            {{ s.label }}
          </button>
        </div>

        <div class="community-search">
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索帖子"
            class="community-search__input"
            @keyup.enter="doSearch"
          />
          <button type="button" class="community-search__button" @click="doSearch">搜索</button>
        </div>
      </div>

      <div class="forum-feed__meta">
        <span>{{ selectedCategoryName }}</span>
        <span>{{ total }} 个主题</span>
      </div>

      <div v-if="loading" class="mt-5 grid gap-3">
        <article v-for="index in 4" :key="index" class="forum-thread forum-thread-skeleton animate-pulse">
          <div class="forum-thread__body">
            <div class="h-4 w-24 rounded bg-slate-200 dark:bg-slate-700"></div>
            <div class="mt-4 h-6 w-3/5 rounded bg-slate-100 dark:bg-slate-800"></div>
            <div class="mt-3 h-4 w-full rounded bg-slate-100 dark:bg-slate-800"></div>
            <div class="mt-2 h-4 w-2/3 rounded bg-slate-100 dark:bg-slate-800"></div>
          </div>
          <div class="forum-thread__aside">
            <div class="h-16 w-16 rounded-2xl bg-slate-100 dark:bg-slate-800"></div>
          </div>
        </article>
      </div>

      <div v-else-if="questions.length" class="forum-thread-list mt-5">
        <article
          v-for="q in questions"
          :key="q.id"
          class="forum-thread"
          :class="questionToneClass(q)"
          @click="$router.push(`/community/question/${q.id}`)"
        >
          <div class="forum-thread__body">
            <div class="forum-thread__badges">
              <span class="forum-thread__status" :class="questionStatusChipClass(q)">
                {{ questionStatusLabel(q) }}
              </span>
              <span v-if="q.categoryName" class="question-pill">{{ q.categoryName }}</span>
            </div>

            <h4 class="forum-thread__title">{{ q.title }}</h4>
            <p class="forum-thread__excerpt">{{ q.content }}</p>

            <div class="forum-thread__meta">
              <span>{{ q.authorName || '匿名用户' }}</span>
              <span>{{ formatTime(q.createdAt) }}</span>
              <span>{{ q.answerCount }} 条回复</span>
            </div>
          </div>

          <div class="forum-thread__aside">
            <div class="forum-thread__reply">
              <strong>{{ q.answerCount }}</strong>
              <span>回复</span>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="mt-5">
        <EmptyState
          class="empty-state-card"
          icon="chat"
          title="当前版块还没有帖子"
          description="换个版块看看，或直接提问。"
        >
          <template #action>
            <RouterLink to="/community/submit" class="hard-button-primary inline-flex"> 发起提问 </RouterLink>
          </template>
        </EmptyState>
      </div>

      <div v-if="totalPages > 1" class="mt-6 flex justify-center gap-2">
        <button
          v-for="p in displayPages"
          :key="p"
          class="community-page-button"
          :class="{ 'community-page-button-active': page === p }"
          @click="handlePageClick(p)"
        >
          {{ p }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { fetchCategoriesApi } from '@/api/category'
import { fetchCommunityQuestionsApi } from '@/api/community'
import AppShellHeader from '@/components/AppShellHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import type { CategoryItem, CommunityQuestion } from '@/types/api'

type BoardOption = {
  id?: number
  name: string
}

const questions = ref<CommunityQuestion[]>([])
const categories = ref<CategoryItem[]>([])
const page = ref(1)
const size = 20
const total = ref(0)
const totalPages = ref(0)
const sort = ref<'new' | 'hot'>('new')
const keyword = ref('')
const loading = ref(false)
const selectedCategoryId = ref<number | undefined>(undefined)

const sorts = [
  { label: '最新', value: 'new' as const },
  { label: '热门', value: 'hot' as const }
]

const boardOptions = computed<BoardOption[]>(() => {
  const boards = categories.value.map((category) => ({
    id: category.id,
    name: category.name
  }))

  return [
    {
      id: undefined,
      name: '全部主题'
    },
    ...boards
  ]
})

const selectedCategoryName = computed(() => {
  if (selectedCategoryId.value == null) return '全部主题'
  return categories.value.find((item) => item.id === selectedCategoryId.value)?.name || '当前版块'
})

const displayPages = computed(() => {
  const pages: number[] = []
  const start = Math.max(1, page.value - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

async function loadCategories() {
  try {
    const { data } = await fetchCategoriesApi({ type: 'question' })
    categories.value = data
  } catch {
    categories.value = []
  }
}

async function fetchQuestions() {
  loading.value = true
  try {
    const { data } = await fetchCommunityQuestionsApi(
      page.value,
      size,
      sort.value,
      selectedCategoryId.value,
      keyword.value || undefined
    )
    questions.value = data.records
    total.value = data.total
    totalPages.value = data.totalPages || Math.ceil(data.total / size)
  } finally {
    loading.value = false
  }
}

function doSearch() {
  page.value = 1
  void fetchQuestions()
}

function handlePageClick(targetPage: number) {
  page.value = targetPage
  void fetchQuestions()
}

function selectBoard(categoryId?: number) {
  if (selectedCategoryId.value === categoryId) return
  selectedCategoryId.value = categoryId
}

function questionResolved(question: CommunityQuestion) {
  return question.accepted || question.status === 'resolved'
}

function questionStatusLabel(question: CommunityQuestion) {
  if (questionResolved(question)) return '已解决'
  if (question.answerCount > 0) return '讨论中'
  return '新帖'
}

function questionToneClass(question: CommunityQuestion) {
  if (questionResolved(question)) return 'forum-thread-resolved'
  if (question.answerCount > 0) return 'forum-thread-warm'
  return 'forum-thread-open'
}

function questionStatusChipClass(question: CommunityQuestion) {
  if (questionResolved(question)) return 'question-chip-resolved'
  if (question.answerCount > 0) return 'question-chip-warm'
  return 'question-chip-open'
}

function formatTime(time?: string) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 60) return `${Math.max(1, minutes)}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  return d.toLocaleDateString('zh-CN')
}

watch([sort, selectedCategoryId], () => {
  page.value = 1
  void fetchQuestions()
})

onMounted(async () => {
  await loadCategories()
  await fetchQuestions()
})
</script>

<style scoped>
.forum-hero {
  position: relative;
  overflow: hidden;
}

.forum-header-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px 18px;
}

.forum-header-bar__title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: max-content;
}

.forum-header-bar__heading {
  color: var(--bc-ink);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.1;
}

.forum-header-bar__tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  flex: 1;
  min-width: min(100%, 420px);
}

.forum-header-bar__action {
  min-height: 42px !important;
  padding-inline: 18px !important;
}

.forum-board-chip {
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
  white-space: nowrap;
  transition:
    border-color var(--motion-base) var(--ease-hard),
    color var(--motion-base) var(--ease-hard),
    background-color var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard);
}

.dark .forum-board-chip {
  background: rgba(255, 255, 255, 0.05);
}

.forum-board-chip:hover {
  border-color: rgba(var(--bc-accent-rgb), 0.26);
  color: var(--bc-ink);
}

.forum-board-chip-active {
  border-color: rgba(var(--bc-accent-rgb), 0.36);
  box-shadow: inset 0 0 0 1px rgba(var(--bc-accent-rgb), 0.14);
  color: var(--bc-ink);
  background: linear-gradient(180deg, rgba(255, 222, 173, 0.84), rgba(255, 247, 235, 0.68));
}

.forum-toolbar {
  display: grid;
  gap: 14px;
}

.mode-switch {
  display: inline-grid;
  gap: 6px;
  grid-template-columns: repeat(2, minmax(0, auto));
  width: fit-content;
  border: 1px solid var(--bc-line);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.32);
  padding: 4px;
}

.dark .mode-switch {
  background: rgba(255, 255, 255, 0.04);
}

.mode-switch__item {
  min-width: 82px;
  min-height: 40px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  transition:
    background-color var(--motion-base) var(--ease-hard),
    color var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard);
}

.mode-switch__item-active {
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.18), rgba(var(--bc-accent-rgb), 0.08));
  color: var(--bc-ink);
  box-shadow: inset 0 0 0 1px rgba(var(--bc-accent-rgb), 0.2);
}

.community-search {
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1fr) auto;
}

.community-search__input {
  min-height: 46px;
  width: 100%;
  border: 1px solid var(--bc-border-strong);
  border-radius: 14px;
  background: var(--bc-surface-input);
  color: var(--bc-ink);
  padding: 0 16px;
  font-size: 14px;
}

.community-search__input:focus {
  outline: 0;
  box-shadow: 0 0 0 4px rgba(var(--bc-accent-rgb), 0.1);
  border-color: rgba(var(--bc-accent-rgb), 0.42);
}

.community-search__button {
  min-height: 46px;
  border: 0;
  border-radius: 14px;
  padding: 0 18px;
  font-size: 13px;
  font-weight: 700;
  color: #101826;
  background: linear-gradient(180deg, #ffd18a 0%, var(--bc-amber) 100%);
}

.forum-feed__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
  color: var(--bc-ink-secondary);
  font-size: 12px;
}

.forum-thread {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) 72px;
  align-items: center;
  border: 1px solid var(--bc-line);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.62), rgba(255, 255, 255, 0.42));
  padding: 20px;
  cursor: pointer;
  transition:
    transform var(--motion-base) var(--ease-hard),
    border-color var(--motion-base) var(--ease-hard),
    box-shadow var(--motion-base) var(--ease-hard);
}

.dark .forum-thread {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.03));
}

.forum-thread:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--bc-accent-rgb), 0.26);
}

.forum-thread-open {
  border-color: rgba(255, 107, 107, 0.18);
}

.forum-thread-warm {
  border-color: rgba(255, 183, 77, 0.22);
}

.forum-thread-resolved {
  border-color: rgba(159, 232, 112, 0.22);
}

.forum-thread-skeleton {
  cursor: default;
}

.forum-thread__body {
  min-width: 0;
}

.forum-thread__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.forum-thread__status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 11px;
  font-weight: 700;
}

.forum-thread__title {
  margin-top: 14px;
  color: var(--bc-ink);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.35;
}

.forum-thread__excerpt {
  display: -webkit-box;
  margin-top: 10px;
  overflow: hidden;
  color: rgb(71 85 105);
  font-size: 14px;
  line-height: 1.8;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.dark .forum-thread__excerpt {
  color: rgb(203 213 225);
}

.forum-thread__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 14px;
  color: var(--bc-ink-secondary);
  font-size: 12px;
}

.forum-thread__aside {
  display: flex;
  align-items: center;
  justify-content: center;
}

.forum-thread__reply {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 78px;
  min-height: 78px;
  border-radius: 22px;
  background: rgba(var(--bc-accent-rgb), 0.08);
  color: var(--bc-ink);
}

.forum-thread__reply strong {
  font-size: 24px;
  line-height: 1;
}

.forum-thread__reply span {
  margin-top: 6px;
  color: var(--bc-ink-secondary);
  font-size: 11px;
}

.question-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid var(--bc-line);
  padding: 6px 10px;
  font-size: 11px;
  color: var(--bc-ink-secondary);
}

.question-chip-open {
  color: var(--bc-coral);
  background: rgba(255, 107, 107, 0.12);
}

.question-chip-warm {
  color: var(--bc-amber);
  background: rgba(255, 183, 77, 0.12);
}

.question-chip-resolved {
  color: var(--bc-lime);
  background: rgba(159, 232, 112, 0.12);
}

.community-page-button {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.4);
  color: var(--bc-ink-secondary);
  font-size: 14px;
  font-weight: 600;
}

.dark .community-page-button {
  background: rgba(255, 255, 255, 0.05);
}

.community-page-button-active {
  background: linear-gradient(180deg, #ffd18a 0%, var(--bc-amber) 100%);
  color: #101826;
  border-color: transparent;
}

@media (min-width: 1024px) {
  .forum-toolbar {
    grid-template-columns: auto minmax(320px, 440px);
    align-items: center;
    justify-content: space-between;
  }
}

@media (max-width: 1024px) {
  .forum-header-bar__tabs {
    order: 3;
    flex-basis: 100%;
    min-width: 0;
  }
}

@media (max-width: 900px) {
  .forum-thread {
    grid-template-columns: minmax(0, 1fr);
  }

  .forum-thread__aside {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .community-search {
    grid-template-columns: minmax(0, 1fr);
  }

  .forum-header-bar {
    align-items: stretch;
  }

  .forum-header-bar__heading {
    font-size: 24px;
  }

  .forum-header-bar__action {
    width: 100%;
  }

  .forum-thread {
    padding: 18px;
    border-radius: 20px;
  }

  .forum-thread__title {
    font-size: 19px;
  }

  .forum-thread__reply {
    width: 68px;
    min-height: 68px;
    border-radius: 18px;
  }
}
</style>
