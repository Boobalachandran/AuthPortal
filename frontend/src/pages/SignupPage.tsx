import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import AuthLayout from '../components/AuthLayout'
import FormInput from '../components/FormInput'
import { signup } from '../api/auth'

interface FormErrors {
  email?: string
  password?: string
  confirmPassword?: string
}

const PASSWORD_REGEX = /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z]).{8,12}$/

export default function SignupPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [errors, setErrors] = useState<FormErrors>({})
  const [loading, setLoading] = useState(false)

  const validate = (): boolean => {
    const errs: FormErrors = {}
    if (!email) errs.email = 'Email is required'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errs.email = 'Invalid email format'

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
      await signup({ email, password, confirmPassword })
      toast.success('Account created! Please sign in 🎉')
      navigate('/login')
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Signup failed. Please try again.'
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  // Password strength indicator
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
          <h1 className="text-3xl font-extrabold text-white tracking-tight">SportsApp</h1>
          <p className="text-white/50 mt-1 text-sm">Your ultimate sports companion</p>
        </div>

        <div className="auth-card">
          <h2 className="text-2xl font-bold text-white mb-1">Create account</h2>
          <p className="text-white/50 text-sm mb-6">Join the SportsApp community today</p>

          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <FormInput
              label="Email address"
              type="email"
              placeholder="you@example.com"
              icon="✉️"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={errors.email}
              autoComplete="email"
            />

            <div className="space-y-1">
              <label className="block text-sm font-medium text-white/70">Password</label>
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
              <label className="block text-sm font-medium text-white/70">Confirm Password</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-white/40 text-lg pointer-events-none">
                  🔐
                </span>
                <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Re-enter your password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="input-field pl-10"
                  autoComplete="new-password"
                />
              </div>
              {errors.confirmPassword && <p className="error-text">{errors.confirmPassword}</p>}
            </div>

            <button type="submit" className="btn-primary mt-2" disabled={loading}>
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  Creating account…
                </span>
              ) : (
                'Create Account'
              )}
            </button>
          </form>

          <div className="relative my-5">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-white/10" />
            </div>
            <div className="relative flex justify-center text-xs">
              <span className="bg-transparent px-3 text-white/30 backdrop-blur-sm">or sign up with</span>
            </div>
          </div>

          <a href="/oauth2/authorization/google" className="btn-google">
            <svg viewBox="0 0 24 24" className="w-5 h-5">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
            Continue with Google
          </a>

          <p className="text-center text-white/50 text-sm mt-6">
            Already have an account?{' '}
            <Link to="/login" className="text-brand-400 hover:text-brand-300 font-semibold transition-colors">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </AuthLayout>
  )
}

