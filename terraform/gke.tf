resource "google_container_cluster" "authportal" {
  project          = var.project_id
  name             = var.cluster_name
  location         = var.region
  enable_autopilot = true

  deletion_protection = false
}
