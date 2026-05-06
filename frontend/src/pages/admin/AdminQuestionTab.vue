<template>
  <div class="grid gap-4 xl:grid-cols-[380px_minmax(0,1fr)]">
    <div class="surface-muted p-4">
      <div class="text-xs text-slate-500 dark:text-slate-400">左侧维护题目内容，右侧筛选并浏览现有题目。</div>
      <div class="text-sm font-semibold text-ink">{{ form.id ? '编辑题目' : '新增题目' }}</div>
      <div class="mt-4 space-y-3">
        <el-input v-model="form.title" placeholder="题目标题" size="large" />
        <el-select v-model="form.categoryId" placeholder="题目分类" size="large" class="w-full">
          <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="form.difficulty" placeholder="难度" size="large" class="w-full">
          <el-option label="简单" value="easy" />
          <el-option label="中等" value="medium" />
          <el-option label="困难" value="hard" />
        </el-select>
        <el-input v-model="form.tags" placeholder="标签，例如：Spring,AOP" size="large" />
        <el-input v-model="form.standardAnswer" type="textarea" :rows="5" placeholder="标准答案" />
      </div>
      <div class="mt-4 flex gap-3">
        <el-button :loading="saving" type="primary" class="action-button" @click="emit('save')">
          {{ form.id ? '保存修改' : '新增题目' }}
        </el-button>
        <el-button class="hard-button-secondary" @click="emit('reset')">重置</el-button>
      </div>
    </div>

    <div class="space-y-4">
      <div class="text-sm text-slate-500 dark:text-slate-400">先筛选，再编辑或删除。</div>
      <div class="grid gap-3 md:grid-cols-3">
        <el-select v-model="filter.categoryId" clearable placeholder="分类筛选" size="large">
          <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="filter.difficulty" clearable placeholder="难度筛选" size="large">
          <el-option label="简单" value="easy" />
          <el-option label="中等" value="medium" />
          <el-option label="困难" value="hard" />
        </el-select>
        <el-input v-model="filter.keyword" clearable placeholder="关键字" size="large" />
      </div>

      <div class="flex gap-3">
        <el-button :loading="loading" type="primary" class="action-button" @click="emit('load')">刷新题库</el-button>
        <el-button class="hard-button-secondary" @click="emit('filterReset')">重置筛选</el-button>
        <el-upload
          :show-file-list="false"
          :before-upload="handleImport"
          accept=".xlsx,.xls"
        >
          <el-button :loading="importing" type="success" plain>批量导入</el-button>
        </el-upload>
      </div>

      <article v-for="item in questions" :key="item.id" class="surface-card p-4">
        <div class="flex items-start justify-between gap-4">
          <div class="min-w-0">
            <div class="font-semibold text-ink">{{ item.title }}</div>
            <div class="mt-2 flex flex-wrap gap-2 text-xs uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
              <span>{{ item.categoryName }}</span>
              <span>{{ item.difficulty === 'easy' ? '简单' : item.difficulty === 'medium' ? '中等' : '困难' }}</span>
            </div>
            <p class="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300">{{ item.standardAnswer || '暂无标准答案' }}</p>
          </div>
          <div class="flex shrink-0 gap-2">
            <button type="button" class="accent-link text-sm font-semibold" @click="emit('edit', item)">编辑</button>
            <button type="button" class="text-sm text-slate-500 dark:text-slate-400 transition hover:text-red-500" @click="emit('remove', item.id)">删除</button>
          </div>
        </div>
      </article>

      <div v-if="totalPages > 1" class="mt-4 flex justify-center">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="(page: number) => emit('pageChange', page)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { CategoryItem, QuestionItem } from '@/types/api'
import { importQuestionsApi } from '@/api/admin'

interface QuestionForm {
  id?: number
  title: string
  categoryId?: number
  difficulty: QuestionItem['difficulty']
  tags: string
  standardAnswer: string
}

interface QuestionFilter {
  categoryId?: number
  difficulty?: QuestionItem['difficulty']
  keyword: string
}

defineProps<{
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
</script>
