import { lazy, Suspense } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import { PageSpin } from '@/components/feedback/StateView'
import { AppShell } from '@/app/shell/AppShell'
import { GuestOnly, RequireAuth } from './RouteGuards'

const LoginPage = lazy(() => import('@/pages/auth/LoginPage').then((m) => ({ default: m.LoginPage })))
const RegisterPage = lazy(() => import('@/pages/auth/RegisterPage').then((m) => ({ default: m.RegisterPage })))
const ForgotPasswordPage = lazy(() => import('@/pages/auth/ForgotPasswordPage').then((m) => ({ default: m.ForgotPasswordPage })))
const TwoFactorVerifyPage = lazy(() => import('@/pages/auth/TwoFactorVerifyPage').then((m) => ({ default: m.TwoFactorVerifyPage })))
const DashboardPage = lazy(() => import('@/pages/dashboard/DashboardPage').then((m) => ({ default: m.DashboardPage })))
const ChatPage = lazy(() => import('@/pages/chat/ChatPage').then((m) => ({ default: m.ChatPage })))
const AgentWorkbenchPage = lazy(() => import('@/pages/agent/AgentWorkbenchPage').then((m) => ({ default: m.AgentWorkbenchPage })))
const QuestionBankPage = lazy(() => import('@/pages/question/QuestionBankPage').then((m) => ({ default: m.QuestionBankPage })))
const KnowledgePage = lazy(() => import('@/pages/knowledge/KnowledgePage').then((m) => ({ default: m.KnowledgePage })))
const InterviewPage = lazy(() => import('@/pages/interview/InterviewPage').then((m) => ({ default: m.InterviewPage })))
const InterviewDetailPage = lazy(() => import('@/pages/interview/InterviewDetailPage').then((m) => ({ default: m.InterviewDetailPage })))
const StudyPlanPage = lazy(() => import('@/pages/study-plan/StudyPlanPage').then((m) => ({ default: m.StudyPlanPage })))
const ResumeAssistantPage = lazy(() => import('@/pages/resume/ResumeAssistantPage').then((m) => ({ default: m.ResumeAssistantPage })))
const ApplicationBoardPage = lazy(() => import('@/pages/applications/ApplicationBoardPage').then((m) => ({ default: m.ApplicationBoardPage })))
const ApplicationDetailPage = lazy(() => import('@/pages/applications/ApplicationDetailPage').then((m) => ({ default: m.ApplicationDetailPage })))
const AnalyticsPage = lazy(() => import('@/pages/analytics/AnalyticsPage').then((m) => ({ default: m.AnalyticsPage })))
const FavoritesPage = lazy(() => import('@/pages/favorites/FavoritesPage').then((m) => ({ default: m.FavoritesPage })))
const WrongBookPage = lazy(() => import('@/pages/wrong/WrongBookPage').then((m) => ({ default: m.WrongBookPage })))
const ReviewPage = lazy(() => import('@/pages/review/ReviewPage').then((m) => ({ default: m.ReviewPage })))
const CommunityPage = lazy(() => import('@/pages/community/CommunityPage').then((m) => ({ default: m.CommunityPage })))
const CommunitySubmitPage = lazy(() => import('@/pages/community/CommunitySubmitPage').then((m) => ({ default: m.CommunitySubmitPage })))
const CommunityQuestionDetailPage = lazy(() => import('@/pages/community/CommunityQuestionDetailPage').then((m) => ({ default: m.CommunityQuestionDetailPage })))
const LeaderboardPage = lazy(() => import('@/pages/community/LeaderboardPage').then((m) => ({ default: m.LeaderboardPage })))
const SettingsPage = lazy(() => import('@/pages/settings/SettingsPage').then((m) => ({ default: m.SettingsPage })))
const AdminPage = lazy(() => import('@/pages/admin/AdminPage').then((m) => ({ default: m.AdminPage })))
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage').then((m) => ({ default: m.NotFoundPage })))

function withSuspense(element: React.ReactNode) {
  return <Suspense fallback={<PageSpin />}>{element}</Suspense>
}

export const router = createBrowserRouter([
  { element: <GuestOnly />, children: [
    { path: '/login', element: withSuspense(<LoginPage />) },
    { path: '/register', element: withSuspense(<RegisterPage />) },
    { path: '/forgot-password', element: withSuspense(<ForgotPasswordPage />) },
    { path: '/verify-2fa', element: withSuspense(<TwoFactorVerifyPage />) }
  ]},
  { element: <RequireAuth />, children: [{ element: <AppShell />, children: [
    { path: '/', element: <Navigate to="/dashboard" replace /> },
    { path: '/dashboard', element: withSuspense(<DashboardPage />) },
    { path: '/chat', element: withSuspense(<ChatPage />) },
    { path: '/agent', element: withSuspense(<AgentWorkbenchPage />) },
    { path: '/question', element: withSuspense(<QuestionBankPage />) },
    { path: '/knowledge', element: withSuspense(<KnowledgePage />) },
    { path: '/knowledge/java-basics', element: <Navigate to="/knowledge" replace /> },
    { path: '/interview', element: withSuspense(<InterviewPage />) },
    { path: '/interview/history', element: <Navigate to="/interview" replace /> },
    { path: '/interview/detail/:id', element: withSuspense(<InterviewDetailPage />) },
    { path: '/study-plan', element: withSuspense(<StudyPlanPage />) },
    { path: '/resume', element: withSuspense(<ResumeAssistantPage />) },
    { path: '/applications', element: withSuspense(<ApplicationBoardPage />) },
    { path: '/applications/:id', element: withSuspense(<ApplicationDetailPage />) },
    { path: '/analytics', element: withSuspense(<AnalyticsPage />) },
    { path: '/favorites', element: withSuspense(<FavoritesPage />) },
    { path: '/wrong', element: withSuspense(<WrongBookPage />) },
    { path: '/review', element: withSuspense(<ReviewPage />) },
    { path: '/community', element: withSuspense(<CommunityPage />) },
    { path: '/community/submit', element: withSuspense(<CommunitySubmitPage />) },
    { path: '/community/question/:id', element: withSuspense(<CommunityQuestionDetailPage />) },
    { path: '/community/leaderboard', element: withSuspense(<LeaderboardPage />) },
    { path: '/settings', element: withSuspense(<SettingsPage />) }
  ]}]},
  { element: <RequireAuth adminOnly />, children: [{ element: <AppShell />, children: [{ path: '/admin', element: withSuspense(<AdminPage />) }] }]},
  { path: '*', element: withSuspense(<NotFoundPage />) }
])
