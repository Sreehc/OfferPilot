<template>
  <div class="space-y-5">
    <StateView
      v-if="loading && !currentResume && !resumeList.length"
      variant="loading"
      :rows="5"
    />

    <section class="shell-section-card resume-state-card p-4 sm:p-5">
      <div class="workspace-head__top">
        <div class="workspace-head__main">
          <div class="flex flex-wrap gap-2">
            <span class="hard-chip">{{ stateChip }}</span>
            <span class="detail-pill">{{ resumeList.length }} 份简历</span>
            <span v-if="seededFocusLabel" class="detail-pill">{{ seededFocusLabel }}</span>
            <span v-if="currentResume" class="detail-pill">{{ currentResume.projects.length }} 个项目</span>
            <span v-if="currentResume" class="detail-pill">{{ flattenedRisks.length }} 条待检查提醒</span>
          </div>

          <h1 class="mt-4 workspace-title">简历助手</h1>
          <p class="workspace-summary">
            {{
              !currentResume
                ? seededWorkspaceSummary || '上传简历后，你可以修改内容、导出提纲或进入模拟面试。'
                : `${stateTitle}，${seededWorkspaceSummary || '处理待修改的重点内容。'}`
            }}
          </p>
        </div>

        <div class="workspace-actions">
          <button v-if="!currentResume" type="button" class="hard-button-primary" @click="scrollToUpload">
            上传简历开始整理
          </button>
          <button
            v-else-if="currentResume.parseStatus === 'failed'"
            type="button"
            class="hard-button-primary"
            @click="startEditing"
          >
            修正简历内容
          </button>
          <button v-else-if="!isEditing" type="button" class="hard-button-primary" @click="startEditing">
            修改这份简历
          </button>
          <button v-else type="button" class="hard-button-primary" :disabled="saving" @click="handleSave">
            保存当前修改
          </button>

          <button
            v-if="currentResume && currentResume.parseStatus === 'failed'"
            type="button"
            class="hard-button-secondary"
            :disabled="retrying"
            @click="handleRetryParse"
          >
            重新识别简历
          </button>
          <button v-if="currentResume && isEditing" type="button" class="hard-button-secondary" @click="cancelEditing">
            取消修改
          </button>
          <button
            v-if="currentResume && interviewResume"
            type="button"
            class="hard-button-secondary"
            @click="handleDownloadResume"
          >
            导出面试提纲
          </button>
          <RouterLink v-if="currentResume" :to="resumeCoachAgentLink" class="hard-button-secondary">
            交给 Agent 优化
          </RouterLink>
          <RouterLink v-if="currentResume" :to="resumeJobPrepLink" class="hard-button-secondary">去 JD 备面</RouterLink>
          <RouterLink v-if="currentResume" :to="resumeRecordingReviewLink" class="hard-button-secondary">去录音复盘</RouterLink>
          <RouterLink v-if="currentResume" :to="resumeInterviewLink" class="hard-button-secondary">去模拟面试</RouterLink>
        </div>
      </div>

      <MetricStrip :items="resumeMetrics" class="mt-4" />
    </section>

      <section class="grid gap-4 xl:grid-cols-[320px_minmax(0,1fr)]">
      <aside class="space-y-4">
        <article id="resume-upload" class="shell-section-card p-5 sm:p-6">
          <section>
            <div class="flex items-center justify-between gap-3">
              <div>
                <h3 class="text-xl font-semibold tracking-[-0.03em] text-ink">上传与选择简历</h3>
                <p class="mt-2 text-sm text-secondary">选择一份简历后，可以整理项目表达、开场和提纲。</p>
              </div>
            </div>

            <el-upload
              class="mt-5 w-full"
              drag
              :show-file-list="false"
              accept=".pdf,.doc,.docx"
              :before-upload="handleUpload"
            >
              <div class="py-5">
                <div class="text-base font-semibold text-ink">拖拽简历到这里，或点击上传</div>
                <p class="mt-2 text-sm text-secondary">支持 PDF / DOC / DOCX，单个文件不超过 10MB</p>
              </div>
            </el-upload>

            <p v-if="uploading" class="mt-3 text-sm text-secondary">正在处理简历内容，请稍候...</p>
          </section>

          <section class="resume-side-panel__section">
            <h4 class="text-lg font-semibold tracking-[-0.03em] text-ink">当前简历</h4>
            <div v-if="resumeList.length" class="mt-4 space-y-3">
              <button
                v-for="item in resumeList"
                :key="item.id"
                type="button"
                class="resume-list-card w-full text-left"
                :class="selectedResumeId === item.id ? 'resume-list-card-active' : ''"
                @click="handleSelectResume(item.id)"
              >
                <div class="flex items-center justify-between gap-3">
                  <div class="min-w-0">
                    <div class="text-sm font-semibold text-ink">{{ item.title }}</div>
                    <div class="mt-1 text-xs text-tertiary">
                      {{ item.fileType.toUpperCase() }} · {{ formatDateTime(item.updateTime) }}
                    </div>
                  </div>
                  <span class="detail-pill">{{ parseStatusLabel(item.parseStatus, item.userFixStatus) }}</span>
                </div>
              </button>
            </div>
            <div v-else class="mt-4 rounded-2xl border border-dashed border-[var(--bc-line)] p-5 text-sm text-secondary">
              {{ EMPTY_STATE_COPY.resumeLibrary.title }}，{{ EMPTY_STATE_COPY.resumeLibrary.description }}
            </div>
          </section>
        </article>

        <article class="shell-section-card p-5 sm:p-6">
          <h3 class="text-xl font-semibold tracking-[-0.03em] text-ink">整理顺序</h3>
          <div class="mt-5 space-y-3">
            <div v-for="step in workflowSteps" :key="step.id" class="resume-step-row">
              <div class="resume-step-index">{{ step.index }}</div>
              <div class="min-w-0 flex-1">
                <div class="text-sm font-semibold text-ink">{{ step.title }}</div>
                <p class="mt-1 text-xs leading-6 text-secondary">{{ step.description }}</p>
              </div>
              <span class="resume-step-status" :class="step.status">{{ step.statusText }}</span>
            </div>
          </div>

          <div v-if="currentResume && resumeVersions.length" class="resume-side-panel__section">
            <div class="flex items-center justify-between gap-3">
              <div>
                <h4 class="text-lg font-semibold tracking-[-0.03em] text-ink">版本历史</h4>
                <p class="mt-1 text-sm text-secondary">每次保存修改会自动创建版本快照。</p>
              </div>
            </div>
            <div class="mt-4 space-y-3">
              <div v-for="ver in resumeVersions" :key="ver.id" class="resume-version-item">
                <div class="flex items-center justify-between gap-2">
                  <span class="text-sm font-semibold text-ink">v{{ ver.version }}</span>
                  <span class="text-xs text-tertiary">{{ formatDateTime(ver.createTime) }}</span>
                </div>
                <p class="mt-1 text-xs text-secondary">{{ ver.changeSummary || '简历内容更新' }}</p>
                <button
                  type="button"
                  class="mt-2 text-xs font-semibold text-accent hover:underline"
                  @click="handleRestoreVersion(ver.id)"
                >
                  回滚到此版本
                </button>
              </div>
            </div>
          </div>
        </article>
      </aside>

      <section class="space-y-4">
        <article v-if="seededFocusCard" class="shell-section-card p-5 sm:p-6">
          <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">当前进入上下文</div>
              <h3 class="mt-2 text-xl font-semibold tracking-[-0.03em] text-ink">{{ seededFocusCard.title }}</h3>
              <p class="mt-2 text-sm leading-7 text-secondary">{{ seededFocusCard.description }}</p>
            </div>
            <div class="flex flex-wrap gap-2">
              <RouterLink :to="resumeJobPrepLink" class="hard-button-secondary">带去 JD 备面</RouterLink>
              <RouterLink :to="resumeRecordingReviewLink" class="hard-button-secondary">带去录音复盘</RouterLink>
              <RouterLink :to="resumeCopilotPrepLink" class="hard-button-secondary">带去 Copilot Prep</RouterLink>
              <RouterLink :to="resumeCoachAgentLink" class="hard-button-secondary">交给 Agent 收口</RouterLink>
            </div>
          </div>
        </article>

        <article v-if="currentResume?.parseStatus === 'failed'" class="shell-section-card p-5 sm:p-6">
          <div class="rounded-2xl border border-coral/25 bg-coral/6 p-4">
            <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
              <div>
                <h3 class="text-xl font-semibold text-ink">这份简历还需要你补一手</h3>
                <p class="mt-2 text-sm leading-7 text-secondary">
                  {{ currentResume.parseError || '这份简历识别不完整，请检查并补充摘要、项目和开场内容。' }}
                </p>
              </div>
              <div class="flex flex-wrap gap-2">
                <button type="button" class="hard-button-primary" @click="startEditing">修正简历内容</button>
                <button type="button" class="hard-button-secondary" :disabled="retrying" @click="handleRetryParse">
                  重新识别
                </button>
              </div>
            </div>
          </div>
        </article>

        <article v-if="currentResume && hasAgentFollowUpDraft" class="shell-section-card p-5 sm:p-6">
          <div class="rounded-2xl border border-accent/20 bg-accent/5 p-4">
            <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
              <div>
                <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">Agent 已写回正式草稿</div>
                <h3 class="mt-2 text-xl font-semibold text-ink">简历项目追问草稿待确认</h3>
                <p class="mt-2 text-sm leading-7 text-secondary">
                  这份草稿来自最近一次简历教练审批写回。先确认项目追问、开场和修改顺序，再决定是直接进入面试训练，还是回到编辑态继续收紧表述。
                </p>
              </div>
              <div class="flex flex-wrap gap-2">
                <button type="button" class="hard-button-secondary" @click="handleCopyAgentDraft">复制草稿</button>
                <button type="button" class="hard-button-secondary" @click="startEditing">继续整理简历</button>
                <RouterLink :to="resumeRecordingReviewLink" class="hard-button-secondary">去录音复盘</RouterLink>
                <RouterLink :to="resumeCopilotPrepLink" class="hard-button-secondary">去 Copilot Prep</RouterLink>
                <RouterLink :to="resumeInterviewLink" class="hard-button-primary">去模拟面试</RouterLink>
              </div>
            </div>
          </div>

          <div class="mt-5 surface-card p-5">
            <div class="flex items-center justify-between gap-3">
              <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">Agent 追问草稿</div>
              <span class="detail-pill">待确认</span>
            </div>
            <pre class="resume-agent-draft mt-4 whitespace-pre-wrap text-sm leading-7 text-primary">{{ agentFollowUpDraftText }}</pre>
          </div>
        </article>

        <article v-if="currentResume" class="shell-section-card p-5 sm:p-6">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">修改简历内容</h3>
              <p class="mt-2 text-sm leading-7 text-secondary">完善摘要、技能和教育信息后，补充项目和开场内容。</p>
            </div>
            <button v-if="!isEditing" type="button" class="hard-button-secondary" @click="startEditing">
              修正简历内容
            </button>
          </div>

          <div v-if="isEditing" class="mt-5 grid gap-4">
            <div class="surface-card p-4">
              <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">简历标题</div>
              <el-input v-model="draft.title" class="mt-2" size="large" placeholder="例如：Java 后端开发" />
            </div>
            <div class="surface-card p-4">
              <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">简历摘要</div>
              <el-input
                v-model="draft.summary"
                class="mt-2"
                type="textarea"
                :rows="4"
                placeholder="用 2 到 3 句话讲清你的方向、技术栈和项目亮点"
              />
            </div>
            <div class="grid gap-4 md:grid-cols-2">
              <div class="surface-card p-4">
                <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">技能关键词</div>
                <el-input
                  v-model="draft.skillsText"
                  class="mt-2"
                  type="textarea"
                  :rows="3"
                  placeholder="用逗号分隔，例如：Java, Spring Boot, MySQL, Redis"
                />
              </div>
              <div class="surface-card p-4">
                <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">教育信息</div>
                <el-input
                  v-model="draft.education"
                  class="mt-2"
                  type="textarea"
                  :rows="3"
                  placeholder="补充学校、专业和学历"
                />
              </div>
            </div>
          </div>

          <div v-else class="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">简历状态</div>
              <p class="mt-2 text-sm leading-6 text-primary">
                {{ parseStatusLabel(currentResume.parseStatus, currentResume.userFixStatus) }}
              </p>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">技能标签</div>
              <div class="mt-3 flex flex-wrap gap-2">
                <span
                  v-for="skill in currentResume.skills.slice(0, 6)"
                  :key="skill"
                  class="rounded-full bg-accent/10 px-3 py-1 text-xs font-semibold text-accent"
                >
                  {{ skill }}
                </span>
              </div>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">教育信息</div>
              <p class="mt-2 text-sm leading-6 text-primary">{{ currentResume.education || '待补充' }}</p>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">最后整理</div>
              <p class="mt-2 text-sm leading-6 text-primary">
                {{ formatDateTime(currentResume.lastParsedAt || currentResume.updateTime) }}
              </p>
            </div>
            <div class="surface-card p-4 md:col-span-2 xl:col-span-4">
              <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">当前摘要</div>
              <p class="mt-2 text-sm leading-7 text-secondary">{{ currentResume.summary || '待补充' }}</p>
            </div>
          </div>
        </article>

        <!-- Resume Score Card -->
        <article v-if="currentResume && resumeScore" class="shell-section-card p-5 sm:p-6">
          <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">简历评分</h3>
          <p class="mt-2 text-sm leading-7 text-secondary">根据完整度、关键词覆盖和 ATS 兼容性三个维度综合评估。</p>

          <div class="mt-5 grid gap-4 md:grid-cols-4">
            <div class="resume-score-ring">
              <div class="resume-score-ring__value">
                <span class="resume-score-ring__number">{{ resumeScore.overallScore }}</span>
                <span class="resume-score-ring__label">综合分</span>
              </div>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">完整度</div>
              <p class="mt-2 text-lg font-semibold text-ink">{{ resumeScore.completenessScore }}%</p>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">关键词覆盖</div>
              <p class="mt-2 text-lg font-semibold text-ink">{{ resumeScore.keywordCoverage }}%</p>
            </div>
            <div class="data-slab p-4">
              <div class="text-xs uppercase tracking-[0.22em] text-tertiary">ATS 兼容性</div>
              <p class="mt-2 text-lg font-semibold text-ink">{{ resumeScore.atsCompatibility }}%</p>
            </div>
          </div>

          <div v-if="resumeScore.suggestions.length" class="mt-5 space-y-3">
            <h4 class="text-sm font-semibold uppercase tracking-[0.18em] text-tertiary">优化建议</h4>
            <div
              v-for="(suggestion, idx) in resumeScore.suggestions"
              :key="idx"
              class="resume-suggestion-item"
              :class="`resume-suggestion-${suggestion.severity}`"
            >
              <span class="resume-suggestion-badge">{{ severityLabel(suggestion.severity) }}</span>
              <span class="detail-pill">{{ suggestion.field }}</span>
              <p class="mt-1 text-sm leading-6 text-primary">{{ suggestion.message }}</p>
            </div>
          </div>
        </article>

        <article v-if="currentResume" class="shell-section-card p-5 sm:p-6">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">项目追问与表达</h3>
              <p class="mt-2 text-sm leading-7 text-secondary">
                每个项目都要能讲背景、职责、取舍和结果。你改完项目内容后，追问会跟着一起更新。
              </p>
            </div>
            <button v-if="isEditing" type="button" class="hard-button-secondary" @click="addProject">新增项目</button>
          </div>

          <div class="mt-5 space-y-4">
            <article v-for="(project, index) in editingProjects" :key="project.localId" class="resume-project-card">
              <div class="flex items-center justify-between gap-3">
                <div class="text-sm font-semibold text-ink">项目 {{ index + 1 }}</div>
                <button
                  v-if="isEditing"
                  type="button"
                  class="text-sm font-semibold text-coral"
                  @click="removeProject(project.localId)"
                >
                  删除
                </button>
              </div>

              <div v-if="isEditing" class="mt-4 grid gap-4">
                <div class="grid gap-4 md:grid-cols-2">
                  <div class="surface-card p-4">
                    <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">项目名称</div>
                    <el-input
                      v-model="project.projectName"
                      class="mt-2"
                      size="large"
                      placeholder="例如：订单中心重构"
                    />
                  </div>
                  <div class="surface-card p-4">
                    <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">你的角色</div>
                    <el-input v-model="project.roleName" class="mt-2" size="large" placeholder="例如：后端负责人" />
                  </div>
                </div>
                <div class="surface-card p-4">
                  <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">技术栈</div>
                  <el-input
                    v-model="project.techStack"
                    class="mt-2"
                    size="large"
                    placeholder="例如：Spring Boot, MySQL, Redis"
                  />
                </div>
                <div class="grid gap-4 md:grid-cols-2">
                  <div class="surface-card p-4">
                    <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">职责</div>
                    <el-input
                      v-model="project.responsibility"
                      class="mt-2"
                      type="textarea"
                      :rows="4"
                      placeholder="讲清你负责什么、做了什么权衡、解决了什么问题"
                    />
                  </div>
                  <div class="surface-card p-4">
                    <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">结果</div>
                    <el-input
                      v-model="project.achievement"
                      class="mt-2"
                      type="textarea"
                      :rows="4"
                      placeholder="尽量补性能、稳定性、效率或业务结果"
                    />
                  </div>
                </div>
                <div class="surface-card p-4">
                  <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">项目摘要</div>
                  <el-input
                    v-model="project.projectSummary"
                    class="mt-2"
                    type="textarea"
                    :rows="3"
                    placeholder="用 2 到 3 句话收住这个项目最值得讲的点"
                  />
                </div>
              </div>

              <div v-else class="mt-4">
                <div class="flex flex-wrap items-start justify-between gap-3">
                  <div class="min-w-0">
                    <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">
                      {{ project.roleName }}
                    </div>
                    <h4 class="mt-2 text-lg font-semibold text-ink">{{ project.projectName }}</h4>
                  </div>
                  <span class="detail-pill">{{ project.techStack || '技术栈待补充' }}</span>
                </div>

                <p class="mt-3 text-sm leading-7 text-secondary">{{ project.projectSummary || '待补充项目摘要' }}</p>

                <div class="mt-4 grid gap-3 md:grid-cols-2">
                  <div class="surface-card p-4">
                    <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">职责</div>
                    <p class="mt-2 text-sm leading-6 text-primary">{{ project.responsibility || '待补充职责' }}</p>
                  </div>
                  <div class="surface-card p-4">
                    <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">结果</div>
                    <p class="mt-2 text-sm leading-6 text-primary">{{ project.achievement || '待补充结果' }}</p>
                  </div>
                </div>

                <div class="mt-4 grid gap-3 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
                  <div class="surface-card p-4">
                    <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">项目追问</div>
                    <div class="mt-3 space-y-3">
                      <div
                        v-for="question in project.followUpQuestions"
                        :key="question.question"
                        class="resume-question-item"
                      >
                        <div class="text-xs font-semibold uppercase tracking-[0.18em] text-tertiary">
                          {{ question.intent }}
                        </div>
                        <p class="mt-2 text-sm leading-6 text-primary">{{ question.question }}</p>
                        <div class="mt-2">
                          <RouterLink
                            :to="buildProjectChatLink(project, question.question)"
                            class="text-xs font-semibold text-accent hover:underline"
                          >
                            单独练习
                          </RouterLink>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="surface-card p-4">
                    <div class="text-xs font-semibold uppercase tracking-[0.2em] text-tertiary">需要补的点</div>
                    <div class="mt-3 space-y-3">
                      <div
                        v-for="risk in project.riskHints"
                        :key="risk"
                        class="rounded-2xl border border-coral/20 bg-coral/5 px-4 py-3 text-sm text-secondary"
                      >
                        {{ risk }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </article>

        <article v-if="currentResume" class="shell-section-card p-5 sm:p-6">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">4. 生成面试开场</h3>
              <p class="mt-2 text-sm leading-7 text-secondary">
                这段话要帮你快速讲清方向、项目和优势。确认它能直接说出口。
              </p>
            </div>
          </div>

          <div v-if="isEditing" class="mt-5 surface-card p-4">
            <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">面试开场</div>
            <el-input
              v-model="draft.selfIntro"
              class="mt-2"
              type="textarea"
              :rows="7"
              placeholder="用一段能直接开口的版本介绍你的方向、技术栈和项目亮点"
            />
          </div>
          <div v-else class="mt-5 surface-card p-5">
            <p class="whitespace-pre-wrap text-sm leading-8 text-primary">
              {{ currentResume.selfIntro || '待补充面试开场' }}
            </p>
          </div>
        </article>

        <article v-if="currentResume && interviewResume" class="shell-section-card p-5 sm:p-6">
          <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">5. 导出面试简历</h3>
              <p class="mt-2 text-sm leading-7 text-secondary">
                检查这份提纲是否顺手，然后决定修改内容还是开始面试练习。
              </p>
            </div>
            <div class="flex flex-wrap gap-2">
              <button type="button" class="hard-button-secondary" @click="handleCopyResume">复制提纲</button>
              <button type="button" class="hard-button-secondary" @click="handleDownloadResume">导出文本</button>
              <RouterLink :to="resumeRecordingReviewLink" class="hard-button-secondary">去录音复盘</RouterLink>
              <RouterLink :to="resumeCopilotPrepLink" class="hard-button-secondary">去 Copilot Prep</RouterLink>
              <RouterLink :to="resumeInterviewLink" class="hard-button-primary">去模拟面试</RouterLink>
            </div>
          </div>

          <div class="mt-4 rounded-2xl border border-accent/20 bg-accent/5 p-4">
            <p class="text-sm leading-7 text-primary">
              <strong>面试使用提示：</strong>
              开场说明方向和亮点，按项目顺序介绍职责、取舍和结果。
              遇到追问时，确认问题后组织答案，不需要一口气讲完。
              如果某个项目回答不顺，可以回到简历助手完善内容。
            </p>
          </div>

          <div class="mt-5 grid gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]">
            <div class="space-y-4">
              <article class="surface-card p-4">
                <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">候选人定位</div>
                <div class="mt-2 text-lg font-semibold text-ink">{{ interviewResume.positioning }}</div>
                <p class="mt-3 text-sm leading-7 text-secondary">{{ interviewResume.summary }}</p>
              </article>

              <article class="surface-card p-4">
                <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">技能关键词</div>
                <div class="mt-3 flex flex-wrap gap-2">
                  <span
                    v-for="skill in interviewResume.skillKeywords"
                    :key="skill"
                    class="rounded-full bg-accent/10 px-3 py-1 text-xs font-semibold text-accent"
                  >
                    {{ skill }}
                  </span>
                </div>
              </article>

              <article class="surface-card p-4">
                <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">作答顺序</div>
                <div class="mt-3 space-y-3">
                  <div v-for="item in interviewResume.speakingChecklist" :key="item" class="resume-question-item">
                    <p class="text-sm leading-6 text-primary">{{ item }}</p>
                  </div>
                </div>
              </article>
            </div>

            <article class="surface-card p-5">
              <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">项目展开顺序</div>
              <div class="mt-4 space-y-4">
                <div
                  v-for="item in interviewResume.projectHighlights"
                  :key="`${item.projectId || item.projectName}`"
                  class="resume-highlight-card"
                >
                  <div class="flex items-start justify-between gap-3">
                    <div>
                      <div class="text-lg font-semibold text-ink">{{ item.projectName }}</div>
                      <div class="mt-1 text-xs uppercase tracking-[0.2em] text-tertiary">{{ item.roleName }}</div>
                    </div>
                  </div>
                  <p class="mt-3 text-sm leading-7 text-secondary">{{ item.talkingPoint }}</p>
                  <div class="mt-3 rounded-2xl bg-[var(--bc-surface-muted)] px-4 py-3 text-sm text-primary">
                    {{ item.result }}
                  </div>
                </div>
              </div>
            </article>
          </div>
        </article>

        <article v-if="!currentResume" class="shell-section-card p-8 text-center">
          <p class="text-lg font-semibold text-ink">{{ EMPTY_STATE_COPY.resumeWorkspace.title }}</p>
          <p class="mt-3 text-sm leading-7 text-secondary">{{ EMPTY_STATE_COPY.resumeWorkspace.description }}</p>
        </article>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter, type LocationQueryRaw, type RouteLocationRaw } from 'vue-router'
import {
  fetchInterviewResumeApi,
  fetchLatestResumeApi,
  fetchResumeDetailApi,
  fetchResumeListApi,
  fetchResumeScoreApi,
  fetchResumeVersionsApi,
  restoreResumeVersionApi,
  retryResumeParseApi,
  updateResumeApi,
  uploadResumeApi
} from '@/api/resume'
import { EMPTY_STATE_COPY, ERROR_COPY } from '@/constants/productCopy'
import { MetricStrip, StateView } from '@/components/ui'
import type {
  EditableInterviewResume,
  ResumeFileDetail,
  ResumeProjectItem,
  ResumeScoreVO,
  ResumeSummaryItem,
  ResumeVersionVO
} from '@/types/api'
import { markGuideSeenForCriticalAction } from '@/utils/guide'
import { buildAgentWorkbenchLocation } from '@/utils/agent'
import { storage } from '@/utils/storage'

interface ResumeProjectDraft {
  localId: string
  id?: string
  projectName: string
  roleName: string
  techStack: string
  responsibility: string
  achievement: string
  projectSummary: string
  followUpQuestions: ResumeProjectItem['followUpQuestions']
  riskHints: string[]
}

const loading = ref(false)
const uploading = ref(false)
const saving = ref(false)
const retrying = ref(false)
const isEditing = ref(false)
const route = useRoute()
const router = useRouter()
const resumeList = ref<ResumeSummaryItem[]>([])
const currentResume = ref<ResumeFileDetail | null>(null)
const interviewResume = ref<EditableInterviewResume | null>(null)
const resumeScore = ref<ResumeScoreVO | null>(null)
const resumeVersions = ref<ResumeVersionVO[]>([])
const selectedResumeId = ref('')
const draft = reactive({
  title: '',
  summary: '',
  skillsText: '',
  education: '',
  selfIntro: ''
})
const editingProjects = ref<ResumeProjectDraft[]>([])
const currentUser = storage.getUser()

const readSeedQueryValue = (key: 'seedTopic' | 'seedWorkflow' | 'seedNote') => {
  const value = route.query[key]
  return typeof value === 'string' ? value.trim() : ''
}

const seededTopic = computed(() => readSeedQueryValue('seedTopic'))
const seededWorkflow = computed(() => readSeedQueryValue('seedWorkflow'))
const seededNote = computed(() => readSeedQueryValue('seedNote'))

const seededFocusLabel = computed(() => {
  if (!seededTopic.value) return ''
  return seededWorkflow.value === 'resume' ? `${seededTopic.value} 简历表达` : `${seededTopic.value} 定向上下文`
})

const seededWorkspaceSummary = computed(() => {
  if (seededNote.value) return seededNote.value
  if (!seededTopic.value) return ''
  return `当前从其他工作区带入「${seededTopic.value}」上下文，优先把它写进项目表达、自我介绍和后续面试口径。`
})

const seededFocusCard = computed(() => {
  if (!seededTopic.value && !seededNote.value) return null
  return {
    title: seededTopic.value ? `围绕 ${seededTopic.value} 收紧简历表达` : '沿着当前上下文继续整理简历',
    description: seededWorkspaceSummary.value || '优先把当前薄弱点落到项目表达、自我介绍和后续备面素材里。'
  }
})

const appendSeedQuery = (query: URLSearchParams) => {
  if (seededTopic.value) {
    query.set('seedTopic', seededTopic.value)
  }
  if (seededWorkflow.value) {
    query.set('seedWorkflow', seededWorkflow.value)
  }
  if (seededNote.value) {
    query.set('seedNote', seededNote.value)
  }
  return query
}

const buildSeededAgentWorkbenchLocation = (
  prefill: Parameters<typeof buildAgentWorkbenchLocation>[0]
): RouteLocationRaw => {
  const location = buildAgentWorkbenchLocation(prefill) as {
    path: string
    query?: LocationQueryRaw
  }
  return {
    path: location.path,
    query: {
      ...(location.query || {}),
      ...(seededTopic.value ? { seedTopic: seededTopic.value } : {}),
      ...(seededWorkflow.value ? { seedWorkflow: seededWorkflow.value } : {}),
      ...(seededNote.value ? { seedNote: seededNote.value } : {})
    }
  }
}

const buildProjectChatLink = (
  project: Pick<ResumeProjectItem, 'projectName'> & { id?: string },
  question: string
): RouteLocationRaw => ({
  path: '/chat',
  query: {
    q: question,
    context: project.projectName,
    resumeId: currentResume.value?.id || undefined,
    resumeTitle: currentResume.value?.title || undefined,
    projectId: project.id || undefined,
    projectName: project.projectName,
    ...(seededTopic.value ? { seedTopic: seededTopic.value } : {}),
    ...(seededWorkflow.value ? { seedWorkflow: seededWorkflow.value } : {}),
    ...(seededNote.value ? { seedNote: seededNote.value } : {})
  }
})

const flattenedRisks = computed(() => {
  if (!currentResume.value) return []
  return [...new Set(currentResume.value.projects.flatMap((item) => item.riskHints || []))]
})

const stateChip = computed(() => {
  if (!currentResume.value) return '上传简历'
  if (currentResume.value.parseStatus === 'failed') return '修正简历内容'
  if (isEditing.value) return '修改简历内容'
  return '检查项目追问'
})

const stateTitle = computed(() => {
  if (!currentResume.value) return '从简历开始整理项目表达'
  if (currentResume.value.parseStatus === 'failed') return currentResume.value.title
  if (isEditing.value) return '把这份简历整理成能直接面试的版本'
  return currentResume.value.title
})

const nextActionText = computed(() => {
  if (!currentResume.value) return '上传简历'
  if (currentResume.value.parseStatus === 'failed') return '修正简历内容'
  if (currentResume.value.userFixStatus === 'agent_draft') return '确认 Agent 追问草稿'
  if (!currentResume.value.projects.length) return '补项目经历'
  if (interviewResume.value) {
    return flattenedRisks.value.length ? '补录音复盘' : '进入 Copilot Prep'
  }
  return '确认开场和面试提纲'
})
const resumeMetrics = computed(() => [
  {
    label: currentResume.value ? '当前状态' : '主目标',
    value: currentResume.value
      ? parseStatusLabel(currentResume.value.parseStatus, currentResume.value.userFixStatus)
      : '上传简历',
    hint: currentResume.value ? '解析与整理状态' : '先建立可训练素材',
    tone: currentResume.value?.parseStatus === 'failed' ? 'danger' as const : 'accent' as const
  },
  {
    label: currentResume.value ? '下一步' : '当前操作',
    value: nextActionText.value,
    hint: seededFocusLabel.value || '保持简历到面试的闭环'
  },
  {
    label: currentResume.value ? '最后更新' : '支持格式',
    value: currentResume.value ? formatDateTime(currentResume.value.updateTime) : 'PDF / DOC / DOCX',
    hint: currentResume.value ? `${currentResume.value.projects.length} 个项目` : '单文件不超过 10MB'
  }
])
const hasAgentFollowUpDraft = computed(() => Boolean(
  currentResume.value?.interviewResumeText?.trim() && currentResume.value?.userFixStatus === 'agent_draft'
))
const agentFollowUpDraftText = computed(() => currentResume.value?.interviewResumeText?.trim() || '')
const resumeCoachAgentLink = computed(() => {
  const contextRefs = ['analytics:profile']
  if (currentResume.value?.id) {
    contextRefs.unshift(`resume:${currentResume.value.id}`)
  } else {
    contextRefs.unshift('resume:latest')
  }
  return buildSeededAgentWorkbenchLocation({
    agentType: 'resume_coach',
    triggerSource: 'resume',
    contextRefs: seededTopic.value ? [...contextRefs, `analytics:topic-name:${seededTopic.value}`] : contextRefs,
    userPrompt: currentResume.value?.parseStatus === 'failed'
      ? '先修正这份简历的缺失内容，再整理成可用于面试和投递的版本。'
      : seededTopic.value
        ? `围绕「${seededTopic.value}」补强简历表达、项目追问和面试提纲，生成下一轮简历优化动作。`
        : '结合当前简历内容、项目追问和面试提纲，生成下一轮简历优化动作。'
  })
})
const resumeInterviewLink = computed(() => {
  if (!currentResume.value?.id) {
    const query = appendSeedQuery(new URLSearchParams({
      workspace: 'mock-interview'
    }))
    return `/interview?${query.toString()}`
  }
  const query = appendSeedQuery(new URLSearchParams({
    workspace: 'mock-interview',
    resumeId: String(currentResume.value.id)
  }))
  return `/interview?${query.toString()}`
})
const resumeJobPrepLink = computed(() => {
  if (!currentResume.value?.id) {
    const query = appendSeedQuery(new URLSearchParams({
      workspace: 'job-prep'
    }))
    return `/interview?${query.toString()}`
  }
  const query = appendSeedQuery(new URLSearchParams({
    workspace: 'job-prep',
    resumeId: String(currentResume.value.id)
  }))
  return `/interview?${query.toString()}`
})
const resumeRecordingReviewLink = computed(() => {
  const query = appendSeedQuery(new URLSearchParams({
    workspace: 'recording-review'
  }))
  if (currentResume.value?.id) {
    query.set('resumeId', String(currentResume.value.id))
  }
  return `/interview?${query.toString()}`
})
const resumeCopilotPrepLink = computed(() => {
  if (!currentResume.value?.id) {
    const query = appendSeedQuery(new URLSearchParams({
      workspace: 'copilot-prep'
    }))
    return `/interview?${query.toString()}`
  }
  const query = appendSeedQuery(new URLSearchParams({
    workspace: 'copilot-prep',
    resumeId: String(currentResume.value.id)
  }))
  return `/interview?${query.toString()}`
})

const readRouteResumeId = () => {
  if (typeof route.query.resumeId !== 'string') return ''
  return route.query.resumeId.trim()
}

const workflowSteps = computed(() => {
  const hasResume = Boolean(currentResume.value)
  const parseReady = currentResume.value?.parseStatus === 'parsed'
  const hasProjects = Boolean(currentResume.value?.projects.length)
  const hasIntro = Boolean(currentResume.value?.selfIntro)
  const hasInterviewResume = Boolean(interviewResume.value?.projectHighlights.length)
  return [
    {
      id: 'upload',
      index: '01',
      title: '上传简历',
      description: '选定一份要整理的简历。',
      status: hasResume ? 'done' : 'pending',
      statusText: hasResume ? '已上传' : '待开始'
    },
    {
      id: 'parse',
      index: '02',
      title: '检查简历内容',
      description: '确认摘要、技能和教育信息是否可用。',
      status: parseReady ? 'done' : hasResume ? 'active' : 'pending',
      statusText: parseReady ? '可用' : hasResume ? '待修正' : '待开始'
    },
    {
      id: 'project',
      index: '03',
      title: '检查项目追问',
      description: '确保每个项目都能讲职责、取舍和结果。',
      status: hasProjects ? 'done' : hasResume ? 'active' : 'pending',
      statusText: hasProjects ? '可查看' : hasResume ? '待补充' : '待开始'
    },
    {
      id: 'intro',
      index: '04',
      title: '生成面试开场',
      description: '把你的方向和项目亮点讲顺。',
      status: hasIntro ? 'done' : hasProjects ? 'active' : 'pending',
      statusText: hasIntro ? '可用' : hasProjects ? '待确认' : '待开始'
    },
    {
      id: 'resume',
      index: '05',
      title: '导出面试提纲',
      description: '确认项目展开顺序后，去录音复盘、Copilot Prep 或模拟面试。',
      status: hasInterviewResume ? 'done' : hasIntro ? 'active' : 'pending',
      statusText: hasInterviewResume ? '可导出' : hasIntro ? '待确认' : '待开始'
    }
  ]
})

const formatDateTime = (value?: string) => {
  if (!value) return '刚刚'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

const parseStatusLabel = (status?: string, userFixStatus?: string) => {
  if (userFixStatus === 'agent_draft') return 'Agent 草稿待确认'
  if (userFixStatus === 'updated') return '已手动修正'
  if (status === 'failed') return '识别失败'
  if (status === 'parsed') return '识别完成'
  if (status === 'pending') return '识别中'
  return '待处理'
}

const buildProjectDraft = (project: ResumeProjectItem): ResumeProjectDraft => ({
  localId: project.id || `${Date.now()}-${Math.random()}`,
  id: project.id,
  projectName: project.projectName,
  roleName: project.roleName,
  techStack: project.techStack,
  responsibility: project.responsibility,
  achievement: project.achievement,
  projectSummary: project.projectSummary,
  followUpQuestions: project.followUpQuestions || [],
  riskHints: project.riskHints || []
})

const hydrateDraft = (resume: ResumeFileDetail) => {
  draft.title = resume.title || ''
  draft.summary = resume.summary || ''
  draft.skillsText = resume.skills.join(', ')
  draft.education = resume.education || ''
  draft.selfIntro = resume.selfIntro || ''
  editingProjects.value = resume.projects.length ? resume.projects.map(buildProjectDraft) : [createEmptyProject()]
}

const createEmptyProject = (): ResumeProjectDraft => ({
  localId: `${Date.now()}-${Math.random()}`,
  projectName: '',
  roleName: '',
  techStack: '',
  responsibility: '',
  achievement: '',
  projectSummary: '',
  followUpQuestions: [],
  riskHints: []
})

const startEditing = () => {
  if (!currentResume.value) return
  hydrateDraft(currentResume.value)
  isEditing.value = true
}

const cancelEditing = () => {
  if (!currentResume.value) return
  hydrateDraft(currentResume.value)
  isEditing.value = false
}

const addProject = () => {
  editingProjects.value.push(createEmptyProject())
}

const removeProject = (localId: string) => {
  editingProjects.value = editingProjects.value.filter((item) => item.localId !== localId)
  if (!editingProjects.value.length) {
    editingProjects.value = [createEmptyProject()]
  }
}

const loadInterviewResume = async (resumeId: string) => {
  const response = await fetchInterviewResumeApi(resumeId)
  interviewResume.value = response.data
}

const loadScore = async (resumeId: string) => {
  try {
    const response = await fetchResumeScoreApi(resumeId)
    resumeScore.value = response.data
  } catch {
    resumeScore.value = null
  }
}

const loadVersions = async (resumeId: string) => {
  try {
    const response = await fetchResumeVersionsApi(resumeId)
    resumeVersions.value = response.data
  } catch {
    resumeVersions.value = []
  }
}

const severityLabel = (severity: string) => {
  if (severity === 'critical') return '必须修复'
  if (severity === 'warn') return '建议优化'
  return '可选改进'
}

const handleSelectResume = async (resumeId: string) => {
  loading.value = true
  try {
    await loadResumeWorkspace(resumeId)
  } finally {
    loading.value = false
  }
}

const syncResumeRoute = async (resumeId?: string) => {
  const nextResumeId = resumeId?.trim() || undefined
  const currentResumeId = typeof route.query.resumeId === 'string' ? route.query.resumeId.trim() : undefined
  if (currentResumeId === nextResumeId) return
  const nextQuery = { ...route.query }
  if (nextResumeId) {
    nextQuery.resumeId = nextResumeId
  } else {
    delete nextQuery.resumeId
  }
  await router.replace({ query: nextQuery })
}

const loadResumeWorkspace = async (resumeId: string) => {
  selectedResumeId.value = resumeId
  const detailResponse = await fetchResumeDetailApi(resumeId)
  currentResume.value = detailResponse.data
  selectedResumeId.value = detailResponse.data.id
  hydrateDraft(detailResponse.data)
  await Promise.all([loadInterviewResume(detailResponse.data.id), loadScore(detailResponse.data.id), loadVersions(detailResponse.data.id)])
  isEditing.value = detailResponse.data.parseStatus === 'failed'
  await syncResumeRoute(detailResponse.data.id)
}

const refreshResumeList = async () => {
  const listResponse = await fetchResumeListApi()
  resumeList.value = listResponse.data
}

const loadData = async () => {
  loading.value = true
  try {
    const [listResponse, latestResponse] = await Promise.all([fetchResumeListApi(), fetchLatestResumeApi()])
    resumeList.value = listResponse.data
    const routeResumeId = readRouteResumeId()
    if (routeResumeId && listResponse.data.some((item) => item.id === routeResumeId)) {
      await loadResumeWorkspace(routeResumeId)
    } else {
      currentResume.value = latestResponse.data
      if (currentResume.value) {
        selectedResumeId.value = currentResume.value.id
        hydrateDraft(currentResume.value)
        await Promise.all([
          loadInterviewResume(currentResume.value.id),
          loadScore(currentResume.value.id),
          loadVersions(currentResume.value.id)
        ])
        isEditing.value = currentResume.value.parseStatus === 'failed'
        await syncResumeRoute(currentResume.value.id)
      } else {
        selectedResumeId.value = ''
        await syncResumeRoute()
      }
    }
  } catch {
    ElMessage.error(ERROR_COPY.resumeWorkspaceLoadFailed)
  } finally {
    loading.value = false
  }
}

const normalizeSkills = (raw: string) => {
  return raw
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

const handleSave = async () => {
  if (!currentResume.value) return
  saving.value = true
  try {
    const response = await updateResumeApi(currentResume.value.id, {
      title: draft.title.trim(),
      summary: draft.summary.trim(),
      skills: normalizeSkills(draft.skillsText),
      education: draft.education.trim(),
      selfIntro: draft.selfIntro.trim(),
      projects: editingProjects.value.map((project, index) => ({
        id: project.id,
        projectName: project.projectName.trim(),
        roleName: project.roleName.trim(),
        techStack: project.techStack.trim(),
        responsibility: project.responsibility.trim(),
        achievement: project.achievement.trim(),
        projectSummary: project.projectSummary.trim(),
        sortOrder: index + 1
      }))
    })
    currentResume.value = response.data
    hydrateDraft(response.data)
    await Promise.all([
      refreshResumeList(),
      loadInterviewResume(response.data.id),
      loadScore(response.data.id),
      loadVersions(response.data.id)
    ])
    await syncResumeRoute(response.data.id)
    isEditing.value = false
    ElMessage.success('简历内容已更新')
  } catch {
    ElMessage.error(ERROR_COPY.resumeSaveFailed)
  } finally {
    saving.value = false
  }
}

const handleRetryParse = async () => {
  if (!currentResume.value) return
  retrying.value = true
  try {
    const response = await retryResumeParseApi(currentResume.value.id)
    currentResume.value = response.data
    hydrateDraft(response.data)
    await Promise.all([
      refreshResumeList(),
      loadInterviewResume(response.data.id),
      loadScore(response.data.id),
      loadVersions(response.data.id)
    ])
    await syncResumeRoute(response.data.id)
    isEditing.value = response.data.parseStatus === 'failed'
    ElMessage.success(response.data.parseStatus === 'parsed' ? '已重新识别简历' : '已刷新当前简历状态')
  } catch {
    ElMessage.error(ERROR_COPY.resumeRetryParseFailed)
  } finally {
    retrying.value = false
  }
}

const handleUpload = async (file: File) => {
  uploading.value = true
  try {
    const response = await uploadResumeApi(file)
    currentResume.value = response.data
    selectedResumeId.value = response.data.id
    hydrateDraft(response.data)
    await Promise.all([
      refreshResumeList(),
      loadInterviewResume(response.data.id),
      loadScore(response.data.id),
      loadVersions(response.data.id)
    ])
    await syncResumeRoute(response.data.id)
    isEditing.value = response.data.parseStatus === 'failed'
    markGuideSeenForCriticalAction(currentUser?.id)
    ElMessage.success(response.data.parseStatus === 'failed' ? '简历已上传，请检查并修正简历内容' : '简历识别完成')
  } catch (error: any) {
    ElMessage.error(error?.message || ERROR_COPY.resumeUploadFailed)
  } finally {
    uploading.value = false
  }
  return false
}

const handleRestoreVersion = async (versionId: string) => {
  loading.value = true
  try {
    const response = await restoreResumeVersionApi(versionId)
    currentResume.value = response.data
    selectedResumeId.value = response.data.id
    hydrateDraft(response.data)
    await Promise.all([
      refreshResumeList(),
      loadInterviewResume(response.data.id),
      loadScore(response.data.id),
      loadVersions(response.data.id)
    ])
    await syncResumeRoute(response.data.id)
    isEditing.value = false
    ElMessage.success('已回滚到该版本')
  } catch {
    ElMessage.error(ERROR_COPY.resumeRestoreVersionFailed)
  } finally {
    loading.value = false
  }
}

const handleCopyResume = async () => {
  if (!interviewResume.value?.exportText) return
  try {
    await navigator.clipboard.writeText(interviewResume.value.exportText)
    ElMessage.success('面试提纲已复制')
  } catch {
    ElMessage.error(ERROR_COPY.resumeOutlineCopyFailed)
  }
}

const handleCopyAgentDraft = async () => {
  if (!agentFollowUpDraftText.value) return
  try {
    await navigator.clipboard.writeText(agentFollowUpDraftText.value)
    ElMessage.success('Agent 追问草稿已复制')
  } catch {
    ElMessage.error(ERROR_COPY.resumeOutlineCopyFailed)
  }
}

const handleDownloadResume = () => {
  if (!interviewResume.value?.exportText || !currentResume.value) return
  const blob = new Blob([interviewResume.value.exportText], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${currentResume.value.title || 'resume'}-interview-outline.txt`
  link.click()
  URL.revokeObjectURL(url)
}

const scrollToUpload = () => {
  document.getElementById('resume-upload')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped>
.resume-state-card {
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.11), transparent 28%),
    radial-gradient(circle at 88% 20%, rgba(var(--bc-cyan-rgb), 0.11), transparent 18%), var(--bc-surface-card);
}

.resume-state-hero {
  min-width: 0;
}

.resume-side-panel__section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--bc-border-subtle);
}

.resume-step-row {
  border-radius: calc(var(--radius-md) - 6px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.38);
  padding: 0.8rem 0.9rem;
  backdrop-filter: blur(10px);
}

.resume-list-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 0.95rem 1rem;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.resume-list-card:hover,
.resume-list-card-active {
  border-color: rgba(var(--bc-accent-rgb), 0.28);
  background: rgba(var(--bc-accent-rgb), 0.08);
}

.resume-step-row {
  display: flex;
  align-items: flex-start;
  gap: 0.9rem;
}

.resume-step-index {
  display: inline-flex;
  min-width: 2rem;
  height: 2rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(var(--bc-accent-rgb), 0.12);
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--bc-accent);
}

.resume-step-status {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 0.22rem 0.65rem;
  font-size: 0.72rem;
  font-weight: 700;
}

.resume-step-status.done {
  background: rgba(74, 122, 73, 0.12);
  color: var(--bc-lime);
}

.resume-step-status.active {
  background: rgba(240, 176, 67, 0.14);
  color: #b7791f;
}

.resume-step-status.pending {
  background: rgba(var(--bc-accent-rgb), 0.1);
  color: var(--bc-accent);
}

.resume-project-card,
.resume-highlight-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 1rem 1.05rem;
}

.resume-question-item {
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.72);
  padding: 0.9rem 1rem;
}

.resume-score-ring {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 1rem;
}

.resume-score-ring__value {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.resume-score-ring__number {
  font-size: 2.5rem;
  font-weight: 800;
  letter-spacing: -0.04em;
  line-height: 1;
  color: var(--bc-accent);
}

.resume-score-ring__label {
  font-size: 12px;
  font-weight: 600;
  color: var(--bc-ink-secondary);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.resume-suggestion-item {
  border-radius: 16px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 12px 14px;
}

.resume-suggestion-critical {
  border-color: rgba(255, 107, 107, 0.3);
}

.resume-suggestion-warn {
  border-color: rgba(255, 183, 77, 0.3);
}

.resume-suggestion-info {
  border-color: rgba(var(--bc-accent-rgb), 0.2);
}

.resume-suggestion-badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 700;
}

.resume-suggestion-critical .resume-suggestion-badge {
  background: rgba(255, 107, 107, 0.12);
  color: var(--bc-coral);
}

.resume-suggestion-warn .resume-suggestion-badge {
  background: rgba(255, 183, 77, 0.14);
  color: #b7791f;
}

.resume-suggestion-info .resume-suggestion-badge {
  background: rgba(var(--bc-accent-rgb), 0.1);
  color: var(--bc-accent);
}

.resume-version-item {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 0.85rem 1rem;
}

.resume-version-item:hover {
  border-color: rgba(var(--bc-accent-rgb), 0.22);
}
</style>
