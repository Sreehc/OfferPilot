import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const pageMeta = {
  dashboard: {
    title: '求职训练工作台'
  },
  question: {
    title: '题库训练'
  },
  chat: {
    title: '问答'
  },
  knowledge: {
    title: '知识库'
  },
  studyPlan: {
    title: '学习计划'
  },
  resume: {
    title: '简历助手'
  },
  applications: {
    title: '投递管理'
  },
  interview: {
    title: '模拟面试'
  },
  review: {
    title: '复习巩固'
  },
  wrong: {
    title: '错题本'
  },
  community: {
    title: '社区'
  },
  analytics: {
    title: '数据分析'
  },
  admin: {
    title: '管理后台'
  },
  favorites: {
    title: '我的收藏'
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
          path: 'question',
          name: 'question',
          component: () => import('@/pages/question/QuestionBankPage.vue'),
          meta: pageMeta.question
        },
        {
          path: 'knowledge',
          name: 'knowledge',
          component: () => import('@/pages/knowledge/KnowledgePage.vue'),
          meta: pageMeta.knowledge
        },
        {
          path: 'knowledge/java-basics',
          redirect: '/knowledge'
        },
        {
          path: 'interview',
          name: 'interview',
          component: () => import('@/pages/interview/InterviewPage.vue'),
          meta: pageMeta.interview
        },
        {
          path: 'study-plan',
          name: 'study-plan',
          component: () => import('@/pages/study-plan/StudyPlanPage.vue'),
          meta: pageMeta.studyPlan
        },
        {
          path: 'resume',
          name: 'resume',
          component: () => import('@/pages/resume/ResumeAssistantPage.vue'),
          meta: pageMeta.resume
        },
        {
          path: 'applications',
          name: 'applications',
          component: () => import('@/pages/applications/ApplicationBoardPage.vue'),
          meta: pageMeta.applications
        },
        {
          path: 'applications/:id',
          name: 'application-detail',
          component: () => import('@/pages/applications/ApplicationDetailPage.vue'),
          meta: { title: '投递详情' }
        },
        {
          path: 'interview/history',
          redirect: '/interview'
        },
        {
          path: 'interview/detail/:id',
          name: 'interview-detail',
          component: () => import('@/pages/interview/InterviewDetailPage.vue'),
          meta: { title: '模拟面试详情' }
        },
        {
          path: 'favorites',
          name: 'favorites',
          component: () => import('@/pages/favorites/FavoritesPage.vue'),
          meta: pageMeta.favorites
        },
        {
          path: 'wrong',
          name: 'wrong',
          component: () => import('@/pages/wrong/WrongBookPage.vue'),
          meta: pageMeta.wrong
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
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('@/pages/auth/ForgotPasswordPage.vue'),
      meta: { guestOnly: true, title: '找回密码' }
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
