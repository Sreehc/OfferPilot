<template>
  <div class="analytics-cockpit space-y-5">
    <AnalyticsInsightBar :data="learningInsights" />

    <section class="shell-section-card p-4 sm:p-5">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="section-kicker">长期画像</p>
          <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">训练画像总览</h3>
          <p class="mt-1 text-sm text-secondary">这里先看长期能力、建议难度和持续薄弱点，再决定下一轮训练动作。</p>
        </div>
        <RouterLink :to="analyticsAgentLink" class="hard-button-secondary">
          刷新训练动作
        </RouterLink>
      </div>

      <div v-if="profileLoading" class="mt-5 flex h-[220px] items-center justify-center">
        <div class="text-center">
          <div class="mx-auto h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
          <p class="mt-3 text-sm text-secondary">正在加载训练画像...</p>
        </div>
      </div>
      <div v-else-if="!abilityProfile.categoryAbilities.length" class="mt-5">
        <EmptyState
          icon="chart"
          title="训练画像还没形成"
          description="先完成几轮题库、复盘或模拟面试，这里会开始沉淀长期趋势。"
          compact
        />
      </div>
      <div v-else class="mt-5 grid gap-4 xl:grid-cols-[300px_minmax(0,1fr)]">
        <div class="profile-summary-shell">
          <article class="profile-summary-card">
            <p class="profile-summary-card__label">综合能力</p>
            <p class="profile-summary-card__value">{{ Math.round(abilityProfile.overallAbility || 0) }}</p>
            <p class="profile-summary-card__hint">
              当前能力画像会结合面试记录、错题表现和 {{ abilityProfile.recordingReviewCount || 0 }} 次录音复盘持续刷新。
            </p>
          </article>
          <article class="profile-summary-card profile-summary-card--accent">
            <p class="profile-summary-card__label">建议难度</p>
            <p class="profile-summary-card__value">{{ difficultyLabel }}</p>
            <p class="profile-summary-card__hint">
              {{ abilityProfile.suggestedFocus ? `当前建议先收紧 ${abilityProfile.suggestedFocus}。` : '目前没有明确的单一薄弱主题。' }}
            </p>
          </article>
        </div>

        <div class="profile-detail-shell">
          <div>
            <p class="text-sm font-semibold text-ink">持续薄弱点</p>
            <div class="mt-3 flex flex-wrap gap-2">
              <span
                v-for="category in weakCategoryChips"
                :key="category"
                class="detail-pill"
              >
                {{ category }}
              </span>
            </div>
          </div>

          <div class="mt-5 grid gap-3 sm:grid-cols-2">
            <article
              v-for="item in profileCategoryCards"
              :key="`${item.categoryId}-${item.categoryName}`"
              class="profile-category-card"
              :class="{ 'profile-category-card--weak': item.isWeak }"
              @click="openTopicDetail(item.categoryId)"
            >
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="text-lg font-semibold text-ink">{{ item.categoryName }}</p>
                  <p class="mt-1 text-xs uppercase tracking-[0.16em] text-tertiary">
                    推荐难度 · {{ difficultyText(item.recommendedDifficulty) }}
                  </p>
                </div>
                <span class="font-mono text-2xl font-semibold text-ink">{{ Math.round(item.abilityScore) }}</span>
              </div>
              <p class="mt-3 text-sm leading-6 text-secondary">
                模拟面试 {{ item.interviewCount }} 场 · 录音复盘 {{ item.recordingReviewCount || 0 }} 次 · 错题 {{ item.wrongCount }} 题
              </p>
            </article>
          </div>

          <div v-if="topicDetailLoading || topicDetail" class="mt-5">
            <div v-if="topicDetailLoading" class="topic-detail-shell flex h-[220px] items-center justify-center">
              <div class="text-center">
                <div class="mx-auto h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
                <p class="mt-3 text-sm text-secondary">正在加载领域详情...</p>
              </div>
            </div>
            <div v-else-if="topicDetail" class="topic-detail-shell">
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p class="section-kicker">领域详情</p>
                  <h4 class="text-2xl font-semibold tracking-[-0.03em] text-ink">{{ topicDetail.categoryName }}</h4>
                  <p class="mt-2 text-sm leading-6 text-secondary">{{ topicDetail.summary }}</p>
                </div>
                <div class="topic-detail-score">
                  <span class="topic-detail-score__label">画像分</span>
                  <span class="topic-detail-score__value">{{ Math.round(topicDetail.abilityScore || 0) }}</span>
                </div>
              </div>

              <div class="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
                <article class="topic-detail-stat">
                  <p class="topic-detail-stat__label">模拟面试</p>
                  <p class="topic-detail-stat__value">{{ topicDetail.interviewCount }}</p>
                </article>
                <article class="topic-detail-stat">
                  <p class="topic-detail-stat__label">录音复盘</p>
                  <p class="topic-detail-stat__value">{{ topicDetail.recordingReviewCount || 0 }}</p>
                </article>
                <article class="topic-detail-stat">
                  <p class="topic-detail-stat__label">错题数</p>
                  <p class="topic-detail-stat__value">{{ topicDetail.wrongCount }}</p>
                </article>
                <article class="topic-detail-stat">
                  <p class="topic-detail-stat__label">待复盘</p>
                  <p class="topic-detail-stat__value">{{ topicDetail.dueCount }}</p>
                </article>
                <article class="topic-detail-stat">
                  <p class="topic-detail-stat__label">稳定率</p>
                  <p class="topic-detail-stat__value">{{ Math.round(topicDetail.masteryRate || 0) }}%</p>
                </article>
              </div>

              <div class="mt-5 grid gap-3 xl:grid-cols-[minmax(0,1fr)_300px]">
                <article class="topic-detail-panel">
                  <p class="topic-detail-panel__title">建议动作</p>
                  <ul class="topic-detail-list mt-3">
                    <li v-for="item in topicDetail.focusRecommendations" :key="item">{{ item }}</li>
                  </ul>
                </article>

                <article class="topic-detail-panel">
                  <p class="topic-detail-panel__title">最近趋势</p>
                  <div class="mt-3 space-y-2">
                    <div
                      v-for="point in topicDetail.recentScores"
                      :key="`${point.week}-${point.score}`"
                      class="topic-detail-point"
                    >
                      <span class="text-xs text-secondary">{{ point.week }}</span>
                      <span class="font-mono text-sm font-semibold text-ink">{{ Math.round(point.score || 0) }}</span>
                    </div>
                  </div>
                </article>
              </div>

              <div class="mt-5 flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p class="text-sm font-semibold text-ink">领域回顾</p>
                  <p class="mt-1 text-sm text-secondary">把画像、错题、复盘债务和趋势合成一份阶段性回顾。</p>
                </div>
                <div class="flex flex-wrap items-center gap-2">
                  <RouterLink v-if="topicDetail" :to="topicPlannerAgentLink" class="hard-button-secondary text-sm">
                    转成下一轮计划
                  </RouterLink>
                  <el-button :loading="retrospectiveLoading" class="action-button" @click="generateRetrospective">
                    生成领域回顾
                  </el-button>
                </div>
              </div>

              <div v-if="retrospectiveLoading" class="topic-retrospective-shell mt-5 flex h-[220px] items-center justify-center">
                <div class="text-center">
                  <div class="mx-auto h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
                  <p class="mt-3 text-sm text-secondary">正在生成领域回顾...</p>
                </div>
              </div>
              <div v-else-if="topicRetrospective" class="topic-retrospective-shell mt-5">
                <div class="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p class="section-kicker">Retrospective</p>
                    <h5 class="text-xl font-semibold tracking-[-0.03em] text-ink">{{ topicRetrospective.title }}</h5>
                    <p class="mt-2 text-sm leading-6 text-secondary">{{ topicRetrospective.summary }}</p>
                  </div>
                  <span class="topic-retrospective-stage" :class="`topic-retrospective-stage--${topicRetrospective.stage}`">
                    {{ retrospectiveStageLabel(topicRetrospective.stage) }}
                  </span>
                </div>

                <div class="mt-5 grid gap-4 xl:grid-cols-3">
                  <article class="topic-detail-panel">
                    <p class="topic-detail-panel__title">关键信号</p>
                    <ul class="topic-detail-list mt-3">
                      <li v-for="item in topicRetrospective.keySignals" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                  <article class="topic-detail-panel">
                    <p class="topic-detail-panel__title">当前风险</p>
                    <ul class="topic-detail-list mt-3">
                      <li v-for="item in topicRetrospective.riskSignals" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                  <article class="topic-detail-panel">
                    <p class="topic-detail-panel__title">下一步动作</p>
                    <ul class="topic-detail-list mt-3">
                      <li v-for="item in topicRetrospective.nextActions" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="shell-section-card p-4 sm:p-5">
      <div class="grid gap-4 xl:grid-cols-[288px_minmax(0,1fr)] xl:items-stretch">
        <aside class="analytics-overview">
          <div>
            <p class="section-kicker">本周总览</p>
            <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">准备趋势</h3>
          </div>

          <div class="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
            <article
              v-for="signal in summarySignals"
              :key="signal.label"
              class="analytics-overview-card"
              :class="signal.toneClass"
            >
              <p class="analytics-overview-card__label">{{ signal.label }}</p>
              <p class="analytics-overview-card__value">{{ signal.value }}</p>
              <p v-if="signal.detail" class="mt-2 text-xs leading-5 text-secondary">{{ signal.detail }}</p>
            </article>
          </div>
        </aside>

        <div class="analytics-main-chart">
          <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">主线进展</h3>
              <p class="mt-1 text-sm text-secondary">这里汇总计划、投递、简历和模拟面试的进展。</p>
            </div>
            <div class="mode-switch grid grid-cols-3 gap-2">
              <button
                v-for="w in weekOptions"
                :key="w.value"
                type="button"
                class="mode-switch__item"
                :class="{ 'mode-switch__item-active': selectedWeeks === w.value }"
                @click="changeWeeks(w.value)"
              >
                {{ w.label }}
              </button>
            </div>
          </div>

          <div v-if="trendLoading" class="mt-4 flex h-[340px] items-center justify-center">
            <div class="text-center">
              <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
              <p class="mt-3 text-sm text-secondary">正在加载准备趋势...</p>
            </div>
          </div>
          <div
            v-else-if="
              !trendData.planProgressTrend?.length &&
              !trendData.applicationActivityTrend?.length &&
              !trendData.resumeActivityTrend?.length &&
              !trendData.overallTrend?.length &&
              !trendData.reviewActivityTrend?.length
            "
            class="mt-4 flex h-[340px] items-center justify-center"
          >
            <EmptyState
              icon="chart"
              :title="EMPTY_STATE_COPY.analyticsTrend.title"
              :description="EMPTY_STATE_COPY.analyticsTrend.description"
              compact
            />
          </div>
          <div v-else class="mt-4">
            <div ref="trendChartRef" class="chart-shell h-[340px] w-full"></div>
          </div>
        </div>
      </div>
    </section>

    <section class="grid gap-4 xl:grid-cols-2">
      <article class="shell-section-card p-4 sm:p-5">
        <div class="flex items-center justify-between gap-3">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">复盘强度</h3>
          </div>
        </div>

        <div v-if="efficiencyLoading" class="mt-5 flex h-[260px] items-center justify-center">
          <div class="text-center">
            <div class="mx-auto h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
            <p class="mt-3 text-sm text-secondary">正在加载复盘强度...</p>
          </div>
        </div>
        <div v-else-if="!efficiencyData.efTrend?.length" class="mt-5 flex h-[260px] items-center justify-center">
          <EmptyState
            icon="review"
            :title="EMPTY_STATE_COPY.analyticsReviewIntensity.title"
            :description="EMPTY_STATE_COPY.analyticsReviewIntensity.description"
            compact
          />
        </div>
        <div v-else class="mt-5">
          <div class="grid gap-3 sm:grid-cols-3">
            <article class="data-slab p-4">
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">平均复盘系数</p>
              <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ efficiencyData.avgEaseFactor }}</p>
            </article>
            <article class="data-slab p-4">
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">到期待复盘</p>
              <p class="mt-3 font-mono text-3xl font-semibold text-ink">
                {{ latestReviewDebtCount }} 题
              </p>
            </article>
            <article class="data-slab p-4">
              <p class="text-[10px] font-semibold uppercase tracking-[0.22em] text-tertiary">连续天数</p>
              <p class="mt-3 font-mono text-3xl font-semibold text-ink">{{ efficiencyData.currentStreak }} 天</p>
            </article>
          </div>
          <div ref="efChartRef" class="chart-shell mt-4 h-[240px] w-full"></div>
        </div>
      </article>

      <article class="shell-section-card p-4 sm:p-5">
        <div class="flex items-center justify-between gap-3">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">复盘稳定性</h3>
          </div>
        </div>

        <div v-if="efficiencyLoading" class="mt-5 flex h-[260px] items-center justify-center">
          <div class="text-center">
            <div class="mx-auto h-6 w-6 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
            <p class="mt-3 text-sm text-secondary">正在加载复盘稳定性...</p>
          </div>
        </div>
        <div
          v-else-if="!efficiencyData.forgettingRateTrend?.length"
          class="mt-5 flex h-[260px] items-center justify-center"
        >
          <EmptyState
            icon="review"
            :title="EMPTY_STATE_COPY.analyticsReviewStability.title"
            :description="EMPTY_STATE_COPY.analyticsReviewStability.description"
            compact
          />
        </div>
        <div v-else class="mt-5">
          <div class="flex flex-wrap gap-2">
            <div v-for="(label, key) in ratingLabels" :key="key" class="rating-chip">
              <span class="h-2.5 w-2.5 rounded-full" :class="ratingColor(Number(key))"></span>
              <span>{{ label }}</span>
              <span class="font-mono text-ink">{{ efficiencyData.ratingDistribution?.[key] ?? 0 }}</span>
            </div>
          </div>
          <div ref="frChartRef" class="chart-shell mt-4 h-[240px] w-full"></div>
        </div>
      </article>
    </section>

    <section class="grid gap-4 xl:grid-cols-2">
      <article class="shell-section-card p-4 sm:p-5">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">分类错题进度</h3>
          </div>
        </div>

        <div v-if="!efficiencyLoading && categoryMasteryItems.length" class="mt-6 space-y-4">
          <article
            v-for="item in categoryMasteryItems"
            :key="`${item.categoryName}-${item.categoryId ?? 'na'}`"
            class="mastery-card"
            @click="item.categoryId ? openTopicDetail(item.categoryId) : undefined"
          >
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="text-lg font-semibold text-ink">{{ item.categoryName }}</p>
                <p class="mt-1 text-sm text-secondary">
                  已掌握 {{ item.masteredCards }}/{{ item.totalCards }} · 待复盘 {{ item.dueCount }}
                </p>
              </div>
              <div class="text-right">
                <p class="font-mono text-3xl font-semibold text-ink">{{ Math.round(item.masteryRate) }}%</p>
                <p class="text-xs font-semibold uppercase tracking-[0.18em] text-accent">稳定率</p>
              </div>
            </div>
            <div class="mastery-track mt-4">
              <span
                class="mastery-track__fill mastery-fill-cyan"
                :style="{ width: `${Math.round(item.masteryRate)}%` }"
              ></span>
            </div>
          </article>
        </div>
        <div v-else class="mt-5">
          <EmptyState
            icon="chart"
            :title="EMPTY_STATE_COPY.analyticsCategoryMastery.title"
            :description="EMPTY_STATE_COPY.analyticsCategoryMastery.description"
            compact
          />
        </div>
      </article>

      <article class="shell-section-card p-4 sm:p-5">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">错题状态分布</h3>
            <p class="mt-1 text-sm text-secondary">{{ totalMasteryCount }} 道题</p>
          </div>
        </div>

        <div v-if="!efficiencyLoading && hasMasteryData" class="mt-6 space-y-4">
          <article v-for="item in masteryItems" :key="item.label" class="mastery-card" :class="item.toneClass">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="text-lg font-semibold text-ink">{{ item.label }}</p>
              </div>
              <div class="text-right">
                <p class="font-mono text-3xl font-semibold text-ink">{{ item.count }}</p>
                <p class="text-xs font-semibold uppercase tracking-[0.18em]" :class="item.textClass">
                  {{ item.percent }}%
                </p>
              </div>
            </div>
            <div class="mastery-track mt-4">
              <span class="mastery-track__fill" :class="item.fillClass" :style="{ width: `${item.percent}%` }"></span>
            </div>
          </article>
        </div>
        <div v-else class="mt-5">
          <EmptyState
            icon="chart"
            :title="EMPTY_STATE_COPY.analyticsMasteryDistribution.title"
            :description="EMPTY_STATE_COPY.analyticsMasteryDistribution.description"
            compact
          />
        </div>
      </article>
    </section>

    <section class="shell-section-card p-4 sm:p-5">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div class="min-w-0">
          <h3 class="text-2xl font-semibold tracking-[-0.03em] text-ink">分类面试趋势</h3>
        </div>
      </div>

      <div v-if="trendLoading" class="mt-5 flex h-[300px] items-center justify-center">
        <div class="text-center">
          <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent"></div>
          <p class="mt-3 text-sm text-secondary">正在加载面试趋势...</p>
        </div>
      </div>
      <div v-else-if="!trendData.overallTrend?.length" class="mt-5 flex h-[300px] items-center justify-center">
        <EmptyState
          icon="chart"
          :title="EMPTY_STATE_COPY.analyticsInterviewTrend.title"
          :description="EMPTY_STATE_COPY.analyticsInterviewTrend.description"
          compact
        />
      </div>
      <div v-else class="mt-5">
        <div v-if="normalizedCategoryTrends.length" class="mb-4 flex flex-wrap gap-2">
          <button
            type="button"
            class="category-chip"
            :class="{ 'category-chip-active': selectedCategories.length === 0 }"
            @click="selectedCategories = []"
          >
            全部分类
          </button>
          <button
            v-for="cat in normalizedCategoryTrends"
            :key="cat.categoryId"
            type="button"
            class="category-chip"
            :class="{ 'category-chip-active': selectedCategories.includes(cat.categoryId) }"
            @click="toggleCategory(cat.categoryId)"
          >
            {{ cat.displayName }}
          </button>
        </div>
        <div ref="interviewTrendChartRef" class="chart-shell h-[300px] w-full"></div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { EMPTY_STATE_COPY } from '@/constants/productCopy'
import { useTheme } from '@/composables/useTheme'
import { readThemePalette } from '@/utils/theme'
import { buildAgentWorkbenchLocation } from '@/utils/agent'
import AnalyticsInsightBar from './AnalyticsInsightBar.vue'
import {
  createAnalyticsTopicRetrospectiveApi,
  fetchAbilityTrendApi,
  fetchAnalyticsProfileApi,
  fetchAnalyticsTopicProfileApi,
  fetchEfficiencyApi,
  fetchLearningInsightsApi
} from '@/api/analytics'
import type {
  AbilityProfile,
  AbilityTrend,
  EfficiencyData,
  LearningInsights,
  ProfileTopicDetail,
  ProfileTopicRetrospective
} from '@/types/api'

const weekOptions = [
  { label: '4 周', value: 4 },
  { label: '8 周', value: 8 },
  { label: '12 周', value: 12 }
]

const selectedWeeks = ref(12)
const selectedCategories = ref<number[]>([])
const trendLoading = ref(true)
const efficiencyLoading = ref(true)
const profileLoading = ref(true)
const trendData = ref<AbilityTrend>({
  weeks: [],
  reviewActivityTrend: [],
  reviewDebtTrend: [],
  masteredGrowthTrend: [],
  overallTrend: [],
  categoryTrends: [],
  planProgressTrend: [],
  applicationActivityTrend: [],
  resumeActivityTrend: []
})
const efficiencyData = ref<EfficiencyData>({
  avgEaseFactor: 2.5,
  efTrend: [],
  ratingDistribution: {},
  forgettingRateTrend: [],
  reviewDebtTrend: [],
  masteredGrowthTrend: [],
  masteryDistribution: {},
  contentTypeDistribution: {},
  categoryMastery: [],
  totalReviews: 0,
  currentStreak: 0,
  forgettingRate: 0
})
const learningInsights = ref<LearningInsights>({
  thisWeekAvgScore: 0,
  lastWeekAvgScore: 0,
  thisWeekInterviewCount: 0,
  lastWeekInterviewCount: 0,
  todayCompletionStatus: '',
  reviewDebtStatus: '',
  masteryGrowthStatus: '',
  planExecutionStatus: '',
  todayPlanCompletedTaskCount: 0,
  todayPlanTaskCount: 0,
  activePlanProgressRate: 0,
  activePlanTitle: '',
  applicationActiveCount: 0,
  applicationOfferCount: 0,
  applicationStatus: '',
  resumeCount: 0,
  latestResumeTitle: '',
  resumeReadinessStatus: '',
  interviewConversionStatus: '',
  nextAction: undefined,
  categoryChanges: [],
  bestStudyHours: []
})
const abilityProfile = ref<AbilityProfile>({
  overallAbility: 0,
  recommendedDifficulty: 'easy',
  recordingReviewCount: 0,
  categoryAbilities: [],
  weakCategories: [],
  suggestedFocus: null
})
const topicDetailLoading = ref(false)
const topicDetail = ref<ProfileTopicDetail | null>(null)
const retrospectiveLoading = ref(false)
const topicRetrospective = ref<ProfileTopicRetrospective | null>(null)
const { theme } = useTheme()

const trendChartRef = ref<HTMLElement | null>(null)
const interviewTrendChartRef = ref<HTMLElement | null>(null)
const efChartRef = ref<HTMLElement | null>(null)
const frChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null
let interviewTrendChart: echarts.ECharts | null = null
let efChart: echarts.ECharts | null = null
let frChart: echarts.ECharts | null = null

const ratingLabels: Record<number, string> = { 1: '重来', 2: '困难', 3: '良好', 4: '轻松' }
const ratingColor = (key: number) => {
  const map: Record<number, string> = {
    1: 'bg-[var(--bc-coral)]',
    2: 'bg-[var(--bc-amber)]',
    3: 'bg-[var(--bc-cyan)]',
    4: 'bg-[var(--bc-lime)]'
  }
  return map[key] || 'bg-[var(--text-tertiary)]'
}

const hasMasteryData = computed(() => {
  const d = efficiencyData.value.masteryDistribution
  return d && Object.values(d).some((v) => v > 0)
})

const totalMasteryCount = computed(() => {
  const d = efficiencyData.value.masteryDistribution
  return Object.values(d || {}).reduce((sum, value) => sum + value, 0)
})

const normalizedCategoryTrends = computed(() => {
  const byId = new Map<number, AbilityTrend['categoryTrends'][number]>()
  for (const trend of trendData.value.categoryTrends || []) {
    if (!byId.has(trend.categoryId)) {
      byId.set(trend.categoryId, trend)
    }
  }
  const deduped = [...byId.values()]
  const nameCount = deduped.reduce<Record<string, number>>((acc, item) => {
    const name = item.categoryName || `分类 ${item.categoryId}`
    acc[name] = (acc[name] || 0) + 1
    return acc
  }, {})
  return deduped.map((item) => ({
    ...item,
    displayName: (() => {
      const name = item.categoryName || `分类 ${item.categoryId}`
      return (nameCount[name] ?? 0) > 1 ? `${name} #${item.categoryId}` : name
    })()
  }))
})

const latestReviewDebtCount = computed(() => {
  const data = efficiencyData.value.reviewDebtTrend
  return data.length ? data[data.length - 1]!.reviewDebtCount : 0
})

const masteryItems = computed(() => {
  const d = efficiencyData.value.masteryDistribution || {}
  const total = totalMasteryCount.value || 1
  return [
    {
      label: '未开始',
      count: d.not_started ?? 0,
      percent: Math.round(((d.not_started ?? 0) / total) * 100),
      description: '',
      toneClass: 'mastery-card-coral',
      textClass: 'text-[var(--bc-coral)]',
      fillClass: 'mastery-fill-coral'
    },
    {
      label: '复习中',
      count: d.reviewing ?? 0,
      percent: Math.round(((d.reviewing ?? 0) / total) * 100),
      description: '',
      toneClass: 'mastery-card-amber',
      textClass: 'text-[var(--bc-amber)]',
      fillClass: 'mastery-fill-amber'
    },
    {
      label: '已掌握',
      count: d.mastered ?? 0,
      percent: Math.round(((d.mastered ?? 0) / total) * 100),
      description: '',
      toneClass: 'mastery-card-lime',
      textClass: 'text-[var(--bc-lime)]',
      fillClass: 'mastery-fill-lime'
    }
  ]
})

const categoryMasteryItems = computed(() => efficiencyData.value.categoryMastery || [])
const weakCategoryChips = computed(() => abilityProfile.value.weakCategories?.length
  ? abilityProfile.value.weakCategories
  : ['暂无持续薄弱点'])
const profileCategoryCards = computed(() => [...(abilityProfile.value.categoryAbilities || [])]
  .sort((left, right) => left.abilityScore - right.abilityScore)
  .slice(0, 4))
const difficultyLabel = computed(() => difficultyText(abilityProfile.value.recommendedDifficulty))
const analyticsAgentLink = computed(() =>
  buildAgentWorkbenchLocation({
    agentType: 'study_planner',
    triggerSource: 'analytics',
    contextRefs: ['analytics:profile', 'analytics:weak-topics', 'study-plan:active'],
    userPrompt: abilityProfile.value.suggestedFocus
      ? `围绕当前薄弱点“${abilityProfile.value.suggestedFocus}”刷新下一轮训练动作。`
      : '根据当前训练画像刷新下一轮训练动作。'
  })
)
const topicPlannerAgentLink = computed(() => {
  if (!topicDetail.value?.categoryId) {
    return analyticsAgentLink.value
  }
  return buildAgentWorkbenchLocation({
    agentType: 'study_planner',
    triggerSource: 'analytics',
    contextRefs: [
      'analytics:profile',
      'analytics:weak-topics',
      `analytics:topic:${topicDetail.value.categoryId}`
    ],
    userPrompt: `结合 ${topicDetail.value.categoryName} 的领域详情和回顾结果，生成下一轮训练动作。`
  })
})

const summarySignals = computed(() => [
  {
    label: '当前重点',
    value: learningInsights.value.activePlanTitle || '暂无计划',
    detail: learningInsights.value.planExecutionStatus || '生成一份训练计划后，这里会显示执行进度',
    toneClass: ''
  },
  {
    label: '活跃投递',
    value: `${learningInsights.value.applicationActiveCount ?? 0} 条`,
    detail: learningInsights.value.applicationStatus || '录入岗位后，这里会显示投递进展',
    toneClass: 'summary-slab-cyan'
  },
  {
    label: '简历准备',
    value: `${learningInsights.value.resumeCount ?? 0} 份`,
    detail: learningInsights.value.resumeReadinessStatus || '上传简历后，这里会显示准备状态',
    toneClass: 'summary-slab-lime'
  },
  {
    label: '本周模拟面试',
    value: `${learningInsights.value.thisWeekInterviewCount ?? 0} 场`,
    detail: learningInsights.value.interviewConversionStatus || '完成模拟面试后，这里会显示趋势变化',
    toneClass: 'summary-slab-amber'
  },
  {
    label: '到期待复盘',
    value: `${latestReviewDebtCount.value} 题`,
    detail: latestReviewDebtCount.value > 0 ? '处理到期错题后，安排新的训练。' : '目前没有到期错题，可以安排新的训练。',
    toneClass: ''
  }
])

const changeWeeks = (w: number) => {
  selectedWeeks.value = w
  void loadTrend()
}

const openTopicDetail = async (topicId: number) => {
  topicDetailLoading.value = true
  topicRetrospective.value = null
  try {
    const response = await fetchAnalyticsTopicProfileApi(String(topicId))
    topicDetail.value = response.data
  } finally {
    topicDetailLoading.value = false
  }
}

const retrospectiveStageLabel = (stage?: string) => {
  if (stage === 'stable') return '相对稳定'
  if (stage === 'needs_attention') return '优先处理'
  return '持续建设'
}

const generateRetrospective = async () => {
  if (!topicDetail.value?.categoryId) return
  retrospectiveLoading.value = true
  try {
    const response = await createAnalyticsTopicRetrospectiveApi(String(topicDetail.value.categoryId))
    topicRetrospective.value = response.data
  } catch {
    ElMessage.error('生成领域回顾失败，请稍后重试。')
  } finally {
    retrospectiveLoading.value = false
  }
}

const toggleCategory = (catId: number) => {
  const idx = selectedCategories.value.indexOf(catId)
  if (idx >= 0) {
    selectedCategories.value = selectedCategories.value.filter((id) => id !== catId)
  } else {
    selectedCategories.value = [...selectedCategories.value, catId]
  }
}

const disposeTrendCharts = () => {
  trendChart?.dispose()
  interviewTrendChart?.dispose()
  trendChart = null
  interviewTrendChart = null
}

const loadTrend = async () => {
  trendLoading.value = true
  disposeTrendCharts()
  try {
    const res = await fetchAbilityTrendApi(
      selectedWeeks.value,
      selectedCategories.value.length > 0 ? selectedCategories.value : undefined
    )
    trendData.value = res.data
  } finally {
    trendLoading.value = false
    await nextTick()
    renderTrendChart()
    renderInterviewTrendChart()
  }
}

const loadEfficiency = async () => {
  efficiencyLoading.value = true
  profileLoading.value = true
  try {
    const [efficiencyRes, insightsRes, profileRes] = await Promise.all([
      fetchEfficiencyApi(),
      fetchLearningInsightsApi(),
      fetchAnalyticsProfileApi()
    ])
    efficiencyData.value = efficiencyRes.data
    learningInsights.value = insightsRes.data
    abilityProfile.value = profileRes.data
    nextTick(() => {
      renderEFChart()
      renderFRChart()
    })
  } finally {
    efficiencyLoading.value = false
    profileLoading.value = false
  }
}

const difficultyText = (value?: string) => {
  if (value === 'hard') return '高强度'
  if (value === 'medium') return '中强度'
  return '基础巩固'
}

const buildChartBase = () => {
  const palette = readThemePalette()
  return {
    textStyle: { color: palette.textSecondary, fontFamily: 'JetBrains Mono, monospace' },
    grid: { left: 44, right: 48, top: 24, bottom: 38, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: palette.surfaceCard,
      borderColor: palette.borderSubtle,
      textStyle: { color: palette.textPrimary }
    },
    xAxis: {
      axisLine: { lineStyle: { color: palette.borderSubtle } },
      axisLabel: { color: palette.textSecondary, fontSize: 11 }
    },
    yAxis: {
      splitLine: { lineStyle: { color: palette.borderSubtle } },
      axisLabel: { color: palette.textSecondary, fontSize: 11 }
    }
  }
}

const renderTrendChart = () => {
  if (!trendChartRef.value) return
  const weeks = trendData.value.weeks || []
  const planProgress = trendData.value.planProgressTrend || []
  const applications = trendData.value.applicationActivityTrend || []
  const resumes = trendData.value.resumeActivityTrend || []
  const interviews = trendData.value.overallTrend || []
  if (!weeks.length && !planProgress.length && !applications.length && !resumes.length && !interviews.length) return

  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const xAxisData = weeks.length
    ? weeks
    : [...new Set([
        ...planProgress.map((item) => item.week),
        ...applications.map((item) => item.week),
        ...resumes.map((item) => item.week),
        ...interviews.map((item) => item.week)
      ])]
  const palette = readThemePalette()
  const base = buildChartBase()
  trendChart.setOption(
    {
      ...base,
      legend: {
        data: ['计划进度', '活跃投递', '简历更新', '面试均分'],
        bottom: 0,
        textStyle: { fontSize: 11, color: palette.textSecondary }
      },
      xAxis: {
        ...(base.xAxis as object),
        type: 'category',
        data: xAxisData
      },
      yAxis: [
        {
          ...(base.yAxis as object),
          type: 'value',
          min: 0,
          max: 100,
          axisLabel: { color: palette.textSecondary, fontSize: 11, formatter: '{value}%', margin: 10 }
        },
        {
          ...(base.yAxis as object),
          type: 'value',
          min: 0,
          position: 'right',
          splitLine: { show: false },
          axisLabel: {
            color: palette.textSecondary,
            fontSize: 11,
            margin: 12,
            formatter: (value: number) => Number(value).toLocaleString('zh-CN')
          }
        }
      ],
      series: [
        {
          name: '计划进度',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          data: xAxisData.map((week) => planProgress.find((item) => item.week === week)?.progressRate ?? null),
          lineStyle: { width: 3, color: palette.cyan },
          itemStyle: { color: palette.cyan }
        },
        {
          name: '活跃投递',
          type: 'bar',
          yAxisIndex: 1,
          data: xAxisData.map((week) => applications.find((item) => item.week === week)?.activeCount ?? null),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: palette.coral },
              { offset: 1, color: `${palette.coral}48` }
            ]),
            borderRadius: [8, 8, 0, 0]
          }
        },
        {
          name: '简历更新',
          type: 'line',
          yAxisIndex: 1,
          smooth: true,
          symbol: 'circle',
          data: xAxisData.map((week) => resumes.find((item) => item.week === week)?.uploadCount ?? null),
          lineStyle: { width: 2, color: palette.lime },
          itemStyle: { color: palette.lime }
        },
        {
          name: '面试均分',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          data: xAxisData.map((week) => interviews.find((item) => item.week === week)?.score ?? null),
          lineStyle: { width: 2, color: palette.amber },
          itemStyle: { color: palette.amber }
        }
      ]
    },
    true
  )
}

const renderInterviewTrendChart = () => {
  if (!interviewTrendChartRef.value || !trendData.value.overallTrend?.length) return

  if (!interviewTrendChart) {
    interviewTrendChart = echarts.init(interviewTrendChartRef.value)
  }

  const palette = readThemePalette()
  const chartColors = palette.chartColors
  const base = buildChartBase()
  const weeks = trendData.value.weeks || []
  const series: echarts.SeriesOption[] = [
    {
      name: '综合分数',
      type: 'line',
      data: weeks.map((w) => trendData.value.overallTrend.find((p) => p.week === w)?.score ?? null),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 3, color: chartColors[0] },
      itemStyle: { color: chartColors[0] }
    }
  ]

  const catTrends =
    selectedCategories.value.length > 0
      ? normalizedCategoryTrends.value.filter((c) => selectedCategories.value.includes(c.categoryId))
      : normalizedCategoryTrends.value

  catTrends.forEach((cat, idx) => {
    series.push({
      name: cat.displayName,
      type: 'line',
      data: weeks.map((w) => cat.points.find((p) => p.week === w)?.score ?? null),
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      lineStyle: { width: 2, color: chartColors[(idx + 1) % chartColors.length] },
      itemStyle: { color: chartColors[(idx + 1) % chartColors.length] }
    })
  })

  interviewTrendChart.setOption(
    {
      ...base,
      legend: {
        data: series.map((item) => item.name as string),
        bottom: 0,
        textStyle: { fontSize: 11, color: palette.textSecondary }
      },
      xAxis: {
        ...(base.xAxis as object),
        type: 'category',
        data: weeks
      },
      yAxis: {
        ...(base.yAxis as object),
        type: 'value',
        min: 0,
        max: 100
      },
      series
    },
    true
  )
}

const renderEFChart = () => {
  if (!efChartRef.value || !efficiencyData.value.efTrend?.length) return
  if (!efChart) {
    efChart = echarts.init(efChartRef.value)
  }

  const palette = readThemePalette()
  const base = buildChartBase()
  const data = efficiencyData.value.efTrend
  efChart.setOption(
    {
      ...base,
      xAxis: {
        ...(base.xAxis as object),
        type: 'category',
        data: data.map((d) => d.week),
        axisLabel: { color: palette.textSecondary, fontSize: 10, rotate: 24 }
      },
      yAxis: {
        ...(base.yAxis as object),
        type: 'value',
        min: 1.3,
        max: 3.2
      },
      series: [
        {
          type: 'line',
          data: data.map((d) => d.avgEF),
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { color: palette.cyan, width: 3 },
          itemStyle: { color: palette.cyan },
          markLine: {
            symbol: 'none',
            lineStyle: { type: 'dashed', color: palette.borderStrong },
            data: [
              { yAxis: 2.5, label: { formatter: '2.5', color: palette.amber } },
              { yAxis: 1.3, label: { formatter: '1.3', color: palette.coral }, lineStyle: { color: palette.coral } }
            ]
          }
        }
      ]
    },
    true
  )
}

const renderFRChart = () => {
  if (!frChartRef.value || !efficiencyData.value.forgettingRateTrend?.length) return
  if (!frChart) {
    frChart = echarts.init(frChartRef.value)
  }

  const palette = readThemePalette()
  const base = buildChartBase()
  const data = efficiencyData.value.forgettingRateTrend
  frChart.setOption(
    {
      ...base,
      xAxis: {
        ...(base.xAxis as object),
        type: 'category',
        data: data.map((d) => d.week),
        axisLabel: { color: palette.textSecondary, fontSize: 10, rotate: 24 }
      },
      yAxis: {
        ...(base.yAxis as object),
        type: 'value',
        min: 0,
        max: 100,
        axisLabel: { color: palette.textSecondary, fontSize: 11, formatter: '{value}%' }
      },
      series: [
        {
          name: '遗忘率',
          type: 'bar',
          data: data.map((d) => +(d.forgettingRate * 100).toFixed(1)),
          barWidth: '48%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: palette.coral },
              { offset: 1, color: `${palette.coral}4d` }
            ]),
            borderRadius: [8, 8, 0, 0]
          }
        },
        {
          name: '重来次数',
          type: 'line',
          data: data.map((d) => d.againCount),
          smooth: true,
          symbol: 'circle',
          symbolSize: 5,
          lineStyle: { width: 2, color: palette.amber },
          itemStyle: { color: palette.amber }
        }
      ]
    },
    true
  )
}

const handleResize = () => {
  trendChart?.resize()
  interviewTrendChart?.resize()
  efChart?.resize()
  frChart?.resize()
}

onMounted(() => {
  void loadTrend()
  void loadEfficiency()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  interviewTrendChart?.dispose()
  efChart?.dispose()
  frChart?.dispose()
  trendChart = null
  interviewTrendChart = null
  efChart = null
  frChart = null
})

watch(selectedCategories, () => {
  void loadTrend()
})

watch(theme, () => {
  nextTick(() => {
    renderTrendChart()
    renderInterviewTrendChart()
    renderEFChart()
    renderFRChart()
  })
})
</script>

<style scoped>
.mode-switch {
  width: min(100%, 320px);
  border: 1px solid var(--bc-border-subtle);
  border-radius: 999px;
  background: var(--interactive-bg);
  padding: 4px;
}

.dark .mode-switch {
  background: var(--interactive-bg);
}

.mode-switch__item {
  min-height: 40px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--bc-ink-secondary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
  transition:
    color 180ms ease,
    background-color 180ms ease,
    box-shadow 180ms ease;
}

.mode-switch__item:hover {
  background: rgba(var(--bc-accent-rgb), 0.08);
  color: var(--bc-ink);
}

.mode-switch__item-active {
  background: rgba(var(--bc-accent-rgb), 0.12);
  color: var(--bc-ink);
}

.analytics-overview {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.profile-summary-shell {
  display: grid;
  gap: 12px;
}

.profile-summary-card {
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background:
    linear-gradient(180deg, rgba(var(--bc-cyan-rgb), 0.08), transparent 55%),
    var(--panel-bg);
  padding: 18px;
}

.profile-summary-card--accent {
  background:
    linear-gradient(180deg, rgba(var(--bc-amber-rgb), 0.08), transparent 55%),
    var(--panel-bg);
}

.profile-summary-card__label {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--bc-ink-tertiary);
}

.profile-summary-card__value {
  margin-top: 0.9rem;
  font-family: theme('fontFamily.mono');
  font-size: clamp(2rem, 3vw, 2.8rem);
  font-weight: 700;
  line-height: 1;
  color: var(--bc-ink);
}

.profile-summary-card__hint {
  margin-top: 0.75rem;
  font-size: 0.9rem;
  line-height: 1.7;
  color: var(--bc-ink-secondary);
}

.profile-detail-shell {
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 18px;
}

.detail-pill {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  border-radius: 999px;
  border: 1px solid rgba(var(--bc-coral-rgb), 0.24);
  background: rgba(var(--bc-coral-rgb), 0.1);
  padding: 0 12px;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.profile-category-card {
  cursor: pointer;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-bg);
  padding: 16px;
  transition:
    transform 180ms ease,
    border-color 180ms ease,
    box-shadow 180ms ease;
}

.profile-category-card:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--bc-cyan-rgb), 0.28);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.profile-category-card--weak {
  border-color: rgba(var(--bc-coral-rgb), 0.28);
  background:
    linear-gradient(180deg, rgba(var(--bc-coral-rgb), 0.08), transparent 58%),
    var(--panel-bg);
}

.topic-detail-shell {
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background:
    linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.05), transparent 62%),
    var(--panel-bg);
  padding: 18px;
}

.topic-detail-score {
  display: inline-flex;
  min-width: 84px;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.topic-detail-score__label {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--bc-ink-tertiary);
}

.topic-detail-score__value {
  font-family: theme('fontFamily.mono');
  font-size: clamp(2rem, 3vw, 2.6rem);
  font-weight: 700;
  line-height: 1;
  color: var(--bc-ink);
}

.topic-detail-stat {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 14px 16px;
}

.topic-detail-stat__label {
  font-size: 0.74rem;
  color: var(--bc-ink-secondary);
}

.topic-detail-stat__value {
  margin-top: 10px;
  font-family: theme('fontFamily.mono');
  font-size: 1.9rem;
  font-weight: 700;
  line-height: 1;
  color: var(--bc-ink);
}

.topic-detail-panel {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 16px;
}

.topic-detail-panel__title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--bc-ink);
}

.topic-detail-list {
  display: grid;
  gap: 10px;
  padding-left: 18px;
  color: var(--bc-ink-secondary);
}

.topic-detail-list li {
  line-height: 1.7;
}

.topic-detail-point {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 999px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 10px 12px;
}

.topic-retrospective-shell {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background:
    radial-gradient(circle at top right, rgba(var(--bc-cyan-rgb), 0.08), transparent 38%),
    var(--panel-bg);
  padding: 18px;
}

.topic-retrospective-stage {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  border-radius: 999px;
  padding: 0 12px;
  font-size: 0.76rem;
  font-weight: 700;
}

.topic-retrospective-stage--stable {
  background: rgba(var(--bc-lime-rgb), 0.14);
  color: #335c12;
}

.topic-retrospective-stage--building {
  background: rgba(var(--bc-cyan-rgb), 0.14);
  color: #0f4d68;
}

.topic-retrospective-stage--needs_attention {
  background: rgba(var(--bc-coral-rgb), 0.14);
  color: #7a241f;
}

.analytics-overview-card {
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-muted);
  padding: 0.95rem 1rem;
}

.analytics-overview-card__label {
  font-size: 0.78rem;
  color: var(--bc-ink-secondary);
}

.analytics-overview-card__value {
  margin-top: 0.55rem;
  font-family: theme('fontFamily.mono');
  font-size: clamp(1.8rem, 2vw, 2.4rem);
  font-weight: 700;
  line-height: 1;
  color: var(--bc-ink);
}

.analytics-main-chart {
  min-width: 0;
}

.insight-card {
  border-radius: calc(var(--radius-md) + 2px);
  border: 1px solid var(--bc-line);
  background: var(--panel-muted);
  padding: 20px;
}

.insight-card-risk {
  border-color: rgba(255, 107, 107, 0.35);
}

.insight-card-cyan {
  border-color: rgba(85, 214, 190, 0.35);
}

.insight-card-lime {
  border-color: rgba(159, 232, 112, 0.45);
}

.signal-lane {
  border-radius: calc(var(--radius-sm) + 2px);
  border: 1px solid var(--bc-line);
  background: var(--panel-muted);
  padding: 14px 16px;
}

.chart-shell {
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background: linear-gradient(180deg, rgba(var(--bc-accent-rgb), 0.04), transparent 26%), var(--panel-bg);
}

.category-chip {
  min-height: 40px;
  border-radius: 999px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--interactive-bg);
  padding: 0 15px;
  font-size: 12px;
  font-weight: 700;
  color: var(--bc-ink-secondary);
}

.category-chip-active {
  border-color: rgba(var(--bc-accent-rgb), 0.18);
  background: rgba(var(--bc-accent-rgb), 0.12);
  color: var(--bc-ink);
}

.rating-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid var(--bc-border-subtle);
  background: var(--interactive-bg);
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 700;
  color: var(--bc-ink-secondary);
}

.mastery-card {
  cursor: pointer;
  border-radius: calc(var(--radius-md) - 2px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--panel-bg);
  padding: 18px;
}

.mastery-card-coral {
  border-color: rgba(255, 107, 107, 0.25);
}

.mastery-card-amber {
  border-color: rgba(255, 183, 77, 0.25);
}

.mastery-card-lime {
  border-color: rgba(159, 232, 112, 0.28);
}

.mastery-track {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
}

.mastery-track__fill {
  display: block;
  height: 100%;
  border-radius: 999px;
}

.mastery-fill-coral {
  background: linear-gradient(90deg, rgba(255, 107, 107, 0.68), rgba(255, 107, 107, 0.95));
}

.mastery-fill-amber {
  background: linear-gradient(90deg, rgba(255, 183, 77, 0.68), rgba(255, 183, 77, 0.95));
}

.mastery-fill-lime {
  background: linear-gradient(90deg, rgba(159, 232, 112, 0.68), rgba(159, 232, 112, 0.95));
}

.mastery-fill-cyan {
  background: linear-gradient(90deg, rgba(var(--bc-cyan-rgb), 0.62), var(--bc-cyan));
}

@media (max-width: 768px) {
  .mode-switch {
    width: 100%;
  }
}
</style>
