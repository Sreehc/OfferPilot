<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <!-- Back -->
    <button
      class="flex items-center gap-2 text-sm text-slate-500 hover:text-ink transition-colors"
      @click="$router.push('/community')"
    >
      <span>&larr;</span> 返回社区
    </button>

    <!-- Loading -->
    <div v-if="loading" class="paper-panel px-8 py-16 text-center text-slate-400">加载中...</div>

    <template v-else-if="question">
      <!-- Question -->
      <div class="paper-panel px-8 py-6">
        <div class="flex items-start gap-4">
          <!-- Vote -->
          <div class="flex flex-col items-center gap-2">
            <button
              class="w-10 h-10 rounded-lg flex items-center justify-center transition-colors"
              :class="question.hasVoted ? 'bg-accent/10 text-accent' : 'bg-slate-100 dark:bg-slate-700 text-slate-500 hover:bg-accent/10 hover:text-accent'"
              @click="voteQuestion"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 20 20" fill="currentColor">
                <path d="M2 10.5a1.5 1.5 0 113 0v6a1.5 1.5 0 01-3 0v-6zM6 10.333v5.43a2 2 0 001.106 1.79l.05.025A4 4 0 008.943 18h5.416a2 2 0 001.962-1.608l1.2-6A2 2 0 0015.56 8H12V4a2 2 0 00-2-2 1 1 0 00-1 1v.667a4 4 0 01-.8 2.4L6.8 7.933a4 4 0 00-.8 2.4z" />
              </svg>
            </button>
            <span class="text-lg font-bold" :class="question.upvoteCount > 0 ? 'text-accent' : 'text-slate-400'">
              {{ question.upvoteCount }}
            </span>
          </div>

          <!-- Content -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-3 mb-3">
              <span
                v-if="question.status === 'resolved'"
                class="px-2.5 py-1 text-xs font-medium rounded bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
              >
                已解决
              </span>
              <span
                v-else
                class="px-2.5 py-1 text-xs font-medium rounded bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400"
              >
                讨论中
              </span>
              <span v-if="question.categoryName" class="text-xs text-slate-400">{{ question.categoryName }}</span>
            </div>

            <h1 class="text-xl font-semibold text-ink">{{ question.title }}</h1>

            <div class="mt-3 flex items-center gap-3 text-xs text-slate-400">
              <span class="font-medium text-slate-600 dark:text-slate-300">{{ question.authorName || '匿名用户' }}</span>
              <span v-if="question.authorRank" class="px-2 py-0.5 rounded bg-accent/10 text-accent">{{ question.authorRank }}</span>
              <span>{{ formatTime(question.createdAt) }}</span>
            </div>

            <div class="mt-4 text-sm leading-7 text-slate-700 dark:text-slate-200 whitespace-pre-wrap">{{ question.content }}</div>

            <!-- Delete Button -->
            <div v-if="isAuthor" class="mt-4 flex gap-2">
              <button
                class="px-3 py-1.5 text-xs font-medium rounded border border-red-300 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                @click="handleDeleteQuestion"
              >
                删除问题
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Answers -->
      <div class="paper-panel px-8 py-6">
        <h3 class="text-base font-semibold text-ink mb-4">
          {{ question.answers.length }} 条回答
        </h3>

        <div class="space-y-4">
          <div
            v-for="a in question.answers"
            :key="a.id"
            class="flex items-start gap-4 py-4 border-b border-slate-100 dark:border-slate-800 last:border-0"
          >
            <!-- Vote -->
            <div class="flex flex-col items-center gap-2">
              <button
                class="w-8 h-8 rounded-lg flex items-center justify-center transition-colors"
                :class="a.hasVoted ? 'bg-accent/10 text-accent' : 'bg-slate-100 dark:bg-slate-700 text-slate-500 hover:bg-accent/10 hover:text-accent'"
                @click="voteAnswer(a.id)"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M2 10.5a1.5 1.5 0 113 0v6a1.5 1.5 0 01-3 0v-6zM6 10.333v5.43a2 2 0 001.106 1.79l.05.025A4 4 0 008.943 18h5.416a2 2 0 001.962-1.608l1.2-6A2 2 0 0015.56 8H12V4a2 2 0 00-2-2 1 1 0 00-1 1v.667a4 4 0 01-.8 2.4L6.8 7.933a4 4 0 00-.8 2.4z" />
                </svg>
              </button>
              <span class="text-sm font-bold" :class="a.upvoteCount > 0 ? 'text-accent' : 'text-slate-400'">
                {{ a.upvoteCount }}
              </span>
            </div>

            <!-- Content -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-3 mb-2">
                <span v-if="a.isAccepted" class="px-2.5 py-1 text-xs font-medium rounded bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">
                  已采纳
                </span>
                <span class="text-xs font-medium text-slate-600 dark:text-slate-300">{{ a.authorName || '匿名用户' }}</span>
                <span v-if="a.authorRank" class="px-2 py-0.5 rounded bg-accent/10 text-accent text-xs">{{ a.authorRank }}</span>
                <span class="text-xs text-slate-400">{{ formatTime(a.createdAt) }}</span>
              </div>

              <div class="text-sm leading-7 text-slate-700 dark:text-slate-200 whitespace-pre-wrap">{{ a.content }}</div>

              <div class="mt-3 flex gap-2">
                <button
                  v-if="isAuthor && !a.isAccepted && !question.accepted"
                  class="px-3 py-1.5 text-xs font-medium rounded bg-green-600 text-white hover:bg-green-700 transition-colors"
                  @click="handleAccept(a.id)"
                >
                  采纳此回答
                </button>
              </div>
            </div>
          </div>

          <!-- No Answers -->
          <div v-if="question.answers.length === 0" class="py-8 text-center text-slate-400 text-sm">
            暂无回答，快来第一个回答吧
          </div>
        </div>
      </div>

      <!-- Submit Answer -->
      <div class="paper-panel px-8 py-6">
        <h3 class="text-base font-semibold text-ink mb-4">发表回答</h3>
        <textarea
          v-model="answerContent"
          rows="5"
          placeholder="分享你的见解..."
          class="w-full px-4 py-3 text-sm bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-accent/30 resize-none"
        ></textarea>
        <div class="mt-3 flex justify-end">
          <button
            class="hard-button-primary px-5 py-2.5 text-sm disabled:opacity-50"
            :disabled="!answerContent.trim() || submitting"
            @click="handleSubmitAnswer"
          >
            {{ submitting ? '提交中...' : '提交回答' }}
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchCommunityQuestionDetailApi,
  submitCommunityAnswerApi,
  acceptCommunityAnswerApi,
  voteCommunityApi,
  deleteCommunityQuestionApi,
} from '@/api/community'
import type { CommunityQuestionDetail } from '@/types/api'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const question = ref<CommunityQuestionDetail | null>(null)
const loading = ref(true)
const answerContent = ref('')
const submitting = ref(false)

const questionId = computed(() => Number(route.params.id))
const isAuthor = computed(() => question.value && authStore.user?.id === question.value.userId)

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
  const answer = question.value.answers.find(a => a.id === answerId)
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
  } finally {
    submitting.value = false
  }
}

async function handleDeleteQuestion() {
  if (!confirm('确定删除此问题吗？')) return
  await deleteCommunityQuestionApi(questionId.value)
  router.push('/community')
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

onMounted(fetchQuestion)
</script>
