import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, iamEnabled, isLoading } = useAuth()
  if (isLoading) return null
  if (!iamEnabled) return <>{children}</>
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <>{children}</>
}
