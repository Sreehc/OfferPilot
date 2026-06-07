<template>
  <div class="community-detail space-y-5">
    <button
      class="inline-flex items-center gap-2 text-sm text-secondary transition-colors hover:text-ink"
      @click="$router.push('/community')"
    >
      <UiIcon name="back" class="h-4 w-4" /> 返回社区
    </button>

    <section v-if="loading" class="shell-section-card p-5 sm:p-6">
      <StateView variant="loading" :rows="4" />
    </section>

    <template v-else-if="question">
      <section class="forum-post shell-section-card p-5 sm:p-6">
        <div class="forum-post__body">
          <div class="forum-post__badges">
            <span class="forum-status-chip" :class="questionResolved(question) ? 'detail-chip-resolved' : 'detail-chip-active'">
              {{ questionResolved(question) ? '已解决' : '讨论中' }}
            </span>
            <span v-if="question.categoryName" class="detail-pill">{{ question.categoryName }}</span>
          </div>

          <h1 class="forum-post__title">{{ question.title }}</h1>

          <div class="forum-post__meta">
            <span>{{ question.authorName || '匿名用户' }}</span>
            <span>{{ formatTime(question.createdAt) }}</span>
            <span>{{ question.answers.length }} 条回复</span>
          </div>

          <div class="forum-post__content">
            {{ question.content }}
          </div>

          <div class="forum-post__actions">
            <button
              type="button"
              class="forum-inline-vote"
              :class="{ 'forum-inline-vote-active': question.hasVoted }"
              @click="voteQuestion"
            >
              <UiIcon name="top" class="h-4 w-4" />
              <span>{{ question.upvoteCount }}</span>
            </button>
            <button type="button" class="hard-button-primary" @click="focusAnswerComposer">写回复</button>
            <button
              v-if="isAuthor"
              type="button"
              class="hard-button-secondary"
              @click="handleEditQuestion"
            >
              编辑帖子
            </button>
            <button
              v-if="isAuthor"
              type="button"
              class="hard-button-secondary !border-red-300 !text-[var(--bc-coral)]"
              @click="handleDeleteQuestion"
            >
              删除帖子
            </button>
          </div>
        </div>
      </section>

      <section class="shell-section-card p-5 sm:p-6">
        <div class="forum-section-head">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">回复 {{ question.answers.length }}</h3>
          </div>
        </div>

        <div class="mt-6 space-y-4">
          <article v-if="acceptedAnswer" class="accepted-answer">
            <div class="forum-reply__body">
              <div class="forum-reply__head">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="hard-chip detail-chip-resolved !px-2 !py-0.5 !text-[9px]">最佳答案</span>
                  <span class="text-sm font-semibold text-ink">{{ acceptedAnswer.authorName || '匿名用户' }}</span>
                  <span v-if="acceptedAnswer.authorRank" class="detail-pill detail-pill-accent">{{ acceptedAnswer.authorRank }}</span>
                </div>
                <div class="flex flex-wrap items-center gap-2 text-xs text-secondary">
                  <span>{{ formatTime(acceptedAnswer.createdAt) }}</span>
                  <span class="forum-inline-vote forum-inline-vote-static">
                    <UiIcon name="top" class="h-4 w-4" />
                    <span>{{ acceptedAnswer.upvoteCount }}</span>
                  </span>
                  <button
                    v-if="canDeleteAnswer(acceptedAnswer)"
                    type="button"
                    class="forum-inline-action forum-inline-action-danger"
                    @click="handleDeleteAnswer(acceptedAnswer.id)"
                  >
                    删除
                  </button>
                </div>
              </div>

              <div class="forum-reply__content">{{ acceptedAnswer.content }}</div>
            </div>
          </article>

          <article v-for="(a, index) in orderedAnswers" :key="a.id" class="forum-reply">
            <div class="forum-reply__body">
              <div class="forum-reply__head">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="forum-floor-tag">#{{ index + 1 }}</span>
                  <span class="text-sm font-semibold text-ink">{{ a.authorName || '匿名用户' }}</span>
                  <span v-if="a.authorRank" class="detail-pill detail-pill-accent">{{ a.authorRank }}</span>
                </div>

                <div class="flex flex-wrap items-center gap-2 text-xs text-secondary">
                  <span>{{ formatTime(a.createdAt) }}</span>
                  <button
                    type="button"
                    class="forum-inline-vote"
                    :class="{ 'forum-inline-vote-active': a.hasVoted }"
                    @click="voteAnswer(a.id)"
                  >
                    <UiIcon name="top" class="h-4 w-4" />
                    <span>{{ a.upvoteCount }}</span>
                  </button>
                  <button
                    v-if="isAuthor && !a.isAccepted && !question.accepted"
                    type="button"
                    class="forum-inline-action"
                    @click="handleAccept(a.id)"
                  >
                    采纳
                  </button>
                  <button
                    v-if="canDeleteAnswer(a)"
                    type="button"
                    class="forum-inline-action forum-inline-action-danger"
                    @click="handleDeleteAnswer(a.id)"
                  >
                    删除
                  </button>
                </div>
              </div>

              <div class="forum-reply__content">{{ a.content }}</div>
            </div>
          </article>

          <StateView
            v-if="question.answers.length === 0"
            icon="chat"
            :title="EMPTY_STATE_COPY.communityReplies.title"
            :description="EMPTY_STATE_COPY.communityReplies.description"
            compact
          />
        </div>
      </section>

      <section class="shell-section-card p-5 sm:p-6">
        <div class="forum-section-head">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">写回复</h3>
          </div>
        </div>

        <div class="mt-5">
          <textarea
            ref="answerInputRef"
            v-model="answerContent"
            rows="6"
            placeholder="输入你的回复"
            class="answer-textarea"
          ></textarea>

          <div class="mt-4 flex justify-end">
            <button
              class="hard-button-primary"
              :disabled="!answerContent.trim() || submitting"
              @click="handleSubmitAnswer"
            >
              {{ submitting ? '提交中...' : '提交回复' }}
            </button>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { EMPTY_STATE_COPY } from '@/constants/productCopy'
import { StateView, UiIcon } from '@/components/ui'
import {
  acceptCommunityAnswerApi,
  deleteCommunityAnswerApi,
  deleteCommunityQuestionApi,
  fetchCommunityQuestionDetailApi,
  submitCommunityAnswerApi,
  voteCommunityApi
} from '@/api/community'
import type { CommunityQuestion, CommunityQuestionDetail } from '@/types/api'
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
    .filter((answer) => !answer.isAccepted)
    .sort((a, b) => b.upvoteCount - a.upvoteCount)
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
      content: answerContent.value.trim()
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

async function handleDeleteAnswer(answerId: number) {
  if (!confirm('确定删除这条回答吗？')) return
  await deleteCommunityAnswerApi(answerId)
  await fetchQuestion()
}

function handleEditQuestion() {
  if (!question.value) return
  void router.push({
    path: '/community/submit',
    query: {
      id: String(question.value.id)
    }
  })
}

function canDeleteAnswer(answer: CommunityQuestionDetail['answers'][number]) {
  if (!question.value) return false
  const currentUserId = authStore.user?.id
  return Boolean(currentUserId && (answer.userId === currentUserId || question.value.userId === currentUserId))
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
  if (minutes < 60) return `${Math.max(1, minutes)}分钟前`
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
.forum-post__badges,
.forum-post__meta,
.forum-post__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.forum-inline-action-danger {
  color: var(--bc-coral);
}

.forum-post__title {
  margin-top: 18px;
  color: var(--bc-ink);
  font-size: 36px;
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.16;
}

.forum-post__meta {
  margin-top: 14px;
  color: var(--bc-ink-secondary);
  font-size: 12px;
}

.forum-post__content,
.forum-reply__content {
  white-space: pre-wrap;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.95;
}

.dark .forum-post__content,
.dark .forum-reply__content {
  color: var(--text-primary);
}

.forum-post__content {
  margin-top: 20px;
  border-radius: 24px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.08);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.04), transparent 34%), var(--panel-muted);
  padding: 22px 24px;
  font-size: 15px;
  line-height: 2;
}

.dark .forum-post__content {
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.06), transparent 34%), var(--panel-muted);
}

.forum-post__actions {
  margin-top: 20px;
}

.forum-section-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.accepted-answer,
.forum-reply {
  border-radius: 24px;
  border: 1px solid var(--bc-line);
  background: var(--panel-bg);
  padding: 18px;
}

.dark .accepted-answer,
.dark .forum-reply {
  background: var(--panel-bg);
}

.accepted-answer {
  border-color: rgba(159, 232, 112, 0.26);
  background:
    linear-gradient(145deg, rgba(159, 232, 112, 0.08), transparent 38%),
    var(--panel-bg);
}

.dark .accepted-answer {
  background:
    linear-gradient(145deg, rgba(159, 232, 112, 0.1), transparent 38%),
    var(--panel-bg);
}

.forum-reply__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.forum-reply__content {
  margin-top: 16px;
}

.forum-floor-tag,
.detail-pill,
.forum-status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid var(--bc-line);
  padding: 6px 10px;
  font-size: 11px;
}

.forum-floor-tag,
.detail-pill {
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

.forum-inline-vote {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  border: 1px solid var(--bc-line);
  border-radius: 999px;
  background: var(--interactive-bg);
  padding: 0 10px;
  color: var(--bc-ink-secondary);
  font-size: 12px;
  font-weight: 600;
  transition:
    border-color var(--motion-base) var(--ease-hard),
    color var(--motion-base) var(--ease-hard),
    background-color var(--motion-base) var(--ease-hard);
}

.dark .forum-inline-vote {
  background: var(--interactive-bg);
}

.forum-inline-vote:hover,
.forum-inline-vote-active {
  border-color: rgba(var(--bc-accent-rgb), 0.24);
  color: var(--bc-accent);
  background: rgba(var(--bc-accent-rgb), 0.08);
}

.forum-inline-vote-static {
  cursor: default;
}

.forum-inline-action {
  border: 0;
  background: transparent;
  color: var(--bc-accent);
  font-size: 12px;
  font-weight: 600;
}

.answer-textarea {
  width: 100%;
  min-height: 180px;
  border: 1px solid var(--bc-border-strong);
  border-radius: 18px;
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

@media (max-width: 900px) {
  .forum-post__title {
    font-size: 28px;
  }
}

@media (max-width: 640px) {
  .forum-post__title {
    font-size: 24px;
  }

  .forum-post__content {
    padding: 18px;
    border-radius: 18px;
  }

  .accepted-answer,
  .forum-reply {
    padding: 16px;
    border-radius: 20px;
  }

  .answer-textarea {
    min-height: 160px;
  }
}
</style>
