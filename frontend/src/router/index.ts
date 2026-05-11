import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const pageMeta = {
  dashboard: {
    title: '首页'
  },
  chat: {
    title: '问答',
    subtitle: '直接提问，或结合知识库继续追问。'
  },
  knowledge: {
    title: '知识库'
  },
  cards: {
    title: '今日卡片'
  },
  interview: {
    title: '面试诊断',
    subtitle: '做一次限时练习，检查你是否真的理解。'
  },
  review: {
    title: '复习中心'
  },
  community: {
    title: '学习社区',
    subtitle: '提问、讨论，补齐自己的盲点。'
  },
  analytics: {
    title: '数据分析',
    subtitle: '看清最近的推进状态和当前重点。'
  },
  admin: {
    title: '管理后台',
    subtitle: '处理用户、内容、题库和文档。'
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
          meta: { title: '面试诊断历史', subtitle: '查看历史记录，并把需要的结果加入今日卡片。' }
        },
        {
          path: 'interview/detail/:id',
          name: 'interview-detail',
          component: () => import('@/pages/interview/InterviewDetailPage.vue'),
          meta: { title: '面试详情', subtitle: '先看总分和低分题，再决定是否加入今日卡片。' }
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
          meta: { title: '发起提问', subtitle: '写清背景、现象和已尝试的方法。' }
        },
        {
          path: 'community/question/:id',
          name: 'community-question-detail',
          component: () => import('@/pages/community/CommunityQuestionDetail.vue'),
          meta: { title: '问题详情', subtitle: '看问题、看回答，或直接参与讨论。' }
        },
        {
          path: 'community/leaderboard',
          name: 'community-leaderboard',
          component: () => import('@/pages/community/LeaderboardPage.vue'),
          meta: { title: '社区排行榜', subtitle: '按贡献度查看社区排名。' }
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
          meta: { title: '账户设置', subtitle: '管理设备、登录安全和数据导出。' }
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
