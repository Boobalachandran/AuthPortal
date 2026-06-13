import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import AuthLayout from '../components/AuthLayout'
import { resetPassword } from '../api/auth'

interface FormErrors {
  password?: string
  confirmPassword?: string
}

const PASSWORD_REGEX = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,12}$/

export default function ResetPasswordPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as { email?: string; token?: string } | null
  const email = state?.email ?? ''
  const token = state?.token ?? ''

  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [errors, setErrors] = useState<FormErrors>({})
  const [loading, setLoading] = useState(false)

  const validate = (): boolean => {
    const errs: FormErrors = {}
    if (!password) errs.password = 'Password is required'
    else if (!PASSWORD_REGEX.test(password))
      errs.password = 'Must be 8-12 chars with uppercase, lowercase, and a digit'
    if (!confirmPassword) errs.confirmPassword = 'Please confirm your password'
    else if (password !== confirmPassword) errs.confirmPassword = 'Passwords do not match'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!validate()) return
    setLoading(true)
    try {
      await resetPassword({ email, token, password, confirmPassword })
      toast.success('Password reset successfully! Please sign in 🔓')
      navigate('/login')
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to reset password. Please try again.'
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  const strength = (() => {
    if (!password) return 0
    let s = 0
    if (password.length >= 8) s++
    if (/[A-Z]/.test(password)) s++
    if (/[0-9]/.test(password)) s++
    if (/[a-z]/.test(password)) s++
    return s
  })()
  const strengthLabel = ['', 'Weak', 'Fair', 'Good', 'Strong'][strength]
  const strengthColor = ['', 'bg-red-500', 'bg-yellow-500', 'bg-blue-400', 'bg-green-400'][strength]

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
          <Link
            to="/verify-otp"
            className="inline-flex items-center gap-1 text-white/50 hover:text-white/80 text-sm mb-6 transition-colors"
          >
            ← Back
          </Link>

          <div className="flex items-center justify-center w-14 h-14 rounded-2xl bg-brand-500/20 border border-brand-500/30 text-3xl mb-4">
            🔐
          </div>
          <h2 className="text-2xl font-bold text-white mb-1">Set new password</h2>
          <p className="text-white/50 text-sm mb-6">
            Choose a strong password for{' '}
            <span className="text-brand-300 font-medium">{email}</span>
          </p>

          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <div className="space-y-1">
              <label className="block text-sm font-medium text-white/70">New Password</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-white/40 text-lg pointer-events-none">
                  🔒
                </span>
                <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder="8-12 chars, upper + lower + digit"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="input-field pl-10 pr-12"
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40 hover:text-white/70 transition-colors text-lg"
                >
                  {showPassword ? '🙈' : '👁️'}
                </button>
              </div>
              {password && (
                <div className="flex items-center gap-2 mt-2">
                  <div className="flex gap-1 flex-1">
                    {[1, 2, 3, 4].map((i) => (
                      <div
                        key={i}
                        className={`h-1 flex-1 rounded-full transition-all duration-300 ${
                          i <= strength ? strengthColor : 'bg-white/10'
                        }`}
                      />
                    ))}
                  </div>
                  <span className="text-xs text-white/50">{strengthLabel}</span>
                </div>
              )}
              {errors.password && <p className="error-text">{errors.password}</p>}
            </div>

            <div className="space-y-1">
              <label className="block text-sm font-medium text-white/70">Confirm New Password</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-white/40 text-lg pointer-events-none">
                  🔐
                </span>
                <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Re-enter your new password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="input-field pl-10"
                  autoComplete="new-password"
                />
              </div>
              {errors.confirmPassword && <p className="error-text">{errors.confirmPassword}</p>}
            </div>

            <div className="bg-white/5 rounded-xl p-3 text-xs text-white/40 space-y-1 border border-white/10">
              <p className="font-medium text-white/60 mb-1">Password requirements:</p>
              <p className={password.length >= 8 && password.length <= 12 ? 'text-green-400' : ''}>
                ✓ 8–12 characters long
              </p>
              <p className={/[A-Z]/.test(password) ? 'text-green-400' : ''}>✓ At least one uppercase letter</p>
              <p className={/[a-z]/.test(password) ? 'text-green-400' : ''}>✓ At least one lowercase letter</p>
              <p className={/[0-9]/.test(password) ? 'text-green-400' : ''}>✓ At least one digit</p>
            </div>

            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  Resetting…
                </span>
              ) : (
                'Reset Password'
              )}
            </button>
          </form>
        </div>
      </div>
    </AuthLayout>
  )
}

