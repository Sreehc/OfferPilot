import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const pageMeta = {
  dashboard: {
    title: '求职训练工作台',
    kicker: '首页',
    subtitle: '把题库训练、知识问答、模拟面试、学习计划、简历打磨和投递管理放进一条连续工作流。'
  },
  question: {
    title: '题库训练',
    kicker: '题库',
    subtitle: '围绕技术分类、难度和岗位方向做结构化刷题，为问答、面试和计划执行提供训练底座。'
  },
  chat: {
    title: '问答',
    kicker: 'RAG 问答',
    subtitle: '针对知识点、项目细节和追问方向发起带引用的知识问答。'
  },
  knowledge: {
    title: '知识库',
    kicker: '资料中心',
    subtitle: '统一管理系统资料、个人文档和后续简历、JD、项目语料。'
  },
  studyPlan: {
    title: '学习计划',
    kicker: '计划',
    subtitle: '先搭建求职训练计划入口，后续在 Phase 3 接入正式计划表、任务和完成状态。'
  },
  resume: {
    title: '简历助手',
    kicker: '简历',
    subtitle: '承接简历解析、项目问答、自我介绍和面试简历制作的独立入口。'
  },
  applications: {
    title: '投递管理',
    kicker: '投递',
    subtitle: '为 JD 分析、投递看板、真实面试记录和复盘建议建立主路径入口。'
  },
  cards: {
    title: '卡片强化',
    kicker: '辅助强化',
    subtitle: '卡片继续保留，用于承接专项巩固和薄弱点强化，不再作为产品主线入口。'
  },
  interview: {
    title: '模拟面试',
    kicker: '面试',
    subtitle: '按岗位方向组织文字或语音面试，沉淀诊断结果、追问和复盘。'
  },
  review: {
    title: '复习巩固',
    kicker: '辅助强化',
    subtitle: '处理到期复习、错题和面试薄弱点，作为计划执行的辅助闭环。'
  },
  community: {
    title: '社区',
    kicker: '外部交流',
    subtitle: '浏览问题、参与讨论和查看排行榜，本轮不调整其功能边界。'
  },
  analytics: {
    title: '数据分析',
    kicker: '洞察',
    subtitle: '从训练节奏、弱项趋势和结果变化观察求职准备进展。'
  },
  admin: {
    title: '管理后台',
    kicker: '治理',
    subtitle: '继续承接内容治理和系统配置，后续扩展 AI 日志、文档治理和提示词模板。'
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
          path: 'interview/history',
          name: 'interview-history',
          component: () => import('@/pages/interview/InterviewHistoryPage.vue'),
          meta: { title: '模拟面试历史', kicker: '面试', subtitle: '查看历次模拟面试记录、得分与复盘。' }
        },
        {
          path: 'interview/detail/:id',
          name: 'interview-detail',
          component: () => import('@/pages/interview/InterviewDetailPage.vue'),
          meta: { title: '模拟面试详情', kicker: '面试', subtitle: '按题目查看回答、评分、追问和建议卡片。' }
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
          meta: { title: '设置', kicker: '个人中心', subtitle: '管理账户安全、设备、数据导出和个人偏好。' }
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
