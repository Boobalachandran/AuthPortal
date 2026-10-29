variable "project_id" {
  description = "Google Cloud project ID for infrastructure deployment"
  type        = string

  # ⚠️ IMPORTANT: This default has been removed to prevent accidental deployment
  # to the wrong GCP project. After this change, you MUST provide the project ID.
  #
  # Instructions:
  # 1. Copy terraform/terraform.tfvars.example to terraform/terraform.tfvars
  # 2. Replace REPLACE_WITH_YOUR_GCP_PROJECT_ID with your actual GCP project ID
  # 3. Keep terraform.tfvars in .gitignore (it is not committed to Git)
  #
  # Alternative methods to provide project_id:
  # - Command line: terraform plan -var="project_id=your-project-id"
  # - Environment variable: export TF_VAR_project_id=your-project-id
  #
  # Existing deployments:
  # - If you have already deployed, terraform.tfstate contains your project ID
  # - Future terraform commands will use the value from terraform.tfvars or CLI
  # - Run 'terraform plan' to verify before applying

  validation {
    condition     = length(var.project_id) > 0
    error_message = "project_id must be a non-empty string"
  }
}

variable "region" {
  description = "Google Cloud region for regional resources"
  type        = string
  default     = "us-central1"
}

variable "cluster_name" {
  description = "Name of the GKE cluster"
  type        = string
  default     = "authportal-gke"
}


