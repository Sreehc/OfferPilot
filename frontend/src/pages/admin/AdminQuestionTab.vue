<template>
  <div class="admin-tool-grid">
    <aside class="space-y-4">
      <section class="surface-muted p-4">
        <p class="section-kicker">筛选与动作</p>
        <div class="mt-4 grid gap-3">
          <el-select v-model="filter.categoryId" clearable placeholder="按分类" size="large">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="filter.type" clearable placeholder="按题型" size="large">
            <el-option label="八股题" value="concept" />
            <el-option label="场景题" value="scenario" />
            <el-option label="项目题" value="project" />
            <el-option label="算法题" value="coding" />
          </el-select>
          <el-select v-model="filter.difficulty" clearable placeholder="按难度" size="large">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
          <el-input v-model="filter.jobDirection" clearable placeholder="岗位方向，如 Java 后端" size="large" />
          <el-input v-model="filter.keyword" clearable placeholder="搜索题目" size="large" />
        </div>

        <div class="mt-4 grid gap-3">
          <el-button :loading="loading" type="primary" class="action-button" @click="emit('load')">刷新列表</el-button>
          <el-button class="hard-button-secondary" @click="emit('filterReset')">重置筛选</el-button>
          <el-button class="hard-button-secondary" @click="toggleEditor">
            {{ editorOpen ? '收起编辑区' : form.id ? '继续编辑' : '新增题目' }}
          </el-button>
          <el-upload :show-file-list="false" :before-upload="handleImport" accept=".xlsx,.xls">
            <el-button :loading="importing" type="success" plain class="w-full">批量导入</el-button>
          </el-upload>
        </div>
      </section>

      <section v-if="editorOpen" class="surface-card p-4">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div class="text-sm font-semibold text-ink">{{ form.id ? '编辑题目' : '新增题目' }}</div>
          <button type="button" class="text-sm text-slate-500 transition hover:text-ink" @click="closeEditor">收起</button>
        </div>

        <div class="mt-4 grid gap-3">
          <el-input v-model="form.title" placeholder="标题" size="large" />
          <el-select v-model="form.categoryId" placeholder="分类" size="large" class="w-full">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="form.type" placeholder="题目类型" size="large" class="w-full">
            <el-option label="八股题" value="concept" />
            <el-option label="场景题" value="scenario" />
            <el-option label="项目题" value="project" />
            <el-option label="算法题" value="coding" />
          </el-select>
          <el-select v-model="form.difficulty" placeholder="难度" size="large" class="w-full">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
          <el-input-number
            v-model="form.frequency"
            :min="0"
            :max="100"
            placeholder="高频度"
            size="large"
            class="w-full"
          />
          <el-input v-model="form.jobDirection" placeholder="岗位方向，如 Java 后端 / 校招" size="large" />
          <el-input v-model="form.applicableScope" placeholder="适用范围，如 实习 / 初级社招 / 中间件专项" size="large" />
          <el-input v-model="form.tags" placeholder="标签" size="large" />
          <el-input v-model="form.source" placeholder="题目来源，如 真题整理 / AI 扩写" size="large" />
          <el-input v-model="form.standardAnswer" type="textarea" :rows="8" placeholder="标准答案" />
          <el-input v-model="form.interviewAnswer" type="textarea" :rows="6" placeholder="面试版回答" />
          <el-input v-model="form.followUpSuggestions" type="textarea" :rows="5" placeholder="追问建议" />
          <el-input v-model="form.commonMistakes" type="textarea" :rows="5" placeholder="常见错误回答" />
          <el-input v-model="form.scoreStandard" type="textarea" :rows="5" placeholder="评分标准" />
        </div>
        <div class="mt-4 flex flex-wrap gap-3">
          <el-button :loading="saving" type="primary" class="action-button" @click="emit('save')">
            {{ form.id ? '保存修改' : '新增题目' }}
          </el-button>
          <el-button class="hard-button-secondary" @click="emit('reset')">重置表单</el-button>
        </div>
      </section>
    </aside>

    <section class="space-y-4">
      <header class="shell-section-card p-5">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p class="section-kicker">题库列表</p>
            <h3 class="admin-section-title mt-3">共 {{ total }} 题</h3>
          </div>
          <div class="text-sm text-slate-500">按分类、难度或关键词筛选</div>
        </div>
      </header>

      <section class="surface-card overflow-hidden">
        <div v-if="questions.length" class="divide-y divide-slate-200/70 dark:divide-slate-700/70">
          <article v-for="item in questions" :key="item.id" class="admin-record">
            <div class="min-w-0">
              <div class="font-semibold text-ink">{{ item.title }}</div>
              <div class="mt-2 flex flex-wrap gap-2 text-xs uppercase tracking-[0.2em] text-secondary">
                <span>{{ item.categoryName }}</span>
                <span>{{ questionTypeLabel(item.type) }}</span>
                <span>{{ item.difficulty === 'easy' ? '简单' : item.difficulty === 'medium' ? '中等' : '困难' }}</span>
                <span v-if="item.frequency">高频度 {{ item.frequency }}</span>
                <span v-if="item.jobDirection">{{ item.jobDirection }}</span>
              </div>
              <p v-if="item.interviewAnswer" class="mt-3 text-sm leading-6 text-secondary">
                {{ preview(item.interviewAnswer) }}
              </p>
            </div>
            <div class="admin-record__actions">
              <button type="button" class="accent-link text-sm font-semibold" @click="emit('edit', item)">编辑</button>
              <button type="button" class="text-sm text-slate-500 transition hover:text-red-500" @click="emit('remove', item.id)">删除</button>
            </div>
          </article>
        </div>
        <div v-else class="px-5 py-12 text-center text-sm text-slate-500">
          暂无题目，先新增一条或导入题库。
        </div>
      </section>

      <div v-if="totalPages > 1" class="mt-4 flex justify-center">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="(page: number) => emit('pageChange', page)"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { ref, watch } from 'vue'
import type { CategoryItem, QuestionItem } from '@/types/api'
import { importQuestionsApi } from '@/api/admin'

interface QuestionForm {
  id?: number
  title: string
  categoryId?: number
  type?: string
  difficulty: QuestionItem['difficulty']
  frequency: number | null
  jobDirection: string
  applicableScope: string
  tags: string
  standardAnswer: string
  interviewAnswer: string
  followUpSuggestions: string
  commonMistakes: string
  scoreStandard: string
  source: string
}

interface QuestionFilter {
  categoryId?: number
  type?: string
  difficulty?: QuestionItem['difficulty']
  jobDirection: string
  keyword: string
}

const props = defineProps<{
  questions: QuestionItem[]
  categories: CategoryItem[]
  form: QuestionForm
  filter: QuestionFilter
  saving: boolean
  loading: boolean
  currentPage: number
  pageSize: number
  total: number
  totalPages: number
}>()

const currentPage = defineModel<number>('currentPage', { default: 1 })
const editorOpen = ref(false)

const emit = defineEmits<{
  save: []
  edit: [item: QuestionItem]
  remove: [id: number]
  reset: []
  filterReset: []
  load: []
  pageChange: [page: number]
}>()

const importing = defineModel<boolean>('importing', { default: false })

watch(
  () => props.form.id,
  (id) => {
    if (id) editorOpen.value = true
  }
)

const toggleEditor = () => {
  if (editorOpen.value) {
    closeEditor()
    return
  }
  editorOpen.value = true
  if (!props.form.id) emit('reset')
}

const closeEditor = () => {
  editorOpen.value = false
  emit('reset')
}

const handleImport = async (file: File) => {
  importing.value = true
  try {
    const res = await importQuestionsApi(file)
    const { successCount, failCount, errors } = res.data
    if (failCount > 0) {
      ElMessage.warning(`导入完成：成功 ${successCount} 条，失败 ${failCount} 条`)
      console.warn('Import errors:', errors)
    } else {
      ElMessage.success(`导入成功：共 ${successCount} 条`)
    }
    emit('load')
  } catch {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
  return false
}

const questionTypeLabel = (type?: string) => {
  if (type === 'concept') return '八股题'
  if (type === 'scenario') return '场景题'
  if (type === 'project') return '项目题'
  if (type === 'coding') return '算法题'
  return '综合题'
}

const preview = (value?: string) => {
  if (!value?.trim()) return ''
  return value.length > 90 ? `${value.slice(0, 90)}...` : value
}
</script>

<style scoped>
.admin-tool-grid {
  display: grid;
  gap: 20px;
}

.admin-section-title {
  color: var(--bc-ink);
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.admin-record {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
}

.admin-record__actions {
  display: flex;
  flex-shrink: 0;
  gap: 12px;
}

@media (min-width: 1280px) {
  .admin-tool-grid {
    grid-template-columns: 288px minmax(0, 1fr);
    align-items: start;
  }
}

@media (max-width: 767px) {
  .admin-record {
    flex-direction: column;
  }

  .admin-record__actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
