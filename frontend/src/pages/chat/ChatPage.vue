<template>
  <div class="grid gap-4 lg:grid-cols-[280px_minmax(0,1fr)]">
    <section class="paper-panel p-5">
      <div class="flex items-center justify-between">
        <p class="section-kicker">会话列表</p>
        <button type="button" class="hard-button-secondary !min-h-10 !px-3 !py-1.5 text-xs" @click="startNewSession">
          新会话
        </button>
      </div>

      <div class="mt-5 space-y-3">
        <article
          v-for="session in sessions"
          :key="session.id"
          class="surface-card surface-card-hover w-full cursor-pointer px-4 py-4 text-left"
          :class="activeSessionId === session.id ? 'ring-1 ring-accent/20' : ''"
          @click="selectSession(session.id, session.mode)"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <div class="truncate font-semibold">{{ session.title }}</div>
              <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">{{ session.mode === 'rag' ? '知识库问答' : '普通问答' }}</p>
            </div>
            <button
              type="button"
              class="text-xs text-slate-400 dark:text-slate-500 transition hover:text-slate-700 dark:hover:text-slate-300"
              @click.stop="removeSession(session.id)"
            >
              删除
            </button>
          </div>
        </article>
      </div>

      <EmptyState
        v-if="!sessions.length"
        icon="chat"
        title="还没有会话"
        description="直接发起第一个问题，系统会自动创建会话标题并开始沉淀历史。"
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
    </section>

    <section class="paper-panel flex min-h-[580px] flex-col p-5">
      <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <p class="section-kicker">对话</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">{{ currentTitle }}</h3>
        </div>
        <el-select v-model="mode" size="large" class="md:w-[180px]">
          <el-option label="知识库问答" value="rag" />
          <el-option label="普通问答" value="chat" />
        </el-select>
      </div>

      <div ref="messageContainer" class="mt-5 flex-1 space-y-4 overflow-y-auto pr-1">
        <article
          v-for="message in messages"
          :key="message.id"
          class="p-4"
          style="border-radius: var(--radius-md);"
          :class="message.role === 'assistant' ? 'surface-card border-l-2 border-l-accent' : 'bg-slate-100 dark:bg-slate-800 text-ink'"
        >
          <div class="text-xs uppercase tracking-[0.24em]" :class="message.role === 'assistant' ? 'text-slate-500 dark:text-slate-400' : 'text-slate-400 dark:text-slate-500'">
            {{ message.role === 'assistant' ? 'AI 助手' : '你' }}
          </div>
          <div class="bc-markdown mt-3 text-sm leading-7" v-html="renderMarkdown(message.content)"></div>

          <div v-if="message.references && message.references.length" class="surface-muted mt-4 space-y-2 px-3 py-3 text-xs text-slate-600 dark:text-slate-300">
            <div class="mb-1 font-semibold text-slate-500 dark:text-slate-400">知识库引用</div>
            <div v-for="reference in message.references" :key="reference.chunkId" class="space-y-1 rounded-md border border-slate-200 dark:border-slate-700 bg-white/60 dark:bg-slate-800/60 px-3 py-2">
              <div class="flex items-center justify-between">
                <div class="font-semibold text-slate-700 dark:text-slate-200">{{ reference.docTitle }}</div>
                <span v-if="reference.score != null" class="rounded-full bg-accent/10 px-2 py-0.5 text-[10px] font-medium text-accent">
                  {{ Math.round(reference.score * 100) }}%
                </span>
              </div>
              <div class="leading-relaxed">{{ reference.snippet }}</div>
            </div>
          </div>
        </article>

        <!-- Streaming message -->
        <article v-if="streaming" class="surface-card border-l-2 border-l-accent p-4" style="border-radius: var(--radius-md);">
          <div class="text-xs uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">AI 助手</div>
          <div class="bc-markdown mt-3 text-sm leading-7">
            <span v-html="renderMarkdown(streamingContent)"></span>
            <span class="streaming-cursor"></span>
          </div>
        </article>

        <div v-if="loadingMessages" class="space-y-3">
          <div v-for="n in 2" :key="n" class="surface-card animate-pulse p-4">
            <div class="h-3 w-20 rounded bg-slate-200 dark:bg-slate-700"></div>
            <div class="mt-3 h-4 w-full rounded bg-slate-100 dark:bg-slate-800"></div>
            <div class="mt-2 h-4 w-4/5 rounded bg-slate-100 dark:bg-slate-800"></div>
          </div>
        </div>

        <EmptyState
          v-if="!messages.length && !loadingMessages && !streaming"
          icon="chat"
          title="开始你的第一个问题"
          description="试试问一个 Java 面试题，或者切到知识库问答检索学习资料。"
          compact
        />
      </div>

      <div class="mt-4 grid gap-3 md:grid-cols-[minmax(0,1fr)_120px]">
        <el-input
          v-model="prompt"
          type="textarea"
          :rows="4"
          placeholder="输入你的问题。知识库问答会先检索相关资料再回答，普通问答直接由 AI 回答。"
          :disabled="streaming"
          @keydown.enter.exact.prevent="submitChat"
        />
        <div class="flex flex-col gap-2">
          <el-button :loading="sending" :disabled="streaming" type="primary" size="large" class="action-button transition active:translate-y-px" @click="submitChat">
            {{ streaming ? '回答中...' : '发送' }}
          </el-button>
          <span class="text-center text-xs text-slate-400 dark:text-slate-500">Enter 发送</span>
        </div>
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
  if (!activeSessionId.value) return mode.value === 'rag' ? '新的知识库问答' : '新的普通问答'
  return sessions.value.find((item) => item.id === activeSessionId.value)?.title || '当前会话'
})

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
    ElMessage.error('消息历史加载失败')
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
      ElMessage.error('问答发送失败')
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
  background: rgba(15, 23, 42, 0.08);
  font-size: 0.92em;
}

.bc-markdown :deep(pre) {
  overflow-x: auto;
  margin: 0.75rem 0;
  padding: 0.9rem 1rem;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.06);
}

.bc-markdown :deep(ul),
.bc-markdown :deep(ol) {
  padding-left: 1.25rem;
}

.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: var(--accent);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: cursor-blink 1s step-end infinite;
}

@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
