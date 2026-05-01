import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react'
import { api } from '@/lib/api'
import type { UserProfile } from '@/types/auth'

interface AuthContextValue {
  isAuthenticated: boolean
  user: UserProfile | null
  isLoading: boolean
  logout: () => Promise<void>
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    api
      .get<UserProfile>('/api/users/me')
      .then((res) => {
        if (!cancelled) {
          setUser(res.data)
          setIsAuthenticated(true)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setUser(null)
          setIsAuthenticated(false)
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const refreshUser = useCallback(async () => {
    try {
      const res = await api.get<UserProfile>('/api/users/me')
      setUser(res.data)
      setIsAuthenticated(true)
    } catch {
      setUser(null)
      setIsAuthenticated(false)
    }
  }, [])

  const logout = useCallback(async () => {
    try {
      await api.post('/api/logout')
    } catch {
      // ignore errors on logout
    }
    setUser(null)
    setIsAuthenticated(false)
    window.location.href = '/'
  }, [])

  return (
    <AuthContext.Provider value={{ isAuthenticated, user, isLoading, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
