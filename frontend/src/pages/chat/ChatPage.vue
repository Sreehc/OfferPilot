<template>
  <div class="chat-cockpit grid gap-4 xl:grid-cols-[280px_minmax(0,1fr)]">
    <aside class="cockpit-panel signal-log p-4 sm:p-5">
      <div class="flex items-start justify-between gap-3">
        <div>
          <div class="flex items-center gap-2">
            <span class="state-pulse" aria-hidden="true"></span>
            <p class="section-kicker">会话列表</p>
          </div>
          <h2 class="mt-3 text-2xl font-semibold tracking-[-0.04em] text-ink">最近会话</h2>
        </div>
        <button type="button" class="hard-button-secondary !min-h-11 !px-3 !py-2 text-xs" @click="startNewSession">
          新会话
        </button>
      </div>

      <div class="mt-5 grid grid-cols-2 gap-3">
        <div class="data-slab p-3">
          <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">会话数</p>
          <p class="mt-2 font-mono text-2xl font-semibold text-ink">{{ sessionTotal }}</p>
        </div>
        <div class="data-slab p-3">
          <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">知识库问答</p>
          <p class="mt-2 font-mono text-2xl font-semibold text-[var(--bc-cyan)]">{{ ragSessionCount }}</p>
        </div>
      </div>

      <div class="mt-5 space-y-3">
        <button
          v-for="session in sessions"
          :key="session.id"
          type="button"
          class="signal-card w-full text-left"
          :class="{ 'signal-card-active': activeSessionId === session.id }"
          @click="selectSession(session.id, session.mode)"
        >
          <span class="signal-card__rail" aria-hidden="true"></span>
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <div class="truncate text-sm font-semibold text-ink">{{ session.title }}</div>
              <div class="mt-3 flex flex-wrap items-center gap-2">
                <span class="hard-chip !px-2 !py-0.5 !text-[9px]" :class="session.mode === 'rag' ? 'signal-chip-rag' : 'signal-chip-chat'">
                  {{ session.mode === 'rag' ? '知识库' : '直答' }}
                </span>
                <span class="text-[11px] text-slate-500 dark:text-slate-400">{{ formatSessionTime(session.lastMessageTime || session.updateTime) }}</span>
              </div>
            </div>
            <span class="signal-card__delete" @click.stop="removeSession(session.id)">删除</span>
          </div>
        </button>
      </div>

      <EmptyState
        v-if="!sessions.length"
        icon="chat"
        title="还没有会话"
        description="发起第一个问题后，这里会保留你的问答历史。"
        compact
        class="mt-5"
      />

      <div v-if="sessionTotalPages > 1" class="mt-4 flex justify-center">
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

    <section class="cockpit-panel chat-console flex min-h-[640px] flex-col p-4 sm:p-5">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div class="min-w-0">
          <p class="section-kicker">智能问答</p>
          <h1 class="mt-3 text-2xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">带引用回答，或直接提问</h1>
          <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-300">
            {{ currentTitle }}。知识库问答会先检索资料并附上来源，直接问答则更适合快速确认概念。
          </p>
        </div>

        <div class="mode-switch grid grid-cols-2 gap-2">
          <button
            type="button"
            class="mode-switch__item"
            :class="{ 'mode-switch__item-active': mode === 'rag' }"
            @click="mode = 'rag'"
          >
            知识库问答
          </button>
          <button
            type="button"
            class="mode-switch__item"
            :class="{ 'mode-switch__item-active': mode === 'chat' }"
            @click="mode = 'chat'"
          >
            直接问答
          </button>
        </div>
      </div>

      <div class="mt-5 grid gap-3 sm:grid-cols-3">
        <div class="data-slab p-3">
          <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">当前状态</p>
          <p class="mt-2 text-sm font-semibold text-ink">{{ cockpitStatus }}</p>
        </div>
        <div class="data-slab p-3">
          <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">消息数</p>
          <p class="mt-2 font-mono text-xl font-semibold text-ink">{{ messages.length }}</p>
        </div>
        <div class="data-slab p-3">
          <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">当前引用</p>
          <p class="mt-2 font-mono text-xl font-semibold text-[var(--bc-cyan)]">{{ referenceDeck.length }}</p>
        </div>
      </div>

      <div class="input-console mt-5 grid gap-3 lg:grid-cols-[minmax(0,1fr)_160px]">
        <div>
          <div class="mb-2 flex flex-wrap items-center gap-2">
            <span class="hard-chip !px-2 !py-0.5 !text-[10px]">{{ mode === 'rag' ? '知识库问答' : '直接问答' }}</span>
            <span class="text-xs text-slate-500 dark:text-slate-400">
              {{ mode === 'rag' ? '先检索资料，再生成回答并附上来源。' : '直接生成回答，不附带知识库引用。' }}
            </span>
          </div>
          <el-input
            v-model="prompt"
            type="textarea"
            :rows="4"
            placeholder="输入你的问题，例如：HashMap 扩容为什么需要重新计算索引？"
            :disabled="streaming"
            @keydown.enter.exact.prevent="submitChat"
          />
        </div>
        <div class="flex flex-col gap-2">
          <el-button :loading="sending" :disabled="streaming" type="primary" size="large" class="action-button !min-h-12 transition active:translate-y-px" @click="submitChat">
            {{ streaming ? '回答中...' : '发送问题' }}
          </el-button>
          <span class="text-center text-xs text-slate-400 dark:text-slate-500">Enter 发送 · Shift 换行</span>
        </div>
      </div>

      <div class="mt-4 grid min-h-0 flex-1 gap-4" :class="referenceDeck.length ? '2xl:grid-cols-[minmax(0,1fr)_310px]' : ''">
        <div ref="messageContainer" class="message-bay min-h-[420px] space-y-4 overflow-y-auto pr-1">
          <article
            v-for="message in messages"
            :key="message.id"
            class="message-card p-4 sm:p-5"
            :class="message.role === 'assistant' ? 'message-card-assistant' : 'message-card-user'"
          >
            <div class="flex items-center justify-between gap-3">
              <div class="text-xs font-semibold uppercase tracking-[0.24em]" :class="message.role === 'assistant' ? 'text-[var(--bc-cyan)]' : 'text-slate-400 dark:text-slate-500'">
                {{ message.role === 'assistant' ? 'ByteCoach' : '我' }}
              </div>
              <span class="text-[11px] text-slate-400 dark:text-slate-500">{{ formatSessionTime(message.createTime) }}</span>
            </div>
            <div class="bc-markdown mt-3 text-sm leading-7" v-html="renderMarkdown(message.content)"></div>

            <div v-if="message.references && message.references.length" class="reference-stack mt-4">
              <div class="mb-3 flex items-center justify-between gap-3">
                <span class="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">引用资料</span>
                <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ message.references.length }} 条引用</span>
              </div>
              <div class="grid gap-3 md:grid-cols-2">
                <article v-for="reference in message.references" :key="reference.chunkId" class="reference-card p-3">
                  <div class="flex items-start justify-between gap-3">
                    <h3 class="line-clamp-1 text-sm font-semibold text-ink">{{ reference.docTitle }}</h3>
                    <span class="reference-score">{{ scorePercent(reference.score) }}</span>
                  </div>
                  <div class="mt-2 flex items-center gap-2 text-[11px] text-slate-500 dark:text-slate-400">
                    <span>片段 #{{ reference.chunkId }}</span>
                    <span class="h-1 w-1 rounded-full bg-slate-400"></span>
                    <span>{{ confidenceLabel(reference.score) }}</span>
                  </div>
                  <p class="mt-3 line-clamp-3 text-xs leading-6 text-slate-600 dark:text-slate-300">{{ reference.snippet }}</p>
                </article>
              </div>
            </div>
          </article>

          <article v-if="streaming" class="message-card message-card-assistant p-4 sm:p-5">
            <div class="flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.24em] text-[var(--bc-cyan)]">
              <span class="state-pulse" aria-hidden="true"></span>
              ByteCoach · {{ mode === 'rag' ? '检索并生成中' : '生成中' }}
            </div>
            <div class="bc-markdown mt-3 text-sm leading-7">
              <span v-html="renderMarkdown(streamingContent)"></span>
              <span class="streaming-cursor"></span>
            </div>
          </article>

          <div v-if="loadingMessages" class="space-y-3">
            <div v-for="n in 2" :key="n" class="surface-card animate-pulse p-4">
              <div class="h-3 w-24 rounded bg-slate-200 dark:bg-slate-700"></div>
              <div class="mt-3 h-4 w-full rounded bg-slate-100 dark:bg-slate-800"></div>
              <div class="mt-2 h-4 w-4/5 rounded bg-slate-100 dark:bg-slate-800"></div>
            </div>
          </div>

          <EmptyState
            v-if="!messages.length && !loadingMessages && !streaming"
            icon="chat"
            title="从一个问题开始"
            description="可以直接提问，也可以用知识库问答查看带来源的回答。"
            compact
          />
        </div>

        <aside v-if="referenceDeck.length" class="reference-deck hidden 2xl:block">
          <div class="flex items-center justify-between gap-3">
            <p class="section-kicker">本轮引用</p>
            <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ referenceDeck.length }} 条</span>
          </div>
          <div class="mt-4 space-y-3">
            <article v-for="reference in referenceDeck" :key="reference.chunkId" class="reference-card p-3">
              <div class="flex items-start justify-between gap-3">
                <h3 class="line-clamp-2 text-sm font-semibold text-ink">{{ reference.docTitle }}</h3>
                <span class="reference-score">{{ scorePercent(reference.score) }}</span>
              </div>
              <p class="mt-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-[var(--bc-cyan)]">
                {{ confidenceLabel(reference.score) }}
              </p>
              <p class="mt-3 line-clamp-4 text-xs leading-6 text-slate-600 dark:text-slate-300">{{ reference.snippet }}</p>
            </article>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { computed, nextTick, onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { deleteChatSessionApi, fetchChatMessagesApi, fetchChatSessionsApi } from '@/api/chat'
import type { ChatMessageItem, ChatSessionItem, KnowledgeReferenceItem } from '@/types/api'
import { storage } from '@/utils/storage'
import { getStoredDeviceId } from '@/utils/device'

const sessions = ref<ChatSessionItem[]>([])
const messages = ref<ChatMessageItem[]>([])
const activeSessionId = ref<number | null>(null)
const mode = ref<'chat' | 'rag'>('rag')
const prompt = ref('')
const loadingMessages = ref(false)
const sending = ref(false)
const streaming = ref(false)
const streamingContent = ref('')
const messageContainer = ref<HTMLElement | null>(null)

let abortController: AbortController | null = null

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}
const sessionPage = ref(1)
const sessionPageSize = ref(20)
const sessionTotal = ref(0)
const sessionTotalPages = ref(0)

marked.setOptions({
  breaks: true,
  gfm: true
})

const currentTitle = computed(() => {
  if (!activeSessionId.value) return mode.value === 'rag' ? '当前是新的知识库问答' : '当前是新的直接问答'
  return sessions.value.find((item) => item.id === activeSessionId.value)?.title || '当前会话'
})

const ragSessionCount = computed(() => sessions.value.filter((session) => session.mode === 'rag').length)

const referenceDeck = computed(() => {
  const latestAssistantMessage = [...messages.value]
    .reverse()
    .find((message) => message.role === 'assistant' && message.references?.length)
  return latestAssistantMessage?.references.slice(0, 4) ?? []
})

const cockpitStatus = computed(() => {
  if (streaming.value) return mode.value === 'rag' ? '正在检索资料并生成回答' : '正在生成回答'
  if (loadingMessages.value) return '正在同步历史会话'
  return mode.value === 'rag' ? '等待你的问题，回答会附带引用' : '等待你的问题，直接生成回答'
})

const formatSessionTime = (value?: string) => {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

const scorePercent = (score?: number) => {
  if (score == null) return 'N/A'
  return `${Math.round(score * 100)}%`
}

const confidenceLabel = (score?: number) => {
  if (score == null) return '待核验'
  if (score >= 0.82) return '高可信'
  if (score >= 0.66) return '可参考'
  return '弱相关'
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

const handleSessionPageChange = (page: number) => {
  sessionPage.value = page
  void loadSessions()
}

const loadMessages = async (sessionId: number) => {
  loadingMessages.value = true
  try {
    const response = await fetchChatMessagesApi(sessionId)
    messages.value = response.data
    scrollToBottom()
  } catch {
    ElMessage.error('会话内容加载失败')
  } finally {
    loadingMessages.value = false
  }
}

const selectSession = async (sessionId: number, nextMode?: 'chat' | 'rag') => {
  activeSessionId.value = sessionId
  if (nextMode) {
    mode.value = nextMode
  }
  await loadMessages(sessionId)
}

const startNewSession = () => {
  activeSessionId.value = null
  messages.value = []
  prompt.value = ''
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

const submitChat = async () => {
  if (!prompt.value.trim() || streaming.value) return
  sending.value = true

  const userMessage = prompt.value.trim()
  prompt.value = ''

  // Add user message to local display immediately
  const userMsg: ChatMessageItem = {
    id: Date.now(),
    role: 'user',
    messageType: 'text',
    content: userMessage,
    createTime: new Date().toISOString(),
    references: []
  }
  messages.value.push(userMsg)
  scrollToBottom()

  // Start streaming
  streaming.value = true
  streamingContent.value = ''
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

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('event:')) {
          // Event name line — handled below with data
          continue
        }
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (!data) continue

          // Determine event type from preceding event: line
          // SSE format: event: xxx\ndata: yyy\n\n
          // We need to parse both lines together
          try {
            const parsed = JSON.parse(data)
            // Check if this is a token event (has "content") or done event (has "sessionId")
            if ('content' in parsed) {
              streamingContent.value += parsed.content
              scrollToBottom()
            } else if ('sessionId' in parsed) {
              finalSessionId = parsed.sessionId
              finalReferences = parsed.references || []
            } else if ('message' in parsed) {
              throw new Error(parsed.message)
            }
          } catch (e) {
            if (e instanceof SyntaxError) {
              // Not JSON, skip
            } else {
              throw e
            }
          }
        }
      }
    }

    // Finalize: add assistant message
    if (streamingContent.value) {
      const assistantMsg: ChatMessageItem = {
        id: Date.now() + 1,
        role: 'assistant',
        messageType: finalReferences.length ? 'reference' : 'text',
        content: streamingContent.value,
        createTime: new Date().toISOString(),
        references: finalReferences
      }
      messages.value.push(assistantMsg)

      if (finalSessionId) {
        activeSessionId.value = finalSessionId
      }
    }

    await loadSessions()
  } catch (e: any) {
    if (e.name === 'AbortError') {
      // User cancelled
    } else {
      ElMessage.error('发送失败，请稍后重试')
      console.error('SSE error:', e)
    }
  } finally {
    streaming.value = false
    streamingContent.value = ''
    sending.value = false
    abortController = null
    scrollToBottom()
  }
}

const renderMarkdown = (value: string) => DOMPurify.sanitize(marked.parse(value || '') as string)

onMounted(async () => {
  await loadSessions(true)
})
</script>

<style scoped>
.bc-markdown :deep(p) {
  margin: 0 0 0.75rem;
}

.bc-markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.bc-markdown :deep(code) {
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  background: rgba(var(--bc-cyan-rgb, 85, 214, 190), 0.12);
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

.chat-cockpit {
  --bc-cyan-rgb: 85, 214, 190;
}

.signal-log,
.chat-console {
  background:
    radial-gradient(circle at 12% 0%, rgba(85, 214, 190, 0.18), transparent 32%),
    linear-gradient(155deg, rgba(var(--bc-accent-rgb), 0.08), transparent 34%),
    var(--bc-panel);
}

.signal-card {
  position: relative;
  min-height: 76px;
  overflow: hidden;
  border: 1px solid var(--bc-border-subtle);
  border-radius: var(--radius-md);
  background: var(--bc-surface-card);
  padding: 14px 14px 14px 18px;
  transition:
    border-color 160ms var(--ease-hard),
    background-color 160ms var(--ease-hard),
    box-shadow 160ms var(--ease-hard),
    transform 160ms var(--ease-hard);
}

.signal-card:hover,
.signal-card-active {
  border-color: rgba(85, 214, 190, 0.46);
  background: var(--bc-surface-card-hover);
  box-shadow: var(--bc-shadow-hover);
  transform: translateY(-1px);
}

.signal-card__rail {
  position: absolute;
  inset: 12px auto 12px 8px;
  width: 3px;
  border-radius: var(--radius-pill);
  background: rgba(148, 163, 184, 0.35);
}

.signal-card-active .signal-card__rail {
  background: linear-gradient(180deg, var(--bc-cyan), var(--bc-amber));
  box-shadow: 0 0 16px rgba(85, 214, 190, 0.32);
}

.signal-card__delete {
  min-height: 32px;
  border-radius: var(--radius-pill);
  padding: 6px 8px;
  color: rgb(148 163 184);
  font-size: 11px;
  transition: color 160ms var(--ease-hard), background-color 160ms var(--ease-hard);
}

.signal-card__delete:hover {
  background: rgba(255, 107, 107, 0.1);
  color: var(--bc-coral);
}

.signal-chip-rag {
  color: var(--bc-cyan) !important;
  background: rgba(85, 214, 190, 0.12) !important;
}

.signal-chip-chat {
  color: var(--bc-amber) !important;
  background: rgba(var(--bc-accent-rgb), 0.12) !important;
}

.mode-switch {
  min-width: min(100%, 300px);
  border: 1px solid var(--bc-border-subtle);
  border-radius: 999px;
  background: rgba(7, 17, 31, 0.08);
  padding: 4px;
}

.mode-switch__item {
  min-height: 44px;
  border-radius: 999px;
  padding: 0 14px;
  color: rgb(100 116 139);
  font-size: 12px;
  font-weight: 700;
  transition:
    background-color 160ms var(--ease-hard),
    color 160ms var(--ease-hard),
    transform 160ms var(--ease-hard);
}

.mode-switch__item-active {
  background: linear-gradient(180deg, rgba(85, 214, 190, 0.22), rgba(var(--bc-accent-rgb), 0.14));
  color: var(--bc-ink);
  box-shadow: inset 0 0 0 1px rgba(85, 214, 190, 0.28);
}

.message-bay {
  border: 1px solid var(--bc-border-subtle);
  border-radius: var(--radius-md);
  background:
    linear-gradient(rgba(var(--bc-ink-rgb), 0.035) 1px, transparent 1px),
    rgba(7, 17, 31, 0.04);
  background-size: 100% 44px, auto;
  padding: 12px;
}

.message-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--bc-border-subtle);
  box-shadow: var(--bc-shadow-soft);
}

.message-card-assistant {
  background:
    linear-gradient(145deg, rgba(85, 214, 190, 0.12), transparent 34%),
    var(--bc-surface-card);
}

.message-card-user {
  margin-left: clamp(0px, 8vw, 96px);
  background:
    linear-gradient(145deg, rgba(var(--bc-accent-rgb), 0.12), transparent 34%),
    var(--bc-surface-card);
}

.reference-stack,
.reference-deck,
.input-console {
  border: 1px solid var(--bc-border-subtle);
  border-radius: var(--radius-md);
  background: rgba(7, 17, 31, 0.05);
  padding: 12px;
}

.reference-card {
  border: 1px solid rgba(85, 214, 190, 0.2);
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(85, 214, 190, 0.12), transparent 48%),
    var(--bc-surface-card);
}

.reference-score {
  display: inline-flex;
  min-width: 46px;
  justify-content: center;
  border-radius: var(--radius-pill);
  background: rgba(85, 214, 190, 0.12);
  padding: 3px 7px;
  color: var(--bc-cyan);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-weight: 700;
}

.line-clamp-1,
.line-clamp-2,
.line-clamp-3,
.line-clamp-4 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-1 {
  -webkit-line-clamp: 1;
}

.line-clamp-2 {
  -webkit-line-clamp: 2;
}

.line-clamp-3 {
  -webkit-line-clamp: 3;
}

.line-clamp-4 {
  -webkit-line-clamp: 4;
}

@media (max-width: 640px) {
  .message-bay {
    max-height: none;
    padding: 8px;
  }

  .message-card-user {
    margin-left: 0;
  }

  .mode-switch {
    width: 100%;
  }
}

@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
