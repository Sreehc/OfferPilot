<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <button
      class="flex items-center gap-2 text-sm text-slate-500 hover:text-ink transition-colors"
      @click="$router.push('/community')"
    >
      <span>&larr;</span> 返回社区
    </button>

    <div class="paper-panel px-8 py-6">
      <p class="section-kicker">排行榜</p>
      <h2 class="mt-2 text-2xl font-semibold text-ink">社区贡献排名</h2>
      <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">
        根据社区积分排名，积分来源：回答被采纳 +50、收到点赞 +2
      </p>
    </div>

    <!-- Leaderboard Table -->
    <div class="paper-panel overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="bg-slate-50 dark:bg-slate-800/50">
              <th class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 w-16">排名</th>
              <th class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">用户</th>
              <th class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">等级</th>
              <th class="px-6 py-4 text-right text-xs font-semibold uppercase tracking-wider text-slate-500">积分</th>
              <th class="px-6 py-4 text-right text-xs font-semibold uppercase tracking-wider text-slate-500">提问</th>
              <th class="px-6 py-4 text-right text-xs font-semibold uppercase tracking-wider text-slate-500">回答</th>
              <th class="px-6 py-4 text-right text-xs font-semibold uppercase tracking-wider text-slate-500">被采纳</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
            <tr
              v-for="entry in leaderboard"
              :key="entry.userId"
              class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors"
            >
              <td class="px-6 py-4">
                <div
                  class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold"
                  :class="rankClass(entry.position)"
                >
                  {{ entry.position }}
                </div>
              </td>
              <td class="px-6 py-4">
                <span class="text-sm font-medium text-ink">{{ entry.username || `用户${entry.userId}` }}</span>
              </td>
              <td class="px-6 py-4">
                <span
                  class="px-2.5 py-1 text-xs font-medium rounded-full"
                  :class="rankBadgeClass(entry.rankTitle)"
                >
                  {{ entry.rankTitle }}
                </span>
              </td>
              <td class="px-6 py-4 text-right">
                <span class="text-sm font-bold text-accent">{{ entry.communityScore }}</span>
              </td>
              <td class="px-6 py-4 text-right text-sm text-slate-600 dark:text-slate-300">
                {{ entry.communityQuestions }}
              </td>
              <td class="px-6 py-4 text-right text-sm text-slate-600 dark:text-slate-300">
                {{ entry.communityAnswers }}
              </td>
              <td class="px-6 py-4 text-right">
                <span class="text-sm font-semibold text-green-600">{{ entry.communityAccepted }}</span>
              </td>
            </tr>
          </tbody>
        </table>

        <EmptyState
          v-if="leaderboard.length === 0"
          icon="trophy"
          title="暂无排名数据"
          description="完成面试和社区互动后，排行榜将显示排名。"
          compact
        />
      </div>
    </div>

    <!-- Rank System Explanation -->
    <div class="paper-panel px-8 py-6">
      <h3 class="text-base font-semibold text-ink mb-4">等级体系</h3>
      <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
        <div v-for="rank in ranks" :key="rank.name" class="flex items-center gap-3 p-3 rounded-lg bg-slate-50 dark:bg-slate-800/50">
          <span class="px-2.5 py-1 text-xs font-medium rounded-full" :class="rankBadgeClass(rank.name)">
            {{ rank.name }}
          </span>
          <span class="text-xs text-slate-500">{{ rank.threshold }}+ 积分</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
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

function rankClass(position: number) {
  if (position === 1) return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400'
  if (position === 2) return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300'
  if (position === 3) return 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400'
  return 'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-300'
}

function rankBadgeClass(rank: string) {
  const map: Record<string, string> = {
    '技术专家': 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
    '架构师': 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
    '高级工程师': 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
    '中级工程师': 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
    '初级工程师': 'bg-teal-100 text-teal-700 dark:bg-teal-900/30 dark:text-teal-400',
    '见习生': 'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-300',
  }
  return map[rank] || 'bg-slate-100 text-slate-600'
}

onMounted(async () => {
  const { data } = await fetchLeaderboardApi(50)
  leaderboard.value = data
})
</script>
