# AuthPortal – GKE Deployment Guide

This guide provides step-by-step instructions for deploying AuthPortal to Google Kubernetes Engine (GKE) using Terraform and kubectl.

**Estimated Time:** 30–45 minutes (first deployment)

**Prerequisites:**
- GCP account with billing enabled
- `gcloud` CLI installed and authenticated: `gcloud auth login`
- `kubectl` installed
- `terraform` 1.x installed
- Docker installed (for building images)
- Git repository cloned locally

---

## 📋 Workflow Overview

```
1. Terraform Setup
   └─ Initialize Terraform
   └─ Provision GKE cluster, Artifact Registry

2. Docker Image Building
   └─ Build auth-service, gateway, frontend, notification-service
   └─ Push to Artifact Registry

3. Kubernetes Manifests Preparation
   └─ Create ConfigMaps and Secrets
   └─ Update image references in YAML manifests

4. Frontend LoadBalancer Deployment
   └─ Deploy frontend first to get external IP
   └─ Update auth-service manifest with frontend IP

5. Backend Services Deployment
   └─ Deploy infrastructure (postgres, redis, kafka)
   └─ Deploy auth-service, gateway, notification-service

6. Google OAuth2 Configuration
   └─ Update Google Cloud Console with callback URI

7. Testing
   └─ Verify service endpoints
   └─ Test signup, login, OAuth workflows

8. Monitoring
   └─ Access Prometheus/Grafana
   └─ Configure alerts
```

---

## Step 1: Terraform Setup

### 1.1 Initialize Terraform

```bash
cd terraform

# Initialize Terraform working directory
terraform init

# Verify initialization
ls -la .terraform/  # Should exist
```

### 1.2 Create terraform.tfvars

```bash
# Copy example file
cp terraform.tfvars.example terraform.tfvars

# Edit with your GCP project ID
nano terraform.tfvars
```

**File content (update placeholder):**
```hcl
project_id   = "your-gcp-project-id"  # Replace with your actual project ID
region       = "us-central1"
cluster_name = "authportal-gke"
```

⚠️ **IMPORTANT:** `terraform.tfvars` is Git-ignored and NOT committed to the repository.

### 1.3 Verify Terraform Plan

```bash
terraform plan -out=tfplan

# Review output to confirm resources to be created:
# - google_container_cluster
# - google_artifact_registry_repository
# Any errors indicate configuration issues
```

### 1.4 Apply Terraform

```bash
# Create GKE cluster (may take 5-10 minutes)
terraform apply tfplan

# Verify cluster was created
gcloud container clusters list
gcloud container clusters get-credentials authportal-gke --region us-central1

# Test kubectl access
kubectl cluster-info
kubectl get nodes
```

---

## Step 2: Docker Image Building & Pushing

### 2.1 Authenticate Docker with Artifact Registry

```bash
# Configure Docker authentication
gcloud auth configure-docker us-central1-docker.pkg.dev

# Verify authentication
docker info | grep Registries
```

### 2.2 Build and Push Images

**Set project ID variable:**
```bash
export PROJECT_ID="your-gcp-project-id"
export REGISTRY="us-central1-docker.pkg.dev"
export REPO="$REGISTRY/$PROJECT_ID/authportal-repo"
```

**Build and push each service:**
```bash
# Auth Service
docker build -t $REPO/authportal-auth-service:latest ./auth-service
docker push $REPO/authportal-auth-service:latest

# Gateway
docker build -t $REPO/authportal-gateway:latest ./gateway
docker push $REPO/authportal-gateway:latest

# Frontend
docker build -t $REPO/authportal-frontend:latest ./frontend
docker push $REPO/authportal-frontend:latest

# Notification Service
docker build -t $REPO/authportal-notification-service:latest ./notification-service
docker push $REPO/authportal-notification-service:latest

# Verify images in Artifact Registry
gcloud artifacts docker images list us-central1-docker.pkg.dev/$PROJECT_ID/authportal-repo
```

### 2.3 Update Kubernetes Manifests with Image Paths

Edit `k8s/auth-service.yaml`, `k8s/gateway.yaml`, `k8s/frontend.yaml`, `k8s/notification-service.yaml`:

Replace:
```yaml
image: us-central1-docker.pkg.dev/sports-app-auth-493607/authportal-repo/authportal-auth-service:latest
```

With:
```yaml
image: us-central1-docker.pkg.dev/$PROJECT_ID/authportal-repo/authportal-auth-service:latest
```

Or use sed to automate:
```bash
for file in k8s/*.yaml; do
  sed -i "s|sports-app-auth-493607|$PROJECT_ID|g" "$file"
done
```

---

## Step 3: Kubernetes Secrets & ConfigMaps

### 3.1 Create Database Secret

```bash
kubectl create secret generic postgres-secret \
  --from-literal=POSTGRES_USER=postgres \
  --from-literal=POSTGRES_PASSWORD=<your-secure-password>
```

### 3.2 Create Auth Service Secret

```bash
kubectl create secret generic auth-service-secret \
  --from-literal=JWT_SECRET=<your-jwt-secret> \
  --from-literal=GOOGLE_CLIENT_ID=<your-google-client-id> \
  --from-literal=GOOGLE_CLIENT_SECRET=<your-google-client-secret> \
  --from-literal=MAIL_USERNAME=<your-email@gmail.com> \
  --from-literal=MAIL_FROM_ADDRESS=<your-email@gmail.com> \
  --from-literal=MAIL_PASSWORD=<your-gmail-app-password>
```

**Generate JWT Secret (if needed):**
```bash
# Linux/Mac
openssl rand -hex 32

# Windows PowerShell
-join((1..64) | ForEach-Object {"{0:x}" -f (Get-Random -Maximum 16)})
```

### 3.3 Verify Secrets

```bash
kubectl get secrets
kubectl describe secret postgres-secret
kubectl describe secret auth-service-secret
```

---

## Step 4: Deploy Infrastructure

### 4.1 Create Namespaces (Optional)

```bash
kubectl create namespace authportal
kubectl config set-context --current --namespace=authportal
```

### 4.2 Deploy Database, Cache, Message Queue

```bash
# PostgreSQL
kubectl apply -f k8s/postgres.yaml

# Redis
kubectl apply -f k8s/redis.yaml

# Kafka
kubectl apply -f k8s/kafka.yaml

# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app=postgres --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis --timeout=300s
kubectl wait --for=condition=ready pod -l app=kafka --timeout=300s

# Verify
kubectl get pods
```

---

## Step 5: Frontend Deployment (Critical for CORS/OAuth)

### 5.1 Deploy Frontend Service

```bash
kubectl apply -f k8s/frontend.yaml
```

### 5.2 Get Frontend LoadBalancer IP

**Wait for external IP to be assigned (may take 1-3 minutes):**
```bash
kubectl get svc frontend --watch

# Stop watching when EXTERNAL-IP is assigned
# Output example:
# NAME       TYPE           CLUSTER-IP      EXTERNAL-IP      PORT(S)        AGE
# frontend   LoadBalancer   10.32.100.50    34.123.45.67     80:30123/TCP   2m
```

**Save the external IP:**
```bash
export FRONTEND_IP=$(kubectl get svc frontend -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo "Frontend IP: $FRONTEND_IP"
```

### 5.3 Update auth-service Manifest with Frontend IP

Edit `k8s/auth-service.yaml` and replace:
```yaml
- name: APP_CORS_ALLOWED_ORIGINS
  value: "http://<REPLACE_WITH_FRONTEND_LOADBALANCER_EXTERNAL_IP>"
- name: APP_OAUTH_FRONTEND_REDIRECT_BASE_URL
  value: "http://<REPLACE_WITH_FRONTEND_LOADBALANCER_EXTERNAL_IP>"
```

With:
```yaml
- name: APP_CORS_ALLOWED_ORIGINS
  value: "http://$FRONTEND_IP"
- name: APP_OAUTH_FRONTEND_REDIRECT_BASE_URL
  value: "http://$FRONTEND_IP"
```

Or use sed:
```bash
sed -i "s|<REPLACE_WITH_FRONTEND_LOADBALANCER_EXTERNAL_IP>|$FRONTEND_IP|g" k8s/auth-service.yaml
```

---

## Step 6: Backend Services Deployment

### 6.1 Deploy Auth Service

```bash
kubectl apply -f k8s/auth-service.yaml

# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app=auth-service --timeout=300s

# Verify
kubectl get pods -l app=auth-service
kubectl logs -l app=auth-service --tail=50
```

### 6.2 Deploy API Gateway

```bash
kubectl apply -f k8s/gateway.yaml

# Verify
kubectl get pods -l app=gateway
```

### 6.3 Deploy Notification Service

```bash
kubectl apply -f k8s/notification-service.yaml

# Verify
kubectl get pods -l app=notification-service
```

### 6.4 Verify All Services

```bash
kubectl get pods
kubectl get svc

# Output should show:
# - postgres (ClusterIP)
# - redis (ClusterIP)
# - kafka (ClusterIP)
# - auth-service (ClusterIP)
# - gateway (ClusterIP)
# - frontend (LoadBalancer with EXTERNAL-IP)
```

---

## Step 7: Google OAuth2 Configuration

### 7.1 Determine OAuth Callback URI

For local testing (auth-service behind gateway):
```
http://$FRONTEND_IP:8080/login/oauth2/code/google
```

For production (with domain + HTTPS):
```
https://app.example.com/login/oauth2/code/google
```

**IMPORTANT:** The callback URI must be **accessible from the browser**, not pod-internal IP.

### 7.2 Update Google Cloud Console

1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Select your OAuth 2.0 Client ID
3. Click "Edit"
4. Add or update "Authorized redirect URIs":
   ```
   http://$FRONTEND_IP:8080/login/oauth2/code/google
   ```
5. Click "Save"

### 7.3 Verify in Kubernetes Secret

```bash
# If you need to update the secret
kubectl delete secret auth-service-secret
kubectl create secret generic auth-service-secret \
  --from-literal=GOOGLE_CLIENT_ID=<new-id> \
  --from-literal=GOOGLE_CLIENT_SECRET=<new-secret> \
  # ... other secrets

# Restart auth-service to pick up new secret
kubectl rollout restart deployment/auth-service
```

---

## Step 8: Testing

### 8.1 Get Service Endpoints

```bash
# Frontend
FRONTEND_IP=$(kubectl get svc frontend -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo "Frontend: http://$FRONTEND_IP"

# Gateway (internal only; test via frontend)
GATEWAY_IP=$(kubectl get svc gateway -o jsonpath='{.spec.clusterIP}')
echo "Gateway (internal): http://$GATEWAY_IP:8081"

# Auth Service (internal only)
AUTH_IP=$(kubectl get svc auth-service -o jsonpath='{.spec.clusterIP}')
echo "Auth Service (internal): http://$AUTH_IP:8080"
```

### 8.2 Test Signup

```bash
curl -X POST http://$FRONTEND_IP/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"P@ssw0rd","confirmPassword":"P@ssw0rd"}'

# Expected: {"message":"User registered successfully"}
# Check Gmail for verification email (if notification service is working)
```

### 8.3 Test Login

```bash
curl -X POST http://$FRONTEND_IP/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"P@ssw0rd"}'

# Expected: {"token":"eyJ0eXA...","email":"test@example.com"}
```

### 8.4 Test OAuth2 (Manual)

1. Open browser: `http://$FRONTEND_IP`
2. Click "Continue with Google"
3. Authenticate with Google account
4. Should be redirected to dashboard with JWT in localStorage

### 8.5 Check Service Health

```bash
# Auth Service health
kubectl port-forward svc/auth-service 8080:8080 &
curl http://localhost:8080/actuator/health

# Gateway health (if available)
kubectl port-forward svc/gateway 8081:8081 &
curl http://localhost:8081/actuator/health
```

---

## Step 9: Monitoring (Prometheus + Grafana)

### 9.1 Deploy Monitoring Stack

**Note:** Prometheus and Grafana are currently Docker Compose-only; for K8s, deploy using Helm:

```bash
# Install Prometheus Operator (optional)
# Using Helm:
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack

# Or deploy manually:
kubectl apply -f k8s/auth-service-monitoring.yaml  # If available
```

### 9.2 Access Prometheus & Grafana

```bash
# Port-forward to local machine
kubectl port-forward -n prometheus svc/prometheus-operated 9090:9090 &
kubectl port-forward -n prometheus svc/grafana 3000:3000 &

# Access
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (default: admin/prom-operator)
```

---

## Troubleshooting

### Pods Not Starting

```bash
# Check pod status
kubectl describe pod <pod-name>

# View logs
kubectl logs <pod-name>
kubectl logs <pod-name> --previous  # If restarting

# Common issues:
# - ImagePullBackOff: Image not pushed to Artifact Registry
# - CrashLoopBackOff: Application error (check logs)
# - Pending: Not enough resources (check node capacity)
```

### Service Not Accessible

```bash
# Check service
kubectl describe svc frontend

# Port-forward to test
kubectl port-forward svc/frontend 3000:80

# Access via localhost:3000
```

### CORS/OAuth Errors

```bash
# Verify auth-service environment variables
kubectl exec -it <auth-service-pod> -- env | grep APP_CORS
kubectl exec -it <auth-service-pod> -- env | grep APP_OAUTH

# If incorrect, update manifest and reapply
kubectl apply -f k8s/auth-service.yaml
kubectl rollout restart deployment/auth-service
```

### Database Connection Issues

```bash
# Test PostgreSQL connection
kubectl exec -it <postgres-pod> -- psql -U postgres -d authportal -c "SELECT version();"

# Check auth-service logs for connection errors
kubectl logs -l app=auth-service | grep -i postgres
```

---

## Clean Up

**Delete GKE cluster (⚠️ deletes all data):**
```bash
# Delete Kubernetes resources
kubectl delete -f k8s/

# Destroy Terraform resources
cd terraform
terraform destroy
```

---

## Production Checklist

- [ ] HTTPS configured (GCP Load Balancer + SSL certificate)
- [ ] Frontend domain registered (e.g., app.example.com)
- [ ] Google OAuth callback URI updated to production domain
- [ ] Database backups configured
- [ ] Monitoring alerts configured
- [ ] Logs aggregated (Cloud Logging, Stackdriver)
- [ ] Network policies restricted (Pod-to-pod communication limited)
- [ ] RBAC configured (Service accounts with least privileges)
- [ ] Autoscaling configured (HPA, Cluster Autoscaler)
- [ ] Secrets rotated regularly
- [ ] Disaster recovery plan in place

