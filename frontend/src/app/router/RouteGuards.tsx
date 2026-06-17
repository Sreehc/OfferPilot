import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { PageSpin } from '@/components/feedback/StateView'
import { useAuthStore } from '@/features/auth/authStore'

export function RequireAuth({ adminOnly = false }: { adminOnly?: boolean }) {
  const location = useLocation()
  const { token, user, restoring } = useAuthStore()
  if (restoring) return <PageSpin />
  if (!token) return <Navigate to="/login" replace state={{ redirect: location.pathname + location.search }} />
  if (adminOnly && user?.role !== 'ADMIN') return <Navigate to="/dashboard" replace />
  return <Outlet />
}

export function GuestOnly() {
  const { token } = useAuthStore()
  if (token) return <Navigate to="/dashboard" replace />
  return <Outlet />
}
