import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { AuthProvider, useAuth } from './AuthContext'
import type { UserProfile } from '@/types/auth'

const server = setupServer()

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

const mockUser: UserProfile = {
  sub: 'user-123',
  username: 'testuser',
  displayName: 'Test User',
  email: 'test@example.com',
  hasAvatar: false,
}

function TestConsumer() {
  const { isAuthenticated, user, isLoading } = useAuth()
  if (isLoading) return <div>loading</div>
  return (
    <div>
      <div data-testid="authenticated">{String(isAuthenticated)}</div>
      <div data-testid="username">{user?.username ?? 'none'}</div>
    </div>
  )
}

function renderWithProvider() {
  return render(
    <AuthProvider>
      <TestConsumer />
    </AuthProvider>
  )
}

describe('AuthContext', () => {
  it('fetches /api/users/me on mount and sets user when 200', async () => {
    server.use(
      http.get('http://localhost:8080/api/users/me', () => {
        return HttpResponse.json(mockUser, { status: 200 })
      })
    )
    renderWithProvider()
    await waitFor(() => {
      expect(screen.getByTestId('username')).toHaveTextContent('testuser')
    })
    expect(screen.getByTestId('authenticated')).toHaveTextContent('true')
  })

  it('sets isAuthenticated=false when /api/users/me returns 401', async () => {
    server.use(
      http.get('http://localhost:8080/api/users/me', () => {
        return HttpResponse.json({ error: 'Unauthorized' }, { status: 401 })
      })
    )
    renderWithProvider()
    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('false')
    })
    expect(screen.getByTestId('username')).toHaveTextContent('none')
  })

  it('shows loading initially, then resolves', async () => {
    let resolveRequest!: () => void
    const requestHeld = new Promise<void>((res) => {
      resolveRequest = res
    })
    server.use(
      http.get('http://localhost:8080/api/users/me', async () => {
        await requestHeld
        return HttpResponse.json(mockUser, { status: 200 })
      })
    )
    renderWithProvider()
    // While the request is pending, loading indicator should be visible
    expect(screen.getByText('loading')).toBeInTheDocument()
    resolveRequest()
    await waitFor(() => {
      expect(screen.queryByText('loading')).not.toBeInTheDocument()
    })
    expect(screen.getByTestId('authenticated')).toHaveTextContent('true')
  })
})
