<template>
  <div class="interview-cockpit h-full">
    <template v-if="phase === 'idle'">
      <section class="interview-setup-bar shell-section-card workspace-shell">
        <div class="workspace-head">
          <div class="workspace-head__top">
            <div class="workspace-head__main">
              <div class="flex flex-wrap items-center gap-2">
                <h1 class="workspace-title sm:text-[1.75rem]">模拟面试</h1>
                <span v-if="recommendedInterview" class="detail-pill">推荐设置</span>
              </div>
              <p class="workspace-summary">选择岗位方向、题量和上下文，开始一轮模拟面试。</p>
            </div>
            <div class="interview-setup-bar__head-actions">
              <div v-if="voiceAvailable" class="interview-mode-switch">
                <button
                  type="button"
                  class="interview-mode-switch__button"
                  :class="interviewMode === 'text' ? 'interview-mode-switch__button-active' : ''"
                  @click="interviewMode = 'text'"
                >
                  打字
                </button>
                <button
                  type="button"
                  class="interview-mode-switch__button"
                  :class="interviewMode === 'voice' ? 'interview-mode-switch__button-active' : ''"
                  @click="interviewMode = 'voice'"
                >
                  语音
                </button>
              </div>
              <el-button :loading="starting" type="primary" size="large" class="action-button !min-h-11" @click="handleStart()">
                {{ interviewMode === 'voice' && voiceAvailable ? '开始语音面试' : '开始模拟面试' }}
              </el-button>
            </div>
          </div>

          <div v-if="recommendedInterview" class="interview-setup-bar__summary">
            <span class="detail-pill">{{ difficultyText(recommendedInterview.difficulty) || '默认' }}</span>
            <span class="font-semibold text-ink">{{ recommendedInterview.direction }}</span>
            <span class="text-secondary">{{ recommendedInterview.questionCount }} 题</span>
            <span class="text-secondary">{{ recommendedInterview.reason || '根据最近训练情况推荐' }}</span>
          </div>

          <div class="interview-setup-grid">
            <div>
              <label class="flat-field-label">方向</label>
              <el-select v-model="direction" size="default" class="w-full">
                <el-option v-for="d in directions" :key="d.name" :label="d.name" :value="d.name" />
              </el-select>
            </div>
            <div>
              <label class="flat-field-label">目标岗位</label>
              <el-input v-model="jobRole" size="default" placeholder="Java 后端开发" />
            </div>
            <div>
              <label class="flat-field-label">经验</label>
              <el-select v-model="experienceLevel" size="default" class="w-full">
                <el-option v-for="item in experienceLevels" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
            <div>
              <label class="flat-field-label">时长</label>
              <el-select v-model="durationMinutes" size="default" class="w-full">
                <el-option v-for="minutes in durationOptions" :key="minutes" :label="`${minutes}分钟`" :value="minutes" />
              </el-select>
            </div>
            <div>
              <label class="flat-field-label">题量</label>
              <el-input-number
                v-model="questionCount"
                :min="3"
                :max="5"
                size="default"
                class="w-full"
                controls-position="right"
              />
            </div>
            <div>
              <label class="flat-field-label">技术范围</label>
              <el-input v-model="techStack" size="default" placeholder="Spring Boot, MySQL, Redis" />
            </div>
          </div>

          <div class="interview-context-row">
            <span class="text-xs font-semibold uppercase tracking-[0.12em] text-tertiary">上下文</span>
            <button
              type="button"
              class="interview-context-chip"
              :class="{ 'interview-context-chip-active': interviewContextPath === 'general' }"
              @click="applyInterviewContextPath('general')"
            >
              不带简历
            </button>
            <button
              type="button"
              class="interview-context-chip"
              :class="{ 'interview-context-chip-active': interviewContextPath === 'resume' }"
              @click="applyInterviewContextPath('resume')"
            >
              结合简历
            </button>
            <button
              type="button"
              class="interview-context-chip"
              :class="{ 'interview-context-chip-active': interviewContextPath === 'project' }"
              @click="applyInterviewContextPath('project')"
            >
              结合项目
            </button>
            <template v-if="interviewContextPath !== 'general'">
              <el-select
                v-model="selectedResumeId"
                clearable
                size="default"
                placeholder="选择简历"
                :loading="loadingResumes"
                class="w-40"
              >
                <el-option v-for="resume in resumes" :key="resume.id" :label="resume.title" :value="resume.id" />
              </el-select>
              <el-select
                v-if="interviewContextPath === 'project'"
                v-model="selectedProjectId"
                clearable
                size="default"
                placeholder="选择项目"
                :disabled="!selectedResumeId || !resumeProjects.length"
                class="w-40"
              >
                <el-option
                  v-for="project in resumeProjects"
                  :key="project.id"
                  :label="project.projectName"
                  :value="project.id"
                />
              </el-select>
            </template>
            <span class="text-xs text-tertiary">{{ draftContextSource?.summary }}</span>
          </div>
        </div>
      </section>

      <section class="shell-section-card workspace-shell interview-job-prep-shell">
        <div class="workspace-section">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <div class="flex flex-wrap items-center gap-2">
                <h3 class="workspace-section-title">JD 备面</h3>
                <span class="detail-pill">TechSpar 迁移能力</span>
              </div>
              <p class="workspace-section-summary">把岗位 JD、简历和投递信息压成一份定向备面草案，再决定下一轮模拟怎么打。</p>
            </div>
            <el-button
              :loading="jobPrepLoading"
              type="primary"
              size="large"
              class="action-button !min-h-11"
              @click="handleGenerateJobPrep"
            >
              生成备面结果
            </el-button>
          </div>

          <div class="job-prep-grid mt-5">
            <div class="space-y-4">
              <div>
                <label class="flat-field-label">关联投递</label>
                <el-select
                  v-model="selectedJobPrepApplicationId"
                  clearable
                  filterable
                  placeholder="选择已有投递，自动带入 JD"
                  :loading="loadingApplications"
                  class="w-full"
                >
                  <el-option
                    v-for="item in applications"
                    :key="item.id"
                    :label="`${item.company} · ${item.jobTitle}`"
                    :value="item.id"
                  />
                </el-select>
              </div>

              <div class="grid gap-4 md:grid-cols-2">
                <div>
                  <label class="flat-field-label">公司</label>
                  <el-input v-model="jobPrepCompany" placeholder="字节 / 阿里 / 自定义" />
                </div>
                <div>
                  <label class="flat-field-label">岗位</label>
                  <el-input v-model="jobPrepJobTitle" placeholder="Java 后端开发" />
                </div>
              </div>

              <div>
                <label class="flat-field-label">绑定简历</label>
                <el-select
                  v-model="jobPrepResumeId"
                  clearable
                  filterable
                  placeholder="选择要用于备面的简历"
                  class="w-full"
                >
                  <el-option v-for="resume in resumes" :key="resume.id" :label="resume.title" :value="resume.id" />
                </el-select>
              </div>

              <div>
                <label class="flat-field-label">岗位 JD</label>
                <el-input
                  v-model="jobPrepJdText"
                  type="textarea"
                  :rows="7"
                  placeholder="粘贴岗位描述，或先从关联投递带入。"
                />
              </div>
            </div>

            <div class="job-prep-result-shell">
              <div v-if="jobPrepLoading" class="flex h-full min-h-[280px] items-center justify-center">
                <div class="text-center">
                  <div class="mx-auto h-7 w-7 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
                  <p class="mt-3 text-sm text-secondary">正在生成定向备面结果...</p>
                </div>
              </div>
              <div v-else-if="!jobPrepSession" class="flex h-full min-h-[280px] items-center justify-center">
                <EmptyState
                  icon="clipboard"
                  title="先生成一份 JD 备面结果"
                  description="这里会给出匹配度、缺口、项目表达重点和建议追问，供下一轮模拟和真实面试直接使用。"
                  compact
                />
              </div>
              <div v-else class="space-y-4">
                <div class="job-prep-summary-card">
                  <div class="flex flex-wrap items-start justify-between gap-3">
                    <div class="min-w-0">
                      <div class="flex flex-wrap items-center gap-2">
                        <span class="detail-pill">{{ jobPrepSession.company || '未设公司' }}</span>
                        <span class="detail-pill">{{ jobPrepSession.jobTitle || '未设岗位' }}</span>
                        <span v-if="jobPrepSession.resumeTitle" class="detail-pill">{{ jobPrepSession.resumeTitle }}</span>
                      </div>
                      <p class="mt-3 text-sm leading-6 text-primary">{{ jobPrepSession.summary }}</p>
                    </div>
                    <div class="job-prep-score">
                      <span class="job-prep-score__label">匹配度</span>
                      <span class="job-prep-score__value">{{ Math.round(jobPrepSession.matchScore || 0) }}</span>
                    </div>
                  </div>
                </div>

                <div class="grid gap-3 sm:grid-cols-3">
                  <article class="job-prep-stat-card">
                    <p class="job-prep-stat-card__label">已命中关键词</p>
                    <p class="job-prep-stat-card__value">{{ jobPrepSession.matchedKeywords.length }}</p>
                  </article>
                  <article class="job-prep-stat-card job-prep-stat-card--risk">
                    <p class="job-prep-stat-card__label">待补缺口</p>
                    <p class="job-prep-stat-card__value">{{ jobPrepSession.missingKeywords.length }}</p>
                  </article>
                  <article class="job-prep-stat-card">
                    <p class="job-prep-stat-card__label">建议动作</p>
                    <p class="job-prep-stat-card__value">{{ jobPrepSession.nextActions.length }}</p>
                  </article>
                </div>

                <div class="grid gap-3 xl:grid-cols-2">
                  <article class="job-prep-panel">
                    <p class="job-prep-panel__title">关键词命中</p>
                    <div class="mt-3 flex flex-wrap gap-2">
                      <span v-for="tag in jobPrepSession.matchedKeywords" :key="`matched-${tag}`" class="job-prep-chip">
                        {{ tag }}
                      </span>
                      <span
                        v-for="tag in jobPrepSession.missingKeywords"
                        :key="`missing-${tag}`"
                        class="job-prep-chip job-prep-chip--risk"
                      >
                        {{ tag }}
                      </span>
                    </div>
                  </article>

                  <article class="job-prep-panel">
                    <p class="job-prep-panel__title">优先补位</p>
                    <ul class="job-prep-list mt-3">
                      <li v-for="item in jobPrepSession.focusAreas" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                </div>

                <div class="grid gap-3 xl:grid-cols-2">
                  <article class="job-prep-panel">
                    <p class="job-prep-panel__title">项目表达重点</p>
                    <ul class="job-prep-list mt-3">
                      <li v-for="item in jobPrepSession.resumeTalkingPoints" :key="item">{{ item }}</li>
                    </ul>
                  </article>

                  <article class="job-prep-panel">
                    <p class="job-prep-panel__title">建议追问</p>
                    <ul class="job-prep-list mt-3">
                      <li v-for="item in jobPrepSession.mockQuestions" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                </div>

                <article class="job-prep-panel">
                  <div class="flex flex-wrap items-center justify-between gap-3">
                    <p class="job-prep-panel__title">下一步动作</p>
                    <div class="flex flex-wrap items-center gap-2">
                      <el-button type="default" size="small" @click="handleGenerateCopilotPrep(true)">生成 Copilot Prep</el-button>
                      <el-button type="default" size="small" @click="applyJobPrepToInterview">把结果带入模拟面试</el-button>
                    </div>
                  </div>
                  <ul class="job-prep-list mt-3">
                    <li v-for="item in jobPrepSession.nextActions" :key="item">{{ item }}</li>
                  </ul>
                </article>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="shell-section-card workspace-shell interview-copilot-prep-shell">
        <div class="workspace-section">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <div class="flex flex-wrap items-center gap-2">
                <h3 class="workspace-section-title">实时 Copilot</h3>
                <span class="detail-pill">Prep Phase</span>
              </div>
              <p class="workspace-section-summary">先把 JD、简历、备面结果和 provider readiness 整理成一份会前 Prep，再进入后续实时阶段。</p>
            </div>
            <el-button
              :loading="copilotPrepLoading"
              type="primary"
              size="large"
              class="action-button !min-h-11"
              @click="handleGenerateCopilotPrep()"
            >
              生成 Copilot Prep
            </el-button>
          </div>

          <div class="copilot-prep-grid mt-5">
            <div class="space-y-4">
              <div v-if="missingCopilotProviders.length" class="copilot-prep-provider-alert">
                <p class="text-sm font-semibold text-ink">实时依赖未完全就绪</p>
                <p class="mt-2 text-sm leading-6 text-secondary">
                  当前仍有部分 provider 未配置或未启用。Prep 可以继续生成，但实时阶段可能会降级。
                </p>
                <div class="mt-3 flex flex-wrap gap-2">
                  <span v-for="item in missingCopilotProviders" :key="item.scope" class="detail-pill">
                    {{ item.label }} · {{ item.statusMessage }}
                  </span>
                </div>
              </div>

              <div>
                <label class="flat-field-label">关联投递</label>
                <el-select
                  v-model="selectedJobPrepApplicationId"
                  clearable
                  filterable
                  placeholder="选择已有投递，统一会前上下文"
                  :loading="loadingApplications"
                  class="w-full"
                >
                  <el-option
                    v-for="item in applications"
                    :key="item.id"
                    :label="`${item.company} · ${item.jobTitle}`"
                    :value="item.id"
                  />
                </el-select>
              </div>

              <div class="grid gap-4 md:grid-cols-2">
                <div>
                  <label class="flat-field-label">公司</label>
                  <el-input v-model="jobPrepCompany" placeholder="字节 / 阿里 / 自定义" />
                </div>
                <div>
                  <label class="flat-field-label">岗位</label>
                  <el-input v-model="jobPrepJobTitle" placeholder="Java 后端开发" />
                </div>
              </div>

              <div>
                <label class="flat-field-label">绑定简历</label>
                <el-select
                  v-model="jobPrepResumeId"
                  clearable
                  filterable
                  placeholder="选择这轮 Prep 要绑定的简历"
                  class="w-full"
                >
                  <el-option v-for="resume in resumes" :key="resume.id" :label="resume.title" :value="resume.id" />
                </el-select>
              </div>

              <div>
                <label class="flat-field-label">会前备注</label>
                <el-input
                  v-model="copilotPrepNotes"
                  type="textarea"
                  :rows="4"
                  placeholder="例如：下周一面偏项目和缓存；需要更关注开场表达和追问收口。"
                />
              </div>

              <div>
                <label class="flat-field-label">岗位 JD</label>
                <el-input
                  v-model="jobPrepJdText"
                  type="textarea"
                  :rows="6"
                  placeholder="可以复用上面的 JD，也可以单独补充这轮实时会话最关注的岗位要求。"
                />
              </div>
            </div>

            <div class="copilot-prep-result-shell">
              <div v-if="copilotPrepLoading" class="flex h-full min-h-[280px] items-center justify-center">
                <div class="text-center">
                  <div class="mx-auto h-7 w-7 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
                  <p class="mt-3 text-sm text-secondary">正在整理会前 Prep...</p>
                </div>
              </div>
              <div v-else-if="!copilotPrepSession" class="flex h-full min-h-[280px] items-center justify-center">
                <EmptyState
                  icon="chat"
                  title="先生成一份 Copilot Prep"
                  description="这里会整理开场提纲、实时提示、追问风险和 provider readiness，供后续实时阶段直接使用。"
                  compact
                />
              </div>
              <div v-else class="space-y-4">
                <div class="copilot-prep-summary-card">
                  <div class="flex flex-wrap items-start justify-between gap-3">
                    <div class="min-w-0">
                      <div class="flex flex-wrap items-center gap-2">
                        <span class="detail-pill">{{ copilotPrepSession.company || '未设公司' }}</span>
                        <span class="detail-pill">{{ copilotPrepSession.jobTitle || '未设岗位' }}</span>
                        <span v-if="copilotPrepSession.resumeTitle" class="detail-pill">{{ copilotPrepSession.resumeTitle }}</span>
                        <span v-if="copilotPrepSession.jobPrepSessionId" class="detail-pill">已关联 JD 备面</span>
                      </div>
                      <p class="mt-3 text-sm leading-6 text-primary">{{ copilotPrepSession.summary }}</p>
                    </div>
                    <el-button type="default" size="small" @click="applyCopilotPrepToInterview">带入模拟面试</el-button>
                  </div>
                </div>

                <article class="copilot-prep-panel">
                  <p class="copilot-prep-panel__title">Provider Readiness</p>
                  <div class="mt-3 grid gap-3 sm:grid-cols-3">
                    <div
                      v-for="item in copilotPrepSession.providerReadiness"
                      :key="item.scope"
                      class="copilot-provider-card"
                      :class="`copilot-provider-card--${item.status}`"
                    >
                      <p class="copilot-provider-card__label">{{ item.label }}</p>
                      <p class="copilot-provider-card__status">{{ item.status }}</p>
                      <p class="mt-2 text-xs leading-5 text-secondary">{{ item.statusMessage }}</p>
                    </div>
                  </div>
                </article>

                <div class="grid gap-3 xl:grid-cols-2">
                  <article class="copilot-prep-panel">
                    <p class="copilot-prep-panel__title">开场提纲</p>
                    <ul class="copilot-prep-list mt-3">
                      <li v-for="item in copilotPrepSession.openingBrief" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                  <article class="copilot-prep-panel">
                    <p class="copilot-prep-panel__title">实时提示</p>
                    <ul class="copilot-prep-list mt-3">
                      <li v-for="item in copilotPrepSession.liveCues" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                </div>

                <div class="grid gap-3 xl:grid-cols-2">
                  <article class="copilot-prep-panel">
                    <p class="copilot-prep-panel__title">当前风险</p>
                    <ul class="copilot-prep-list mt-3">
                      <li v-for="item in copilotPrepSession.keyRisks" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                  <article class="copilot-prep-panel">
                    <p class="copilot-prep-panel__title">建议追问</p>
                    <ul class="copilot-prep-list mt-3">
                      <li v-for="item in copilotPrepSession.followUpQuestions" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                </div>

                <article class="copilot-prep-panel">
                  <p class="copilot-prep-panel__title">下一步动作</p>
                  <ul class="copilot-prep-list mt-3">
                    <li v-for="item in copilotPrepSession.nextActions" :key="item">{{ item }}</li>
                  </ul>
                </article>

                <article class="copilot-prep-panel copilot-realtime-panel">
                  <div class="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <div class="flex flex-wrap items-center gap-2">
                        <p class="copilot-prep-panel__title !mb-0">Realtime Phase</p>
                        <span class="detail-pill">{{ copilotRealtimeConnectionLabel }}</span>
                        <span
                          v-if="copilotRealtimeSession"
                          class="detail-pill"
                          :class="copilotRealtimeSession.providerStatus === 'degraded' ? 'detail-pill-risk' : ''"
                        >
                          {{ copilotRealtimeSession.providerStatus === 'degraded' ? '降级模式' : '依赖就绪' }}
                        </span>
                      </div>
                      <p class="mt-2 text-sm leading-6 text-secondary">
                        这一阶段先承接会话创建、连接状态和事件时间线，后续再逐步接入实时转写和建议流。
                      </p>
                    </div>
                    <div class="flex flex-wrap gap-2">
                      <el-button
                        :loading="copilotRealtimeLoading"
                        type="default"
                        size="small"
                        @click="handleCreateCopilotRealtimeSession"
                      >
                        {{ copilotRealtimeSession ? '刷新实时会话' : '创建实时会话' }}
                      </el-button>
                      <el-button
                        :loading="copilotRealtimeConnecting"
                        :disabled="!copilotRealtimeSession || copilotRealtimeSocketState === 'connected'"
                        type="primary"
                        size="small"
                        @click="handleConnectCopilotRealtime"
                      >
                        {{ copilotRealtimeSocketState === 'connected' ? '已连接' : '连接实时阶段' }}
                      </el-button>
                      <el-button
                        :disabled="copilotRealtimeSocketState !== 'connected'"
                        type="default"
                        size="small"
                        @click="handleDisconnectCopilotRealtime"
                      >
                        断开连接
                      </el-button>
                    </div>
                  </div>

                  <div v-if="!copilotRealtimeSession" class="copilot-realtime-empty">
                    <p class="text-sm font-semibold text-ink">还没有实时会话</p>
                    <p class="mt-2 text-sm leading-6 text-secondary">
                      先基于当前 Prep 创建一条实时会话，再连接 `WS /ws/interview/copilot/{sessionId}`。
                    </p>
                  </div>
                  <div v-else class="mt-4 space-y-4">
                    <div class="grid gap-3 md:grid-cols-3">
                      <article class="copilot-realtime-status-card">
                        <p class="copilot-realtime-status-card__label">会话状态</p>
                        <p class="copilot-realtime-status-card__value">{{ realtimeStatusText(copilotRealtimeSession.status) }}</p>
                        <p class="mt-2 text-xs leading-5 text-secondary">{{ copilotRealtimeSession.latestEventSummary || '等待下一条实时事件。' }}</p>
                      </article>
                      <article class="copilot-realtime-status-card">
                        <p class="copilot-realtime-status-card__label">连接状态</p>
                        <p class="copilot-realtime-status-card__value">{{ copilotRealtimeConnectionLabel }}</p>
                        <p class="mt-2 text-xs leading-5 text-secondary">
                          {{ copilotRealtimeSession.connectedAt ? `最近连接：${formatRelativeTime(copilotRealtimeSession.connectedAt)}` : '还未建立连接。' }}
                        </p>
                      </article>
                      <article class="copilot-realtime-status-card">
                        <p class="copilot-realtime-status-card__label">Provider Gate</p>
                        <p class="copilot-realtime-status-card__value">
                          {{ copilotRealtimeSession.providerStatus === 'degraded' ? '降级' : '正常' }}
                        </p>
                        <p class="mt-2 text-xs leading-5 text-secondary">
                          {{ copilotRealtimeSession.providerStatus === 'degraded' ? '仍有 provider 未完全就绪，实时阶段只开放骨架能力。' : '当前 provider readiness 已满足基础实时阶段。' }}
                        </p>
                      </article>
                    </div>

                    <div v-if="copilotRealtimeSession.providerStatus === 'degraded'" class="copilot-prep-provider-alert">
                      <p class="text-sm font-semibold text-ink">实时阶段当前是降级模式</p>
                      <div class="mt-3 flex flex-wrap gap-2">
                        <span
                          v-for="item in copilotRealtimeSession.providerReadiness.filter((entry) => entry.status !== 'ready' && entry.status !== 'saved')"
                          :key="`realtime-provider-${item.scope}`"
                          class="detail-pill"
                        >
                          {{ item.label }} · {{ item.statusMessage }}
                        </span>
                      </div>
                    </div>

                    <div class="grid gap-3 xl:grid-cols-[minmax(0,1fr),minmax(0,1.2fr)]">
                      <div class="space-y-3">
                        <div>
                          <p class="copilot-prep-panel__title">实时检查清单</p>
                          <ul class="copilot-prep-list mt-3">
                            <li v-for="item in copilotRealtimeSession.liveChecklist" :key="`check-${item}`">{{ item }}</li>
                          </ul>
                        </div>
                        <div>
                          <label class="flat-field-label">运行中备注</label>
                          <el-input
                            v-model="copilotRealtimeNote"
                            type="textarea"
                            :rows="3"
                            :disabled="copilotRealtimeSocketState !== 'connected'"
                            placeholder="连接建立后，可以把面试官风格、突发追问、要补的例子写成运行中备注。"
                          />
                          <div class="mt-3 flex flex-wrap gap-2">
                            <el-button
                              :disabled="copilotRealtimeSocketState !== 'connected' || !copilotRealtimeNote.trim()"
                              type="default"
                              size="small"
                              @click="handleSendCopilotRealtimeNote"
                            >
                              发送备注
                            </el-button>
                            <el-button
                              :disabled="copilotRealtimeSocketState !== 'connected'"
                              type="default"
                              size="small"
                              @click="handleCompleteCopilotRealtime"
                            >
                              结束实时阶段
                            </el-button>
                          </div>
                        </div>
                      </div>

                      <div>
                        <div
                          v-if="copilotRealtimeSession.postInterviewReview"
                          class="copilot-post-review-shell mb-3"
                        >
                          <div class="flex flex-wrap items-start justify-between gap-3">
                            <div>
                              <p class="copilot-prep-panel__title !mb-0">面后复盘建议</p>
                              <p class="mt-2 text-sm leading-6 text-secondary">
                                {{ copilotRealtimeSession.postInterviewReview.summary }}
                              </p>
                            </div>
                            <RouterLink :to="copilotRealtimeAgentLink" class="hard-button-primary">
                              {{ copilotRealtimeSession.postInterviewReview.nextActionLabel || '前往面后复盘' }}
                            </RouterLink>
                          </div>
                          <div class="mt-4 grid gap-3 md:grid-cols-3">
                            <article class="copilot-post-review-card">
                              <p class="copilot-post-review-card__title">保留项</p>
                              <ul class="copilot-prep-list mt-3">
                                <li v-for="item in copilotRealtimeSession.postInterviewReview.strengths" :key="`strength-${item}`">{{ item }}</li>
                              </ul>
                            </article>
                            <article class="copilot-post-review-card">
                              <p class="copilot-post-review-card__title">待处理</p>
                              <ul class="copilot-prep-list mt-3">
                                <li v-for="item in copilotRealtimeSession.postInterviewReview.weakPoints" :key="`weak-${item}`">{{ item }}</li>
                              </ul>
                            </article>
                            <article class="copilot-post-review-card">
                              <p class="copilot-post-review-card__title">下一步</p>
                              <ul class="copilot-prep-list mt-3">
                                <li v-for="item in copilotRealtimeSession.postInterviewReview.recommendedActions" :key="`action-${item}`">{{ item }}</li>
                              </ul>
                            </article>
                          </div>
                        </div>
                        <p class="copilot-prep-panel__title">事件时间线</p>
                        <div class="copilot-realtime-events mt-3">
                          <article
                            v-for="event in copilotRealtimeSession.events"
                            :key="event.id"
                            class="copilot-realtime-event"
                          >
                            <div class="flex items-start justify-between gap-3">
                              <div class="min-w-0">
                                <p class="copilot-realtime-event__title">{{ realtimeEventLabel(event.eventType) }}</p>
                                <p class="mt-1 text-sm leading-6 text-primary">{{ event.summary }}</p>
                              </div>
                              <div class="text-right">
                                <p class="text-xs uppercase tracking-[0.18em] text-tertiary">{{ event.source }}</p>
                                <p class="mt-1 text-xs text-secondary">{{ formatRelativeTime(event.createTime) }}</p>
                              </div>
                            </div>
                          </article>
                          <div v-if="!copilotRealtimeSession.events.length" class="copilot-realtime-empty !mt-0">
                            <p class="text-sm text-secondary">连接后会在这里看到会话事件和状态流转。</p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </article>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="shell-section-card workspace-shell interview-recording-review-shell">
        <div class="workspace-section">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <div class="flex flex-wrap items-center gap-2">
                <h3 class="workspace-section-title">录音复盘</h3>
                <span class="detail-pill">真实面试回放</span>
              </div>
              <p class="workspace-section-summary">上传一段真实面试录音，系统会先转写，再给出结构化复盘、薄弱点和下一步训练动作。</p>
            </div>
            <el-button
              :loading="recordingReviewLoading"
              :disabled="!voiceAvailable || !recordingReviewFile"
              type="primary"
              size="large"
              class="action-button !min-h-11"
              @click="handleCreateRecordingReview"
            >
              生成录音复盘
            </el-button>
          </div>

          <div class="recording-review-grid mt-5">
            <div class="space-y-4">
              <div v-if="!voiceAvailable" class="recording-review-provider-alert">
                <p class="text-sm font-semibold text-ink">ASR 未配置</p>
                <p class="mt-2 text-sm leading-6 text-secondary">
                  录音复盘依赖语音识别服务。请先到设置页完成 `ASR provider` 配置，再回来上传录音。
                </p>
              </div>

              <div>
                <label class="flat-field-label">复盘方向</label>
                <el-input v-model="direction" placeholder="例如 Spring / MySQL / 一面综合" />
              </div>

              <div>
                <label class="flat-field-label">目标岗位</label>
                <el-input v-model="jobRole" placeholder="Java 后端开发" />
              </div>

              <div>
                <label class="flat-field-label">场景备注</label>
                <el-input
                  v-model="recordingReviewNotes"
                  type="textarea"
                  :rows="4"
                  placeholder="例如：一面主要问项目、缓存和线程池，这段录音是 Redis 部分。"
                />
              </div>

              <div>
                <label class="flat-field-label">录音文件</label>
                <label class="recording-review-upload">
                  <input accept="audio/*,.webm,.wav,.mp3,.m4a,.ogg" class="hidden" type="file" @change="handleRecordingFileChange" />
                  <span class="recording-review-upload__title">
                    {{ recordingReviewFile ? recordingReviewFile.name : '选择录音文件' }}
                  </span>
                  <span class="recording-review-upload__hint">支持 webm / wav / mp3 / m4a / ogg，最大 15MB</span>
                </label>
              </div>
            </div>

            <div class="recording-review-result-shell">
              <div v-if="recordingReviewLoading && !recordingReviewSession" class="flex h-full min-h-[280px] items-center justify-center">
                <div class="text-center">
                  <div class="mx-auto h-7 w-7 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
                  <p class="mt-3 text-sm text-secondary">正在转写录音并生成复盘...</p>
                </div>
              </div>
              <div v-else-if="!recordingReviewSession" class="flex h-full min-h-[280px] items-center justify-center">
                <EmptyState
                  icon="review"
                  title="先上传一段真实录音"
                  description="这里会展示转写文本、片段信号、优点、薄弱点和下一步训练动作。"
                  compact
                />
              </div>
              <div v-else-if="recordingReviewPending" class="recording-review-status-shell">
                <div class="recording-review-status-pill">
                  {{ recordingReviewStatusLabel(recordingReviewSession.status) }}
                </div>
                <h4 class="mt-4 font-display text-2xl font-semibold text-ink">录音已接收，正在后台处理中</h4>
                <p class="mt-3 text-sm leading-6 text-secondary">
                  {{ recordingReviewSession.statusMessage || recordingReviewSession.summary || '系统正在依次执行上传校验、转写和复盘整理。' }}
                </p>
                <div class="mt-5 flex items-center gap-3">
                  <div class="h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
                  <span class="text-xs text-tertiary">页面会自动刷新状态，不需要重复上传。</span>
                </div>
              </div>
              <div v-else-if="recordingReviewSession.status === 'failed'" class="recording-review-status-shell recording-review-status-shell--failed">
                <div class="recording-review-status-pill recording-review-status-pill--failed">
                  {{ recordingReviewStatusLabel(recordingReviewSession.status) }}
                </div>
                <h4 class="mt-4 font-display text-2xl font-semibold text-ink">这段录音还没成功生成复盘</h4>
                <p class="mt-3 text-sm leading-6 text-secondary">
                  {{ recordingReviewSession.statusMessage || recordingReviewSession.summary || '请重新上传更清晰的录音，或先检查 ASR provider。' }}
                </p>
              </div>
              <div v-else class="space-y-4">
                <div class="recording-review-summary-card">
                  <div class="flex flex-wrap items-start justify-between gap-3">
                    <div class="min-w-0">
                      <div class="flex flex-wrap items-center gap-2">
                        <span class="detail-pill">{{ recordingReviewSession.direction || '未设方向' }}</span>
                        <span class="detail-pill">{{ recordingReviewSession.jobRole || '未设岗位' }}</span>
                        <span class="detail-pill">{{ recordingReviewStatusLabel(recordingReviewSession.status) }}</span>
                        <span v-if="recordingReviewSession.transcriptConfidence" class="detail-pill">
                          置信度 {{ Math.round(recordingReviewSession.transcriptConfidence * 100) }}%
                        </span>
                      </div>
                      <p class="mt-3 text-sm leading-6 text-primary">{{ recordingReviewSession.summary }}</p>
                    </div>
                    <div class="flex flex-col items-end gap-3">
                      <div class="recording-review-score">
                        <span class="recording-review-score__label">复盘分</span>
                        <span class="recording-review-score__value">{{ Math.round(recordingReviewSession.overallScore || 0) }}</span>
                      </div>
                      <RouterLink :to="recordingReviewAgentLink" class="hard-button-secondary text-sm">
                        转成训练动作
                      </RouterLink>
                    </div>
                  </div>
                </div>

                <div class="grid gap-3 xl:grid-cols-2">
                  <article class="recording-review-panel">
                    <p class="recording-review-panel__title">转写文本</p>
                    <p class="mt-3 whitespace-pre-wrap text-sm leading-6 text-primary">{{ recordingReviewSession.transcript }}</p>
                  </article>

                  <article class="recording-review-panel">
                    <p class="recording-review-panel__title">建议动作</p>
                    <ul class="recording-review-list mt-3">
                      <li v-for="item in recordingReviewSession.suggestedActions" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                </div>

                <div class="grid gap-3 xl:grid-cols-2">
                  <article class="recording-review-panel">
                    <p class="recording-review-panel__title">这次做得好的地方</p>
                    <ul class="recording-review-list mt-3">
                      <li v-for="item in recordingReviewSession.strengths" :key="item">{{ item }}</li>
                    </ul>
                  </article>

                  <article class="recording-review-panel">
                    <p class="recording-review-panel__title">当前薄弱点</p>
                    <ul class="recording-review-list mt-3">
                      <li v-for="item in recordingReviewSession.weakPoints" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                </div>

                <article class="recording-review-panel">
                  <p class="recording-review-panel__title">转写片段</p>
                  <div class="mt-3 space-y-2">
                    <div
                      v-for="segment in recordingReviewSession.segments"
                      :key="segment.id"
                      class="recording-review-segment"
                    >
                      <div class="flex flex-wrap items-center gap-2 text-xs text-secondary">
                        <span class="font-mono text-ink">#{{ segment.segmentIndex }}</span>
                        <span>{{ signalLabel(segment.signalType) }}</span>
                        <span>{{ formatSegmentOffset(segment.startOffsetMs) }} - {{ formatSegmentOffset(segment.endOffsetMs) }}</span>
                      </div>
                      <p class="mt-2 text-sm leading-6 text-primary">{{ segment.transcriptText }}</p>
                    </div>
                  </div>
                </article>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="shell-section-card workspace-shell interview-history-shell">
        <div class="workspace-section">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h3 class="workspace-section-title">面试记录</h3>
              <p class="workspace-section-summary">查看最近几轮面试记录和得分。</p>
            </div>
            <el-select v-model="historyFilterDirection" clearable placeholder="全部方向" size="default" class="w-36">
              <el-option v-for="d in directions" :key="d.name" :label="d.name" :value="d.name" />
            </el-select>
          </div>

          <div v-if="allHistoryLoading" class="mt-4 py-6 text-center text-xs text-tertiary">正在加载最近几轮模拟面试记录...</div>
          <div v-else-if="!allHistoryItems.length" class="mt-4 py-6 text-center">
            <p class="text-xs leading-5 text-secondary">{{ EMPTY_STATE_COPY.interviewHistory.description }}</p>
          </div>
          <div v-else class="mt-3 space-y-1">
            <RouterLink
              v-for="item in allHistoryItems"
              :key="item.sessionId"
              :to="`/interview/detail/${item.sessionId}`"
              class="flex items-center gap-4 rounded-xl px-3 py-2 transition hover:bg-[var(--interactive-hover)]"
            >
              <div
                class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg font-mono text-base font-bold"
                :class="scoreClass(item.totalScore)"
                :style="{ background: 'rgba(var(--bc-accent-rgb), 0.08)' }"
              >
                {{ Math.round(item.totalScore) }}
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2">
                  <span class="text-sm font-semibold text-ink">{{ item.direction }}</span>
                  <span class="text-xs text-tertiary">{{ item.questionCount }} 题</span>
                  <span
                    v-if="item.mode === 'voice'"
                    class="rounded-full bg-accent/10 px-1.5 py-0.5 text-xs font-medium text-accent"
                    >语音</span
                  >
                  <span class="text-xs text-tertiary">{{ item.jobRole || '未设置岗位' }}</span>
                  <span class="text-xs text-tertiary">{{ experienceLabel(item.experienceLevel) }}</span>
                </div>
                <div class="text-xs text-tertiary">{{ formatRelativeTime(item.endTime || item.startTime) }}</div>
              </div>
              <svg
                class="h-4 w-4 shrink-0 text-tertiary"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
              </svg>
            </RouterLink>
          </div>

          <div v-if="allHistoryTotalPages > 1" class="mt-4 flex justify-center">
            <el-pagination
              v-model:current-page="allHistoryPage"
              :page-size="allHistoryPageSize"
              :total="allHistoryTotal"
              layout="prev, pager, next"
              @current-change="loadAllHistory"
            />
          </div>
        </div>
      </section>
    </template>

    <!-- Active states: two-column layout -->
    <section v-if="phase !== 'idle'" class="grid gap-3 lg:grid-cols-[260px_minmax(0,1fr)]">
      <aside class="shell-section-card p-3 sm:p-4">
        <h3 class="text-sm font-bold text-ink">
          {{ phase === 'finished' ? '面试已完成' : phase === 'result' ? '本题已评分' : '当前面试' }}
        </h3>

        <div class="mt-3 space-y-2">
          <!-- Progress -->
          <div class="flex items-center justify-between text-xs uppercase tracking-[0.22em] text-tertiary">
            <span>进度</span>
            <span>{{ currentQuestion?.currentIndex ?? 0 }} / {{ currentQuestion?.questionCount ?? 0 }}</span>
          </div>
          <div class="h-1.5 overflow-hidden rounded-full bg-[var(--panel-muted)]">
            <div
              class="h-full rounded-full bg-accent transition-[width] duration-500"
              :style="{ width: `${progressPercent}%` }"
            ></div>
          </div>

          <!-- Compact info grid -->
          <div class="mt-2 grid grid-cols-2 gap-x-3 gap-y-1.5 text-xs">
            <div>
              <span class="text-tertiary">方向</span>
              <div class="font-semibold text-ink">{{ sessionDirection }}</div>
            </div>
            <div>
              <span class="text-tertiary">模式</span>
              <div class="font-semibold text-ink">
                {{ interviewMode === 'voice' && voiceAvailable ? '语音' : '文字' }}
              </div>
            </div>
            <div>
              <span class="text-tertiary">岗位</span>
              <div class="font-semibold text-ink">{{ sessionJobRole || '未设置' }}</div>
            </div>
            <div>
              <span class="text-tertiary">经验</span>
              <div class="font-semibold text-ink">{{ experienceLabel(sessionExperienceLevel) }}</div>
            </div>
            <div>
              <span class="text-tertiary">技术</span>
              <div class="font-semibold text-ink">{{ sessionTechStack || '未限定' }}</div>
            </div>
            <div>
              <span class="text-tertiary">配置</span>
              <div class="font-semibold text-ink">
                {{ sessionDurationMinutes }}分钟/{{ currentQuestion?.questionCount ?? questionCount }}题
              </div>
            </div>
          </div>

          <div v-if="activeContextSummary" class="mt-1 text-xs text-secondary">
            <span class="font-semibold text-tertiary">{{ activeContextSource?.label || '上下文' }}：</span
            >{{ activeContextSummary }}
          </div>
        </div>
      </aside>

      <section class="shell-section-card interview-session-card flex flex-col p-4 sm:p-6">
        <div v-if="false"></div>

        <div v-else-if="phase === 'answering'" class="flex flex-1 flex-col">
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
                  {{ currentQuestion?.questionTitle ?? '正在加载当前问题...' }}
                </h4>
              </div>
              <div class="question-spotlight__timer">
                <span class="question-spotlight__timer-label">
                  {{ interviewMode === 'voice' && voiceAvailable ? '语音作答' : '当前作答' }}
                </span>
                <span class="question-spotlight__timer-value" :class="countdownUrgent ? 'text-coral' : 'text-accent'">
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

          <template v-if="interviewMode !== 'voice' || !voiceAvailable">
            <div class="mt-2 flex items-center justify-between">
              <span class="text-xs text-tertiary">Ctrl + Enter 快速提交</span>
            </div>
            <el-input
              v-model="answerText"
              type="textarea"
              :rows="8"
              placeholder="写结论，并补充关键原因和取舍。"
              class="interview-answer-input mt-2 flex-1"
              @keydown.ctrl.enter.prevent="handleSubmitAnswer"
            />
            <div class="mt-3">
              <el-button
                :loading="submitting"
                type="primary"
                size="large"
                class="action-button w-full"
                @click="handleSubmitAnswer"
              >
                提交答案并评分
              </el-button>
            </div>
          </template>

          <template v-else>
            <div class="mt-3 flex-1">
              <VoiceRecorder
                :disabled="voiceSubmitting"
                @recorded="handleVoiceRecorded"
                @cleared="handleVoiceCleared"
              />
            </div>
            <div class="mt-3">
              <el-button
                :loading="voiceSubmitting"
                :disabled="!voiceAudioBlob"
                type="primary"
                size="large"
                class="action-button w-full"
                @click="handleVoiceSubmit"
              >
                提交语音答案
              </el-button>
            </div>
          </template>
        </div>

        <div v-else-if="phase === 'scoring'" class="flex flex-1 items-center justify-center">
          <div class="w-full max-w-sm text-center">
            <div class="scoring-scan mx-auto flex h-28 w-28 items-center justify-center rounded-full">
              <div class="h-10 w-10 animate-spin rounded-full border-4 border-accent border-t-transparent"></div>
            </div>
            <h4 class="mt-5 font-display text-3xl font-semibold leading-none text-ink">正在评分</h4>
          </div>
        </div>

        <div v-else-if="phase === 'result'" class="space-y-3">
          <div v-if="voiceTranscript" class="py-2">
            <div class="flex items-center justify-between">
              <span class="text-xs uppercase tracking-[0.24em] text-tertiary">语音转录</span>
              <span v-if="lastVoiceResult?.transcriptConfidence" class="font-mono text-xs text-tertiary">
                {{ Math.round(lastVoiceResult.transcriptConfidence * 100) }}%
              </span>
            </div>
            <p class="mt-1 text-sm leading-6 text-primary">{{ voiceTranscript }}</p>
          </div>
          <div v-if="voiceTranscript" class="flat-field-divider"></div>

          <div class="score-card p-4" :class="(lastResult?.score ?? 0) >= 60 ? 'score-card-pass' : 'score-card-risk'">
            <div class="flex items-center gap-4">
              <div class="font-mono text-5xl font-semibold text-white" style="font-variant-numeric: tabular-nums">{{ animatedScore }}</div>
              <div class="min-w-0 flex-1">
                <div class="text-xs uppercase tracking-[0.24em] text-white/65">智能评分</div>
                <p class="mt-1 text-sm leading-6 text-white/82 line-clamp-2">{{ lastResult?.comment }}</p>
              </div>
            </div>
          </div>

          <div v-if="lastResult?.scoreBreakdown?.length" class="flex gap-3">
            <div v-for="item in lastResult.scoreBreakdown" :key="`${item.dimension}-${item.score}`" class="flex-1">
              <div class="flex items-center justify-between">
                <span class="text-xs uppercase tracking-[0.22em] text-tertiary">{{ item.dimension }}</span>
                <span class="font-mono text-xl font-semibold text-ink" style="font-variant-numeric: tabular-nums">{{ item.score }}</span>
              </div>
              <p class="mt-0.5 text-xs leading-4 text-secondary line-clamp-1">{{ item.summary }}</p>
            </div>
          </div>

          <div class="flat-field-divider"></div>

          <div v-if="lastResult?.weakPointTags?.length" class="flex flex-wrap items-center gap-2">
            <span class="text-xs text-tertiary">薄弱点</span>
            <span
              v-for="tag in lastResult.weakPointTags"
              :key="tag"
              class="rounded-full bg-coral/10 px-2.5 py-0.5 text-xs font-semibold text-coral"
            >
              {{ tag }}
            </span>
          </div>

          <div v-if="lastResult?.reviewSummary" class="text-xs text-secondary">
            <span class="font-semibold text-tertiary">复盘：</span>{{ lastResult.reviewSummary }}
          </div>

          <div class="flat-field-divider"></div>

          <div class="grid gap-3 md:grid-cols-2">
            <div>
              <div class="flex items-center justify-between">
                <span class="text-xs font-semibold text-ink">标准答案</span>
                <button
                  v-if="lastResult?.standardAnswer"
                  type="button"
                  class="text-xs text-accent hover:underline"
                  @click="speakText(lastResult!.standardAnswer!)"
                >
                  朗读
                </button>
              </div>
              <p class="mt-1 text-xs leading-5 text-secondary line-clamp-3">
                {{ lastResult?.standardAnswer || '这题暂时没有参考答案。可以回看你的回答和复盘建议。' }}
              </p>
            </div>
            <div>
              <div class="flex items-center justify-between">
                <span class="text-xs font-semibold text-ink">追问</span>
                <button
                  v-if="lastResult?.followUp"
                  type="button"
                  class="text-xs text-accent hover:underline"
                  @click="speakText(lastResult!.followUp!)"
                >
                  朗读
                </button>
              </div>
              <p class="mt-1 text-xs leading-5 text-secondary line-clamp-3">{{ lastResult?.followUp || '无' }}</p>
            </div>
          </div>

          <p v-if="lastResult?.addedToWrongBook" class="text-xs text-coral">
            已加入错题本 — 得分低于 60 分，后续进入间隔复习。
          </p>

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

        <div v-else-if="phase === 'finished'" class="space-y-3">
          <div class="score-card p-4" :class="'score-card-pass'">
            <div class="flex items-center gap-4">
              <div class="font-mono text-5xl font-semibold tracking-[-0.05em] text-white">
                {{ detail?.totalScore ?? '-' }}
              </div>
              <div class="min-w-0 flex-1">
                <div class="text-xs uppercase tracking-[0.24em] text-white/65">总分</div>
                <p class="mt-1 text-sm text-white/82">
                  共 {{ detail?.questionCount ?? 0 }} 题，方向：{{ detail?.direction }}
                  <span
                    v-if="detail?.mode === 'voice'"
                    class="ml-2 inline-flex items-center rounded-full bg-white/20 px-2 py-0.5 text-xs"
                    >语音</span
                  >
                </p>
              </div>
            </div>
          </div>

          <div class="flex flex-wrap gap-2 text-xs">
            <span class="detail-pill">{{ detail?.jobRole || '未设置岗位' }}</span>
            <span class="detail-pill">{{ experienceLabel(detail?.experienceLevel) }}</span>
            <span class="detail-pill">{{ detail?.techStack || '未限定' }}</span>
            <span class="detail-pill">{{ detail?.durationMinutes || durationMinutes }}分钟</span>
            <span class="detail-pill">{{ interviewContextLabel(detail?.contextSource, detail?.includeResumeProject) }}</span>
          </div>

          <div v-if="detail?.records?.length" class="space-y-1">
            <div
              v-for="(record, index) in detail.records"
              :key="record.questionId"
              class="cursor-pointer rounded-xl p-2.5 transition hover:bg-[var(--interactive-hover)]"
              @click="toggleQuestion(record.questionId)"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0 flex-1">
                  <div class="text-xs uppercase tracking-[0.22em] text-tertiary">Q{{ index + 1 }}</div>
                  <div class="mt-0.5 text-sm font-semibold text-ink">{{ record.questionTitle }}</div>
                  <p class="mt-1 text-xs leading-5 text-secondary line-clamp-1">
                    {{ record.comment || '这题暂时没有点评。可以结合得分和标准答案复盘。' }}
                  </p>
                </div>
                <div class="flex shrink-0 items-center gap-2">
                  <div
                    class="font-mono text-2xl font-semibold"
                    style="font-variant-numeric: tabular-nums"
                    :class="record.score >= 60 ? 'text-accent' : 'text-coral'"
                  >
                    {{ record.score ?? '-' }}
                  </div>
                  <svg
                    class="h-3.5 w-3.5 text-tertiary transition-transform"
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
                class="mt-2 space-y-2 border-t border-[var(--bc-line)] pt-2"
              >
                <div v-if="record.userAnswer">
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">我的回答</div>
                  <p class="mt-0.5 whitespace-pre-wrap text-xs leading-5 text-primary line-clamp-3">
                    {{ record.userAnswer }}
                  </p>
                </div>
                <div v-if="record.standardAnswer">
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">标准答案</div>
                  <p class="mt-0.5 whitespace-pre-wrap text-xs leading-5 text-primary line-clamp-3">
                    {{ record.standardAnswer }}
                  </p>
                </div>
                <div v-if="record.followUp">
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">追问</div>
                  <p class="mt-0.5 text-xs leading-5 text-primary">{{ record.followUp }}</p>
                </div>
                <div v-if="record.scoreBreakdown?.length" class="flex gap-2">
                  <div
                    v-for="item in record.scoreBreakdown"
                    :key="`${record.questionId}-${item.dimension}`"
                    class="flex-1 rounded-xl bg-[var(--panel-muted)] p-2"
                  >
                    <div class="flex items-center justify-between">
                      <span class="text-xs text-tertiary">{{ item.dimension }}</span>
                      <span class="font-mono text-lg font-semibold text-ink">{{ item.score }}</span>
                    </div>
                    <p class="mt-0.5 text-xs leading-4 text-secondary line-clamp-1">{{ item.summary }}</p>
                  </div>
                </div>
                <div v-if="record.weakPointTags?.length" class="flex flex-wrap gap-1.5">
                  <span
                    v-for="tag in record.weakPointTags"
                    :key="`${record.questionId}-${tag}`"
                    class="rounded-full bg-coral/10 px-2 py-0.5 text-xs font-semibold text-coral"
                  >
                    {{ tag }}
                  </span>
                </div>
                <div v-if="record.reviewSummary">
                  <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">复盘</div>
                  <p class="mt-0.5 whitespace-pre-wrap text-xs leading-5 text-primary line-clamp-2">
                    {{ record.reviewSummary }}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div class="flex gap-2">
            <RouterLink to="/wrong" class="hard-button-secondary flex-1 text-center text-sm">错题本</RouterLink>
            <RouterLink to="/review" class="hard-button-secondary flex-1 text-center text-sm">去复习</RouterLink>
            <el-button type="primary" size="default" class="action-button flex-1" @click="handleNewInterview">
              开始新一场
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
import { fetchApplicationBoardApi } from '@/api/applications'
import { EMPTY_STATE_COPY, ERROR_COPY } from '@/constants/productCopy'
import {
  buildCopilotRealtimeWebSocketUrl,
  createCopilotPrepSessionApi,
  createCopilotRealtimeSessionApi,
  createRecordingReviewApi,
  createJobPrepSessionApi,
  currentQuestionApi,
  fetchCopilotRealtimeSessionApi,
  fetchInterviewHistoryApi,
  fetchRecordingReviewApi,
  fetchVoiceStatusApi,
  interviewDetailApi,
  startInterviewApi,
  startVoiceInterviewApi,
  submitAnswerApi,
  submitVoiceAnswerApi
} from '@/api/interview'
import { fetchRecommendInterviewApi } from '@/api/adaptive'
import { fetchResumeDetailApi, fetchResumeListApi } from '@/api/resume'
import { fetchProviderConfigsApi } from '@/api/settings'
import EmptyState from '@/components/EmptyState.vue'
import { isProviderStatusMissing } from '@/utils/providerReadiness'
import type {
  CopilotPrepSession,
  CopilotRealtimeSession,
  ContextSource,
  InterviewAnswerResult,
  InterviewCurrentQuestion,
  InterviewDetail,
  InterviewHistoryItem,
  JobApplicationItem,
  JobPrepSession,
  RecordingReviewSession,
  RecommendInterview,
  ResumeProjectItem,
  ResumeSummaryItem,
  UserProviderConfigItem,
  VoiceSubmitResult
} from '@/types/api'
import { buildAgentWorkbenchLocation } from '@/utils/agent'
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
const applications = ref<JobApplicationItem[]>([])
const loadingApplications = ref(false)
const selectedJobPrepApplicationId = ref('')
const jobPrepResumeId = ref('')
const jobPrepCompany = ref('')
const jobPrepJobTitle = ref('Java 后端开发')
const jobPrepJdText = ref('')
const jobPrepLoading = ref(false)
const jobPrepSession = ref<JobPrepSession | null>(null)
const copilotPrepNotes = ref('')
const copilotPrepLoading = ref(false)
const copilotPrepSession = ref<CopilotPrepSession | null>(null)
const copilotRealtimeLoading = ref(false)
const copilotRealtimeConnecting = ref(false)
const copilotRealtimeSession = ref<CopilotRealtimeSession | null>(null)
const copilotRealtimeSocketState = ref<'idle' | 'connecting' | 'connected' | 'disconnected' | 'error'>('idle')
const copilotRealtimeNote = ref('')
const linkedCopilotJobPrepId = ref('')
const recordingReviewNotes = ref('')
const recordingReviewFile = ref<File | null>(null)
const recordingReviewLoading = ref(false)
const recordingReviewSession = ref<RecordingReviewSession | null>(null)
const providerConfigs = ref<UserProviderConfigItem[]>([])
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
// Full history list
const allHistoryItems = ref<InterviewHistoryItem[]>([])
const allHistoryLoading = ref(false)
const allHistoryPage = ref(1)
const allHistoryPageSize = ref(10)
const allHistoryTotal = ref(0)
const allHistoryTotalPages = ref(0)
const historyFilterDirection = ref('')

const seededQuestionTitle = ref('')
const seededQuestionMeta = ref('')
const recommendedInterview = ref<RecommendInterview | null>(null)

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
        ? `本轮面试会结合项目「${project.projectName}」出题和追问。`
        : resume
          ? `本轮面试会结合简历《${resume.title}》里的经历和项目内容。`
          : '选择一份简历，也可以进一步指定项目。'
    }
  }
  if (interviewContextPath.value === 'resume') {
    const resume = resumes.value.find((item) => item.id === selectedResumeId.value)
    return {
      type: 'resume',
      label: '简历上下文',
      resumeId: selectedResumeId.value || undefined,
      resumeTitle: resume?.title,
      summary: resume ? `本轮面试会结合简历《${resume.title}》里的经历和项目内容。` : '请选择一份简历后开始面试。'
    }
  }
  return {
    type: 'general',
    label: '不带简历',
    summary: seededQuestionTitle.value
      ? `本轮面试会围绕题目「${seededQuestionTitle.value}」展开${seededQuestionMeta.value ? `，重点参考${seededQuestionMeta.value}` : ''}。`
      : '本轮以当前方向题为主，不绑定简历或项目经历。'
  }
})

const activeContextSource = computed(
  () => currentQuestion.value?.contextSource || detail.value?.contextSource || draftContextSource.value
)
const activeContextSummary = computed(() => activeContextSource.value?.summary || '')
const copilotProviderItems = computed(() =>
  providerConfigs.value.filter((item) => ['asr', 'search', 'voiceprint'].includes(item.scope))
)
const missingCopilotProviders = computed(() =>
  copilotProviderItems.value.filter((item) => isProviderStatusMissing(item.status))
)

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

const experienceLabel = (value?: string) => {
  return experienceLevels.find((item) => item.value === value)?.label || '未设置'
}

const difficultyText = (value?: string) => {
  if (value === 'easy') return '建议难度：简单'
  if (value === 'hard') return '建议难度：困难'
  if (value === 'medium') return '建议难度：中等'
  return ''
}

const interviewContextLabel = (context?: ContextSource | null, includeResumeProject?: boolean) => {
  if (context?.type === 'project') return '结合项目'
  if (context?.type === 'resume') return '结合简历'
  if (includeResumeProject) return '结合项目'
  return '不带简历'
}

const getCountdownSeconds = () => {
  const totalMinutes = currentQuestion.value?.durationMinutes || durationMinutes.value || 20
  const totalQuestions = currentQuestion.value?.questionCount || questionCount.value || 3
  return Math.max(90, Math.round((totalMinutes * 60) / totalQuestions))
}

const countdown = ref(getCountdownSeconds())
let countdownTimer: ReturnType<typeof setInterval> | null = null
let copilotRealtimeSocket: WebSocket | null = null
let recordingReviewPollingTimer: ReturnType<typeof setTimeout> | null = null

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

const closeCopilotRealtimeSocket = () => {
  if (copilotRealtimeSocket) {
    copilotRealtimeSocket.close()
    copilotRealtimeSocket = null
  }
}

const stopRecordingReviewPolling = () => {
  if (recordingReviewPollingTimer) {
    clearTimeout(recordingReviewPollingTimer)
    recordingReviewPollingTimer = null
  }
}

onBeforeUnmount(() => {
  stopCountdown()
  closeCopilotRealtimeSocket()
  stopRecordingReviewPolling()
})

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
const copilotRealtimeConnectionLabel = computed(() => {
  if (copilotRealtimeSocketState.value === 'connecting') return '连接中'
  if (copilotRealtimeSocketState.value === 'connected') return '已连接'
  if (copilotRealtimeSocketState.value === 'disconnected') return '已断开'
  if (copilotRealtimeSocketState.value === 'error') return '连接失败'
  return '未连接'
})
const recordingReviewPending = computed(() => isRecordingReviewPendingStatus(recordingReviewSession.value?.status))
const recordingReviewAgentLink = computed(() => {
  if (!recordingReviewSession.value) return '/agent'
  return buildAgentWorkbenchLocation({
    agentType: 'recording_review',
    triggerSource: 'recording_review',
    contextRefs: [`interview:recording-review:${recordingReviewSession.value.id}`, 'analytics:profile', 'study-plan:active'],
    userPrompt: '把这次录音复盘的薄弱点转成下一轮训练动作。'
  })
})
const copilotRealtimeAgentLink = computed(() => {
  if (!copilotRealtimeSession.value) return '/agent'
  return buildAgentWorkbenchLocation({
    agentType: 'interview_review',
    triggerSource: 'interview_live',
    contextRefs: [`interview:copilot-realtime:${copilotRealtimeSession.value.id}`, 'analytics:profile', 'study-plan:active'],
    userPrompt: '把这次实时面试的现场追问和卡壳点转成面后复盘与下一轮训练动作。'
  })
})

const handleStart = async (reanswerQuestionId?: number) => {
  if (interviewContextPath.value !== 'general' && !selectedResumeId.value) {
    ElMessage.warning('请选择一份简历后开始面试')
    return
  }
  if (interviewContextPath.value === 'project' && !selectedProjectId.value) {
    ElMessage.warning('请选择一个项目后开始面试。')
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
    ElMessage.success(isVoice ? '语音模拟面试已开始，请点击录音按钮作答' : '模拟面试已开始，请回答第一题')
  } catch {
    ElMessage.error('无法开始这轮面试，请确认当前方向下有可用题目。')
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
    ElMessage.error(error?.message || '这道题提交失败，请检查答案后重试。')
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
    ElMessage.error('语音答案提交失败，请重新录制后重试。')
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

const loadAllHistory = async () => {
  allHistoryLoading.value = true
  try {
    const res = await fetchInterviewHistoryApi(
      historyFilterDirection.value || undefined,
      allHistoryPage.value,
      allHistoryPageSize.value
    )
    allHistoryItems.value = res.data.records
    allHistoryTotal.value = res.data.total
    allHistoryTotalPages.value = res.data.totalPages
  } catch {
    allHistoryItems.value = []
    allHistoryTotal.value = 0
    allHistoryTotalPages.value = 0
    ElMessage.error(ERROR_COPY.interviewHistoryLoadFailed)
  } finally {
    allHistoryLoading.value = false
  }
}

watch(historyFilterDirection, () => {
  allHistoryPage.value = 1
  void loadAllHistory()
})

const loadResumes = async () => {
  loadingResumes.value = true
  try {
    const response = await fetchResumeListApi()
    resumes.value = response.data
    if (!jobPrepResumeId.value && response.data[0]) {
      jobPrepResumeId.value = response.data[0].id
    }
  } catch {
    resumes.value = []
  } finally {
    loadingResumes.value = false
  }
}

const loadApplications = async () => {
  loadingApplications.value = true
  try {
    const response = await fetchApplicationBoardApi()
    applications.value = response.data
  } catch {
    applications.value = []
  } finally {
    loadingApplications.value = false
  }
}

const loadProviderConfigs = async () => {
  try {
    const response = await fetchProviderConfigsApi()
    providerConfigs.value = response.data
  } catch {
    providerConfigs.value = []
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
    ElMessage.error(ERROR_COPY.interviewNextQuestionLoadFailed)
  }
}

const handleFinish = async () => {
  if (!currentQuestion.value) return

  try {
    const response = await interviewDetailApi(currentQuestion.value.sessionId)
    detail.value = response.data
    phase.value = 'finished'
  } catch {
    ElMessage.error(ERROR_COPY.interviewSummaryLoadFailed)
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

const handleGenerateJobPrep = async () => {
  if (!jobPrepJdText.value.trim() && !selectedJobPrepApplicationId.value) {
    ElMessage.warning('请先粘贴岗位 JD，或者选择一条已有投递。')
    return
  }
  jobPrepLoading.value = true
  try {
    const response = await createJobPrepSessionApi({
      applicationId: selectedJobPrepApplicationId.value || undefined,
      resumeId: jobPrepResumeId.value || undefined,
      company: jobPrepCompany.value.trim() || undefined,
      jobTitle: jobPrepJobTitle.value.trim() || undefined,
      jdText: jobPrepJdText.value.trim() || undefined
    })
    jobPrepSession.value = response.data
    ElMessage.success('JD 备面结果已生成。')
  } catch (error: any) {
    ElMessage.error(error?.message || 'JD 备面生成失败，请检查 JD 或简历后重试。')
  } finally {
    jobPrepLoading.value = false
  }
}

const syncJobPrepToCopilot = () => {
  if (!jobPrepSession.value) return
  linkedCopilotJobPrepId.value = jobPrepSession.value.id
  selectedJobPrepApplicationId.value = jobPrepSession.value.applicationId || selectedJobPrepApplicationId.value
  jobPrepResumeId.value = jobPrepSession.value.resumeFileId || jobPrepResumeId.value
  jobPrepCompany.value = jobPrepSession.value.company || jobPrepCompany.value
  jobPrepJobTitle.value = jobPrepSession.value.jobTitle || jobPrepJobTitle.value
  jobPrepJdText.value = jobPrepSession.value.jdText || jobPrepJdText.value
}

const applyJobPrepToInterview = () => {
  if (!jobPrepSession.value) return
  const matched = jobPrepSession.value.matchedKeywords.slice(0, 3).join(', ')
  const missing = jobPrepSession.value.missingKeywords.slice(0, 2).join(', ')
  techStack.value = [matched, missing].filter(Boolean).join(', ') || techStack.value
  jobRole.value = jobPrepSession.value.jobTitle || jobRole.value
  if (jobPrepSession.value.resumeFileId) {
    selectedResumeId.value = jobPrepSession.value.resumeFileId
    interviewContextPath.value = 'resume'
  }
  ElMessage.success('已把 JD 备面结果带入当前模拟面试配置。')
}

const handleGenerateCopilotPrep = async (useJobPrep = false) => {
  if (!useJobPrep && !jobPrepJdText.value.trim() && !selectedJobPrepApplicationId.value && !jobPrepResumeId.value) {
    ElMessage.warning('请先提供 JD、已有投递或简历上下文，再生成 Copilot Prep。')
    return
  }
  if (useJobPrep && !jobPrepSession.value) {
    ElMessage.warning('请先生成一份 JD 备面结果。')
    return
  }
  if (useJobPrep) {
    syncJobPrepToCopilot()
  }
  copilotPrepLoading.value = true
  try {
    const response = await createCopilotPrepSessionApi({
      applicationId: selectedJobPrepApplicationId.value || undefined,
      resumeId: jobPrepResumeId.value || undefined,
      jobPrepSessionId: useJobPrep ? jobPrepSession.value?.id : linkedCopilotJobPrepId.value || undefined,
      company: jobPrepCompany.value.trim() || undefined,
      jobTitle: jobPrepJobTitle.value.trim() || undefined,
      jdText: jobPrepJdText.value.trim() || undefined,
      notes: copilotPrepNotes.value.trim() || undefined
    })
    copilotPrepSession.value = response.data
    ElMessage.success('Copilot Prep 已生成。')
  } catch (error: any) {
    ElMessage.error(error?.message || 'Copilot Prep 生成失败，请检查当前上下文后重试。')
  } finally {
    copilotPrepLoading.value = false
  }
}

const hydrateRealtimeSession = async (sessionId: string) => {
  const response = await fetchCopilotRealtimeSessionApi(sessionId)
  copilotRealtimeSession.value = response.data
}

const handleCreateCopilotRealtimeSession = async () => {
  if (!copilotPrepSession.value) {
    ElMessage.warning('请先生成一份 Copilot Prep。')
    return
  }
  copilotRealtimeLoading.value = true
  try {
    const response = await createCopilotRealtimeSessionApi({
      copilotPrepSessionId: copilotPrepSession.value.id,
      openingNote: copilotPrepNotes.value.trim() || undefined
    })
    copilotRealtimeSession.value = response.data
    copilotRealtimeSocketState.value = 'idle'
    ElMessage.success('实时 Copilot 会话已创建。')
  } catch (error: any) {
    ElMessage.error(error?.message || '实时会话创建失败，请稍后重试。')
  } finally {
    copilotRealtimeLoading.value = false
  }
}

const handleConnectCopilotRealtime = async () => {
  if (!copilotRealtimeSession.value) {
    ElMessage.warning('请先创建实时会话。')
    return
  }
  closeCopilotRealtimeSocket()
  copilotRealtimeConnecting.value = true
  copilotRealtimeSocketState.value = 'connecting'
  try {
    const socket = new WebSocket(buildCopilotRealtimeWebSocketUrl(copilotRealtimeSession.value.id))
    copilotRealtimeSocket = socket
    socket.onopen = () => {
      copilotRealtimeConnecting.value = false
      copilotRealtimeSocketState.value = 'connected'
    }
    socket.onmessage = (event) => {
      try {
        const parsed = JSON.parse(String(event.data || '{}'))
        if (parsed.type === 'snapshot' && parsed.session) {
          copilotRealtimeSession.value = parsed.session as CopilotRealtimeSession
          return
        }
        if (parsed.type === 'error' && parsed.message) {
          ElMessage.error(parsed.message)
        }
      } catch {
        // Ignore malformed skeleton events for now.
      }
    }
    socket.onerror = () => {
      copilotRealtimeConnecting.value = false
      copilotRealtimeSocketState.value = 'error'
    }
    socket.onclose = () => {
      copilotRealtimeConnecting.value = false
      if (copilotRealtimeSocketState.value !== 'error') {
        copilotRealtimeSocketState.value = 'disconnected'
      }
      copilotRealtimeSocket = null
      if (copilotRealtimeSession.value) {
        void hydrateRealtimeSession(copilotRealtimeSession.value.id).catch(() => undefined)
      }
    }
  } catch (error: any) {
    copilotRealtimeConnecting.value = false
    copilotRealtimeSocketState.value = 'error'
    ElMessage.error(error?.message || '实时连接初始化失败。')
  }
}

const handleDisconnectCopilotRealtime = () => {
  if (!copilotRealtimeSocket) return
  copilotRealtimeSocketState.value = 'disconnected'
  closeCopilotRealtimeSocket()
}

const handleSendCopilotRealtimeNote = () => {
  if (!copilotRealtimeSocket || copilotRealtimeSocket.readyState !== WebSocket.OPEN || !copilotRealtimeNote.value.trim()) {
    return
  }
  copilotRealtimeSocket.send(JSON.stringify({ type: 'note', note: copilotRealtimeNote.value.trim() }))
  copilotRealtimeNote.value = ''
}

const handleCompleteCopilotRealtime = () => {
  if (!copilotRealtimeSocket || copilotRealtimeSocket.readyState !== WebSocket.OPEN) {
    return
  }
  copilotRealtimeSocket.send(JSON.stringify({ type: 'complete', summary: '当前实时阶段已结束，转入面后复盘。' }))
}

const applyCopilotPrepToInterview = () => {
  if (!copilotPrepSession.value) return
  jobRole.value = copilotPrepSession.value.jobTitle || jobRole.value
  if (copilotPrepSession.value.resumeFileId) {
    selectedResumeId.value = copilotPrepSession.value.resumeFileId
    interviewContextPath.value = 'resume'
  }
  const cue = copilotPrepSession.value.liveCues?.[0]
  if (cue) {
    ElMessage.success(`已带入会前准备。提示：${cue}`)
    return
  }
  ElMessage.success('已把 Copilot Prep 带入当前模拟面试配置。')
}

const handleRecordingFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  recordingReviewFile.value = target.files?.[0] || null
}

const handleCreateRecordingReview = async () => {
  if (!voiceAvailable.value) {
    ElMessage.warning('请先配置 ASR provider，再使用录音复盘。')
    return
  }
  if (!recordingReviewFile.value) {
    ElMessage.warning('请先选择一段录音文件。')
    return
  }
  recordingReviewLoading.value = true
  try {
    const response = await createRecordingReviewApi({
      direction: direction.value.trim() || undefined,
      jobRole: jobRole.value.trim() || undefined,
      notes: recordingReviewNotes.value.trim() || undefined,
      audioFile: recordingReviewFile.value
    })
    recordingReviewSession.value = response.data
    if (isRecordingReviewPendingStatus(response.data.status)) {
      scheduleRecordingReviewPoll(response.data.id)
      ElMessage.success('录音已上传，正在后台转写。')
    } else {
      ElMessage.success('录音复盘结果已生成。')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '录音复盘生成失败，请检查文件后重试。')
  } finally {
    recordingReviewLoading.value = false
  }
}

const scheduleRecordingReviewPoll = (sessionId: string, delay = 2200) => {
  stopRecordingReviewPolling()
  recordingReviewPollingTimer = setTimeout(async () => {
    try {
      const previousStatus = recordingReviewSession.value?.status
      const response = await fetchRecordingReviewApi(sessionId)
      recordingReviewSession.value = response.data
      if (isRecordingReviewPendingStatus(response.data.status)) {
        scheduleRecordingReviewPoll(sessionId)
        return
      }
      if (previousStatus !== response.data.status && response.data.status === 'ready') {
        ElMessage.success('录音复盘已生成。')
      }
      if (previousStatus !== response.data.status && response.data.status === 'failed') {
        ElMessage.error(response.data.statusMessage || '录音复盘失败，请重新上传更清晰的录音。')
      }
    } catch {
      scheduleRecordingReviewPoll(sessionId, 3200)
    }
  }, delay)
}

const formatSegmentOffset = (ms?: number) => {
  const safe = Math.max(0, Math.floor((ms || 0) / 1000))
  const minutes = Math.floor(safe / 60)
  const seconds = safe % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

const signalLabel = (signalType?: string) => {
  if (signalType === 'example') return '项目例子'
  if (signalType === 'structure') return '结构表达'
  if (signalType === 'reasoning') return '原因 / 取舍'
  return '普通片段'
}

const isRecordingReviewPendingStatus = (status?: string) => ['processing', 'transcribing', 'analyzing'].includes(status || '')

const recordingReviewStatusLabel = (status?: string) => {
  if (status === 'processing') return '等待转写'
  if (status === 'transcribing') return '转写中'
  if (status === 'analyzing') return '复盘生成中'
  if (status === 'failed') return '处理失败'
  if (status === 'ready') return '已完成'
  return status || '未知'
}

const realtimeStatusText = (status?: string) => {
  if (status === 'awaiting_connection') return '待连接'
  if (status === 'live') return '实时中'
  if (status === 'disconnected') return '已断开'
  if (status === 'completed') return '已结束'
  return status || '未知'
}

const realtimeEventLabel = (eventType?: string) => {
  if (eventType === 'session_created') return '会话创建'
  if (eventType === 'opening_note') return '会前备注'
  if (eventType === 'connection_established') return '连接建立'
  if (eventType === 'provider_degraded') return '降级提醒'
  if (eventType === 'runtime_note') return '运行中备注'
  if (eventType === 'session_completed') return '阶段结束'
  if (eventType === 'connection_closed') return '连接关闭'
  return eventType || '事件'
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
  void loadAllHistory()
  void loadResumes()
  void loadApplications()
  void loadProviderConfigs()
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
      recommendedInterview.value = rec ?? null
      if (!seededQuestionTitle.value && rec && rec.direction && directions.some((d) => d.name === rec.direction)) {
        direction.value = rec.direction
      }
      if (rec && rec.questionCount) {
        questionCount.value = rec.questionCount
      }
    })
    .catch(() => {
      recommendedInterview.value = null
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

watch(selectedJobPrepApplicationId, (applicationId) => {
  const item = applications.value.find((application) => application.id === applicationId)
  if (!item) return
  jobPrepCompany.value = item.company || ''
  jobPrepJobTitle.value = item.jobTitle || 'Java 后端开发'
  jobPrepJdText.value = item.jdText || ''
  if (item.resumeFileId) {
    jobPrepResumeId.value = item.resumeFileId
  }
})
</script>

<style scoped>
.interview-cockpit {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.interview-setup-bar {
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.12), transparent 30%),
    var(--bc-surface-card);
}

.interview-history-shell {
  margin-top: 12px;
  border: 1px solid var(--bc-border-subtle);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(var(--bc-accent-rgb), 0.03));
}

.interview-job-prep-shell {
  margin-top: 12px;
  border: 1px solid var(--bc-border-subtle);
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.08), transparent 36%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(var(--bc-cyan-rgb), 0.03));
}

.interview-copilot-prep-shell {
  margin-top: 12px;
  border: 1px solid var(--bc-border-subtle);
  background:
    radial-gradient(circle at top center, rgba(var(--bc-cyan-rgb), 0.08), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(var(--bc-accent-rgb), 0.03));
}

.interview-recording-review-shell {
  margin-top: 12px;
  border: 1px solid var(--bc-border-subtle);
  background:
    radial-gradient(circle at top right, rgba(var(--bc-coral-rgb), 0.08), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(var(--bc-amber-rgb), 0.03));
}

.interview-setup-bar__head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.interview-setup-bar__head-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.interview-setup-bar__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  margin-top: 14px;
  font-size: 12px;
}

.interview-mode-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px;
  border-radius: 999px;
  background: var(--panel-muted);
}

.interview-mode-switch__button {
  min-height: 38px;
  border-radius: 999px;
  padding: 0 14px;
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--bc-ink-secondary);
  transition: background var(--motion-fast) var(--ease-hard), color var(--motion-fast) var(--ease-hard);
}

.interview-mode-switch__button-active {
  background: rgba(var(--bc-accent-rgb), 0.12);
  color: var(--bc-ink);
}

.interview-setup-grid {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.job-prep-grid {
  display: grid;
  gap: 16px;
}

.copilot-prep-grid {
  display: grid;
  gap: 16px;
}

.recording-review-grid {
  display: grid;
  gap: 16px;
}

.copilot-prep-provider-alert {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid rgba(var(--bc-amber-rgb), 0.22);
  background: rgba(var(--bc-amber-rgb), 0.08);
  padding: 16px;
}

.recording-review-provider-alert {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid rgba(var(--bc-coral-rgb), 0.22);
  background: rgba(var(--bc-coral-rgb), 0.08);
  padding: 16px;
}

.recording-review-upload {
  display: grid;
  gap: 6px;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px dashed rgba(var(--bc-accent-rgb), 0.28);
  background: var(--panel-bg);
  padding: 16px;
  cursor: pointer;
}

.recording-review-upload__title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.recording-review-upload__hint {
  font-size: 0.82rem;
  color: var(--bc-ink-secondary);
}

.recording-review-result-shell {
  min-height: 100%;
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 18px;
}

.recording-review-status-shell {
  display: flex;
  min-height: 280px;
  flex-direction: column;
  justify-content: center;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.18);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.06), rgba(255, 255, 255, 0.92));
  padding: 24px;
}

.recording-review-status-shell--failed {
  border-color: rgba(var(--bc-coral-rgb), 0.18);
  background: linear-gradient(180deg, rgba(var(--bc-coral-rgb), 0.06), rgba(255, 255, 255, 0.94));
}

.recording-review-status-pill {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  border-radius: 999px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.22);
  background: rgba(var(--bc-accent-rgb), 0.08);
  padding: 4px 10px;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.recording-review-status-pill--failed {
  border-color: rgba(var(--bc-coral-rgb), 0.22);
  background: rgba(var(--bc-coral-rgb), 0.08);
}

.recording-review-summary-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid rgba(var(--bc-coral-rgb), 0.16);
  background:
    linear-gradient(180deg, rgba(var(--bc-coral-rgb), 0.08), transparent 58%),
    var(--panel-bg);
  padding: 18px;
}

.recording-review-score {
  display: inline-flex;
  min-width: 88px;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.recording-review-score__label {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--bc-ink-tertiary);
}

.recording-review-score__value {
  font-family: theme('fontFamily.mono');
  font-size: clamp(2rem, 4vw, 2.8rem);
  font-weight: 700;
  line-height: 1;
  color: var(--bc-ink);
}

.recording-review-panel {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-bg);
  padding: 16px;
}

.recording-review-panel__title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.recording-review-list {
  display: grid;
  gap: 10px;
  padding-left: 18px;
  color: var(--bc-ink-secondary);
}

.recording-review-list li {
  line-height: 1.7;
}

.recording-review-segment {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-bg);
  padding: 14px;
}

.job-prep-result-shell {
  min-height: 100%;
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 18px;
}

.copilot-prep-result-shell {
  min-height: 100%;
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 18px;
}

.job-prep-summary-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid rgba(var(--bc-accent-rgb), 0.18);
  background:
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.08), transparent 58%),
    var(--panel-bg);
  padding: 18px;
}

.copilot-prep-summary-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid rgba(var(--bc-cyan-rgb), 0.18);
  background:
    linear-gradient(180deg, rgba(var(--bc-cyan-rgb), 0.08), transparent 58%),
    var(--panel-bg);
  padding: 18px;
}

.job-prep-score {
  display: inline-flex;
  min-width: 88px;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.job-prep-score__label {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--bc-ink-tertiary);
}

.job-prep-score__value {
  font-family: theme('fontFamily.mono');
  font-size: clamp(2rem, 4vw, 2.8rem);
  font-weight: 700;
  line-height: 1;
  color: var(--bc-ink);
}

.job-prep-stat-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-bg);
  padding: 14px 16px;
}

.job-prep-stat-card--risk {
  border-color: rgba(var(--bc-coral-rgb), 0.28);
}

.job-prep-stat-card__label {
  font-size: 0.74rem;
  color: var(--bc-ink-secondary);
}

.job-prep-stat-card__value {
  margin-top: 10px;
  font-family: theme('fontFamily.mono');
  font-size: 2rem;
  font-weight: 700;
  line-height: 1;
  color: var(--bc-ink);
}

.job-prep-panel {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-bg);
  padding: 16px;
}

.copilot-prep-panel {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-bg);
  padding: 16px;
}

.job-prep-panel__title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.copilot-prep-panel__title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.job-prep-chip {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  border-radius: 999px;
  border: 1px solid rgba(var(--bc-cyan-rgb), 0.24);
  background: rgba(var(--bc-cyan-rgb), 0.1);
  padding: 0 11px;
  font-size: 0.74rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.job-prep-chip--risk {
  border-color: rgba(var(--bc-coral-rgb), 0.24);
  background: rgba(var(--bc-coral-rgb), 0.1);
}

.job-prep-list {
  display: grid;
  gap: 10px;
  padding-left: 18px;
  color: var(--bc-ink-secondary);
}

.copilot-prep-list {
  display: grid;
  gap: 10px;
  padding-left: 18px;
  color: var(--bc-ink-secondary);
}

.job-prep-list li {
  line-height: 1.7;
}

.copilot-prep-list li {
  line-height: 1.7;
}

.detail-pill-risk {
  border-color: rgba(var(--bc-coral-rgb), 0.24);
  background: rgba(var(--bc-coral-rgb), 0.1);
  color: var(--bc-ink);
}

.copilot-provider-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-bg);
  padding: 14px 16px;
}

.copilot-provider-card--ready {
  border-color: rgba(var(--bc-lime-rgb), 0.24);
}

.copilot-provider-card--saved {
  border-color: rgba(var(--bc-accent-rgb), 0.24);
}

.copilot-provider-card--incomplete,
.copilot-provider-card--missing {
  border-color: rgba(var(--bc-coral-rgb), 0.24);
}

.copilot-provider-card__label {
  font-size: 0.74rem;
  color: var(--bc-ink-secondary);
}

.copilot-provider-card__status {
  margin-top: 10px;
  font-family: theme('fontFamily.mono');
  font-size: 1.1rem;
  font-weight: 700;
  line-height: 1;
  color: var(--bc-ink);
}

.copilot-realtime-panel {
  border: 1px solid rgba(var(--bc-accent-rgb), 0.16);
}

.copilot-realtime-empty {
  margin-top: 1rem;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px dashed rgba(100, 116, 139, 0.28);
  background: rgba(248, 250, 252, 0.84);
  padding: 1rem;
}

.copilot-realtime-status-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid rgba(100, 116, 139, 0.18);
  background: rgba(255, 255, 255, 0.92);
  padding: 1rem;
}

.copilot-realtime-status-card__label {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--bc-ink-tertiary);
}

.copilot-realtime-status-card__value {
  margin-top: 0.5rem;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.copilot-realtime-events {
  display: grid;
  gap: 0.75rem;
}

.copilot-realtime-event {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid rgba(100, 116, 139, 0.18);
  background: rgba(255, 255, 255, 0.88);
  padding: 0.95rem 1rem;
}

.copilot-realtime-event__title {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--bc-ink-tertiary);
}

.interview-setup-grid__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 8px;
}

.interview-cockpit :deep(.el-textarea__inner) {
  min-height: 180px !important;
  font-size: 14px;
  line-height: 1.7;
}

.interview-session-card {
  min-height: 0;
  flex: 1;
}

.flat-field-label {
  display: block;
  font-size: 0.74rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--bc-ink-secondary);
  margin-bottom: 0.45rem;
}

.flat-field-divider {
  height: 1px;
  background: var(--bc-line);
  opacity: 0.5;
  margin: 0.25rem 0;
}

.flat-field-grid {
  padding: 0.25rem 0;
}

.interview-context-chip {
  min-height: 2.5rem;
  border-radius: 999px;
  border: 1px solid var(--bc-line);
  background: var(--panel-muted);
  color: var(--bc-ink-secondary);
  font-size: 0.88rem;
  font-weight: 600;
  padding: 0 1rem;
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

.interview-context-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--bc-border-subtle);
}

.question-spotlight {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.14);
  border-radius: 20px;
  padding: 16px 18px 14px;
  background: radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.18), transparent 34%), var(--panel-bg);
  box-shadow: none;
}

.dark .question-spotlight {
  background: radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.22), transparent 34%), var(--panel-bg);
}

.question-spotlight::after {
  content: '';
  position: absolute;
  right: -32px;
  top: -32px;
  width: 100px;
  height: 100px;
  border-radius: 999px;
  border: 1px dashed rgba(var(--bc-accent-rgb), 0.26);
  opacity: 0.6;
}

.question-spotlight__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.question-spotlight__index {
  font-family: theme('fontFamily.mono');
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--bc-ink-secondary);
}

.question-spotlight__title {
  margin-top: 10px;
  max-width: 700px;
  font-family: theme('fontFamily.display');
  font-size: clamp(1.2rem, 2.5vw, 1.8rem);
  font-weight: 600;
  line-height: 1.15;
  letter-spacing: 0;
  color: var(--bc-ink);
  text-wrap: balance;
}

.question-spotlight-compact {
  padding: 16px 18px 12px;
}

.question-spotlight__topline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.question-spotlight__main {
  min-width: 0;
  flex: 1;
}

.question-spotlight__timer {
  display: flex;
  min-width: 90px;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  text-align: right;
}

.question-spotlight__timer-label {
  color: var(--bc-ink-secondary);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.question-spotlight__timer-value {
  font-family: theme('fontFamily.mono');
  font-size: clamp(1.5rem, 2.5vw, 2rem);
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0;
  font-variant-numeric: tabular-nums;
}

.question-spotlight__progress {
  overflow: hidden;
  margin-top: 10px;
  height: 4px;
  border-radius: 999px;
  background: var(--panel-muted);
}

.question-spotlight__progress-bar {
  height: 100%;
  border-radius: inherit;
  transition: width 500ms ease;
}

@media (min-width: 900px) {
  .interview-setup-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (min-width: 1280px) {
  .job-prep-grid {
    grid-template-columns: minmax(0, 360px) minmax(0, 1fr);
  }

  .copilot-prep-grid {
    grid-template-columns: minmax(0, 360px) minmax(0, 1fr);
  }

.recording-review-grid {
    grid-template-columns: minmax(0, 360px) minmax(0, 1fr);
  }
}

.copilot-post-review-shell {
  border-radius: 20px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.16);
  background:
    radial-gradient(circle at top right, rgba(var(--bc-accent-rgb), 0.14), transparent 36%),
    var(--panel-bg);
  padding: 16px;
}

.copilot-post-review-card {
  min-width: 0;
  border-radius: 16px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 14px;
}

.copilot-post-review-card__title {
  margin: 0;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--bc-ink-secondary);
}

.scoring-scan {
  position: relative;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.14);
  background: radial-gradient(circle, rgba(var(--bc-accent-rgb), 0.18), transparent 58%), var(--panel-bg);
  box-shadow: none;
}

.scoring-scan::before {
  content: '';
  position: absolute;
  inset: 14px;
  border: 1px dashed rgba(var(--bc-accent-rgb), 0.34);
  border-radius: inherit;
}

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
  .scoring-scan::after {
    animation: none;
  }
}

@media (max-width: 768px) {
  .interview-history-shell {
    margin-top: 10px;
  }

  .question-spotlight__topline {
    flex-direction: column;
    gap: 10px;
  }

  .question-spotlight__timer {
    min-width: 0;
    align-items: flex-start;
    text-align: left;
  }
}
</style>
