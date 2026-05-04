<template>
  <div class="grid gap-4 xl:grid-cols-[320px_minmax(0,1fr)]">
    <section class="paper-panel p-5">
      <div class="flex items-center justify-between">
        <p class="section-kicker">会话列表</p>
        <button type="button" class="hard-button-secondary !min-h-9 !px-3 !py-1 text-xs" @click="startNewSession">
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

      <div v-if="!sessions.length" class="empty-state-card mt-5">
        <div class="font-semibold text-ink">还没有会话</div>
        <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">直接发起第一个问题，系统会自动创建会话标题并开始沉淀历史。</p>
      </div>

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
          :class="message.role === 'assistant' ? 'surface-card' : 'bg-accent text-white shadow-[0_14px_30px_rgba(47,79,157,0.14)]'"
        >
          <div class="text-xs uppercase tracking-[0.24em]" :class="message.role === 'assistant' ? 'text-slate-500 dark:text-slate-400' : 'text-white/60'">
            {{ message.role }}
          </div>
          <div class="bc-markdown mt-3 text-sm leading-7" v-html="renderMarkdown(message.content)"></div>

          <div v-if="message.references.length" class="surface-muted mt-4 space-y-2 px-3 py-3 text-xs text-slate-600 dark:text-slate-300">
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

        <div v-if="loadingMessages" class="space-y-3">
          <div v-for="n in 2" :key="n" class="surface-card animate-pulse p-4">
            <div class="h-3 w-20 rounded bg-slate-200 dark:bg-slate-700"></div>
            <div class="mt-3 h-4 w-full rounded bg-slate-100 dark:bg-slate-800"></div>
            <div class="mt-2 h-4 w-4/5 rounded bg-slate-100 dark:bg-slate-800"></div>
          </div>
        </div>

        <div v-if="!messages.length && !loadingMessages" class="empty-state-card">
          <div class="font-semibold text-ink">新会话已就绪</div>
          <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">可以先用知识库问答问一个具体主题，或者切到普通问答直接开问。</p>
        </div>
      </div>

      <div class="mt-4 grid gap-3 md:grid-cols-[minmax(0,1fr)_120px]">
        <el-input
          v-model="prompt"
          type="textarea"
          :rows="4"
          placeholder="输入你的问题。知识库问答会先检索相关资料再回答，普通问答直接由 AI 回答。"
          @keydown.enter.exact.prevent="submitChat"
        />
        <div class="flex flex-col gap-2">
          <el-button :loading="sending" type="primary" size="large" class="action-button transition active:translate-y-px" @click="submitChat">
            发送
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
import { deleteChatSessionApi, fetchChatMessagesApi, fetchChatSessionsApi, sendChatApi } from '@/api/chat'
import type { ChatMessageItem, ChatSessionItem } from '@/types/api'

const sessions = ref<ChatSessionItem[]>([])
const messages = ref<ChatMessageItem[]>([])
const activeSessionId = ref<number | null>(null)
const mode = ref<'chat' | 'rag'>('rag')
const prompt = ref('')
const loadingMessages = ref(false)
const sending = ref(false)
const messageContainer = ref<HTMLElement | null>(null)

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
  if (!prompt.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  sending.value = true
  try {
    const response = await sendChatApi({
      sessionId: activeSessionId.value ?? undefined,
      mode: mode.value,
      message: prompt.value.trim()
    })
    activeSessionId.value = response.data.sessionId
    prompt.value = ''
    await loadSessions()
    await loadMessages(response.data.sessionId)
    scrollToBottom()
  } catch {
    ElMessage.error('问答发送失败')
  } finally {
    sending.value = false
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
</style>
