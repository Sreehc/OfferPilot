<template>
  <div class="community-cockpit space-y-6">
    <section class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div class="min-w-0 max-w-3xl">
          <div class="flex items-center gap-3">
            <span class="state-pulse" aria-hidden="true"></span>
            <p class="section-kicker">社区问答</p>
          </div>
          <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">先看问题，再决定回答还是提问</h2>
          <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
            这里优先展示真实问题列表。先搜同类问题，再进入回答；如果没人提过，再发起新问题。
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button
            class="hard-button-primary text-sm"
            @click="$router.push('/community/submit')"
          >
            发起提问
          </button>
          <button
            class="hard-button-secondary text-sm"
            @click="$router.push('/community/leaderboard')"
          >
            查看排行榜
          </button>
        </div>
      </div>

      <div class="community-overview-bar mt-6">
        <article
          v-for="signal in overviewSignals"
          :key="signal.label"
          class="data-slab p-4"
          :class="signal.toneClass"
        >
          <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">
            {{ signal.label }}
          </p>
          <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ signal.value }}</p>
          <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ signal.detail }}</p>
        </article>
      </div>

      <div class="priority-strip mt-4">
        <article
          v-for="item in priorityTips"
          :key="item.label"
          class="priority-strip__item"
        >
          <div class="flex items-center gap-3">
            <span class="inline-flex h-2.5 w-2.5 rounded-full" :class="item.dotClass"></span>
            <div>
              <p class="text-sm font-semibold text-ink">{{ item.label }}</p>
              <p class="text-xs text-slate-500 dark:text-slate-400">{{ item.detail }}</p>
            </div>
          </div>
          <span class="font-mono text-sm font-semibold text-ink">{{ item.value }}</span>
        </article>
      </div>
    </section>

    <section class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
        <div class="min-w-0">
          <p class="section-kicker">问题列表</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">搜索、筛选并直接进入问题</h3>
          <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-300">
            优先查看待补充和讨论中的问题。已解决的问题更适合当作答案参考，不需要重复提问。
          </p>
        </div>

        <div class="community-filter-bar">
          <div class="mode-switch grid grid-cols-2 gap-2">
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
              placeholder="搜索问题、分类或关键词..."
              class="community-search__input"
              @keyup.enter="doSearch"
            />
            <button type="button" class="community-search__button" @click="doSearch">检索</button>
          </div>
        </div>
      </div>

      <div v-if="loading" class="mt-5 grid gap-4">
        <article v-for="index in 3" :key="index" class="surface-card animate-pulse p-5">
          <div class="flex items-start gap-4">
            <div class="h-14 w-14 rounded-2xl bg-slate-200 dark:bg-slate-700"></div>
            <div class="flex-1">
              <div class="h-4 w-24 rounded bg-slate-200 dark:bg-slate-700"></div>
              <div class="mt-4 h-6 w-3/5 rounded bg-slate-100 dark:bg-slate-800"></div>
              <div class="mt-3 h-4 w-full rounded bg-slate-100 dark:bg-slate-800"></div>
              <div class="mt-2 h-4 w-4/5 rounded bg-slate-100 dark:bg-slate-800"></div>
            </div>
          </div>
        </article>
      </div>

      <div v-else-if="questions.length" class="mt-5 space-y-4">
        <article
          v-for="q in questions"
          :key="q.id"
          class="mission-card question-card p-4 sm:p-5"
          :class="questionToneClass(q)"
          @click="$router.push(`/community/question/${q.id}`)"
        >
          <div class="flex flex-col gap-4 lg:flex-row lg:items-start">
            <div class="question-card__scoreboard">
              <div class="question-card__metric">
                <span class="question-card__metric-value">{{ q.upvoteCount }}</span>
                <span class="question-card__metric-label">赞同</span>
              </div>
              <div class="question-card__metric">
                <span class="question-card__metric-value">{{ q.answerCount }}</span>
                <span class="question-card__metric-label">{{ q.accepted ? '已解' : '回答' }}</span>
              </div>
            </div>

            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center gap-2">
                <span class="hard-chip !px-2 !py-0.5 !text-[9px]" :class="questionStatusChipClass(q)">
                  {{ questionStatusLabel(q) }}
                </span>
                <span v-if="q.categoryName" class="question-pill">{{ q.categoryName }}</span>
                <span class="question-pill">{{ questionHeatLabel(q) }}</span>
              </div>

              <h4 class="mt-4 text-xl font-semibold tracking-[-0.03em] text-ink">{{ q.title }}</h4>
              <p class="mt-3 line-clamp-2 text-sm leading-7 text-slate-600 dark:text-slate-300">{{ q.content }}</p>

              <div class="mt-4 flex flex-wrap items-center gap-3 text-xs text-slate-500 dark:text-slate-400">
                <span>{{ q.authorName || '匿名用户' }}</span>
                <span v-if="q.authorRank" class="question-pill question-pill-accent">{{ q.authorRank }}</span>
                <span>{{ formatTime(q.createdAt) }}</span>
                <span>{{ questionActionHint(q) }}</span>
              </div>
            </div>

            <div class="question-card__signal">
              <div class="question-card__signal-row">
                <span class="inline-flex items-center gap-2">
                  <span class="h-2 w-2 rounded-full" :class="questionStatusDotClass(q)"></span>
                  状态
                </span>
                <span>{{ questionStatusLabel(q) }}</span>
              </div>
              <div class="question-card__signal-row">
                <span>互动数</span>
                <span>{{ q.upvoteCount + q.answerCount }}</span>
              </div>
              <div class="question-card__signal-row">
                <span>分类</span>
                <span>{{ q.categoryName || '未分类' }}</span>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="mt-5">
        <EmptyState
          class="empty-state-card"
          icon="chat"
          title="还没有找到问题"
          description="换个关键词继续搜，或者直接发起一个新问题。"
        >
          <template #action>
            <RouterLink to="/community/submit" class="hard-button-primary inline-flex">
              发起提问
            </RouterLink>
          </template>
        </EmptyState>
      </div>

      <div v-if="totalPages > 1" class="mt-6 flex justify-center gap-2">
        <button
          v-for="p in displayPages"
          :key="p"
          class="community-page-button"
          :class="{ 'community-page-button-active': page === p }"
          @click="page = p; fetchQuestions()"
        >
          {{ p }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { fetchCommunityQuestionsApi } from '@/api/community'
import EmptyState from '@/components/EmptyState.vue'
import { useAuthStore } from '@/stores/auth'
import type { CommunityQuestion } from '@/types/api'

const authStore = useAuthStore()
const questions = ref<CommunityQuestion[]>([])
const page = ref(1)
const size = 20
const total = ref(0)
const totalPages = ref(0)
const sort = ref<'new' | 'hot'>('new')
const keyword = ref('')
const loading = ref(false)

const sorts = [
  { label: '最新节奏', value: 'new' as const },
  { label: '热门围观', value: 'hot' as const },
]

const resolvedCount = computed(() => questions.value.filter((item) => questionResolved(item)).length)
const activeCount = computed(() => questions.value.length - resolvedCount.value)
const solvedRate = computed(() => {
  if (!questions.value.length) return 0
  return Math.round((resolvedCount.value / questions.value.length) * 100)
})

const personalQuestionCount = computed(() => {
  const currentUserId = authStore.user?.id
  if (!currentUserId) return 0
  return questions.value.filter((item) => item.userId === currentUserId).length
})

const overviewSignals = computed(() => [
  {
    label: '当前页问题数',
    value: questions.value.length,
    detail: '便于快速判断这一页是否需要继续翻页查看。',
    toneClass: '',
  },
  {
    label: '已解决占比',
    value: `${solvedRate.value}%`,
    detail: '优先把已解决问题当答案参考，把未解决问题当参与入口。',
    toneClass: 'community-slab-cyan',
  },
  {
    label: '待补充问题',
    value: activeCount.value,
    detail: '这部分最适合直接补充回答或继续追问。',
    toneClass: 'community-slab-coral',
  },
  {
    label: '我的提问',
    value: personalQuestionCount.value,
    detail: personalQuestionCount.value > 0 ? '你可以继续追踪自己的问题是否得到解决。' : '还没有你的问题，遇到卡点可以直接发起提问。',
    toneClass: 'community-slab-lime',
  },
])

const priorityTips = computed(() => [
  {
    label: '推荐动作',
    value: activeCount.value > 0 ? '去答疑' : '去提问',
    detail: activeCount.value > 0 ? '还有问题没有形成结论，优先进入详情页补充回答。' : '当前列表较平稳，适合补充一个新问题。' ,
    dotClass: activeCount.value > 0 ? 'bg-[var(--bc-amber)]' : 'bg-[var(--bc-cyan)]',
  },
  {
    label: '最热问题',
    value: hottestQuestion.value ? `${hottestQuestion.value.upvoteCount + hottestQuestion.value.answerCount}` : '0',
    detail: hottestQuestion.value ? hottestQuestion.value.title : '暂无高热问题',
    dotClass: 'bg-[var(--bc-coral)]',
  },
  {
    label: '讨论最多的分类',
    value: topCategory.value,
    detail: '能帮助你快速判断最近大家最常遇到的主题。',
    dotClass: 'bg-[var(--bc-lime)]',
  },
])

const hottestQuestion = computed(() => {
  if (!questions.value.length) return null
  return [...questions.value].sort((a, b) => (b.upvoteCount + b.answerCount) - (a.upvoteCount + a.answerCount))[0] ?? null
})

const topCategory = computed(() => {
  const bucket = new Map<string, number>()
  questions.value.forEach((item) => {
    const key = item.categoryName || '未分类'
    bucket.set(key, (bucket.get(key) ?? 0) + 1)
  })
  const top = [...bucket.entries()].sort((a, b) => b[1] - a[1])[0]
  return top?.[0] ?? '未分类'
})

const displayPages = computed(() => {
  const pages: number[] = []
  const start = Math.max(1, page.value - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

async function fetchQuestions() {
  loading.value = true
  try {
    const { data } = await fetchCommunityQuestionsApi(page.value, size, sort.value, undefined, keyword.value || undefined)
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

function questionResolved(question: CommunityQuestion) {
  return question.accepted || question.status === 'resolved'
}

function questionStatusLabel(question: CommunityQuestion) {
  if (questionResolved(question)) return '已解决'
  if (question.answerCount > 0) return '讨论中'
  return '待补充'
}

function questionHeatLabel(question: CommunityQuestion) {
  const intensity = question.upvoteCount + question.answerCount
  if (intensity >= 12) return '高热围观'
  if (intensity >= 5) return '持续讨论'
  return '新进问题'
}

function questionActionHint(question: CommunityQuestion) {
  if (questionResolved(question)) return '适合复盘答案结构'
  if (question.answerCount > 0) return '适合继续补充视角'
  return '适合率先给出可执行回答'
}

function questionToneClass(question: CommunityQuestion) {
  if (questionResolved(question)) return 'question-card-resolved'
  if (question.answerCount > 0) return 'question-card-warm'
  return 'question-card-open'
}

function questionStatusDotClass(question: CommunityQuestion) {
  if (questionResolved(question)) return 'bg-[var(--bc-lime)]'
  if (question.answerCount > 0) return 'bg-[var(--bc-amber)]'
  return 'bg-[var(--bc-coral)]'
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
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  return d.toLocaleDateString('zh-CN')
}

watch(sort, () => {
  page.value = 1
  void fetchQuestions()
})

onMounted(() => {
  void fetchQuestions()
})
</script>

<style scoped>
.community-filter-bar {
  display: grid;
  gap: 12px;
}

.community-overview-bar {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.priority-strip {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.priority-strip__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-radius: 18px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.34);
  padding: 14px 16px;
}

.dark .priority-strip__item {
  background: rgba(255, 255, 255, 0.05);
}

.mode-switch {
  border: 1px solid var(--bc-line);
  border-radius: calc(var(--radius-md) + 4px);
  background: rgba(255, 255, 255, 0.32);
  padding: 4px;
}

.dark .mode-switch {
  background: rgba(255, 255, 255, 0.04);
}

.mode-switch__item {
  min-height: 44px;
  border: 0;
  border-radius: calc(var(--radius-sm) + 2px);
  background: transparent;
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
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
  min-height: 48px;
  width: 100%;
  border: 1px solid var(--bc-border-strong);
  border-radius: var(--radius-sm);
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
  min-height: 48px;
  border: 0;
  border-radius: var(--radius-sm);
  padding: 0 16px;
  font-size: 13px;
  font-weight: 700;
  color: #101826;
  background: linear-gradient(180deg, #ffd18a 0%, var(--bc-amber) 100%);
}

.community-slab-cyan {
  border-left-color: var(--bc-cyan);
}

.community-slab-coral {
  border-left-color: var(--bc-coral);
}

.community-slab-lime {
  border-left-color: var(--bc-lime);
}

.question-card {
  cursor: pointer;
}

.question-card-open {
  border-color: rgba(255, 107, 107, 0.24);
}

.question-card-warm {
  border-color: rgba(255, 183, 77, 0.28);
}

.question-card-resolved {
  border-color: rgba(159, 232, 112, 0.26);
}

.question-card__scoreboard {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  flex-shrink: 0;
  min-width: 132px;
}

.question-card__metric {
  border-radius: 20px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.34);
  padding: 14px 12px;
  text-align: center;
}

.dark .question-card__metric {
  background: rgba(255, 255, 255, 0.05);
}

.question-card__metric-value {
  display: block;
  font-family: theme('fontFamily.mono');
  font-size: 24px;
  font-weight: 700;
  color: var(--bc-ink);
}

.question-card__metric-label {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--bc-ink-secondary);
}

.question-card__signal {
  display: grid;
  gap: 10px;
  min-width: 180px;
  border-radius: 20px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.36);
  padding: 14px;
  font-size: 12px;
}

.dark .question-card__signal {
  background: rgba(255, 255, 255, 0.05);
}

.question-card__signal-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--bc-ink-secondary);
}

.question-card__signal-row span:last-child {
  color: var(--bc-ink);
  font-weight: 600;
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

.question-pill-accent {
  border-color: rgba(var(--bc-accent-rgb), 0.24);
  color: var(--bc-accent);
  background: rgba(var(--bc-accent-rgb), 0.08);
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

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 1024px) {
  .community-overview-bar,
  .priority-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .question-card__signal {
    min-width: 100%;
  }
}

@media (max-width: 640px) {
  .community-overview-bar,
  .priority-strip {
    grid-template-columns: minmax(0, 1fr);
  }

  .community-search {
    grid-template-columns: minmax(0, 1fr);
  }

  .question-card__scoreboard {
    min-width: 100%;
  }
}
</style>
