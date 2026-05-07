<template>
  <div class="cards-cockpit space-y-5">
    <section class="cockpit-panel p-5 sm:p-6">
      <div class="cards-hero">
        <div class="cards-hero__copy">
          <p class="section-kicker">知识卡片</p>
          <h2 class="cards-hero__title">把资料整理成每天能推进的记忆任务</h2>
          <p class="cards-hero__description">
            从知识库里的文档生成卡片，按天安排学习进度，再用翻牌和“会 / 不会”快速推进记忆。
          </p>
        </div>

        <div class="cards-hero__actions">
          <RouterLink to="/knowledge" class="hard-button-primary">去知识库选资料</RouterLink>
          <button type="button" class="hard-button-secondary" disabled>卡片功能即将上线</button>
        </div>
      </div>
    </section>

    <section class="cards-grid">
      <article class="cockpit-panel p-5 sm:p-6">
        <p class="section-kicker">推荐流程</p>
        <h3 class="cards-section-title">从资料到记忆的最短路径</h3>
        <div class="cards-flow mt-5">
          <div class="cards-step">
            <span class="cards-step__index">01</span>
            <h4>选择文档</h4>
            <p>从知识库选一本书或一个 md 文件，作为卡片来源。</p>
          </div>
          <div class="cards-step">
            <span class="cards-step__index">02</span>
            <h4>生成卡片</h4>
            <p>系统会把知识点整理成适合翻牌复习的问题和答案。</p>
          </div>
          <div class="cards-step">
            <span class="cards-step__index">03</span>
            <h4>按天推进</h4>
            <p>每天只处理固定数量卡片，用“会 / 不会”持续推进掌握度。</p>
          </div>
        </div>
      </article>

      <article class="cockpit-panel p-5 sm:p-6">
        <p class="section-kicker">当前状态</p>
        <h3 class="cards-section-title">先保留导航入口，再逐步完善能力</h3>
        <div class="cards-notes mt-5">
          <div class="cards-note">
            <span class="hard-chip">已确定</span>
            <p>模块命名、一级导航位置、移动端入口和资料来源路径。</p>
          </div>
          <div class="cards-note">
            <span class="hard-chip">下一步</span>
            <p>补齐卡片任务、翻牌交互、会/不会记录和文档入口联动。</p>
          </div>
        </div>
      </article>
    </section>

    <section class="cockpit-panel p-5 sm:p-6">
      <EmptyState
        icon="review"
        title="知识卡片功能正在接入"
        description="你已经可以从导航进入这里。下一阶段会接入文档选择、卡片生成和每日复习流程。"
        compact
      >
        <template #action>
          <RouterLink to="/knowledge" class="hard-button-secondary text-sm">查看知识库</RouterLink>
        </template>
      </EmptyState>
    </section>
  </div>
</template>

<script setup lang="ts">
import EmptyState from '@/components/EmptyState.vue'
</script>

<style scoped>
.cards-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.85fr);
}

.cards-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1.25rem;
}

.cards-hero__copy {
  max-width: 42rem;
}

.cards-hero__title,
.cards-section-title {
  margin-top: 0.75rem;
  color: var(--bc-ink);
  font-size: 1.7rem;
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.08;
}

.cards-hero__description {
  margin-top: 0.9rem;
  max-width: 38rem;
  color: rgb(100 116 139);
  font-size: 0.95rem;
  line-height: 1.9;
}

.cards-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.cards-flow {
  display: grid;
  gap: 0.9rem;
}

.cards-step,
.cards-note {
  border: 1px solid var(--bc-line);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.42);
  padding: 1rem 1rem 1.05rem;
}

.cards-step__index {
  display: inline-flex;
  margin-bottom: 0.7rem;
  color: var(--bc-cyan);
  font-family: theme('fontFamily.mono');
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.cards-step h4,
.cards-note p {
  color: var(--bc-ink);
}

.cards-step h4 {
  font-size: 1rem;
  font-weight: 700;
}

.cards-step p,
.cards-note p {
  margin-top: 0.45rem;
  color: rgb(100 116 139);
  font-size: 0.9rem;
  line-height: 1.8;
}

.cards-notes {
  display: grid;
  gap: 0.9rem;
}

.cards-note {
  display: grid;
  gap: 0.8rem;
}

@media (max-width: 1023px) {
  .cards-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 767px) {
  .cards-hero__title,
  .cards-section-title {
    font-size: 1.42rem;
  }

  .cards-hero__actions {
    width: 100%;
  }

  .cards-hero__actions > * {
    width: 100%;
  }
}
</style>
