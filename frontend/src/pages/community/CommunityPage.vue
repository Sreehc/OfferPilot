<template>
  <div class="community-cockpit space-y-6">
    <section class="grid gap-4 xl:grid-cols-[minmax(0,1.12fr)_minmax(320px,0.88fr)]">
      <article class="cockpit-panel p-5 sm:p-6">
        <div class="flex flex-wrap items-start justify-between gap-4">
          <div class="min-w-0">
            <div class="flex items-center gap-3">
              <span class="state-pulse" aria-hidden="true"></span>
              <p class="section-kicker">Community Arena</p>
            </div>
            <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">训练社区热区</h2>
            <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-300">
              把面试讨论从普通问答列表升级为同伴训练场，先看今天最活跃的问题、解决率和个人贡献信号，再决定是提问、围观还是直接参与解答。
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
              排行榜
            </button>
          </div>
        </div>

        <div class="mt-6 grid gap-3 md:grid-cols-3">
          <article
            v-for="signal in arenaSignals"
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
      </article>

      <article class="cockpit-panel p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="section-kicker">My Contribution</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">我的训练信号</h3>
          </div>
          <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ myContribution.badge }}</span>
        </div>

        <div class="mt-5 rounded-[24px] border border-[var(--bc-line)] bg-white/40 p-5 dark:bg-white/5">
          <div class="flex items-start justify-between gap-4">
            <div class="min-w-0">
              <p class="text-sm font-semibold text-ink">{{ myContribution.title }}</p>
              <p class="mt-2 text-sm leading-7 text-slate-600 dark:text-slate-300">{{ myContribution.description }}</p>
            </div>
            <div class="contribution-gauge">
              <span class="contribution-gauge__value">{{ myContribution.score }}</span>
            </div>
          </div>

          <div class="mt-5 grid gap-3 sm:grid-cols-3">
            <article v-for="item in contributionStats" :key="item.label" class="contribution-node">
              <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">{{ item.label }}</p>
              <p class="mt-2 font-mono text-2xl font-semibold text-ink">{{ item.value }}</p>
              <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">{{ item.caption }}</p>
            </article>
          </div>
        </div>

        <div class="mt-5 space-y-3">
          <article
            v-for="highlight in laneHighlights"
            :key="highlight.label"
            class="highlight-lane"
          >
            <div class="flex items-center justify-between gap-3">
              <div class="flex items-center gap-3">
                <span class="inline-flex h-2.5 w-2.5 rounded-full" :class="highlight.dotClass"></span>
                <div>
                  <p class="text-sm font-semibold text-ink">{{ highlight.label }}</p>
                  <p class="text-xs text-slate-500 dark:text-slate-400">{{ highlight.detail }}</p>
                </div>
              </div>
              <span class="font-mono text-sm font-semibold text-ink">{{ highlight.value }}</span>
            </div>
          </article>
        </div>
      </article>
    </section>

    <section class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
        <div class="min-w-0">
          <p class="section-kicker">Question Stream</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">问题流</h3>
          <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-300">
            先按节奏筛选讨论流，再进入具体问题。已解决、待补充、热门围观这三种状态会直接影响你下一步是学习、答疑还是提问。
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
                  State
                </span>
                <span>{{ questionStatusLabel(q) }}</span>
              </div>
              <div class="question-card__signal-row">
                <span>Intensity</span>
                <span>{{ q.upvoteCount + q.answerCount }}</span>
              </div>
              <div class="question-card__signal-row">
                <span>Path</span>
                <span>{{ q.categoryName || 'General' }}</span>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="mt-5">
        <EmptyState
          class="empty-state-card"
          icon="chat"
          title="热区里还没有问题"
          description="成为第一个提问的人，或者换一个关键词查看已有训练讨论。"
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

const myContribution = computed(() => {
  const rank = authStore.user?.role === 'ADMIN' ? 'Control' : 'Pilot'
  const score = personalQuestionCount.value * 12 + resolvedCount.value * 4 + questions.value.reduce((sum, item) => sum + item.upvoteCount, 0)
  return {
    badge: rank,
    score,
    title: personalQuestionCount.value > 0 ? '你已经在训练社区留下了问题轨迹' : '还没有你的问题轨迹',
    description: personalQuestionCount.value > 0
      ? '继续补充高质量问题或参与讨论，可以更快建立自己的知识协作网络。'
      : '先发起一个真实的面试问题，系统会把你的首个问题接入社区热区。',
  }
})

const arenaSignals = computed(() => [
  {
    label: 'Today Questions',
    value: questions.value.length,
    detail: '当前页可见的问题数量，反映今天的讨论密度。',
    toneClass: '',
  },
  {
    label: 'Solved Rate',
    value: `${solvedRate.value}%`,
    detail: '已解决问题占比，越高说明社区更接近可执行答案。',
    toneClass: 'community-slab-cyan',
  },
  {
    label: 'Open Threads',
    value: activeCount.value,
    detail: '仍在讨论中的问题，适合直接切入回答或收藏跟进。',
    toneClass: 'community-slab-coral',
  },
])

const contributionStats = computed(() => [
  {
    label: '我的问题',
    value: personalQuestionCount.value,
    caption: '当前可见问题中属于你的提问数。',
  },
  {
    label: '已解决',
    value: resolvedCount.value,
    caption: '当前热区中已经形成明确答案的问题数。',
  },
  {
    label: '总互动',
    value: questions.value.reduce((sum, item) => sum + item.upvoteCount + item.answerCount, 0),
    caption: '赞同和回答的总量，衡量讨论强度。',
  },
])

const laneHighlights = computed(() => [
  {
    label: '推荐动作',
    value: activeCount.value > 0 ? '去答疑' : '去提问',
    detail: activeCount.value > 0 ? '还有问题处于待解决状态，优先进入回答工作台。' : '热区偏平稳，适合补充新的真实问题。' ,
    dotClass: activeCount.value > 0 ? 'bg-[var(--bc-amber)]' : 'bg-[var(--bc-cyan)]',
  },
  {
    label: '最强信号',
    value: hottestQuestion.value ? `${hottestQuestion.value.upvoteCount + hottestQuestion.value.answerCount}` : '0',
    detail: hottestQuestion.value ? hottestQuestion.value.title : '暂无高热问题',
    dotClass: 'bg-[var(--bc-coral)]',
  },
  {
    label: '主要路径',
    value: topCategory.value,
    detail: '当前热区里讨论最密集的分类路径。',
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
    const key = item.categoryName || 'General'
    bucket.set(key, (bucket.get(key) ?? 0) + 1)
  })
  const top = [...bucket.entries()].sort((a, b) => b[1] - a[1])[0]
  return top?.[0] ?? 'General'
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

.contribution-gauge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 84px;
  height: 84px;
  border-radius: 999px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.24);
  background:
    radial-gradient(circle at center, rgba(var(--bc-accent-rgb), 0.18), transparent 62%),
    rgba(255, 255, 255, 0.38);
}

.dark .contribution-gauge {
  background:
    radial-gradient(circle at center, rgba(var(--bc-accent-rgb), 0.2), transparent 62%),
    rgba(255, 255, 255, 0.05);
}

.contribution-gauge__value {
  font-family: theme('fontFamily.mono');
  font-size: 26px;
  font-weight: 700;
  color: var(--bc-ink);
}

.contribution-node {
  border-radius: 18px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.36);
  padding: 14px;
}

.dark .contribution-node {
  background: rgba(255, 255, 255, 0.05);
}

.highlight-lane {
  border-radius: 18px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.32);
  padding: 14px 16px;
}

.dark .highlight-lane {
  background: rgba(255, 255, 255, 0.04);
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
  .question-card__signal {
    min-width: 100%;
  }
}

@media (max-width: 640px) {
  .community-search {
    grid-template-columns: minmax(0, 1fr);
  }

  .question-card__scoreboard {
    min-width: 100%;
  }
}
</style>
