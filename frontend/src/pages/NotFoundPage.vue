<template>
  <div class="flex min-h-screen items-center justify-center px-4">
    <section class="paper-panel max-w-2xl p-10 text-center">
      <p class="section-kicker">404</p>
      <h1 class="mt-4 text-5xl font-semibold tracking-[-0.03em] text-ink">页面没有被纳入这期 MVP</h1>
      <p class="mt-4 text-sm leading-7 text-slate-600 dark:text-slate-300">
        路由不存在，或者功能还没有进入阶段 1 的实现范围。
      </p>
      <RouterLink to="/dashboard" class="hard-button-primary mt-8">
        返回 Dashboard
      </RouterLink>
    </section>
  </div>
</template>

<script setup lang="ts">
</script>
