resource "google_artifact_registry_repository" "authportal" {
  project       = var.project_id
  location      = var.region
  repository_id = "authportal-repo"
  description   = "Docker images for the AuthPortal application"
  format        = "DOCKER"
}
