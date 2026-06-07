<template>
  <div class="space-y-6">
    <section class="shell-section-card p-6 sm:p-8">
      <StateView
        icon="document"
        title="请使用简历助手"
        description="这里暂不提供从零创建简历。已有简历时，可前往简历助手上传并优化。"
      >
        <template #action>
          <RouterLink :to="resumeAssistantLink" class="hard-button-primary">
            去简历助手
          </RouterLink>
        </template>
      </StateView>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { StateView } from '@/components/ui'

const route = useRoute()

const resumeAssistantLink = computed(() => {
  const query = new URLSearchParams()
  const seedTopic = typeof route.query.seedTopic === 'string' ? route.query.seedTopic.trim() : ''
  const seedWorkflow = typeof route.query.seedWorkflow === 'string' ? route.query.seedWorkflow.trim() : ''
  const seedNote = typeof route.query.seedNote === 'string' ? route.query.seedNote.trim() : ''
  if (seedTopic) {
    query.set('seedTopic', seedTopic)
  }
  if (seedWorkflow) {
    query.set('seedWorkflow', seedWorkflow)
  }
  if (seedNote) {
    query.set('seedNote', seedNote)
  }
  return query.toString() ? `/resume?${query.toString()}` : '/resume'
})
</script>
