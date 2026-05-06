import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const pageMeta = {
  dashboard: {
    kicker: '学习概览',
    title: '今日学习概览',
    subtitle: '先完成今天最重要的训练，再查看分析、趋势和历史记录。'
  },
  chat: {
    kicker: '智能问答',
    title: '带着资料上下文来提问',
    subtitle: '普通问答和知识问答放在同一入口，方便后续把问题直接带回复习链路。'
  },
  knowledge: {
    kicker: '知识库',
    title: '集中管理你的学习资料',
    subtitle: '内置资料库支持检索和引用，也可以上传自己的学习材料统一管理。'
  },
  interview: {
    kicker: '模拟面试',
    title: '先把真实面试前的压力练熟',
    subtitle: '每场控制在 3-5 题，通过评分、点评和追问把问题沉淀到后续复习。'
  },
  wrong: {
    kicker: '错题本',
    title: '把薄弱回答变成后续复习重点',
    subtitle: '错题不只是归档，而是驱动下一步复习与计划生成的基础资产。'
  },
  review: {
    kicker: '间隔复习',
    title: '在合适的时间复习，而不是天天重复',
    subtitle: '基于遗忘曲线自动调度复习时机，让每一次复习都恰好在快要忘记的时候。'
  },
  community: {
    kicker: '学习社区',
    title: '在交流中补齐盲点',
    subtitle: '提问、讨论、互助，通过社区贡献积累积分和等级。'
  },
  plan: {
    kicker: '学习计划',
    title: '把薄弱点拆成每天能完成的任务',
    subtitle: '计划模块把薄弱点和错题拆成可完成的节奏，而不是停留在泛泛建议。'
  },
  analytics: {
    kicker: '数据分析',
    title: '看清你的学习节奏和问题分布',
    subtitle: '从趋势、效率、时段等维度深入分析你的学习模式。'
  },
  admin: {
    kicker: '管理后台',
    title: '直接在站内处理内容与数据',
    subtitle: '后台仍然嵌在同一个应用里，先服务题库、知识库和分类管理。'
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
          path: 'interview',
          name: 'interview',
          component: () => import('@/pages/interview/InterviewPage.vue'),
          meta: pageMeta.interview
        },
        {
          path: 'interview/history',
          name: 'interview-history',
          component: () => import('@/pages/interview/InterviewHistoryPage.vue'),
          meta: { kicker: '面试历史', title: '面试历史记录', subtitle: '查看所有已完成的面试记录。' }
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
          component: () => import('@/pages/wrong/WrongPage.vue'),
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
          path: 'plan',
          name: 'plan',
          component: () => import('@/pages/plan/PlanPage.vue'),
          meta: pageMeta.plan
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
          meta: { kicker: '账户安全', title: '账户设置', subtitle: '管理已登录设备、查看登录历史，保障账户安全。' }
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
