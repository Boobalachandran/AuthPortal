# AuthPortal Frontend

React and TypeScript web client, built with Vite.

> **AI authorship:** The frontend was created entirely by AI after the backend REST APIs had been implemented. This includes the user interface and the client-side integration with those APIs.

## Stack

| Technology | Purpose |
|---|---|
| React | UI |
| TypeScript | Type checking |
| Vite | Development server and production bundling |
| Tailwind CSS | Styling |
| React Router | Client-side routing |
| Axios | HTTP requests |
| React Hot Toast | User feedback |

## Screens and routes

| Route | Purpose |
|---|---|
| `/login` | Email/password login and Google OAuth entry point |
| `/signup` | Account registration |
| `/forgot-password` | Request a password-reset OTP |
| `/verify-otp` | Verify the OTP |
| `/reset-password` | Complete password reset |
| `/dashboard` | Authenticated landing page |

## Run locally

From the repository root:

```sh
cd frontend
npm install
npm run dev
```

Vite serves the frontend on port 3000 and proxies `/auth`, `/oauth2`, and OAuth callback paths to the API gateway at `http://localhost:8081`.

Build the production assets with:

```sh
npm run build
```

The production Docker image serves the built application with Nginx and proxies API and OAuth requests to the `gateway` service.

## Backend integration

The frontend calls the auth service REST API through the gateway:

| Client operation | Method and path |
|---|---|
| Login | `POST /auth/login` |
| Signup | `POST /auth/signup` |
| Request password-reset OTP | `POST /auth/forgotPassword` |
| Verify OTP | `POST /auth/verifyOtp` |
| Reset password | `POST /auth/resetPassword` |
| Start Google OAuth | `GET /oauth2/authorization/google` |

The client uses browser storage for its JWT session. Google OAuth returns the token to the dashboard route in URL query parameters before the client processes the callback. These are known security limitations and should be addressed before a security-sensitive production deployment.
