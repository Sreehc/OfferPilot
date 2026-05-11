import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const pageMeta = {
  dashboard: {
    title: '首页'
  },
  chat: {
    title: '问答'
  },
  knowledge: {
    title: '知识库'
  },
  cards: {
    title: '今日卡片'
  },
  interview: {
    title: '面试诊断'
  },
  review: {
    title: '复习'
  },
  community: {
    title: '社区'
  },
  analytics: {
    title: '数据分析'
  },
  admin: {
    title: '管理后台'
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
          meta: { title: '面试诊断历史' }
        },
        {
          path: 'interview/detail/:id',
          name: 'interview-detail',
          component: () => import('@/pages/interview/InterviewDetailPage.vue'),
          meta: { title: '面试详情' }
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
          meta: { title: '发起提问' }
        },
        {
          path: 'community/question/:id',
          name: 'community-question-detail',
          component: () => import('@/pages/community/CommunityQuestionDetail.vue'),
          meta: { title: '问题详情' }
        },
        {
          path: 'community/leaderboard',
          name: 'community-leaderboard',
          component: () => import('@/pages/community/LeaderboardPage.vue'),
          meta: { title: '社区排行榜' }
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
          meta: { title: '设置' }
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
