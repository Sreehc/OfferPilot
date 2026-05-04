<template>
  <div class="max-w-3xl mx-auto space-y-6">
    <button
      class="flex items-center gap-2 text-sm text-slate-500 hover:text-ink transition-colors"
      @click="$router.back()"
    >
      <span>&larr;</span> 返回
    </button>

    <div class="paper-panel px-8 py-8">
      <h2 class="text-xl font-semibold text-ink mb-6">发起提问</h2>

      <div class="space-y-5">
        <div>
          <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">标题</label>
          <input
            v-model="form.title"
            type="text"
            placeholder="简明扼要地描述你的问题"
            maxlength="200"
            class="w-full px-4 py-3 text-sm bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-accent/30"
          />
          <p class="mt-1 text-xs text-slate-400">{{ form.title.length }}/200</p>
        </div>

        <div>
          <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">内容</label>
          <textarea
            v-model="form.content"
            rows="12"
            placeholder="详细描述你的问题，包括背景、代码、错误信息等..."
            maxlength="10000"
            class="w-full px-4 py-3 text-sm bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-accent/30 resize-none"
          ></textarea>
          <p class="mt-1 text-xs text-slate-400">{{ form.content.length }}/10000</p>
        </div>

        <div class="flex justify-end gap-3 pt-2">
          <button
            class="px-5 py-2.5 text-sm font-semibold rounded-lg border border-slate-300 dark:border-slate-600 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors"
            @click="$router.back()"
          >
            取消
          </button>
          <button
            class="hard-button-primary px-5 py-2.5 text-sm disabled:opacity-50"
            :disabled="!canSubmit || submitting"
            @click="handleSubmit"
          >
            {{ submitting ? '提交中...' : '发布问题' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { createCommunityQuestionApi } from '@/api/community'

const router = useRouter()

const form = ref({
  title: '',
  content: '',
})

const submitting = ref(false)

const canSubmit = computed(() => form.value.title.trim().length > 0 && form.value.content.trim().length > 0)

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const { data } = await createCommunityQuestionApi({
      title: form.value.title.trim(),
      content: form.value.content.trim(),
    })
    router.push(`/community/question/${data}`)
  } finally {
    submitting.value = false
  }
}
</script>
