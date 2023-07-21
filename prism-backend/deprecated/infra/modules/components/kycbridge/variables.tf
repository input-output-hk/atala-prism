variable "parent_name" {
  description = "A short abbreviation of resource that the kycbridge instance is created for, e.g. environment name"
}

variable "aws_region" {
  description = "The AWS region to create resources in."
}

variable "aws_ecs_capacity_provider" {
  description = "Capacity provider strategy FARGATE_SPOT or FARGATE"
  type        = string
}

variable "enabled" {
  description = "Whether or not this module is enabled (as 'count' for modules doesn't work in Terraform 0.12)"
  type        = bool
  default     = true
}

variable "execution_role_arn" {
  type = string
}

variable "kycbridge_docker_image" {
  description = "Docker image for the kycbridge."
}

variable "port" {
  description = "Port number for the kycbridge"
  type        = number
}

variable "ecs_cluster_id" {
  description = "ID of ECS cluster"
}

variable "ecs_cluster_iam_role_name" {
  description = "Name of ECS cluster IAM role (used for dependencies only)"
}

variable "node_host" {
  description = "PRISM node host for kycbridge to connect to"
  type        = string
}

variable "node_port" {
  description = "PRISM node port for kycbridge to connect to"
  type        = number
}

variable "connector_host" {
  description = "PRISM connector host for kycbridge to connect to"
  type        = string
}

variable "connector_port" {
  description = "PRISM connector port for kycbridge to connect to"
  type        = number
}

variable "psql_host" {
  description = "PostgreSQL host for kycbridge to connect to"
}

variable "psql_database" {
  description = "PostgreSQL database to use for kycbridge"
}

variable "psql_username" {
  description = "PostgreSQL username to use for kycbridge"
}

variable "psql_password" {
  description = "PostgreSQL password to use for kycbridge"
}


variable "did" {
  description = "DID that kycbridge should use to issue credentials and interact with connector"
}

variable "did_private_key" {
  description = "Serialization of kycbridge DID master private key"
}

variable "acuant_username" {
  description = "Username for interacting with Acuant API"
}

variable "acuant_password" {
  description = "Password for interacting with Acuant API"
}

variable "acuant_subscription_id" {
  description = "Subscription ID for interacting with Acuant API"
}

variable "identitymind_url" {
  description = "URL of Acuant IdentityMind API"
  default     = "https://sandbox.identitymind.com"
}

variable "identitymind_profile" {
  description = "Profile tu use when interacting with Acuant IdentityMind API"
  default     = "assureid"
}

variable "identitymind_username" {
  description = "Username for interacting with Acuant IdentityMind API"
}

variable "identitymind_password" {
  description = "Password for interacting with Acuant IdentityMind API"
}

variable "vpc_id" {
  description = "ID of VPC to use"
}

variable "security_group_id" {
  description = "ID of Security Group to use"
}

variable "subnets" {
  description = "Subnet to deploy kycbridge to - using AWSVPC networking"
}

variable "private_dns_namespace_id" {
  description = "ID of private DNS namespace to register to for Service Discovery"
}

variable "private_dns_namespace_name" {
  description = "Name of private DNS namespace to register to for Service Discovery"
}

variable "log_group_name" {
  description = "Name of AWS Cloudwatch log group"
}
