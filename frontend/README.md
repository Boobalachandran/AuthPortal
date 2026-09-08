# Web Client Overview

React + TypeScript + Vite client for an authentication platform.

## Tech Stack

| Tool | Purpose |
|------|---------|
| React | UI framework |
| TypeScript | Type safety |
| Vite | Development server and bundling |
| Tailwind CSS | Utility-first styling |
| React Router | Client-side routing |
| Axios | HTTP communication |
| Toast library | User feedback notifications |

## Routes

| Route | Description |
|-------|-------------|
| `/login` | Email/password login and OAuth option |
| `/signup` | New account registration |
| `/forgot-password` | OTP request flow |
| `/verify-otp` | OTP verification |
| `/reset-password` | Password reset completion |
| `/dashboard` | Authenticated landing page |

## Local Run

```bash
cd <web-client-root>
npm install
npm run dev
```

## Production Build

```bash
npm run build
```

## API Mapping

| Client Action | API Endpoint |
|---------------|--------------|
| Login | `POST /auth/login` |
| Signup | `POST /auth/signup` |
| Forgot Password | `POST /auth/forgotPassword` |
| Verify OTP | `POST /auth/verifyOtp` |
| Reset Password | `POST /auth/resetPassword` |
| OAuth Login | `GET /oauth2/authorization/google` |

