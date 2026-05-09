import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const pageMeta = {
  dashboard: {
    kicker: '今日记忆任务',
    title: '今天先完成你的卡片复习进度',
    subtitle: '先处理今日卡片，再用问答、面试诊断和分析继续补强。'
  },
  chat: {
    kicker: '辅助问答',
    title: '用资料问答补齐卡片后的疑问',
    subtitle: '带着知识库上下文提问，快速澄清卡片里还没吃透的点。'
  },
  knowledge: {
    kicker: '知识库',
    title: '管理会进入卡片系统的学习资料',
    subtitle: '上传、筛选和整理资料，为今日卡片与问答提供内容来源。'
  },
  cards: {
    kicker: '今日卡片',
    title: '记忆工作台',
    subtitle: '处理今天该学和该复习的卡片，持续推进掌握度。'
  },
  interview: {
    kicker: '面试诊断',
    title: '在卡片之外做一次进阶诊断',
    subtitle: '完成限时练习，查看评分、点评和追问，验证是否真正理解。'
  },
  review: {
    kicker: '复习中心',
    title: '处理到期复习与积压问题',
    subtitle: '统一完成今日复习、查看积压项，并回看所有需要巩固的题目。'
  },
  community: {
    kicker: '学习社区',
    title: '在交流中补齐盲点',
    subtitle: '提问、讨论、互助，通过社区贡献积累积分和等级。'
  },
  analytics: {
    kicker: '数据分析',
    title: '看清你的学习节奏和问题分布',
    subtitle: '查看学习趋势、复习情况和当前重点。'
  },
  admin: {
    kicker: '管理后台',
    title: '直接在站内处理内容与数据',
    subtitle: '管理用户、内容、题库和文档。'
  }
} as const

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      meta: { requiresAuth: true },
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/pages/dashboard/DashboardPage.vue'),
          meta: pageMeta.dashboard
        },
        {
          path: 'chat',
          name: 'chat',
          component: () => import('@/pages/chat/ChatPage.vue'),
          meta: pageMeta.chat
        },
        {
          path: 'knowledge',
          name: 'knowledge',
          component: () => import('@/pages/knowledge/KnowledgePage.vue'),
          meta: pageMeta.knowledge
        },
        {
          path: 'cards',
          name: 'cards',
          component: () => import('@/pages/cards/CardsPage.vue'),
          meta: pageMeta.cards
        },
        {
          path: 'interview',
          name: 'interview',
          component: () => import('@/pages/interview/InterviewPage.vue'),
          meta: pageMeta.interview
        },
        {
          path: 'interview/history',
          name: 'interview-history',
          component: () => import('@/pages/interview/InterviewHistoryPage.vue'),
          meta: { kicker: '面试历史', title: '面试诊断历史', subtitle: '查看所有已完成的诊断记录与复盘。' }
        },
        {
          path: 'interview/detail/:id',
          name: 'interview-detail',
          component: () => import('@/pages/interview/InterviewDetailPage.vue'),
          meta: { kicker: '面试详情', title: '面试详情', subtitle: '查看面试复盘和答案对比。' }
        },
        {
          path: 'wrong',
          name: 'wrong',
          redirect: { path: '/review', query: { tab: 'all' } }
        },
        {
          path: 'review',
          name: 'review',
          component: () => import('@/pages/review/ReviewPage.vue'),
          meta: pageMeta.review
        },
        {
          path: 'community',
          name: 'community',
          component: () => import('@/pages/community/CommunityPage.vue'),
          meta: pageMeta.community
        },
        {
          path: 'community/submit',
          name: 'community-submit',
          component: () => import('@/pages/community/CommunitySubmitPage.vue'),
          meta: { kicker: '发起提问', title: '发起提问', subtitle: '分享你的问题，获得社区帮助。' }
        },
        {
          path: 'community/question/:id',
          name: 'community-question-detail',
          component: () => import('@/pages/community/CommunityQuestionDetail.vue'),
          meta: { kicker: '问题详情', title: '问题详情', subtitle: '查看问题和回答。' }
        },
        {
          path: 'community/leaderboard',
          name: 'community-leaderboard',
          component: () => import('@/pages/community/LeaderboardPage.vue'),
          meta: { kicker: '排行榜', title: '社区排行榜', subtitle: '查看社区贡献排名。' }
        },
        {
          path: 'analytics',
          name: 'analytics',
          component: () => import('@/pages/analytics/AnalyticsPage.vue'),
          meta: pageMeta.analytics
        },
        {
          path: 'admin',
          name: 'admin',
          component: () => import('@/pages/admin/AdminPage.vue'),
          meta: { ...pageMeta.admin, requiresAdmin: true }
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('@/pages/settings/SettingsPage.vue'),
          meta: { kicker: '账户安全', title: '账户设置', subtitle: '管理设备、登录安全和数据导出。' }
        }
      ]
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/auth/LoginPage.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/pages/auth/RegisterPage.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/verify-2fa',
      name: 'verify-2fa',
      component: () => import('@/pages/auth/TwoFactorVerifyPage.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/pages/NotFoundPage.vue')
    }
  ]
})

let restored = false

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!restored && authStore.token) {
    restored = true
    await authStore.restoreProfile()
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin && authStore.user?.role !== 'ADMIN') {
    return '/dashboard'
  }

  if (to.meta.guestOnly && authStore.isLoggedIn) {
    return '/dashboard'
  }

  return true
})

export default router
