# AuthPortal – Authentication & Authorization Microservices

A modern, scalable **authentication and authorization platform** built with Spring Boot, React, and Kubernetes. Supports email/password login, OAuth2 (Google), password reset with OTP, and JWT-based session management.

**Status:** ✅ Tested locally with Docker Compose | ✅ Tested on GKE | ⚠️ HTTP-only (see [Security](#-security-warnings))

---

## ⚠️ Security Warnings

**CRITICAL ISSUES FOR PRODUCTION DEPLOYMENT:**

1. **JWT in URL Query Parameters:**
   - After Google OAuth login, the JWT is passed in the URL: `http://localhost:3000/dashboard?token=<JWT>&email=<email>`
   - **Risks:** Visible in browser history, HTTP referrer headers, server logs, and reverse proxy logs
   - **Mitigation (current):** Frontend extracts JWT immediately and stores in localStorage; URL is cleared via `window.history.replaceState()`
   - **Recommendation:** Refactor to HTTP-only secure cookies (requires frontend and OAuth handler changes)

2. **HTTP-Only Deployment:**
   - This application uses unencrypted HTTP
   - **NOT suitable for production** unless behind an HTTPS reverse proxy or load balancer
   - **Google OAuth requires HTTPS** for the redirect URI in production (HTTP allowed only for `localhost`)
   - See [Production Deployment](#production-deployment) for HTTPS setup via GCP Load Balancer

3. **Hardcoded GCP Project ID (before patch):**
   - Kubernetes and Terraform manifests contained personal GCP project ID
   - Patch removes hardcoded values; follow deployment guide to supply your own project ID

---

## 🏗️ Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Browser / Client                           │
└─────────────────────────┬───────────────────────────────────────────┘
                          │
                          ▼
    ┌─────────────────────────────────────────────────────┐
    │              Frontend (React + TypeScript)           │
    │     ├─ Nginx (Docker) / Vite (local development)   │
    │     └─ Port: 3000                                  │
    └──────┬───────────────────────────────────┬──────────┘
           │                                   │
    Email/Password                        Google OAuth
           │                                   │
           ▼                                   ▼
    ┌────────────────────────────────────────────────────┐
    │       Spring Cloud Gateway (API Router)            │
    │       ├─ Routes: /auth/*, /oauth2/*, /login/*     │
    │       └─ Port: 8081                               │
    └──────┬──────────────────────────────────────────┬──┘
           │                                          │
           └──────────┬──────────────────────┬────────┘
                      │                      │
                      ▼                      ▼
            ┌──────────────────┐  ┌──────────────────┐
            │  Auth Service    │  │   (Google OAuth  │
            │  Spring Boot     │  │    Callback)     │
            │  Port: 8080      │  └──────────────────┘
            └────┬────────────┬┘
                 │            │
           ┌─────▼────┐  ┌────▼──────┐
           │ PostgreSQL│  │   Redis   │
           │ (Users)   │  │  (OTP)    │
           └──────────┘  └───────────┘
                 │
                 │ Publishes user-signed-up event
                 ▼
            ┌──────────────┐
            │    Kafka     │
            │   (Events)   │
            └──────┬───────┘
                   │
                   ▼
            ┌──────────────────┐
            │Notification Svc  │
            │ (Email Dispatch) │
            └──────────────────┘
                   │
                   ▼
              Gmail SMTP
```

### Technology Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Frontend | React + TypeScript | 18.x / 5.x | User interface |
| Frontend Server | Vite (dev) / Nginx (prod) | 5.x / latest | Bundling & serving |
| API Gateway | Spring Cloud Gateway | 2025.1.3 | API routing & load balancing |
| Auth Service | Spring Boot | 4.0.4 | Authentication, JWT, OAuth2 |
| Database | PostgreSQL | 17 | Persistent user data |
| Cache | Redis | 7 | OTP storage with TTL |
| Events | Apache Kafka | 4.0.0 | Async email notifications |
| Email | Gmail SMTP | – | Email delivery |
| Monitoring | Prometheus + Grafana | latest | Metrics & dashboards |
| Container Orchestration | Kubernetes (GKE) | 1.x | Production deployment |
| Infrastructure | Terraform | 1.x | GCP resource automation |

---

## 🚀 Quick Start

### Prerequisites

- **Docker & Docker Compose** ([install](https://docs.docker.com/get-docker/))
- **Gmail Account** (for email notifications)
- **Google OAuth2 Credentials** ([setup guide](#google-oauth2-setup))

### Local Development (Docker Compose)

**1. Clone Repository**
```bash
git clone https://github.com/<your-org>/AuthPortal.git
cd AuthPortal
```

**2. Create `.env` File**
```bash
cp .env.example .env
# Edit .env and replace all <REPLACE_WITH_*> placeholders:
# - POSTGRES_PASSWORD: secure password
# - DB_PASSWORD: same as POSTGRES_PASSWORD
# - JWT_SECRET: openssl rand -hex 32
# - GOOGLE_CLIENT_ID & GOOGLE_CLIENT_SECRET: from Google Console
# - MAIL_USERNAME & MAIL_PASSWORD: Gmail address + app password
nano .env
```

**3. Start Services**
```bash
docker-compose up --build
```

**4. Access Application**
- Frontend: http://localhost:3000
- Auth API: http://localhost:8080
- API Gateway: http://localhost:8081
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001

**5. Test Signup/Login**
- Email signup: http://localhost:3000/signup → enter email/password → check Gmail inbox for verification
- Google OAuth: Click "Continue with Google" → authenticate → should redirect with JWT in URL → stored in localStorage

### Google OAuth2 Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Create OAuth 2.0 Client ID (Web application)
3. Add authorized redirect URIs:
   - **Local:** `http://localhost:8080/login/oauth2/code/google`
   - **GKE:** `https://<your-external-ip>:443/login/oauth2/code/google` (after deployment)
4. Copy Client ID & Client Secret → `.env`

---

## 📦 Deployment

### Docker Compose (Local / Single Machine)

```bash
docker-compose up -d --build
docker-compose ps
docker-compose logs -f auth-service
```

### Kubernetes (GKE)

See [DEPLOYMENT.md](./DEPLOYMENT.md) for step-by-step instructions including:
- Terraform infrastructure provisioning
- Docker image building and pushing to Artifact Registry
- Kubernetes deployment with ConfigMaps and Secrets
- Frontend LoadBalancer configuration
- Google OAuth2 callback URI registration
- Service exposure and testing

**Quick start (if infrastructure already provisioned):**
```bash
# Build and push images
docker build -t us-central1-docker.pkg.dev/<PROJECT_ID>/authportal-repo/authportal-auth-service:latest ./auth-service
docker push us-central1-docker.pkg.dev/<PROJECT_ID>/authportal-repo/authportal-auth-service:latest

# Deploy to GKE
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/kafka.yaml
kubectl apply -f k8s/auth-service.yaml
kubectl apply -f k8s/gateway.yaml
kubectl apply -f k8s/frontend.yaml

# Check status
kubectl get pods
kubectl get svc frontend  # Get LoadBalancer external IP
```

---

## 🔐 CORS & OAuth Configuration

### CORS (Cross-Origin Resource Sharing)

The backend restricts which frontend origins can make HTTP requests to avoid cross-site attacks.

**Configuration:**
- Property: `app.cors.allowed-origins` (in `application.properties` or `APP_CORS_ALLOWED_ORIGINS` env var)
- Format: comma-separated list of allowed origins
- Example: `http://localhost:3000,https://app.example.com`
- Validation: Scheme must be HTTP/HTTPS; host must be non-null; no embedded credentials

**Local Development:**
```properties
app.cors.allowed-origins=http://localhost:3000
```

**GKE Deployment:**
```properties
app.cors.allowed-origins=http://<frontend-loadbalancer-external-ip>
```

### OAuth2 Frontend Redirect

After Google OAuth login, auth-service redirects the browser to the frontend with a JWT in the query string.

**Configuration:**
- Property: `app.oauth.frontend-redirect-base-url` (in `application.properties` or `APP_OAUTH_FRONTEND_REDIRECT_BASE_URL` env var)
- Example: `http://localhost:3000` (local) or `https://app.example.com` (production)
- **MUST match a CORS-allowed origin** for the browser to receive the response

**Frontend Processing:**
1. Browser receives: `http://localhost:3000/dashboard?token=<JWT>&email=<email>`
2. React's `processOAuthCallback()` extracts token and email from URL query params
3. Stores in localStorage: `localStorage.setItem('token', token)`
4. Removes query params from URL: `window.history.replaceState({}, '', '/dashboard')`
5. Protected routes check `isLoggedIn()` and use token for API requests

---

## 🧪 Testing

### Signup & Login

```bash
# Signup
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"P@ssw0rd","confirmPassword":"P@ssw0rd"}'

# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"P@ssw0rd"}'

# Response: {"token":"eyJ0eXA...","email":"test@example.com"}
```

### OAuth2 (Google)

1. Open browser: http://localhost:3000/login
2. Click "Continue with Google"
3. Authenticate with Google account
4. Browser redirected to `http://localhost:3000/dashboard?token=<JWT>&email=<email>`
5. Check browser console: `localStorage.getItem('token')` should contain JWT

### Forgot Password

```bash
# Request OTP
curl -X POST http://localhost:8080/auth/forgotPassword \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'

# Verify OTP (check Gmail for code)
curl -X POST http://localhost:8080/auth/verifyOtp \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","otp":"123456"}'

# Reset Password
curl -X POST http://localhost:8080/auth/resetPassword \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","token":"<token-from-verify>","password":"NewP@ssw0rd","confirmPassword":"NewP@ssw0rd"}'
```

---

## 🐛 Troubleshooting

### Docker Compose Issues

**Services not connecting:**
```bash
docker-compose ps  # Check all services are running
docker-compose logs auth-service  # Check for errors
docker network inspect authportal_default  # Verify service names in network
```

**Ports already in use:**
```bash
# On Windows PowerShell:
Get-NetTCPConnection -LocalPort 3000 | Stop-Process

# On Mac/Linux:
lsof -ti:3000 | xargs kill -9
```

**Clear Docker state (⚠️ deletes local database):**
```bash
docker-compose down -v  # Remove containers and volumes
docker-compose up --build
```

### OAuth Login Issues

**Browser stuck after Google OAuth redirect:**
- Check auth-service logs: `docker-compose logs auth-service | grep OAuth`
- Verify `APP_OAUTH_FRONTEND_REDIRECT_BASE_URL` matches actual frontend URL
- Verify Google OAuth callback URI matches `GOOGLE_REDIRECT_URI` in settings

**JWT not in localStorage:**
- Check browser console for errors
- Verify URL query parameters are present after redirect: `window.location.search`
- Check frontend logs: `docker-compose logs frontend`

### CORS Errors

**Error: "Access to XMLHttpRequest blocked by CORS policy"**
- Verify `APP_CORS_ALLOWED_ORIGINS` includes the frontend origin
- Check if Auth API is accessible directly: `curl -H "Origin: http://localhost:3000" http://localhost:8080/actuator/health`

---

## 📄 Known Limitations

| Feature | Status | Details |
|---------|--------|---------|
| Email/Password Login | ✅ Works | Signup, login, forgot password with OTP verified via Gmail |
| Google OAuth | ✅ Works | JWT returned in URL (query param); see [Security Warnings](#-security-warnings) |
| JWT Session | ✅ Works | 5-minute default expiration; extends on API requests |
| Multi-Factor Auth | ❌ Not Implemented | Password reset uses 6-digit OTP; true MFA (TOTP) not available |
| Role-Based Access | ❌ Not Implemented | No role/permission model; all authenticated users have same access |
| HTTP-Only Sessions | ❌ Not Implemented | JWT passed in URL/localStorage; refactor to HTTP-only cookies needed |
| HTTPS | ⚠️ Requires Reverse Proxy | Application is HTTP-only; deploy behind GCP Load Balancer with SSL for HTTPS |
| Database Replication | ❌ Not Implemented | Single PostgreSQL instance; no replication for high availability |
| Email Queue | ⚠️ Basic Kafka | Notification service consumes Kafka events; no retry policy on email delivery failure |

---

## 📚 Documentation

- **[ARCHITECTURE.md](./ARCHITECTURE.md)** – Detailed system design and component descriptions
- **[DEPLOYMENT.md](./DEPLOYMENT.md)** – Step-by-step GKE deployment with Terraform
- **[TESTING_GUIDE.md](./TESTING_GUIDE.md)** – Manual and automated test procedures

---

## 🔄 Development Workflow

### Local Changes

```bash
# Backend (Java)
cd auth-service
mvn clean package
docker-compose up --build auth-service

# Frontend (React)
cd frontend
npm install
npm run dev  # Vite dev server (http://localhost:5173)
# or rebuild Docker image:
docker-compose up --build frontend
```

### Debugging

```bash
# Attach to running container
docker-compose exec auth-service bash

# View logs with filtering
docker-compose logs auth-service | grep ERROR
docker-compose logs -f --tail=50 auth-service

# Enable SQL logging (in .env)
SHOW_SQL=true
docker-compose restart auth-service
```

---

## 📦 Production Deployment

**Prerequisites:**
- GCP account with GKE cluster
- Terraform 1.x installed
- gcloud CLI configured
- Docker & kubectl installed

**High-Level Steps:**
1. Configure Terraform variables in `terraform.tfvars`
2. Run Terraform to provision GKE cluster, Artifact Registry, Cloud SQL
3. Build and push Docker images to Artifact Registry
4. Update Kubernetes manifests with actual resource IPs
5. Deploy to GKE with `kubectl apply`
6. Configure GCP Load Balancer for HTTPS and frontend domain
7. Update Google OAuth callback URI to production domain
8. Monitor with Prometheus + Grafana

See [DEPLOYMENT.md](./DEPLOYMENT.md) for complete instructions.

---

## 🔒 GitHub Publication (Before Public Release)

**Apply the GitHub-readiness patch before making the repository public:**

This patch externalizes hardcoded values, removes personal GCP project IDs, and adds comprehensive security documentation.

**Files Modified:**
- Kubernetes manifests: Remove hardcoded GCP project ID and IP
- Terraform: Remove personal project ID default
- Java code: Externalize CORS origins and OAuth redirect URL
- Configuration: Add environment variable examples
- Documentation: Add security warnings and deployment guide

**Publication Checklist:**
- [ ] `.env` file is Git-ignored (verify: `git status | grep .env`)
- [ ] `.tfvars` file is Git-ignored (verify: `git status | grep tfvars`)
- [ ] No personal credentials in committed files
- [ ] No hardcoded GCP project IDs
- [ ] No hardcoded IP addresses (except localhost:3000)
- [ ] `.env.example` and `terraform.tfvars.example` provided
- [ ] Security warnings in README.md
- [ ] DEPLOYMENT.md with step-by-step instructions
- [ ] Git history clean (no credentials in commits): `git log --all --source --grep=password`

---

## 📝 License

[Add license here]

---

## 🤝 Contributing

[Add contribution guidelines here]

