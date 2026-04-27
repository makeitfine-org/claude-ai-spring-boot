import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from './features/auth/LoginPage'
import { PersonsPage } from './features/persons/PersonsPage'
import { ProtectedRoute } from './auth/ProtectedRoute'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/persons"
        element={
          <ProtectedRoute>
            <PersonsPage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/persons" replace />} />
    </Routes>
  )
}

export default App
