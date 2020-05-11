variable env_name_short {
  description = "A short abbreviation for the environment name, used as in resource an DNS names."
}

variable aws_region {
  description = "The AWS region to create resources in."
}

variable connector_docker_image {
  description = "Docker image for the connector."
}

variable connector_port {
  description = "Port number for the connector"
  type        = number
}

variable landing_docker_image {
  description = "Docker image for the interactive demo web app."
}

variable landing_port {
  description = "Port for the interactive demo web app."
  type        = number
}

variable envoy_docker_image {
  description = "Docker image for the envoy grpc proxy."
}

variable ecs_cluster_id {
  description = "ID of ECS cluster"
}

variable ecs_cluster_iam_role_name {
  description = "Name of ECS cluster IAM role (used for dependencies only)"
}

variable envoy_port {
  description = "Port for the envoy gprc-web proxy."
  type        = number
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

variable intdemo_subnets {
  description = "Subnet to deploy intdemo service to - using AWSVPC networking"
}

variable envoy_subnets {
  description = "Subnet where ECS cluster is located"
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

variable tls_certificate_arn {
  description = "ARN of TLS certificate to install in the NLB."
}
