<template>
  <div class="space-y-6">
    <AppShellHeader>
      <template #actions>
        <RouterLink
          to="/resume"
          class="hard-button-primary"
        >
          整理简历
        </RouterLink>
        <RouterLink
          to="/analytics"
          class="hard-button-secondary"
        >
          看训练趋势
        </RouterLink>
      </template>
    </AppShellHeader>

    <section class="shell-section-card application-stage-card p-5 sm:p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div class="max-w-3xl">
          <div class="flex flex-wrap gap-2">
            <span class="hard-chip">Phase 0 入口已建立</span>
            <span class="detail-pill">Phase 5 接投递看板与 JD 分析</span>
          </div>
          <h2 class="mt-5 font-display text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">
            把岗位、简历、真实面试和复盘收进一个投递看板
          </h2>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-secondary">
            当前先建立独立的“投递管理”主路径，避免真实投递流程继续散落在表格、聊天记录或外部工具里。后续 Phase 5 会在这里接入岗位卡片、JD 分析、状态流转、面试记录和复盘建议。
          </p>
        </div>

        <div class="application-stage-aside">
          <span>当前阶段</span>
          <strong>信息架构就位</strong>
          <p>后续只需向该入口接入 application 域模型和流程页面。</p>
        </div>
      </div>
    </section>

    <section class="shell-section-card p-5 sm:p-6">
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="section-kicker">
            目标看板
          </p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
            未来会在这里维护完整投递流
          </h3>
        </div>
        <span class="detail-pill">当前为结构预览</span>
      </div>

      <div class="application-board mt-6">
        <article
          v-for="column in columns"
          :key="column.title"
          class="application-column"
        >
          <div class="flex items-center justify-between gap-3">
            <div>
              <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">
                {{ column.stage }}
              </div>
              <h4 class="mt-2 text-lg font-semibold text-ink">
                {{ column.title }}
              </h4>
            </div>
            <span class="application-column__count">{{ column.count }}</span>
          </div>

          <div class="mt-4 space-y-3">
            <article
              v-for="card in column.cards"
              :key="card.title"
              class="application-card"
            >
              <div class="text-xs font-semibold uppercase tracking-[0.18em] text-tertiary">
                {{ card.kicker }}
              </div>
              <h5 class="mt-2 text-base font-semibold text-ink">
                {{ card.title }}
              </h5>
              <p class="mt-2 text-sm leading-7 text-secondary">
                {{ card.description }}
              </p>
            </article>
          </div>
        </article>
      </div>
    </section>

    <section class="grid gap-4 xl:grid-cols-3">
      <article
        v-for="insight in insights"
        :key="insight.title"
        class="shell-section-card p-5 sm:p-6"
      >
        <p class="section-kicker">
          {{ insight.stage }}
        </p>
        <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
          {{ insight.title }}
        </h3>
        <p class="mt-3 text-sm leading-7 text-secondary">
          {{ insight.description }}
        </p>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import AppShellHeader from '@/components/AppShellHeader.vue'

const columns = [
  {
    stage: '阶段 1',
    title: '待分析',
    count: 'JD',
    cards: [
      {
        kicker: '输入',
        title: '待录入岗位信息',
        description: '后续会支持录入岗位名称、公司、城市、薪资范围、JD 原文和投递渠道。'
      }
    ]
  },
  {
    stage: '阶段 2',
    title: '待投递',
    count: '简历',
    cards: [
      {
        kicker: '联动',
        title: '匹配简历版本',
        description: '会把岗位分析结果和简历助手里的版本绑定，避免同一岗位错投或忘记版本差异。'
      }
    ]
  },
  {
    stage: '阶段 3',
    title: '面试中',
    count: '复盘',
    cards: [
      {
        kicker: '记录',
        title: '沉淀真实面试记录',
        description: '后续会将真实面试问题、结果和复盘建议沉淀到 application 域，与模拟面试区分。'
      }
    ]
  },
  {
    stage: '阶段 4',
    title: '已完成',
    count: '总结',
    cards: [
      {
        kicker: '分析',
        title: '产出投递与面试复盘',
        description: '用于识别通过率、被拒原因、岗位偏差和下一轮准备重点。'
      }
    ]
  }
]

const insights = [
  {
    stage: 'Phase 5',
    title: 'JD 分析',
    description: 'AI 模块会在这里输出岗位关键词、技术要求、项目匹配点和潜在风险提醒。'
  },
  {
    stage: 'Phase 5',
    title: '状态流转',
    description: '投递、笔试、初试、复试、终面、Offer、淘汰等状态会形成正式工作流，而不是仅做静态列表。'
  },
  {
    stage: 'Phase 5',
    title: '真实面试复盘',
    description: '真实面试和模拟面试会分域管理，但会共享复盘思路和薄弱点回流能力。'
  }
]
</script>

<style scoped>
.application-stage-card {
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.11), transparent 28%),
    radial-gradient(circle at 88% 20%, rgba(var(--bc-cyan-rgb), 0.11), transparent 20%),
    var(--bc-surface-card);
}

.application-stage-aside {
  max-width: 280px;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.36);
  padding: 1rem 1.1rem;
  backdrop-filter: blur(10px);
}

.application-stage-aside span {
  display: block;
  font-size: 0.78rem;
  color: var(--bc-ink-secondary);
}

.application-stage-aside strong {
  display: block;
  margin-top: 0.55rem;
  font-size: 1.3rem;
  color: var(--bc-ink);
}

.application-stage-aside p {
  margin-top: 0.7rem;
  font-size: 0.88rem;
  line-height: 1.7;
  color: var(--bc-ink-secondary);
}

.application-board {
  display: grid;
  gap: 1rem;
}

.application-column {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.05), transparent 60%), var(--bc-surface-muted);
  padding: 1rem;
}

.application-column__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2.8rem;
  border-radius: 999px;
  border: 1px solid rgba(var(--bc-accent-rgb), 0.2);
  background: rgba(var(--bc-accent-rgb), 0.1);
  padding: 0.35rem 0.7rem;
  font-size: 0.76rem;
  font-weight: 700;
  color: var(--bc-accent);
}

.application-card {
  border-radius: 16px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-card);
  padding: 0.95rem;
}

@media (min-width: 1200px) {
  .application-board {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
