variable "parent_name" {
  description = "A short abbreviation of resource that the management console instance is created for, e.g. environment name"
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

variable "management_console_docker_image" {
  description = "Docker image for the management console."
}

variable "port" {
  description = "Port number for the management console"
  type        = number
}

variable "ecs_cluster_id" {
  description = "ID of ECS cluster"
}

variable "ecs_cluster_iam_role_name" {
  description = "Name of ECS cluster IAM role (used for dependencies only)"
}

variable "node_host" {
  description = "PRISM node host for management console to connect to"
  type        = string
}

variable "node_port" {
  description = "PRISM node port for management console to connect to"
  type        = number
}

variable "connector_host" {
  description = "PRISM connector host for management console to connect to"
  type        = string
}

variable "connector_port" {
  description = "PRISM connector port for management console to connect to"
  type        = number
}

variable "psql_host" {
  description = "PostgreSQL host for management console to connect to"
}

variable "psql_database" {
  description = "PostgreSQL database to use for management console"
}

variable "psql_username" {
  description = "PostgreSQL username to use for management console"
}

variable "psql_password" {
  description = "PostgreSQL password to use for management console"
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

variable "vpc_id" {
  description = "ID of VPC to use"
}

variable "security_group_id" {
  description = "ID of Security Group to use"
}

variable "subnets" {
  description = "Subnet to deploy management console to - using AWSVPC networking"
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
