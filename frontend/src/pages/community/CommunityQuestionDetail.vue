<template>
  <div class="community-detail space-y-6">
    <button
      class="inline-flex items-center gap-2 text-sm text-slate-500 transition-colors hover:text-ink"
      @click="$router.push('/community')"
    >
      <span>&larr;</span> 返回社区
    </button>

    <section v-if="loading" class="cockpit-panel px-8 py-16 text-center text-slate-400">
      正在同步问题工作台...
    </section>

    <template v-else-if="question">
      <section class="grid gap-4 xl:grid-cols-[minmax(0,1.08fr)_minmax(320px,0.92fr)]">
        <article class="cockpit-panel p-5 sm:p-6">
          <div class="flex flex-col gap-5 lg:flex-row">
            <div class="question-vote-rail">
              <button
                class="question-vote-rail__button"
                :class="{ 'question-vote-rail__button-active': question.hasVoted }"
                @click="voteQuestion"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M2 10.5a1.5 1.5 0 113 0v6a1.5 1.5 0 01-3 0v-6zM6 10.333v5.43a2 2 0 001.106 1.79l.05.025A4 4 0 008.943 18h5.416a2 2 0 001.962-1.608l1.2-6A2 2 0 0015.56 8H12V4a2 2 0 00-2-2 1 1 0 00-1 1v.667a4 4 0 01-.8 2.4L6.8 7.933a4 4 0 00-.8 2.4z" />
                </svg>
              </button>
              <span class="question-vote-rail__value">{{ question.upvoteCount }}</span>
              <span class="question-vote-rail__label">赞同</span>
            </div>

            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center gap-2">
                <span class="hard-chip !px-2 !py-0.5 !text-[9px]" :class="questionResolved(question) ? 'detail-chip-resolved' : 'detail-chip-active'">
                  {{ questionResolved(question) ? '已解决' : '讨论中' }}
                </span>
                <span v-if="question.categoryName" class="detail-pill">{{ question.categoryName }}</span>
                <span class="detail-pill">{{ question.answers.length }} 条回答</span>
              </div>

              <h1 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink">{{ question.title }}</h1>

              <div class="mt-4 flex flex-wrap items-center gap-3 text-xs text-slate-500 dark:text-slate-400">
                <span>{{ question.authorName || '匿名用户' }}</span>
                <span v-if="question.authorRank" class="detail-pill detail-pill-accent">{{ question.authorRank }}</span>
                <span>{{ formatTime(question.createdAt) }}</span>
                <span>{{ questionResolved(question) ? '已形成可复盘答案' : '仍在等待更完整回答' }}</span>
              </div>

              <div class="mt-5 rounded-[24px] border border-[var(--bc-line)] bg-white/36 p-5 text-sm leading-8 text-slate-700 dark:bg-white/5 dark:text-slate-200 whitespace-pre-wrap">
                {{ question.content }}
              </div>

              <div class="mt-5 flex flex-wrap gap-3">
                <button
                  v-if="isAuthor"
                  type="button"
                  class="hard-button-secondary !border-red-300 !text-[var(--bc-coral)]"
                  @click="handleDeleteQuestion"
                >
                  删除问题
                </button>
                <button
                  type="button"
                  class="hard-button-secondary"
                  @click="focusAnswerComposer"
                >
                  直接回答
                </button>
              </div>
            </div>
          </div>
        </article>

        <aside class="cockpit-panel p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="section-kicker">Answer Deck</p>
              <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">回答态势板</h3>
            </div>
            <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ questionResolved(question) ? 'Resolved' : 'Open' }}</span>
          </div>

          <div class="mt-5 grid gap-3 sm:grid-cols-3">
            <article v-for="signal in answerSignals" :key="signal.label" class="data-slab p-4" :class="signal.toneClass">
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ signal.label }}</p>
              <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ signal.value }}</p>
              <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ signal.detail }}</p>
            </article>
          </div>

          <div class="mt-5 space-y-3">
            <article v-for="lane in answerLanes" :key="lane.label" class="answer-lane">
              <div class="flex items-center justify-between gap-3">
                <div class="flex items-center gap-3">
                  <span class="inline-flex h-2.5 w-2.5 rounded-full" :class="lane.dotClass"></span>
                  <div>
                    <p class="text-sm font-semibold text-ink">{{ lane.label }}</p>
                    <p class="text-xs text-slate-500 dark:text-slate-400">{{ lane.detail }}</p>
                  </div>
                </div>
                <span class="font-mono text-sm font-semibold text-ink">{{ lane.value }}</span>
              </div>
            </article>
          </div>
        </aside>
      </section>

      <section class="cockpit-panel p-5 sm:p-6">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p class="section-kicker">Answer Workspace</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">回答工作台</h3>
          </div>
          <div class="flex flex-wrap gap-2">
            <span class="detail-pill">{{ question.answers.length }} 条回答</span>
            <span class="detail-pill">{{ acceptedAnswer ? '已存在最佳答案' : '等待最佳答案' }}</span>
          </div>
        </div>

        <div class="mt-6 space-y-4">
          <article
            v-if="acceptedAnswer"
            class="accepted-answer"
          >
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div class="flex items-center gap-3">
                <span class="hard-chip detail-chip-resolved !px-2 !py-0.5 !text-[9px]">最佳答案</span>
                <span class="text-sm font-semibold text-ink">{{ acceptedAnswer.authorName || '匿名用户' }}</span>
                <span v-if="acceptedAnswer.authorRank" class="detail-pill detail-pill-accent">{{ acceptedAnswer.authorRank }}</span>
              </div>
              <span class="text-xs text-slate-500 dark:text-slate-400">{{ formatTime(acceptedAnswer.createdAt) }}</span>
            </div>
            <div class="mt-4 text-sm leading-8 text-slate-700 dark:text-slate-200 whitespace-pre-wrap">{{ acceptedAnswer.content }}</div>
          </article>

          <article
            v-for="a in orderedAnswers"
            :key="a.id"
            class="answer-card"
            :class="{ 'answer-card-accepted': a.isAccepted }"
          >
            <div class="flex flex-col gap-4 lg:flex-row">
              <div class="answer-vote-rail">
                <button
                  class="answer-vote-rail__button"
                  :class="{ 'answer-vote-rail__button-active': a.hasVoted }"
                  @click="voteAnswer(a.id)"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M2 10.5a1.5 1.5 0 113 0v6a1.5 1.5 0 01-3 0v-6zM6 10.333v5.43a2 2 0 001.106 1.79l.05.025A4 4 0 008.943 18h5.416a2 2 0 001.962-1.608l1.2-6A2 2 0 0015.56 8H12V4a2 2 0 00-2-2 1 1 0 00-1 1v.667a4 4 0 01-.8 2.4L6.8 7.933a4 4 0 00-.8 2.4z" />
                  </svg>
                </button>
                <span class="answer-vote-rail__value">{{ a.upvoteCount }}</span>
              </div>

              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <span v-if="a.isAccepted" class="hard-chip detail-chip-resolved !px-2 !py-0.5 !text-[9px]">已采纳</span>
                  <span class="text-sm font-semibold text-ink">{{ a.authorName || '匿名用户' }}</span>
                  <span v-if="a.authorRank" class="detail-pill detail-pill-accent">{{ a.authorRank }}</span>
                  <span class="text-xs text-slate-500 dark:text-slate-400">{{ formatTime(a.createdAt) }}</span>
                </div>

                <div class="mt-4 text-sm leading-8 text-slate-700 dark:text-slate-200 whitespace-pre-wrap">{{ a.content }}</div>

                <div class="mt-4 flex flex-wrap gap-3">
                  <button
                    v-if="isAuthor && !a.isAccepted && !question.accepted"
                    type="button"
                    class="hard-button-primary !min-h-10 !px-4 text-xs"
                    @click="handleAccept(a.id)"
                  >
                    采纳此回答
                  </button>
                  <span class="detail-pill">{{ a.upvoteCount }} 次赞同</span>
                </div>
              </div>
            </div>
          </article>

          <EmptyState
            v-if="question.answers.length === 0"
            class="empty-state-card"
            icon="chat"
            title="还没有回答"
            description="现在就写下第一个回答，帮助这个问题形成可执行结论。"
            compact
          />
        </div>
      </section>

      <section class="cockpit-panel p-5 sm:p-6">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p class="section-kicker">Reply Console</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">发表回答</h3>
            <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
              直接给出结论、推理链条和必要前提。如果你是在解释原理，尽量把“为什么”讲清楚，而不是只给结论。
            </p>
          </div>
          <span class="detail-pill">可先写结论，再补过程</span>
        </div>

        <div class="mt-6 grid gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
          <div>
            <textarea
              ref="answerInputRef"
              v-model="answerContent"
              rows="8"
              placeholder="建议结构：1. 先给结论；2. 再说明推理链；3. 如有误区，补充为什么很多人会混淆。"
              class="answer-textarea"
            ></textarea>
            <div class="mt-4 flex justify-end">
              <button
                class="hard-button-primary"
                :disabled="!answerContent.trim() || submitting"
                @click="handleSubmitAnswer"
              >
                {{ submitting ? '提交中...' : '提交回答' }}
              </button>
            </div>
          </div>

          <aside class="compose-side space-y-4">
            <article class="compose-note">
              <p class="text-sm font-semibold text-ink">回答建议</p>
              <ul class="mt-3 space-y-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
                <li>优先解决提问者当前卡住的那个点，而不是泛泛介绍整套知识。</li>
                <li>如果问题和面试表达有关，补一段适合口头回答的简化版本。</li>
                <li>如果你不同意已有答案，直接指出分歧点和依据。</li>
              </ul>
            </article>

            <article class="compose-note">
              <p class="text-sm font-semibold text-ink">采纳规则</p>
              <div class="mt-3 space-y-3 text-sm leading-6 text-slate-600 dark:text-slate-300">
                <p>只有提问者可以采纳回答。</p>
                <p>采纳后问题会进入“已解决”状态，成为社区里可复盘的训练资产。</p>
              </div>
            </article>
          </aside>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'
import {
  acceptCommunityAnswerApi,
  deleteCommunityQuestionApi,
  fetchCommunityQuestionDetailApi,
  submitCommunityAnswerApi,
  voteCommunityApi,
} from '@/api/community'
import type { CommunityAnswer, CommunityQuestion, CommunityQuestionDetail } from '@/types/api'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const question = ref<CommunityQuestionDetail | null>(null)
const loading = ref(true)
const answerContent = ref('')
const submitting = ref(false)
const answerInputRef = ref<HTMLTextAreaElement | null>(null)

const questionId = computed(() => Number(route.params.id))
const isAuthor = computed(() => !!question.value && authStore.user?.id === question.value.userId)
const acceptedAnswer = computed(() => question.value?.answers.find((answer) => answer.isAccepted) ?? null)
const orderedAnswers = computed(() => {
  if (!question.value) return []
  return [...question.value.answers]
    .sort((a, b) => Number(b.isAccepted) - Number(a.isAccepted) || b.upvoteCount - a.upvoteCount)
})

const answerSignals = computed(() => {
  const currentQuestion = question.value
  if (!currentQuestion) return []
  const totalVotes = currentQuestion.answers.reduce((sum, answer) => sum + answer.upvoteCount, 0)
  return [
    {
      label: 'Answers',
      value: currentQuestion.answers.length,
      detail: '当前问题已经收到的回答数量。',
      toneClass: '',
    },
    {
      label: 'Best Answer',
      value: acceptedAnswer.value ? '1' : '0',
      detail: acceptedAnswer.value ? '已经存在最佳答案。' : '提问者尚未采纳回答。',
      toneClass: 'detail-slab-lime',
    },
    {
      label: 'Support',
      value: totalVotes,
      detail: '所有回答累计收到的赞同数量。',
      toneClass: 'detail-slab-cyan',
    },
  ]
})

const answerLanes = computed(() => {
  const currentQuestion = question.value
  if (!currentQuestion) return []
  return [
    {
      label: '问题状态',
      value: questionResolved(currentQuestion) ? 'Resolved' : 'Open',
      detail: questionResolved(currentQuestion) ? '已经形成明确答案。' : '仍然需要补充结论或推理。',
      dotClass: questionResolved(currentQuestion) ? 'bg-[var(--bc-lime)]' : 'bg-[var(--bc-amber)]',
    },
    {
      label: '最高赞同',
      value: currentQuestion.answers.length ? Math.max(...currentQuestion.answers.map((answer) => answer.upvoteCount)) : 0,
      detail: '当前最受认可的回答支持度。',
      dotClass: 'bg-[var(--bc-cyan)]',
    },
    {
      label: '你的权限',
      value: isAuthor.value ? '可采纳' : '可回答',
      detail: isAuthor.value ? '你可以把最合适的回答采纳为最佳答案。' : '你可以直接补充自己的观点。',
      dotClass: 'bg-[var(--bc-amber)]',
    },
  ]
})

async function fetchQuestion() {
  loading.value = true
  try {
    const { data } = await fetchCommunityQuestionDetailApi(questionId.value)
    question.value = data
  } finally {
    loading.value = false
  }
}

async function voteQuestion() {
  if (!question.value || question.value.hasVoted) return
  await voteCommunityApi({ targetType: 'question', targetId: question.value.id })
  question.value.upvoteCount++
  question.value.hasVoted = true
}

async function voteAnswer(answerId: number) {
  if (!question.value) return
  const answer = question.value.answers.find((item) => item.id === answerId)
  if (!answer || answer.hasVoted) return
  await voteCommunityApi({ targetType: 'answer', targetId: answerId })
  answer.upvoteCount++
  answer.hasVoted = true
}

async function handleAccept(answerId: number) {
  if (!question.value) return
  await acceptCommunityAnswerApi(question.value.id, answerId)
  await fetchQuestion()
}

async function handleSubmitAnswer() {
  if (!answerContent.value.trim() || !question.value) return
  submitting.value = true
  try {
    await submitCommunityAnswerApi({
      questionId: question.value.id,
      content: answerContent.value.trim(),
    })
    answerContent.value = ''
    await fetchQuestion()
    focusAnswerComposer()
  } finally {
    submitting.value = false
  }
}

async function handleDeleteQuestion() {
  if (!confirm('确定删除此问题吗？')) return
  await deleteCommunityQuestionApi(questionId.value)
  await router.push('/community')
}

function questionResolved(target: CommunityQuestion | CommunityQuestionDetail) {
  return target.accepted || target.status === 'resolved'
}

function focusAnswerComposer() {
  nextTick(() => {
    answerInputRef.value?.focus()
  })
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

onMounted(() => {
  void fetchQuestion()
})
</script>

<style scoped>
.question-vote-rail,
.answer-vote-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  min-width: 72px;
  border-radius: 24px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.36);
  padding: 16px 12px;
  flex-shrink: 0;
}

.dark .question-vote-rail,
.dark .answer-vote-rail {
  background: rgba(255, 255, 255, 0.05);
}

.question-vote-rail__button,
.answer-vote-rail__button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 0;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.6);
  color: var(--bc-ink-secondary);
  transition:
    background-color var(--motion-base) var(--ease-hard),
    color var(--motion-base) var(--ease-hard),
    transform var(--motion-base) var(--ease-hard);
}

.dark .question-vote-rail__button,
.dark .answer-vote-rail__button {
  background: rgba(255, 255, 255, 0.08);
}

.question-vote-rail__button-active,
.answer-vote-rail__button-active,
.question-vote-rail__button:hover,
.answer-vote-rail__button:hover {
  background: rgba(var(--bc-accent-rgb), 0.14);
  color: var(--bc-accent);
}

.question-vote-rail__value,
.answer-vote-rail__value {
  font-family: theme('fontFamily.mono');
  font-size: 24px;
  font-weight: 700;
  color: var(--bc-ink);
}

.question-vote-rail__label {
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--bc-ink-secondary);
}

.detail-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid var(--bc-line);
  padding: 6px 10px;
  font-size: 11px;
  color: var(--bc-ink-secondary);
}

.detail-pill-accent {
  border-color: rgba(var(--bc-accent-rgb), 0.24);
  color: var(--bc-accent);
  background: rgba(var(--bc-accent-rgb), 0.08);
}

.detail-chip-active {
  color: var(--bc-amber);
  background: rgba(255, 183, 77, 0.12);
}

.detail-chip-resolved {
  color: var(--bc-lime);
  background: rgba(159, 232, 112, 0.12);
}

.detail-slab-cyan {
  border-left-color: var(--bc-cyan);
}

.detail-slab-lime {
  border-left-color: var(--bc-lime);
}

.answer-lane,
.compose-note,
.accepted-answer,
.answer-card {
  border-radius: 24px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.34);
  padding: 16px;
}

.dark .answer-lane,
.dark .compose-note,
.dark .accepted-answer,
.dark .answer-card {
  background: rgba(255, 255, 255, 0.05);
}

.accepted-answer {
  border-color: rgba(159, 232, 112, 0.26);
  background:
    linear-gradient(145deg, rgba(159, 232, 112, 0.08), transparent 38%),
    rgba(255, 255, 255, 0.34);
}

.dark .accepted-answer {
  background:
    linear-gradient(145deg, rgba(159, 232, 112, 0.09), transparent 38%),
    rgba(255, 255, 255, 0.05);
}

.answer-card-accepted {
  border-color: rgba(159, 232, 112, 0.24);
}

.answer-textarea {
  width: 100%;
  min-height: 240px;
  border: 1px solid var(--bc-border-strong);
  border-radius: var(--radius-md);
  background: var(--bc-surface-input);
  color: var(--bc-ink);
  padding: 16px 18px;
  font-size: 14px;
  line-height: 1.8;
  resize: vertical;
}

.answer-textarea:focus {
  outline: 0;
  box-shadow: 0 0 0 4px rgba(var(--bc-accent-rgb), 0.1);
  border-color: rgba(var(--bc-accent-rgb), 0.42);
}

@media (max-width: 1024px) {
  .question-vote-rail,
  .answer-vote-rail {
    min-width: 100%;
    flex-direction: row;
    justify-content: center;
  }
}
</style>
