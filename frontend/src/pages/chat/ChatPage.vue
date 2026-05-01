<template>
  <div class="grid gap-4 xl:grid-cols-[320px_minmax(0,1fr)]">
    <section class="paper-panel p-5">
      <div class="flex items-center justify-between">
        <p class="section-kicker">Sessions</p>
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
              <p class="mt-2 text-sm leading-6 text-slate-500">{{ session.mode === 'rag' ? '知识库问答' : '普通问答' }}</p>
            </div>
            <button
              type="button"
              class="text-xs text-slate-400 transition hover:text-slate-700"
              @click.stop="removeSession(session.id)"
            >
              删除
            </button>
          </div>
        </article>
      </div>

      <div v-if="!sessions.length" class="empty-state-card mt-5">
        <div class="font-semibold text-ink">还没有会话</div>
        <p class="mt-2 text-sm leading-6 text-slate-500">直接发起第一个问题，系统会自动创建会话标题并开始沉淀历史。</p>
      </div>
    </section>

    <section class="paper-panel flex min-h-[580px] flex-col p-5">
      <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <p class="section-kicker">Chat</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">{{ currentTitle }}</h3>
        </div>
        <el-select v-model="mode" size="large" class="md:w-[180px]">
          <el-option label="知识库问答" value="rag" />
          <el-option label="普通问答" value="chat" />
        </el-select>
      </div>

      <div class="mt-5 flex-1 space-y-4 overflow-y-auto pr-1">
        <article
          v-for="message in messages"
          :key="message.id"
          class="p-4"
          style="border-radius: var(--radius-md);"
          :class="message.role === 'assistant' ? 'surface-card' : 'bg-accent text-white shadow-[0_14px_30px_rgba(47,79,157,0.14)]'"
        >
          <div class="text-xs uppercase tracking-[0.24em]" :class="message.role === 'assistant' ? 'text-slate-500' : 'text-white/60'">
            {{ message.role }}
          </div>
          <div class="bc-markdown mt-3 text-sm leading-7" v-html="renderMarkdown(message.content)"></div>

          <div v-if="message.references.length" class="surface-muted mt-4 space-y-2 px-3 py-3 text-xs text-slate-600">
            <div v-for="reference in message.references" :key="reference.chunkId" class="space-y-1">
              <div class="font-semibold text-slate-700">{{ reference.docTitle }}</div>
              <div>{{ reference.snippet }}</div>
            </div>
          </div>
        </article>

        <div v-if="loadingMessages" class="space-y-3">
          <div v-for="n in 2" :key="n" class="surface-card animate-pulse p-4">
            <div class="h-3 w-20 rounded bg-slate-200"></div>
            <div class="mt-3 h-4 w-full rounded bg-slate-100"></div>
            <div class="mt-2 h-4 w-4/5 rounded bg-slate-100"></div>
          </div>
        </div>

        <div v-if="!messages.length && !loadingMessages" class="empty-state-card">
          <div class="font-semibold text-ink">新会话已就绪</div>
          <p class="mt-2 text-sm leading-6 text-slate-500">可以先用知识库问答问一个具体主题，或者切到普通问答直接开问。</p>
        </div>
      </div>

      <div class="mt-4 grid gap-3 md:grid-cols-[minmax(0,1fr)_120px]">
        <el-input
          v-model="prompt"
          type="textarea"
          :rows="4"
          placeholder="输入你的问题。知识库问答会先检索 `knowledge_chunk`，普通问答直接发给 LLM。"
          @keydown.enter.exact.prevent="submitChat"
        />
        <el-button :loading="sending" type="primary" size="large" class="action-button transition active:translate-y-px" @click="submitChat">
          发送
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { computed, onMounted, ref } from 'vue'
import { deleteChatSessionApi, fetchChatMessagesApi, fetchChatSessionsApi, sendChatApi } from '@/api/chat'
import type { ChatMessageItem, ChatSessionItem } from '@/types/api'

const sessions = ref<ChatSessionItem[]>([])
const messages = ref<ChatMessageItem[]>([])
const activeSessionId = ref<number | null>(null)
const mode = ref<'chat' | 'rag'>('rag')
const prompt = ref('')
const loadingMessages = ref(false)
const sending = ref(false)

marked.setOptions({
  breaks: true,
  gfm: true
})

const currentTitle = computed(() => {
  if (!activeSessionId.value) return mode.value === 'rag' ? '新的知识库问答' : '新的普通问答'
  return sessions.value.find((item) => item.id === activeSessionId.value)?.title || '当前会话'
})

const loadSessions = async (selectLatest = false) => {
  const response = await fetchChatSessionsApi()
  sessions.value = response.data
  if (selectLatest && sessions.value.length) {
    await selectSession(sessions.value[0].id, sessions.value[0].mode)
  }
}

const loadMessages = async (sessionId: number) => {
  loadingMessages.value = true
  try {
    const response = await fetchChatMessagesApi(sessionId)
    messages.value = response.data
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
