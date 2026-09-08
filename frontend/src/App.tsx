import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import type { ReactElement } from 'react'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import VerifyOtpPage from './pages/VerifyOtpPage'
import ResetPasswordPage from './pages/ResetPasswordPage'
import DashboardPage from './pages/DashboardPage'
import { isLoggedIn, saveSession } from './api/auth'

/**
 * Process OAuth2 callback parameters synchronously before route rendering.
 * This runs immediately when the module loads, handling the OAuth redirect before
 * any route protection checks occur.
 */
function processOAuthCallback() {
  // Only process on initial page load (not on React Router navigations)
  const params = new URLSearchParams(window.location.search)
  const token = params.get('token')
  const email = params.get('email')

  if (token && email && window.location.pathname === '/dashboard') {
    // Save to localStorage so protected route check will pass
    saveSession(token, email)
    // Clean up URL by removing query params
    window.history.replaceState({}, '', '/dashboard')
  }
}

// Run OAuth callback processing synchronously on module load
processOAuthCallback()

function ProtectedRoute({ children }: { children: ReactElement }) {
  const location = useLocation()
  if (!isLoggedIn()) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return children
}

function App() {

  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/verify-otp" element={<VerifyOtpPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
    </Routes>
  )
}

export default App

