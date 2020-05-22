variable parent_name {
  description = "A short abbreviation of resource that the node instance is created for, e.g. environment name"
}

variable aws_region {
  description = "The AWS region to create resources in."
}

variable enabled {
  description = "Whether or not this module is enabled (as 'count' for modules doesn't work in Terraform 0.12)"
  type        = bool
  default     = true
}

variable execution_role_arn {
  type = string
}

variable node_docker_image {
  description = "Docker image for the node."
}

variable port {
  description = "Port number for the node"
  type        = number
}

variable ecs_cluster_id {
  description = "ID of ECS cluster"
}

variable ecs_cluster_iam_role_name {
  description = "Name of ECS cluster IAM role (used for dependencies only)"
}

variable psql_host {
  description = "PostgreSQL host for node to connect to"
}

variable psql_database {
  description = "PostgreSQL database to use for node"
}

variable psql_username {
  description = "PostgreSQL username to use for node"
}

variable psql_password {
  description = "PostgreSQL password to use for node"
}

variable vpc_id {
  description = "ID of VPC to use"
}

variable security_group_id {
  description = "ID of Security Group to use"
}

variable subnets {
  description = "Subnet to deploy node to - using AWSVPC networking"
}

variable private_dns_namespace_id {
  description = "ID of private DNS namespace to register to for Service Discovery"
}

variable private_dns_namespace_name {
  description = "Name of private DNS namespace to register to for Service Discovery"
}

variable log_group_name {
  description = "Name of AWS Cloudwatch log group"
}
