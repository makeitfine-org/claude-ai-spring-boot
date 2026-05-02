import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react'
import { api, clearTokens } from '@/lib/api'
import type { UserProfile } from '@/types/auth'

interface AuthContextValue {
  isAuthenticated: boolean
  iamEnabled: boolean
  user: UserProfile | null
  isLoading: boolean
  logout: () => Promise<void>
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [iamEnabled, setIamEnabled] = useState(true)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    api
      .get<{ iamEnabled: boolean }>('/api/config')
      .then((res) => {
        if (cancelled) return
        const enabled = res.data.iamEnabled
        setIamEnabled(enabled)
        if (!enabled) {
          setIsAuthenticated(true)
          setIsLoading(false)
          return
        }
        return api.get<UserProfile>('/api/users/me').then((userRes) => {
          if (!cancelled) {
            setUser(userRes.data)
            setIsAuthenticated(true)
          }
        })
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
    if (!iamEnabled) return
    try {
      const res = await api.get<UserProfile>('/api/users/me')
      setUser(res.data)
      setIsAuthenticated(true)
    } catch {
      setUser(null)
      setIsAuthenticated(false)
    }
  }, [iamEnabled])

  const logout = useCallback(async () => {
    if (iamEnabled) {
      try {
        await api.post('/api/logout')
      } catch {
        // ignore errors on logout
      }
      clearTokens()
    }
    setUser(null)
    setIsAuthenticated(false)
    window.location.href = '/login'
  }, [iamEnabled])

  return (
    <AuthContext.Provider value={{ isAuthenticated, iamEnabled, user, isLoading, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
