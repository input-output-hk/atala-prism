variable "parent_name" {
  description = "A short abbreviation of resource that the envoy instance is created for, e.g. environment name"
}

variable "aws_region" {
  description = "The AWS region to create resources in."
}

variable "aws_ecs_capacity_provider" {
  description = "Capacity provider strategy types FARGATE_SPOT or FARGATE"
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

variable "envoy_docker_image" {
  description = "Docker image for the envoy."
}

variable "exposed_ports" {
  description = "Ports that should be exposed by envoy and corresponding AWS LB target groups"
  type        = list(object({ port = number, lb_target_group_arn = string }))
}

variable "environment" {
  description = "Environment variables to pass to the envoy container"
  type        = list(object({ name = string, value = string }))
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
  description = "Subnet to deploy envoy to"
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
