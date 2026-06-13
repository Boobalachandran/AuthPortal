# SportsApp Frontend

React + TypeScript + Vite frontend for the SportsApp Spring Boot backend.

## Tech Stack

| Tool | Purpose |
|------|---------|
| **React 18** | UI framework |
| **TypeScript** | Type safety |
| **Vite** | Dev server & bundler |
| **Tailwind CSS** | Utility-first styling |
| **React Router v6** | Client-side routing |
| **Axios** | HTTP requests |
| **react-hot-toast** | Toast notifications |

## Pages

| Route | Description |
|-------|-------------|
| `/login` | Email + password login, Google OAuth button |
| `/signup` | Register with password strength meter |
| `/forgot-password` | Request OTP via email |
| `/verify-otp` | Enter 6-digit OTP (paste-friendly) |
| `/reset-password` | Set new password with requirements checklist |
| `/dashboard` | Post-login landing page |

## Getting Started

### Prerequisites
- [Node.js](https://nodejs.org/) v18+

### Install & Run

```bash
cd frontend
npm install
npm run dev
```

The app runs at **http://localhost:3000** and automatically proxies `/auth/**` and `/oauth2/**`
to your Spring Boot backend at `http://localhost:8080`.

### Build for Production

```bash
npm run build
```

Outputs to `frontend/dist/`.

## Backend API Mapping

| Frontend action | Backend endpoint |
|-----------------|-----------------|
| Login | `POST /auth/login` |
| Signup | `POST /auth/signup` |
| Forgot Password | `POST /auth/forgotPassword` |
| Verify OTP | `POST /auth/verifyOtp` |
| Reset Password | `POST /auth/resetPassword` |
| Google OAuth | `GET /oauth2/authorization/google` |

