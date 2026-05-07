<template>
  <div class="space-y-6">
    <!-- Back Link -->
    <div>
      <button type="button" class="flex items-center gap-1 text-sm text-slate-500 dark:text-slate-400 transition hover:text-accent" @click="router.back()">
        <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
        </svg>
        返回面试历史
      </button>
    </div>

    <!-- Loading -->
    <section v-if="loading" class="paper-panel p-8 text-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      <p class="mt-4 text-sm text-slate-500 dark:text-slate-400">加载面试详情...</p>
    </section>

    <!-- Not Found -->
    <section v-else-if="!detail" class="paper-panel p-8 text-center">
      <p class="text-lg font-semibold text-ink">面试记录未找到</p>
      <RouterLink to="/interview/history" class="hard-button-primary mt-4 inline-flex">返回历史</RouterLink>
    </section>

    <template v-else>
      <section class="paper-panel p-6">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p class="section-kicker">总览</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
              {{ detail.direction }} 方向面试
              <span v-if="detail.mode === 'voice'" class="ml-2 inline-flex items-center rounded-full bg-accent/10 px-2.5 py-0.5 text-xs font-semibold text-accent">语音</span>
            </h3>
            <div class="mt-2 flex items-center gap-4 text-sm text-slate-500 dark:text-slate-400">
              <span>{{ detail.questionCount }} 题</span>
              <span v-if="detail.startTime">{{ formatTime(detail.startTime) }}</span>
              <span v-if="detail.endTime">~ {{ formatTime(detail.endTime) }}</span>
            </div>
          </div>
          <div
            class="p-6 text-white shadow-[0_16px_36px_rgba(47,79,157,0.18)]"
            style="border-radius: var(--radius-lg); background: linear-gradient(135deg, #365ab0 0%, #233d79 100%);"
          >
            <div class="text-xs uppercase tracking-[0.24em] text-white/60">总分</div>
            <div class="mt-2 text-5xl font-semibold tracking-[-0.03em]">
              {{ formatScore(detail.totalScore) }}
            </div>
          </div>
        </div>
      </section>

      <section class="paper-panel p-6">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p class="section-kicker">逐题复盘</p>
            <h4 class="mt-2 text-xl font-semibold text-ink">按题目对比你的回答与标准答案</h4>
            <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">
              每题都按“题目、点评、我的回答、标准答案、追问”的固定顺序展示，方便快速扫读。
            </p>
          </div>
          <RouterLink to="/interview" class="text-sm font-semibold text-accent hover:underline">
            再来一场类似练习
          </RouterLink>
        </div>
      </section>

      <section
        v-for="(record, index) in detail.records"
        :key="record.questionId"
        class="paper-panel overflow-hidden"
      >
        <div class="flex items-center justify-between border-b border-slate-200/60 dark:border-slate-700/60 px-6 py-4">
          <div class="flex items-center gap-3">
            <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-accent text-sm font-bold text-white">
              {{ index + 1 }}
            </span>
            <span class="font-semibold text-ink">{{ record.questionTitle }}</span>
          </div>
          <span
            class="text-2xl font-semibold tracking-[-0.03em]"
            :class="record.score >= 60 ? 'text-accent' : 'text-red-500'"
          >
            {{ formatScore(record.score) }}
          </span>
        </div>

        <div class="surface-muted mx-6 mt-4 rounded-lg p-4">
          <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">本题点评</div>
          <p class="mt-1 text-sm leading-6 text-slate-700 dark:text-slate-200">{{ record.comment || '暂无点评' }}</p>
        </div>

        <div v-if="record.voiceTranscript" class="mx-6 mt-4 surface-card p-4">
          <div class="flex items-center gap-2">
            <div class="h-2 w-2 rounded-full bg-purple-400"></div>
            <span class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">语音转录</span>
            <span v-if="record.voiceConfidence" class="ml-auto text-xs text-slate-400">
              置信度 {{ Math.round(record.voiceConfidence * 100) }}%
            </span>
          </div>
          <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-700 dark:text-slate-200">
            {{ record.voiceTranscript }}
          </p>
        </div>

        <div class="mt-4 grid gap-4 px-6 pb-5 md:grid-cols-2">
          <div class="surface-card p-4">
            <div class="flex items-center gap-2">
              <div class="h-2 w-2 rounded-full bg-amber-400"></div>
              <span class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">{{ record.voiceTranscript ? '转录文本' : '我的回答' }}</span>
            </div>
            <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-700 dark:text-slate-200">
              {{ record.userAnswer || '未作答' }}
            </p>
          </div>

          <div class="surface-card p-4">
            <div class="flex items-center gap-2">
              <div class="h-2 w-2 rounded-full bg-green-400"></div>
              <span class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">标准答案</span>
            </div>
            <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-700 dark:text-slate-200">
              {{ record.standardAnswer || '暂无标准答案' }}
            </p>
          </div>
        </div>

        <div v-if="record.followUp" class="border-t border-slate-200/60 dark:border-slate-700/60 px-6 py-4">
          <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">追问</div>
          <p class="mt-1 text-sm leading-6 text-slate-600 dark:text-slate-300">{{ record.followUp }}</p>
        </div>
      </section>

      <section class="paper-panel p-6">
        <p class="section-kicker">下一步</p>
        <h4 class="mt-2 text-xl font-semibold text-ink">继续练习，或先回到错题与历史记录</h4>
        <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">
          如果某几题得分明显偏低，建议先去错题本和复习页处理，再开始下一场。
        </p>
      </section>

      <section class="flex gap-3">
        <RouterLink to="/interview/history" class="hard-button-secondary flex-1 text-center">
          查看历史
        </RouterLink>
        <RouterLink to="/interview" class="hard-button-primary flex-1 text-center">
          再来一场
        </RouterLink>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { interviewDetailApi } from '@/api/interview'
import type { InterviewDetail } from '@/types/api'

const route = useRoute()
const router = useRouter()

const detail = ref<InterviewDetail | null>(null)
const loading = ref(true)

const formatScore = (score: number | undefined | null): string => {
  if (score == null) return '-'
  return Number.isInteger(score) ? String(score) : Number(score).toFixed(2)
}

const formatTime = (time: string): string => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const loadData = async () => {
  const sessionId = String(route.params.id || '')
  if (!sessionId) {
    loading.value = false
    return
  }
  try {
    const response = await interviewDetailApi(sessionId)
    detail.value = response.data
  } catch {
    ElMessage.error('加载面试详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>
