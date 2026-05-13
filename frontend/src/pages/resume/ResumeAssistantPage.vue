<template>
  <div class="space-y-6">
    <AppShellHeader>
      <template #actions>
        <RouterLink
          to="/knowledge"
          class="hard-button-primary"
        >
          整理资料
        </RouterLink>
        <RouterLink
          to="/interview"
          class="hard-button-secondary"
        >
          准备项目追问
        </RouterLink>
      </template>
    </AppShellHeader>

    <section class="shell-section-card resume-stage-card p-5 sm:p-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div class="max-w-3xl">
          <div class="flex flex-wrap gap-2">
            <span class="hard-chip">Phase 0 入口已建立</span>
            <span class="detail-pill">Phase 4 接简历解析与项目问答</span>
          </div>
          <h2 class="mt-5 font-display text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">
            简历、项目、自我介绍会在这里形成一条独立工作流
          </h2>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-secondary">
            当前先把“简历助手”作为一级入口接入产品心智，避免后续能力散落在知识库、设置页或面试页里。正式的简历上传、解析、项目拆解、自我介绍生成和面试简历制作将在 Phase 4 实现。
          </p>
        </div>

        <div class="resume-stage-aside">
          <span>当前阶段</span>
          <strong>主路径已就位</strong>
          <p>后续只需往这个入口内接文件、解析和问答能力。</p>
        </div>
      </div>
    </section>

    <section class="grid gap-4 xl:grid-cols-2">
      <article
        v-for="capability in capabilities"
        :key="capability.title"
        class="shell-section-card p-5 sm:p-6"
      >
        <p class="section-kicker">
          {{ capability.stage }}
        </p>
        <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
          {{ capability.title }}
        </h3>
        <p class="mt-3 text-sm leading-7 text-secondary">
          {{ capability.description }}
        </p>
        <ul class="mt-5 space-y-3 text-sm leading-7 text-secondary">
          <li
            v-for="point in capability.points"
            :key="point"
            class="resume-bullet"
          >
            {{ point }}
          </li>
        </ul>
      </article>
    </section>

    <section class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
      <article class="shell-section-card p-5 sm:p-6">
        <p class="section-kicker">
          当前建议
        </p>
        <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
          现在先准备这些输入
        </h3>

        <div class="mt-5 grid gap-3">
          <article
            v-for="prep in preparations"
            :key="prep.title"
            class="resume-prep-card"
          >
            <div class="text-xs font-semibold uppercase tracking-[0.22em] text-tertiary">
              {{ prep.label }}
            </div>
            <h4 class="mt-2 text-lg font-semibold text-ink">
              {{ prep.title }}
            </h4>
            <p class="mt-2 text-sm leading-7 text-secondary">
              {{ prep.description }}
            </p>
          </article>
        </div>
      </article>

      <article class="shell-section-card p-5 sm:p-6">
        <p class="section-kicker">
          后续联动
        </p>
        <div class="mt-5 space-y-3 text-sm leading-7 text-secondary">
          <p>知识库会成为简历原文、项目材料和补充资料的统一语料入口。</p>
          <p>模拟面试会消费简历解析结果，用于生成更贴近项目背景的追问。</p>
          <p>投递管理会复用这里的简历版本，形成“岗位 - 简历 - 面试复盘”联动。</p>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import AppShellHeader from '@/components/AppShellHeader.vue'

const capabilities = [
  {
    stage: 'Phase 4',
    title: '简历解析',
    description: '上传简历文件后，抽取基础信息、教育背景、项目经历、技能栈和可追问点。',
    points: ['统一走对象存储与文件治理', '输出结构化项目列表和追问线索', '为后续自我介绍和投递管理提供基础数据']
  },
  {
    stage: 'Phase 4',
    title: '项目问答与自我介绍',
    description: '围绕项目经历沉淀标准回答、自我介绍模版和可扩展追问，直接服务模拟面试。',
    points: ['项目亮点和难点拆解', '自我介绍版本生成', '常见项目追问脚本']
  },
  {
    stage: 'Phase 4',
    title: '面试简历制作',
    description: '在通用简历基础上生成更适合面试讨论的项目版简历，用于重点突出项目价值。',
    points: ['按岗位方向重排项目表述', '保留项目深挖信息', '与 JD 分析结果联动']
  },
  {
    stage: 'Phase 5',
    title: '投递联动',
    description: '把简历版本和投递岗位绑定，形成“用哪份简历投了哪个岗位”的可追踪链路。',
    points: ['记录简历版本', '关联 JD 分析', '关联真实面试与复盘']
  }
]

const preparations = [
  {
    label: '资料',
    title: '把项目文档和技术笔记收进知识库',
    description: '后续简历解析和项目问答会直接消费这些材料，越早整理越能减少后续补资料成本。'
  },
  {
    label: '项目',
    title: '准备每个项目的背景、职责、难点和结果',
    description: '这些会成为简历解析后的结构化字段，也是面试追问最密集的区域。'
  },
  {
    label: '表达',
    title: '提前积累自我介绍与项目口述版本',
    description: '后续页面会生成多个版本，但前期先准备素材，Phase 4 接入后能立即形成成品。'
  }
]
</script>

<style scoped>
.resume-stage-card {
  background:
    radial-gradient(circle at top left, rgba(var(--bc-accent-rgb), 0.1), transparent 28%),
    radial-gradient(circle at 90% 22%, rgba(var(--bc-cyan-rgb), 0.1), transparent 18%),
    var(--bc-surface-card);
}

.resume-stage-aside {
  max-width: 280px;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: rgba(255, 255, 255, 0.36);
  padding: 1rem 1.1rem;
  backdrop-filter: blur(10px);
}

.resume-stage-aside span {
  display: block;
  font-size: 0.78rem;
  color: var(--bc-ink-secondary);
}

.resume-stage-aside strong {
  display: block;
  margin-top: 0.55rem;
  font-size: 1.3rem;
  color: var(--bc-ink);
}

.resume-stage-aside p {
  margin-top: 0.7rem;
  font-size: 0.88rem;
  line-height: 1.7;
  color: var(--bc-ink-secondary);
}

.resume-bullet {
  position: relative;
  padding-left: 1rem;
}

.resume-bullet::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.75rem;
  width: 0.35rem;
  height: 0.35rem;
  border-radius: 999px;
  background: var(--bc-accent);
}

.resume-prep-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 1rem 1.05rem;
}
</style>
