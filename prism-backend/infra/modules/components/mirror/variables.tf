variable "parent_name" {
  description = "A short abbreviation of resource that the mirror instance is created for, e.g. environment name"
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

variable "mirror_docker_image" {
  description = "Docker image for the mirror."
}

variable "port" {
  description = "Port number for the mirror"
  type        = number
}

variable "ecs_cluster_id" {
  description = "ID of ECS cluster"
}

variable "ecs_cluster_iam_role_name" {
  description = "Name of ECS cluster IAM role (used for dependencies only)"
}

variable "node_host" {
  description = "PRISM node host for mirror to connect to"
  type        = string
}

variable "node_port" {
  description = "PRISM node port for mirror to connect to"
  type        = number
}

variable "connector_host" {
  description = "PRISM node host for mirror to connect to"
  type        = string
}

variable "connector_port" {
  description = "PRISM node port for mirror to connect to"
  type        = number
}

variable "psql_host" {
  description = "PostgreSQL host for mirror to connect to"
}

variable "psql_database" {
  description = "PostgreSQL database to use for mirror"
}

variable "psql_username" {
  description = "PostgreSQL username to use for mirror"
}

variable "psql_password" {
  description = "PostgreSQL password to use for mirror"
}

variable "cardano_db_sync_psql_host" {
  description = "PostgreSQL host for the cardano-db-sync db"
}

variable "cardano_db_sync_psql_username" {
  description = "PostgreSQL username for the cardano-db-sync db"
}

variable "cardano_db_sync_psql_password" {
  description = "PostgreSQL password for the cardano-db-sync db"
}

variable "cardano_db_sync_psql_database" {
  description = "PostgreSQL dababase name for the cardano-db-sync db"
}

variable "did" {
  description = "DID that mirror should use to issue credentials and interact with connector"
}

variable "did_private_key" {
  description = "Serialization of mirror DID master private key"
}

variable "vpc_id" {
  description = "ID of VPC to use"
}

variable "security_group_id" {
  description = "ID of Security Group to use"
}

variable "subnets" {
  description = "Subnet to deploy mirror to - using AWSVPC networking"
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
