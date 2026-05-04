import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const pageMeta = {
  dashboard: {
    kicker: '学习概览',
    title: 'Your Java interview cockpit',
    subtitle: '把最近的学习行为、面试结果、错题和计划压缩成一块可操作的主看板。'
  },
  chat: {
    kicker: '智能问答',
    title: 'Ask with context, not just chat',
    subtitle: '普通问答和知识问答放在同一入口，方便后续把问题直接带回复习链路。'
  },
  knowledge: {
    kicker: '知识库',
    title: 'A built-in library for interview recall',
    subtitle: '内置资料库支持检索和引用，也可以上传自己的学习材料统一管理。'
  },
  interview: {
    kicker: '模拟面试',
    title: 'Simulate the pressure before the real round',
    subtitle: '每场控制在 3-5 题，通过评分、点评和追问把问题沉淀到后续复习。'
  },
  wrong: {
    kicker: '错题本',
    title: 'Turn weak answers into review assets',
    subtitle: '错题不只是归档，而是驱动下一步复习与计划生成的基础资产。'
  },
  review: {
    kicker: '间隔复习',
    title: 'Review on the right day, not every day',
    subtitle: '基于遗忘曲线自动调度复习时机，让每一次复习都恰好在快要忘记的时候。'
  },
  plan: {
    kicker: '学习计划',
    title: 'Convert weak points into daily execution',
    subtitle: '计划模块把薄弱点和错题拆成可完成的节奏，而不是停留在泛泛建议。'
  },
  admin: {
    kicker: '管理后台',
    title: 'Operate content without splitting the product',
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
          path: 'plan',
          name: 'plan',
          component: () => import('@/pages/plan/PlanPage.vue'),
          meta: pageMeta.plan
        },
        {
          path: 'admin',
          name: 'admin',
          component: () => import('@/pages/admin/AdminPage.vue'),
          meta: pageMeta.admin
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

  if (to.meta.guestOnly && authStore.isLoggedIn) {
    return '/dashboard'
  }

  return true
})

export default router
