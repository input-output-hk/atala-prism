variable "parent_name" {
  description = "A short abbreviation of resource that the PRISM console instance is created for, e.g. environment name"
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

variable "prism_console_docker_image" {
  description = "Docker image for the interactive demo web app."
}

variable "port" {
  description = "Port for the interactive demo web app."
  type        = number
}

variable "connector_grpc_url" {
  type = string
}

variable "ecs_cluster_id" {
  description = "ID of ECS cluster"
}

variable "ecs_cluster_iam_role_name" {
  description = "Name of ECS cluster IAM role (used for dependencies only)"
}

variable "vpc_id" {
  description = "ID of VPC to use"
}

variable "security_group_id" {
  description = "ID of Security Group to use"
}

variable "subnets" {
  description = "Subnet to deploy intdemo PRISM console page to - using AWSVPC networking"
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
