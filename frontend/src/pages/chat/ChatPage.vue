<template>
  <div class="chat-page">
    <section class="chat-toolbar shell-section-card p-4 sm:p-5">
      <div class="chat-toolbar__head">
        <div class="min-w-0">
          <h2 class="chat-toolbar__title">先确定你现在要怎么问</h2>
          <p class="chat-toolbar__summary">
            先选提问路径，再决定要不要带资料、简历或项目上下文。顶部只保留这一步，不再解释模式定义。
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button type="button" class="session-toggle" @click="toggleSessionPanel">
            <span class="session-toggle__icon" aria-hidden="true">
              <svg v-if="desktopSessionVisible && isDesktopViewport" viewBox="0 0 24 24" fill="none">
                <path d="M8 4v16M12 4h8v16h-8" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none">
                <path d="M16 4v16M12 4H4v16h8" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
              </svg>
            </span>
            <span>会话</span>
          </button>
          <button type="button" class="topbar-primary" @click="startNewSession">新对话</button>
        </div>
      </div>

      <div class="chat-toolbar__paths" role="tablist" aria-label="提问路径">
        <button
          type="button"
          class="chat-path-chip"
          :class="{ 'chat-path-chip-active': chatPath === 'general' }"
          @click="applyChatPath('general')"
        >
          自由提问
        </button>
        <button
          type="button"
          class="chat-path-chip"
          :class="{ 'chat-path-chip-active': chatPath === 'knowledge' }"
          @click="applyChatPath('knowledge')"
        >
          基于资料回答
        </button>
        <button
          type="button"
          class="chat-path-chip"
          :class="{ 'chat-path-chip-active': chatPath === 'project' }"
          @click="applyChatPath('project')"
        >
          简历 / 项目上下文
        </button>
      </div>

      <div class="chat-toolbar__actions">
        <div class="chat-toolbar__selectors">
          <div v-if="chatPath !== 'general'" class="chat-context-grid">
            <el-select v-model="knowledgeScope" size="large" placeholder="资料范围">
              <el-option v-for="item in knowledgeScopes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <template v-if="chatPath === 'project'">
              <el-select
                v-model="selectedResumeId"
                size="large"
                clearable
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
                v-model="selectedProjectId"
                size="large"
                clearable
                placeholder="再锁定一个项目（可选）"
                :disabled="!selectedResumeId || !resumeProjects.length"
              >
                <el-option
                  v-for="project in resumeProjects"
                  :key="project.id"
                  :label="project.projectName"
                  :value="project.id"
                />
              </el-select>
            </template>
          </div>
          <div class="chat-context-note">
            <span class="detail-pill">{{ chatPathLabel(chatPath) }}</span>
            <span class="text-sm text-secondary">{{ draftContextSummary }}</span>
          </div>
        </div>
      </div>
    </section>

    <div class="chat-shell">
      <section class="chat-main shell-section-card">
        <div class="chat-main__workspace" :class="{ 'chat-main__workspace-sidebar-hidden': !desktopSessionVisible }">
          <aside
            class="session-sidebar hidden lg:flex"
            :class="{ 'session-sidebar-collapsed': !desktopSessionVisible }"
            :aria-hidden="!desktopSessionVisible"
            :inert="!desktopSessionVisible"
          >
            <div class="session-sidebar__header">
              <div>
                <h2 class="session-sidebar__title">会话列表</h2>
                <p class="session-sidebar__meta">{{ sessionTotal }} 个会话</p>
              </div>
            </div>

            <div class="session-sidebar__toolbar">
              <el-input v-model="sessionKeyword" placeholder="搜索会话" clearable class="session-search" />
              <div class="session-filters" role="tablist" aria-label="会话筛选">
                <button
                  v-for="filter in sessionFilters"
                  :key="filter.value"
                  type="button"
                  class="session-filter"
                  :class="{ 'session-filter-active': sessionFilter === filter.value }"
                  @click="sessionFilter = filter.value"
                >
                  {{ filter.label }}
                </button>
              </div>
            </div>

            <div class="session-sidebar__list">
              <div v-if="filteredSessions.length" class="session-list">
                <div
                  v-for="session in filteredSessions"
                  :key="session.id"
                  class="session-pill session-pill--stack"
                  role="button"
                  tabindex="0"
                  :class="{ 'session-pill-active': activeSessionId === session.id }"
                  @click="selectSession(session.id, session.mode)"
                  @keydown.enter.prevent="selectSession(session.id, session.mode)"
                  @keydown.space.prevent="selectSession(session.id, session.mode)"
                >
                  <span class="session-pill__title">{{ session.title || '未命名会话' }}</span>
                  <div class="session-pill__meta">
                    <span class="session-mode-tag" :class="session.mode === 'rag' ? 'tag-rag' : 'tag-chat'">
                      {{ session.mode === 'rag' ? '基于资料回答' : '自由提问' }}
                    </span>
                    <span>{{ formatSessionTime(session.lastMessageTime || session.updateTime) }}</span>
                  </div>
                  <p v-if="session.contextSource?.summary" class="session-pill__context">
                    {{ session.contextSource.summary }}
                  </p>
                  <button
                    type="button"
                    class="session-pill__delete"
                    aria-label="删除会话"
                    @click.stop="removeSession(session.id)"
                  >
                    删除
                  </button>
                </div>
              </div>

              <EmptyState
                v-else
                icon="chat"
                :title="sessions.length ? '没有找到相关会话' : '还没有会话'"
                :description="sessions.length ? '换个关键词试试。' : '新对话会显示在这里。'"
                compact
                class="session-strip__empty"
              />
            </div>

            <div v-if="sessionTotalPages > 1" class="session-sidebar__pagination">
              <el-pagination
                v-model:current-page="sessionPage"
                :page-size="sessionPageSize"
                :total="sessionTotal"
                layout="prev, pager, next"
                small
                @current-change="handleSessionPageChange"
              />
            </div>
          </aside>

          <div class="chat-main__content">
            <header class="conversation-bar">
              <div class="conversation-bar__title">
                <div class="conversation-bar__headline">
                  <p class="conversation-bar__name">{{ activeSessionTitle }}</p>
                  <span class="session-mode-tag" :class="mode === 'rag' ? 'tag-rag' : 'tag-chat'">
                    {{ chatPathLabel(activeChatPath) }}
                  </span>
                </div>
                <p v-if="activeContextSummary" class="conversation-bar__meta">
                  {{ activeContextSummary }}
                </p>
                <p v-else-if="activeSession" class="conversation-bar__meta">
                  {{ formatSessionTime(activeSession.lastMessageTime || activeSession.updateTime) }}
                </p>
              </div>

              <div class="conversation-bar__actions">
                <button type="button" class="topbar-secondary lg:hidden" @click="sessionDrawerVisible = true">
                  会话
                </button>
                <button
                  v-if="mode === 'rag' && hasFocusedReferences"
                  type="button"
                  class="topbar-secondary"
                  @click="openReferences"
                >
                  查看引用
                </button>
                <button type="button" class="topbar-primary topbar-primary-compact lg:hidden" @click="startNewSession">
                  新对话
                </button>
              </div>
            </header>

            <div class="chat-body">
              <div class="conversation-column">
                <div ref="messageContainer" class="message-scroll" @scroll.passive="handleMessageScroll">
                  <div v-if="loadingMessages" class="message-loading">
                    <div v-for="n in 3" :key="n" class="message-skeleton">
                      <div class="message-skeleton__meta"></div>
                      <div class="message-skeleton__line w-full"></div>
                      <div class="message-skeleton__line w-4/5"></div>
                    </div>
                  </div>

                  <div v-else-if="!messages.length && !streaming" class="chat-empty-wrap">
                    <EmptyState icon="chat" title="开始提问" :description="emptyStateDescription" compact />
                    <div class="prompt-suggestions">
                      <button
                        v-for="suggestion in promptSuggestions"
                        :key="suggestion"
                        type="button"
                        class="prompt-chip"
                        @click="applyPromptSuggestion(suggestion)"
                      >
                        {{ suggestion }}
                      </button>
                    </div>
                  </div>

                  <div v-else class="message-stream">
                    <article
                      v-for="message in messages"
                      :key="message.id"
                      class="message-row"
                      :class="message.role === 'user' ? 'message-row-user' : 'message-row-assistant'"
                    >
                      <div class="message-bubble-wrap">
                        <div class="message-meta">
                          <span>{{ message.role === 'assistant' ? '学习助手' : '我' }}</span>
                          <span>{{ formatSessionTime(message.createTime) }}</span>
                        </div>

                        <div
                          class="message-bubble"
                          :class="message.role === 'user' ? 'message-bubble-user' : 'message-bubble-assistant'"
                        >
                          <div class="bc-markdown text-sm leading-7" v-html="renderMarkdown(message.content)"></div>
                        </div>

                        <div
                          v-if="message.role === 'assistant' && message.references?.length"
                          class="message-reference-entry"
                        >
                          <button type="button" class="reference-link" @click="focusMessageReferences(message.id)">
                            查看 {{ message.references.length }} 条引用
                          </button>
                        </div>

                        <div v-if="message.messageType === 'error'" class="message-error-actions">
                          <button type="button" class="topbar-secondary" @click="retryLastQuestion">重试</button>
                        </div>
                      </div>
                    </article>

                    <article v-if="streaming" class="message-row message-row-assistant">
                      <div class="message-bubble-wrap">
                        <div class="message-meta">
                          <span>学习助手</span>
                          <span>{{ mode === 'rag' ? '正在检索资料' : '正在回答' }}</span>
                        </div>
                        <div class="message-bubble message-bubble-assistant">
                          <div class="bc-markdown text-sm leading-7">
                            <span v-html="renderMarkdown(streamingContent)"></span>
                            <span class="streaming-cursor"></span>
                          </div>
                        </div>
                      </div>
                    </article>
                  </div>
                </div>
              </div>

              <footer class="composer-shell">
                <div class="composer-shell__meta">
                  <span class="composer-hint">
                    {{ chatPathLabel(chatPath) }}
                  </span>
                  <span class="composer-hint">{{ composerContextHint }}</span>
                  <span class="composer-shortcut">Enter 发送 · Shift + Enter 换行</span>
                </div>

                <div class="composer-shell__input">
                  <el-input
                    ref="composerRef"
                    class="composer-textarea"
                    v-model="prompt"
                    type="textarea"
                    :rows="1"
                    resize="none"
                    :autosize="{ minRows: 1, maxRows: 4 }"
                    placeholder="输入问题"
                    :disabled="streaming"
                    @keydown.enter.exact.prevent="submitChat"
                  />

                  <div class="composer-actions">
                    <el-button v-if="streaming" class="composer-stop" @click="stopStreaming">停止</el-button>
                    <el-button
                      type="primary"
                      class="composer-send"
                      :loading="sending"
                      :disabled="streaming || !prompt.trim()"
                      @click="submitChat"
                    >
                      发送
                    </el-button>
                  </div>
                </div>

                <div v-if="suggestedQuestions.length" class="follow-up-strip">
                  <span class="follow-up-strip__label">推荐追问</span>
                  <button
                    v-for="question in suggestedQuestions"
                    :key="question"
                    type="button"
                    class="prompt-chip"
                    @click="applyPromptSuggestion(question)"
                  >
                    {{ question }}
                  </button>
                </div>
              </footer>
            </div>
          </div>
        </div>
      </section>
    </div>

    <el-drawer
      v-model="sessionDrawerVisible"
      direction="ltr"
      :size="isDesktopViewport ? '300px' : '84%'"
      :with-header="false"
      class="chat-drawer"
    >
      <div class="drawer-shell drawer-shell-sessions">
        <div class="drawer-header">
          <div>
            <p>会话列表</p>
          </div>
          <button type="button" class="reference-close" @click="sessionDrawerVisible = false">关闭</button>
        </div>

        <div class="session-sidebar__toolbar">
          <el-input v-model="sessionKeyword" placeholder="搜索会话" clearable class="session-search" />
          <div class="session-filters" role="tablist" aria-label="会话筛选">
            <button
              v-for="filter in sessionFilters"
              :key="filter.value"
              type="button"
              class="session-filter"
              :class="{ 'session-filter-active': sessionFilter === filter.value }"
              @click="sessionFilter = filter.value"
            >
              {{ filter.label }}
            </button>
          </div>
        </div>

        <div class="drawer-session-list">
          <div
            v-for="session in filteredSessions"
            :key="`drawer-${session.id}`"
            class="session-pill session-pill--stack"
            role="button"
            tabindex="0"
            :class="{ 'session-pill-active': activeSessionId === session.id }"
            @click="selectSession(session.id, session.mode)"
            @keydown.enter.prevent="selectSession(session.id, session.mode)"
            @keydown.space.prevent="selectSession(session.id, session.mode)"
          >
            <span class="session-pill__title">{{ session.title || '未命名会话' }}</span>
            <div class="session-pill__meta">
              <span class="session-mode-tag" :class="session.mode === 'rag' ? 'tag-rag' : 'tag-chat'">
                {{ session.mode === 'rag' ? '基于资料回答' : '自由提问' }}
              </span>
              <span>{{ formatSessionTime(session.lastMessageTime || session.updateTime) }}</span>
            </div>
            <button
              type="button"
              class="session-pill__delete"
              aria-label="删除会话"
              @click.stop="removeSession(session.id)"
            >
              删除
            </button>
          </div>

          <EmptyState
            v-if="!filteredSessions.length"
            icon="chat"
            :title="sessions.length ? '没有找到相关会话' : '还没有会话'"
            :description="sessions.length ? '换个关键词试试。' : '新对话会显示在这里。'"
            compact
          />
        </div>

        <div v-if="sessionTotalPages > 1" class="session-sidebar__pagination">
          <el-pagination
            v-model:current-page="sessionPage"
            :page-size="sessionPageSize"
            :total="sessionTotal"
            layout="prev, pager, next"
            small
            @current-change="handleSessionPageChange"
          />
        </div>
      </div>
    </el-drawer>

    <el-drawer
      v-model="referenceDrawerVisible"
      :direction="isDesktopViewport ? 'rtl' : 'btt'"
      :size="isDesktopViewport ? '420px' : '70%'"
      :with-header="false"
      class="chat-drawer"
    >
      <div class="drawer-shell drawer-shell-references">
        <div class="drawer-header">
          <div>
            <p>引用资料</p>
          </div>
          <button type="button" class="reference-close" @click="referenceDrawerVisible = false">关闭</button>
        </div>

        <div class="reference-panel__list">
          <article
            v-for="reference in focusedReferences"
            :key="`drawer-ref-${reference.docId}-${reference.chunkId}`"
            class="reference-card"
          >
            <div class="reference-card__top">
              <h3>{{ reference.docTitle }}</h3>
              <span class="reference-score">{{ scorePercent(reference.score) }}</span>
            </div>
            <div class="reference-card__meta">
              <span>片段 #{{ reference.chunkId }}</span>
              <span>{{ confidenceLabel(reference.score) }}</span>
              <span v-if="reference.libraryScope">{{ knowledgeScopeLabel(reference.libraryScope) }}</span>
              <span v-if="reference.fileType">{{ reference.fileType.toUpperCase() }}</span>
            </div>
            <p>{{ reference.snippet }}</p>
          </article>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import type { ComponentPublicInstance } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'
import { deleteChatSessionApi, fetchChatMessagesApi, fetchChatSessionsApi } from '@/api/chat'
import { fetchResumeDetailApi, fetchResumeListApi } from '@/api/resume'
import type {
  ChatKnowledgeScope,
  ChatMessageItem,
  ChatSessionItem,
  ContextSource,
  KnowledgeReferenceItem,
  ResumeProjectItem,
  ResumeSummaryItem
} from '@/types/api'
import { storage } from '@/utils/storage'
import { getStoredDeviceId } from '@/utils/device'

type SessionFilterValue = 'all' | 'rag' | 'chat'
type ElTextareaInstance = ComponentPublicInstance<{ focus?: () => void }>
const route = useRoute()

const sessions = ref<ChatSessionItem[]>([])
const messages = ref<ChatMessageItem[]>([])
const activeSessionId = ref<number | null>(null)
const mode = ref<'chat' | 'rag'>('rag')
const knowledgeScope = ref<ChatKnowledgeScope>('all')
const chatPath = ref<'general' | 'knowledge' | 'project'>('knowledge')
const prompt = ref('')
const loadingMessages = ref(false)
const sending = ref(false)
const streaming = ref(false)
const streamingContent = ref('')
const messageContainer = ref<HTMLElement | null>(null)
const composerRef = ref<ElTextareaInstance | null>(null)

const sessionPage = ref(1)
const sessionPageSize = ref(20)
const sessionTotal = ref(0)
const sessionTotalPages = ref(0)
const sessionKeyword = ref('')
const sessionFilter = ref<SessionFilterValue>('all')
const desktopSessionVisible = ref(true)
const sessionDrawerVisible = ref(false)
const referenceDrawerVisible = ref(false)
const focusedReferenceMessageId = ref<number | null>(null)
const lastQuestion = ref('')
const suggestedQuestions = ref<string[]>([])
const autoStickToBottom = ref(true)
const isDesktopViewport = ref(typeof window !== 'undefined' ? window.innerWidth >= 1024 : true)
const resumes = ref<ResumeSummaryItem[]>([])
const loadingResumes = ref(false)
const selectedResumeId = ref('')
const selectedProjectId = ref('')
const resumeProjects = ref<ResumeProjectItem[]>([])
const seededQuestionSummary = ref('')

const sessionFilters: Array<{ label: string; value: SessionFilterValue }> = [
  { label: '全部', value: 'all' },
  { label: '资料问答', value: 'rag' },
  { label: '自由提问', value: 'chat' }
]

const knowledgeScopes: Array<{ label: string; value: ChatKnowledgeScope }> = [
  { label: '全部资料', value: 'all' },
  { label: '推荐资料', value: 'system' },
  { label: '我的资料', value: 'personal' }
]

const promptSuggestions = ['帮我梳理这次学习的重点', '解释一下这个知识点的核心原理', '给我一份更容易复习的总结']

let abortController: AbortController | null = null

marked.setOptions({
  breaks: true,
  gfm: true
})

const activeSession = computed(() => sessions.value.find((item) => item.id === activeSessionId.value) ?? null)

const activeChatPath = computed(() => {
  if (activeSession.value?.contextType === 'project' || activeSession.value?.contextType === 'resume') return 'project'
  if (activeSession.value?.mode === 'rag') return 'knowledge'
  return activeSession.value ? 'general' : chatPath.value
})

const activeSessionTitle = computed(() => {
  if (!activeSessionId.value) return '新对话'
  return activeSession.value?.title || '当前会话'
})

const sourceQuestionTitle = computed(() => String(route.query.sourceQuestionTitle || '').trim())

const draftContextSource = computed<ContextSource | null>(() => {
  if (chatPath.value === 'project') {
    const resume = resumes.value.find((item) => item.id === selectedResumeId.value)
    const project = resumeProjects.value.find((item) => item.id === selectedProjectId.value)
    return {
      type: project ? 'project' : 'resume',
      label: project ? '项目上下文' : '简历上下文',
      knowledgeScope: knowledgeScope.value,
      resumeId: selectedResumeId.value || undefined,
      resumeTitle: resume?.title,
      projectId: project?.id,
      projectName: project?.projectName,
      summary: project
        ? seededQuestionSummary.value || `当前会优先围绕项目「${project.projectName}」回答，并检索${knowledgeScopeLabel(knowledgeScope.value)}。`
        : resume
          ? seededQuestionSummary.value || `当前会优先结合简历《${resume.title}》回答，并检索${knowledgeScopeLabel(knowledgeScope.value)}。`
          : seededQuestionSummary.value || '先选一份简历，再决定要不要锁定到某个项目。'
    }
  }
  if (chatPath.value === 'knowledge') {
    return {
      type: 'knowledge',
      label: '资料上下文',
      knowledgeScope: knowledgeScope.value,
      summary: seededQuestionSummary.value || `当前会优先基于${knowledgeScopeLabel(knowledgeScope.value)}里的资料回答。`
    }
  }
  return {
    type: 'general',
    label: '自由提问',
    summary: seededQuestionSummary.value || '当前不会绑定资料、简历或项目，适合直接追问原理、场景和表达。'
  }
})

const activeContextSource = computed(() => activeSession.value?.contextSource ?? draftContextSource.value)
const activeContextSummary = computed(() => activeContextSource.value?.summary || '')
const draftContextSummary = computed(() => draftContextSource.value?.summary || '')
const composerContextHint = computed(() => {
  if (chatPath.value === 'project') {
    return selectedProjectId.value ? '会优先结合当前项目来回答' : '会优先结合当前简历来回答'
  }
  if (chatPath.value === 'knowledge') {
    return `会优先检索${knowledgeScopeLabel(knowledgeScope.value)}`
  }
  return '不绑定额外上下文'
})

const emptyStateDescription = computed(() => {
  if (sourceQuestionTitle.value) {
    return `当前会围绕「${sourceQuestionTitle.value}」继续提问。`
  }
  return '输入问题即可。'
})

const filteredSessions = computed(() => {
  const keyword = sessionKeyword.value.trim().toLowerCase()
  return sessions.value.filter((session) => {
    const matchMode = sessionFilter.value === 'all' || session.mode === sessionFilter.value
    const matchKeyword = !keyword || (session.title || '').toLowerCase().includes(keyword)
    return matchMode && matchKeyword
  })
})

const focusedMessage = computed(() => {
  if (!focusedReferenceMessageId.value) return null
  return messages.value.find((message) => message.id === focusedReferenceMessageId.value) ?? null
})

const focusedReferences = computed<KnowledgeReferenceItem[]>(() => focusedMessage.value?.references ?? [])

const hasFocusedReferences = computed(() => focusedReferences.value.length > 0)

const formatSessionTime = (value?: string) => {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

const scorePercent = (score?: number) => {
  if (score == null) return 'N/A'
  return `${Math.round(score * 100)}%`
}

const confidenceLabel = (score?: number) => {
  if (score == null) return '待核验'
  if (score >= 0.82) return '高相关'
  if (score >= 0.66) return '可参考'
  return '弱相关'
}

const chatPathLabel = (value: 'general' | 'knowledge' | 'project') => {
  if (value === 'project') return '简历 / 项目上下文'
  if (value === 'knowledge') return '基于资料回答'
  return '自由提问'
}

const knowledgeScopeLabel = (value?: string) => {
  if (value === 'system') return '推荐资料'
  if (value === 'personal') return '我的资料'
  return '全部资料'
}

const renderMarkdown = (value: string) => DOMPurify.sanitize(marked.parse(value || '') as string)

const focusComposer = () => {
  nextTick(() => {
    composerRef.value?.focus?.()
  })
}

const handleMessageScroll = () => {
  const container = messageContainer.value
  if (!container) return
  const threshold = 72
  autoStickToBottom.value = container.scrollHeight - container.scrollTop - container.clientHeight < threshold
}

const scrollToBottom = (force = false) => {
  nextTick(() => {
    const container = messageContainer.value
    if (!container) return
    if (!force && !autoStickToBottom.value) return
    container.scrollTop = container.scrollHeight
  })
}

const syncFocusedReferences = () => {
  if (focusedReferenceMessageId.value) {
    const stillExists = messages.value.some(
      (message) => message.id === focusedReferenceMessageId.value && message.references?.length
    )
    if (stillExists) return
  }
  const fallbackMessage = [...messages.value]
    .reverse()
    .find((message) => message.role === 'assistant' && message.references?.length)
  focusedReferenceMessageId.value = fallbackMessage?.id ?? null
}

const loadSessions = async (selectLatest = false) => {
  const response = await fetchChatSessionsApi(sessionPage.value, sessionPageSize.value)
  sessions.value = response.data.records
  sessionTotal.value = response.data.total
  sessionTotalPages.value = response.data.totalPages
  if (selectLatest && sessions.value.length) {
    const latest = sessions.value[0]
    if (latest) {
      await selectSession(latest.id, latest.mode)
    }
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

const handleSessionPageChange = (page: number) => {
  sessionPage.value = page
  void loadSessions()
}

const loadMessages = async (sessionId: number) => {
  loadingMessages.value = true
  focusedReferenceMessageId.value = null
  suggestedQuestions.value = []
  try {
    const response = await fetchChatMessagesApi(sessionId)
    messages.value = response.data
    syncFocusedReferences()
    autoStickToBottom.value = true
    scrollToBottom(true)
  } catch {
    ElMessage.error('会话内容加载失败')
  } finally {
    loadingMessages.value = false
  }
}

const selectSession = async (sessionId: number, nextMode?: 'chat' | 'rag') => {
  activeSessionId.value = sessionId
  const session = sessions.value.find((item) => item.id === sessionId)
  if (nextMode) {
    mode.value = nextMode
  }
  if (session) {
    applySessionContext(session)
  }
  sessionDrawerVisible.value = false
  await loadMessages(sessionId)
}

const clearFocusedReferences = () => {
  focusedReferenceMessageId.value = null
  referenceDrawerVisible.value = false
}

const focusMessageReferences = (messageId: number) => {
  if (focusedReferenceMessageId.value === messageId && hasFocusedReferences.value) {
    clearFocusedReferences()
    return
  }
  focusedReferenceMessageId.value = messageId
  referenceDrawerVisible.value = true
}

const openReferences = () => {
  if (!hasFocusedReferences.value) return
  referenceDrawerVisible.value = !referenceDrawerVisible.value
}

const toggleSessionPanel = () => {
  if (isDesktopViewport.value) {
    desktopSessionVisible.value = !desktopSessionVisible.value
    return
  }
  sessionDrawerVisible.value = true
}

const startNewSession = () => {
  activeSessionId.value = null
  messages.value = []
  prompt.value = ''
  focusedReferenceMessageId.value = null
  suggestedQuestions.value = []
  referenceDrawerVisible.value = false
  sessionDrawerVisible.value = false
  autoStickToBottom.value = true
  focusComposer()
}

const applyQuestionSeedFromRoute = () => {
  if (!sourceQuestionTitle.value || activeSessionId.value) return
  const meta = [route.query.sourceQuestionCategory, route.query.sourceQuestionDirection, route.query.sourceQuestionTag]
    .map((item) => String(item || '').trim())
    .filter(Boolean)
  seededQuestionSummary.value = meta.length
    ? `当前会优先围绕题目「${sourceQuestionTitle.value}」继续提问，重点参考${meta.join(' / ')}。`
    : `当前会优先围绕题目「${sourceQuestionTitle.value}」继续提问。`
  prompt.value = `围绕题目「${sourceQuestionTitle.value}」继续追问，帮我补充答案结构、常见追问和更好的表达方式。`
  if (chatPath.value === 'general') {
    applyChatPath('knowledge')
  }
}

const applySessionContext = (session: ChatSessionItem) => {
  if (session.contextType === 'project' || session.contextType === 'resume') {
    chatPath.value = 'project'
    mode.value = 'rag'
    knowledgeScope.value = session.contextSource?.knowledgeScope || session.knowledgeScope || 'personal'
    selectedResumeId.value = session.contextSource?.resumeId || ''
    selectedProjectId.value = session.contextSource?.projectId || ''
    if (selectedResumeId.value) {
      void loadResumeProjects(selectedResumeId.value)
    }
    return
  }
  if (session.mode === 'rag') {
    chatPath.value = 'knowledge'
    mode.value = 'rag'
    knowledgeScope.value = session.contextSource?.knowledgeScope || session.knowledgeScope || 'all'
    selectedResumeId.value = ''
    selectedProjectId.value = ''
    return
  }
  chatPath.value = 'general'
  mode.value = 'chat'
  selectedResumeId.value = ''
  selectedProjectId.value = ''
}

const applyChatPath = (path: 'general' | 'knowledge' | 'project') => {
  chatPath.value = path
  if (path === 'general') {
    mode.value = 'chat'
    selectedResumeId.value = ''
    selectedProjectId.value = ''
    return
  }
  mode.value = 'rag'
  selectedProjectId.value = ''
  if (path === 'knowledge') {
    selectedResumeId.value = ''
    return
  }
  if (path === 'project') {
    if (knowledgeScope.value === 'all') {
      knowledgeScope.value = 'personal'
    }
    if (!selectedResumeId.value && resumes.value[0]) {
      selectedResumeId.value = resumes.value[0].id
    }
  }
}

const removeSession = async (sessionId: number) => {
  try {
    await deleteChatSessionApi(sessionId)
    ElMessage.success('会话已删除')
    if (activeSessionId.value === sessionId) {
      startNewSession()
    }
    await loadSessions()
  } catch {
    ElMessage.error('删除会话失败')
  }
}

const pushErrorMessage = (content = '本次回答失败，请重试。') => {
  messages.value.push({
    id: Date.now() + 2,
    role: 'assistant',
    messageType: 'error',
    content,
    createTime: new Date().toISOString(),
    references: []
  })
  autoStickToBottom.value = true
  scrollToBottom(true)
}

const stopStreaming = () => {
  abortController?.abort()
}

const parseSseChunk = (
  chunkText: string,
  onToken: (token: string) => void,
  onDone: (payload: {
    sessionId?: number | null
    references?: KnowledgeReferenceItem[]
    suggestedQuestions?: string[]
    contextType?: string
    contextSource?: ContextSource | null
  }) => void
) => {
  let eventName = 'message'
  const dataLines: string[] = []

  for (const rawLine of chunkText.split(/\r?\n/)) {
    const line = rawLine.trimEnd()
    if (!line) continue
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim() || 'message'
      continue
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  }

  if (!dataLines.length) return false

  const payloadText = dataLines.join('\n')
  if (!payloadText) return false

  try {
    const parsed = JSON.parse(payloadText)
    if (eventName === 'token' || 'content' in parsed) {
      if (parsed.content) {
        onToken(String(parsed.content))
      }
      return false
    }
    if (eventName === 'done' || 'sessionId' in parsed) {
      onDone({
        sessionId: parsed.sessionId ?? null,
        references: parsed.references || [],
        suggestedQuestions: Array.isArray(parsed.suggestedQuestions) ? parsed.suggestedQuestions : [],
        contextType: parsed.contextType || '',
        contextSource: parsed.contextSource || null
      })
      return true
    }
    if (eventName === 'error' || 'message' in parsed) {
      throw new Error(parsed.message || 'stream error')
    }
  } catch (error) {
    if (error instanceof SyntaxError) {
      return false
    }
    throw error
  }

  return false
}

const runChat = async (userMessage: string) => {
  sending.value = true
  lastQuestion.value = userMessage

  const userMsg: ChatMessageItem = {
    id: Date.now(),
    role: 'user',
    messageType: 'text',
    content: userMessage,
    createTime: new Date().toISOString(),
    references: []
  }
  messages.value.push(userMsg)
  autoStickToBottom.value = true
  scrollToBottom(true)

  streaming.value = true
  streamingContent.value = ''
  suggestedQuestions.value = []
  abortController = new AbortController()

  try {
    const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
    const token = storage.getToken()
    const deviceId = getStoredDeviceId()

    const response = await fetch(`${baseURL}/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(deviceId ? { 'X-Device-Id': deviceId } : {})
      },
      body: JSON.stringify({
        sessionId: activeSessionId.value,
        mode: mode.value,
        answerMode: chatPath.value === 'project' ? 'project' : 'learning',
        knowledgeScope: knowledgeScope.value,
        resumeId: selectedResumeId.value || undefined,
        projectId: selectedProjectId.value || undefined,
        message: userMessage
      }),
      signal: abortController.signal
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const reader = response.body?.getReader()
    if (!reader) throw new Error('No reader')

    const decoder = new TextDecoder()
    let buffer = ''
    let finalSessionId: number | null = null
    let finalReferences: KnowledgeReferenceItem[] = []
    let finalSuggestedQuestions: string[] = []
    let finalContextType = ''
    let finalContextSource: ContextSource | null = null
    let streamCompleted = false

    while (!streamCompleted) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const normalized = buffer.replace(/\r\n/g, '\n')
      const chunks = normalized.split('\n\n')
      buffer = chunks.pop() || ''

      for (const chunk of chunks) {
        const completed = parseSseChunk(
          chunk,
          (token) => {
            streamingContent.value += token
            scrollToBottom()
          },
          ({ sessionId, references, suggestedQuestions: nextSuggestedQuestions, contextType, contextSource }) => {
            finalSessionId = sessionId ?? null
            finalReferences = references || []
            finalSuggestedQuestions = nextSuggestedQuestions || []
            finalContextType = contextType || ''
            finalContextSource = contextSource || null
          }
        )
        if (completed) {
          streamCompleted = true
          await reader.cancel()
          break
        }
      }
    }

    if (!streamCompleted && buffer.trim()) {
      parseSseChunk(
        buffer,
        (token) => {
          streamingContent.value += token
          scrollToBottom()
        },
        ({ sessionId, references, suggestedQuestions: nextSuggestedQuestions, contextType, contextSource }) => {
          finalSessionId = sessionId ?? null
          finalReferences = references || []
          finalSuggestedQuestions = nextSuggestedQuestions || []
          finalContextType = contextType || ''
          finalContextSource = contextSource || null
          streamCompleted = true
        }
      )
    }

    if (streamingContent.value) {
      const assistantMessageId = Date.now() + 1
      const assistantMsg: ChatMessageItem = {
        id: assistantMessageId,
        role: 'assistant',
        messageType: finalReferences.length ? 'reference' : 'text',
        content: streamingContent.value,
        createTime: new Date().toISOString(),
        references: finalReferences
      }
      messages.value.push(assistantMsg)
      if (finalReferences.length) {
        focusedReferenceMessageId.value = assistantMessageId
      }
      if (finalSessionId) {
        activeSessionId.value = finalSessionId
      }
      suggestedQuestions.value = finalSuggestedQuestions
      if (activeSessionId.value && finalContextSource) {
        const session = sessions.value.find((item) => item.id === activeSessionId.value)
        if (session) {
          session.contextType = finalContextType as ChatSessionItem['contextType']
          session.contextSource = finalContextSource
        }
      }
    }

    await loadSessions()
  } catch (error: any) {
    if (error.name === 'AbortError') {
      pushErrorMessage('回答已停止。你可以继续提问，或重新发送上一条问题。')
    } else {
      ElMessage.error('发送失败，请稍后重试')
      pushErrorMessage()
      console.error('SSE error:', error)
    }
  } finally {
    streaming.value = false
    streamingContent.value = ''
    sending.value = false
    abortController = null
    autoStickToBottom.value = true
    scrollToBottom(true)
  }
}

const submitChat = async () => {
  if (!prompt.value.trim() || streaming.value) return
  const userMessage = prompt.value.trim()
  prompt.value = ''
  await runChat(userMessage)
}

const retryLastQuestion = async () => {
  if (!lastQuestion.value || streaming.value) return
  await runChat(lastQuestion.value)
}

const applyPromptSuggestion = (suggestion: string) => {
  prompt.value = suggestion
  focusComposer()
}

const syncViewport = () => {
  isDesktopViewport.value = window.innerWidth >= 1024
}

onMounted(async () => {
  await Promise.all([loadSessions(true), loadResumes()])
  applyQuestionSeedFromRoute()
  syncViewport()
  window.addEventListener('resize', syncViewport)
  focusComposer()
})

onUnmounted(() => {
  window.removeEventListener('resize', syncViewport)
})

watch(selectedResumeId, async (resumeId) => {
  selectedProjectId.value = ''
  if (!resumeId) {
    resumeProjects.value = []
    return
  }
  await loadResumeProjects(resumeId)
})

watch(
  () => route.fullPath,
  () => {
    if (!activeSessionId.value) {
      applyQuestionSeedFromRoute()
    }
  }
)
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.session-mode-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 0.25rem 0.55rem;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
}

.tag-rag {
  background: rgba(85, 214, 190, 0.14);
  color: var(--bc-cyan);
}

.tag-chat {
  background: rgba(77, 163, 255, 0.12);
  color: #2563eb;
}

.chat-main {
  display: flex;
  flex: 1;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.chat-toolbar {
  margin-bottom: 1rem;
}

.chat-toolbar__head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.chat-toolbar__title {
  color: var(--bc-ink);
  font-size: 1.55rem;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.03em;
}

.chat-toolbar__summary {
  margin-top: 0.75rem;
  max-width: 48rem;
  color: var(--bc-ink-secondary);
  font-size: 0.92rem;
  line-height: 1.75;
}

.chat-toolbar__paths {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1rem;
}

.chat-path-chip {
  border-radius: 999px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 0.72rem 1rem;
  color: var(--bc-ink-secondary);
  font-size: 0.92rem;
  font-weight: 600;
  transition:
    border-color var(--motion-fast) var(--ease-hard),
    background var(--motion-fast) var(--ease-hard),
    color var(--motion-fast) var(--ease-hard);
}

.chat-path-chip-active {
  border-color: rgba(var(--bc-accent-rgb), 0.24);
  background: rgba(var(--bc-accent-rgb), 0.1);
  color: var(--bc-ink);
}

.chat-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
}

.chat-toolbar__selectors {
  display: grid;
  gap: 0.7rem;
}

.chat-context-grid {
  display: grid;
  gap: 0.75rem;
  width: min(100%, 760px);
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.chat-context-note {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.session-pill__context {
  margin-top: 0.45rem;
  color: var(--bc-ink-secondary);
  font-size: 0.8rem;
  line-height: 1.5;
}

.scope-toggle {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.scope-toggle__item {
  min-height: 36px;
  border-radius: 999px;
  border: 1px solid var(--bc-line);
  padding: 0 0.9rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--bc-ink-secondary);
  background: var(--interactive-bg);
  transition:
    border-color var(--motion-base) var(--ease-hard),
    color var(--motion-base) var(--ease-hard),
    background-color var(--motion-base) var(--ease-hard);
}

.scope-toggle__item-active {
  border-color: rgba(var(--bc-accent-rgb), 0.32);
  background: rgba(var(--bc-accent-rgb), 0.12);
  color: var(--bc-ink);
}

.chat-shell {
  overflow: hidden;
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  width: 100%;
}

.follow-up-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  margin-top: 0.9rem;
  align-items: center;
}

.follow-up-strip__label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--bc-ink-secondary);
}

.chat-main__workspace {
  display: grid;
  grid-template-columns: 288px minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  flex: 1;
  width: 100%;
  overflow: hidden;
  transition: grid-template-columns 220ms var(--ease-hard);
}

.chat-main__workspace-sidebar-hidden {
  grid-template-columns: 0 minmax(0, 1fr);
}

.chat-main__content {
  display: flex;
  min-width: 0;
  min-height: 0;
  width: 100%;
  flex-direction: column;
}

.session-sidebar {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 0.9rem;
  padding: 1.1rem 1rem 1rem 1rem;
  border-right: 1px solid var(--border-subtle);
  min-width: 0;
  overflow: hidden;
  opacity: 1;
  transform: translateX(0);
  transition:
    opacity 180ms var(--ease-hard),
    transform 220ms var(--ease-hard),
    padding 220ms var(--ease-hard),
    border-color 220ms var(--ease-hard);
}

.session-sidebar-collapsed {
  pointer-events: none;
  opacity: 0;
  transform: translateX(-18px);
  padding-inline: 0;
  border-color: transparent;
}

.session-sidebar__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  padding-inline: 0.1rem;
}

.session-sidebar__title {
  margin-top: 0.1rem;
  color: var(--bc-ink);
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.session-sidebar__meta {
  margin-top: 0.22rem;
  color: rgb(148 163 184);
  font-size: 11px;
}

.session-sidebar__toolbar {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  padding-inline: 0.1rem;
}

.session-sidebar__list {
  min-height: 0;
  flex: 1;
}

.session-sidebar__pagination {
  padding-top: 0.25rem;
}

.session-list {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 0.65rem;
  overflow-y: auto;
  padding-inline: 0.1rem 0.3rem;
}

.chat-page__topbar {
  margin-bottom: 1rem;
  flex-shrink: 0;
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
  display: flex;
  align-items: center;
  gap: 0.9rem;
  min-width: 0;
  flex-wrap: wrap;
}

.module-topbar__heading {
  color: var(--bc-ink);
  font-size: 1.28rem;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.module-topbar__action {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
  margin-left: auto;
}

.mode-toggle-page {
  width: fit-content;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(241, 245, 249, 0.72);
  padding: 0.2rem;
}

.session-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  border: 1px solid var(--bc-border-subtle);
  border-radius: 999px;
  background: var(--interactive-bg);
  padding: 0.55rem 0.8rem;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  transition:
    border-color 160ms var(--ease-hard),
    background-color 160ms var(--ease-hard),
    color 160ms var(--ease-hard),
    transform 160ms var(--ease-hard);
}

.session-toggle:hover {
  border-color: rgba(var(--bc-accent-rgb), 0.28);
  background: var(--interactive-hover);
  color: var(--bc-accent);
}

.session-toggle:active {
  transform: translateY(1px);
}

.session-toggle__icon {
  display: inline-flex;
  width: 1rem;
  height: 1rem;
}

.session-toggle__icon svg {
  width: 100%;
  height: 100%;
}

.conversation-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1rem 0.75rem;
}

.conversation-bar__title {
  min-width: 0;
}

.conversation-bar__headline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.65rem;
}

.conversation-bar__name {
  overflow: hidden;
  color: var(--bc-ink);
  font-size: 1.05rem;
  font-weight: 700;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-bar__meta {
  margin-top: 0.2rem;
  color: var(--text-secondary);
  font-size: 12px;
}

.conversation-bar__actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.mode-toggle {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  border-radius: 999px;
  background: var(--interactive-bg);
  padding: 0.25rem;
}

.mode-toggle__item,
.topbar-icon,
.topbar-secondary,
.topbar-primary,
.reference-close {
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
  transition: all 160ms var(--ease-hard);
}

.mode-toggle__item {
  padding: 0.56rem 0.82rem;
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 600;
}

.mode-toggle__item-active {
  background: rgba(var(--bc-accent-rgb), 0.14);
  color: var(--bc-ink);
  box-shadow: inset 0 0 0 1px rgba(var(--bc-accent-rgb), 0.18);
}

.topbar-icon,
.topbar-secondary,
.reference-close {
  border: 1px solid var(--bc-border-subtle);
  background: var(--interactive-bg);
  padding: 0.75rem 0.95rem;
  color: var(--text-secondary);
}

.topbar-primary {
  background: linear-gradient(180deg, #c4791e 0%, var(--bc-amber) 100%);
  padding: 0.8rem 1rem;
  color: white;
  box-shadow: 0 14px 24px rgba(var(--bc-accent-rgb), 0.18);
}

.topbar-primary-compact {
  padding-inline: 0.92rem;
}

.session-strip {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  padding: 0 1rem 1rem;
  border-bottom: 1px solid var(--bc-border-subtle);
}

.session-strip__toolbar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.session-search {
  width: 100%;
}

.session-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.session-filter {
  border: 1px solid var(--bc-border-subtle);
  border-radius: 999px;
  background: var(--interactive-bg);
  padding: 0.42rem 0.72rem;
  color: var(--text-secondary);
  font-size: 11px;
  line-height: 1;
  transition: all 160ms var(--ease-hard);
}

.session-filter-active {
  border-color: rgba(var(--bc-accent-rgb), 0.3);
  background: rgba(var(--bc-accent-rgb), 0.14);
  color: var(--bc-accent);
}

.session-rail {
  display: flex;
  gap: 0.75rem;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 0.15rem;
}

.session-rail::-webkit-scrollbar {
  height: 8px;
}

.session-rail::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.3);
}

.session-pill {
  position: relative;
  display: flex;
  min-width: 220px;
  max-width: 280px;
  flex: 0 0 220px;
  flex-direction: column;
  gap: 0.5rem;
  border: 1px solid var(--bc-border-subtle);
  border-radius: var(--radius-lg);
  background: var(--panel-bg);
  padding: 0.82rem 0.9rem;
  transition:
    border-color 160ms var(--ease-hard),
    box-shadow 160ms var(--ease-hard),
    background-color 160ms var(--ease-hard);
}

.session-pill--stack {
  min-width: 0;
  max-width: none;
  flex: 0 0 auto;
}

.session-pill:hover,
.session-pill-active {
  border-color: rgba(var(--bc-accent-rgb), 0.24);
  background: var(--bc-surface-card-hover);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.07);
}

.session-pill__title {
  display: -webkit-box;
  overflow: hidden;
  padding-right: 2rem;
  color: var(--bc-ink);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.38;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.session-pill__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
  color: var(--text-secondary);
  font-size: 10px;
}

.session-pill__delete {
  position: absolute;
  top: 0.62rem;
  right: 0.62rem;
  border-radius: 999px;
  padding: 0.2rem 0.42rem;
  color: var(--text-tertiary);
  font-size: 10px;
  transition: all 160ms var(--ease-hard);
}

.session-pill__delete:hover {
  background: rgba(255, 107, 107, 0.1);
  color: var(--bc-coral);
}

.session-strip__empty,
.session-strip__pagination {
  flex: none;
}

.chat-body {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
  padding: 0.85rem 1rem 1rem;
  overflow: hidden;
}

.conversation-column {
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
}

.message-scroll {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 0.25rem 0.25rem 0.5rem;
  overscroll-behavior: contain;
}

.message-stream,
.message-loading {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.chat-empty-wrap {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  justify-content: center;
  gap: 1.25rem;
  padding: 2rem 0;
}

.prompt-suggestions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.75rem;
}

.prompt-chip {
  border: 1px solid rgba(var(--bc-accent-rgb), 0.18);
  border-radius: 999px;
  background: var(--interactive-bg);
  padding: 0.7rem 1rem;
  color: var(--text-primary);
  font-size: 13px;
  transition: all 160ms var(--ease-hard);
}

.prompt-chip:hover {
  border-color: rgba(var(--bc-accent-rgb), 0.28);
  background: rgba(var(--bc-accent-rgb), 0.08);
}

.message-row {
  display: flex;
}

.message-row-user {
  justify-content: flex-end;
}

.message-row-assistant {
  justify-content: flex-start;
}

.message-bubble-wrap {
  display: flex;
  max-width: min(78%, 860px);
  min-width: 0;
  flex-direction: column;
  gap: 0.45rem;
}

.message-row-user .message-bubble-wrap {
  align-items: flex-end;
}

.message-row-assistant .message-bubble-wrap {
  align-items: flex-start;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  color: var(--text-tertiary);
  font-size: 11px;
}

.message-bubble {
  width: 100%;
  border-radius: 22px;
  padding: 1rem 1rem 0.95rem;
  box-shadow: none;
}

.message-bubble-user {
  border-bottom-right-radius: 8px;
  background: linear-gradient(135deg, rgba(var(--bc-accent-rgb), 0.16), rgba(var(--bc-cyan-rgb), 0.14));
  border: 1px solid rgba(var(--bc-accent-rgb), 0.2);
}

.message-bubble-assistant {
  border: 1px solid var(--bc-border-subtle);
  border-bottom-left-radius: 8px;
  background: var(--panel-bg);
}

.message-reference-entry,
.message-error-actions {
  display: flex;
  align-items: center;
}

.reference-link {
  border: 1px solid rgba(var(--bc-accent-rgb), 0.22);
  border-radius: 999px;
  background: rgba(var(--bc-accent-rgb), 0.08);
  padding: 0.5rem 0.8rem;
  color: var(--bc-accent);
  font-size: 12px;
  font-weight: 600;
}

.composer-shell {
  margin-top: 0.75rem;
  border: 1px solid var(--bc-border-subtle);
  border-radius: 18px;
  background: var(--panel-bg);
  padding: 0.7rem;
  box-shadow: none;
  backdrop-filter: none;
}

.composer-shell__meta {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  padding: 0 0.1rem 0.45rem;
}

.composer-hint {
  color: var(--text-secondary);
  font-size: 12px;
}

.composer-shortcut {
  color: var(--text-tertiary);
  font-size: 11px;
  white-space: nowrap;
}

.composer-shell__input {
  display: grid;
  gap: 0.6rem;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: flex-end;
}

.composer-actions {
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  align-items: center;
  gap: 0.5rem;
}

.composer-send,
.composer-stop {
  width: auto;
  min-height: 38px;
  min-width: 88px;
  border-radius: 10px;
  font-weight: 700;
}

.composer-send {
  border: 0;
  background: linear-gradient(180deg, #c4791e 0%, var(--bc-amber) 100%);
  box-shadow: 0 12px 24px rgba(var(--bc-accent-rgb), 0.2);
}

.composer-stop {
  border: 1px solid rgba(255, 107, 107, 0.2);
  background: rgba(255, 107, 107, 0.08);
  color: var(--bc-coral);
}

.composer-textarea :deep(.el-textarea__inner) {
  min-height: 52px !important;
  border: 1px solid var(--bc-border-subtle);
  border-radius: 14px;
  background: var(--bc-surface-input);
  padding: 0.7rem 0.85rem;
  color: var(--bc-ink);
  font-size: 14px;
  line-height: 1.55;
  box-shadow:
    inset 0 1px 0 rgba(var(--bc-ink-rgb), 0.04),
    0 8px 18px rgba(15, 23, 42, 0.03);
  transition:
    border-color 160ms var(--ease-hard),
    box-shadow 160ms var(--ease-hard),
    transform 160ms var(--ease-hard);
}

.composer-textarea :deep(.el-textarea__inner:focus) {
  border-color: rgba(var(--bc-accent-rgb), 0.42);
  box-shadow:
    0 0 0 4px rgba(var(--bc-accent-rgb), 0.12),
    0 12px 20px rgba(var(--bc-accent-rgb), 0.08);
}

.composer-textarea :deep(.el-textarea__inner::placeholder) {
  color: var(--text-tertiary);
}

.reference-panel {
  min-height: 0;
  flex-direction: column;
  padding: 1rem;
}

.reference-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.reference-panel__title {
  color: var(--bc-ink);
  font-size: 15px;
  font-weight: 700;
}

.reference-panel__subtitle {
  margin-top: 0.25rem;
  color: var(--text-secondary);
  font-size: 12px;
}

.reference-panel__list {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 0.75rem;
  overflow-y: auto;
  margin-top: 1rem;
  padding-right: 0.2rem;
}

.reference-card {
  border: 1px solid var(--bc-border-subtle);
  border-radius: var(--radius-lg);
  background: var(--panel-muted);
  padding: 0.95rem;
}

.reference-card__top,
.reference-card__meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.reference-card__top h3 {
  color: var(--bc-ink);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
}

.reference-score {
  color: var(--bc-accent);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.reference-card__meta {
  margin-top: 0.55rem;
  color: var(--text-secondary);
  font-size: 11px;
}

.reference-card p {
  margin-top: 0.8rem;
  color: var(--text-primary);
  font-size: 12px;
  line-height: 1.75;
}

.message-skeleton {
  border: 1px solid var(--bc-border-subtle);
  border-radius: 20px;
  background: var(--panel-muted);
  padding: 1rem;
}

.message-skeleton__meta,
.message-skeleton__line {
  border-radius: 999px;
  background: var(--skeleton-bg);
}

.message-skeleton__meta {
  height: 12px;
  width: 96px;
}

.message-skeleton__line {
  margin-top: 0.8rem;
  height: 14px;
}

.drawer-shell {
  display: flex;
  height: 100%;
  flex-direction: column;
}

.drawer-shell-references {
  padding-bottom: 0.5rem;
}

.drawer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.drawer-header p {
  color: var(--bc-ink);
  font-size: 16px;
  font-weight: 700;
}

.drawer-header span {
  display: inline-block;
  margin-top: 0.25rem;
  color: rgb(100 116 139);
  font-size: 12px;
}

.drawer-session-list {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 0.75rem;
  overflow-y: auto;
  margin-top: 0.9rem;
  padding-right: 0.2rem;
}

.bc-markdown :deep(p) {
  margin: 0 0 0.75rem;
}

.bc-markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.bc-markdown :deep(code) {
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  background: rgba(85, 214, 190, 0.12);
  font-size: 0.92em;
}

.bc-markdown :deep(pre) {
  overflow-x: auto;
  margin: 0.75rem 0;
  padding: 0.9rem 1rem;
  border-radius: 8px;
  border: 1px solid var(--bc-border-subtle);
  background: rgba(7, 17, 31, 0.72);
  color: #e6f6ff;
}

.bc-markdown :deep(ul),
.bc-markdown :deep(ol) {
  padding-left: 1.25rem;
}

.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: var(--bc-cyan);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: cursor-blink 1s step-end infinite;
}

@keyframes cursor-blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

@media (max-width: 1279px) {
  .message-bubble-wrap {
    max-width: min(88%, 820px);
  }
}

@media (max-width: 1023px) {
  .chat-main {
    height: calc(100dvh - 196px);
  }

  .chat-toolbar__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .module-topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .module-topbar__title-group,
  .module-topbar__action {
    justify-content: flex-start;
  }

  .mode-toggle-page {
    width: 100%;
  }

  .chat-main__workspace,
  .chat-main__workspace-sidebar-hidden {
    grid-template-columns: minmax(0, 1fr);
  }

  .conversation-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .conversation-bar__actions {
    flex-wrap: wrap;
    justify-content: space-between;
  }

  .session-strip__toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .session-search {
    width: 100%;
  }

  .mode-toggle {
    order: 3;
    width: 100%;
  }

  .mode-toggle__item {
    flex: 1;
    justify-content: center;
  }
}

@media (min-width: 1024px) {
  .chat-main {
    min-height: calc(100dvh - 232px);
  }

  .chat-shell {
    height: 100%;
  }
}

@media (max-width: 767px) {
  .chat-main {
    height: calc(100dvh - 164px);
  }

  .module-topbar__title {
    align-items: flex-start;
  }

  .module-topbar__title-group {
    gap: 0.7rem;
  }

  .module-topbar__heading {
    font-size: 1.18rem;
  }

  .conversation-bar {
    padding: 0.85rem 0.85rem 0;
  }

  .chat-body {
    padding: 0.75rem 0.85rem 0.85rem;
  }

  .message-bubble-wrap {
    max-width: 100%;
  }

  .composer-shell__input {
    grid-template-columns: minmax(0, 1fr);
  }

  .composer-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .composer-send,
  .composer-stop {
    width: auto;
    min-width: 104px;
  }

  .topbar-primary,
  .topbar-secondary,
  .topbar-icon,
  .reference-close {
    padding: 0.7rem 0.9rem;
  }

  .chat-main__workspace {
    min-height: calc(100dvh - 164px);
  }
}
</style>
