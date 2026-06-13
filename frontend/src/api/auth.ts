import axios from 'axios'

const api = axios.create({
  baseURL: '/auth',
  headers: { 'Content-Type': 'application/json' },
})

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
export const isLoggedIn = () => !!getToken()

