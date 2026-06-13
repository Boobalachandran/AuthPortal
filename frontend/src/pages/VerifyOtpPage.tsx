import { useState, useRef, KeyboardEvent, ClipboardEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import AuthLayout from '../components/AuthLayout'
import { verifyOtp } from '../api/auth'

const OTP_LENGTH = 6

export default function VerifyOtpPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const email: string = (location.state as { email?: string })?.email ?? ''

  const [digits, setDigits] = useState<string[]>(Array(OTP_LENGTH).fill(''))
  const [loading, setLoading] = useState(false)
  const inputRefs = useRef<(HTMLInputElement | null)[]>([])

  const otp = digits.join('')

  const handleChange = (index: number, value: string) => {
    const cleaned = value.replace(/\D/g, '').slice(-1)
    const next = [...digits]
    next[index] = cleaned
    setDigits(next)
    if (cleaned && index < OTP_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus()
    }
  }

  const handleKeyDown = (index: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus()
    }
  }

  const handlePaste = (e: ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault()
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH)
    const next = [...digits]
    pasted.split('').forEach((ch, i) => { next[i] = ch })
    setDigits(next)
    const lastFilled = Math.min(pasted.length, OTP_LENGTH - 1)
    inputRefs.current[lastFilled]?.focus()
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (otp.length < OTP_LENGTH) {
      toast.error('Please enter the complete 6-digit OTP')
      return
    }
    setLoading(true)
    try {
      const jwtToken = await verifyOtp({ email, otp })   // backend returns a JWT string
      toast.success('OTP verified! Set your new password ✅')
      navigate('/reset-password', { state: { email, token: jwtToken } })
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Invalid or expired OTP'
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
          <h1 className="text-3xl font-extrabold text-white tracking-tight">SportsApp</h1>
        </div>

        <div className="auth-card">
          <Link
            to="/forgot-password"
            className="inline-flex items-center gap-1 text-white/50 hover:text-white/80 text-sm mb-6 transition-colors"
          >
            ← Back
          </Link>

          <div className="flex items-center justify-center w-14 h-14 rounded-2xl bg-brand-500/20 border border-brand-500/30 text-3xl mb-4">
            📩
          </div>
          <h2 className="text-2xl font-bold text-white mb-1">Check your email</h2>
          <p className="text-white/50 text-sm mb-2">
            We sent a 6-digit code to
          </p>
          <p className="text-brand-300 font-semibold text-sm mb-6 truncate">{email || 'your email'}</p>

          <form onSubmit={handleSubmit} className="space-y-6" noValidate>
            <div className="flex justify-center gap-3">
              {digits.map((digit, i) => (
                <input
                  key={i}
                  ref={(el) => { inputRefs.current[i] = el }}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={digit}
                  onChange={(e) => handleChange(i, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(i, e)}
                  onPaste={i === 0 ? handlePaste : undefined}
                  className="otp-input"
                  autoFocus={i === 0}
                />
              ))}
            </div>

            <button type="submit" className="btn-primary" disabled={loading || otp.length < OTP_LENGTH}>
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  Verifying…
                </span>
              ) : (
                'Verify OTP'
              )}
            </button>
          </form>

          <p className="text-center text-white/40 text-sm mt-6">
            Didn't receive the code?{' '}
            <button
              onClick={() => navigate('/forgot-password')}
              className="text-brand-400 hover:text-brand-300 font-semibold transition-colors"
            >
              Resend OTP
            </button>
          </p>
        </div>
      </div>
    </AuthLayout>
  )
}

