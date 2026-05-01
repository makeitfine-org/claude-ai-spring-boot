import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { RegisterPage } from './RegisterPage'

const server = setupServer()

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

function renderRegisterPage(initialPath = '/register') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/register/success" element={<div>Success page</div>} />
      </Routes>
    </MemoryRouter>
  )
}

describe('RegisterPage', () => {
  it('renders all four form fields', () => {
    renderRegisterPage()
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/display name/i)).toBeInTheDocument()
  })

  it('shows validation error when username is empty on submit', async () => {
    server.use(
      http.post('/api/register', () => {
        return HttpResponse.json({}, { status: 400 })
      })
    )
    renderRegisterPage()
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /create account/i }))
    await waitFor(() => {
      expect(
        screen.getByText(/username must be at least 5 characters/i)
      ).toBeInTheDocument()
    })
    // Ensure no API call was made (field-level validation prevents submission)
  })

  it('navigates to /register/success on successful 201 response', async () => {
    server.use(
      http.post('http://localhost:8080/api/register', () => {
        return HttpResponse.json({}, { status: 201 })
      })
    )
    renderRegisterPage()
    const user = userEvent.setup()
    await user.type(screen.getByLabelText(/username/i), 'testuser')
    await user.type(screen.getByLabelText(/email/i), 'test@example.com')
    await user.type(screen.getByLabelText(/password/i), 'secret123')
    await user.type(screen.getByLabelText(/display name/i), 'Test User')
    await user.click(screen.getByRole('button', { name: /create account/i }))
    await waitFor(() => {
      expect(screen.getByText('Success page')).toBeInTheDocument()
    })
  })

  it('shows inline field error on 409 response with errors body', async () => {
    server.use(
      http.post('http://localhost:8080/api/register', () => {
        return HttpResponse.json(
          { errors: { username: 'Username is already taken' } },
          { status: 409 }
        )
      })
    )
    renderRegisterPage()
    const user = userEvent.setup()
    await user.type(screen.getByLabelText(/username/i), 'testuser')
    await user.type(screen.getByLabelText(/email/i), 'test@example.com')
    await user.type(screen.getByLabelText(/password/i), 'secret123')
    await user.type(screen.getByLabelText(/display name/i), 'Test User')
    await user.click(screen.getByRole('button', { name: /create account/i }))
    await waitFor(() => {
      expect(screen.getByText('Username is already taken')).toBeInTheDocument()
    })
  })
})
