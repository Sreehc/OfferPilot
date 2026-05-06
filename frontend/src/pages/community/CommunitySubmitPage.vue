<template>
  <div class="community-submit space-y-6">
    <button
      class="inline-flex items-center gap-2 text-sm text-slate-500 transition-colors hover:text-ink"
      @click="$router.back()"
    >
      <span>&larr;</span> 返回社区
    </button>

    <section class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div class="flex items-center gap-3">
            <span class="state-pulse" aria-hidden="true"></span>
            <p class="section-kicker">发起提问</p>
          </div>
          <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">把问题写清楚，更容易得到有效回答</h2>
          <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-600 dark:text-slate-300">
            写清背景、现象和已尝试的方法，更容易获得有效回答。
          </p>
        </div>
        <span class="quality-badge" :class="qualityBadgeClass">{{ qualityBadge }}</span>
      </div>

      <div class="mt-6 grid gap-3 md:grid-cols-3">
        <article v-for="tip in writingSignals" :key="tip.label" class="data-slab p-4" :class="tip.toneClass">
          <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ tip.label }}</p>
          <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ tip.value }}</p>
          <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ tip.detail }}</p>
        </article>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        class="mt-6"
        label-position="top"
        @submit.prevent
      >
        <div class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
          <div class="space-y-5">
            <el-form-item label="标题" prop="title">
              <template #label>
                <div class="form-label-row">
                  <span>标题</span>
                  <span class="text-xs text-slate-400">{{ form.title.length }}/200</span>
                </div>
              </template>
              <el-input
                v-model="form.title"
                maxlength="200"
                size="large"
                placeholder="例如：Spring 循环依赖里三级缓存的作用为什么不能被二级缓存完全替代？"
              />
            </el-form-item>

            <el-form-item label="问题内容" prop="content">
              <template #label>
                <div class="form-label-row">
                  <span>问题内容</span>
                  <span class="text-xs text-slate-400">{{ form.content.length }}/10000</span>
                </div>
              </template>
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="14"
                maxlength="10000"
                resize="none"
                placeholder="可按“背景 / 现象 / 已尝试方案”描述问题。"
              />
            </el-form-item>
          </div>

          <aside class="compose-side space-y-4">
            <article class="compose-note">
              <p class="text-sm font-semibold text-ink">提问前先检查</p>
              <div class="mt-4 space-y-3">
                <article v-for="check in qualityChecklist" :key="check.label" class="quality-check">
                  <div class="flex items-center justify-between gap-3">
                    <div class="flex items-center gap-3">
                      <span class="inline-flex h-2.5 w-2.5 rounded-full" :class="check.ok ? 'bg-[var(--bc-lime)]' : 'bg-[var(--bc-coral)]'"></span>
                      <span class="text-sm text-ink">{{ check.label }}</span>
                    </div>
                    <span class="text-xs font-semibold" :class="check.ok ? 'text-[var(--bc-lime)]' : 'text-[var(--bc-coral)]'">
                      {{ check.ok ? '已满足' : '待补充' }}
                    </span>
                  </div>
                  <p class="mt-1 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ check.detail }}</p>
                </article>
              </div>
            </article>

            <article class="compose-note">
              <p class="text-sm font-semibold text-ink">推荐结构</p>
              <div class="mt-4 space-y-3">
                <article v-for="section in promptSections" :key="section.title" class="prompt-lane">
                  <div class="flex items-start justify-between gap-3">
                    <div>
                      <p class="text-sm font-semibold text-ink">{{ section.title }}</p>
                      <p class="mt-1 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ section.description }}</p>
                    </div>
                  </div>
                </article>
              </div>
            </article>

            <article class="compose-note">
              <p class="text-sm font-semibold text-ink">写作提醒</p>
              <ul class="mt-3 space-y-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
                <li>说明你在哪个知识点、项目或面试情境里遇到这个问题。</li>
                <li>写清你已经尝试过哪些分析，而不是只问“怎么做”。</li>
                <li>如果有多个疑点，优先保留最关键的一个。</li>
              </ul>
            </article>

            <article class="compose-note">
              <p class="text-sm font-semibold text-ink">示例开场</p>
              <div class="mt-3 rounded-[18px] bg-slate-100/80 p-4 text-sm leading-7 text-slate-600 dark:bg-slate-900/50 dark:text-slate-300">
                <p>背景：我在准备 Spring Boot 面试，自己实现一个简化版 IOC 容器。</p>
                <p>现象：循环依赖场景下，二级缓存和三级缓存的职责总是解释不清。</p>
                <p>已尝试：看过源码和几篇文章，但仍无法把创建流程串成完整叙事。</p>
              </div>
            </article>

            <article class="compose-note">
              <p class="text-sm font-semibold text-ink">发布后会怎样</p>
              <div class="mt-3 space-y-3">
                <div v-for="step in publishSteps" :key="step.title" class="publish-step">
                  <span class="publish-step__index">{{ step.index }}</span>
                  <div>
                    <p class="text-sm font-semibold text-ink">{{ step.title }}</p>
                    <p class="text-xs leading-6 text-slate-500 dark:text-slate-400">{{ step.detail }}</p>
                  </div>
                </div>
              </div>
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
            :disabled="!canSubmit || submitting"
            @click="handleSubmit"
          >
            {{ submitting ? '提交中...' : '发布问题' }}
          </button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createCommunityQuestionApi } from '@/api/community'
import { useFormRules } from '@/composables/useFormRules'

const router = useRouter()
const formRef = ref<FormInstance>()
const { presets } = useFormRules()

const form = reactive({
  title: '',
  content: '',
})

const submitting = ref(false)

const formRules: FormRules<typeof form> = {
  title: presets.title,
  content: presets.content,
}

const titleScore = computed(() => {
  const length = form.title.trim().length
  if (length >= 36) return 35
  if (length >= 20) return 26
  if (length >= 10) return 16
  if (length > 0) return 8
  return 0
})

const contentScore = computed(() => {
  const length = form.content.trim().length
  if (length >= 280) return 35
  if (length >= 160) return 28
  if (length >= 80) return 18
  if (length > 0) return 10
  return 0
})

const hasContextKeywords = computed(() => /背景|场景|项目|面试|环境/.test(form.content))
const hasProblemKeywords = computed(() => /问题|现象|报错|异常|卡住|不会/.test(form.content))
const hasAttemptKeywords = computed(() => /尝试|排查|分析|看过|验证/.test(form.content))

const qualityScore = computed(() => {
  const structureBonus = [hasContextKeywords.value, hasProblemKeywords.value, hasAttemptKeywords.value].filter(Boolean).length * 10
  return Math.min(100, titleScore.value + contentScore.value + structureBonus)
})

const qualityBadge = computed(() => {
  if (qualityScore.value >= 80) return '可发布'
  if (qualityScore.value >= 55) return '待补充'
  return '草稿中'
})

const qualityBadgeClass = computed(() => {
  if (qualityScore.value >= 80) return 'quality-badge-ready'
  if (qualityScore.value >= 55) return 'quality-badge-improving'
  return 'quality-badge-draft'
})

const canSubmit = computed(() => form.title.trim().length > 0 && form.content.trim().length > 0)

const qualityChecklist = computed(() => [
  {
    label: '说明了问题背景',
    ok: hasContextKeywords.value,
    detail: '最好交代这是面试题、项目场景还是源码理解问题。',
  },
  {
    label: '描述了具体现象',
    ok: hasProblemKeywords.value,
    detail: '回答者需要知道你到底卡在哪里，而不是只看到一个抽象概念。',
  },
  {
    label: '写了已尝试方案',
    ok: hasAttemptKeywords.value,
    detail: '补上你已经查过什么，避免社区重复给出你已经知道的建议。',
  },
])

const writingSignals = computed(() => [
  {
    label: '标题字数',
    value: form.title.trim().length || 0,
    detail: '越具体，越容易吸引真正理解该问题的人回答。',
    toneClass: '',
  },
  {
    label: '内容段落',
    value: form.content.trim().split('\n').filter(Boolean).length,
    detail: '建议至少写出背景、现象两个段落。',
    toneClass: 'submit-slab-cyan',
  },
  {
    label: '完整度',
    value: qualityScore.value,
    detail: '80 分以上通常已经具备可讨论的基础。',
    toneClass: 'submit-slab-lime',
  },
])

const promptSections = [
  {
    title: '背景',
    description: '你在准备哪一类面试、看哪段源码、或在什么项目场景里遇到了这个问题。',
  },
  {
    title: '现象',
    description: '你具体卡在哪一步，是无法解释原理、还是逻辑链条断了，或者结果和预期不一致。',
  },
  {
    title: '已尝试方案',
    description: '列出你已经查过、试过或仍然不能说服自己的部分，帮助回答者直接进入关键分歧点。',
  },
]

const publishSteps = [
  { index: '01', title: '进入问题流', detail: '你的问题会立刻出现在社区热区中。' },
  { index: '02', title: '等待回答', detail: '其他用户可以围绕你的问题继续补充、点赞和解答。' },
  { index: '03', title: '沉淀最佳答案', detail: '采纳回答后，这个问题会变成可复盘的社区资产。' },
]

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const { data } = await createCommunityQuestionApi({
      title: form.title.trim(),
      content: form.content.trim(),
    })
    await router.push(`/community/question/${data}`)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.prompt-lane,
.quality-check,
.compose-note {
  border-radius: 20px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.34);
  padding: 14px 16px;
}

.dark .prompt-lane,
.dark .quality-check,
.dark .compose-note {
  background: rgba(255, 255, 255, 0.05);
}

.quality-gauge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 86px;
  height: 86px;
  border-radius: 999px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.24);
  background:
    radial-gradient(circle at center, rgba(var(--bc-accent-rgb), 0.18), transparent 62%),
    rgba(255, 255, 255, 0.38);
}

.dark .quality-gauge {
  background:
    radial-gradient(circle at center, rgba(var(--bc-accent-rgb), 0.2), transparent 62%),
    rgba(255, 255, 255, 0.05);
}

.quality-gauge__value {
  font-family: theme('fontFamily.mono');
  font-size: 26px;
  font-weight: 700;
  color: var(--bc-ink);
}

.quality-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 700;
}

.quality-badge-ready {
  background: rgba(159, 232, 112, 0.14);
  color: var(--bc-lime);
}

.quality-badge-improving {
  background: rgba(255, 183, 77, 0.14);
  color: var(--bc-amber);
}

.quality-badge-draft {
  background: rgba(140, 166, 191, 0.14);
  color: var(--bc-ink-secondary);
}

.submit-slab-cyan {
  border-left-color: var(--bc-cyan);
}

.submit-slab-lime {
  border-left-color: var(--bc-lime);
}

.question-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid var(--bc-line);
  padding: 6px 10px;
  font-size: 11px;
  color: var(--bc-ink-secondary);
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

.publish-step {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.publish-step__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  height: 34px;
  border-radius: 999px;
  background: rgba(var(--bc-accent-rgb), 0.14);
  color: var(--bc-accent);
  font-family: theme('fontFamily.mono');
  font-size: 11px;
  font-weight: 700;
}

@media (max-width: 1280px) {
  .compose-side {
    order: -1;
  }
}
</style>
