import { describe, it, expect, beforeAll, afterEach, afterAll, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { ThemeProvider } from '@/contexts/ThemeContext'
import { LoginPage } from './LoginPage'

const navigateMock = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => navigateMock }
})

const refreshUserMock = vi.fn().mockResolvedValue(undefined)
vi.mock('@/auth/AuthContext', () => ({
  useAuth: () => ({
    isAuthenticated: false,
    user: null,
    isLoading: false,
    logout: vi.fn(),
    refreshUser: refreshUserMock,
  }),
}))

const server = setupServer()
beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }))
afterEach(() => {
  server.resetHandlers()
  navigateMock.mockReset()
  refreshUserMock.mockClear()
  localStorage.clear()
})
afterAll(() => server.close())

function renderLoginPage() {
  return render(
    <ThemeProvider>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('LoginPage', () => {
  it('renders email input, password input and Sign in button', () => {
    renderLoginPage()
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })

  it('renders a link to /register', () => {
    renderLoginPage()
    const link = screen.getByRole('link', { name: /register/i })
    expect(link).toHaveAttribute('href', '/register')
  })

  it('on successful submit stores tokens and navigates to /persons', async () => {
    server.use(
      http.post('http://localhost:8080/api/auth/login', () =>
        HttpResponse.json(
          { accessToken: 'access-1', refreshToken: 'refresh-1', tokenType: 'Bearer' },
          { status: 200 },
        ),
      ),
    )

    renderLoginPage()
    await userEvent.type(screen.getByLabelText(/email/i), 'test@example.com')
    await userEvent.type(screen.getByLabelText(/password/i), 'password')
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(localStorage.getItem('accessToken')).toBe('access-1')
    })
    expect(localStorage.getItem('refreshToken')).toBe('refresh-1')
    expect(refreshUserMock).toHaveBeenCalled()
    expect(navigateMock).toHaveBeenCalledWith('/persons', { replace: true })
  })

  it('on failed submit shows an error alert and does not navigate', async () => {
    server.use(
      http.post('http://localhost:8080/api/auth/login', () =>
        HttpResponse.json({ error: 'Bad credentials' }, { status: 401 }),
      ),
    )

    renderLoginPage()
    await userEvent.type(screen.getByLabelText(/email/i), 'test@example.com')
    await userEvent.type(screen.getByLabelText(/password/i), 'wrongpass')
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/invalid email or password/i)
    expect(navigateMock).not.toHaveBeenCalled()
    expect(localStorage.getItem('accessToken')).toBeNull()
  })
})
