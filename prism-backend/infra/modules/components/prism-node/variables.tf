variable "parent_name" {
  description = "A short abbreviation of resource that the node instance is created for, e.g. environment name"
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

variable "node_docker_image" {
  description = "Docker image for the node."
}

variable "port" {
  description = "Port number for the node"
  type        = number
}

variable "ecs_cluster_id" {
  description = "ID of ECS cluster"
}

variable "ecs_cluster_iam_role_name" {
  description = "Name of ECS cluster IAM role (used for dependencies only)"
}

variable "psql_host" {
  description = "PostgreSQL host for node to connect to"
}

variable "psql_database" {
  description = "PostgreSQL database to use for node"
}

variable "psql_username" {
  description = "PostgreSQL username to use for node"
}

variable "psql_password" {
  description = "PostgreSQL password to use for node"
}

variable "prometheus_enabled" {
  description = "If true, then will be added to Prometheus targets by Prometheus ECS"
  type        = bool
  default     = true
}

variable "prometheus_port" {
  description = "Port for Prometheus metrics endpoint"
  type        = number
  default     = 9095
}

variable "prometheus_endpoint" {
  description = "Prometheus metrics endpoint with scraping interval prefix (needed for https://github.com/signal-ai/prometheus-ecs-sd)"
  type        = string
  default     = "15s:/metrics"
}

variable "cardano_confirmation_blocks" {
  description = "Number of blocks to wait for a particular Cardano block to be confirmed by PRISM"
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

variable "cardano_wallet_api_host" {
  description = "Cardano wallet API host"
}

variable "cardano_wallet_api_port" {
  description = "Cardano wallet API port"
  type        = number
  default     = 8090
}

variable "cardano_wallet_id" {
  description = "Cardano wallet id to use for PRISM transactions"
}

variable "cardano_wallet_passphrase" {
  description = "Passphrase to the wallet used for PRISM transactions"
}

variable "cardano_payment_address" {
  description = "Address to send funds to when publishing PRISM transactions"
}

variable "vpc_id" {
  description = "ID of VPC to use"
}

variable "security_group_id" {
  description = "ID of Security Group to use"
}

variable "subnets" {
  description = "Subnet to deploy node to - using AWSVPC networking"
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
