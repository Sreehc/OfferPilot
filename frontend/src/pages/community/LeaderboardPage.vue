<template>
  <div class="leaderboard-cockpit space-y-6">
    <button
      class="inline-flex items-center gap-2 text-sm text-slate-500 transition-colors hover:text-ink"
      @click="$router.push('/community')"
    >
      <span>&larr;</span> 返回社区
    </button>

    <section class="grid gap-4 xl:grid-cols-[minmax(0,1.08fr)_minmax(320px,0.92fr)]">
      <article class="cockpit-panel p-5 sm:p-6">
        <div class="flex items-center gap-3">
          <span class="state-pulse" aria-hidden="true"></span>
          <p class="section-kicker">Contribution Board</p>
        </div>
        <h2 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-ink sm:text-4xl">社区贡献榜</h2>
        <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-300">
          排名不只是积分表，而是训练社区里谁在持续输出高质量回答、帮助别人把问题落成可复盘答案的状态面板。
        </p>

        <div class="mt-6 grid gap-3 md:grid-cols-3">
          <article v-for="signal in boardSignals" :key="signal.label" class="data-slab p-4" :class="signal.toneClass">
            <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-500 dark:text-slate-400">{{ signal.label }}</p>
            <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ signal.value }}</p>
            <p class="mt-2 text-xs leading-6 text-slate-500 dark:text-slate-400">{{ signal.detail }}</p>
          </article>
        </div>
      </article>

      <article class="cockpit-panel p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="section-kicker">Top Signal</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">榜首聚焦</h3>
          </div>
          <span class="hard-chip !px-2 !py-0.5 !text-[9px]">{{ topEntry ? 'No.1' : 'Pending' }}</span>
        </div>

        <div v-if="topEntry" class="top-pilot-card mt-5">
          <div class="flex items-start justify-between gap-4">
            <div class="min-w-0">
              <span class="hard-chip detail-rank-chip !px-2 !py-0.5 !text-[9px]">Top Pilot</span>
              <h4 class="mt-4 text-2xl font-semibold tracking-[-0.03em] text-ink">{{ topEntry.username || `用户${topEntry.userId}` }}</h4>
              <p class="mt-2 text-sm leading-7 text-slate-600 dark:text-slate-300">
                以回答质量和被采纳次数领跑当前社区热区，属于最值得参考的训练同伴。
              </p>
            </div>
            <div class="rank-medal">#1</div>
          </div>

          <div class="mt-5 grid gap-3 sm:grid-cols-3">
            <article class="top-pilot-node">
              <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">积分</p>
              <p class="mt-2 font-mono text-2xl font-semibold text-ink">{{ topEntry.communityScore }}</p>
            </article>
            <article class="top-pilot-node">
              <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">回答</p>
              <p class="mt-2 font-mono text-2xl font-semibold text-ink">{{ topEntry.communityAnswers }}</p>
            </article>
            <article class="top-pilot-node">
              <p class="text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">采纳</p>
              <p class="mt-2 font-mono text-2xl font-semibold text-ink">{{ topEntry.communityAccepted }}</p>
            </article>
          </div>
        </div>

        <EmptyState
          v-else
          class="empty-state-card mt-5"
          icon="trophy"
          title="还没有榜首信号"
          description="一旦社区里出现真实互动，这里会显示最强贡献者。"
          compact
        />
      </article>
    </section>

    <section class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="section-kicker">Ranking Flow</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">贡献流</h3>
          <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-300">
            每个人的排名由提问、回答和被采纳共同决定。优先看回答与采纳比，能更快识别谁更擅长把问题解释清楚。
          </p>
        </div>
        <div class="flex flex-wrap gap-2">
          <span class="leaderboard-pill">采纳 +50</span>
          <span class="leaderboard-pill">点赞 +2</span>
          <span class="leaderboard-pill">问题与回答共同计入</span>
        </div>
      </div>

      <div v-if="leaderboard.length" class="mt-6 grid gap-4 xl:grid-cols-[minmax(0,1.1fr)_minmax(280px,0.9fr)]">
        <div class="space-y-4">
          <article
            v-for="entry in leaderboard"
            :key="entry.userId"
            class="rank-card"
            :class="rankCardClass(entry.position)"
          >
            <div class="flex flex-col gap-4 lg:flex-row lg:items-start">
              <div class="rank-card__position" :class="rankClass(entry.position)">
                {{ entry.position }}
              </div>

              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="text-lg font-semibold text-ink">{{ entry.username || `用户${entry.userId}` }}</span>
                  <span class="detail-rank-pill" :class="rankBadgeClass(entry.rankTitle)">{{ entry.rankTitle }}</span>
                </div>

                <div class="mt-4 grid gap-3 sm:grid-cols-4">
                  <article class="rank-metric">
                    <p class="rank-metric__label">积分</p>
                    <p class="rank-metric__value rank-metric__value-accent">{{ entry.communityScore }}</p>
                  </article>
                  <article class="rank-metric">
                    <p class="rank-metric__label">提问</p>
                    <p class="rank-metric__value">{{ entry.communityQuestions }}</p>
                  </article>
                  <article class="rank-metric">
                    <p class="rank-metric__label">回答</p>
                    <p class="rank-metric__value">{{ entry.communityAnswers }}</p>
                  </article>
                  <article class="rank-metric">
                    <p class="rank-metric__label">采纳</p>
                    <p class="rank-metric__value rank-metric__value-lime">{{ entry.communityAccepted }}</p>
                  </article>
                </div>
              </div>

              <div class="rank-card__signal">
                <div class="rank-card__signal-row">
                  <span>Answer Rate</span>
                  <span>{{ answerRate(entry) }}</span>
                </div>
                <div class="rank-card__signal-row">
                  <span>Accept Rate</span>
                  <span>{{ acceptRate(entry) }}</span>
                </div>
                <div class="rank-card__signal-row">
                  <span>Tier</span>
                  <span>{{ rankIntensity(entry.position) }}</span>
                </div>
              </div>
            </div>
          </article>
        </div>

        <aside class="space-y-4">
          <article class="cockpit-panel p-5">
            <p class="section-kicker">Tier Map</p>
            <h4 class="mt-3 text-xl font-semibold text-ink">等级体系</h4>
            <div class="mt-4 space-y-3">
              <article v-for="rank in ranks" :key="rank.name" class="tier-card">
                <div class="flex items-center justify-between gap-3">
                  <span class="detail-rank-pill" :class="rankBadgeClass(rank.name)">{{ rank.name }}</span>
                  <span class="font-mono text-sm font-semibold text-ink">{{ rank.threshold }}+</span>
                </div>
              </article>
            </div>
          </article>

          <article class="cockpit-panel p-5">
            <p class="section-kicker">Interpretation</p>
            <h4 class="mt-3 text-xl font-semibold text-ink">怎么看这张榜</h4>
            <div class="mt-4 space-y-3 text-sm leading-7 text-slate-600 dark:text-slate-300">
              <p>高积分但低采纳：说明参与度高，但答案未必最稳定。</p>
              <p>回答数不高但采纳率高：通常代表更擅长给出可执行答案。</p>
              <p>提问数高：常常意味着这个用户愿意公开暴露学习盲点，适合持续关注。</p>
            </div>
          </article>
        </aside>
      </div>

      <EmptyState
        v-else
        class="empty-state-card mt-6"
        icon="trophy"
        title="暂无排名数据"
        description="完成面试和社区互动后，排行榜会在这里形成贡献流。"
        compact
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { fetchLeaderboardApi } from '@/api/community'
import type { LeaderboardEntry } from '@/types/api'

const leaderboard = ref<LeaderboardEntry[]>([])

const ranks = [
  { name: '见习生', threshold: 0 },
  { name: '初级工程师', threshold: 100 },
  { name: '中级工程师', threshold: 300 },
  { name: '高级工程师', threshold: 800 },
  { name: '架构师', threshold: 2000 },
  { name: '技术专家', threshold: 5000 },
]

const topEntry = computed(() => leaderboard.value[0] ?? null)
const totalScore = computed(() => leaderboard.value.reduce((sum, entry) => sum + entry.communityScore, 0))
const totalAccepted = computed(() => leaderboard.value.reduce((sum, entry) => sum + entry.communityAccepted, 0))
const totalAnswers = computed(() => leaderboard.value.reduce((sum, entry) => sum + entry.communityAnswers, 0))

const boardSignals = computed(() => [
  {
    label: 'Visible Pilots',
    value: leaderboard.value.length,
    detail: '当前榜单中可见的贡献者数量。',
    toneClass: '',
  },
  {
    label: 'Total Score',
    value: totalScore.value,
    detail: '所有上榜用户累计贡献分，反映社区整体活跃度。',
    toneClass: 'leaderboard-slab-cyan',
  },
  {
    label: 'Accepted Answers',
    value: totalAccepted.value,
    detail: '社区里真正落地为最佳答案的总数。',
    toneClass: 'leaderboard-slab-lime',
  },
])

function rankClass(position: number) {
  if (position === 1) return 'rank-medal-gold'
  if (position === 2) return 'rank-medal-silver'
  if (position === 3) return 'rank-medal-bronze'
  return 'rank-medal-default'
}

function rankCardClass(position: number) {
  if (position === 1) return 'rank-card-top'
  if (position <= 3) return 'rank-card-leading'
  return ''
}

function rankIntensity(position: number) {
  if (position === 1) return 'Top'
  if (position <= 3) return 'Lead'
  if (position <= 10) return 'Active'
  return 'Watch'
}

function rankBadgeClass(rank: string) {
  const map: Record<string, string> = {
    '技术专家': 'rank-badge-expert',
    '架构师': 'rank-badge-architect',
    '高级工程师': 'rank-badge-senior',
    '中级工程师': 'rank-badge-mid',
    '初级工程师': 'rank-badge-junior',
    '见习生': 'rank-badge-trainee',
  }
  return map[rank] || 'rank-badge-trainee'
}

function answerRate(entry: LeaderboardEntry) {
  const total = entry.communityQuestions + entry.communityAnswers
  if (total === 0) return '0%'
  return `${Math.round((entry.communityAnswers / total) * 100)}%`
}

function acceptRate(entry: LeaderboardEntry) {
  if (entry.communityAnswers === 0) return '0%'
  return `${Math.round((entry.communityAccepted / entry.communityAnswers) * 100)}%`
}

onMounted(async () => {
  const { data } = await fetchLeaderboardApi(50)
  leaderboard.value = data
})
</script>

<style scoped>
.top-pilot-card,
.top-pilot-node,
.rank-card,
.rank-metric,
.tier-card {
  border-radius: 24px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.34);
}

.dark .top-pilot-card,
.dark .top-pilot-node,
.dark .rank-card,
.dark .rank-metric,
.dark .tier-card {
  background: rgba(255, 255, 255, 0.05);
}

.top-pilot-card {
  padding: 18px;
}

.top-pilot-node,
.tier-card,
.rank-metric {
  padding: 14px;
}

.rank-card {
  padding: 16px;
}

.rank-card-top {
  border-color: rgba(255, 183, 77, 0.34);
}

.rank-card-leading {
  border-color: rgba(85, 214, 190, 0.22);
}

.rank-medal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  height: 74px;
  border-radius: 999px;
  background: linear-gradient(180deg, #ffe0ad 0%, var(--bc-amber) 100%);
  color: #101826;
  font-family: theme('fontFamily.mono');
  font-size: 20px;
  font-weight: 700;
  box-shadow: 0 18px 30px rgba(var(--bc-accent-rgb), 0.24);
}

.rank-card__position {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  height: 56px;
  border-radius: 20px;
  font-family: theme('fontFamily.mono');
  font-size: 18px;
  font-weight: 700;
}

.rank-medal-gold {
  background: rgba(255, 183, 77, 0.18);
  color: var(--bc-amber);
}

.rank-medal-silver {
  background: rgba(148, 163, 184, 0.18);
  color: #94a3b8;
}

.rank-medal-bronze {
  background: rgba(249, 115, 22, 0.16);
  color: #f97316;
}

.rank-medal-default {
  background: rgba(140, 166, 191, 0.16);
  color: var(--bc-ink-secondary);
}

.rank-card__signal {
  display: grid;
  gap: 10px;
  min-width: 180px;
  border-radius: 20px;
  border: 1px solid var(--bc-line);
  background: rgba(255, 255, 255, 0.38);
  padding: 14px;
  font-size: 12px;
}

.dark .rank-card__signal {
  background: rgba(255, 255, 255, 0.05);
}

.rank-card__signal-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--bc-ink-secondary);
}

.rank-card__signal-row span:last-child {
  color: var(--bc-ink);
  font-weight: 600;
}

.rank-metric__label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--bc-ink-secondary);
}

.rank-metric__value {
  margin-top: 8px;
  font-family: theme('fontFamily.mono');
  font-size: 24px;
  font-weight: 700;
  color: var(--bc-ink);
}

.rank-metric__value-accent {
  color: var(--bc-accent);
}

.rank-metric__value-lime {
  color: var(--bc-lime);
}

.detail-rank-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 11px;
  font-weight: 600;
}

.detail-rank-chip {
  color: var(--bc-amber);
}

.rank-badge-expert {
  background: rgba(255, 107, 107, 0.14);
  color: var(--bc-coral);
}

.rank-badge-architect {
  background: rgba(255, 183, 77, 0.14);
  color: var(--bc-amber);
}

.rank-badge-senior {
  background: rgba(85, 214, 190, 0.14);
  color: var(--bc-cyan);
}

.rank-badge-mid {
  background: rgba(159, 232, 112, 0.14);
  color: var(--bc-lime);
}

.rank-badge-junior {
  background: rgba(118, 180, 255, 0.14);
  color: #76b4ff;
}

.rank-badge-trainee {
  background: rgba(140, 166, 191, 0.14);
  color: var(--bc-ink-secondary);
}

.leaderboard-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid var(--bc-line);
  padding: 6px 10px;
  font-size: 11px;
  color: var(--bc-ink-secondary);
}

.leaderboard-slab-cyan {
  border-left-color: var(--bc-cyan);
}

.leaderboard-slab-lime {
  border-left-color: var(--bc-lime);
}

@media (max-width: 1024px) {
  .rank-card__signal {
    min-width: 100%;
  }
}
</style>
