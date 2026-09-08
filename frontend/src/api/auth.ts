import axios from 'axios'

const api = axios.create({
  baseURL: '/auth',
  headers: { 'Content-Type': 'application/json' },
})

const LOGIN_PATH = '/login'

// ─── Types ────────────────────────────────────────────────────────────────────

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  email: string
}

export interface RegisterRequest {
  email: string
  password: string
  confirmPassword: string
}

export interface SignupResponse {
  message: string
}

export interface ForgotPasswordRequest {
  email: string
}

export interface OtpResponse {
  message: string
}

export interface VerifyOtpRequest {
  email: string
  otp: string
}

export interface ChangePasswordRequest {
  email: string
  token: string
  password: string
  confirmPassword: string
}

export interface ChangePasswordResponse {
  message: string
}

// ─── API calls ────────────────────────────────────────────────────────────────

export const login = (data: LoginRequest) =>
  api.post<LoginResponse>('/login', data).then((r) => r.data)

export const signup = (data: RegisterRequest) =>
  api.post<SignupResponse>('/signup', data).then((r) => r.data)

export const forgotPassword = (data: ForgotPasswordRequest) =>
  api.post<OtpResponse>('/forgotPassword', data).then((r) => r.data)

export const verifyOtp = (data: VerifyOtpRequest) =>
  api.post<string>('/verifyOtp', data).then((r) => r.data)

export const resetPassword = (data: ChangePasswordRequest) =>
  api.post<ChangePasswordResponse>('/resetPassword', data).then((r) => r.data)


// ─── Local session helpers ────────────────────────────────────────────────────

export const saveSession = (token: string, email: string) => {
  localStorage.setItem('token', token)
  localStorage.setItem('email', email)
}

export const clearSession = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('email')
}

export const getToken = () => localStorage.getItem('token')
export const getEmail = () => localStorage.getItem('email')

const decodeJwtPayload = (token: string): { exp?: number } | null => {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
    return JSON.parse(atob(padded)) as { exp?: number }
  } catch {
    return null
  }
}

export const isTokenExpired = (token: string): boolean => {
  const payload = decodeJwtPayload(token)
  if (!payload?.exp) return true
  return Date.now() >= payload.exp * 1000
}

const redirectToLogin = () => {
  const currentPath = window.location.pathname
  const publicPaths = ['/login', '/signup', '/forgot-password', '/verify-otp', '/reset-password']
  if (!publicPaths.includes(currentPath)) {
    window.location.assign(LOGIN_PATH)
  }
}

export const isLoggedIn = () => {
  const token = getToken()
  if (!token) return false
  if (isTokenExpired(token)) {
    clearSession()
    return false
  }
  return true
}

api.interceptors.request.use((config) => {
  const token = getToken()
  if (!token) return config

  if (isTokenExpired(token)) {
    clearSession()
    redirectToLogin()
    return Promise.reject(new Error('Session expired. Please log in again.'))
  }

  config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      clearSession()
      redirectToLogin()
    }
    return Promise.reject(error)
  },
)

