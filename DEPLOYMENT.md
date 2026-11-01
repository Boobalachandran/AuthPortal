# AuthPortal Deployment Guide

This guide describes the GKE and Kubernetes deployment assets currently present in this repository. Read the limitations below before using them for anything beyond evaluation or development.

## What the repository provisions

- Terraform creates a GKE Autopilot cluster and an Artifact Registry Docker repository.
- Kubernetes manifests deploy PostgreSQL, Redis, Kafka, the auth service, API gateway, notification service, and frontend.
- The frontend Service is a public HTTP `LoadBalancer`.
- The Terraform configuration does not create Cloud SQL, a TLS certificate, an HTTPS ingress/load balancer, or a production monitoring stack.

The supplied Kubernetes manifests use project-specific image references and deployment placeholders. Review and update those references and environment-specific values for your own environment before applying them. Do not put credential values in this document, source files, or committed configuration.

## Prerequisites

- A Google Cloud project with billing and the required GKE and Artifact Registry permissions
- Google Cloud CLI (`gcloud`), Terraform 1.x, Docker, and `kubectl`
- OAuth and mail-provider configuration for the flows you intend to use

Authenticate with Google Cloud and select the intended project before proceeding:

```sh
gcloud auth login
gcloud config set project <YOUR_PROJECT_ID>
```

## 1. Provision GKE and Artifact Registry

```sh
cd terraform
terraform init
```

Provide `project_id` using the Terraform variable mechanism of your choice. Review the proposed changes, then apply:

```sh
terraform plan -var="project_id=<YOUR_PROJECT_ID>"
terraform apply -var="project_id=<YOUR_PROJECT_ID>"
```

The cluster name and region are configurable in the Terraform variables. Retrieve cluster credentials using the same name and region:

```sh
gcloud container clusters get-credentials <CLUSTER_NAME> --region <REGION>
kubectl get nodes
```

## 2. Build and publish container images

Authenticate Docker with Artifact Registry, then build and push the service images. Replace the sample project and region values with your own:

```sh
gcloud auth configure-docker <REGION>-docker.pkg.dev

docker build -t <REGION>-docker.pkg.dev/<YOUR_PROJECT_ID>/authportal-repo/authportal-auth-service:latest ./auth-service
docker build -t <REGION>-docker.pkg.dev/<YOUR_PROJECT_ID>/authportal-repo/authportal-gateway:latest ./gateway
docker build -t <REGION>-docker.pkg.dev/<YOUR_PROJECT_ID>/authportal-repo/authportal-frontend:latest ./frontend
docker build -t <REGION>-docker.pkg.dev/<YOUR_PROJECT_ID>/authportal-repo/authportal-notification-service:latest ./notification-service

docker push <REGION>-docker.pkg.dev/<YOUR_PROJECT_ID>/authportal-repo/authportal-auth-service:latest
docker push <REGION>-docker.pkg.dev/<YOUR_PROJECT_ID>/authportal-repo/authportal-gateway:latest
docker push <REGION>-docker.pkg.dev/<YOUR_PROJECT_ID>/authportal-repo/authportal-frontend:latest
docker push <REGION>-docker.pkg.dev/<YOUR_PROJECT_ID>/authportal-repo/authportal-notification-service:latest
```

Before deploying, update the image fields in the Kubernetes manifests to match the image paths you pushed. Confirm each manifest points to the intended project, region, repository, and image tag.

## 3. Configure runtime settings

The manifests require database and application integration settings at runtime. Configure them in the target cluster through an approved out-of-band process before deploying the workloads. Do not put sensitive values in this document, source files, or committed configuration.

Set the auth service's CORS allowed origin and OAuth frontend redirect base URL to the browser-visible frontend origin. They must use the same origin. Set the OAuth callback URI to the public callback URL that is routed to the auth service through the frontend/gateway path, and register that exact URI with the OAuth provider.

The current frontend Service serves HTTP only. Google OAuth and production usage should be configured with a publicly reachable HTTPS endpoint; the TLS edge and routing for it must be provisioned separately.

## 4. Deploy the application

Apply the manifests from the repository root. Start with stateful infrastructure, then application services:

```sh
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/kafka.yaml

kubectl apply -f k8s/frontend.yaml
kubectl apply -f k8s/auth-service.yaml
kubectl apply -f k8s/gateway.yaml
kubectl apply -f k8s/notification-service.yaml
```

Wait for workloads to start and inspect their status:

```sh
kubectl get pods
kubectl get services
kubectl get service frontend
```

The frontend Service receives a public IP asynchronously. Check the frontend and API gateway paths from a browser after the service is ready. If its public origin changes, update the auth service's CORS and OAuth redirect configuration to match, then restart or redeploy the auth service.

## 5. Verify deployment

Use the frontend's public URL to verify the UI and test the signup, login, password-recovery, and OAuth flows. Check application logs if a service is not ready:

```sh
kubectl logs -l app=auth-service --tail=100
kubectl logs -l app=gateway --tail=100
kubectl logs -l app=frontend --tail=100
kubectl logs -l app=notification-service --tail=100
```

For health checks on services without a public endpoint, use `kubectl port-forward` and query the relevant local endpoint. The auth service exposes Spring Boot Actuator health and metrics endpoints.

## Notes and limitations

- The Kubernetes manifests are a basic deployment, not a production-hardened setup. They use single replicas and include stateful workloads that need operational backup and recovery plans.
- Use an appropriate managed configuration store and restrict access to sensitive runtime settings.
- The manifests do not create an HTTPS ingress, TLS certificate, network policies, autoscaling rules, or a production monitoring stack.
- OAuth returns a JWT in URL query parameters and the frontend stores it in browser storage. Treat this as a known security limitation.
- Validate the manifest image references, resource requests, access controls, storage, and network exposure for your target environment.

## Clean up

To remove the Kubernetes workloads:

```sh
kubectl delete -f k8s/
```

To remove the Terraform-managed cluster and Artifact Registry resources, review the impact and then run:

```sh
cd terraform
terraform destroy -var="project_id=<YOUR_PROJECT_ID>"
```
