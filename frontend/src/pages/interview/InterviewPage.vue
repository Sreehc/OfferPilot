<template>
  <div class="space-y-4">
    <!-- Setup / Active Interview Header -->
    <section class="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
      <!-- Left Panel -->
      <div class="paper-panel p-6">
        <p class="section-kicker">面试设置</p>
        <h3 class="mt-4 text-3xl font-semibold tracking-[-0.03em] text-ink">
          {{ phase === 'idle' ? '每场 3-5 题，一轮追问' : `第 ${currentQuestion?.currentIndex ?? '?'} 题 / 共 ${currentQuestion?.questionCount ?? '?'} 题` }}
        </h3>

        <!-- Idle: Setup Form -->
        <div v-if="phase === 'idle'" class="mt-6 space-y-4">
          <div class="surface-card p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">面试方向</div>
            <el-select v-model="direction" size="large" class="mt-2 w-full">
              <el-option v-for="d in directions" :key="d" :label="d" :value="d" />
            </el-select>
          </div>
          <div class="surface-card p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">题目数量</div>
            <el-input-number v-model="questionCount" :min="3" :max="5" size="large" class="mt-2 w-full" />
          </div>
          <div v-if="voiceAvailable" class="surface-card p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">作答模式</div>
            <div class="mt-3 flex gap-3">
              <button
                type="button"
                class="flex-1 rounded-xl p-3 text-sm font-medium transition-all"
                :class="interviewMode === 'text' ? 'bg-accent text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-slate-700 dark:text-slate-300 dark:hover:bg-slate-600'"
                @click="interviewMode = 'text'"
              >
                打字作答
              </button>
              <button
                type="button"
                class="flex-1 rounded-xl p-3 text-sm font-medium transition-all"
                :class="interviewMode === 'voice' ? 'bg-accent text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-slate-700 dark:text-slate-300 dark:hover:bg-slate-600'"
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
            {{ interviewMode === 'voice' && voiceAvailable ? '开始语音面试' : '开始面试' }}
          </el-button>
        </div>

        <!-- In Progress: Current Question -->
        <div v-else-if="phase !== 'idle'" class="mt-6 space-y-4">
          <!-- Progress Bar -->
          <div class="surface-card p-4">
            <div class="flex items-center justify-between text-xs uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">
              <span>进度</span>
              <span>{{ currentQuestion?.currentIndex ?? 0 }} / {{ currentQuestion?.questionCount ?? 0 }}</span>
            </div>
            <div class="mt-3 h-2 rounded-full bg-slate-200/80 dark:bg-slate-700/80">
              <div
                class="h-2 rounded-full bg-accent transition-all duration-500"
                :style="{ width: `${progressPercent}%` }"
              ></div>
            </div>
          </div>

          <!-- Current Question -->
          <div class="surface-card p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">当前题目</div>
            <div class="mt-2 text-lg font-semibold text-ink leading-relaxed">
              {{ currentQuestion?.questionTitle ?? '加载中...' }}
            </div>
          </div>

          <!-- Direction Badge -->
          <div class="surface-card p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">面试方向</div>
            <div class="mt-2 font-semibold">{{ direction }}</div>
          </div>

          <el-button
            v-if="phase === 'finished'"
            size="large"
            class="hard-button-secondary w-full"
            @click="handleViewDetail"
          >
            查看面试详情
          </el-button>
        </div>
      </div>

      <!-- Right Panel -->
      <div class="paper-panel flex flex-col p-6">
        <!-- Idle State -->
        <div v-if="phase === 'idle'" class="flex flex-1 items-center justify-center">
          <div class="text-center">
            <div class="text-5xl font-semibold tracking-[-0.03em] text-slate-300 dark:text-slate-600">?</div>
            <p class="mt-4 text-sm leading-6 text-slate-500 dark:text-slate-400">
              选择面试方向并点击"开始面试"，系统会从题库中抽取题目并由 AI 进行评分。
            </p>
          </div>
        </div>

        <!-- Answering State -->
        <div v-else-if="phase === 'answering'" class="flex flex-1 flex-col">
          <div class="flex items-center justify-between">
            <p class="section-kicker">{{ interviewMode === 'voice' && voiceAvailable ? '语音作答' : '你的回答' }}</p>
            <div class="flex items-center gap-2 text-sm">
              <span class="text-slate-500 dark:text-slate-400">倒计时</span>
              <span class="font-semibold tabular-nums" :class="countdown <= 30 ? 'text-red-500' : 'text-accent'">{{ formatCountdown(countdown) }}</span>
            </div>
          </div>

          <!-- Text Mode -->
          <template v-if="interviewMode !== 'voice' || !voiceAvailable">
            <el-input
              v-model="answerText"
              type="textarea"
              :rows="10"
              placeholder="请在此输入你的回答。尽量覆盖关键点，结构化表达会获得更高分数。"
              class="mt-4 flex-1"
              size="large"
              @keydown.ctrl.enter.prevent="handleSubmitAnswer"
            />
            <div class="mt-4 flex gap-3">
              <el-button
                :loading="submitting"
                type="primary"
                size="large"
                class="action-button flex-1"
                @click="handleSubmitAnswer"
              >
                提交答案
              </el-button>
              <span class="self-center text-xs text-slate-400 dark:text-slate-500">Ctrl + Enter 快捷提交</span>
            </div>
          </template>

          <!-- Voice Mode -->
          <template v-else>
            <div class="mt-4 flex-1">
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

        <!-- Loading AI Score -->
        <div v-else-if="phase === 'scoring'" class="flex flex-1 items-center justify-center">
          <div class="text-center">
            <div class="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-accent border-t-transparent"></div>
            <p class="mt-4 text-sm text-slate-500 dark:text-slate-400">AI 正在评分中，请稍候...</p>
          </div>
        </div>

        <!-- Result State: Show score for current question -->
        <div v-else-if="phase === 'result'" class="space-y-4">
          <p class="section-kicker">评分结果</p>

          <!-- Voice Transcript (if voice mode) -->
          <div v-if="voiceTranscript" class="surface-card p-4">
            <div class="flex items-center justify-between">
              <div class="text-xs uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">语音转录</div>
              <div v-if="lastVoiceResult?.transcriptConfidence" class="text-xs text-slate-400">
                置信度 {{ Math.round(lastVoiceResult.transcriptConfidence * 100) }}%
              </div>
            </div>
            <p class="mt-2 text-sm leading-6 text-slate-700 dark:text-slate-200">{{ voiceTranscript }}</p>
          </div>

          <!-- Score Card -->
          <div
            class="p-6 text-white shadow-[0_16px_36px_rgba(47,79,157,0.18)]"
            style="border-radius: var(--radius-lg); background: linear-gradient(180deg, #365ab0 0%, #2f4f9d 100%);"
          >
            <div class="text-xs uppercase tracking-[0.24em] text-white/60">AI 评分</div>
            <div class="mt-3 text-5xl font-semibold tracking-[-0.03em]">{{ animatedScore }}</div>
            <p class="mt-4 text-sm leading-7 text-white/80">{{ lastResult?.comment }}</p>
          </div>

          <!-- Standard Answer & Follow-up -->
          <div class="grid gap-3 md:grid-cols-2">
            <div class="surface-card p-4">
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
              <p class="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">{{ lastResult?.standardAnswer || '暂无' }}</p>
            </div>
            <div class="surface-card p-4">
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
              <p class="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">{{ lastResult?.followUp || '无' }}</p>
            </div>
          </div>

          <!-- Wrong Book Notice -->
          <div v-if="lastResult?.addedToWrongBook" class="surface-muted p-4 text-sm text-slate-600 dark:text-slate-300">
            <span class="font-semibold text-ink">已自动加入错题本</span> — 该题得分低于 60 分，已沉淀到错题本供后续复习。
          </div>

          <!-- Next / Finish -->
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
            <el-button
              v-else
              type="primary"
              size="large"
              class="action-button flex-1"
              @click="handleFinish"
            >
              查看面试结果
            </el-button>
          </div>
        </div>

        <!-- Finished State: Summary -->
        <div v-else-if="phase === 'finished'" class="space-y-4">
          <p class="section-kicker">面试完成</p>

          <div
            class="p-6 text-white shadow-[0_16px_36px_rgba(47,79,157,0.18)]"
            style="border-radius: var(--radius-lg); background: linear-gradient(135deg, #365ab0 0%, #233d79 100%);"
          >
            <div class="text-xs uppercase tracking-[0.24em] text-white/60">总分</div>
            <div class="mt-3 text-6xl font-semibold tracking-[-0.03em]">{{ detail?.totalScore ?? '-' }}</div>
            <p class="mt-4 text-sm text-white/80">
              共 {{ detail?.questionCount ?? 0 }} 题，方向：{{ detail?.direction }}
              <span v-if="detail?.mode === 'voice'" class="ml-2 inline-flex items-center rounded-full bg-white/20 px-2 py-0.5 text-xs">语音面试</span>
            </p>
          </div>

          <!-- Per-question Results -->
          <div v-if="detail?.records?.length" class="space-y-3">
            <div
              v-for="(record, index) in detail.records"
              :key="record.questionId"
              class="surface-card p-4"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0 flex-1">
                  <div class="text-xs uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">Q{{ index + 1 }}</div>
                  <div class="mt-1 font-semibold text-ink">{{ record.questionTitle }}</div>
                  <p class="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">{{ record.comment || '暂无点评' }}</p>
                </div>
                <div
                  class="shrink-0 text-3xl font-semibold tracking-[-0.03em]"
                  :class="record.score >= 60 ? 'text-accent' : 'text-red-500'"
                >
                  {{ record.score ?? '-' }}
                </div>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex gap-3">
            <RouterLink to="/wrong" class="hard-button-secondary flex-1 text-center">
              查看错题本
            </RouterLink>
            <el-button type="primary" size="large" class="action-button flex-1" @click="handleNewInterview">
              再来一场
            </el-button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
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

watch(() => lastResult.value?.score, (score) => {
  if (score != null) {
    animateScore(Number(score))
  } else {
    animatedScore.value = '-'
  }
})

const progressPercent = computed(() => {
  if (!currentQuestion.value) return 0
  const { currentIndex, questionCount: total } = currentQuestion.value
  return Math.round(((currentIndex - 1) / total) * 100)
})

const handleStart = async (reanswerQuestionId?: number) => {
  starting.value = true
  try {
    const isVoice = interviewMode.value === 'voice' && voiceAvailable.value
    const payload = {
      direction: direction.value,
      questionCount: reanswerQuestionId ? 1 : questionCount.value,
      ...(reanswerQuestionId ? { reanswerQuestionId } : {})
    }

    const response = isVoice
      ? await startVoiceInterviewApi(payload)
      : await startInterviewApi(payload)

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
  } catch {
    ElMessage.error('提交答案失败，请重试')
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
  void fetchVoiceStatusApi().then(res => {
    voiceAvailable.value = res.data.available
  }).catch(() => {
    voiceAvailable.value = false
  })

  // Load recommended interview direction
  void fetchRecommendInterviewApi().then(res => {
    const rec = res.data
    if (rec && rec.direction && directions.includes(rec.direction)) {
      direction.value = rec.direction
    }
    if (rec && rec.questionCount) {
      questionCount.value = rec.questionCount
    }
  }).catch(() => {
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
