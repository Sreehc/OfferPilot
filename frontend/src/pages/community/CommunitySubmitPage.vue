<template>
  <div class="community-submit space-y-6">
    <button
      class="inline-flex items-center gap-2 text-sm text-secondary transition-colors hover:text-ink"
      @click="$router.back()"
    >
      <span>&larr;</span> 返回社区
    </button>

    <section class="shell-section-card p-5 sm:p-6">
      <div v-if="loading" class="pb-5 text-sm text-tertiary">
        正在加载帖子内容...
      </div>
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top" @submit.prevent>
        <div class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
          <div class="space-y-5">
            <el-form-item label="标题" prop="title">
              <template #label>
                <div class="form-label-row">
                  <span>标题</span>
                  <span class="text-xs text-tertiary">{{ form.title.length }}/200</span>
                </div>
              </template>
              <el-input
                v-model="form.title"
                maxlength="200"
                size="large"
                placeholder="例如：Spring 循环依赖为什么要三级缓存"
              />
            </el-form-item>

            <el-form-item label="问题内容" prop="content">
              <template #label>
                <div class="form-label-row">
                  <span>问题内容</span>
                  <span class="text-xs text-tertiary">{{ form.content.length }}/10000</span>
                </div>
              </template>
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="14"
                maxlength="10000"
                resize="none"
                placeholder="描述问题，并补充你已经尝试过的内容"
              />
            </el-form-item>
          </div>

          <aside class="compose-side space-y-4">
            <article class="compose-note">
              <p class="text-sm font-semibold text-ink">描述问题，并补充已尝试内容</p>
            </article>
          </aside>
        </div>

        <div class="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            class="hard-button-secondary"
            @click="$router.back()"
          >
            取消
          </button>
          <button
            type="button"
            class="hard-button-primary"
            :disabled="loading || !canSubmit || submitting"
            @click="handleSubmit"
          >
            {{ submitting ? '提交中...' : submitButtonText }}
          </button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createCommunityQuestionApi, fetchCommunityQuestionDetailApi, updateCommunityQuestionApi } from '@/api/community'
import { ERROR_COPY } from '@/constants/productCopy'
import { useFormRules } from '@/composables/useFormRules'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const { presets } = useFormRules()

const form = reactive({
  title: '',
  content: '',
})

const submitting = ref(false)
const loading = ref(false)
const editingQuestionId = computed(() => {
  const id = Number(route.query.id)
  return Number.isFinite(id) && id > 0 ? id : null
})
const submitButtonText = computed(() => {
  if (submitting.value) return '提交中...'
  return editingQuestionId.value ? '保存修改' : '发布问题'
})

const formRules: FormRules<typeof form> = {
  title: presets.title,
  content: presets.content,
}

const canSubmit = computed(() => form.title.trim().length > 0 && form.content.trim().length > 0)

async function loadEditingQuestion() {
  if (!editingQuestionId.value) return
  loading.value = true
  try {
    const { data } = await fetchCommunityQuestionDetailApi(editingQuestionId.value)
    form.title = data.title || ''
    form.content = data.content || ''
  } catch {
    ElMessage.error(ERROR_COPY.communityQuestionEditLoadFailed)
    await router.push('/community')
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (editingQuestionId.value) {
      await updateCommunityQuestionApi({
        id: editingQuestionId.value,
        title: form.title.trim(),
        content: form.content.trim()
      })
      ElMessage.success('帖子已更新')
      await router.push(`/community/question/${editingQuestionId.value}`)
      return
    }

    const { data } = await createCommunityQuestionApi({
      title: form.title.trim(),
      content: form.content.trim(),
    })
    await router.push(`/community/question/${data}`)
  } catch {
    ElMessage.error(ERROR_COPY.communityQuestionSaveFailed)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void loadEditingQuestion()
})
</script>

<style scoped>
.compose-note {
  border-radius: 20px;
  border: 1px solid var(--bc-line);
  background: var(--panel-muted);
  padding: 14px 16px;
}

.dark .compose-note {
  background: var(--panel-muted);
}

.form-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 12px;
}

.compose-side {
  min-width: 0;
}

@media (max-width: 1280px) {
  .compose-side {
    order: -1;
  }
}
</style>
