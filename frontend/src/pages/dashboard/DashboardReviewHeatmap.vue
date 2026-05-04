<template>
  <article class="paper-panel p-6">
    <div class="flex items-end justify-between">
      <div>
        <p class="section-kicker">复习日历</p>
        <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">
          {{ streak }} 天连续打卡
        </h3>
        <p class="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
          {{ todayPending > 0 ? `今日还有 ${todayPending} 道待复习` : '今日复习已完成' }}
        </p>
      </div>
      <RouterLink v-if="todayPending > 0" to="/review" class="hard-button-primary !min-h-9 !px-4 !py-1.5 text-sm">
        去复习
      </RouterLink>
    </div>

    <!-- Heatmap Grid -->
    <div class="mt-6 overflow-x-auto">
      <div class="inline-flex gap-[3px]">
        <div v-for="(week, wi) in weeks" :key="wi" class="flex flex-col gap-[3px]">
          <div
            v-for="(day, di) in week"
            :key="di"
            class="h-[14px] w-[14px] rounded-[3px] transition-colors"
            :class="heatmapColor(day.count)"
            :title="day.date ? `${day.date}: ${day.count} 次` : ''"
          ></div>
        </div>
      </div>
    </div>

    <!-- Legend -->
    <div class="mt-3 flex items-center justify-end gap-1 text-[10px] text-slate-400">
      <span>少</span>
      <div class="h-[10px] w-[10px] rounded-[2px] bg-slate-100 dark:bg-slate-800"></div>
      <div class="h-[10px] w-[10px] rounded-[2px] bg-accent/20"></div>
      <div class="h-[10px] w-[10px] rounded-[2px] bg-accent/50"></div>
      <div class="h-[10px] w-[10px] rounded-[2px] bg-accent/80"></div>
      <div class="h-[10px] w-[10px] rounded-[2px] bg-accent"></div>
      <span>多</span>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface HeatmapDay {
  date: string | null
  count: number
}

const props = defineProps<{
  heatmap?: Record<string, number>
  streak: number
  todayPending: number
}>()

const weeks = computed(() => {
  const data: HeatmapDay[][] = []
  const today = new Date()
  // Go back 12 weeks (84 days)
  const startDate = new Date(today)
  startDate.setDate(startDate.getDate() - 83)

  // Align to start of week (Monday)
  const dayOfWeek = startDate.getDay()
  const offset = dayOfWeek === 0 ? 6 : dayOfWeek - 1
  startDate.setDate(startDate.getDate() - offset)

  const current = new Date(startDate)
  let currentWeek: HeatmapDay[] = []

  while (current <= today) {
    const dateStr = current.toISOString().split('T')[0]
    currentWeek.push({
      date: dateStr,
      count: props.heatmap?.[dateStr] || 0
    })

    if (currentWeek.length === 7) {
      data.push(currentWeek)
      currentWeek = []
    }
    current.setDate(current.getDate() + 1)
  }

  // Pad remaining days in the last week
  while (currentWeek.length < 7) {
    currentWeek.push({ date: null, count: 0 })
  }
  if (currentWeek.some(d => d.date)) {
    data.push(currentWeek)
  }

  return data
})

const heatmapColor = (count: number) => {
  if (count === 0) return 'bg-slate-100 dark:bg-slate-800'
  if (count <= 2) return 'bg-accent/20'
  if (count <= 5) return 'bg-accent/50'
  if (count <= 10) return 'bg-accent/80'
  return 'bg-accent'
}
</script>
