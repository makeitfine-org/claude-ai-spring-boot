import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ThemeProvider } from '@/contexts/ThemeContext'
import { LoginPromptPage } from './LoginPromptPage'

function renderLoginPromptPage() {
  return render(
    <ThemeProvider>
      <MemoryRouter>
        <LoginPromptPage />
      </MemoryRouter>
    </ThemeProvider>
  )
}

describe('LoginPromptPage', () => {
  it('renders a Sign in button', () => {
    renderLoginPromptPage()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })

  it('renders a link to /register', () => {
    renderLoginPromptPage()
    const registerLink = screen.getByRole('link', { name: /register/i })
    expect(registerLink).toBeInTheDocument()
    expect(registerLink).toHaveAttribute('href', '/register')
  })
})
