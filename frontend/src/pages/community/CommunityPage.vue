<template>
  <div class="max-w-6xl mx-auto space-y-6">
    <!-- Header -->
    <div class="paper-panel px-8 py-6 flex items-center justify-between">
      <div>
        <p class="section-kicker">学习社区</p>
        <h2 class="mt-2 text-2xl font-semibold text-ink">提问、讨论、共同成长</h2>
      </div>
      <div class="flex gap-3">
        <button
          class="hard-button-primary px-5 py-2.5 text-sm"
          @click="$router.push('/community/submit')"
        >
          发起提问
        </button>
        <button
          class="px-5 py-2.5 text-sm font-semibold rounded-lg border border-slate-300 dark:border-slate-600 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors"
          @click="$router.push('/community/leaderboard')"
        >
          排行榜
        </button>
      </div>
    </div>

    <!-- Filters -->
    <div class="paper-panel px-8 py-4 flex items-center gap-4">
      <div class="flex gap-2">
        <button
          v-for="s in sorts"
          :key="s.value"
          class="px-4 py-2 text-sm font-medium rounded-lg transition-colors"
          :class="sort === s.value ? 'bg-accent/10 text-accent' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700'"
          @click="sort = s.value"
        >
          {{ s.label }}
        </button>
      </div>
      <div class="flex-1">
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索问题..."
          class="w-full max-w-sm px-4 py-2.5 text-sm bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-accent/30"
          @keyup.enter="doSearch"
        />
      </div>
    </div>

    <!-- Question List -->
    <div class="space-y-4">
      <div
        v-for="q in questions"
        :key="q.id"
        class="surface-card px-8 py-6 cursor-pointer hover:shadow-md transition-shadow"
        @click="$router.push(`/community/question/${q.id}`)"
      >
        <div class="flex items-start gap-4">
          <!-- Vote Count -->
          <div class="flex flex-col items-center gap-1 min-w-[48px]">
            <div class="text-lg font-bold" :class="q.upvoteCount > 0 ? 'text-accent' : 'text-slate-400'">
              {{ q.upvoteCount }}
            </div>
            <div class="text-[11px] text-slate-400">赞</div>
          </div>

          <!-- Answer Count -->
          <div class="flex flex-col items-center gap-1 min-w-[48px]">
            <div
              class="text-lg font-bold"
              :class="q.accepted ? 'text-green-600' : q.answerCount > 0 ? 'text-slate-700 dark:text-slate-200' : 'text-slate-400'"
            >
              {{ q.answerCount }}
            </div>
            <div class="text-[11px]" :class="q.accepted ? 'text-green-600' : 'text-slate-400'">
              {{ q.accepted ? '已解决' : '回答' }}
            </div>
          </div>

          <!-- Content -->
          <div class="flex-1 min-w-0">
            <h3 class="text-base font-semibold text-ink truncate">{{ q.title }}</h3>
            <p class="mt-1.5 text-sm text-slate-500 dark:text-slate-400 line-clamp-2">{{ q.content }}</p>
            <div class="mt-3 flex items-center gap-4 text-xs text-slate-400">
              <span>{{ q.authorName || '匿名用户' }}</span>
              <span v-if="q.authorRank" class="px-2 py-0.5 rounded bg-accent/10 text-accent">{{ q.authorRank }}</span>
              <span>{{ formatTime(q.createdAt) }}</span>
              <span v-if="q.categoryName" class="px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-700">{{ q.categoryName }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="!loading && questions.length === 0" class="paper-panel px-8 py-16 text-center">
        <p class="text-lg text-slate-400">暂无问题</p>
        <p class="mt-2 text-sm text-slate-400">成为第一个提问的人吧</p>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="flex justify-center gap-2 pb-8">
      <button
        v-for="p in displayPages"
        :key="p"
        class="w-10 h-10 rounded-lg text-sm font-medium transition-colors"
        :class="page === p ? 'bg-accent text-white' : 'bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700'"
        @click="page = p; fetchQuestions()"
      >
        {{ p }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { fetchCommunityQuestionsApi } from '@/api/community'
import type { CommunityQuestion } from '@/types/api'

const questions = ref<CommunityQuestion[]>([])
const page = ref(1)
const size = 20
const total = ref(0)
const totalPages = ref(0)
const sort = ref<'new' | 'hot'>('new')
const keyword = ref('')
const loading = ref(false)

const sorts = [
  { label: '最新', value: 'new' as const },
  { label: '最热', value: 'hot' as const },
]

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
  fetchQuestions()
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
  fetchQuestions()
})

onMounted(fetchQuestions)
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
