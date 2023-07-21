variable "parent_name" {
  description = "A short abbreviation of resource that the prism SDK website docs instance is created for, e.g. environment name"
}

variable "aws_region" {
  description = "The AWS region to create resources in."
}

variable "aws_ecs_capacity_provider" {
  description = "Capacity provider strategy FARGATE_SPOT or FARGATE"
  type        = string
}

variable "execution_role_arn" {
  type = string
}

variable "prism_sdk_website_docs_docker_image" {
  description = "Docker image for the SDK docs website."
}

variable "prism_sdk_website_docs_port" {
  description = "Port for the SDK docs website."
  type        = number
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
  description = "Subnet to deploy prism SDK website docs to - using AWSVPC networking"
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
