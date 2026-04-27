import { createContext, useContext, useState, useCallback, type ReactNode } from 'react'
import type { AuthResponse } from '@/types/auth'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
}

interface AuthContextValue extends AuthState {
  login: (response: AuthResponse) => void
  logout: () => void
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    accessToken: localStorage.getItem('accessToken'),
    refreshToken: localStorage.getItem('refreshToken'),
  })

  const login = useCallback((response: AuthResponse) => {
    localStorage.setItem('accessToken', response.accessToken)
    localStorage.setItem('refreshToken', response.refreshToken)
    setState({ accessToken: response.accessToken, refreshToken: response.refreshToken })
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    setState({ accessToken: null, refreshToken: null })
  }, [])

  return (
    <AuthContext.Provider
      value={{ ...state, login, logout, isAuthenticated: !!state.accessToken }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
