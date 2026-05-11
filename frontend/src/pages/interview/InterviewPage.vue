<template>
  <div class="space-y-4 interview-cockpit">
    <AppShellHeader>
      <template #actions>
        <div v-if="phase === 'idle'" class="interview-status">
          <span class="detail-pill">{{ direction }}</span>
          <span class="detail-pill">{{ questionCount }} 题</span>
        </div>
        <div v-else class="interview-status">
          <span class="detail-pill">{{ direction }}</span>
          <span class="detail-pill">
            {{ interviewMode === 'voice' && voiceAvailable ? '语音作答' : '文字作答' }}
          </span>
        </div>
        <el-button
          v-if="phase === 'finished'"
          size="large"
          class="hard-button-secondary !min-h-11 !px-5"
          @click="handleViewDetail"
        >
          查看详情
        </el-button>
      </template>
    </AppShellHeader>

    <section
      class="grid gap-4"
      :class="phase === 'idle' ? 'lg:grid-cols-[0.92fr_1.08fr]' : 'lg:grid-cols-[360px_minmax(0,1fr)]'"
    >
      <aside class="cockpit-panel p-4 sm:p-6">
        <div class="panel-heading">
          <p class="section-kicker">{{ phase === 'idle' ? '面试设置' : '当前状态' }}</p>
          <h3 class="panel-heading__title">
            {{
              phase === 'idle'
                ? '开始前设置'
                : phase === 'finished'
                  ? '面试已完成'
                  : phase === 'result'
                    ? '本题已评分'
                    : '继续当前流程'
            }}
          </h3>
          <p class="panel-heading__meta">
            {{
              phase === 'idle'
                ? '选择方向、题量和作答方式后即可开始。'
                : phase === 'finished'
                  ? '查看本场面试结果，或直接开始下一场。'
                  : '按结论、原理、场景和权衡来组织答案。'
            }}
          </p>
        </div>

        <div v-if="phase === 'idle'" class="mt-6 space-y-4">
          <div class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">方向</div>
            <el-select v-model="direction" size="large" class="mt-2 w-full">
              <el-option v-for="d in directions" :key="d" :label="d" :value="d" />
            </el-select>
          </div>
          <div class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">题量</div>
            <el-input-number v-model="questionCount" :min="3" :max="5" size="large" class="mt-2 w-full" />
          </div>
          <div v-if="voiceAvailable" class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">作答模式</div>
            <div class="mt-3 grid grid-cols-2 gap-3">
              <button
                type="button"
                class="min-h-11 rounded-2xl border px-3 py-2 text-sm font-semibold transition-all"
                :class="
                  interviewMode === 'text'
                    ? 'border-[var(--bc-line-hot)] bg-accent/10 text-ink'
                    : 'border-[var(--bc-line)] text-slate-600 hover:bg-white/40 dark:text-slate-300 dark:hover:bg-white/5'
                "
                @click="interviewMode = 'text'"
              >
                打字作答
              </button>
              <button
                type="button"
                class="min-h-11 rounded-2xl border px-3 py-2 text-sm font-semibold transition-all"
                :class="
                  interviewMode === 'voice'
                    ? 'border-[var(--bc-line-hot)] bg-accent/10 text-ink'
                    : 'border-[var(--bc-line)] text-slate-600 hover:bg-white/40 dark:text-slate-300 dark:hover:bg-white/5'
                "
                @click="interviewMode = 'voice'"
              >
                语音作答
              </button>
            </div>
          </div>
          <el-button
            :loading="starting"
            type="primary"
            size="large"
            class="action-button w-full"
            @click="handleStart()"
          >
            {{ interviewMode === 'voice' && voiceAvailable ? '开始语音诊断' : '开始诊断' }}
          </el-button>
        </div>

        <div v-else class="mt-6 space-y-4">
          <div class="data-slab p-4">
            <div
              class="flex items-center justify-between text-xs uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400"
            >
              <span>进度</span>
              <span>{{ currentQuestion?.currentIndex ?? 0 }} / {{ currentQuestion?.questionCount ?? 0 }}</span>
            </div>
            <div class="mt-3 h-2 overflow-hidden rounded-full bg-slate-200/80 dark:bg-white/10">
              <div
                class="h-full rounded-full bg-accent transition-all duration-500"
                :style="{ width: `${progressPercent}%` }"
              ></div>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">方向</div>
              <div class="mt-2 font-semibold text-ink">{{ direction }}</div>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">模式</div>
              <div class="mt-2 font-semibold text-ink">
                {{ interviewMode === 'voice' && voiceAvailable ? '语音' : '文字' }}
              </div>
            </div>
          </div>

        </div>
      </aside>

      <section class="cockpit-panel flex min-h-[560px] flex-col p-4 sm:p-6">
        <div v-if="phase === 'idle'" class="flex flex-1 items-center justify-center">
          <div class="max-w-md text-center">
            <h4 class="text-2xl font-semibold text-ink">设置好方向和题量后开始</h4>
            <p class="mt-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
              只保留当前题目、倒计时和结果，不在这里讲流程。
            </p>
          </div>
        </div>

        <div v-else-if="phase === 'answering'" class="flex flex-1 flex-col">
          <div class="question-stage question-stage-answering">
            <article class="question-spotlight question-spotlight-compact">
              <div class="question-spotlight__topline">
                <div class="question-spotlight__main">
                  <div class="question-spotlight__meta">
                    <span class="hard-chip">当前问题</span>
                    <span class="question-spotlight__index">
                      Q{{ currentQuestion?.currentIndex ?? 0 }} / {{ currentQuestion?.questionCount ?? 0 }}
                    </span>
                  </div>
                  <h4 class="question-spotlight__title">
                    {{ currentQuestion?.questionTitle ?? '加载中...' }}
                  </h4>
                  <p class="question-spotlight__hint">先给结论，再说明核心机制、适用场景和关键权衡。</p>
                </div>

                <div class="question-spotlight__timer">
                  <span class="question-spotlight__timer-label">
                    {{ interviewMode === 'voice' && voiceAvailable ? '语音作答' : '当前作答' }}
                  </span>
                  <span
                    class="question-spotlight__timer-value"
                    :class="countdownUrgent ? 'text-coral' : 'text-accent'"
                  >
                    {{ formatCountdown(countdown) }}
                  </span>
                </div>
              </div>

              <div class="question-spotlight__progress">
                <div
                  class="question-spotlight__progress-bar"
                  :class="countdownUrgent ? 'bg-coral' : 'bg-accent'"
                  :style="{ width: `${countdownPercent}%` }"
                ></div>
              </div>
            </article>
          </div>

          <template v-if="interviewMode !== 'voice' || !voiceAvailable">
            <div class="mt-5 flex items-center justify-between gap-3">
              <p class="section-kicker">回答区</p>
              <span class="text-xs text-slate-400 dark:text-slate-500">`Ctrl + Enter` 快速提交</span>
            </div>
            <el-input
              v-model="answerText"
              type="textarea"
              :rows="12"
              placeholder="按：结论 → 核心机制 → 场景权衡 → 风险补充 组织答案"
              class="interview-answer-input mt-5 flex-1"
              size="large"
              @keydown.ctrl.enter.prevent="handleSubmitAnswer"
            />
            <div class="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
              <el-button
                :loading="submitting"
                type="primary"
                size="large"
                class="action-button flex-1"
                @click="handleSubmitAnswer"
              >
                提交答案并评分
              </el-button>
            </div>
          </template>

          <template v-else>
            <div class="mt-5 flex-1">
              <VoiceRecorder
                :disabled="voiceSubmitting"
                @recorded="handleVoiceRecorded"
                @cleared="handleVoiceCleared"
              />
            </div>
            <div class="mt-4 flex gap-3">
              <el-button
                :loading="voiceSubmitting"
                :disabled="!voiceAudioBlob"
                type="primary"
                size="large"
                class="action-button flex-1"
                @click="handleVoiceSubmit"
              >
                提交语音答案
              </el-button>
            </div>
          </template>
        </div>

        <div v-else-if="phase === 'scoring'" class="flex flex-1 items-center justify-center">
          <div class="w-full max-w-md text-center">
            <div class="scoring-scan mx-auto flex h-40 w-40 items-center justify-center rounded-full">
              <div class="h-12 w-12 animate-spin rounded-full border-4 border-accent border-t-transparent"></div>
            </div>
            <p class="section-kicker mt-8">正在评分</p>
            <h4 class="mt-3 font-display text-4xl font-semibold leading-none text-ink">系统正在分析你的回答</h4>
            <p class="mt-4 text-sm text-slate-500 dark:text-slate-400">
              准确性、完整性、结构化和深度会被纳入本题评分。
            </p>
          </div>
        </div>

        <div v-else-if="phase === 'result'" class="space-y-4">
          <p class="section-kicker">本题结果</p>

          <div v-if="voiceTranscript" class="data-slab p-4">
            <div class="flex items-center justify-between">
              <div class="text-xs uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">语音转录</div>
              <div v-if="lastVoiceResult?.transcriptConfidence" class="font-mono text-xs text-slate-400">
                {{ Math.round(lastVoiceResult.transcriptConfidence * 100) }}%
              </div>
            </div>
            <p class="mt-2 text-sm leading-6 text-slate-700 dark:text-slate-200">{{ voiceTranscript }}</p>
          </div>

          <div class="score-card p-6" :class="(lastResult?.score ?? 0) >= 60 ? 'score-card-pass' : 'score-card-risk'">
            <div class="text-xs uppercase tracking-[0.24em] text-white/65">智能评分</div>
            <div class="mt-3 font-mono text-6xl font-semibold tracking-[-0.04em] text-white">{{ animatedScore }}</div>
            <p class="mt-4 text-sm leading-7 text-white/82">{{ lastResult?.comment }}</p>
          </div>

          <div class="grid gap-3 md:grid-cols-2">
            <div class="data-slab p-4">
              <div class="flex items-center justify-between">
                <div class="font-semibold text-ink">标准答案</div>
                <button
                  v-if="lastResult?.standardAnswer"
                  type="button"
                  class="text-xs text-accent hover:underline"
                  @click="speakText(lastResult!.standardAnswer!)"
                >
                  朗读
                </button>
              </div>
              <p class="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
                {{ lastResult?.standardAnswer || '暂无' }}
              </p>
            </div>
            <div class="data-slab p-4">
              <div class="flex items-center justify-between">
                <div class="font-semibold text-ink">追问</div>
                <button
                  v-if="lastResult?.followUp"
                  type="button"
                  class="text-xs text-accent hover:underline"
                  @click="speakText(lastResult!.followUp!)"
                >
                  朗读
                </button>
              </div>
              <p class="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
                {{ lastResult?.followUp || '无' }}
              </p>
            </div>
          </div>

          <div
            v-if="lastResult?.addedToWrongBook"
            class="rounded-2xl border border-coral/30 bg-coral/10 p-4 text-sm text-slate-600 dark:text-slate-300"
          >
            <span class="font-semibold text-ink">已加入错题本</span>：该题得分低于 60 分，后续会进入间隔复习。
          </div>

          <div class="flex gap-3">
            <el-button
              v-if="lastResult?.hasNextQuestion"
              type="primary"
              size="large"
              class="action-button flex-1"
              @click="handleNextQuestion"
            >
              下一题
            </el-button>
            <el-button v-else type="primary" size="large" class="action-button flex-1" @click="handleFinish">
              查看面试结果
            </el-button>
          </div>
        </div>

        <div v-else-if="phase === 'finished'" class="space-y-4">
          <p class="section-kicker">面试完成</p>

          <div class="score-card score-card-pass p-6">
            <div class="text-xs uppercase tracking-[0.24em] text-white/65">总分</div>
            <div class="mt-3 font-mono text-7xl font-semibold tracking-[-0.05em] text-white">
              {{ detail?.totalScore ?? '-' }}
            </div>
            <p class="mt-4 text-sm text-white/82">
              共 {{ detail?.questionCount ?? 0 }} 题，方向：{{ detail?.direction }}
              <span
                v-if="detail?.mode === 'voice'"
                class="ml-2 inline-flex items-center rounded-full bg-white/20 px-2 py-0.5 text-xs"
                >语音面试</span
              >
            </p>
          </div>

          <div v-if="detail?.records?.length" class="space-y-3">
            <div
              v-for="(record, index) in detail.records"
              :key="record.questionId"
              class="data-slab cursor-pointer p-4 transition hover:shadow-md"
              @click="toggleQuestion(record.questionId)"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0 flex-1">
                  <div class="text-xs uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">
                    Q{{ index + 1 }}
                  </div>
                  <div class="mt-1 font-semibold text-ink">{{ record.questionTitle }}</div>
                  <p class="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300 line-clamp-2">
                    {{ record.comment || '暂无点评' }}
                  </p>
                </div>
                <div class="flex shrink-0 items-center gap-2">
                  <div
                    class="font-mono text-3xl font-semibold tracking-[-0.03em]"
                    :class="record.score >= 60 ? 'text-accent' : 'text-coral'"
                  >
                    {{ record.score ?? '-' }}
                  </div>
                  <svg
                    class="h-4 w-4 text-slate-400 transition-transform"
                    :class="expandedQuestions.has(record.questionId) ? 'rotate-180' : ''"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                  </svg>
                </div>
              </div>

              <div
                v-if="expandedQuestions.has(record.questionId)"
                class="mt-4 space-y-3 border-t border-[var(--bc-line)] pt-4"
              >
                <div v-if="record.userAnswer">
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                    我的回答
                  </div>
                  <p class="mt-1 whitespace-pre-wrap text-sm leading-6 text-slate-700 dark:text-slate-200">
                    {{ record.userAnswer }}
                  </p>
                </div>
                <div v-if="record.standardAnswer">
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                    标准答案
                  </div>
                  <p class="mt-1 whitespace-pre-wrap text-sm leading-6 text-slate-700 dark:text-slate-200">
                    {{ record.standardAnswer }}
                  </p>
                </div>
                <div v-if="record.followUp">
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                    追问
                  </div>
                  <p class="mt-1 text-sm leading-6 text-slate-700 dark:text-slate-200">{{ record.followUp }}</p>
                </div>
              </div>
            </div>
          </div>

          <div class="flex gap-3">
            <RouterLink to="/wrong" class="hard-button-secondary flex-1 text-center"> 查看错题本 </RouterLink>
            <RouterLink to="/review" class="hard-button-secondary flex-1 text-center"> 去复习中心 </RouterLink>
            <el-button type="primary" size="large" class="action-button flex-1" @click="handleNewInterview">
              再来一场
            </el-button>
          </div>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppShellHeader from '@/components/AppShellHeader.vue'
import {
  currentQuestionApi,
  fetchVoiceStatusApi,
  interviewDetailApi,
  startInterviewApi,
  startVoiceInterviewApi,
  submitAnswerApi,
  submitVoiceAnswerApi
} from '@/api/interview'
import { fetchRecommendInterviewApi } from '@/api/adaptive'
import type { InterviewAnswerResult, InterviewCurrentQuestion, InterviewDetail, VoiceSubmitResult } from '@/types/api'
import VoiceRecorder from '@/components/VoiceRecorder.vue'

const route = useRoute()

type Phase = 'idle' | 'answering' | 'scoring' | 'result' | 'finished'

const directions = ['Spring', 'JVM', 'MySQL', 'Redis', '并发', '微服务']

const phase = ref<Phase>('idle')
const direction = ref('Spring')
const questionCount = ref(3)
const interviewMode = ref<'text' | 'voice'>('text')
const voiceAvailable = ref(false)
const starting = ref(false)
const submitting = ref(false)
const answerText = ref('')
const voiceAudioBlob = ref<Blob | null>(null)
const voiceTranscript = ref('')
const voiceSubmitting = ref(false)

const currentQuestion = ref<InterviewCurrentQuestion | null>(null)
const lastResult = ref<InterviewAnswerResult | null>(null)
const lastVoiceResult = ref<VoiceSubmitResult | null>(null)
const detail = ref<InterviewDetail | null>(null)
const expandedQuestions = ref<Set<string>>(new Set())

const toggleQuestion = (questionId: string) => {
  if (expandedQuestions.value.has(questionId)) {
    expandedQuestions.value.delete(questionId)
  } else {
    expandedQuestions.value.add(questionId)
  }
}

// Countdown timer (5 minutes per question)
const COUNTDOWN_SECONDS = 300
const countdown = ref(COUNTDOWN_SECONDS)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const formatCountdown = (seconds: number) => {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

const startCountdown = () => {
  stopCountdown()
  countdown.value = COUNTDOWN_SECONDS
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      stopCountdown()
      ElMessage.warning('答题时间已到，自动提交')
      void handleSubmitAnswer()
    }
  }, 1000)
}

const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

onBeforeUnmount(stopCountdown)

// Score animation
const animatedScore = ref<string>('-')
let scoreAnimFrame: ReturnType<typeof requestAnimationFrame> | null = null

const animateScore = (target: number) => {
  if (scoreAnimFrame) cancelAnimationFrame(scoreAnimFrame)
  const duration = 800
  const startTime = performance.now()
  const tick = (now: number) => {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3) // ease-out cubic
    animatedScore.value = String(Math.round(target * eased))
    if (progress < 1) {
      scoreAnimFrame = requestAnimationFrame(tick)
    }
  }
  scoreAnimFrame = requestAnimationFrame(tick)
}

watch(
  () => lastResult.value?.score,
  (score) => {
    if (score != null) {
      animateScore(Number(score))
    } else {
      animatedScore.value = '-'
    }
  }
)

const progressPercent = computed(() => {
  if (!currentQuestion.value) return 0
  const { currentIndex, questionCount: total } = currentQuestion.value
  return Math.round(((currentIndex - 1) / total) * 100)
})

const countdownPercent = computed(() => Math.max(0, Math.round((countdown.value / COUNTDOWN_SECONDS) * 100)))
const countdownUrgent = computed(() => countdown.value <= 30)

const handleStart = async (reanswerQuestionId?: number) => {
  starting.value = true
  try {
    const isVoice = interviewMode.value === 'voice' && voiceAvailable.value
    const payload = {
      direction: direction.value,
      questionCount: reanswerQuestionId ? 1 : questionCount.value,
      ...(reanswerQuestionId ? { reanswerQuestionId } : {})
    }

    const response = isVoice ? await startVoiceInterviewApi(payload) : await startInterviewApi(payload)

    currentQuestion.value = response.data
    answerText.value = ''
    voiceAudioBlob.value = null
    voiceTranscript.value = ''
    lastResult.value = null
    lastVoiceResult.value = null
    detail.value = null
    phase.value = 'answering'
    startCountdown()
    ElMessage.success(isVoice ? '语音面试已开始，请点击录音按钮作答' : '面试已开始，请回答第一题')
  } catch {
    ElMessage.error('启动面试失败，请确认题库中有对应方向的题目')
  } finally {
    starting.value = false
  }
}

const handleSubmitAnswer = async () => {
  if (!answerText.value.trim()) {
    ElMessage.warning('请输入你的答案')
    return
  }
  if (!currentQuestion.value) return

  phase.value = 'scoring'
  stopCountdown()
  submitting.value = true
  try {
    const response = await submitAnswerApi({
      sessionId: currentQuestion.value.sessionId,
      questionId: currentQuestion.value.questionId,
      answer: answerText.value.trim()
    })
    lastResult.value = response.data
    phase.value = 'result'
  } catch (error: any) {
    ElMessage.error(error?.message || '提交答案失败，请重试')
    phase.value = 'answering'
  } finally {
    submitting.value = false
  }
}

const handleVoiceSubmit = async () => {
  if (!voiceAudioBlob.value || !currentQuestion.value) return

  phase.value = 'scoring'
  stopCountdown()
  voiceSubmitting.value = true
  try {
    const response = await submitVoiceAnswerApi(
      currentQuestion.value.sessionId,
      currentQuestion.value.questionId,
      voiceAudioBlob.value
    )
    lastVoiceResult.value = response.data
    voiceTranscript.value = response.data.transcript
    // Also populate lastResult for the result display
    lastResult.value = {
      score: response.data.score,
      comment: response.data.comment,
      standardAnswer: response.data.standardAnswer,
      followUp: response.data.followUp,
      addedToWrongBook: response.data.addedToWrongBook,
      hasNextQuestion: response.data.hasNextQuestion
    }
    phase.value = 'result'
  } catch {
    ElMessage.error('语音提交失败，请重试')
    phase.value = 'answering'
  } finally {
    voiceSubmitting.value = false
  }
}

const handleVoiceRecorded = (blob: Blob) => {
  voiceAudioBlob.value = blob
}

const handleVoiceCleared = () => {
  voiceAudioBlob.value = null
}

const speakText = (text: string) => {
  if (!text || !('speechSynthesis' in window)) return
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'zh-CN'
  utterance.rate = 1.0
  window.speechSynthesis.speak(utterance)
}

const handleNextQuestion = async () => {
  if (!currentQuestion.value) return

  try {
    const response = await currentQuestionApi(currentQuestion.value.sessionId)
    currentQuestion.value = response.data
    answerText.value = ''
    voiceAudioBlob.value = null
    voiceTranscript.value = ''
    lastResult.value = null
    lastVoiceResult.value = null
    phase.value = 'answering'
    startCountdown()
  } catch {
    ElMessage.error('获取下一题失败')
  }
}

const handleFinish = async () => {
  if (!currentQuestion.value) return

  try {
    const response = await interviewDetailApi(currentQuestion.value.sessionId)
    detail.value = response.data
    phase.value = 'finished'
  } catch {
    ElMessage.error('获取面试详情失败')
  }
}

const handleViewDetail = async () => {
  if (!currentQuestion.value) return
  try {
    const response = await interviewDetailApi(currentQuestion.value.sessionId)
    detail.value = response.data
  } catch {
    ElMessage.error('获取面试详情失败')
  }
}

const handleNewInterview = () => {
  phase.value = 'idle'
  currentQuestion.value = null
  lastResult.value = null
  lastVoiceResult.value = null
  detail.value = null
  answerText.value = ''
  voiceAudioBlob.value = null
  voiceTranscript.value = ''
}

onMounted(() => {
  // Check voice availability
  void fetchVoiceStatusApi()
    .then((res) => {
      voiceAvailable.value = res.data.available
    })
    .catch(() => {
      voiceAvailable.value = false
    })

  // Load recommended interview direction
  void fetchRecommendInterviewApi()
    .then((res) => {
      const rec = res.data
      if (rec && rec.direction && directions.includes(rec.direction)) {
        direction.value = rec.direction
      }
      if (rec && rec.questionCount) {
        questionCount.value = rec.questionCount
      }
    })
    .catch(() => {
      // Silently fail — use defaults
    })

  // Auto-start if reanswer query param is present (from wrong book)
  const reanswerId = route.query.reanswer
  if (reanswerId) {
    questionCount.value = 1
    void handleStart(Number(reanswerId))
  }
})
</script>

<style scoped>
.interview-cockpit :deep(.el-textarea__inner) {
  min-height: 320px !important;
  font-size: 15px;
  line-height: 1.75;
}

.module-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.module-topbar__title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}

.module-topbar__title-group {
  min-width: 0;
}

.module-topbar__title-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
  flex-wrap: wrap;
}

.module-topbar__heading {
  color: var(--bc-ink);
  font-size: 1.2rem;
  font-weight: 700;
  line-height: 1.2;
}

.module-topbar__summary {
  margin-top: 0.25rem;
  color: var(--bc-ink-secondary);
  font-size: 12px;
}

.module-topbar__center {
  display: flex;
  justify-content: center;
  min-width: 0;
  flex: 1;
}

.module-topbar__action {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.interview-status {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.panel-heading__title {
  margin-top: 12px;
  color: var(--bc-ink);
  font-size: 1.55rem;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.03em;
}

.panel-heading__meta {
  margin-top: 12px;
  color: var(--bc-ink-secondary);
  font-size: 14px;
  line-height: 1.75;
}

.question-stage {
  display: grid;
  gap: 16px;
}

.question-stage-answering {
  gap: 0;
}

.question-spotlight {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.2);
  border-radius: 28px;
  padding: 24px 24px 22px;
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.18), transparent 34%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.62));
  box-shadow:
    0 24px 50px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.dark .question-spotlight {
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.22), transparent 34%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.03));
  box-shadow:
    0 24px 50px rgba(0, 0, 0, 0.26),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.question-spotlight::after {
  content: '';
  position: absolute;
  right: -42px;
  top: -42px;
  width: 148px;
  height: 148px;
  border-radius: 999px;
  border: 1px dashed rgba(var(--bc-accent-rgb), 0.26);
  opacity: 0.8;
}

.question-spotlight__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.question-spotlight__index {
  font-family: theme('fontFamily.mono');
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--bc-ink-secondary);
}

.question-spotlight__title {
  margin-top: 18px;
  max-width: 820px;
  font-family: theme('fontFamily.display');
  font-size: clamp(1.8rem, 3vw, 2.8rem);
  font-weight: 600;
  line-height: 1.08;
  letter-spacing: -0.05em;
  color: var(--bc-ink);
  text-wrap: balance;
}

.question-spotlight__hint {
  margin-top: 18px;
  max-width: 760px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--bc-ink-secondary);
}

.question-spotlight-compact {
  padding: 22px 24px 18px;
}

.question-spotlight__topline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.question-spotlight__main {
  min-width: 0;
  flex: 1;
}

.question-spotlight__timer {
  display: flex;
  min-width: 120px;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  text-align: right;
}

.question-spotlight__timer-label {
  color: var(--bc-ink-secondary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.question-spotlight__timer-value {
  font-family: theme('fontFamily.mono');
  font-size: clamp(2rem, 3vw, 3rem);
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.05em;
}

.question-spotlight__progress {
  overflow: hidden;
  margin-top: 18px;
  height: 8px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.78);
}

.question-spotlight__progress-bar {
  height: 100%;
  border-radius: inherit;
  transition: width 500ms ease;
}

.interview-orbit,
.scoring-scan {
  position: relative;
  border: 1px solid var(--bc-line-hot);
  background:
    radial-gradient(circle, rgba(var(--bc-accent-rgb), 0.18), transparent 58%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.08), transparent);
  box-shadow:
    0 0 0 12px rgba(var(--bc-accent-rgb), 0.04),
    0 0 60px rgba(var(--bc-accent-rgb), 0.18);
}

.interview-orbit::before,
.scoring-scan::before {
  content: '';
  position: absolute;
  inset: 20px;
  border: 1px dashed rgba(var(--bc-accent-rgb), 0.34);
  border-radius: inherit;
}

.interview-orbit::after,
.scoring-scan::after {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  background: conic-gradient(from 120deg, transparent, rgba(var(--bc-accent-rgb), 0.45), transparent 34%);
  opacity: 0.5;
  mask: radial-gradient(circle, transparent 63%, black 64%);
  animation: orbit-spin 8s linear infinite;
}

.score-card {
  overflow: hidden;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 26px 56px rgba(0, 0, 0, 0.22);
}

.score-card-pass {
  background:
    radial-gradient(circle at 88% 14%, rgba(85, 214, 190, 0.34), transparent 32%),
    linear-gradient(135deg, #123b48 0%, #10243a 55%, #07111f 100%);
}

.score-card-risk {
  background:
    radial-gradient(circle at 88% 14%, rgba(255, 107, 107, 0.34), transparent 32%),
    linear-gradient(135deg, #4a1d23 0%, #10243a 60%, #07111f 100%);
}

@keyframes orbit-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .interview-orbit::after,
  .scoring-scan::after {
    animation: none;
  }
}

@media (max-width: 768px) {
  .module-topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .module-topbar__title-row {
    align-items: flex-start;
  }

  .module-topbar__center,
  .module-topbar__action {
    justify-content: flex-start;
  }

  .question-spotlight {
    padding: 20px 18px 18px;
    border-radius: 24px;
  }

  .question-spotlight__topline {
    flex-direction: column;
    gap: 18px;
  }

  .question-spotlight__timer {
    min-width: 0;
    align-items: flex-start;
    text-align: left;
  }

  .question-spotlight__title {
    font-size: 1.9rem;
  }
}
</style>
