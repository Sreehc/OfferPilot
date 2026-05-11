<template>
  <div class="grid gap-4 xl:grid-cols-[320px_minmax(0,1fr)]">
    <div class="surface-muted p-4">
      <div class="text-sm font-semibold text-ink">{{ form.id ? '编辑分类' : '新增分类' }}</div>
      <div class="mt-4 space-y-3">
        <el-input v-model="form.name" placeholder="名称" size="large" />
        <el-select v-model="form.type" placeholder="类型" size="large" class="w-full">
          <el-option label="题目" value="question" />
          <el-option label="知识文档" value="knowledge" />
          <el-option label="面试" value="interview" />
        </el-select>
        <el-input-number v-model="form.sortOrder" :min="0" class="w-full" />
      </div>
      <div class="mt-4 flex gap-3">
        <el-button :loading="saving" type="primary" class="action-button" @click="emit('save')">
          {{ form.id ? '保存修改' : '新增分类' }}
        </el-button>
        <el-button class="hard-button-secondary" @click="emit('reset')">重置</el-button>
      </div>
    </div>

    <div class="space-y-3">
      <div class="text-sm text-slate-500 dark:text-slate-400">共 {{ categories.length }} 个分类</div>
      <article v-for="item in categories" :key="item.id" class="surface-card p-4">
        <div class="flex items-start justify-between gap-3">
          <div>
            <div class="font-semibold text-ink">{{ item.name }}</div>
            <div class="mt-1 text-xs uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">
              {{ item.type === 'question' ? '题目' : item.type === 'knowledge' ? '知识文档' : '面试' }}
            </div>
          </div>
          <div class="flex gap-2">
            <button type="button" class="accent-link text-sm font-semibold" @click="emit('edit', item)">编辑</button>
            <button type="button" class="text-sm text-slate-500 dark:text-slate-400 transition hover:text-red-500" @click="emit('remove', item.id)">删除</button>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CategoryItem } from '@/types/api'

interface CategoryForm {
  id?: number
  name: string
  type: CategoryItem['type']
  sortOrder: number
}

defineProps<{
  categories: CategoryItem[]
  form: CategoryForm
  saving: boolean
}>()

const emit = defineEmits<{
  save: []
  edit: [item: CategoryItem]
  remove: [id: number]
  reset: []
}>()
</script>
