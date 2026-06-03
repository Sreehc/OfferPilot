<template>
  <div class="space-y-6">
    <section class="shell-section-card p-6 sm:p-8">
      <EmptyState
        icon="document"
        title="请使用简历助手"
        description="这里暂不提供从零创建简历。已有简历时，可前往简历助手上传并优化。"
      >
        <template #action>
          <RouterLink to="/resume" class="hard-button-primary">
            去简历助手
          </RouterLink>
        </template>
      </EmptyState>
    </section>
  </div>
</template>

<script setup lang="ts">
import EmptyState from '@/components/EmptyState.vue'
</script>
