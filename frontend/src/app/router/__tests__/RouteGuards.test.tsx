import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@/features/auth/authStore'
import { RequireAuth } from '../RouteGuards'

function renderGuard(adminOnly = false) {
  return render(
    <MemoryRouter initialEntries={['/admin']}>
      <Routes>
        <Route element={<RequireAuth adminOnly={adminOnly} />}>
          <Route path="/admin" element={<div>admin content</div>} />
        </Route>
        <Route path="/login" element={<div>login page</div>} />
        <Route path="/dashboard" element={<div>dashboard page</div>} />
      </Routes>
    </MemoryRouter>
  )
}

describe('RouteGuards', () => {
  beforeEach(() => {
    localStorage.clear()
    useAuthStore.getState().clear()
  })

  it('redirects anonymous users to login', () => {
    renderGuard()
    expect(screen.getByText('login page')).toBeInTheDocument()
  })

  it('redirects non-admin users away from admin routes', () => {
    useAuthStore.getState().persistFromResponse({ token: 'token', userInfo: { id: 1, username: 'user', role: 'USER' } })
    renderGuard(true)
    expect(screen.getByText('dashboard page')).toBeInTheDocument()
  })

  it('allows admins into admin routes', () => {
    useAuthStore.getState().persistFromResponse({ token: 'token', userInfo: { id: 1, username: 'admin', role: 'ADMIN' } })
    renderGuard(true)
    expect(screen.getByText('admin content')).toBeInTheDocument()
  })
})
