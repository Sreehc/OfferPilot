<template>
  <div class="space-y-6">
    <section class="shell-section-card p-6 sm:p-8">
      <EmptyState
        icon="document"
        title="当前请先使用简历助手"
        description="这里暂时不提供从零新建简历的流程。若你已经有简历，先去简历助手上传、解析和继续优化会更直接。"
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
