# AuthPortal

AuthPortal is an authentication and account-management application built with Spring Boot services, a React/TypeScript web client, PostgreSQL, Redis, and Kafka.

> **Frontend authorship:** The backend REST APIs were implemented first. After those APIs were in place, the frontend was created entirely by AI, including its UI and integration with the backend endpoints.

## Features

- Email and password signup and login
- Google OAuth2 login
- Password recovery using email OTP
- JWT-based authentication
- Email notifications dispatched asynchronously through Kafka

## Architecture

```text
Browser
  └── React + TypeScript frontend (Vite locally, Nginx in Docker)
        └── Spring Cloud Gateway
              └── Spring Boot auth service
                    ├── PostgreSQL (account data)
                    ├── Redis (OTP data)
                    └── Kafka ──> notification service ──> email provider
```

| Component | Role | Local port |
|---|---|---:|
| Frontend | React UI, served by Vite or Nginx | 3000 |
| API Gateway | Routes authentication and OAuth requests | 8081 |
| Auth service | REST API, authentication, and OAuth handling | 8080 |
| Notification service | Processes notification events | 8082 |
| PostgreSQL | Persistent account data | 5433 (host) |
| Redis | OTP storage | 6379 |
| Kafka | Asynchronous event transport | 9092, 9094 |
| Prometheus | Metrics | 9090 |
| Grafana | Dashboards | 3001 |

## Local development

### Requirements

- Docker and Docker Compose
- Node.js and npm for frontend-only development
- Java and Maven for backend-only development

### Run the complete application

1. Clone the repository and open its root directory.
2. Copy `.env.example` to `.env` and provide the local configuration required by the services. Keep local configuration files out of source control.
3. Start the application:

   ```sh
   docker compose up --build
   ```

4. Open the frontend at `http://localhost:3000`. The gateway is available at `http://localhost:8081`; the auth service is also exposed directly at `http://localhost:8080`.

The application depends on the configured database, cache, event broker, OAuth provider, and mail provider. Configure external integrations in your local environment before exercising those flows.

### Run the frontend by itself

```sh
cd frontend
npm install
npm run dev
```

The Vite development server listens on port 3000 and proxies API and OAuth paths to the gateway on port 8081.

Create a production frontend build with:

```sh
npm run build
```

## Backend API

The auth service exposes these REST endpoints under `/auth`:

| Operation | Method and path |
|---|---|
| Login | `POST /auth/login` |
| Signup | `POST /auth/signup` |
| Request password-reset OTP | `POST /auth/forgotPassword` |
| Verify OTP | `POST /auth/verifyOtp` |
| Reset password | `POST /auth/resetPassword` |

Google OAuth starts at `GET /oauth2/authorization/google`. In Docker and local frontend development, the browser-facing routes are routed through the gateway.

## Deployment

The repository includes Terraform configuration for a GKE Autopilot cluster and Artifact Registry, plus Kubernetes manifests for the application services and supporting components. See [DEPLOYMENT.md](DEPLOYMENT.md) for deployment scope and instructions.

The current Kubernetes frontend manifest provisions an HTTP LoadBalancer. The Terraform configuration does not provision a TLS-enabled ingress or HTTPS load balancer; configure a suitable HTTPS edge and public OAuth callback separately before production use.

## Current limitations

- OAuth returns a JWT in the redirect URL, and the frontend stores the session in browser `localStorage`. This is a known security limitation; use a secure, HTTP-only cookie-based session design before deploying in a security-sensitive production environment.
- The Kubernetes manifests run single replicas for application and data services; they are not a high-availability production configuration.
- Configure and validate HTTPS, backups, monitoring, and recovery procedures for any deployment beyond local development.

