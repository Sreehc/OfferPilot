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

    <!-- Idle state: centered single-column layout -->
    <div v-if="phase === 'idle'" class="mx-auto max-w-2xl space-y-4">
      <aside class="shell-section-card p-4 sm:p-6">
        <div class="panel-heading">
          <h3 class="panel-heading__title">开始前设置</h3>
        </div>

        <div class="mt-6 space-y-4">
          <div class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-tertiary">方向</div>
            <el-select v-model="direction" size="large" class="mt-2 w-full">
              <el-option v-for="d in directions" :key="d.name" :label="d.name" :value="d.name">
                <span>{{ d.name }}</span>
                <span class="ml-2 text-xs text-tertiary">{{ d.desc }}</span>
              </el-option>
            </el-select>
          </div>
          <div class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-tertiary">目标岗位</div>
            <el-input
              v-model="jobRole"
              size="large"
              class="mt-2"
              placeholder="例如：Java 后端开发 / 实习生 / 初级开发"
            />
          </div>
          <div class="grid gap-4 md:grid-cols-2">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.24em] text-tertiary">经验阶段</div>
              <el-select v-model="experienceLevel" size="large" class="mt-2 w-full">
                <el-option
                  v-for="item in experienceLevels"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.24em] text-tertiary">面试时长</div>
              <el-select v-model="durationMinutes" size="large" class="mt-2 w-full">
                <el-option
                  v-for="minutes in durationOptions"
                  :key="minutes"
                  :label="`${minutes} 分钟`"
                  :value="minutes"
                />
              </el-select>
            </div>
          </div>
          <div class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-tertiary">技术范围</div>
            <el-input
              v-model="techStack"
              size="large"
              class="mt-2"
              placeholder="例如：Spring Boot, MySQL, Redis, MQ"
            />
            <p class="mt-2 text-xs leading-5 text-tertiary">
              支持逗号分隔，系统会优先抽取与你当前技术栈更贴近的问题。
            </p>
          </div>
          <div class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-tertiary">本轮上下文</div>
            <div class="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-3">
              <button
                type="button"
                class="interview-context-chip"
                :class="{ 'interview-context-chip-active': interviewContextPath === 'general' }"
                @click="applyInterviewContextPath('general')"
              >
                通用模拟
              </button>
              <button
                type="button"
                class="interview-context-chip"
                :class="{ 'interview-context-chip-active': interviewContextPath === 'resume' }"
                @click="applyInterviewContextPath('resume')"
              >
                基于简历
              </button>
              <button
                type="button"
                class="interview-context-chip"
                :class="{ 'interview-context-chip-active': interviewContextPath === 'project' }"
                @click="applyInterviewContextPath('project')"
              >
                基于项目
              </button>
            </div>
            <div v-if="interviewContextPath !== 'general'" class="mt-4 grid gap-3 sm:grid-cols-2">
              <el-select
                v-model="selectedResumeId"
                clearable
                size="large"
                placeholder="先选一份简历"
                :loading="loadingResumes"
              >
                <el-option
                  v-for="resume in resumes"
                  :key="resume.id"
                  :label="resume.title"
                  :value="resume.id"
                />
              </el-select>
              <el-select
                v-if="interviewContextPath === 'project'"
                v-model="selectedProjectId"
                clearable
                size="large"
                placeholder="再锁定一个项目"
                :disabled="!selectedResumeId || !resumeProjects.length"
              >
                <el-option
                  v-for="project in resumeProjects"
                  :key="project.id"
                  :label="project.projectName"
                  :value="project.id"
                />
              </el-select>
            </div>
            <p class="mt-3 text-xs leading-5 text-tertiary">
              {{ draftContextSource?.summary }}
            </p>
          </div>
          <div class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-tertiary">题量</div>
            <el-input-number v-model="questionCount" :min="3" :max="5" size="large" class="mt-2 w-full" />
          </div>
          <div v-if="voiceAvailable" class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.24em] text-tertiary">作答模式</div>
            <div class="mt-3 grid grid-cols-2 gap-3">
              <button
                type="button"
                class="min-h-11 rounded-2xl border px-3 py-2 text-sm font-semibold transition-all"
                :class="
                  interviewMode === 'text'
                    ? 'border-[var(--bc-line-hot)] bg-accent/10 text-ink'
                    : 'border-[var(--bc-line)] text-secondary hover:bg-[var(--interactive-hover)]'
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
                    : 'border-[var(--bc-line)] text-secondary hover:bg-[var(--interactive-hover)]'
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
      </aside>

      <!-- Recent interview history -->
      <section class="shell-section-card p-4 sm:p-6">
        <div class="flex items-center justify-between">
          <h3 class="interview-history__heading">最近面试</h3>
          <RouterLink
            v-if="recentInterviews.length > 0"
            to="/interview/history"
            class="text-sm font-medium text-accent transition hover:opacity-80"
          >
            查看全部
          </RouterLink>
        </div>

        <div v-if="loadingHistory" class="py-8 text-center text-sm text-tertiary">加载中...</div>

        <div v-else-if="recentInterviews.length === 0" class="interview-history-empty py-6 text-center">
          <div class="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-[var(--panel-muted)]">
            <svg class="h-6 w-6 text-tertiary" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456z" />
            </svg>
          </div>
          <p class="text-sm leading-6 text-secondary">
            还没有面试记录。AI 面试官会针对你的知识薄弱点出题，回答后即时评分并给出标准答案和追问。
          </p>
        </div>

        <div v-else class="mt-4 space-y-2">
          <RouterLink
            v-for="item in recentInterviews"
            :key="item.sessionId"
            :to="`/interview/detail/${item.sessionId}`"
            class="interview-history-item data-slab flex items-center gap-4 p-4 transition hover:shadow-sm"
          >
            <div
              class="interview-history-item__score flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl font-mono text-lg font-bold"
              :class="scoreClass(item.totalScore)"
            >
              {{ Math.round(item.totalScore) }}
            </div>
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2">
                <span class="font-semibold text-ink">{{ item.direction }}</span>
                <span class="text-xs text-tertiary">{{ item.questionCount }} 题</span>
                <span v-if="item.mode === 'voice'" class="rounded-full bg-accent/10 px-2 py-0.5 text-xs font-medium text-accent">
                  语音
                </span>
              </div>
              <div class="mt-1 text-xs text-tertiary">
                {{ formatRelativeTime(item.endTime || item.startTime) }}
              </div>
            </div>
            <svg class="h-4 w-4 shrink-0 text-tertiary" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
            </svg>
          </RouterLink>
        </div>
      </section>
    </div>

    <!-- Active states: two-column layout -->
    <section v-if="phase !== 'idle'" class="grid gap-4 lg:grid-cols-[360px_minmax(0,1fr)]">
      <aside class="shell-section-card p-4 sm:p-6">
        <div class="panel-heading">
          <h3 class="panel-heading__title">
            {{
              phase === 'finished'
                ? '面试已完成'
                : phase === 'result'
                  ? '本题已评分'
                  : '继续当前流程'
            }}
          </h3>
        </div>

        <div class="mt-6 space-y-4">
          <div class="data-slab p-4">
            <div
              class="flex items-center justify-between text-xs uppercase tracking-[0.22em] text-tertiary"
            >
              <span>进度</span>
              <span>{{ currentQuestion?.currentIndex ?? 0 }} / {{ currentQuestion?.questionCount ?? 0 }}</span>
            </div>
            <div class="mt-3 h-2 overflow-hidden rounded-full bg-[var(--panel-muted)]">
              <div
                class="h-full rounded-full bg-accent transition-all duration-500"
                :style="{ width: `${progressPercent}%` }"
              ></div>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">方向</div>
              <div class="mt-2 font-semibold text-ink">{{ sessionDirection }}</div>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">模式</div>
              <div class="mt-2 font-semibold text-ink">
                {{ interviewMode === 'voice' && voiceAvailable ? '语音' : '文字' }}
              </div>
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">目标岗位</div>
              <div class="mt-2 font-semibold text-ink">{{ sessionJobRole || '未设置' }}</div>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">经验阶段</div>
              <div class="mt-2 font-semibold text-ink">{{ experienceLabel(sessionExperienceLevel) }}</div>
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">技术范围</div>
              <div class="mt-2 text-sm font-semibold leading-6 text-ink">
                {{ sessionTechStack || '通用方向题' }}
              </div>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">面试配置</div>
              <div class="mt-2 space-y-1 text-sm font-semibold text-ink">
                <div>{{ sessionDurationMinutes }} 分钟 / {{ currentQuestion?.questionCount ?? questionCount }} 题</div>
                <div>{{ sessionIncludeResumeProject ? '结合项目追问' : '纯知识与表达训练' }}</div>
              </div>
            </div>
          </div>
          <div v-if="activeContextSummary" class="data-slab p-4">
            <div class="text-xs uppercase tracking-[0.22em] text-tertiary">{{ activeContextSource?.label || '上下文' }}</div>
            <p class="mt-2 text-sm leading-6 text-secondary">{{ activeContextSummary }}</p>
          </div>

        </div>
      </aside>

      <section class="shell-section-card flex min-h-[560px] flex-col p-4 sm:p-6">
        <div v-if="false"></div>

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
                    <span v-if="currentQuestion?.contextSource?.label" class="detail-pill">
                      {{ currentQuestion.contextSource.label }}
                    </span>
                  </div>
                  <h4 class="question-spotlight__title">
                    {{ currentQuestion?.questionTitle ?? '加载中...' }}
                  </h4>
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
              <span class="text-xs text-tertiary">`Ctrl + Enter` 快速提交</span>
            </div>
            <el-input
              v-model="answerText"
              type="textarea"
              :rows="12"
              placeholder="先写结论，再补关键原因和权衡"
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
            <h4 class="mt-8 font-display text-4xl font-semibold leading-none text-ink">正在评分</h4>
          </div>
        </div>

        <div v-else-if="phase === 'result'" class="space-y-4">
          <div v-if="voiceTranscript" class="data-slab p-4">
            <div class="flex items-center justify-between">
              <div class="text-xs uppercase tracking-[0.24em] text-tertiary">语音转录</div>
              <div v-if="lastVoiceResult?.transcriptConfidence" class="font-mono text-xs text-tertiary">
                {{ Math.round(lastVoiceResult.transcriptConfidence * 100) }}%
              </div>
            </div>
            <p class="mt-2 text-sm leading-6 text-primary">{{ voiceTranscript }}</p>
          </div>

          <div class="score-card p-6" :class="(lastResult?.score ?? 0) >= 60 ? 'score-card-pass' : 'score-card-risk'">
            <div class="text-xs uppercase tracking-[0.24em] text-white/65">智能评分</div>
            <div class="mt-3 font-mono text-6xl font-semibold tracking-[-0.04em] text-white">{{ animatedScore }}</div>
            <p class="mt-4 text-sm leading-7 text-white/82">{{ lastResult?.comment }}</p>
          </div>

          <div v-if="lastResult?.scoreBreakdown?.length" class="grid gap-3 md:grid-cols-3">
            <div
              v-for="item in lastResult.scoreBreakdown"
              :key="`${item.dimension}-${item.score}`"
              class="data-slab p-4"
            >
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">{{ item.dimension }}</div>
              <div class="mt-2 font-mono text-3xl font-semibold tracking-[-0.03em] text-ink">
                {{ item.score }}
              </div>
              <p class="mt-2 text-sm leading-6 text-secondary">{{ item.summary }}</p>
            </div>
          </div>

          <div v-if="lastResult?.weakPointTags?.length || lastResult?.reviewSummary" class="grid gap-3 md:grid-cols-2">
            <div v-if="lastResult?.weakPointTags?.length" class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.24em] text-tertiary">薄弱点标签</div>
              <div class="mt-3 flex flex-wrap gap-2">
                <span
                  v-for="tag in lastResult.weakPointTags"
                  :key="tag"
                  class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral"
                >
                  {{ tag }}
                </span>
              </div>
            </div>
            <div v-if="lastResult?.reviewSummary" class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.24em] text-tertiary">复盘建议</div>
              <p class="mt-2 text-sm leading-6 text-secondary">{{ lastResult.reviewSummary }}</p>
            </div>
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
              <p class="mt-2 text-sm leading-6 text-secondary">
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
              <p class="mt-2 text-sm leading-6 text-secondary">
                {{ lastResult?.followUp || '无' }}
              </p>
            </div>
          </div>

          <div
            v-if="lastResult?.addedToWrongBook"
            class="rounded-2xl border border-coral/30 bg-coral/10 p-4 text-sm text-secondary"
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

          <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">目标岗位</div>
              <div class="mt-2 text-sm font-semibold leading-6 text-ink">{{ detail?.jobRole || '未设置' }}</div>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">经验阶段</div>
              <div class="mt-2 text-sm font-semibold leading-6 text-ink">
                {{ experienceLabel(detail?.experienceLevel) }}
              </div>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">技术范围</div>
              <div class="mt-2 text-sm font-semibold leading-6 text-ink">{{ detail?.techStack || '通用方向题' }}</div>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">面试配置</div>
              <div class="mt-2 space-y-1 text-sm font-semibold text-ink">
                <div>{{ detail?.durationMinutes || durationMinutes }} 分钟</div>
                <div>{{ detail?.includeResumeProject ? '结合项目复盘' : '通用问答训练' }}</div>
              </div>
            </div>
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
                  <div class="text-xs uppercase tracking-[0.22em] text-tertiary">
                    Q{{ index + 1 }}
                  </div>
                  <div class="mt-1 font-semibold text-ink">{{ record.questionTitle }}</div>
                  <p class="mt-2 text-sm leading-6 text-secondary line-clamp-2">
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
                    class="h-4 w-4 text-tertiary transition-transform"
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
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">
                    我的回答
                  </div>
                  <p class="mt-1 whitespace-pre-wrap text-sm leading-6 text-primary">
                    {{ record.userAnswer }}
                  </p>
                </div>
                <div v-if="record.standardAnswer">
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">
                    标准答案
                  </div>
                  <p class="mt-1 whitespace-pre-wrap text-sm leading-6 text-primary">
                    {{ record.standardAnswer }}
                  </p>
                </div>
                <div v-if="record.followUp">
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">
                    追问
                  </div>
                  <p class="mt-1 text-sm leading-6 text-primary">{{ record.followUp }}</p>
                </div>
                <div v-if="record.scoreBreakdown?.length" class="grid gap-3 md:grid-cols-3">
                  <div
                    v-for="item in record.scoreBreakdown"
                    :key="`${record.questionId}-${item.dimension}`"
                    class="rounded-2xl bg-[var(--panel-muted)] p-3"
                  >
                    <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">
                      {{ item.dimension }}
                    </div>
                    <div class="mt-2 font-mono text-2xl font-semibold tracking-[-0.03em] text-ink">
                      {{ item.score }}
                    </div>
                    <p class="mt-2 text-sm leading-6 text-secondary">{{ item.summary }}</p>
                  </div>
                </div>
                <div v-if="record.weakPointTags?.length">
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">
                    薄弱点标签
                  </div>
                  <div class="mt-2 flex flex-wrap gap-2">
                    <span
                      v-for="tag in record.weakPointTags"
                      :key="`${record.questionId}-${tag}`"
                      class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral"
                    >
                      {{ tag }}
                    </span>
                  </div>
                </div>
                <div v-if="record.reviewSummary">
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">
                    复盘摘要
                  </div>
                  <p class="mt-1 whitespace-pre-wrap text-sm leading-6 text-primary">
                    {{ record.reviewSummary }}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div class="flex gap-3">
            <RouterLink to="/wrong" class="hard-button-secondary flex-1 text-center"> 查看错题本 </RouterLink>
            <RouterLink to="/review" class="hard-button-secondary flex-1 text-center"> 去复习 </RouterLink>
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
  fetchInterviewHistoryApi,
  fetchVoiceStatusApi,
  interviewDetailApi,
  startInterviewApi,
  startVoiceInterviewApi,
  submitAnswerApi,
  submitVoiceAnswerApi
} from '@/api/interview'
import { fetchRecommendInterviewApi } from '@/api/adaptive'
import { fetchResumeDetailApi, fetchResumeListApi } from '@/api/resume'
import type {
  ContextSource,
  InterviewAnswerResult,
  InterviewCurrentQuestion,
  InterviewDetail,
  InterviewHistoryItem,
  ResumeProjectItem,
  ResumeSummaryItem,
  VoiceSubmitResult
} from '@/types/api'
import VoiceRecorder from '@/components/VoiceRecorder.vue'

const route = useRoute()

type Phase = 'idle' | 'answering' | 'scoring' | 'result' | 'finished'

const directions = [
  { name: 'Spring', desc: 'IoC、AOP、Boot 自动配置' },
  { name: 'JVM', desc: '内存模型、GC、类加载' },
  { name: 'MySQL', desc: '索引、事务、锁机制' },
  { name: 'Redis', desc: '数据结构、缓存、持久化' },
  { name: '并发', desc: '线程池、锁、CAS、AQS' },
  { name: '微服务', desc: '网关、注册中心、限流' }
]
const experienceLevels = [
  { label: '在校 / 实习准备', value: 'intern' },
  { label: '0-1 年', value: 'junior' },
  { label: '1-3 年', value: 'mid' },
  { label: '3 年以上', value: 'senior' }
]
const durationOptions = [10, 15, 20, 30, 45]

const phase = ref<Phase>('idle')
const direction = ref('Spring')
const jobRole = ref('Java 后端开发')
const experienceLevel = ref('junior')
const techStack = ref('Spring Boot, MySQL, Redis')
const durationMinutes = ref(20)
const questionCount = ref(3)
const interviewContextPath = ref<'general' | 'resume' | 'project'>('general')
const resumes = ref<ResumeSummaryItem[]>([])
const loadingResumes = ref(false)
const selectedResumeId = ref('')
const selectedProjectId = ref('')
const resumeProjects = ref<ResumeProjectItem[]>([])
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
const recentInterviews = ref<InterviewHistoryItem[]>([])
const loadingHistory = ref(false)
const seededQuestionTitle = ref('')
const seededQuestionMeta = ref('')

const draftContextSource = computed<ContextSource | null>(() => {
  if (interviewContextPath.value === 'project') {
    const resume = resumes.value.find((item) => item.id === selectedResumeId.value)
    const project = resumeProjects.value.find((item) => item.id === selectedProjectId.value)
    return {
      type: project ? 'project' : 'resume',
      label: project ? '项目上下文' : '简历上下文',
      resumeId: selectedResumeId.value || undefined,
      resumeTitle: resume?.title,
      projectId: project?.id,
      projectName: project?.projectName,
      summary: project
        ? `本轮会优先围绕项目「${project.projectName}」出题和追问。`
        : resume
          ? `本轮会优先结合简历《${resume.title}》里的经历和项目出题。`
          : '先选一份简历，再决定要不要锁定某个项目。'
    }
  }
  if (interviewContextPath.value === 'resume') {
    const resume = resumes.value.find((item) => item.id === selectedResumeId.value)
    return {
      type: 'resume',
      label: '简历上下文',
      resumeId: selectedResumeId.value || undefined,
      resumeTitle: resume?.title,
      summary: resume ? `本轮会优先结合简历《${resume.title}》里的经历和项目出题。` : '先选一份简历，再开始这轮面试。'
    }
  }
  return {
    type: 'general',
    label: '通用模拟',
    summary: seededQuestionTitle.value
      ? `本轮会优先围绕题目「${seededQuestionTitle.value}」练表达${seededQuestionMeta.value ? `，重点参考${seededQuestionMeta.value}` : ''}。`
      : '本轮以通用方向题为主，不绑定特定简历或项目。'
  }
})

const activeContextSource = computed(() => currentQuestion.value?.contextSource || detail.value?.contextSource || draftContextSource.value)
const activeContextSummary = computed(() => activeContextSource.value?.summary || '')
const draftIncludeResumeProject = computed(() => interviewContextPath.value !== 'general')

const toggleQuestion = (questionId: string) => {
  if (expandedQuestions.value.has(questionId)) {
    expandedQuestions.value.delete(questionId)
  } else {
    expandedQuestions.value.add(questionId)
  }
}

const sessionDirection = computed(() => currentQuestion.value?.direction || detail.value?.direction || direction.value)
const sessionJobRole = computed(() => currentQuestion.value?.jobRole || detail.value?.jobRole || jobRole.value)
const sessionExperienceLevel = computed(
  () => currentQuestion.value?.experienceLevel || detail.value?.experienceLevel || experienceLevel.value
)
const sessionTechStack = computed(() => currentQuestion.value?.techStack || detail.value?.techStack || techStack.value)
const sessionDurationMinutes = computed(
  () => currentQuestion.value?.durationMinutes || detail.value?.durationMinutes || durationMinutes.value
)
const sessionIncludeResumeProject = computed(
  () =>
    currentQuestion.value?.includeResumeProject ??
    detail.value?.includeResumeProject ??
    draftIncludeResumeProject.value
)

const experienceLabel = (value?: string) => {
  return experienceLevels.find((item) => item.value === value)?.label || '未设置'
}

const getCountdownSeconds = () => {
  const totalMinutes = currentQuestion.value?.durationMinutes || durationMinutes.value || 20
  const totalQuestions = currentQuestion.value?.questionCount || questionCount.value || 3
  return Math.max(90, Math.round((totalMinutes * 60) / totalQuestions))
}

const countdown = ref(getCountdownSeconds())
let countdownTimer: ReturnType<typeof setInterval> | null = null

const formatCountdown = (seconds: number) => {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

const startCountdown = () => {
  stopCountdown()
  countdown.value = getCountdownSeconds()
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

const countdownPercent = computed(() => {
  const total = getCountdownSeconds()
  return Math.max(0, Math.round((countdown.value / total) * 100))
})
const countdownUrgent = computed(() => countdown.value <= 30)

const handleStart = async (reanswerQuestionId?: number) => {
  if (interviewContextPath.value !== 'general' && !selectedResumeId.value) {
    ElMessage.warning('先选一份简历，再开始这轮面试')
    return
  }
  if (interviewContextPath.value === 'project' && !selectedProjectId.value) {
    ElMessage.warning('先锁定一个项目，再开始这轮面试')
    return
  }
  starting.value = true
  try {
    const isVoice = interviewMode.value === 'voice' && voiceAvailable.value
    const payload = {
      direction: direction.value,
      jobRole: jobRole.value.trim() || undefined,
      experienceLevel: experienceLevel.value,
      techStack: techStack.value.trim() || undefined,
      resumeId: selectedResumeId.value || undefined,
      projectId: selectedProjectId.value || undefined,
      durationMinutes: durationMinutes.value,
      includeResumeProject: interviewContextPath.value !== 'general',
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
      scoreBreakdown: response.data.scoreBreakdown,
      weakPointTags: response.data.weakPointTags,
      reviewSummary: response.data.reviewSummary,
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

const scoreClass = (score: number) => (score >= 80 ? 'text-accent' : score >= 60 ? 'text-amber-500' : 'text-coral')

const formatRelativeTime = (dateStr?: string) => {
  if (!dateStr) return ''
  const now = Date.now()
  const then = new Date(dateStr).getTime()
  const diffMs = now - then
  if (diffMs < 0) return '刚刚'
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour} 小时前`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 30) return `${diffDay} 天前`
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const loadRecentInterviews = async () => {
  loadingHistory.value = true
  try {
    const res = await fetchInterviewHistoryApi(undefined, 1, 5)
    recentInterviews.value = res.data.records
  } catch {
    // silent fail
  } finally {
    loadingHistory.value = false
  }
}

const loadResumes = async () => {
  loadingResumes.value = true
  try {
    const response = await fetchResumeListApi()
    resumes.value = response.data
  } catch {
    resumes.value = []
  } finally {
    loadingResumes.value = false
  }
}

const loadResumeProjects = async (resumeId: string) => {
  try {
    const response = await fetchResumeDetailApi(resumeId)
    resumeProjects.value = response.data.projects || []
  } catch {
    resumeProjects.value = []
  }
}

const applyInterviewContextPath = (path: 'general' | 'resume' | 'project') => {
  interviewContextPath.value = path
  if (path === 'general') {
    selectedResumeId.value = ''
    selectedProjectId.value = ''
    return
  }
  if (!selectedResumeId.value && resumes.value[0]) {
    selectedResumeId.value = resumes.value[0].id
  }
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

const applyQuestionSeedFromRoute = () => {
  const title = String(route.query.sourceQuestionTitle || '').trim()
  if (!title) return
  const category = String(route.query.sourceQuestionCategory || '').trim()
  const tag = String(route.query.sourceQuestionTag || '').trim()
  const directionQuery = String(route.query.sourceQuestionDirection || '').trim()
  seededQuestionTitle.value = title
  seededQuestionMeta.value = [category, directionQuery, tag].filter(Boolean).join(' / ')
  if (directionQuery && directions.some((item) => item.name === directionQuery)) {
    direction.value = directionQuery
  }
  interviewContextPath.value = 'general'
}

onMounted(() => {
  // Load recent interviews for idle state
  void loadRecentInterviews()
  void loadResumes()
  applyQuestionSeedFromRoute()

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
      if (rec && rec.direction && directions.some((d) => d.name === rec.direction)) {
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

watch(selectedResumeId, async (resumeId) => {
  selectedProjectId.value = ''
  if (!resumeId) {
    resumeProjects.value = []
    return
  }
  await loadResumeProjects(resumeId)
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

.interview-context-chip {
  min-height: 2.75rem;
  border-radius: 1rem;
  border: 1px solid var(--bc-line);
  background: var(--panel-muted);
  color: var(--bc-ink-secondary);
  font-size: 0.9rem;
  font-weight: 600;
  transition:
    border-color var(--motion-fast) var(--ease-hard),
    background var(--motion-fast) var(--ease-hard),
    color var(--motion-fast) var(--ease-hard);
}

.interview-context-chip-active {
  border-color: rgba(var(--bc-accent-rgb), 0.26);
  background: rgba(var(--bc-accent-rgb), 0.1);
  color: var(--bc-ink);
}

.interview-history__heading {
  color: var(--bc-ink);
  font-size: 1.1rem;
  font-weight: 700;
  line-height: 1.2;
}

.interview-history-item {
  cursor: pointer;
  text-decoration: none;
}

.interview-history-item__score {
  background: rgba(var(--bc-accent-rgb), 0.08);
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
  border: 1px solid rgba(var(--bc-accent-rgb), 0.14);
  border-radius: 24px;
  padding: 22px 22px 20px;
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.18), transparent 34%),
    var(--panel-bg);
  box-shadow: none;
}

.dark .question-spotlight {
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.22), transparent 34%),
    var(--panel-bg);
  box-shadow:
    0 24px 50px rgba(0, 0, 0, 0.26),
    inset 0 1px 0 rgba(var(--bc-ink-rgb), 0.04);
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
  background: var(--panel-muted);
}

.question-spotlight__progress-bar {
  height: 100%;
  border-radius: inherit;
  transition: width 500ms ease;
}

.interview-orbit,
.scoring-scan {
  position: relative;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.14);
  background:
    radial-gradient(circle, rgba(var(--bc-accent-rgb), 0.18), transparent 58%),
    var(--panel-bg);
  box-shadow: none;
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
  border: 1px solid rgba(var(--bc-accent-rgb), 0.2);
  box-shadow: none;
}

.score-card-pass {
  background:
    radial-gradient(circle at 88% 14%, rgba(var(--bc-cyan-rgb), 0.32), transparent 32%),
    linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.92), rgba(54, 48, 42, 0.98));
}

.score-card-risk {
  background:
    radial-gradient(circle at 88% 14%, rgba(255, 107, 107, 0.34), transparent 32%),
    linear-gradient(135deg, rgba(74, 29, 35, 0.96), rgba(44, 34, 31, 0.98));
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
