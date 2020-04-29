variable parent_name {
  description = "A short abbreviation of resource that the connector instance is created for, e.g. environment name"
}

variable aws_region {
  description = "The AWS region to create resources in."
}

variable execution_role_arn {
  type = string
}

variable connector_docker_image {
  description = "Docker image for the connector."
}

variable port {
  description = "Port number for the connector"
  type        = number
}

variable ecs_cluster_id {
  description = "ID of ECS cluster"
}

variable ecs_cluster_iam_role_name {
  description = "Name of ECS cluster IAM role (used for dependencies only)"
}

variable psql_host {
  description = "PostgreSQL host for connector to connect to"
}

variable psql_database {
  description = "PostgreSQL database to use for connector"
}

variable psql_username {
  description = "PostgreSQL username to use for connector"
}

variable psql_password {
  description = "PostgreSQL password to use for connector"
}

variable vpc_id {
  description = "ID of VPC to use"
}

variable security_group_id {
  description = "ID of Security Group to use"
}

variable subnets {
  description = "Subnet to deploy connector to - using AWSVPC networking"
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
