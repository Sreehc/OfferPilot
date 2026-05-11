<template>
  <div class="leaderboard-cockpit space-y-6">
    <button
      class="inline-flex items-center gap-2 text-sm text-slate-500 transition-colors hover:text-ink"
      @click="$router.push('/community')"
    >
      <span>&larr;</span> 返回社区
    </button>

    <AppShellHeader compact />

    <section class="cockpit-panel p-5 sm:p-6">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h3 class="text-xl font-semibold tracking-[-0.03em] text-ink sm:text-2xl">贡献者排名</h3>
        </div>
        <span v-if="topEntry" class="detail-rank-pill" :class="rankBadgeClass(topEntry.rankTitle)">
          榜首：{{ topEntry.username || `用户${topEntry.userId}` }}
        </span>
      </div>

      <div v-if="leaderboard.length" class="mt-6 grid gap-4 xl:grid-cols-[minmax(0,1.2fr)_minmax(280px,0.8fr)]">
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
                  <span>回答占比</span>
                  <span>{{ answerRate(entry) }}</span>
                </div>
                <div class="rank-card__signal-row">
                  <span>采纳率</span>
                  <span>{{ acceptRate(entry) }}</span>
                </div>
                <div class="rank-card__signal-row">
                  <span>活跃度</span>
                  <span>{{ rankIntensity(entry.position) }}</span>
                </div>
              </div>
            </div>
          </article>
        </div>

        <aside class="space-y-4">
          <article v-if="topEntry" class="cockpit-panel p-5">
            <h4 class="text-xl font-semibold text-ink">{{ topEntry.username || `用户${topEntry.userId}` }}</h4>
            <div class="mt-4 grid gap-3 sm:grid-cols-3 xl:grid-cols-1">
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
          </article>
        </aside>
      </div>

      <EmptyState
        v-else
        class="empty-state-card mt-6"
        icon="trophy"
        title="暂无排名数据"
        description="有了提问、回答和采纳记录后，这里会自动生成排行榜。"
        compact
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppShellHeader from '@/components/AppShellHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { fetchLeaderboardApi } from '@/api/community'
import type { LeaderboardEntry } from '@/types/api'

const leaderboard = ref<LeaderboardEntry[]>([])

const topEntry = computed(() => leaderboard.value[0] ?? null)

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
  if (position === 1) return '榜首'
  if (position <= 3) return '领先'
  if (position <= 10) return '活跃'
  return '持续参与'
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
