<template>
  <div class="space-y-6">
    <section class="shell-section-card p-6 sm:p-8">
      <EmptyState
        icon="document"
        title="简历制作即将上线"
        description="我们正在开发从零制作简历的功能。在此之前，你可以先使用简历助手来整理已有的简历。"
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
