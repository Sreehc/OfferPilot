<template>
  <div class="space-y-6">
    <div>
      <button
        type="button"
        class="flex items-center gap-1 text-sm text-secondary transition hover:text-accent"
        @click="router.back()"
      >
        <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
        </svg>
        返回面试历史
      </button>
    </div>

    <section v-if="loading" class="shell-section-card p-8 text-center">
      <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
      <p class="mt-4 text-sm text-secondary">正在加载...</p>
    </section>

    <section v-else-if="!detail" class="shell-section-card p-8 text-center">
      <p class="text-lg font-semibold text-ink">面试记录未找到</p>
      <RouterLink to="/interview/history" class="hard-button-primary mt-4 inline-flex">返回历史</RouterLink>
    </section>

    <template v-else>
      <section class="shell-section-card p-6">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div class="mt-3 flex flex-wrap items-center gap-3">
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">
                {{ detail.direction }} 方向面试诊断
              </h3>
            </div>
            <div class="mt-2 flex flex-wrap items-center gap-4 text-sm text-secondary">
              <span>{{ detail.mode === 'voice' ? '语音面试' : '文本面试' }}</span>
              <span>{{ detail.questionCount }} 题</span>
              <span>{{ detail.cardsGenerated ? `已生成 ${detail.generatedCardCount || 0} 张复习卡片` : '未生成复习卡片' }}</span>
              <span v-if="detail.startTime">{{ formatTime(detail.startTime) }}</span>
              <span v-if="detail.endTime">~ {{ formatTime(detail.endTime) }}</span>
            </div>
          </div>
          <div class="flex flex-col gap-3 lg:items-end">
            <div class="interview-score-card p-6 text-white">
              <div class="text-xs uppercase tracking-[0.24em] text-white/60">总分</div>
              <div class="mt-2 text-5xl font-semibold tracking-[-0.03em]">
                {{ formatScore(detail.totalScore) }}
              </div>
            </div>
            <div class="flex flex-wrap gap-3">
              <el-button
                v-if="!detail.cardsGenerated"
                :loading="generating"
                size="large"
                class="hard-button-primary"
                @click="handleGenerateCards"
              >
                生成复习卡片
              </el-button>
              <el-button
                v-else
                :loading="activating"
                size="large"
                class="hard-button-primary"
                @click="handleActivateCards"
              >
                加入今日卡片
              </el-button>
              <RouterLink to="/cards" class="hard-button-secondary inline-flex items-center justify-center px-4">
                查看卡片工作台
              </RouterLink>
            </div>
          </div>
        </div>
      </section>

      <section
        v-for="(record, index) in sortedRecords"
        :key="record.questionId"
        class="shell-section-card overflow-hidden"
      >
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200/60 px-6 py-4 dark:border-slate-700/60">
          <div class="flex min-w-0 items-center gap-3">
            <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-accent text-sm font-bold text-white">
              {{ index + 1 }}
            </span>
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <span class="font-semibold text-ink">{{ record.questionTitle }}</span>
                <span
                  v-if="record.isLowScore"
                  class="inline-flex items-center rounded-full bg-coral/10 px-2 py-0.5 text-xs font-semibold text-coral"
                >
                  低分题
                </span>
              </div>
            </div>
          </div>
          <span
            class="text-2xl font-semibold tracking-[-0.03em]"
            :class="record.score >= 60 ? 'text-accent' : 'text-red-500'"
          >
            {{ formatScore(record.score) }}
          </span>
        </div>

        <div class="surface-muted mx-6 mt-4 rounded-lg p-4">
          <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">本题点评</div>
          <p class="mt-1 text-sm leading-6 text-primary">{{ record.comment || '暂无点评' }}</p>
        </div>

        <div v-if="record.voiceTranscript" class="mx-6 mt-4 surface-card p-4">
          <div class="flex items-center gap-2">
            <div class="h-2 w-2 rounded-full bg-purple-400"></div>
            <span class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">语音转录</span>
            <span v-if="record.voiceConfidence" class="ml-auto text-xs text-tertiary">
              置信度 {{ Math.round(record.voiceConfidence * 100) }}%
            </span>
          </div>
          <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-primary">
            {{ record.voiceTranscript }}
          </p>
        </div>

        <div class="mt-4 grid gap-4 px-6 pb-5 md:grid-cols-2">
          <div class="surface-card p-4">
            <div class="flex items-center gap-2">
              <div class="h-2 w-2 rounded-full bg-amber-400"></div>
              <span class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">
                {{ record.voiceTranscript ? '转录文本' : '我的回答' }}
              </span>
            </div>
            <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-primary">
              {{ record.userAnswer || '未作答' }}
            </p>
          </div>

          <div class="surface-card p-4">
            <div class="flex items-center gap-2">
              <div class="h-2 w-2 rounded-full bg-green-400"></div>
              <span class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">标准答案</span>
            </div>
            <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-primary">
              {{ record.standardAnswer || '暂无标准答案' }}
            </p>
          </div>
        </div>

        <div v-if="record.isLowScore" class="mx-6 mb-5 rounded-2xl border border-coral/20 bg-coral/5 p-4">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <div class="text-xs font-semibold uppercase tracking-[0.2em] text-coral">推荐复习卡片</div>
              <p class="mt-1 text-sm text-secondary">
                会进入 {{ detail.interviewDeckTitle || '面试诊断卡片' }}。
              </p>
            </div>
            <span
              class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold"
              :class="record.generatedCardId ? 'generated-status generated-status-ready' : 'generated-status generated-status-pending'"
            >
              {{ record.generatedCardId ? '已生成' : '待生成' }}
            </span>
          </div>
          <div class="mt-4 grid gap-3 md:grid-cols-2">
            <div class="surface-card p-4">
              <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">正面问题</div>
              <p class="mt-2 text-sm leading-6 text-primary">
                {{ record.recommendedCardFront || record.questionTitle }}
              </p>
            </div>
            <div class="surface-card p-4">
              <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">背面答案</div>
              <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-primary">
                {{ record.recommendedCardBack || '暂无标准答案' }}
              </p>
            </div>
            <div class="surface-card p-4 md:col-span-2">
              <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">解释</div>
              <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-primary">
                {{ record.recommendedCardExplanation || '暂无说明' }}
              </p>
            </div>
            <div v-if="record.recommendedCardFollowUp" class="surface-card p-4 md:col-span-2">
              <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">追问点</div>
              <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-primary">
                {{ record.recommendedCardFollowUp }}
              </p>
            </div>
          </div>
        </div>

        <div v-else-if="record.followUp" class="border-t border-slate-200/60 px-6 py-4 dark:border-slate-700/60">
          <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">追问</div>
          <p class="mt-1 text-sm leading-6 text-secondary">{{ record.followUp }}</p>
        </div>
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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { activateInterviewCardsApi, generateInterviewCardsApi, interviewDetailApi } from '@/api/interview'
import type { InterviewDetail } from '@/types/api'

const route = useRoute()
const router = useRouter()

const detail = ref<InterviewDetail | null>(null)
const loading = ref(true)
const generating = ref(false)
const activating = ref(false)
const sortedRecords = computed(() => {
  if (!detail.value?.records) return []
  return [...detail.value.records].sort((a, b) => Number(b.isLowScore) - Number(a.isLowScore))
})

const formatScore = (score: number | undefined | null): string => {
  if (score == null) return '-'
  return Number.isInteger(score) ? String(score) : Number(score).toFixed(2)
}

const formatTime = (time: string): string => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const sessionId = () => String(route.params.id || '')

const loadData = async () => {
  const id = sessionId()
  if (!id) {
    loading.value = false
    return
  }
  try {
    const response = await interviewDetailApi(id)
    detail.value = response.data
  } catch {
    ElMessage.error('加载面试详情失败')
  } finally {
    loading.value = false
  }
}

const handleGenerateCards = async () => {
  const id = sessionId()
  if (!id) return
  generating.value = true
  try {
    const response = await generateInterviewCardsApi(id)
    detail.value = response.data
    ElMessage.success('已生成面试复习卡片')
  } catch {
    ElMessage.error('生成复习卡片失败')
  } finally {
    generating.value = false
  }
}

const handleActivateCards = async () => {
  const id = sessionId()
  if (!id) return
  activating.value = true
  try {
    await activateInterviewCardsApi(id)
    ElMessage.success('已加入今日卡片')
    await router.push('/cards')
  } catch {
    ElMessage.error('加入今日卡片失败')
  } finally {
    activating.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped>
.interview-score-card {
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 86% 18%, rgba(var(--bc-cyan-rgb), 0.28), transparent 34%),
    linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.94), rgba(75, 64, 49, 0.96));
  box-shadow: 0 16px 36px rgba(var(--bc-accent-rgb), 0.16);
}

.generated-status {
  border: 1px solid var(--bc-border-subtle);
}

.generated-status-ready {
  background: rgba(74, 122, 73, 0.12);
  color: var(--bc-lime);
}

.generated-status-pending {
  background: var(--interactive-bg);
  color: var(--bc-coral);
}
</style>
