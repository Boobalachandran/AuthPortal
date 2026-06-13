import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import AuthLayout from '../components/AuthLayout'
import FormInput from '../components/FormInput'
import { forgotPassword } from '../api/auth'

export default function ForgotPasswordPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const validate = () => {
    if (!email) { setError('Email is required'); return false }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { setError('Invalid email format'); return false }
    setError('')
    return true
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!validate()) return
    setLoading(true)
    try {
      await forgotPassword({ email })
      toast.success('OTP sent to your email 📧')
      navigate('/verify-otp', { state: { email } })
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to send OTP. Please try again.'
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout>
      <div className="flex flex-col items-center">
        <div className="mb-8 text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-brand-400 to-brand-600 shadow-lg shadow-brand-500/30 mb-4 text-3xl">
            ⚽
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">AuthPortal</h1>
        </div>

        <div className="auth-card">
          {/* Back button */}
          <Link
            to="/login"
            className="inline-flex items-center gap-1 text-white/50 hover:text-white/80 text-sm mb-6 transition-colors"
          >
            ← Back to sign in
          </Link>

          <div className="flex items-center justify-center w-14 h-14 rounded-2xl bg-brand-500/20 border border-brand-500/30 text-3xl mb-4">
            🔑
          </div>
          <h2 className="text-2xl font-bold text-white mb-1">Forgot password?</h2>
          <p className="text-white/50 text-sm mb-6">
            No worries! Enter your email and we'll send you a one-time passcode.
          </p>

          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <FormInput
              label="Email address"
              type="email"
              placeholder="you@example.com"
              icon="✉️"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={error}
              autoComplete="email"
            />

            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  Sending OTP…
                </span>
              ) : (
                'Send OTP'
              )}
            </button>
          </form>
        </div>
      </div>
    </AuthLayout>
  )
}

